/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

import io.github.dsheirer.sample.Listener;

/**
 * Interface for a decoder to expose access to noise squelch controls and status registration endpoint(s).
 */
public interface INoiseSquelchController
{
    /**
     * Registers the listener to receive noise squelch states every 10 milliseconds for UI display.
     * @param listener to receive states or pass null to de-register a listener.
     */
    void setNoiseSquelchStateListener(Listener<NoiseSquelchState> listener);

    /**
     * Sets the open and close noise thresholds in range 0.0 to 1.0, recommend: 0.1 open and 0.2 close
     * @param open for the open noise variance calculation.
     * @param close for the close noise variance calculation.
     */
    void setNoiseThreshold(float open, float close);

    /**
     * Sets the hysteresis threshold in units of 10 milliseconds
     * @param open in range 1-10, recommend: 4
     * @param close in range 1-10, recommend: 6
     */
    void setHysteresisThreshold(int open, int close);

    /**
     * Manual override of the squelch control to set an always un-squelch state (true) or automatic control (false).
     * @param override (true) or (false) to turn off squelch override.
     */
    void setSquelchOverride(boolean override);

    /**
     * Marks this channel's squelch settings as having been manually adjusted by the user, locking out the
     * automatic squelch calibrator so it leaves the manual thresholds unchanged.  Default no-op for
     * controllers that do not support automatic calibration.
     */
    default void markSquelchManuallyAdjusted() {}

    /**
     * Clears the manual-adjustment lock, re-enabling automatic squelch calibration for this channel.
     * Default no-op.
     */
    default void clearSquelchManualAdjustment() {}

    /**
     * Indicates whether the user has manually adjusted this channel's squelch settings (locking out the
     * automatic squelch calibrator).  Default false.
     * @return true when manually adjusted.
     */
    default boolean isSquelchManuallyAdjusted() { return false; }

    /**
     * Enables or disables adaptive noise-floor tracking, which raises the effective open threshold toward (but
     * never above) the close threshold based on the measured noise floor so weak signals can open the squelch.
     * Default no-op for controllers that do not support it.
     * @param enabled true to enable adaptive tracking.
     */
    default void setAdaptiveSquelch(boolean enabled) {}

    /**
     * Indicates whether adaptive noise-floor tracking is enabled for this channel.  Default false.
     * @return true when adaptive tracking is enabled.
     */
    default boolean isAdaptiveSquelch() { return false; }

    /**
     * Channel name for this controller, used to key per-channel squelch calibration history (persistence
     * and learning).  Default null for controllers not associated with a named channel.
     * @return channel name, or null when unknown.
     */
    default String getChannelName() { return null; }
}
