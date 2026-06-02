package io.github.dsheirer.module.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AI-powered squelch threshold advisor that analyzes noise floor samples over time
 * and recommends optimal squelch thresholds based on environmental conditions.
 * Uses rolling average + headroom calculation to adapt to changing RF environments.
 */
public class SquelchAIAdvisor
{
    private static final Logger mLog = LoggerFactory.getLogger(SquelchAIAdvisor.class);
    private static final int HISTORY_SIZE = 100;
    private static final float HEADROOM_DB = 6.0f;

    private final float[] mNoiseHistory = new float[HISTORY_SIZE];
    private int mHistoryIndex = 0;
    private float mRecommendedThreshold = -80.0f;

    /**
     * Record a noise floor measurement for analysis.
     * @param noiseDbfs noise floor in dBFS
     */
    public void recordNoiseFloor(float noiseDbfs)
    {
        mNoiseHistory[mHistoryIndex % HISTORY_SIZE] = noiseDbfs;
        mHistoryIndex++;

        if(mHistoryIndex % HISTORY_SIZE == 0)
        {
            recalculate();
        }
    }

    private void recalculate()
    {
        int count = Math.min(mHistoryIndex, HISTORY_SIZE);
        float sum = 0;
        float max = Float.MIN_VALUE;

        for(int i = 0; i < count; i++)
        {
            sum += mNoiseHistory[i];
            if(mNoiseHistory[i] > max)
            {
                max = mNoiseHistory[i];
            }
        }

        float avg = sum / count;
        mRecommendedThreshold = avg + HEADROOM_DB;

        mLog.info("SquelchAI: avg noise={}dBFS, peak={}dBFS, recommended threshold={}dBFS",
            String.format("%.1f", avg), String.format("%.1f", max),
            String.format("%.1f", mRecommendedThreshold));
    }

    /**
     * Get the currently recommended squelch threshold.
     * @return threshold in dBFS
     */
    public float getRecommendedThreshold()
    {
        return mRecommendedThreshold;
    }

    /**
     * Get the average noise floor from recorded samples.
     * @return average noise floor in dBFS
     */
    public float getAverageNoiseFloor()
    {
        int count = Math.min(mHistoryIndex, HISTORY_SIZE);
        if(count == 0) return -100.0f;

        float sum = 0;
        for(int i = 0; i < count; i++)
        {
            sum += mNoiseHistory[i];
        }
        return sum / count;
    }
}
