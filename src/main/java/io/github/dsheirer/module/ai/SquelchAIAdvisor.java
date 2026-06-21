package io.github.dsheirer.module.ai;

import io.github.dsheirer.dsp.squelch.NoiseSquelch;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AI-powered squelch threshold advisor that analyzes noise floor samples over time
 * and recommends optimal squelch thresholds based on environmental conditions.
 * Uses rolling average + headroom calculation to adapt to changing RF environments.
 *
 * Two analysis modes are provided:
 * <ul>
 *     <li><b>dBFS noise-floor tracking</b> ({@link #recordNoiseFloor(float)}) - a rolling average of
 *     noise-floor power measurements with a fixed headroom, used for power-based advisories.</li>
 *     <li><b>Noise-variance calibration</b> ({@link #recordVarianceSample(float)} / {@link #calibrate()}) -
 *     samples the live {@code NoiseSquelch} variance stream (units 0.1-0.5, matching the squelch open/close
 *     thresholds) and recommends open/close thresholds positioned between the signal and noise-floor
 *     variance clusters.  This powers the manual "Calibrate Squelch" button.</li>
 * </ul>
 */
public class SquelchAIAdvisor
{
    private static final Logger mLog = LoggerFactory.getLogger(SquelchAIAdvisor.class);
    private static final int HISTORY_SIZE = 100;
    private static final float HEADROOM_DB = 6.0f;

    /**
     * Minimum number of variance samples required before a calibration can be computed.  At the
     * ~50 ms squelch-state broadcast cadence this is roughly two seconds of audio.
     */
    public static final int MIN_CALIBRATION_SAMPLES = 40;

    /**
     * Minimum spread between the low (signal) and high (noise-floor) variance percentiles for the
     * calibration to treat the capture as containing both signal and noise.  Below this the capture is
     * assumed to be noise-floor only and a conservative below-the-floor placement is used instead.
     */
    private static final float MINIMUM_CLUSTER_SEPARATION = 0.03f;

    //Tail-removal recommendation bounds (milliseconds) and the noise-floor-to-tail scaling factor.
    private static final int DEFAULT_TAIL_REMOVAL_MS = 100;
    private static final int MIN_TAIL_REMOVAL_MS = 50;
    private static final int MAX_TAIL_REMOVAL_MS = 250;
    private static final float TAIL_NOISE_SCALE_MS = 400f;

    private final float[] mNoiseHistory = new float[HISTORY_SIZE];
    private int mHistoryIndex = 0;
    private float mRecommendedThreshold = -80.0f;

    private final List<Float> mVarianceSamples = new ArrayList<>();

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

    /**
     * Records a single noise-variance sample from the live squelch state stream for calibration.  Units
     * match the {@code NoiseSquelch} open/close thresholds.  Invalid (negative/NaN/infinite) values are
     * ignored.
     * @param variance noise variance value, as reported by {@code NoiseSquelchState.noise()}.
     */
    public void recordVarianceSample(float variance)
    {
        if(variance >= 0f && !Float.isNaN(variance) && !Float.isInfinite(variance))
        {
            mVarianceSamples.add(variance);
        }
    }

    /**
     * @return number of variance samples collected since the last {@link #resetCalibration()}.
     */
    public int getCalibrationSampleCount()
    {
        return mVarianceSamples.size();
    }

    /**
     * @return true when enough variance samples have been collected to compute a calibration.
     */
    public boolean hasSufficientCalibrationData()
    {
        return mVarianceSamples.size() >= MIN_CALIBRATION_SAMPLES;
    }

    /**
     * Clears the collected variance samples to start a fresh calibration capture.
     */
    public void resetCalibration()
    {
        mVarianceSamples.clear();
    }

    /**
     * Computes recommended noise open/close thresholds from the collected variance samples.
     *
     * In the {@code NoiseSquelch} design, low variance indicates a clean signal (squelch should open) and
     * high variance indicates noise (squelch should close).  This positions the open and close thresholds
     * in the gap between the low (signal) and high (noise-floor) variance clusters.  When the capture
     * contains only the noise floor (little spread between clusters), the thresholds are placed
     * conservatively just below the noise floor so that ambient noise reliably keeps the squelch closed.
     *
     * @return a recommendation with open/close thresholds clamped into the valid NoiseSquelch range, or
     * null when there is insufficient data.
     */
    public Recommendation calibrate()
    {
        if(!hasSufficientCalibrationData())
        {
            return null;
        }

        List<Float> sorted = new ArrayList<>(mVarianceSamples);
        Collections.sort(sorted);

        float low = percentile(sorted, 10);     //~signal-present variance (clean audio)
        float median = percentile(sorted, 50);
        float high = percentile(sorted, 90);    //~noise-floor variance
        float separation = high - low;

        float open, close;

        if(separation >= MINIMUM_CLUSTER_SEPARATION)
        {
            //Both signal and noise observed - place thresholds in the gap between the clusters.
            open = low + (0.25f * separation);
            close = low + (0.55f * separation);
        }
        else
        {
            //Noise-floor only - place thresholds just below the floor so noise keeps squelch closed.
            open = median * 0.70f;
            close = median * 0.88f;
        }

        //Clamp into the valid control range and enforce a sensible open <= close ordering with a small gap.
        open = clampThreshold(open);
        close = clampThreshold(close);

        if(close < open)
        {
            float swap = open;
            open = close;
            close = swap;
        }

        //Ensure a minimum hysteresis gap between open and close where range allows.
        float minimumGap = 0.02f;
        if((close - open) < minimumGap)
        {
            close = clampThreshold(open + minimumGap);
            if((close - open) < minimumGap)
            {
                open = clampThreshold(close - minimumGap);
            }
        }

        //Hysteresis (consecutive-buffer hold counts, units of 10 ms): marginal signals - where the signal
        //and noise-floor variance clusters sit close together - chatter the squelch open/closed, so they get
        //longer hold counts to ride through brief dropouts; clean, well-separated signals can toggle faster.
        //Constrained to the squelch view's slider ranges (open 1-5, close = open + 0..5).
        int hysteresisOpen, hysteresisClose;
        if(separation >= 0.10f)
        {
            hysteresisOpen = 2; hysteresisClose = 4;     //clean, well-separated - react quickly
        }
        else if(separation >= 0.05f)
        {
            hysteresisOpen = 3; hysteresisClose = 6;     //moderate - close to defaults
        }
        else if(separation >= MINIMUM_CLUSTER_SEPARATION)
        {
            hysteresisOpen = 3; hysteresisClose = 7;     //marginal - hold longer before closing
        }
        else
        {
            hysteresisOpen = 4; hysteresisClose = 8;     //very marginal / noise-only - ride through stutter
        }

        String rationale;
        if(separation >= MINIMUM_CLUSTER_SEPARATION)
        {
            rationale = String.format("Saw both a clean-signal level (~%s) and a noise-floor level (~%s); set " +
                    "open/close in the gap between them so real signals open the squelch while noise keeps it closed.",
                    fmt(low), fmt(high));
        }
        else
        {
            rationale = String.format("Saw mostly background noise (~%s) with little signal; set the thresholds just " +
                    "below the noise floor so ambient noise reliably keeps the squelch closed.", fmt(median));
        }

        String hysteresisReason = (separation >= 0.10f)
                ? "react quickly on this clean, well-separated signal"
                : (separation < MINIMUM_CLUSTER_SEPARATION)
                        ? "ride through brief dropouts and stop squelch stutter on this marginal signal"
                        : "balance responsiveness against squelch stutter";
        rationale += String.format("  Set hysteresis to open=%d, close=%d (x10 ms) to %s.",
                hysteresisOpen, hysteresisClose, hysteresisReason);

        mLog.info("SquelchAI calibration: samples={}, low(p10)={}, median={}, high(p90)={} -> open={}, close={}, " +
                        "hysteresis open={}, close={}", mVarianceSamples.size(), fmt(low), fmt(median), fmt(high),
                fmt(open), fmt(close), hysteresisOpen, hysteresisClose);

        return new Recommendation(open, close, hysteresisOpen, hysteresisClose, rationale);
    }

    /**
     * Recommends a squelch tail-removal duration (milliseconds) based on the measured noise floor.  Noisier
     * channels get a longer tail to cut the squelch-tail noise burst after a transmission ends, while
     * cleaner channels get a shorter tail to avoid clipping the end of audio.
     * @return recommended tail removal in milliseconds, clamped to a sensible range.
     */
    public int getRecommendedTailRemovalMs()
    {
        if(!hasSufficientCalibrationData())
        {
            return DEFAULT_TAIL_REMOVAL_MS;
        }

        List<Float> sorted = new ArrayList<>(mVarianceSamples);
        Collections.sort(sorted);
        float high = percentile(sorted, 90); //~noise-floor variance

        int tail = Math.round(MIN_TAIL_REMOVAL_MS + (high * TAIL_NOISE_SCALE_MS));
        return Math.max(MIN_TAIL_REMOVAL_MS, Math.min(MAX_TAIL_REMOVAL_MS, tail));
    }

    /**
     * Clamps a noise threshold into the valid NoiseSquelch control range.
     */
    private static float clampThreshold(float value)
    {
        if(value < NoiseSquelch.MINIMUM_NOISE_THRESHOLD)
        {
            return NoiseSquelch.MINIMUM_NOISE_THRESHOLD;
        }
        if(value > NoiseSquelch.MAXIMUM_NOISE_THRESHOLD)
        {
            return NoiseSquelch.MAXIMUM_NOISE_THRESHOLD;
        }
        return value;
    }

    /**
     * Returns the value at the given percentile (0-100) from a pre-sorted, non-empty list.
     */
    private static float percentile(List<Float> sorted, int percentile)
    {
        int index = Math.round((percentile / 100.0f) * (sorted.size() - 1));
        index = Math.max(0, Math.min(sorted.size() - 1, index));
        return sorted.get(index);
    }

    private static String fmt(float value)
    {
        return String.format("%.3f", value);
    }

    /**
     * Recommended noise squelch settings, in NoiseSquelch control units.
     * @param openThreshold recommended noise open threshold (squelch opens below this variance).
     * @param closeThreshold recommended noise close threshold (squelch closes above this variance).
     * @param hysteresisOpen recommended open hysteresis hold count (consecutive 10 ms buffers).
     * @param hysteresisClose recommended close hysteresis hold count (consecutive 10 ms buffers).
     * @param rationale human-readable explanation of why these settings were chosen (shown to the user).
     */
    public record Recommendation(float openThreshold, float closeThreshold, int hysteresisOpen,
            int hysteresisClose, String rationale) {}
}
