package io.github.dsheirer.transcription;

import io.github.dsheirer.audio.AudioSegment;

public class TranscriptionEvent {
    private AudioSegment mAudioSegment;
    private String mTranscript;

    public TranscriptionEvent(AudioSegment audioSegment, String transcript) {
        mAudioSegment = audioSegment;
        mTranscript = transcript;
    }

    public AudioSegment getAudioSegment() {
        return mAudioSegment;
    }

    public String getTranscript() {
        return mTranscript;
    }
}
