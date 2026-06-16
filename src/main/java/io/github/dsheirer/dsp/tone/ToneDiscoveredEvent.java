package io.github.dsheirer.dsp.tone;

import io.github.dsheirer.audio.AudioSegment;

/**
 * Event broadcast when an unknown A/B two-tone or a long A tone is discovered.
 */
public class ToneDiscoveredEvent {
    private final double mToneA;
    private final double mToneB;
    private final AudioSegment mAudioSegment;

    public ToneDiscoveredEvent(double toneA, double toneB, AudioSegment segment) {
        this.mToneA = toneA;
        this.mToneB = toneB;
        this.mAudioSegment = segment;
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
}
