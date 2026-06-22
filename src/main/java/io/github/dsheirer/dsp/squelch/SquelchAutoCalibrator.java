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
 * Drives <b>scheduled</b> squelch calibration for a channel by consuming the live {@link NoiseSquelchState}
 * stream.  Squelch calibration is manual by default (the per-channel "Calibrate" button); this calibrator only
 * acts when the user opts in to a schedule in preferences.  When the schedule is enabled, it captures the
 * channel's noise variance for a few seconds and applies recommended open/close thresholds (and a tail-removal
 * duration) once per configured interval, never more often than every 12 hours, and never immediately on
 * channel start (a full interval must elapse first).
 *
 * Gated by an enabled supplier (the scheduled-calibration preference) and a manual-lock supplier (set when the
 * user adjusts the squelch by hand): when scheduling is off it does nothing, and when locked no changes are
 * applied, so manual settings are always respected.
 */
public class SquelchAutoCalibrator
{
    private static final Logger mLog = LoggerFactory.getLogger(SquelchAutoCalibrator.class);

    private static final long CAPTURE_MS = 5_000;
    //Never auto-calibrate more often than every 12 hours, to conserve CPU and avoid churning the user's
    //squelch thresholds.  The configured interval (from preferences) is clamped up to this floor.
    private static final long MINIMUM_INTERVAL_MS = 12L * 60L * 60L * 1_000L;
    private static final float CHANGE_DEAD_BAND = 0.02f; //Skip a new calibration that barely differs from the current one.

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

    private enum Phase {MONITOR, CAPTURE}

    private final String mChannelName;
    private final BooleanSupplier mEnabledSupplier;
    private final BooleanSupplier mLockedSupplier;
    private final java.util.function.LongSupplier mIntervalMsSupplier;
    private final ThresholdApplier mThresholdApplier;
    private final TailApplier mTailApplier;

    private final SquelchAIAdvisor mAdvisor = new SquelchAIAdvisor();
    private Phase mPhase = Phase.MONITOR;
    private long mPhaseStartMs = -1;
    private float mCurrentOpen = NoiseSquelch.DEFAULT_NOISE_OPEN_THRESHOLD;
    private float mCurrentClose = NoiseSquelch.DEFAULT_NOISE_CLOSE_THRESHOLD;

    /**
     * Constructs an instance.
     * @param channelName for logging.
     * @param enabledSupplier returns true when SCHEDULED automatic squelch calibration is enabled.  When false,
     *                        calibration is manual-only (the per-channel Calibrate button) and this calibrator
     *                        does nothing.
     * @param lockedSupplier returns true when the user has manually adjusted the squelch (lock changes out).
     * @param intervalMsSupplier scheduled re-calibration interval in milliseconds (floored at 12 hours).
     * @param thresholdApplier applies recommended open/close thresholds.
     * @param tailApplier applies a recommended tail-removal duration.
     */
    public SquelchAutoCalibrator(String channelName, BooleanSupplier enabledSupplier,
                                 BooleanSupplier lockedSupplier, java.util.function.LongSupplier intervalMsSupplier,
                                 ThresholdApplier thresholdApplier, TailApplier tailApplier)
    {
        mChannelName = channelName;
        mEnabledSupplier = enabledSupplier;
        mLockedSupplier = lockedSupplier;
        mIntervalMsSupplier = intervalMsSupplier;
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
            //Scheduled auto-calibration is disabled - calibration is manual-only via the Calibrate button.
            //Reset so that if the user later enables the schedule, a fresh full interval elapses first.
            mPhase = Phase.MONITOR;
            mPhaseStartMs = -1;
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
            case MONITOR:
                //Wait a full interval (>= 12 hours) before the first and each subsequent scheduled calibration,
                //so a freshly started channel is never auto-calibrated immediately.
                if(elapsed >= intervalMs())
                {
                    beginCapture(now);
                }
                break;

            case CAPTURE:
                mAdvisor.recordVarianceSample(state.noise());
                if(elapsed >= CAPTURE_MS)
                {
                    applyCapture();
                    enterMonitor(now);
                }
                break;
        }
    }

    private long intervalMs()
    {
        long ms = (mIntervalMsSupplier != null) ? mIntervalMsSupplier.getAsLong() : MINIMUM_INTERVAL_MS;
        return Math.max(MINIMUM_INTERVAL_MS, ms);
    }

    private void beginCapture(long now)
    {
        mAdvisor.resetCalibration();
        mPhase = Phase.CAPTURE;
        mPhaseStartMs = now;
    }

    private void enterMonitor(long now)
    {
        mPhase = Phase.MONITOR;
        mPhaseStartMs = now;
    }

    /**
     * Computes a recommendation from the captured samples and applies it, unless the channel is locked by a
     * manual adjustment or the recommendation barely differs from the currently applied thresholds.
     */
    private void applyCapture()
    {
        SquelchAIAdvisor.Recommendation recommendation = mAdvisor.calibrate();

        if(recommendation == null || mLockedSupplier.getAsBoolean())
        {
            return;
        }

        float open = clamp(recommendation.openThreshold());
        float close = clamp(recommendation.closeThreshold());

        if(close < open)
        {
            float swap = open;
            open = close;
            close = swap;
        }

        //Skip applying when the new calibration is essentially the same as the current one, to avoid an
        //unnecessary threshold change (and squelch disruption) on each scheduled run.
        if(Math.abs(open - mCurrentOpen) < CHANGE_DEAD_BAND && Math.abs(close - mCurrentClose) < CHANGE_DEAD_BAND)
        {
            return;
        }

        mCurrentOpen = open;
        mCurrentClose = close;
        mThresholdApplier.apply(open, close);

        if(mTailApplier != null)
        {
            mTailApplier.apply(mAdvisor.getRecommendedTailRemovalMs());
        }

        mLog.info("Auto-squelch (scheduled) calibrated for channel '{}': open={}, close={}",
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
