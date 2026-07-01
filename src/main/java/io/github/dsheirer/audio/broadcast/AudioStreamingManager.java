/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.audio.broadcast;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.id.broadcast.BroadcastChannel;
import io.github.dsheirer.audio.AudioSegment;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.patch.PatchGroup;
import io.github.dsheirer.identifier.patch.PatchGroupIdentifier;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.record.AudioSegmentRecorder;
import io.github.dsheirer.record.RecordFormat;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.util.TimeStamp;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.dsheirer.health.SystemHealthAlertEvent;
import io.github.dsheirer.eventbus.MyEventBus;

/**
 * Audio streaming manager monitors audio segments through completion and creates temporary streaming recordings on
 * disk and enqueues the temporary recording for streaming.
 */
public class AudioStreamingManager implements Listener<AudioSegment>
{
    private final static Logger mLog = LoggerFactory.getLogger(AudioStreamingManager.class);
    private LinkedTransferQueue<AudioSegment> mNewAudioSegments = new LinkedTransferQueue<>();
    private List<AudioSegment> mAudioSegments = new ArrayList<>();
    private Listener<AudioRecording> mAudioRecordingListener;
    private BroadcastFormat mBroadcastFormat;
    private UserPreferences mUserPreferences;
    private ScheduledFuture<?> mAudioSegmentProcessorFuture;
    //Dedicated, higher-priority executor for the core audio-streaming dispatch so it is not head-of-line blocked
    //behind ancillary tasks (monitors, AI, discovery) on the shared scheduled pool.
    private java.util.concurrent.ScheduledExecutorService mDispatchExecutor;
    private int mNextRecordingNumber = 1;
    private BroadcastModel mBroadcastModel;

    // Real-time streaming state: maps audio segment to its active real-time broadcasters
    private java.util.Map<AudioSegment, List<IRealTimeAudioBroadcaster>> mRealTimeStreams =
            new java.util.concurrent.ConcurrentHashMap<>();
    private java.util.Map<AudioSegment, Integer> mLastBufferIndex = new java.util.concurrent.ConcurrentHashMap<>();
    //Tracks segments for which we have already emitted a real-time routing diagnostic so the log isn't flooded.
    private java.util.Set<AudioSegment> mDiagnosedSegments =
            java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    //sdrtrunk decoded audio is 8 kHz mono.
    private static final double AUDIO_SAMPLE_RATE_HZ = 8000.0;
    //Real-time stream min-duration gate: when set > 0, a real-time stream is not opened until this much squelch-open
    //audio has accumulated for the segment.  This prevents ultra-short openings (noise/static bursts on a weak
    //channel, e.g. Sidney FD) from pushing static to listeners and from churning the broadcaster with rapid
    //start/stop cycles (which also triggers "channel is not ready" deferrals).  No audio is lost: forwarding starts
    //from buffer index 0, so the buffered audio is flushed when the stream opens - the start is just delayed by the
    //gate window, and openings shorter than the gate never open a stream.
    //
    //DEFAULT 0 (disabled): the gate is global (affects every stream), so enabling it would also delay/suppress
    //legitimate short transmissions on all channels.  It is therefore opt-in via the system property; for a single
    //static-prone channel, prefer the per-channel mitigations (tuner gain, CTCSS/DCS tone squelch, noise-squelch
    //threshold, minimum call duration).  Enable globally with, e.g., -Dsdrtrunk.realtime.stream.min.duration.ms=700
    private static final long MIN_REALTIME_STREAM_DURATION_MS =
            Long.getLong("sdrtrunk.realtime.stream.min.duration.ms", 0L);

    //Deferred-call replay: a real-time stream (e.g. Zello) can be momentarily not-ready when a call completes -
    //connecting/reconnecting after an app restart, recovering from a dropped connection, or the channel not yet
    //online.  Such a call is otherwise lost silently (it is never queued, so it doesn't show in the aged-off or
    //error counters - only the channel's "received" count moves, which is why received > streamed).  Instead of
    //dropping it, the completed call's audio is held briefly and replayed to the broadcaster once it becomes ready,
    //so a reconnect window no longer loses calls.  Replayed calls flow through the normal start/stop stream path, so
    //they also count in the stream's Streamed/Uploaded total.
    //
    //Enabled by default; disable with -Dsdrtrunk.realtime.deferred.replay.disabled=true.  TTL bounds how late a call
    //may be replayed (default 120s) so nothing is delivered so far behind live that it misleads listeners.
    private static final boolean DEFERRED_REPLAY_ENABLED =
            !Boolean.getBoolean("sdrtrunk.realtime.deferred.replay.disabled");
    private static final long DEFERRED_REPLAY_TTL_MS =
            Long.getLong("sdrtrunk.realtime.deferred.replay.ttl.ms", 120000L);
    private static final int MAX_DEFERRED_REPLAYS = 50;

    //Completed real-time calls that could not be streamed because the broadcaster was not ready, awaiting replay.
    //Accessed only from the single audio-streaming dispatch thread (capture and flush both run there).
    private final java.util.ArrayDeque<DeferredCall> mDeferredCalls = new java.util.ArrayDeque<>();

    /**
     * A completed call that could not be streamed in real time because its broadcaster was not ready, held for replay.
     */
    private static class DeferredCall
    {
        private final String mBroadcastChannelName;
        private final List<float[]> mAudio;
        private final IdentifierCollection mIdentifiers;
        private final long mExpiresAt;

        private DeferredCall(String broadcastChannelName, List<float[]> audio, IdentifierCollection identifiers,
                             long expiresAt)
        {
            mBroadcastChannelName = broadcastChannelName;
            mAudio = audio;
            mIdentifiers = identifiers;
            mExpiresAt = expiresAt;
        }
    }

    /**
     * Constructs an instance
     * @param listener to receive completed audio recordings
     * @param broadcastFormat for temporary recordings
     * @param userPreferences to manage recording directories
     */
    public AudioStreamingManager(Listener<AudioRecording> listener, BroadcastFormat broadcastFormat, UserPreferences userPreferences)
    {
        mAudioRecordingListener = listener;
        mBroadcastFormat = broadcastFormat;
        mUserPreferences = userPreferences;

        // If the listener is a BroadcastModel, store it for real-time broadcaster lookups
        if(listener instanceof BroadcastModel bm)
        {
            mBroadcastModel = bm;
        }
    }

    /**
     * Sets the broadcast model to enable real-time audio routing to Zello and other
     * real-time broadcasters.
     * @param broadcastModel the broadcast model
     */
    public void setBroadcastModel(BroadcastModel broadcastModel)
    {
        mBroadcastModel = broadcastModel;
    }

    /**
     * Primary receive method
     */
    @Override
    public void receive(AudioSegment audioSegment)
    {
        mNewAudioSegments.add(audioSegment);
    }

    /**
     * Starts the scheduled audio segment processor
     */
    public void start()
    {
        if(mAudioSegmentProcessorFuture == null)
        {
            if(mDispatchExecutor == null)
            {
                mDispatchExecutor = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(
                    new io.github.dsheirer.controller.NamingThreadFactory("sdrtrunk audio streaming",
                        Thread.NORM_PRIORITY + 2));
            }

            mAudioSegmentProcessorFuture = mDispatchExecutor.scheduleAtFixedRate(new AudioSegmentProcessor(),
                0, 250, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Stops the scheduled audio segment processor
     */
    public void stop()
    {
        if(mAudioSegmentProcessorFuture != null)
        {
            mAudioSegmentProcessorFuture.cancel(true);
            mAudioSegmentProcessorFuture = null;
        }

        if(mDispatchExecutor != null)
        {
            mDispatchExecutor.shutdownNow();
            mDispatchExecutor = null;
        }

        for(AudioSegment audioSegment: mNewAudioSegments)
        {
            audioSegment.decrementConsumerCount();
        }

        mNewAudioSegments.clear();

        for(AudioSegment audioSegment: mAudioSegments)
        {
            stopRealTimeStreams(audioSegment);
            audioSegment.decrementConsumerCount();
        }

        mAudioSegments.clear();
        mRealTimeStreams.clear();
        mLastBufferIndex.clear();
        mDiagnosedSegments.clear();
        mDeferredCalls.clear();
    }

    /**
     * Main processing method to process audio segments
     */
    private void processAudioSegments()
    {
        mNewAudioSegments.drainTo(mAudioSegments);
        if (!mAudioSegments.isEmpty() && mLog.isTraceEnabled()) {
            //This runs every 250ms whenever audio is flowing; keep it at trace so it doesn't flood the logs or do
            //per-cycle string work in normal operation.
            mLog.trace("AudioStreamingManager processing {} segments...", mAudioSegments.size());
        }

        Iterator<AudioSegment> it = mAudioSegments.iterator();
        AudioSegment audioSegment;
        while(it.hasNext())
        {
            audioSegment = it.next();

            if(audioSegment.isDuplicate() && mUserPreferences.getCallManagementPreference().isDuplicateStreamingSuppressionEnabled())
            {
                it.remove();
                stopRealTimeStreams(audioSegment);
                audioSegment.decrementConsumerCount();
            }
            else
            {
                // Forward new audio buffers to any active real-time broadcasters
                forwardRealTimeAudio(audioSegment);

                if(audioSegment.completeProperty().get())
                {
                    it.remove();

                if(mAudioRecordingListener != null && audioSegment.hasBroadcastChannels())
                {
                    IdentifierCollection identifiers =
                            new IdentifierCollection(audioSegment.getIdentifierCollection().getIdentifiers());

                    if(identifiers.getToIdentifier() instanceof PatchGroupIdentifier patchGroupIdentifier)
                    {
                        if(mUserPreferences.getCallManagementPreference()
                                .getPatchGroupStreamingOption() == PatchGroupStreamingOption.TALKGROUPS)
                        {
                            //Decompose the patch group into the individual (patched) talkgroups and process the audio
                            //segment for each patched talkgroup.
                            PatchGroup patchGroup = patchGroupIdentifier.getValue();

                            List<Identifier> ids = new ArrayList<>();
                            ids.addAll(patchGroup.getPatchedTalkgroupIdentifiers());
                            ids.addAll(patchGroup.getPatchedRadioIdentifiers());

                            //If there are no patched radios/talkgroups, override user preference and stream as a patch group
                            if(ids.isEmpty() || audioSegment.getAliasList() == null)
                            {
                                processAudioSegment(audioSegment, identifiers, audioSegment.getBroadcastChannels());
                            }
                            else
                            {
                                AliasList aliasList = audioSegment.getAliasList();

                                for(Identifier identifier: ids)
                                {
                                    List<Alias> aliases = aliasList.getAliases(identifier);
                                    Set<BroadcastChannel> broadcastChannels = new HashSet<>();
                                    for(Alias alias: aliases)
                                    {
                                        broadcastChannels.addAll(alias.getBroadcastChannels());
                                    }

                                    if(!broadcastChannels.isEmpty())
                                    {
                                        MutableIdentifierCollection decomposedIdentifiers =
                                                new MutableIdentifierCollection(identifiers.getIdentifiers());
                                        //Remove patch group TO identifier & replace with the patched talkgroup/radio
                                        decomposedIdentifiers.remove(Role.TO);
                                        decomposedIdentifiers.update(identifier);
                                        processAudioSegment(audioSegment, decomposedIdentifiers, broadcastChannels);
                                    }
                                }
                            }
                        }
                        else
                        {
                            processAudioSegment(audioSegment, identifiers, audioSegment.getBroadcastChannels());
                        }
                    }
                    else
                    {
                        processAudioSegment(audioSegment, identifiers, audioSegment.getBroadcastChannels());
                    }
                }

                    //If the call completed without ever being streamed to a real-time broadcaster that was simply
                    //not ready (connecting/reconnecting/offline), capture its audio for replay when the broadcaster
                    //recovers, rather than losing it silently.
                    captureDeferredCallIfNeeded(audioSegment);

                    audioSegment.decrementConsumerCount();
                    stopRealTimeStreams(audioSegment);
                }
            }
        }

        flushDeferredCalls();
    }

    /**
     * Captures a completed call's audio for later replay when, at completion, one or more of its real-time
     * broadcasters never streamed it because the broadcaster was not ready.  Only real-time (e.g. Zello) broadcasters
     * are considered - recording-based broadcasters have their own queue/age-off handling.  Calls that did not pass
     * the min-duration gate are not captured (their suppression is intentional).
     */
    private void captureDeferredCallIfNeeded(AudioSegment audioSegment)
    {
        if(!DEFERRED_REPLAY_ENABLED || mBroadcastModel == null || !audioSegment.hasBroadcastChannels())
        {
            return;
        }

        //Respect the min-duration gate: a call too short to open a stream was intentionally suppressed.
        if(accumulatedAudioMs(audioSegment) < Math.max(MIN_REALTIME_STREAM_DURATION_MS, 1))
        {
            return;
        }

        List<IRealTimeAudioBroadcaster> alreadyStreamed = mRealTimeStreams.get(audioSegment);
        List<float[]> capturedAudio = null;
        IdentifierCollection capturedIdentifiers = null;

        for(BroadcastChannel broadcastChannel : audioSegment.getBroadcastChannels())
        {
            AbstractAudioBroadcaster<?> broadcaster = mBroadcastModel.getBroadcaster(broadcastChannel.getChannelName());

            if(!(broadcaster instanceof IRealTimeAudioBroadcaster rtb))
            {
                continue; //non-real-time broadcasters route via the recording path with their own age-off handling
            }

            //Skip broadcasters we actually streamed this call to.
            if(alreadyStreamed != null && alreadyStreamed.contains(rtb))
            {
                continue;
            }

            //Copy the audio once (shared, read-only) across however many broadcasters missed this call.
            if(capturedAudio == null)
            {
                capturedAudio = copyAudioBuffers(audioSegment);

                if(capturedAudio.isEmpty())
                {
                    return; //nothing to replay
                }

                capturedIdentifiers = audioSegment.getIdentifierCollection() != null
                        ? new IdentifierCollection(audioSegment.getIdentifierCollection().getIdentifiers())
                        : null;
            }

            //Bound the queue: drop the oldest pending replay if at capacity.
            while(mDeferredCalls.size() >= MAX_DEFERRED_REPLAYS)
            {
                mDeferredCalls.pollFirst();
            }

            mDeferredCalls.offerLast(new DeferredCall(broadcastChannel.getChannelName(), capturedAudio,
                    capturedIdentifiers, System.currentTimeMillis() + DEFERRED_REPLAY_TTL_MS));
            mLog.info("Audio call to stream [{}] held for replay - broadcaster was not ready when the call completed",
                    broadcastChannel.getChannelName());
        }
    }

    /**
     * Replays any held deferred calls whose broadcaster is now ready, and discards any that have exceeded their TTL.
     * Runs once per processing cycle on the dispatch thread.  A replay uses the normal start/forward/stop stream
     * path, so it also increments the stream's Streamed/Uploaded counter.
     */
    private void flushDeferredCalls()
    {
        if(mDeferredCalls.isEmpty())
        {
            return;
        }

        long now = System.currentTimeMillis();
        Iterator<DeferredCall> it = mDeferredCalls.iterator();

        while(it.hasNext())
        {
            DeferredCall deferred = it.next();

            if(deferred.mExpiresAt <= now)
            {
                it.remove();
                mLog.info("Discarding held call for stream [{}] - not replayed within {} ms",
                        deferred.mBroadcastChannelName, DEFERRED_REPLAY_TTL_MS);
                continue;
            }

            AbstractAudioBroadcaster<?> broadcaster = mBroadcastModel != null
                    ? mBroadcastModel.getBroadcaster(deferred.mBroadcastChannelName) : null;

            if(broadcaster instanceof IRealTimeAudioBroadcaster rtb && rtb.isRealTimeReady())
            {
                try
                {
                    rtb.startRealTimeStream(deferred.mIdentifiers != null
                            ? new IdentifierCollection(deferred.mIdentifiers.getIdentifiers()) : null);

                    for(float[] buffer : deferred.mAudio)
                    {
                        rtb.receiveRealTimeAudio(buffer);
                    }

                    rtb.stopRealTimeStream();
                    mLog.info("Replayed held call to stream [{}]", deferred.mBroadcastChannelName);
                }
                catch(Exception e)
                {
                    mLog.error("Error replaying held call to stream [{}]", deferred.mBroadcastChannelName, e);
                }

                it.remove();
            }
            //else: broadcaster still not ready (or a live/replay stream is active) - try again next cycle.
        }
    }

    /**
     * Snapshots the segment's audio buffers into a private, read-only list so the audio survives after the segment's
     * consumer count is decremented and it is recycled.
     */
    private List<float[]> copyAudioBuffers(AudioSegment audioSegment)
    {
        List<float[]> copy = new ArrayList<>();
        List<float[]> buffers = audioSegment.getAudioBuffers();

        if(buffers != null)
        {
            int size = buffers.size();

            for(int i = 0; i < size; i++)
            {
                float[] b = audioSegment.getAudioBuffer(i);

                if(b != null)
                {
                    copy.add(b.clone());
                }
            }
        }

        return copy;
    }

    /**
     * Starts or continues real-time audio forwarding for an audio segment.
     * Finds real-time broadcasters (like Zello) that match the segment's broadcast channels
     * and forwards new audio buffers to them incrementally.
     */
    private void forwardRealTimeAudio(AudioSegment audioSegment)
    {
        if(mBroadcastModel == null)
        {
            return;
        }

        if(!audioSegment.hasBroadcastChannels())
        {
            //Diagnostic: the alias for this call's talkgroup did not resolve to any broadcast channel. This is the
            //most common cause of "channel is configured to an alias but does not stream" - the alias either has no
            //stream assigned, or the talkgroup value/protocol on the alias does not match what the decoder produced.
            if(mDiagnosedSegments.add(audioSegment))
            {
                mLog.info("Audio call not streamed - no stream/alias match for its talkgroup. {} Call identifiers: {}",
                        diagnoseNoStreamReason(audioSegment),
                        audioSegment.getIdentifierCollection() != null
                                ? audioSegment.getIdentifierCollection().getIdentifiers() : "none");
            }
            return;
        }

        //Min-duration gate: wait until enough audio has accumulated before opening the stream so brief noise/static
        //bursts never start one.  Only applies before the stream has been opened; once open, audio always flows.
        if(MIN_REALTIME_STREAM_DURATION_MS > 0 &&
                !mRealTimeStreams.containsKey(audioSegment) &&
                accumulatedAudioMs(audioSegment) < MIN_REALTIME_STREAM_DURATION_MS)
        {
            return;
        }

        // Start real-time streams for newly arrived segments
        if(!mRealTimeStreams.containsKey(audioSegment))
        {
            List<IRealTimeAudioBroadcaster> rtBroadcasters = new ArrayList<>();
            boolean firstDiagnostic = mDiagnosedSegments.add(audioSegment);

            for(BroadcastChannel broadcastChannel : audioSegment.getBroadcastChannels())
            {
                AbstractAudioBroadcaster<?> broadcaster = mBroadcastModel.getBroadcaster(broadcastChannel.getChannelName());

                if(broadcaster == null)
                {
                    if(firstDiagnostic)
                    {
                        mLog.warn("Audio call not streamed - alias references stream [{}] but no broadcaster with that " +
                                "name exists. The stream may be renamed, disabled, or deleted.",
                                broadcastChannel.getChannelName());
                    }
                    continue;
                }

                if(broadcaster instanceof IRealTimeAudioBroadcaster rtb)
                {
                    if(rtb.isRealTimeReady())
                    {
                        IdentifierCollection identifiers = audioSegment.getIdentifierCollection() != null
                                ? new IdentifierCollection(audioSegment.getIdentifierCollection().getIdentifiers())
                                : null;
                        rtb.startRealTimeStream(identifiers);
                        rtBroadcasters.add(rtb);
                    }
                    else if(firstDiagnostic)
                    {
                        //Not ready: typically connecting/offline, or a prior stream is still active (relaxation hold).
                        mLog.info("Audio call to stream [{}] deferred - broadcaster not ready (connecting, channel " +
                                "offline, or previous stream still active).", broadcastChannel.getChannelName());
                    }
                }
                else if(firstDiagnostic)
                {
                    mLog.debug("Stream [{}] is not a real-time broadcaster; audio will be routed via file recording.",
                            broadcastChannel.getChannelName());
                }
            }

            if(!rtBroadcasters.isEmpty())
            {
                mRealTimeStreams.put(audioSegment, rtBroadcasters);
                mLastBufferIndex.put(audioSegment, 0);
            }
        }

        // Forward any new audio buffers
        List<IRealTimeAudioBroadcaster> rtBroadcasters = mRealTimeStreams.get(audioSegment);
        if(rtBroadcasters != null && !rtBroadcasters.isEmpty())
        {
            int lastIndex = mLastBufferIndex.getOrDefault(audioSegment, 0);
            List<float[]> buffers = audioSegment.getAudioBuffers();
            int currentSize = buffers.size();

            for(int i = lastIndex; i < currentSize; i++)
            {
                float[] buffer = audioSegment.getAudioBuffer(i);
                if(buffer != null)
                {
                    for(IRealTimeAudioBroadcaster rtb : rtBroadcasters)
                    {
                        rtb.receiveRealTimeAudio(buffer);
                    }
                }
            }

            mLastBufferIndex.put(audioSegment, currentSize);
        }
    }

    /**
     * Computes how many milliseconds of audio have accumulated for the segment so far, used by the real-time stream
     * min-duration gate.  Iterates by index against a size snapshot so concurrent growth from the decode thread can't
     * throw.  Audio is 8 kHz mono, so duration(ms) = samples / 8.
     * @param segment whose buffered audio is measured
     * @return accumulated audio duration in milliseconds
     */
    private double accumulatedAudioMs(AudioSegment segment)
    {
        List<float[]> buffers = segment.getAudioBuffers();

        if(buffers == null)
        {
            return 0;
        }

        long samples = 0;
        int size = buffers.size();

        for(int i = 0; i < size; i++)
        {
            float[] b = segment.getAudioBuffer(i);

            if(b != null)
            {
                samples += b.length;
            }
        }

        return (samples / AUDIO_SAMPLE_RATE_HZ) * 1000.0;
    }

    /**
     * Builds a specific, human-readable explanation for why an audio call produced no broadcast channels, so the log
     * pinpoints whether the cause is (a) no TO talkgroup, (b) the channel's talkgroup not matching any alias in its
     * alias list, (c) a matching alias that has no stream assigned, or (d) the channel having no alias list at all.
     */
    private String diagnoseNoStreamReason(AudioSegment audioSegment)
    {
        try
        {
            IdentifierCollection ic = audioSegment.getIdentifierCollection();
            Identifier to = (ic != null) ? ic.getToIdentifier() : null;

            if(to == null)
            {
                io.github.dsheirer.protocol.Protocol protocol = callProtocol(ic);

                //Analog (NBFM/AM) channels carry no talkgroup over the air, so the operator must set one in the channel
                //editor.  Digital protocols (DMR/P25/etc.) DO carry talkgroups, so a missing one means this particular
                //transmission had none (a direct/data or non-voice call) - telling the user to set 'Talkgroup To Assign'
                //would be wrong for those.
                if(protocol == io.github.dsheirer.protocol.Protocol.NBFM ||
                        protocol == io.github.dsheirer.protocol.Protocol.AM)
                {
                    return "The call has no TO/talkgroup identifier. NBFM/analog channels must have a 'Talkgroup To " +
                            "Assign' value set in the channel editor for alias and stream matching to work.";
                }

                return "The call has no TO/talkgroup identifier" +
                        (protocol != io.github.dsheirer.protocol.Protocol.UNKNOWN ? " (" + protocol + ")" : "") +
                        " - this transmission carried no talkgroup (for example a direct/data or non-voice call), so " +
                        "there is nothing to match against an alias or stream.";
            }

            String toDescription;
            if(to instanceof io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier tg)
            {
                toDescription = "TO talkgroup [" + Integer.toUnsignedString(tg.getValue()) + "] protocol [" +
                        tg.getProtocol() + "]";
            }
            else
            {
                toDescription = "TO identifier [" + to + "] (not a talkgroup; form=" + to.getForm() + ")";
            }

            AliasList aliasList = audioSegment.getAliasList();

            if(aliasList == null || aliasList.getName() == null || aliasList.getName().isEmpty())
            {
                return toDescription + " - the channel has no alias list assigned, so no alias/stream can match. " +
                        "Assign an alias list in the channel editor.";
            }

            List<Alias> matches = aliasList.getAliases(to);

            if(matches == null || matches.isEmpty())
            {
                return toDescription + " - no alias in list [" + aliasList.getName() + "] has a matching talkgroup " +
                        "value AND protocol. Verify the channel's 'Talkgroup To Assign' exactly equals the alias " +
                        "talkgroup (both value and protocol, e.g. NBFM vs APCO-25).";
            }

            boolean anyStream = false;
            for(Alias alias : matches)
            {
                if(!alias.getBroadcastChannels().isEmpty())
                {
                    anyStream = true;
                    break;
                }
            }

            if(!anyStream)
            {
                return toDescription + " matched alias(es) " + matches + " in list [" + aliasList.getName() +
                        "], but none of them has a stream assigned. Open the alias > Streaming tab and assign the stream.";
            }

            //Matched and has a stream but still no broadcast channels on the segment - unexpected; report raw state.
            return toDescription + " matched alias(es) " + matches + " with a stream assigned, but no broadcast " +
                    "channel reached the audio segment. This may be a timing/identifier-update issue - please report it.";
        }
        catch(Exception e)
        {
            return "(diagnosis failed: " + e.getMessage() + ")";
        }
    }

    /**
     * Best-effort determination of the protocol a call was decoded with, used only to tailor the "not streamed"
     * diagnostic message.  Returns the first identifier's concrete protocol, or UNKNOWN when none is available.
     */
    private static io.github.dsheirer.protocol.Protocol callProtocol(IdentifierCollection ic)
    {
        if(ic != null)
        {
            for(Identifier identifier : ic.getIdentifiers())
            {
                io.github.dsheirer.protocol.Protocol protocol = identifier.getProtocol();
                if(protocol != null && protocol != io.github.dsheirer.protocol.Protocol.UNKNOWN)
                {
                    return protocol;
                }
            }
        }

        return io.github.dsheirer.protocol.Protocol.UNKNOWN;
    }

    /**
     * Stops any active real-time streams for the given audio segment.
     */
    private void stopRealTimeStreams(AudioSegment audioSegment)
    {
        List<IRealTimeAudioBroadcaster> rtBroadcasters = mRealTimeStreams.remove(audioSegment);
        mLastBufferIndex.remove(audioSegment);
        mDiagnosedSegments.remove(audioSegment);

        if(rtBroadcasters != null)
        {
            for(IRealTimeAudioBroadcaster rtb : rtBroadcasters)
            {
                try
                {
                    rtb.stopRealTimeStream();
}
                catch(Exception e)
                {
                    mLog.error("Error stopping real-time stream", e);
                    MyEventBus.getGlobalEventBus().post(new SystemHealthAlertEvent(
                        SystemHealthAlertEvent.AlertType.INTEGRATION,
                        "Integration and Network Failure",
                        "Streaming disconnect: " + e.getMessage()
                    ));
                }
            }
        }
    }

    /**
     * Processes an audio segment for streaming by creating a temporary MP3 recording and submitting the recording
     * to the specific broadcast channel(s).
     * @param audioSegment to process for streaming
     * @param identifierCollection to use for the streamed audio recording.
     * @param broadcastChannels to receive the audio recording
     */
    private void processAudioSegment(AudioSegment audioSegment, IdentifierCollection identifierCollection,
                                     Set<BroadcastChannel> broadcastChannels)
    {
        Path path = getTemporaryRecordingPath();
        long length = 0;

        for(float[] audioBuffer: audioSegment.getAudioBuffers())
        {
            length += audioBuffer.length;
        }

        length /= 8; //Sample rate is 8000 samples per second, or 8 samples per millisecond.

        try
        {
            //Ensure the streaming directory exists - it can be deleted or become unavailable at runtime
            Files.createDirectories(path.getParent());

            AudioSegmentRecorder.record(audioSegment, path, RecordFormat.MP3, mUserPreferences, identifierCollection);

            AudioRecording audioRecording = new AudioRecording(path, broadcastChannels, identifierCollection,
                    audioSegment.getStartTimestamp(), length);
            mAudioRecordingListener.receive(audioRecording);
        }
        catch(Exception ioe)
        {
            mLog.error("Error recording temporary stream MP3 to [" + path + "] - check that the streaming " +
                "directory exists, is writable, and the disk is not full", ioe);
            MyEventBus.getGlobalEventBus().post(new SystemHealthAlertEvent(
                SystemHealthAlertEvent.AlertType.INTEGRATION,
                "Integration and Network Failure",
                "Error recording temporary stream MP3: " + ioe.getMessage()
            ));
        }
    }

    /**
     * Creates a temporary streaming recording file path
     */
    private Path getTemporaryRecordingPath()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(BroadcastModel.TEMPORARY_STREAM_FILE_SUFFIX);

        //Check for integer overflow and readjust negative value to 0
        if(mNextRecordingNumber < 0)
        {
            mNextRecordingNumber = 1;
        }

        int recordingNumber = mNextRecordingNumber++;

        sb.append(recordingNumber).append("_");
        sb.append(TimeStamp.getLongTimeStamp("_"));
        sb.append(mBroadcastFormat.getFileExtension());

        Path temporaryRecordingPath = mUserPreferences.getDirectoryPreference().getDirectoryStreaming().resolve(sb.toString());

        return temporaryRecordingPath;
    }

    /**
     * Scheduled runnable to process audio segments.
     */
    public class AudioSegmentProcessor implements Runnable
    {
        @Override
        public void run()
        {
            try
            {
                processAudioSegments();
            }
            catch(Throwable t)
            {
                mLog.error("Error processing audio segments for streaming", t);
            }
        }
    }
}
