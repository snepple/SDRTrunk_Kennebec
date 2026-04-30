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

package io.github.dsheirer.preference.application;

import io.github.dsheirer.preference.Preference;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.sample.Listener;
import java.util.prefs.Preferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * General/Miscellaneous preferences
 */
public class ApplicationPreference extends Preference
{
    private static final String PREFERENCE_KEY_CHANNEL_AUTO_DIAGNOSTIC_MONITORING = "automatic.diagnostic.monitoring";
    private static final String PREFERENCE_KEY_CHANNEL_AUTO_START_TIMEOUT = "channel.auto.start.timeout";
    private static final String PREFERENCE_KEY_ALLOCATED_MEMORY = "allocated.memory";
    private static final String PREFERENCE_KEY_USB_MONITOR_INSTALLED = "usb.monitor.installed";
    private static final String PREFERENCE_KEY_USB_MONITOR_PROMPTED = "usb.monitor.prompted";

    private final static Logger mLog = LoggerFactory.getLogger(ApplicationPreference.class);
    private Preferences mPreferences = Preferences.userNodeForPackage(ApplicationPreference.class);
    private Integer mChannelAutoStartTimeout;
    private Integer mAllocatedMemory;
    private Boolean mAutomaticDiagnosticMonitoring;
    private Boolean mUsbMonitorInstalled;
    private Boolean mUsbMonitorPrompted;

    /**
     * Constructs an instance
     * @param updateListener to receive notifications that a preference has been updated
     */
    public ApplicationPreference(Listener<PreferenceType> updateListener)
    {
        super(updateListener);
    }

    @Override
    public PreferenceType getPreferenceType()
    {
        return PreferenceType.APPLICATION;
    }


    /**
     * Channel auto-start timeout.  This is the countdown in seconds to allow the user to cancel the channel auto-start.
     * @return timeout in seconds.
     */
    public int getChannelAutoStartTimeout()
    {
        if(mChannelAutoStartTimeout == null)
        {
            mChannelAutoStartTimeout = mPreferences.getInt(PREFERENCE_KEY_CHANNEL_AUTO_START_TIMEOUT, 10);
        }

        return mChannelAutoStartTimeout;
    }

    /**
     * Sets the channel auto-start timeout seconds value.
     * @param timeout in seconds.
     */
    public void setChannelAutoStartTimeout(int timeout)
    {
        mChannelAutoStartTimeout = timeout;
        mPreferences.putInt(PREFERENCE_KEY_CHANNEL_AUTO_START_TIMEOUT, timeout);
        notifyPreferenceUpdated();
    }

    /**
     * Indicates if automatic diagnostic monitoring is enabled.
     * @return enabled.
     */
    public boolean isAutomaticDiagnosticMonitoring()
    {
        if(mAutomaticDiagnosticMonitoring == null)
        {
            mAutomaticDiagnosticMonitoring = mPreferences.getBoolean(PREFERENCE_KEY_CHANNEL_AUTO_DIAGNOSTIC_MONITORING, true);
        }

        return mAutomaticDiagnosticMonitoring;
    }

    /**
     * Sets the enabled state for automatic diagnostic monitoring.
     * @param enabled true to turn on monitoring.
     */
    public void setAutomaticDiagnosticMonitoring(boolean enabled)
    {
        mAutomaticDiagnosticMonitoring = enabled;
        mPreferences.putBoolean(PREFERENCE_KEY_CHANNEL_AUTO_DIAGNOSTIC_MONITORING, enabled);
        notifyPreferenceUpdated();
    }
    /**
     * Gets the allocated memory in GB
     * @return memory in GB.
     */
    public int getAllocatedMemory()
    {
        if(mAllocatedMemory == null)
        {
            mAllocatedMemory = mPreferences.getInt(PREFERENCE_KEY_ALLOCATED_MEMORY, 6);
        }

        return mAllocatedMemory;
    }

    /**
     * Sets the allocated memory in GB and writes to SDRTrunk.memory
     * @param gb memory in GB.
     */
    public void setAllocatedMemory(int gb)
    {
        mAllocatedMemory = gb;
        mPreferences.putInt(PREFERENCE_KEY_ALLOCATED_MEMORY, gb);

        try
        {
            java.nio.file.Path memoryFile = java.nio.file.Paths.get(System.getProperty("user.home"), "SDRTrunk", "SDRTrunk.memory");
            if(!java.nio.file.Files.exists(memoryFile.getParent())) {
                java.nio.file.Files.createDirectories(memoryFile.getParent());
            }
            java.nio.file.Files.writeString(memoryFile, String.valueOf(gb));
        }
        catch(java.io.IOException e)
        {
            mLog.error("Error writing SDRTrunk.memory file", e);
        }

        notifyPreferenceUpdated();
    }

    /**
     * Indicates if the USB Monitor script is installed.
     * @return true if installed.
     */
    public boolean isUsbMonitorInstalled()
    {
        if(mUsbMonitorInstalled == null)
        {
            mUsbMonitorInstalled = mPreferences.getBoolean(PREFERENCE_KEY_USB_MONITOR_INSTALLED, false);
        }
        return mUsbMonitorInstalled;
    }

    /**
     * Sets the installed state for the USB Monitor script.
     * @param installed true to mark as installed.
     */
    public void setUsbMonitorInstalled(boolean installed)
    {
        mUsbMonitorInstalled = installed;
        mPreferences.putBoolean(PREFERENCE_KEY_USB_MONITOR_INSTALLED, installed);
        notifyPreferenceUpdated();
    }

    /**
     * Indicates if the user has been prompted to install the USB Monitor script.
     * @return true if prompted.
     */
    public boolean isUsbMonitorPrompted()
    {
        if(mUsbMonitorPrompted == null)
        {
            mUsbMonitorPrompted = mPreferences.getBoolean(PREFERENCE_KEY_USB_MONITOR_PROMPTED, false);
        }
        return mUsbMonitorPrompted;
    }

    public void setUsbMonitorPrompted(boolean prompted)
    {
        mUsbMonitorPrompted = prompted;
        mPreferences.putBoolean(PREFERENCE_KEY_USB_MONITOR_PROMPTED, prompted);
        notifyPreferenceUpdated();
    }
}
