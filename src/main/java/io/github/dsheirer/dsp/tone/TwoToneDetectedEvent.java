package io.github.dsheirer.dsp.tone;

public class TwoToneDetectedEvent {
    private final String mChannel;
    private final String mMessage;
    private final boolean mShowNotification;

    public TwoToneDetectedEvent(String channel, String message, boolean showNotification) {
        mChannel = channel;
        mMessage = message;
        mShowNotification = showNotification;
    }

    public String getChannel() {
        return mChannel;
    }

    public String getMessage() {
        return mMessage;
    }

    public boolean isShowNotification() {
        return mShowNotification;
    }
}
