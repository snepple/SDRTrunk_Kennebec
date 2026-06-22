package io.github.dsheirer.preference.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dsheirer.dsp.tone.ToneDiscoveredEvent;
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
    private static final double TONE_TOLERANCE_HZ = 5.0;
    private static final String STATE_FILE_PATH = "ai_tone_discovery_state.json";

    private final UserPreferences mUserPreferences;
    private final PlaylistManager mPlaylistManager;
    private ToneDiscoveryState mState;

    //A discovered tone waits here (keyed by its AudioSegment id) until the matching transcript arrives on the
    //event bus - this is how tone discovery "naturally waits" for the (low-priority, possibly delayed)
    //transcript.  Bounded with LRU eviction so that tones whose transcript never arrives (e.g. transcription
    //disabled, too short, or shed under extreme load) do not pin their AudioSegment - and its audio buffers -
    //in memory indefinitely.
    private static final int MAX_PENDING_TONE_TRANSCRIPTS = 200;
    private final Map<Long, ToneDiscoveredEvent> mPendingTranscripts = Collections.synchronizedMap(
        new LinkedHashMap<Long, ToneDiscoveredEvent>(16, 0.75f, false) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, ToneDiscoveredEvent> eldest) {
                return size() > MAX_PENDING_TONE_TRANSCRIPTS;
            }
        });

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

        String toneKey = getToneKey(event.getToneA(), event.getToneB());
        
        // If we've already finalized or blocklisted this tone, ignore it
        if (mState.getFinalizedTones().contains(toneKey)) {
            return;
        }

        // Store it so we can pair it when the transcript arrives
        if (event.getAudioSegment() != null) {
            // Using hashcode or some unique ID from AudioSegment. If no unique ID exists, we can use object hash or timestamp.
            mPendingTranscripts.put((long) event.getAudioSegment().hashCode(), event);
        }
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

        // Run async
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            String agency = GeminiApiHelper.extractAgencyFromTranscript(apiKey, model, transcript);
            if (agency != null && !agency.equalsIgnoreCase("UNKNOWN") && !agency.trim().isEmpty()) {
                synchronized (this) {
                    String toneKey = getToneKey(event.getToneA(), event.getToneB());
                    if (mState.getFinalizedTones().contains(toneKey)) {
                        return; // Race condition check
                    }

                    List<String> agencies = mState.getPendingDiscoveries().computeIfAbsent(toneKey, k -> new ArrayList<>());
                    agencies.add(agency);

                    mLog.info("AI Tone Discovery: Heard tone " + toneKey + ". AI identified agency: " + agency + ". Count: " + agencies.size());

                    if (agencies.size() >= CONFIDENCE_THRESHOLD) {
                        // Finalize it
                        finalizeDetector(event.getToneA(), event.getToneB(), toneKey, agencies);
                    } else {
                        saveState();
                    }
                }
            }
        });
    }

    private void finalizeDetector(double toneA, double toneB, String toneKey, List<String> agencies) {
        // Pick the most common agency name
        Map<String, Integer> frequencyMap = new HashMap<>();
        for (String a : agencies) {
            frequencyMap.put(a, frequencyMap.getOrDefault(a, 0) + 1);
        }
        String bestAgency = agencies.get(0);
        int max = 0;
        for (Map.Entry<String, Integer> entry : frequencyMap.entrySet()) {
            if (entry.getValue() > max) {
                max = entry.getValue();
                bestAgency = entry.getKey();
            }
        }

        mLog.info("AI Tone Discovery: Finalizing detector for tone " + toneKey + " as [" + bestAgency + "]");

        TwoToneConfiguration newConfig = new TwoToneConfiguration();
        newConfig.setToneA((float) toneA);
        newConfig.setToneB((float) toneB);
        newConfig.setAlias("[AI] " + bestAgency);
        newConfig.setEnabled(false); // User must review
        newConfig.setAutoDiscovered(true);

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
        mState.getFinalizedTones().add(toneKey);
        mState.getFinalizedToneNames().put(toneKey, bestAgency);
        saveState();
    }
}
