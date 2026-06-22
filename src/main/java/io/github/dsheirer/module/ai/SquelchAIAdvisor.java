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
 *     thresholds) and recommends open/close thresholds anchored to the measured noise floor.  This powers the
 *     manual "Calibrate Squelch" button.</li>
 * </ul>
 *
 * <h2>NBFM squelch calibration methodology</h2>
 * The {@code NoiseSquelch} measures the statistical <i>variance</i> of the high-pass filtered, demodulated audio in
 * 10 ms windows.  A clean signal has <b>low</b> variance and the squelch opens when variance falls <b>below</b> the
 * open threshold; ambient noise has <b>high</b> variance and the squelch closes when variance rises <b>above</b> the
 * close threshold (so {@code open <= close}).  Note the squelch <i>view</i> presents an inverted, x20-scaled value
 * (display = 10 - 20*variance), so on screen a strong signal reads <b>high</b> ("Noise (N)" near 10) and the idle
 * noise floor reads lower; raising the on-screen open line above the idle noise peak is equivalent to setting the
 * raw open threshold below the idle noise variance.
 * <p>
 * Calibration follows these rules:
 * <ol>
 *     <li><b>Noise-floor baseline:</b> from an idle (no-signal) capture, find the worst-case (most signal-like)
 *     noise excursion - the lowest noise variance - and treat it as the noise floor edge.</li>
 *     <li><b>Open threshold:</b> placed a margin <i>below</i> the noise floor edge so ambient noise never opens the
 *     squelch (only a genuinely cleaner signal can).</li>
 *     <li><b>Close threshold:</b> placed a small margin <i>above</i> the open threshold (a Schmitt-trigger / amplitude
 *     hysteresis) so an established transmission can fade or degrade slightly without prematurely muting.</li>
 *     <li><b>Temporal hysteresis (10 ms units):</b> a low, non-zero open hold (~3 = 30 ms) rejects instantaneous
 *     static spikes without clipping the first syllables; the close hold is kept low (~6 = 60 ms) to cut the
 *     squelch-tail noise burst quickly, but is increased when the capture shows rapid fluctuation (picket-fencing)
 *     to bridge temporary audio dropouts.</li>
 * </ol>
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
     * Minimum spread between the low (signal) and high (noise-floor) variance percentiles before a capture can be
     * considered to contain a separate signal cluster at all.  Below this the capture is treated as noise-floor only
     * and a conservative below-the-floor placement is used.
     */
    private static final float MINIMUM_CLUSTER_SEPARATION = 0.03f;

    /**
     * Fraction of the total p10..p90 spread that the largest interior decile gap (the "valley" between a signal
     * cluster and the noise floor) must reach for a capture to be treated as bimodal signal+noise rather than
     * unimodal noise.  A unimodal noise capture spreads its range roughly evenly across the deciles (each gap
     * ~1/8 of the range), so requiring one dominant valley reliably rejects noisy-but-idle channels - which is
     * essential, because mistaking noise spread for a signal cluster drops the thresholds inside the noise floor
     * and makes the squelch chatter (rapid open/close, stuttering audio).
     */
    private static final float BIMODAL_VALLEY_FRACTION = 0.40f;

    //Noise-floor anchored placement (used when the capture is noise-floor only, i.e. an idle channel).  The noise
    //floor edge is the worst-case (lowest-variance, most signal-like) noise excursion; the open threshold is placed a
    //margin below it so ambient noise never opens the squelch, and the close threshold a small margin above it for an
    //amplitude-hysteresis (Schmitt-trigger) gap.
    private static final int NOISE_FLOOR_EDGE_PERCENTILE = 5;
    private static final float OPEN_FRACTION_BELOW_FLOOR = 0.90f;   //open = 90% of the noise floor edge (10% below)
    private static final float CLOSE_FRACTION_ABOVE_FLOOR = 1.05f;  //close = 105% of the noise floor edge (5% above)

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

        mLog.debug("SquelchAI: avg noise={}dBFS, peak={}dBFS, recommended threshold={}dBFS",
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

        //Only place thresholds "between the clusters" when the capture is genuinely bimodal (a signal cluster AND a
        //noise cluster separated by a valley).  An idle channel's noise still has a p10..p90 spread, so keying off
        //spread alone (the old behaviour) dropped the thresholds inside the noise floor on idle channels and made the
        //squelch chatter.  A unimodal noise capture is anchored below the floor instead.
        boolean bimodal = isBimodal(sorted, low, high);

        float open, close;

        if(bimodal)
        {
            //Both signal and noise observed - place thresholds in the gap between the clusters: open near the clean
            //signal level, close toward the noise floor, so real signals open the squelch while noise keeps it closed.
            open = low + (0.25f * separation);
            close = low + (0.55f * separation);
        }
        else
        {
            //Noise-floor only (idle channel) - anchor to the noise floor: find the worst-case (lowest-variance, most
            //signal-like) noise excursion and place the open threshold a margin BELOW it so ambient noise can never
            //open the squelch, with the close threshold a small margin above it for amplitude hysteresis.  This is the
            //raw-variance equivalent of "set the on-screen open line just above the highest idle noise peak".
            float noiseFloorEdge = percentile(sorted, NOISE_FLOOR_EDGE_PERCENTILE);
            open = noiseFloorEdge * OPEN_FRACTION_BELOW_FLOOR;
            close = noiseFloorEdge * CLOSE_FRACTION_ABOVE_FLOOR;
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

        //Temporal hysteresis (consecutive-buffer hold counts, units of 10 ms).
        //Open hold: low but non-zero (~30 ms) rejects instantaneous static spikes without clipping the first
        //syllables of a valid transmission.  Close hold: kept low (~60 ms) to cut the squelch-tail noise burst
        //quickly, but increased when the capture shows rapid fluctuation (picket-fencing) so brief dropouts don't
        //prematurely mute an active signal.  Clamped to the squelch view's slider ranges (open 1-6, close = open+0..5).
        int hysteresisOpen = 3;
        int hysteresisClose = 6;

        //Picket-fencing / rapid-fading proxy: how variable the captured variance is relative to its level.  A wide,
        //fluctuating distribution indicates a fading signal that needs a longer close hold to bridge dropouts.
        float relativeSpread = (median > 0.0001f) ? (separation / median) : 0f;

        if(relativeSpread >= 1.0f)
        {
            hysteresisClose = 9;   //heavy fluctuation - bridge dropouts aggressively
        }
        else if(relativeSpread >= 0.5f)
        {
            hysteresisClose = 7;   //moderate fluctuation - hold a little longer before muting
        }
        else if(separation >= 0.10f)
        {
            //Clean, well-separated and stable - react a touch faster.
            hysteresisOpen = 2;
            hysteresisClose = 4;
        }

        //Clamp to the squelch control ranges and the view's open+(0..5) close relationship.
        hysteresisOpen = Math.max(1, Math.min(6, hysteresisOpen));
        hysteresisClose = Math.max(hysteresisOpen, Math.min(hysteresisOpen + 5, hysteresisClose));

        String rationale;
        if(bimodal)
        {
            rationale = String.format("Saw both a clean-signal level (~%s) and a noise-floor level (~%s); set " +
                    "open/close in the gap between them so real signals open the squelch while noise keeps it closed.",
                    fmt(low), fmt(high));
        }
        else
        {
            rationale = String.format("Saw mostly background noise (noise floor ~%s); set the open threshold just " +
                    "below the noise floor and the close threshold just above it, so ambient noise can't open the " +
                    "squelch while a genuinely cleaner signal still can.", fmt(percentile(sorted, NOISE_FLOOR_EDGE_PERCENTILE)));
        }

        String hysteresisReason = (relativeSpread >= 0.5f)
                ? "hold the gate open longer to bridge dropouts on this fading (picket-fencing) signal"
                : (separation >= 0.10f)
                        ? "react quickly on this clean, well-separated signal"
                        : "reject static spikes on open while cutting the squelch tail quickly on close";
        rationale += String.format("  Set hysteresis to open=%d, close=%d (x10 ms) to %s.",
                hysteresisOpen, hysteresisClose, hysteresisReason);

        mLog.debug("SquelchAI calibration: samples={}, low(p10)={}, median={}, high(p90)={}, mode={} -> open={}, " +
                        "close={}, hysteresis open={}, close={}", mVarianceSamples.size(), fmt(low), fmt(median),
                fmt(high), bimodal ? "signal+noise" : "noise-floor", fmt(open), fmt(close), hysteresisOpen,
                hysteresisClose);

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
    /**
     * Decides whether a sorted variance capture is bimodal (a low-variance signal cluster AND a high-variance noise
     * cluster separated by a valley) versus unimodal (noise floor only).  Placing open/close "between the clusters"
     * is only correct for a genuinely bimodal capture; doing it on unimodal noise drops the thresholds inside the
     * noise distribution so ambient noise constantly crosses them and the squelch chatters (the stuttering bug).
     *
     * Scale-free test: sample the distribution at deciles (p10..p90) and measure the largest gap between adjacent
     * interior deciles, ignoring the outer p10-p20 / p80-p90 tails so a skewed-but-unimodal noise tail isn't
     * mistaken for a valley.  A unimodal distribution spreads its range roughly evenly (each gap ~1/8 of the range);
     * a bimodal distribution concentrates it into one valley.  Require that valley to be a large fraction of the
     * total p10..p90 spread.
     *
     * @param sorted ascending variance samples.
     * @param low p10 of the capture.
     * @param high p90 of the capture.
     * @return true when the capture looks like signal+noise, false when it looks like noise floor only.
     */
    private static boolean isBimodal(List<Float> sorted, float low, float high)
    {
        float separation = high - low;

        if(separation < MINIMUM_CLUSTER_SEPARATION)
        {
            return false;   //Too little spread to contain two separate clusters.
        }

        float[] deciles = new float[9];     //p10, p20, ... p90
        for(int i = 0; i < deciles.length; i++)
        {
            deciles[i] = percentile(sorted, (i + 1) * 10);
        }

        //Largest gap among the interior deciles p20..p80 - the valley between clusters lives here, away from the
        //outer tails which can be uneven even for unimodal noise.
        float maxInteriorGap = 0f;
        for(int i = 2; i <= 7; i++)
        {
            maxInteriorGap = Math.max(maxInteriorGap, deciles[i] - deciles[i - 1]);
        }

        return maxInteriorGap >= (BIMODAL_VALLEY_FRACTION * separation);
    }

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
