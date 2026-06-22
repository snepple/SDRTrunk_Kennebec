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
    }

    /**
     * Main processing method to process audio segments
     */
    private void processAudioSegments()
    {
        mNewAudioSegments.drainTo(mAudioSegments);
        if (!mAudioSegments.isEmpty()) {
            mLog.info("AudioStreamingManager processing " + mAudioSegments.size() + " segments...");
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

                    audioSegment.decrementConsumerCount();
                    stopRealTimeStreams(audioSegment);
                }
            }
        }
    }

    /**
     * Starts or continues real-time audio forwarding for an audio segment.
     * Finds real-time broadcasters (like Zello) that match the segment's broadcast channels
     * and forwards new audio buffers to them incrementally.
     */
    private void forwardRealTimeAudio(AudioSegment audioSegment)
    {
        if(mBroadcastModel == null || !audioSegment.hasBroadcastChannels())
        {
            return;
        }

        // Start real-time streams for newly arrived segments
        if(!mRealTimeStreams.containsKey(audioSegment))
        {
            List<IRealTimeAudioBroadcaster> rtBroadcasters = new ArrayList<>();

            for(BroadcastChannel broadcastChannel : audioSegment.getBroadcastChannels())
            {
                AbstractAudioBroadcaster<?> broadcaster = mBroadcastModel.getBroadcaster(broadcastChannel.getChannelName());
                if(broadcaster instanceof IRealTimeAudioBroadcaster rtb && rtb.isRealTimeReady())
                {
                    IdentifierCollection identifiers = audioSegment.getIdentifierCollection() != null
                            ? new IdentifierCollection(audioSegment.getIdentifierCollection().getIdentifiers())
                            : null;
                    rtb.startRealTimeStream(identifiers);
                    rtBroadcasters.add(rtb);
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
     * Stops any active real-time streams for the given audio segment.
     */
    private void stopRealTimeStreams(AudioSegment audioSegment)
    {
        List<IRealTimeAudioBroadcaster> rtBroadcasters = mRealTimeStreams.remove(audioSegment);
        mLastBufferIndex.remove(audioSegment);

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
