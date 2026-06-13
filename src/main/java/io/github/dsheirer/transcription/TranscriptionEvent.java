package io.github.dsheirer.transcription;

import io.github.dsheirer.audio.AudioSegment;
import io.github.dsheirer.protocol.Protocol;

/**
 * Event carrying a completed audio transcription.
 *
 * Identifier values (FROM radio, alias list) are captured at transcription time because the
 * audio segment is recycled asynchronously and may no longer be valid when subscribers process
 * this event.  The FROM radio ID is null for protocols without subscriber unit identifiers
 * (e.g. NBFM analog channels).
 */
public class TranscriptionEvent {
    private AudioSegment mAudioSegment;
    private String mTranscript;
    private Integer mFromRadioId;
    private Protocol mProtocol;
    private String mAliasListName;

    public TranscriptionEvent(AudioSegment audioSegment, String transcript) {
        this(audioSegment, transcript, null, null, null);
    }

    public TranscriptionEvent(AudioSegment audioSegment, String transcript, Integer fromRadioId,
                              Protocol protocol, String aliasListName) {
        mAudioSegment = audioSegment;
        mTranscript = transcript;
        mFromRadioId = fromRadioId;
        mProtocol = protocol;
        mAliasListName = aliasListName;
    }

    public AudioSegment getAudioSegment() {
        return mAudioSegment;
    }

    public String getTranscript() {
        return mTranscript;
    }

    /**
     * FROM radio (subscriber unit) ID, or null when the source protocol has none (e.g. NBFM).
     */
    public Integer getFromRadioId() {
        return mFromRadioId;
    }

    public Protocol getProtocol() {
        return mProtocol;
    }

    public String getAliasListName() {
        return mAliasListName;
    }
}
