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
    private static final String PREFERENCE_KEY_AUTO_START = "auto.start.enabled";
    private static final String PREFERENCE_KEY_WATCHDOG_ENABLED = "watchdog.enabled";
    private static final String PREFERENCE_KEY_REMOTE_ACCESS_OPTIMIZATION = "remote.access.optimization";

    private static final String PREFERENCE_KEY_AUDIO_TWO_TONE_DETECT = "audio.two.tone.detect.enabled";
    private static final String PREFERENCE_KEY_AUDIO_ALIAS_DETECT = "audio.alias.detect.enabled";
    private static final String PREFERENCE_KEY_TRUNKED_AUTO_LABEL = "trunked.auto.label.enabled";

    private final static Logger mLog = LoggerFactory.getLogger(ApplicationPreference.class);
    private Preferences mPreferences = Preferences.userNodeForPackage(ApplicationPreference.class);
    private Integer mChannelAutoStartTimeout;
    private Integer mAllocatedMemory;
    private Boolean mAutomaticDiagnosticMonitoring;
    private Boolean mUsbMonitorInstalled;
    private Boolean mUsbMonitorPrompted;
    private Boolean mAutoStartEnabled;
    private Boolean mWatchdogEnabled;
    private Boolean mRemoteAccessOptimization;
    private Boolean mAudioTwoToneDetectEnabled;
    private Boolean mAudioAliasDetectEnabled;
    private Boolean mTrunkedAutoLabelEnabled;

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
     * Indicates if trunked-system System/Site labels should be auto-filled from decoded identifiers.  This
     * only ever populates labels the user left blank and never overwrites a manually-set value.
     * @return enabled (default true).
     */
    public boolean isTrunkedSystemAutoLabelEnabled()
    {
        if(mTrunkedAutoLabelEnabled == null)
        {
            mTrunkedAutoLabelEnabled = mPreferences.getBoolean(PREFERENCE_KEY_TRUNKED_AUTO_LABEL, true);
        }

        return mTrunkedAutoLabelEnabled;
    }

    /**
     * Sets the enabled state for trunked-system System/Site label auto-fill.
     * @param enabled true to auto-fill empty labels from decoded identifiers.
     */
    public void setTrunkedSystemAutoLabelEnabled(boolean enabled)
    {
        mTrunkedAutoLabelEnabled = enabled;
        mPreferences.putBoolean(PREFERENCE_KEY_TRUNKED_AUTO_LABEL, enabled);
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
            
            // Also directly update scripts
            MemoryScriptUpdater.updateMemoryLimit(gb);
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

    public boolean isAutoStartEnabled()
    {
        if(mAutoStartEnabled == null)
        {
            mAutoStartEnabled = mPreferences.getBoolean(PREFERENCE_KEY_AUTO_START, false);
        }
        return mAutoStartEnabled;
    }

    public void setAutoStartEnabled(boolean enabled)
    {
        mAutoStartEnabled = enabled;
        mPreferences.putBoolean(PREFERENCE_KEY_AUTO_START, enabled);
        notifyPreferenceUpdated();
    }

    public boolean isWatchdogEnabled()
    {
        if(mWatchdogEnabled == null)
        {
            //Default enabled: unattended systems should restart automatically after a crash.  The
            //watchdog includes crash-loop protection and is disabled by a graceful exit marker.
            mWatchdogEnabled = mPreferences.getBoolean(PREFERENCE_KEY_WATCHDOG_ENABLED, true);
        }
        return mWatchdogEnabled;
    }

    public void setWatchdogEnabled(boolean enabled)
    {
        mWatchdogEnabled = enabled;
        mPreferences.putBoolean(PREFERENCE_KEY_WATCHDOG_ENABLED, enabled);
        notifyPreferenceUpdated();
    }

    public boolean isRemoteAccessOptimization()
    {
        if(mRemoteAccessOptimization == null)
        {
            mRemoteAccessOptimization = mPreferences.getBoolean(PREFERENCE_KEY_REMOTE_ACCESS_OPTIMIZATION, true);
        }
        return mRemoteAccessOptimization;
    }

    public void setRemoteAccessOptimization(boolean enabled)
    {
        mRemoteAccessOptimization = enabled;
        mPreferences.putBoolean(PREFERENCE_KEY_REMOTE_ACCESS_OPTIMIZATION, enabled);
        notifyPreferenceUpdated();
    }

    public boolean isAudioTwoToneDetectEnabled()
    {
        if(mAudioTwoToneDetectEnabled == null)
        {
            mAudioTwoToneDetectEnabled = mPreferences.getBoolean(PREFERENCE_KEY_AUDIO_TWO_TONE_DETECT, true);
        }
        return mAudioTwoToneDetectEnabled;
    }

    public void setAudioTwoToneDetectEnabled(boolean enabled)
    {
        mAudioTwoToneDetectEnabled = enabled;
        mPreferences.putBoolean(PREFERENCE_KEY_AUDIO_TWO_TONE_DETECT, enabled);
        notifyPreferenceUpdated();
    }

    public boolean isAudioAliasDetectEnabled()
    {
        if(mAudioAliasDetectEnabled == null)
        {
            mAudioAliasDetectEnabled = mPreferences.getBoolean(PREFERENCE_KEY_AUDIO_ALIAS_DETECT, true);
        }
        return mAudioAliasDetectEnabled;
    }

    public void setAudioAliasDetectEnabled(boolean enabled)
    {
        mAudioAliasDetectEnabled = enabled;
        mPreferences.putBoolean(PREFERENCE_KEY_AUDIO_ALIAS_DETECT, enabled);
        notifyPreferenceUpdated();
    }
}