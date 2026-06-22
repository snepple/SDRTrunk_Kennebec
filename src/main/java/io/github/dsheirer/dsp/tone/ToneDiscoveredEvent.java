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

    public ToneDiscoveredEvent(double toneA, double toneB, AudioSegment segment) {
        this(toneA, toneB, segment, 0.0);
    }

    public ToneDiscoveredEvent(double toneA, double toneB, AudioSegment segment, double channelFrequency) {
        this.mToneA = toneA;
        this.mToneB = toneB;
        this.mAudioSegment = segment;
        this.mChannelFrequency = channelFrequency;
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
