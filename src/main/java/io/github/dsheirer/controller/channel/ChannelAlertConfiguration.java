package io.github.dsheirer.controller.channel;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class ChannelAlertConfiguration {

    private boolean mInactivityAlertEnabled = false;
    private int mInactivityDurationThresholdMinutes = 10;
    private boolean mInactivityAutoRestartEnabled = false;

    private boolean mAiAudioMonitoringEnabled = false;
    private int mAiAudioMonitoringCheckInterval = 5;
    private boolean mAiAudioMonitoringWaitNewAudio = true;
    private int mAiAudioMonitoringAlertThreshold = 3;

    public ChannelAlertConfiguration() {
    }

    @JacksonXmlProperty(isAttribute = true, localName = "inactivityAlertEnabled")
    public boolean isInactivityAlertEnabled() {
        return mInactivityAlertEnabled;
    }

    public void setInactivityAlertEnabled(boolean inactivityAlertEnabled) {
        mInactivityAlertEnabled = inactivityAlertEnabled;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "inactivityDurationThresholdMinutes")
    public int getInactivityDurationThresholdMinutes() {
        return mInactivityDurationThresholdMinutes;
    }

    public void setInactivityDurationThresholdMinutes(int inactivityDurationThresholdMinutes) {
        mInactivityDurationThresholdMinutes = inactivityDurationThresholdMinutes;
    }

    /**
     * When enabled, the channel is automatically restarted (up to 2 attempts per inactivity episode)
     * when the inactivity threshold is exceeded, before alerting the user.
     */
    @JacksonXmlProperty(isAttribute = true, localName = "inactivityAutoRestartEnabled")
    public boolean isInactivityAutoRestartEnabled() {
        return mInactivityAutoRestartEnabled;
    }

    public void setInactivityAutoRestartEnabled(boolean inactivityAutoRestartEnabled) {
        mInactivityAutoRestartEnabled = inactivityAutoRestartEnabled;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "aiAudioMonitoringEnabled")
    public boolean isAiAudioMonitoringEnabled() {
        return mAiAudioMonitoringEnabled;
    }

    public void setAiAudioMonitoringEnabled(boolean aiAudioMonitoringEnabled) {
        mAiAudioMonitoringEnabled = aiAudioMonitoringEnabled;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "aiAudioMonitoringCheckInterval")
    public int getAiAudioMonitoringCheckInterval() {
        return mAiAudioMonitoringCheckInterval;
    }

    public void setAiAudioMonitoringCheckInterval(int aiAudioMonitoringCheckInterval) {
        mAiAudioMonitoringCheckInterval = aiAudioMonitoringCheckInterval;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "aiAudioMonitoringWaitNewAudio")
    public boolean isAiAudioMonitoringWaitNewAudio() {
        return mAiAudioMonitoringWaitNewAudio;
    }

    public void setAiAudioMonitoringWaitNewAudio(boolean aiAudioMonitoringWaitNewAudio) {
        mAiAudioMonitoringWaitNewAudio = aiAudioMonitoringWaitNewAudio;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "aiAudioMonitoringAlertThreshold")
    public int getAiAudioMonitoringAlertThreshold() {
        return mAiAudioMonitoringAlertThreshold;
    }

    public void setAiAudioMonitoringAlertThreshold(int aiAudioMonitoringAlertThreshold) {
        mAiAudioMonitoringAlertThreshold = aiAudioMonitoringAlertThreshold;
    }
}
