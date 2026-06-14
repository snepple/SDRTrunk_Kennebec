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
    public static final String KEY_GAIN_ADVISOR_ENABLED = "ai.gain.advisor.enabled";

    private Preferences mPreferences = Preferences.userNodeForPackage(AIPreference.class);

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
     * Whether to automatically optimize NBFM audio DSP settings using Gemini after every 5th completed call.
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
     * Whether to enable the Adaptive Gain Advisor, which monitors I/Q signal power levels
     * across all active channels and logs recommendations when gain appears sub-optimal.
     * Works with or without AI; when AI is enabled, provides hourly Gemini-assisted analysis
     * that accounts for propagation patterns and multi-channel interactions.
     */
    public boolean isGainAdvisorEnabled() {
        return mPreferences.getBoolean(KEY_GAIN_ADVISOR_ENABLED, true);
    }

    public void setGainAdvisorEnabled(boolean enabled) {
        mPreferences.putBoolean(KEY_GAIN_ADVISOR_ENABLED, enabled);
        notifyPreferenceUpdated();
    }
}
