package io.github.dsheirer.dsp.tone;

import io.github.dsheirer.audio.AudioSegment;

/**
 * Event broadcast when an unknown A/B two-tone or a long A tone is discovered.
 */
public class ToneDiscoveredEvent {
    private final double mToneA;
    private final double mToneB;
    private final AudioSegment mAudioSegment;
    //RF channel frequency (Hz) the tone was heard on, or 0 if unknown.  Part of the tombstone primary key and the
    //human-review package ("RF channel of origin").
    private final double mChannelFrequency;
    //Human-readable channel/alias name the tone was heard on (or null/empty if unknown).  Used to seed an
    //AI-created detector's draft name and detection history.
    private final String mChannelName;

    public ToneDiscoveredEvent(double toneA, double toneB, AudioSegment segment) {
        this(toneA, toneB, segment, 0.0);
    }

    public ToneDiscoveredEvent(double toneA, double toneB, AudioSegment segment, double channelFrequency) {
        this(toneA, toneB, segment, channelFrequency, null);
    }

    public ToneDiscoveredEvent(double toneA, double toneB, AudioSegment segment, double channelFrequency,
                               String channelName) {
        this.mToneA = toneA;
        this.mToneB = toneB;
        this.mAudioSegment = segment;
        this.mChannelFrequency = channelFrequency;
        this.mChannelName = channelName;
    }

    /**
     * Human-readable channel/alias name the tone was heard on, or null/empty if unknown.
     */
    public String getChannelName() {
        return mChannelName;
    }

    public double getToneA() {
        return mToneA;
    }

    public double getToneB() {
        return mToneB;
    }

    public AudioSegment getAudioSegment() {
        return mAudioSegment;
    }

    /**
     * RF channel frequency (Hz) the tone was discovered on, or 0 if it could not be determined.
     */
    public double getChannelFrequency() {
        return mChannelFrequency;
    }
}
