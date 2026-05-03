package io.github.dsheirer.dsp.tone;

public class TwoToneDetectedEvent {
    private final String mChannel;
    private final String mMessage;

    public TwoToneDetectedEvent(String channel, String message) {
        mChannel = channel;
        mMessage = message;
    }

    public String getChannel() {
        return mChannel;
    }

    public String getMessage() {
        return mMessage;
    }
}
