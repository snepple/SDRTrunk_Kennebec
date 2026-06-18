package io.github.dsheirer.preference.ai;

import io.github.dsheirer.preference.Preference;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.sample.Listener;
import java.util.prefs.Preferences;

public class AIPreference extends Preference {
    public static final String PREFERENCE_NAME = "AI";
    public static final String KEY_GEMINI_API_KEY = "gemini.api.key";
    public static final String KEY_GEMINI_API_KEY_TESTED = "gemini.api.key.tested";
    public static final String KEY_AI_ENABLED = "ai.enabled";
    public static final String KEY_SYSTEM_HEALTH_ENABLED = "ai.system.health.enabled";
    public static final String KEY_GEMINI_MODEL = "gemini.model";
    public static final String KEY_AI_LOG_ANALYSIS_ENABLED = "ai.log.analysis.enabled";
    public static final String KEY_AI_TONE_DISCOVERY_ENABLED = "ai.tone.discovery.enabled";
    
    public static final String KEY_TRANSCRIPTION_ENABLED = "ai.transcription.enabled";
    public static final String KEY_TRANSCRIPTION_ENGINE = "ai.transcription.engine";
    public static final String KEY_GOOGLE_STT_API_KEY = "ai.transcription.google.key";
    public static final String KEY_WHISPER_API_KEY = "ai.transcription.whisper.key";
    public static final String KEY_NBFM_AUTO_OPTIMIZE = "ai.nbfm.auto.optimize";
    public static final String KEY_NBFM_LAST_OPTIMIZE_PREFIX = "ai.nbfm.last.optimize.";
    public static final String KEY_GAIN_ADVISOR_ENABLED = "ai.gain.advisor.enabled";
    public static final String KEY_SQUELCH_ADVISOR_ENABLED = "ai.squelch.advisor.enabled";

    //Auto-optimize a given NBFM channel at most this often.  Running every few calls burns Gemini
    //tokens very quickly when many channels are configured, so the cadence is time-based (twice a day).
    private static final long NBFM_AUTO_OPTIMIZE_INTERVAL_MS = 12L * 60L * 60L * 1000L;

    private Preferences mPreferences = Preferences.userNodeForPackage(AIPreference.class);

    //Cache the gain-advisor flag.  isGainAdvisorEnabled() is polled from the per-sample-buffer decoder
    //path (e.g. NBFMDecoder.receive()); reading the registry-backed Preferences there thousands of times
    //per second per channel thrashes the Windows registry.  Read it once and refresh on update.
    private Boolean mGainAdvisorEnabled;

    public AIPreference(Listener<PreferenceType> updateListener) {
        super(updateListener);
    }

    @Override
    public PreferenceType getPreferenceType() {
        return PreferenceType.APPLICATION; // Or create a new one, but let's reuse APPLICATION for simplicity or create AI
    }

    public boolean isAIEnabled() {
        return mPreferences.getBoolean(KEY_AI_ENABLED, false);
    }

    public void setAIEnabled(boolean enabled) {
        mPreferences.putBoolean(KEY_AI_ENABLED, enabled);
        notifyPreferenceUpdated();
    }

    public boolean isAILogAnalysisEnabled() {
        return mPreferences.getBoolean(KEY_AI_LOG_ANALYSIS_ENABLED, false);
    }

    public void setAILogAnalysisEnabled(boolean enabled) {
        mPreferences.putBoolean(KEY_AI_LOG_ANALYSIS_ENABLED, enabled);
        notifyPreferenceUpdated();
    }

    public boolean isAIToneDiscoveryEnabled() {
        return mPreferences.getBoolean(KEY_AI_TONE_DISCOVERY_ENABLED, false);
    }

    public void setAIToneDiscoveryEnabled(boolean enabled) {
        mPreferences.putBoolean(KEY_AI_TONE_DISCOVERY_ENABLED, enabled);
        notifyPreferenceUpdated();
    }

    public String getGeminiApiKey() {
        String key = mPreferences.get(KEY_GEMINI_API_KEY, "");

        //Support scripted/headless provisioning: fall back to environment variable or system
        //property when no key has been configured through the GUI
        if(key == null || key.isEmpty()) {
            key = System.getProperty("sdrtrunk.gemini.api.key", System.getenv("GEMINI_API_KEY"));
        }

        return key == null ? "" : key;
    }

    public void setGeminiApiKey(String apiKey) {
        mPreferences.put(KEY_GEMINI_API_KEY, apiKey);
        notifyPreferenceUpdated();
    }

    public boolean isGeminiApiKeyTested() {
        return mPreferences.getBoolean(KEY_GEMINI_API_KEY_TESTED, false);
    }

    public void setGeminiApiKeyTested(boolean tested) {
        mPreferences.putBoolean(KEY_GEMINI_API_KEY_TESTED, tested);
        notifyPreferenceUpdated();
    }

    public boolean isSystemHealthAdvisorEnabled() {
        return mPreferences.getBoolean(KEY_SYSTEM_HEALTH_ENABLED, false);
    }

    public void setSystemHealthAdvisorEnabled(boolean enabled) {
        mPreferences.putBoolean(KEY_SYSTEM_HEALTH_ENABLED, enabled);
        notifyPreferenceUpdated();
    }
    public String getGeminiModel() {
        return mPreferences.get(KEY_GEMINI_MODEL, "models/gemini-1.5-flash");
    }

    public void setGeminiModel(String model) {
        mPreferences.put(KEY_GEMINI_MODEL, model);
        notifyPreferenceUpdated();
    }

    public boolean isTranscriptionEnabled() {
        return mPreferences.getBoolean(KEY_TRANSCRIPTION_ENABLED, false);
    }

    public void setTranscriptionEnabled(boolean enabled) {
        mPreferences.putBoolean(KEY_TRANSCRIPTION_ENABLED, enabled);
        notifyPreferenceUpdated();
    }

    public String getTranscriptionEngine() {
        return mPreferences.get(KEY_TRANSCRIPTION_ENGINE, "WHISPER"); // "WHISPER" or "GOOGLE"
    }

    public void setTranscriptionEngine(String engine) {
        mPreferences.put(KEY_TRANSCRIPTION_ENGINE, engine);
        notifyPreferenceUpdated();
    }

    public String getGoogleSttApiKey() {
        return mPreferences.get(KEY_GOOGLE_STT_API_KEY, "");
    }

    public void setGoogleSttApiKey(String apiKey) {
        mPreferences.put(KEY_GOOGLE_STT_API_KEY, apiKey);
        notifyPreferenceUpdated();
    }

    public String getWhisperApiKey() {
        return mPreferences.get(KEY_WHISPER_API_KEY, "");
    }

    public void setWhisperApiKey(String apiKey) {
        mPreferences.put(KEY_WHISPER_API_KEY, apiKey);
        notifyPreferenceUpdated();
    }

    /**
     * Whether to automatically optimize NBFM audio DSP settings using Gemini.  When enabled, each
     * channel is optimized at most twice a day (see {@link #isNBFMAutoOptimizeDue(String)}) to keep
     * Gemini token usage reasonable even with many channels configured.
     * Requires AI to be enabled and a Gemini API key to be configured.
     */
    public boolean isNBFMAudioAutoOptimizeEnabled() {
        return isAIEnabled() && mPreferences.getBoolean(KEY_NBFM_AUTO_OPTIMIZE, false);
    }

    public void setNBFMAudioAutoOptimizeEnabled(boolean enabled) {
        mPreferences.putBoolean(KEY_NBFM_AUTO_OPTIMIZE, enabled);
        notifyPreferenceUpdated();
    }

    /**
     * Preferences keys are length-limited, so long channel names are hashed to stay within bounds.
     */
    private static String nbfmOptimizeKey(String channelName) {
        String safe = channelName == null ? "" : channelName;
        if((KEY_NBFM_LAST_OPTIMIZE_PREFIX.length() + safe.length()) <= Preferences.MAX_KEY_LENGTH) {
            return KEY_NBFM_LAST_OPTIMIZE_PREFIX + safe;
        }
        return KEY_NBFM_LAST_OPTIMIZE_PREFIX + Integer.toHexString(safe.hashCode());
    }

    /**
     * Epoch-millis of the last automatic NBFM optimization for the given channel (0 if never).
     */
    public long getNBFMLastOptimizeMs(String channelName) {
        return mPreferences.getLong(nbfmOptimizeKey(channelName), 0L);
    }

    public void setNBFMLastOptimizeMs(String channelName, long timestampMs) {
        mPreferences.putLong(nbfmOptimizeKey(channelName), timestampMs);
    }

    /**
     * Whether an automatic NBFM optimization is due for the given channel.  True only when
     * auto-optimize is enabled and at least the twice-daily interval has elapsed since the last run.
     */
    public boolean isNBFMAutoOptimizeDue(String channelName) {
        if(!isNBFMAudioAutoOptimizeEnabled()) {
            return false;
        }
        return (System.currentTimeMillis() - getNBFMLastOptimizeMs(channelName)) >= NBFM_AUTO_OPTIMIZE_INTERVAL_MS;
    }

    /**
     * Whether to enable the Adaptive Gain Advisor, which monitors I/Q signal power levels
     * across all active channels and logs recommendations when gain appears sub-optimal.
     * Works with or without AI; when AI is enabled, provides hourly Gemini-assisted analysis
     * that accounts for propagation patterns and multi-channel interactions.
     */
    public boolean isGainAdvisorEnabled() {
        Boolean cached = mGainAdvisorEnabled;

        if(cached == null) {
            //Default OFF so a fresh install matches the lean upstream/original profile (no per-sample-buffer
            //power accounting or advisor thread competing with channel I/Q processing). Opt-in via the UI.
            cached = mPreferences.getBoolean(KEY_GAIN_ADVISOR_ENABLED, false);
            mGainAdvisorEnabled = cached;
        }

        return cached;
    }

    public void setGainAdvisorEnabled(boolean enabled) {
        mPreferences.putBoolean(KEY_GAIN_ADVISOR_ENABLED, enabled);
        mGainAdvisorEnabled = enabled;
        notifyPreferenceUpdated();
    }

    /**
     * Whether to enable the Squelch Advisor, which powers the manual "Calibrate Squelch" button in the
     * NBFM audio squelch view.  When enabled, the user can sample the live noise-variance for a few
     * seconds and have recommended open/close noise thresholds applied automatically.  This is a local,
     * heuristic feature - it analyses the channel's own noise statistics and does not call any external
     * AI service.
     */
    public boolean isSquelchAdvisorEnabled() {
        return mPreferences.getBoolean(KEY_SQUELCH_ADVISOR_ENABLED, false);
    }

    public void setSquelchAdvisorEnabled(boolean enabled) {
        mPreferences.putBoolean(KEY_SQUELCH_ADVISOR_ENABLED, enabled);
        notifyPreferenceUpdated();
    }
}
