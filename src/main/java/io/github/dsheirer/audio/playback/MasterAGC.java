package io.github.dsheirer.audio.playback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Master Automatic Gain Control that normalizes audio output to a target dBFS level.
 * Uses envelope detection with configurable attack and release time constants.
 */
public class MasterAGC
{
    private static final Logger mLog = LoggerFactory.getLogger(MasterAGC.class);
    private static final float TARGET_DBFS = -18.0f;
    private static final float MAX_GAIN = 30.0f;
    private static final float MIN_GAIN = -10.0f;
    private static final float ATTACK_MS = 5.0f;
    private static final float RELEASE_MS = 50.0f;

    private float mCurrentGain = 0.0f;
    private float mAttackCoeff;
    private float mReleaseCoeff;
    private boolean mEnabled = true;

    public MasterAGC(float sampleRate)
    {
        mAttackCoeff = (float)Math.exp(-1.0 / (sampleRate * ATTACK_MS / 1000.0));
        mReleaseCoeff = (float)Math.exp(-1.0 / (sampleRate * RELEASE_MS / 1000.0));
        mLog.info("MasterAGC initialized: target={}dBFS, attack={}ms, release={}ms",
            TARGET_DBFS, ATTACK_MS, RELEASE_MS);
    }

    /**
     * Process a buffer of audio samples through the AGC.
     * @param samples input audio samples (-1.0 to 1.0 range)
     * @return gain-adjusted samples clamped to -1.0 to 1.0
     */
    public float[] process(float[] samples)
    {
        if(!mEnabled || samples == null || samples.length == 0)
        {
            return samples;
        }

        // Find peak amplitude
        float peak = 0;
        for(float s : samples)
        {
            float abs = Math.abs(s);
            if(abs > peak) peak = abs;
        }

        if(peak < 1e-10f)
        {
            return samples;
        }

        // Calculate desired gain adjustment
        float peakDbfs = 20.0f * (float)Math.log10(peak);
        float desiredGain = TARGET_DBFS - peakDbfs;
        desiredGain = Math.max(MIN_GAIN, Math.min(MAX_GAIN, desiredGain));

        // Smooth gain changes with attack/release envelope
        float coeff = (desiredGain < mCurrentGain) ? mAttackCoeff : mReleaseCoeff;
        mCurrentGain = coeff * mCurrentGain + (1.0f - coeff) * desiredGain;

        // Apply gain
        float linearGain = (float)Math.pow(10.0, mCurrentGain / 20.0);
        float[] output = new float[samples.length];
        for(int i = 0; i < samples.length; i++)
        {
            output[i] = Math.max(-1.0f, Math.min(1.0f, samples[i] * linearGain));
        }

        return output;
    }

    public void setEnabled(boolean enabled) { mEnabled = enabled; }
    public boolean isEnabled() { return mEnabled; }
    public float getCurrentGainDb() { return mCurrentGain; }
}
