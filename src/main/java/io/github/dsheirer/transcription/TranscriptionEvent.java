package io.github.dsheirer.transcription;

import io.github.dsheirer.audio.AudioSegment;
import io.github.dsheirer.protocol.Protocol;

/**
 * Event carrying a completed audio transcription.
 *
 * Identifier values (FROM radio, alias list), the audio start timestamp and the TO talkgroup id are
 * captured at transcription time because the audio segment is recycled asynchronously and may no longer
 * be valid when subscribers process this event.  The FROM radio ID is null for protocols without
 * subscriber unit identifiers (e.g. NBFM analog channels).
 */
public class TranscriptionEvent {
    private AudioSegment mAudioSegment;
    private String mTranscript;
    private Integer mFromRadioId;
    private Protocol mProtocol;
    private String mAliasListName;
    private long mStartTimestamp;
    private Integer mToId;

    public TranscriptionEvent(AudioSegment audioSegment, String transcript) {
        this(audioSegment, transcript, null, null, null, 0L, null);
    }

    public TranscriptionEvent(AudioSegment audioSegment, String transcript, Integer fromRadioId,
                              Protocol protocol, String aliasListName) {
        this(audioSegment, transcript, fromRadioId, protocol, aliasListName, 0L, null);
    }

    public TranscriptionEvent(AudioSegment audioSegment, String transcript, Integer fromRadioId,
                              Protocol protocol, String aliasListName, long startTimestamp, Integer toId) {
        mAudioSegment = audioSegment;
        mTranscript = transcript;
        mFromRadioId = fromRadioId;
        mProtocol = protocol;
        mAliasListName = aliasListName;
        mStartTimestamp = startTimestamp;
        mToId = toId;
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

    /**
     * Audio segment start timestamp (epoch millis), captured while the segment was valid.  Used to
     * correlate the transcript to its decode event in the events table.
     */
    public long getStartTimestamp() {
        return mStartTimestamp;
    }

    /**
     * TO talkgroup id captured at transcription time, or null if unavailable.  Used together with the
     * start timestamp to correlate the transcript to its decode event.
     */
    public Integer getToId() {
        return mToId;
    }
}
