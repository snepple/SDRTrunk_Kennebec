package io.github.dsheirer.preference.notification;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class NotificationRecipient {

    public enum DeliveryMethod {
        EMAIL, TELEGRAM
    }

    private DeliveryMethod mDeliveryMethod = DeliveryMethod.EMAIL;
    private String mDestination = "";
    private boolean mChannelInactivityEnabled = false;
    private boolean mAiAudioMonitoringEnabled = false;

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
}
