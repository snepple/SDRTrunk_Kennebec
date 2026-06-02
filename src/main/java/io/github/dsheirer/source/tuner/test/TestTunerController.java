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
package io.github.dsheirer.source.tuner.test;

import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.LoggingTunerErrorListener;
import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.configuration.TunerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestTunerController extends TunerController
{
    private final static Logger mLog = LoggerFactory.getLogger(TestTunerController.class);

    public static final long MINIMUM_FREQUENCY = 1l;
    public static final long MAXIMUM_FREQUENCY = 1_000_000_000l;
    public static final int SAMPLE_RATE = 2_400_000;
    public static final int DC_NOISE_BANDWIDTH = 0;
    public static final double USABLE_BANDWIDTH_PERCENTAGE = 1.00;

    public static final int SPECTRAL_FRAME_RATE = 20;
    public static final long SAMPLE_GENERATION_INTERVAL = 1000 / SPECTRAL_FRAME_RATE;

    private SampleGenerator mSampleGenerator;
    private long mFrequency = 460662500l;

    /**
     * Tuner controller testing implementation.
      */
    public TestTunerController()
    {
        super(new LoggingTunerErrorListener());

        setMinimumFrequency(MINIMUM_FREQUENCY);
        setMaximumFrequency(MAXIMUM_FREQUENCY);
        setMiddleUnusableHalfBandwidth(DC_NOISE_BANDWIDTH);
        setUsableBandwidthPercentage(USABLE_BANDWIDTH_PERCENTAGE);

        int sweepRate = 0;  //Hz per interval
//        long initialToneFrequency = SAMPLE_RATE / 2 + 100;
        long initialToneFrequency = 50000;
        mSampleGenerator = new SampleGenerator(SAMPLE_RATE, initialToneFrequency, sweepRate);

        try
        {
            mFrequencyController.setFrequency(mFrequency);
            mFrequencyController.setSampleRate(SAMPLE_RATE);
        }
        catch(Exception e)
        {
            mLog.error("Error!", e);
        }
    }

    @Override
    public int getBufferSampleCount()
    {
        return SAMPLE_RATE / SPECTRAL_FRAME_RATE;
    }

    @Override
    public void start() throws SourceException
    {
        //No-op
    }

    @Override
    public void stop()
    {
        //No-op
    }

    @Override
    public TunerType getTunerType()
    {
        return TunerType.TEST;
    }

    @Override
    public void addBufferListener(Listener<INativeBuffer> listener)
    {
        mSampleGenerator.addListener(listener);
    }

    @Override
    public void removeBufferListener(Listener<INativeBuffer> listener)
    {
        mSampleGenerator.removeListener(listener);
    }

    @Override
    public void apply(TunerConfiguration config) throws SourceException
    {
        mLog.error("Request to apply tuner configuration was ignored");
    }

    /**
     * Current center frequency for this tuner
     * @throws SourceException
     */
    @Override
    public long getTunedFrequency() throws SourceException
    {
        return mFrequency;
    }

    /**
     * Sets the center frequency for this tuner
     * @param frequency in hertz
     * @throws SourceException
     */
    @Override
    public void setTunedFrequency(long frequency) throws SourceException
    {
        mFrequency = frequency;
    }

    /**
     * Sets the tone output of the frequency generator
     * @param frequency in the range: 0 <> Sample Rate
     */
    public void setToneFrequency(long frequency)
    {
        mSampleGenerator.setFrequency(frequency);
    }

    /**
     * Frequency of the tone being generated
     *
     * @return tone frequency in range: 0 <> Sample Rate
     */
    public long getToneFrequency()
    {
        return mSampleGenerator.getFrequency();
    }

    /**
     * Sets a synthetic waveform to be played back in a loop.
     * @param waveform complex baseband float array (interleaved I/Q)
     */
    public void setSyntheticWaveform(float[] waveform)
    {
        mSampleGenerator.setSyntheticWaveform(waveform);
    }

    /**
     * Synthesize a Two-Tone FM modulated signal
     */
    public void setTwoToneWaveform(float toneA, float durA, float toneB, float durB)
    {
        int sampleRate = SAMPLE_RATE;
        int samplesA = (int) (sampleRate * durA);
        int samplesB = (int) (sampleRate * durB);
        float[] iq = new float[(samplesA + samplesB) * 2];
        float deviation = 3000.0f;
        float kf = deviation / sampleRate;
        float phase = 0;
        int idx = 0;
        for (int i = 0; i < samplesA; i++) {
            float t = (float) i / sampleRate;
            float m = (float) Math.cos(2 * Math.PI * toneA * t);
            phase += 2 * Math.PI * kf * m;
            iq[idx++] = (float) Math.cos(phase);
            iq[idx++] = (float) Math.sin(phase);
        }
        for (int i = 0; i < samplesB; i++) {
            float t = (float) i / sampleRate;
            float m = (float) Math.cos(2 * Math.PI * toneB * t);
            phase += 2 * Math.PI * kf * m;
            iq[idx++] = (float) Math.cos(phase);
            iq[idx++] = (float) Math.sin(phase);
        }
        setSyntheticWaveform(iq);
    }

    /**
     * Synthesize an NBFM signal (simple tone)
     */
    public void setNbfmWaveform(float toneFreq, float duration)
    {
        setTwoToneWaveform(toneFreq, duration, toneFreq, 0);
    }

    /**
     * Synthesize a P25 Phase 1 C4FM signal (synthetic test pattern)
     */
    public void setP25Waveform(float duration)
    {
        int sampleRate = SAMPLE_RATE;
        int numSamples = (int) (sampleRate * duration);
        float[] iq = new float[numSamples * 2];
        float phase = 0;
        int idx = 0;
        // Symbol rate is 4800 baud. We will just cycle through -1800, -600, +600, +1800 deviations
        float[] deviations = {-1800f, -600f, 600f, 1800f};
        int samplesPerSymbol = sampleRate / 4800;
        
        for (int i = 0; i < numSamples; i++) {
            int symIdx = (i / samplesPerSymbol) % 4;
            float freqDev = deviations[symIdx];
            phase += 2 * Math.PI * freqDev / sampleRate;
            iq[idx++] = (float) Math.cos(phase);
            iq[idx++] = (float) Math.sin(phase);
        }
        setSyntheticWaveform(iq);
    }

    /**
     * Current sample rate for this tuner controller
     */
    @Override
    public double getCurrentSampleRate()
    {
        return mSampleGenerator.getSampleRate();
    }

    /**
     * Sets the sample rate for this tuner controller
     */
    public void setSampleRate(int sampleRate) throws SourceException
    {
        mSampleGenerator.setSampleRate(sampleRate);
        mFrequencyController.setSampleRate(sampleRate);
    }
}
