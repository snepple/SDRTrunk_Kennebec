/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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
package io.github.dsheirer.dsp.squelch;

import io.github.dsheirer.module.ai.SquelchAIAdvisor;
import java.util.function.BooleanSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Drives automatic squelch calibration for a channel by consuming the live {@link NoiseSquelchState} stream:
 * <ul>
 *     <li><b>Auto-calibrate on channel start</b> - after a short warm-up, captures the channel's noise
 *     variance for a few seconds and applies recommended open/close thresholds and a tail-removal duration.</li>
 *     <li><b>Continuous drift correction</b> - periodically re-captures and gently nudges the thresholds
 *     toward the new recommendation (with a dead-band to avoid jitter) so the squelch tracks slow changes in
 *     the RF environment.</li>
 * </ul>
 *
 * Both behaviours are gated by an enabled supplier (the Squelch Advisor preference) and a manual-lock supplier
 * (set when the user adjusts the squelch by hand): when locked, captured statistics are still observed but no
 * changes are applied, so manual settings are always respected.
 */
public class SquelchAutoCalibrator
{
    private static final Logger mLog = LoggerFactory.getLogger(SquelchAutoCalibrator.class);

    private static final long WARMUP_MS = 2_000;
    private static final long CAPTURE_MS = 5_000;
    private static final long DRIFT_INTERVAL_MS = 10L * 60L * 1_000L; //Re-check drift every ten minutes.
    private static final float DRIFT_STEP = 0.34f;   //Move ~1/3 of the way toward the new recommendation.
    private static final float DRIFT_DEAD_BAND = 0.02f; //Ignore changes smaller than this to avoid jitter.

    /**
     * Applies recommended noise open/close thresholds to the live squelch and persisted configuration.
     */
    public interface ThresholdApplier
    {
        void apply(float open, float close);
    }

    /**
     * Applies a recommended squelch tail-removal duration (milliseconds).
     */
    public interface TailApplier
    {
        void apply(int tailRemovalMs);
    }

    private enum Phase {WARMUP, INITIAL_CAPTURE, MONITOR, DRIFT_CAPTURE}

    private final String mChannelName;
    private final BooleanSupplier mEnabledSupplier;
    private final BooleanSupplier mLockedSupplier;
    private final ThresholdApplier mThresholdApplier;
    private final TailApplier mTailApplier;

    private final SquelchAIAdvisor mAdvisor = new SquelchAIAdvisor();
    private Phase mPhase = Phase.WARMUP;
    private long mPhaseStartMs = -1;
    private float mCurrentOpen = NoiseSquelch.DEFAULT_NOISE_OPEN_THRESHOLD;
    private float mCurrentClose = NoiseSquelch.DEFAULT_NOISE_CLOSE_THRESHOLD;

    /**
     * Constructs an instance.
     * @param channelName for logging.
     * @param enabledSupplier returns true when automatic squelch calibration is enabled (Squelch Advisor).
     * @param lockedSupplier returns true when the user has manually adjusted the squelch (lock changes out).
     * @param thresholdApplier applies recommended open/close thresholds.
     * @param tailApplier applies a recommended tail-removal duration.
     */
    public SquelchAutoCalibrator(String channelName, BooleanSupplier enabledSupplier,
                                 BooleanSupplier lockedSupplier, ThresholdApplier thresholdApplier,
                                 TailApplier tailApplier)
    {
        mChannelName = channelName;
        mEnabledSupplier = enabledSupplier;
        mLockedSupplier = lockedSupplier;
        mThresholdApplier = thresholdApplier;
        mTailApplier = tailApplier;
    }

    /**
     * Processes one live noise squelch state.  Cheap and safe to call from the decode thread.
     * @param state the latest squelch state (provides the current noise variance).
     */
    public void process(NoiseSquelchState state)
    {
        if(state == null || !mEnabledSupplier.getAsBoolean())
        {
            return;
        }

        long now = System.currentTimeMillis();

        if(mPhaseStartMs < 0)
        {
            mPhaseStartMs = now;
        }

        long elapsed = now - mPhaseStartMs;

        switch(mPhase)
        {
            case WARMUP:
                if(elapsed >= WARMUP_MS)
                {
                    beginCapture(now, Phase.INITIAL_CAPTURE);
                }
                break;

            case INITIAL_CAPTURE:
                mAdvisor.recordVarianceSample(state.noise());
                if(elapsed >= CAPTURE_MS)
                {
                    applyCapture(true);
                    enterMonitor(now);
                }
                break;

            case MONITOR:
                if(elapsed >= DRIFT_INTERVAL_MS)
                {
                    beginCapture(now, Phase.DRIFT_CAPTURE);
                }
                break;

            case DRIFT_CAPTURE:
                mAdvisor.recordVarianceSample(state.noise());
                if(elapsed >= CAPTURE_MS)
                {
                    applyCapture(false);
                    enterMonitor(now);
                }
                break;
        }
    }

    private void beginCapture(long now, Phase capturePhase)
    {
        mAdvisor.resetCalibration();
        mPhase = capturePhase;
        mPhaseStartMs = now;
    }

    private void enterMonitor(long now)
    {
        mPhase = Phase.MONITOR;
        mPhaseStartMs = now;
    }

    /**
     * Computes a recommendation from the captured samples and applies it, unless the channel is locked by a
     * manual adjustment.  For the initial capture the recommendation is applied directly; for drift captures
     * the current thresholds are nudged partway toward the recommendation with a dead-band.
     * @param initial true for the on-start calibration, false for a drift correction.
     */
    private void applyCapture(boolean initial)
    {
        SquelchAIAdvisor.Recommendation recommendation = mAdvisor.calibrate();

        if(recommendation == null || mLockedSupplier.getAsBoolean())
        {
            return;
        }

        float open;
        float close;

        if(initial)
        {
            open = recommendation.openThreshold();
            close = recommendation.closeThreshold();
        }
        else
        {
            //Drift: only move when the change is meaningful, then step partway toward the target.
            if(Math.abs(recommendation.openThreshold() - mCurrentOpen) < DRIFT_DEAD_BAND &&
               Math.abs(recommendation.closeThreshold() - mCurrentClose) < DRIFT_DEAD_BAND)
            {
                return;
            }

            open = mCurrentOpen + (DRIFT_STEP * (recommendation.openThreshold() - mCurrentOpen));
            close = mCurrentClose + (DRIFT_STEP * (recommendation.closeThreshold() - mCurrentClose));
        }

        //Clamp and order before applying.
        open = clamp(open);
        close = clamp(close);
        if(close < open)
        {
            float swap = open;
            open = close;
            close = swap;
        }

        mCurrentOpen = open;
        mCurrentClose = close;
        mThresholdApplier.apply(open, close);

        if(initial && mTailApplier != null)
        {
            mTailApplier.apply(mAdvisor.getRecommendedTailRemovalMs());
        }

        mLog.info("Auto-squelch {} for channel '{}': open={}, close={}", initial ? "calibrated" : "drift-adjusted",
                mChannelName, String.format("%.3f", open), String.format("%.3f", close));
    }

    private static float clamp(float value)
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
}
