package io.github.dsheirer.module.decode.nbfm.ai;

import io.github.dsheirer.preference.UserPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Base64;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

public class AudioWatchdogService {
    private static final Logger mLog = LoggerFactory.getLogger(AudioWatchdogService.class);
    
    private final UserPreferences mUserPreferences;
    private final HttpClient mHttpClient;
    private final ScheduledExecutorService mScheduler;

    private final AtomicLong mLastIQDataTime = new AtomicLong(System.currentTimeMillis());
    private final AtomicLong mLastAudioDataTime = new AtomicLong(System.currentTimeMillis());
    
    // Configurable thresholds
    private static final long IQ_SILENCE_THRESHOLD_MS = 10000;
    private static final long AUDIO_ZERO_THRESHOLD_MS = 3000;

    //Once a watchdog fires it backs off before it can fire again, so a channel that is simply idle
    //(no traffic) does not generate a continuous storm of triggers/log spam/Gemini calls.
    private static final long TRIGGER_BACKOFF_MS = 60000;

    //After this many consecutive Gemini failures (e.g. 404/bad model/no network) stop calling the
    //API entirely and fall back to local handling. Prevents a synchronous-HTTP retry storm.
    private static final int GEMINI_FAILURE_LIMIT = 3;

    private List<float[]> mRecentAudioSnapshot = new ArrayList<>();

    //The watchdog only watches for the disappearance of signal that was previously present. Until a
    //channel has actually produced non-zero audio/IQ at least once, startup/idle silence is expected
    //and must not trigger USB resets or JMBE flushes.
    private volatile boolean mHasReceivedAudio = false;
    private volatile boolean mHasReceivedIQ = false;

    private final AtomicLong mLastTriggerTime = new AtomicLong(0);
    private final AtomicInteger mGeminiFailureCount = new AtomicInteger(0);
    private volatile boolean mGeminiDisabled = false;

    public AudioWatchdogService(UserPreferences prefs) {
        mUserPreferences = prefs;
        mHttpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();
        
        mScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AudioWatchdogService-Thread");
            t.setDaemon(true);
            return t;
        });
        
        mScheduler.scheduleAtFixedRate(this::checkWatchdogs, 1, 1, TimeUnit.SECONDS);
    }

    public void feedIQData(float[] iqSamples) {
        boolean isSilent = true;
        for (float sample : iqSamples) {
            if (Math.abs(sample) > 0.0001f) {
                isSilent = false;
                break;
            }
        }
        if (!isSilent) {
            mLastIQDataTime.set(System.currentTimeMillis());
            mHasReceivedIQ = true;
        }
    }

    public void feedAudioData(float[] audioSamples) {
        boolean isZero = true;
        for (float sample : audioSamples) {
            if (Math.abs(sample) > 0.0001f) {
                isZero = false;
                break;
            }
        }
        
        if (!isZero) {
            mLastAudioDataTime.set(System.currentTimeMillis());
            mHasReceivedAudio = true;
        }

        synchronized(mRecentAudioSnapshot) {
            mRecentAudioSnapshot.add(audioSamples);
            if (mRecentAudioSnapshot.size() > 50) { // Keep recent context
                mRecentAudioSnapshot.remove(0);
            }
        }
    }

    private void checkWatchdogs() {
        try {
            long now = System.currentTimeMillis();

            //Back off after any trigger so an idle channel doesn't generate a continuous storm of
            //resets/flushes/Gemini calls and the associated log spam + GC churn.
            if (now - mLastTriggerTime.get() < TRIGGER_BACKOFF_MS) {
                return;
            }

            long iqSilenceDuration = now - mLastIQDataTime.get();
            long audioZeroDuration = now - mLastAudioDataTime.get();

            //Only react to signal that was previously present and then disappeared. Channels that have
            //never produced data (startup, never-active channels) must not trigger anything.
            if (mHasReceivedIQ && iqSilenceDuration > IQ_SILENCE_THRESHOLD_MS) {
                mLog.warn("Hardware Resiliency Watchdog: I/Q Data silence detected for >10s. Triggering Soft USB Reset.");
                triggerSoftUsbReset();
                mLastTriggerTime.set(now);
                mLastIQDataTime.set(now); // Reset timer to prevent spam
            } else if (mHasReceivedAudio && audioZeroDuration > AUDIO_ZERO_THRESHOLD_MS) {
                mLog.warn("Hardware Resiliency Watchdog: Audio buffer zero amplitude detected for >3s.");
                List<float[]> snapshot;
                synchronized(mRecentAudioSnapshot) {
                    snapshot = new ArrayList<>(mRecentAudioSnapshot);
                }
                if (snapshot.size() > 0 && !mGeminiDisabled) {
                    analyzeFailureWithGemini(snapshot);
                } else {
                    mLog.warn("No audio data to analyze. Triggering JMBE flush fallback.");
                    triggerJmbeFlush();
                }
                mLastTriggerTime.set(now);
                mLastAudioDataTime.set(now); // Reset timer
            }
        } catch (Exception e) {
            //A watchdog must never let an exception kill its own scheduler.
            mLog.error("AudioWatchdogService check failed", e);
        }
    }

    /**
     * Records a Gemini call failure and disables further calls once the failure limit is reached so a
     * misconfigured/unreachable endpoint (e.g. HTTP 404) cannot drive a synchronous-HTTP retry storm.
     */
    private void noteGeminiFailure(String reason) {
        int failures = mGeminiFailureCount.incrementAndGet();
        if (failures >= GEMINI_FAILURE_LIMIT && !mGeminiDisabled) {
            mGeminiDisabled = true;
            mLog.warn("Disabling Gemini audio triage after {} consecutive failures ({}). Using local JMBE flush fallback from now on.",
                    failures, reason);
        }
    }

    private void analyzeFailureWithGemini(List<float[]> snapshot) {
        mLog.info("Using Gemini to differentiate transient noise vs. terminal failure...");
        try {
            String apiKey = mUserPreferences.getAIPreference().getGeminiApiKey();
            if (apiKey == null || apiKey.isEmpty()) {
                mLog.warn("No Gemini API key. Disabling Gemini audio triage; using local JMBE Flush.");
                mGeminiDisabled = true;
                triggerJmbeFlush();
                return;
            }
            
            String model = mUserPreferences.getAIPreference().getGeminiModel();
            if (model == null || model.isEmpty()) model = "models/gemini-2.5-pro";
            //Normalize so the path has exactly one "models/" segment regardless of how the
            //model id is stored (the preference default already includes the "models/" prefix).
            if (!model.startsWith("models/")) model = "models/" + model;

            String url = "https://generativelanguage.googleapis.com/v1beta/" + model + ":generateContent?key=" + apiKey;
            
            byte[] wavBytes = WavUtil.floatsToWav(snapshot, 48000);
            String base64Data = Base64.getEncoder().encodeToString(wavBytes);

            Gson gson = new Gson();
            JsonObject payload = new JsonObject();
            JsonArray contents = new JsonArray();
            JsonObject contentObj = new JsonObject();
            JsonArray parts = new JsonArray();

            JsonObject textPart = new JsonObject();
            textPart.addProperty("text", "Analyze this audio snapshot. It has been zero or near-zero amplitude for 3 seconds. Determine if this is 'TRANSIENT_NOISE' (e.g. natural signal fade) or a 'TERMINAL_FAILURE' (e.g. decoder locked up). Respond with only one of those two phrases.");
            parts.add(textPart);

            JsonObject inlineDataObj = new JsonObject();
            JsonObject inlineData = new JsonObject();
            inlineData.addProperty("mimeType", "audio/wav");
            inlineData.addProperty("data", base64Data);
            inlineDataObj.add("inlineData", inlineData);
            parts.add(inlineDataObj);

            contentObj.add("parts", parts);
            contents.add(contentObj);
            payload.add("contents", contents);

            String jsonPayload = gson.toJson(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = mHttpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                mGeminiFailureCount.set(0);
                JsonObject responseJson = gson.fromJson(response.body(), JsonObject.class);
                JsonArray candidates = responseJson.getAsJsonArray("candidates");
                if (candidates != null && candidates.size() > 0) {
                    JsonObject content = candidates.get(0).getAsJsonObject().getAsJsonObject("content");
                    String resultText = content.getAsJsonArray("parts").get(0).getAsJsonObject().get("text").getAsString().trim();
                    mLog.info("Gemini analysis result: " + resultText);

                    if (resultText.contains("TERMINAL_FAILURE")) {
                        mLog.error("Terminal failure confirmed by AI. Triggering JMBE flush.");
                        triggerJmbeFlush();
                    } else {
                        mLog.info("AI determined this is transient noise. Ignoring.");
                    }
                }
            } else {
                mLog.warn("Gemini API error (HTTP {}). Defaulting to JMBE Flush.", response.statusCode());
                noteGeminiFailure("HTTP " + response.statusCode());
                triggerJmbeFlush();
            }

        } catch (Exception e) {
            mLog.error("Error analyzing failure with Gemini", e);
            noteGeminiFailure(e.getClass().getSimpleName());
            triggerJmbeFlush();
        }
    }

    private void triggerSoftUsbReset() {
        mLog.info(">>> TRIGGERING SOFT USB RESET <<<");
        // Implementation would hook into LibUsb reset
    }

    private void triggerJmbeFlush() {
        mLog.info(">>> TRIGGERING JMBE FLUSH <<<");
        // Implementation would hook into JMBE decoder buffer clear
    }
}
