package io.github.dsheirer.preference.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Persists the state of the AI Tone Discovery process.
 */
public class ToneDiscoveryState {
    
    // Key: "ToneA_ToneB" (e.g. "450.0_500.0"). Value: List of AI-extracted agency names
    private Map<String, List<String>> mPendingDiscoveries = new HashMap<>();
    
    // Set of "ToneA_ToneB" that have been finalized or blocklisted
    private Set<String> mFinalizedTones = new HashSet<>();

    // Key: "ToneA_ToneB", Value: The agency name AI figured out
    private Map<String, String> mFinalizedToneNames = new HashMap<>();

    // Soft-deletion markers for tone pairs the user rejected/deleted - an absolute barrier to regeneration.
    private List<ToneTombstone> mTombstones = new ArrayList<>();

    // Key: "ToneA_ToneB", Value: number of times the tone has been heard+transcribed (used for graceful
    // degradation: after enough observations without a confident name, stage an "Unknown Unit" placeholder).
    private Map<String, Integer> mObservationCounts = new HashMap<>();

    public ToneDiscoveryState() {
    }

    public Map<String, List<String>> getPendingDiscoveries() {
        return mPendingDiscoveries;
    }

    public void setPendingDiscoveries(Map<String, List<String>> pendingDiscoveries) {
        mPendingDiscoveries = pendingDiscoveries;
    }

    public Set<String> getFinalizedTones() {
        return mFinalizedTones;
    }

    public void setFinalizedTones(Set<String> finalizedTones) {
        mFinalizedTones = finalizedTones;
    }

    public Map<String, String> getFinalizedToneNames() {
        return mFinalizedToneNames;
    }

    public void setFinalizedToneNames(Map<String, String> finalizedToneNames) {
        mFinalizedToneNames = finalizedToneNames;
    }

    public List<ToneTombstone> getTombstones() {
        return mTombstones;
    }

    public void setTombstones(List<ToneTombstone> tombstones) {
        mTombstones = (tombstones != null) ? tombstones : new ArrayList<>();
    }

    public Map<String, Integer> getObservationCounts() {
        return mObservationCounts;
    }

    public void setObservationCounts(Map<String, Integer> observationCounts) {
        mObservationCounts = (observationCounts != null) ? observationCounts : new HashMap<>();
    }
}
