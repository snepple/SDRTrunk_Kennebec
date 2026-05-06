content = """package io.github.dsheirer.preference.notification;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class NotificationRecipient {

    public enum DeliveryMethod {
        EMAIL, TELEGRAM
    }

    private DeliveryMethod mDeliveryMethod = DeliveryMethod.EMAIL;
    private String mDestination = "";
    private boolean mChannelInactivityEnabled = false;
    private boolean mAiAudioMonitoringEnabled = false;
    private boolean mHardwareAlertEnabled = false;
    private boolean mSignalAlertEnabled = false;
    private boolean mSystemAlertEnabled = false;
    private boolean mIntegrationAlertEnabled = false;

    public NotificationRecipient() {
    }

    @JacksonXmlProperty(isAttribute = true, localName = "deliveryMethod")
    public DeliveryMethod getDeliveryMethod() {
        return mDeliveryMethod;
    }

    public void setDeliveryMethod(DeliveryMethod deliveryMethod) {
        mDeliveryMethod = deliveryMethod;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "destination")
    public String getDestination() {
        return mDestination;
    }

    public void setDestination(String destination) {
        mDestination = destination;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "channelInactivityEnabled")
    public boolean isChannelInactivityEnabled() {
        return mChannelInactivityEnabled;
    }

    public void setChannelInactivityEnabled(boolean channelInactivityEnabled) {
        mChannelInactivityEnabled = channelInactivityEnabled;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "aiAudioMonitoringEnabled")
    public boolean isAiAudioMonitoringEnabled() {
        return mAiAudioMonitoringEnabled;
    }

    public void setAiAudioMonitoringEnabled(boolean aiAudioMonitoringEnabled) {
        mAiAudioMonitoringEnabled = aiAudioMonitoringEnabled;
    }


    @JacksonXmlProperty(isAttribute = true, localName = "hardwareAlertEnabled")
    public boolean isHardwareAlertEnabled() {
        return mHardwareAlertEnabled;
    }

    public void setHardwareAlertEnabled(boolean hardwareAlertEnabled) {
        mHardwareAlertEnabled = hardwareAlertEnabled;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "signalAlertEnabled")
    public boolean isSignalAlertEnabled() {
        return mSignalAlertEnabled;
    }

    public void setSignalAlertEnabled(boolean signalAlertEnabled) {
        mSignalAlertEnabled = signalAlertEnabled;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "systemAlertEnabled")
    public boolean isSystemAlertEnabled() {
        return mSystemAlertEnabled;
    }

    public void setSystemAlertEnabled(boolean systemAlertEnabled) {
        mSystemAlertEnabled = systemAlertEnabled;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "integrationAlertEnabled")
    public boolean isIntegrationAlertEnabled() {
        return mIntegrationAlertEnabled;
    }

    public void setIntegrationAlertEnabled(boolean integrationAlertEnabled) {
        mIntegrationAlertEnabled = integrationAlertEnabled;
    }
}
"""

with open('src/main/java/io/github/dsheirer/preference/notification/NotificationRecipient.java', 'w') as f:
    f.write(content)
