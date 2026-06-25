/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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
package io.github.dsheirer.audio;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.audio.squelch.ISquelchStateListener;
import io.github.dsheirer.audio.squelch.SquelchState;
import io.github.dsheirer.audio.squelch.SquelchStateEvent;
import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.dsp.filter.fir.FIRFilterSpecification;
import io.github.dsheirer.dsp.filter.fir.real.IRealFilter;
import io.github.dsheirer.dsp.filter.fir.remez.RemezFIRFilterDesigner;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.real.IRealBufferListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides packaging of demodulated audio sample buffers into audio segments for broadcast to registered listeners.
 * Includes audio packet metadata in constructed audio segments.
 *
 * Incorporates audio squelch state listener to control if audio packets are broadcast or ignored.
 */
public class AudioModule extends AbstractAudioModule implements ISquelchStateListener, IRealBufferListener,
    Listener<float[]>
{
    private static final Logger mLog = LoggerFactory.getLogger(AudioModule.class);
    private static final int SAMPLE_RATE = 8000;
    private static float[] sHighPassFilterCoefficients;
    private final boolean mAudioFilterEnable;

    static
    {
        FIRFilterSpecification specification = FIRFilterSpecification.highPassBuilder()
            .sampleRate(8000)
            .stopBandCutoff(200)
            .stopBandAmplitude(0.0)
            .stopBandRipple(0.025)
            .passBandStart(300)
            .passBandAmplitude(1.0)
            .passBandRipple(0.01)
            .build();
        try
        {
            RemezFIRFilterDesigner designer = new RemezFIRFilterDesigner(specification);

            if(designer.isValid())
            {
                sHighPassFilterCoefficients = designer.getImpulseResponse();
            }
        }
        catch(FilterDesignException fde)
        {
            mLog.error("Filter design error", fde);
        }
    }

    private final IRealFilter mHighPassFilter = FilterFactory.getRealFilter(sHighPassFilterCoefficients);
    private final SquelchStateListener mSquelchStateListener = new SquelchStateListener();
    private SquelchState mSquelchState = SquelchState.SQUELCH;
    private Identifier mConfiguredTalkgroup;

    //Minimum call duration gating: when > 0, audio is buffered internally until the accumulated duration
    //exceeds this threshold. If squelch closes before the threshold is reached, the buffered audio is
    //discarded silently — preventing brief static bursts from being streamed. Once the call qualifies,
    //all buffered audio is flushed into the audio segment and normal real-time streaming resumes.
    private int mMinCallDurationMs = 0;
    private int mMinCallDurationSamples = 0;
    private List<float[]> mPendingAudioBuffer;
    private int mPendingAudioSampleCount = 0;
    private boolean mCallQualified = false;

    /**
     * Creates an Audio Module.
     *
     * @param aliasList for aliasing identifiers
     * @param timeslot for this audio module
     * @param maxAudioSegmentLength in milliseconds
     * @param audioFilterEnable to enable or disable high-pass audio filter
     */
    public AudioModule(AliasList aliasList, int timeslot, long maxAudioSegmentLength, boolean audioFilterEnable)
    {
        super(aliasList, timeslot, maxAudioSegmentLength);
        mAudioFilterEnable = audioFilterEnable;
    }

    /**
     * Creates an Audio Module.
     * @param aliasList for aliasing identifiers
     * @param audioFilterEnable to enable or disable high-pass audio filter
     */
    public AudioModule(AliasList aliasList, boolean audioFilterEnable)
    {
        super(aliasList);
        mAudioFilterEnable = audioFilterEnable;
    }

    @Override
    protected int getTimeslot()
    {
        return 0;
    }

    /**
     * Sets a configured talkgroup identifier that will be preserved across reset() calls.
     * Conventional (NBFM/AM) channels use a fixed per-channel talkgroup for alias/stream
     * routing. Without this, reset() clears the talkgroup and subsequent audio segments
     * have no TO identifier, breaking streaming and alias matching.
     *
     * @param talkgroup the talkgroup identifier to preserve across resets
     */
    public void setConfiguredTalkgroup(Identifier talkgroup)
    {
        mConfiguredTalkgroup = talkgroup;
    }

    @Override
    public void reset()
    {
        getIdentifierCollection().clear();

        //Re-seed the configured talkgroup so the next audio segment carries it for alias/stream routing.
        //Without this, conventional channels lose their talkgroup after each call ends and the next call's
        //audio segment has no TO identifier, causing "Audio call not streamed - no stream/alias match".
        if(mConfiguredTalkgroup != null)
        {
            getIdentifierCollection().update(mConfiguredTalkgroup);
        }
    }

    @Override
    public void start()
    {
    }

    /**
     * Sets the minimum call duration in milliseconds. When > 0, audio is buffered until the call
     * duration exceeds this threshold before being dispatched to the audio segment / streaming.
     * If squelch closes before the threshold, the buffered audio is discarded, preventing static
     * bursts from being streamed.
     *
     * @param minCallDurationMs minimum duration in milliseconds (0 = disabled, audio flows immediately)
     */
    public void setMinCallDurationMs(int minCallDurationMs)
    {
        mMinCallDurationMs = Math.max(0, minCallDurationMs);
        mMinCallDurationSamples = (mMinCallDurationMs * SAMPLE_RATE) / 1000;

        if(mMinCallDurationMs > 0)
        {
            mLog.info("Audio min call duration gate set to {}ms ({} samples)", mMinCallDurationMs, mMinCallDurationSamples);
        }
    }

    @Override
    public Listener<SquelchStateEvent> getSquelchStateListener()
    {
        return mSquelchStateListener;
    }

    @Override
    public void receive(float[] audioBuffer)
    {
        if(mSquelchState == SquelchState.UNSQUELCH)
        {
            if(mAudioFilterEnable)
            {
                audioBuffer = mHighPassFilter.filter(audioBuffer);
            }

            if(mMinCallDurationMs > 0 && !mCallQualified)
            {
                //Buffer audio until the call qualifies
                if(mPendingAudioBuffer == null)
                {
                    mPendingAudioBuffer = new ArrayList<>();
                }
                mPendingAudioBuffer.add(audioBuffer);
                mPendingAudioSampleCount += audioBuffer.length;

                if(mPendingAudioSampleCount >= mMinCallDurationSamples)
                {
                    //Call has qualified - flush all buffered audio into the segment
                    mCallQualified = true;
                    for(float[] buffered : mPendingAudioBuffer)
                    {
                        addAudio(buffered);
                    }
                    mPendingAudioBuffer.clear();
                    mPendingAudioBuffer = null;
                    mPendingAudioSampleCount = 0;
                }
            }
            else
            {
                //Either no min duration configured, or call already qualified - pass audio through
                addAudio(audioBuffer);
            }
        }
    }

    @Override
    public Listener<float[]> getBufferListener()
    {
        //Redirect received reusable buffers to the receive(buffer) method
        return this;
    }

    /**
     * Discards any pending (not-yet-qualified) audio buffer. Called when squelch closes before
     * the call reaches the minimum duration threshold.
     */
    private void discardPendingAudio()
    {
        if(mPendingAudioBuffer != null)
        {
            int discardedMs = (mPendingAudioSampleCount * 1000) / SAMPLE_RATE;
            if(discardedMs > 0)
            {
                mLog.debug("Discarded {}ms of audio (below {}ms min call duration threshold)",
                        discardedMs, mMinCallDurationMs);
            }
            mPendingAudioBuffer.clear();
            mPendingAudioBuffer = null;
        }
        mPendingAudioSampleCount = 0;
    }

    /**
     * Wrapper for squelch state listener
     */
    public class SquelchStateListener implements Listener<SquelchStateEvent>
    {
        @Override
        public void receive(SquelchStateEvent event)
        {
            SquelchState squelchState = event.getSquelchState();

            if(mSquelchState != squelchState)
            {
                mSquelchState = squelchState;

                if(mSquelchState == SquelchState.SQUELCH)
                {
                    if(mMinCallDurationMs > 0 && !mCallQualified)
                    {
                        //Call ended before qualifying — discard buffered audio silently
                        discardPendingAudio();
                    }
                    else
                    {
                        closeAudioSegment();
                    }

                    //Reset qualification state for the next call
                    mCallQualified = false;
                }
            }
        }
    }
}
