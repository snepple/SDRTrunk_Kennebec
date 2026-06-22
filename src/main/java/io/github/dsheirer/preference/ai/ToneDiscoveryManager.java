package io.github.dsheirer.preference.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dsheirer.dsp.tone.ToneDiscoveredEvent;
import io.github.dsheirer.dsp.tone.ToneStandards;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.playlist.TwoToneConfiguration;
import io.github.dsheirer.transcription.TranscriptionEvent;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestrates AI-driven Two-Tone paging discovery.
 * Correlates discovered tones with resulting transcripts, queries Gemini for unit identification,
 * and builds confidence over multiple occurrences to automatically create accurate playlist detectors.
 */
public class ToneDiscoveryManager {
    private static final Logger mLog = LoggerFactory.getLogger(ToneDiscoveryManager.class);

    private static ToneDiscoveryManager mInstance;

    private static final int CONFIDENCE_THRESHOLD = 3;
    //After this many observations of a tone with no confident agency name, stage an "Unknown Unit" placeholder so
    //the (accurately measured) tone data is preserved for human review rather than being lost (graceful degradation).
    private static final int UNKNOWN_PLACEHOLDER_THRESHOLD = 4;
    private static final double TONE_TOLERANCE_HZ = 5.0;
    private static final String STATE_FILE_PATH = "ai_tone_discovery_state.json";

    private final UserPreferences mUserPreferences;
    private final PlaylistManager mPlaylistManager;
    private ToneDiscoveryState mState;

    // Ephemeral map of AudioSegment ID to ToneDiscoveredEvent waiting for a transcript
    private final Map<Long, ToneDiscoveredEvent> mPendingTranscripts = new ConcurrentHashMap<>();

    public ToneDiscoveryManager(UserPreferences userPreferences, PlaylistManager playlistManager) {
        mUserPreferences = userPreferences;
        mPlaylistManager = playlistManager;
        loadState();
        MyEventBus.getGlobalEventBus().register(this);
        mInstance = this;
    }

    public static ToneDiscoveryManager getInstance() {
        return mInstance;
    }

    public Set<String> getFinalizedTones() {
        if (mState != null) {
            return mState.getFinalizedTones();
        }
        return Collections.emptySet();
    }

    public void unignoreTone(String toneKey) {
        if (mState != null && mState.getFinalizedTones().contains(toneKey)) {
            mState.getFinalizedTones().remove(toneKey);
            mState.getFinalizedToneNames().remove(toneKey);
            saveState();
        }
    }

    public String getFinalizedToneName(String toneKey) {
        if (mState != null && mState.getFinalizedToneNames() != null) {
            return mState.getFinalizedToneNames().get(toneKey);
        }
        return null;
    }

    private void loadState() {
        File file = new File(System.getProperty("user.home"), STATE_FILE_PATH);
        if (file.exists()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                mState = mapper.readValue(file, ToneDiscoveryState.class);
            } catch (IOException e) {
                mLog.error("Failed to load ToneDiscoveryState", e);
                mState = new ToneDiscoveryState();
            }
        } else {
            mState = new ToneDiscoveryState();
        }
    }

    private synchronized void saveState() {
        File file = new File(System.getProperty("user.home"), STATE_FILE_PATH);
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(file, mState);
        } catch (IOException e) {
            mLog.error("Failed to save ToneDiscoveryState", e);
        }
    }

    private String getToneKey(double toneA, double toneB) {
        // Find if an existing key matches within tolerance
        for (String existingKey : mState.getPendingDiscoveries().keySet()) {
            String[] parts = existingKey.split("_");
            if (parts.length == 2) {
                double exA = Double.parseDouble(parts[0]);
                double exB = Double.parseDouble(parts[1]);
                if (Math.abs(exA - toneA) <= TONE_TOLERANCE_HZ && Math.abs(exB - toneB) <= TONE_TOLERANCE_HZ) {
                    return existingKey;
                }
            }
        }
        for (String existingKey : mState.getFinalizedTones()) {
            String[] parts = existingKey.split("_");
            if (parts.length == 2) {
                double exA = Double.parseDouble(parts[0]);
                double exB = Double.parseDouble(parts[1]);
                if (Math.abs(exA - toneA) <= TONE_TOLERANCE_HZ && Math.abs(exB - toneB) <= TONE_TOLERANCE_HZ) {
                    return existingKey;
                }
            }
        }
        return Math.round(toneA) + "_" + Math.round(toneB);
    }

    @Subscribe
    public void onToneDiscovered(ToneDiscoveredEvent event) {
        if (!mUserPreferences.getAIPreference().isAIEnabled() || 
            !mUserPreferences.getAIPreference().isAIToneDiscoveryEnabled()) {
            return;
        }

        //Multi-gated reconciliation runs BEFORE we stage the segment for the expensive transcription/NLP pipeline,
        //so we never burn CPU transcribing a tone the user already has configured or has explicitly rejected.
        String toneKey = getToneKey(event.getToneA(), event.getToneB());

        // Gate 1 (active): a detector for these tones already exists in the playlist (within analog tolerance).
        if (matchesActiveConfiguration(event.getToneA(), event.getToneB())) {
            return;
        }

        // Gate 2 (tombstone): the user previously rejected/deleted these tones on this channel.
        if (matchesTombstone(event.getToneA(), event.getToneB(), event.getChannelFrequency())) {
            return;
        }

        // Gate 3: we've already finalized/blocklisted this tone.
        if (mState.getFinalizedTones().contains(toneKey)) {
            return;
        }

        // Store it so we can pair it when the transcript arrives
        if (event.getAudioSegment() != null) {
            // Using hashcode or some unique ID from AudioSegment. If no unique ID exists, we can use object hash or timestamp.
            mPendingTranscripts.put((long) event.getAudioSegment().hashCode(), event);
        }
    }

    /**
     * Indicates whether an active two-tone detector already exists in the current playlist for these tones, evaluated
     * within the standard analog tolerance so a slightly drifted measurement still counts as a match.
     */
    private boolean matchesActiveConfiguration(double toneA, double toneB) {
        if (mPlaylistManager == null) {
            return false;
        }

        try {
            for (TwoToneConfiguration config : mPlaylistManager.getTwoToneConfigurations()) {
                if (ToneStandards.toneSequencesMatch(config.getToneA(), config.getToneB(), toneA, toneB)) {
                    return true;
                }
            }
        } catch (Exception e) {
            mLog.debug("Unable to reconcile against active two-tone configurations - " + e.getMessage());
        }

        return false;
    }

    /**
     * Indicates whether a tombstone (user rejection) exists for these tones, evaluated within the standard analog
     * tolerance.  A tombstone with an unknown channel frequency (0) matches any channel; otherwise the channel
     * frequency must also match within tolerance.
     */
    private boolean matchesTombstone(double toneA, double toneB, double channelFrequency) {
        if (mState == null || mState.getTombstones() == null) {
            return false;
        }

        for (ToneTombstone tombstone : mState.getTombstones()) {
            if (!ToneStandards.toneSequencesMatch(tombstone.getToneA(), tombstone.getToneB(), toneA, toneB)) {
                continue;
            }

            double tombstoneFrequency = tombstone.getChannelFrequency();

            //A 0/unknown frequency on either side means "any channel"; otherwise require the channels to match.
            if (tombstoneFrequency <= 0 || channelFrequency <= 0 ||
                ToneStandards.withinTolerance(tombstoneFrequency, channelFrequency)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Records a tombstone (soft-deletion marker) so an AI-discovered tone pair the user rejected/deleted is never
     * regenerated.  Idempotent within tolerance: an existing equivalent tombstone is not duplicated.  Also drops any
     * in-flight pending/observation state for the tone.
     *
     * @param toneA first tone (Hz)
     * @param toneB second tone (Hz), or &le;0 for a single-tone (Long A / All-Call) page
     * @param channelFrequency RF channel frequency the detector was discovered on (Hz), or 0 if unknown
     */
    public synchronized void recordTombstone(double toneA, double toneB, double channelFrequency) {
        if (mState == null) {
            return;
        }

        if (matchesTombstone(toneA, toneB, channelFrequency)) {
            return; //Already excluded within tolerance.
        }

        mState.getTombstones().add(new ToneTombstone(toneA, toneB, channelFrequency, System.currentTimeMillis()));

        //Clear any in-flight accumulation for this tone so it cannot finalize after being rejected.
        String toneKey = getToneKey(toneA, toneB);
        mState.getPendingDiscoveries().remove(toneKey);
        if (mState.getObservationCounts() != null) {
            mState.getObservationCounts().remove(toneKey);
        }

        mLog.info("AI Tone Discovery: recorded tombstone for rejected tones " + toneKey +
            (channelFrequency > 0 ? (" on " + channelFrequency + " Hz") : ""));
        saveState();
    }

    @Subscribe
    public void onTranscriptionEvent(TranscriptionEvent event) {
        if (!mUserPreferences.getAIPreference().isAIEnabled() || 
            !mUserPreferences.getAIPreference().isAIToneDiscoveryEnabled()) {
            return;
        }

        long segmentId = (long) event.getAudioSegment().hashCode();
        ToneDiscoveredEvent toneEvent = mPendingTranscripts.remove(segmentId);
        
        if (toneEvent != null) {
            processDiscoveredToneWithTranscript(toneEvent, event.getTranscript());
        }
    }

    private void processDiscoveredToneWithTranscript(ToneDiscoveredEvent event, String transcript) {
        String apiKey = mUserPreferences.getAIPreference().getGeminiApiKey();
        String model = mUserPreferences.getAIPreference().getGeminiModel();

        if (apiKey == null || apiKey.isEmpty()) return;

        // Run async on a low-priority background path so the resource-intensive NLP never competes with the core
        // tuner -> demod -> audio -> stream pipeline.
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            String agency = GeminiApiHelper.extractAgencyFromTranscript(apiKey, model, transcript);
            boolean haveName = agency != null && !agency.equalsIgnoreCase("UNKNOWN") && !agency.trim().isEmpty();

            synchronized (this) {
                String toneKey = getToneKey(event.getToneA(), event.getToneB());

                //Re-check the exclusion gates - state may have changed while the async NLP call was in flight.
                if (mState.getFinalizedTones().contains(toneKey) ||
                    matchesActiveConfiguration(event.getToneA(), event.getToneB()) ||
                    matchesTombstone(event.getToneA(), event.getToneB(), event.getChannelFrequency())) {
                    return;
                }

                //Count every observation of this tone (named or not) to drive graceful degradation.
                int observations = mState.getObservationCounts().merge(toneKey, 1, Integer::sum);

                if (haveName) {
                    List<String> agencies = mState.getPendingDiscoveries().computeIfAbsent(toneKey, k -> new ArrayList<>());
                    agencies.add(agency.trim());

                    mLog.info("AI Tone Discovery: Heard tone " + toneKey + ". AI identified unit: " + agency.trim() +
                        ". Count: " + agencies.size());

                    if (agencies.size() >= CONFIDENCE_THRESHOLD) {
                        finalizeDetector(event.getToneA(), event.getToneB(), toneKey, agencies,
                            event.getChannelFrequency(), transcript);
                    } else {
                        saveState();
                    }
                } else {
                    boolean hasNamedCandidate = mState.getPendingDiscoveries().containsKey(toneKey) &&
                        !mState.getPendingDiscoveries().get(toneKey).isEmpty();

                    //Graceful degradation: enough unintelligible observations and no nameable candidate -> preserve
                    //the accurately measured tone data behind an "Unknown Unit" placeholder for human review.
                    if (!hasNamedCandidate && observations >= UNKNOWN_PLACEHOLDER_THRESHOLD) {
                        finalizeDetector(event.getToneA(), event.getToneB(), toneKey, null,
                            event.getChannelFrequency(), transcript);
                    } else {
                        saveState();
                    }
                }
            }
        });
    }

    /**
     * Stages a new (disabled, review-pending) detector for the discovered tones.  Tones are snapped to the nearest
     * standardized paging matrix value, and the RF channel of origin plus the raw transcript are stored as the
     * human-review package.  A null/empty {@code agencies} list produces an "Unknown Unit" placeholder that preserves
     * the tone data (graceful degradation).
     */
    private void finalizeDetector(double toneA, double toneB, String toneKey, List<String> agencies,
                                  double channelFrequency, String transcript) {
        //Snap to the nearest standard matrix frequency to avoid marginally varying duplicates and present clean values.
        double snappedA = ToneStandards.snap(toneA);
        double snappedB = toneB > 0 ? ToneStandards.snap(toneB) : toneB;

        String displayName;
        String resolvedName;

        if (agencies != null && !agencies.isEmpty()) {
            //Pick the most common extracted name.
            Map<String, Integer> frequencyMap = new HashMap<>();
            for (String a : agencies) {
                frequencyMap.put(a, frequencyMap.getOrDefault(a, 0) + 1);
            }
            resolvedName = agencies.get(0);
            int max = 0;
            for (Map.Entry<String, Integer> entry : frequencyMap.entrySet()) {
                if (entry.getValue() > max) {
                    max = entry.getValue();
                    resolvedName = entry.getKey();
                }
            }
            displayName = "[AI] " + resolvedName;
        } else {
            resolvedName = buildUnknownPlaceholder(snappedA, snappedB, channelFrequency);
            displayName = "[AI] " + resolvedName;
        }

        mLog.info("AI Tone Discovery: Finalizing detector for tone " + toneKey + " as [" + resolvedName + "]");

        TwoToneConfiguration newConfig = new TwoToneConfiguration();
        newConfig.setToneA(snappedA);
        newConfig.setToneB(snappedB);
        newConfig.setLongATone(snappedB <= 0);
        newConfig.setAlias(displayName);
        newConfig.setEnabled(false); // User must review
        newConfig.setAutoDiscovered(true);
        newConfig.setDiscoveryFrequency(channelFrequency);
        newConfig.setDiscoveryTranscript(transcript != null ? transcript.trim() : "");

        javafx.application.Platform.runLater(() -> {
            try {
                mPlaylistManager.getTwoToneConfigurations().add(newConfig);
                mPlaylistManager.schedulePlaylistSave();
            } catch (Exception e) {
                mLog.error("Error saving new AI two tone configuration", e);
            }
        });

        // Mark as finalized so we never process it again (even if user deletes it)
        mState.getPendingDiscoveries().remove(toneKey);
        mState.getObservationCounts().remove(toneKey);
        mState.getFinalizedTones().add(toneKey);
        mState.getFinalizedToneNames().put(toneKey, resolvedName);
        saveState();
    }

    /**
     * Builds a descriptive placeholder name that preserves the measured tone data and RF channel of origin when no
     * confident unit name could be extracted, e.g. {@code "Unknown Unit (Tones: 1006.9/832.5 on 154.1450)"}.
     */
    static String buildUnknownPlaceholder(double toneA, double toneB, double channelFrequency) {
        StringBuilder sb = new StringBuilder("Unknown Unit (Tones: ");
        sb.append(formatTone(toneA));

        if (toneB > 0) {
            sb.append('/').append(formatTone(toneB));
        }

        if (channelFrequency > 0) {
            //channelFrequency is in Hz; present as MHz to match standard channel notation.
            sb.append(String.format(" on %.4f", channelFrequency / 1_000_000.0));
        }

        sb.append(')');
        return sb.toString();
    }

    private static String formatTone(double tone) {
        return String.format("%.1f", tone);
    }
}
