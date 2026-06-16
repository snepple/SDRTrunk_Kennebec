package io.github.dsheirer.module.decode.nbfm.ai;

import io.github.dsheirer.module.decode.nbfm.DecodeConfigNBFM;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.module.decode.nbfm.ai.WavUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;


public class AIAudioOptimizer {
    private static final Logger mLog = LoggerFactory.getLogger(AIAudioOptimizer.class);
    private final UserPreferences mUserPreferences;
    private final HttpClient mHttpClient;

    private static final java.util.Map<String, AIAnalysisResult> mAnalysisCache = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<String, AIAnalysisResult> mRawAnalysisCache = new java.util.concurrent.ConcurrentHashMap<>();

    //Circuit breaker: after CIRCUIT_BREAKER_FAILURE_THRESHOLD consecutive API failures, fail fast for
    //CIRCUIT_BREAKER_COOLDOWN_MS instead of blocking the caller for the full network timeout on every
    //attempt.  This ensures a Gemini outage or invalid API key cannot repeatedly stall audio processing.
    private static final int CIRCUIT_BREAKER_FAILURE_THRESHOLD = 3;
    private static final long CIRCUIT_BREAKER_COOLDOWN_MS = 10 * 60 * 1000; //10 minutes
    private static final java.util.concurrent.atomic.AtomicInteger mConsecutiveFailures =
        new java.util.concurrent.atomic.AtomicInteger();
    private static volatile long mCircuitOpenUntil = 0;

    /**
     * Throws immediately if the circuit breaker is open due to repeated API failures.
     */
    private static void checkCircuitBreaker() throws Exception {
        if (System.currentTimeMillis() < mCircuitOpenUntil) {
            throw new Exception("AI audio optimization is temporarily disabled after " +
                CIRCUIT_BREAKER_FAILURE_THRESHOLD + " consecutive Gemini API failures - will retry after cooldown");
        }
    }

    /**
     * Records an API call outcome, opening the circuit breaker after repeated consecutive failures.
     */
    private static void recordApiOutcome(boolean success) {
        if (success) {
            mConsecutiveFailures.set(0);
        }
        else if (mConsecutiveFailures.incrementAndGet() >= CIRCUIT_BREAKER_FAILURE_THRESHOLD) {
            mCircuitOpenUntil = System.currentTimeMillis() + CIRCUIT_BREAKER_COOLDOWN_MS;
            mConsecutiveFailures.set(0);
            mLog.warn("AI audio optimization disabled for " + (CIRCUIT_BREAKER_COOLDOWN_MS / 60000) +
                " minutes after repeated Gemini API failures");
        }
    }

    private String getCacheKey(List<Path> audioFiles) {
        StringBuilder sb = new StringBuilder();
        for (Path path : audioFiles) {
            sb.append(path.toAbsolutePath().toString())
              .append(":")
              .append(path.toFile().lastModified())
              .append(";");
        }
        return sb.toString();
    }

    private String getRawCacheKey(List<List<float[]>> audioEvents) {
        StringBuilder sb = new StringBuilder();
        sb.append(audioEvents.size()).append("-");
        for (List<float[]> event : audioEvents) {
            sb.append(event.size()).append("-");
            if (!event.isEmpty() && event.get(0).length > 0) {
                sb.append(event.get(0)[0]).append(";");
            }
        }
        return sb.toString();
    }

    public AIAudioOptimizer(UserPreferences userPreferences) {
        mUserPreferences = userPreferences;
        mHttpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public AIAnalysisResult analyze(DecodeConfigNBFM config, List<Path> audioFiles) throws Exception {
        String apiKey = mUserPreferences.getAIPreference().getGeminiApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            throw new Exception("Gemini API Key is missing. Cannot optimize audio.");
        }

        String cacheKey = getCacheKey(audioFiles);
        if (mAnalysisCache.containsKey(cacheKey)) {
            mLog.info("Returning cached audio analysis result.");
            return mAnalysisCache.get(cacheKey);
        }

        checkCircuitBreaker();

        mLog.info("Analyzing audio with Gemini...");

        try {
            String model = mUserPreferences.getAIPreference().getGeminiModel();
            String baseUrl = System.getProperty("gemini.api.url", "https://generativelanguage.googleapis.com");
            String url = baseUrl + "/v1beta/" + normalizeModelPath(model) + ":generateContent?key=" + apiKey;

            String promptText = "Role & Objective\n" +
                "You are an expert audio engineer specializing in LMR (Land Mobile Radio) and NBFM communication systems. Your task is to analyze an array of the 5 most recent audio recordings from a specific radio channel and determine the optimal DSP (Digital Signal Processing) filter settings to maximize human vocal clarity and eliminate background noise.\n\n" +
                "Audio Characteristics to Analyze & Filter Mapping Rules:\n\n" +
                "Squelch Tail (Head Trim, Tail Trim, Hangtime)\n" +
                "What to listen for: Listen specifically to the first 100ms and the last 500ms of each file. Look for a \"squelch crash\"—a sharp, loud burst of static/white noise immediately following the end of speech, or a mechanical \"click/pop\" right before speech begins.\n" +
                "Action: If you hear a static burst at the end, set squelchTailRemovalEnabled = True. Set squelchTailRemovalMs to the duration of the burst (typically 100 to 250 ms). If there is an opening click, set squelchHeadRemovalMs (typically 25 to 75 ms). Set noiseGateHoldTime to bridge micro-drops in signal (typically 0 unless the audio is stuttering mid-transmission).\n\n" +
                "Low-Pass Filter\n" +
                "What to listen for: Frequencies above 4000 Hz. Human speech over NBFM rarely exceeds 3500 Hz. Listen for high-pitched whines, digital control channel bleed, or shrill, ear-piercing static.\n" +
                "Action: If high-frequency interference is present, set lowPassEnabled = True. Set lowPassCutoff between 3000 Hz and 3500 Hz to shave off the top-end noise without muffling the consonants of the human voice.\n\n" +
                "Hiss Reduction\n" +
                "What to listen for: A continuous, broadband \"sssss\" sound (white noise) that sits underneath the human voice. This is common in weak FM signals.\n" +
                "Action: If a continuous hiss is detected, set hissReductionEnabled = True. Set hissReductionCorner to the approximate start of the hiss (usually 2000 Hz to 2500 Hz). Set hissReductionDb to a negative dB value. Start conservatively at -3.0 dB to -6.0 dB. Avoid going past -9.0 dB unless the hiss is extreme, as heavy reduction causes an underwater, phasing artifact.\n\n" +
                "Bass Boost\n" +
                "What to listen for: Audio that sounds excessively \"thin\", \"tinny\", or like it is coming through a cheap telephone. This means the low-end fundamental frequencies of the speaker's voice (100 Hz - 300 Hz) are missing.\n" +
                "Action: If the voice lacks warmth/depth, set bassBoostEnabled = True. Set bassBoostDb between +2.0 dB and +5.0 dB. If the audio is already muddy or booming, leave this Disabled.\n\n" +
                "Voice Enhancement\n" +
                "What to listen for: Voices that are muffled, lack intelligibility, or are buried beneath engine noise, sirens, or wind.\n" +
                "Action: If vocal clarity is low, set agcEnabled = True. Set agcTargetLevel (mapped to Amount) to a percentage (store as float, e.g. 25.0 to 75.0 mapped to target level). Use 25 for minor clarification and 50 - 75 for heavily obscured voices.\n\n" +
                "Squelch / Noise Gate\n" +
                "What to listen for: Periods of \"dead air\" where no one is speaking, but a low-level static or hum is still broadcasting. Calculate the volume delta between active speech (Peak RMS) and the quietest moments (Noise Floor RMS).\n" +
                "Action: If there is audible noise between words, set noiseGateEnabled = True. Set noiseGateThreshold just above the noise floor (usually between 2.0 and 5.0). Set noiseGateReduction high (e.g., 0.80 to 1.0) to aggressively mute the dead air. Set noiseGateHoldTime (Delay) to 250 to 500 ms to ensure the gate doesn't slam shut during natural pauses between words.\n\n" +
                "Output Gain\n" +
                "What to listen for: The overall average loudness of the transmissions.\n" +
                "Action: Determine a multiplier to normalize the audio to a standard listening volume (e.g., targeting -3dB Peak). If the audio is very quiet, set agcMaxGain > 1.0. If it is clipping/distorted, set agcMaxGain < 1.0.\n\n" +
                "Return JSON with these exact fields: hissReductionEnabled (boolean), hissReductionDb (float), hissReductionCorner (double), lowPassEnabled (boolean), lowPassCutoff (double), deemphasisEnabled (boolean), bassBoostEnabled (boolean), bassBoostDb (float), agcEnabled (boolean), agcTargetLevel (float), noiseGateEnabled (boolean), noiseGateThreshold (float), noiseGateReduction (float), agcMaxGain (float), squelchTailRemovalEnabled (boolean), squelchTailRemovalMs (int), squelchHeadRemovalMs (int), noiseGateHoldTime (int), issuesFound (string), improvements (string), explanation (string).";

            Gson gson = new Gson();
            JsonObject payload = new JsonObject();
            JsonArray contents = new JsonArray();
            JsonObject contentObj = new JsonObject();
            JsonArray parts = new JsonArray();

            // Add system instruction as part
            JsonObject textPart = new JsonObject();
            textPart.addProperty("text", promptText);
            parts.add(textPart);

            // Add audio files
            for (Path path : audioFiles) {
                byte[] fileBytes = Files.readAllBytes(path);
                String base64Data = Base64.getEncoder().encodeToString(fileBytes);
                String mimeType = path.toString().toLowerCase().endsWith(".mp3") ? "audio/mp3" : "audio/wav";

                JsonObject inlineDataObj = new JsonObject();
                JsonObject inlineData = new JsonObject();
                inlineData.addProperty("mimeType", mimeType);
                inlineData.addProperty("data", base64Data);
                inlineDataObj.add("inlineData", inlineData);
                parts.add(inlineDataObj);
            }

            contentObj.add("parts", parts);
            contents.add(contentObj);
            payload.add("contents", contents);

            // Force JSON response
            JsonObject generationConfig = new JsonObject();
            generationConfig.addProperty("responseMimeType", "application/json");
            payload.add("generationConfig", generationConfig);

            String jsonPayload = gson.toJson(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = mHttpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                if (response.statusCode() == 429 || response.body().contains("RESOURCE_EXHAUSTED")) {
                    String fallbackModel = getFallbackModel(model);
                    if (fallbackModel != null) {
                        mUserPreferences.getAIPreference().setGeminiModel(fallbackModel);
                        throw new Exception("Gemini API quota exhausted for " + model + ". The system has automatically downgraded to " + fallbackModel + " for the next request. Please retry.");
                    } else {
                        throw new Exception("Gemini API quota exhausted for " + model + " and no further fallback models are available. Please check your API quotas.");
                    }
                }
                throw new Exception("Gemini API error: " + response.statusCode() + " " + response.body());
            }

            JsonObject responseJson = gson.fromJson(response.body(), JsonObject.class);
            JsonArray candidates = responseJson.getAsJsonArray("candidates");
            if (candidates == null || candidates.size() == 0) {
                throw new Exception("No candidates returned from Gemini");
            }
            JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
            JsonObject content = firstCandidate.getAsJsonObject("content");
            JsonArray responseParts = content.getAsJsonArray("parts");
            String resultText = responseParts.get(0).getAsJsonObject().get("text").getAsString();

            // Parse the result JSON into AIAnalysisResult
            AIAnalysisResult result = gson.fromJson(resultText, AIAnalysisResult.class);
            mAnalysisCache.put(cacheKey, result);
            if (mAnalysisCache.size() > 100) {
                mAnalysisCache.clear();
            }
            recordApiOutcome(true);
            return result;

        } catch (Exception e) {
            recordApiOutcome(false);
            mLog.error("Error calling Gemini API: " + e.getMessage(), e);
            throw new Exception("Error calling Gemini API: " + e.getMessage(), e);
        }
    }

    public AIAnalysisResult analyzeRawAudio(DecodeConfigNBFM config, List<List<float[]>> audioEvents) throws Exception {
        String apiKey = mUserPreferences.getAIPreference().getGeminiApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            throw new Exception("Gemini API key is not configured in settings");
        }

        String cacheKey = getRawCacheKey(audioEvents);
        if (mRawAnalysisCache.containsKey(cacheKey)) {
            mLog.info("Returning cached raw audio analysis result.");
            return mRawAnalysisCache.get(cacheKey);
        }

        checkCircuitBreaker();

        String model = mUserPreferences.getAIPreference().getGeminiModel();
        if (model == null || model.isEmpty()) {
            model = "gemini-2.5-pro";
        }
        String baseUrl = System.getProperty("gemini.api.url", "https://generativelanguage.googleapis.com");
        String url = baseUrl + "/v1beta/" + normalizeModelPath(model) + ":generateContent?key=" + apiKey;

        try {
            String promptText = "You are an expert RF DSP engineer and Audio DSP specialist configuring settings for an SDRTrunk Narrow-Band FM channel.\n" +
                "You are provided with up to 5 of the most recent raw audio recordings from this channel. These recordings contain the current DSP state applied by the user (which you must analyze) AND the intrinsic characteristics of the radio signal itself.\n" +
                "Analyze the audio across all clips and suggest an optimal set of filters and settings to maximize human vocal intelligibility and minimize noise/hiss.\n\n" +
                "Here is the context of the user's current settings:\n" +
                "hissReductionEnabled: " + config.isHissReductionEnabled() + "\n" +
                "hissReductionDb: " + config.getHissReductionDb() + "\n" +
                "hissReductionCornerHz: " + config.getHissReductionCornerHz() + "\n" +
                "lowPassEnabled: " + config.isLowPassEnabled() + "\n" +
                "lowPassCutoff: " + config.getLowPassCutoff() + "\n" +
                "deemphasisEnabled: " + config.isDeemphasisEnabled() + "\n" +
                "bassBoostEnabled: " + config.isBassBoostEnabled() + "\n" +
                "bassBoostDb: " + config.getBassBoostDb() + "\n" +
                "agcEnabled: " + config.isAgcEnabled() + "\n" +
                "agcTargetLevel: " + config.getAgcTargetLevel() + "\n" +
                "noiseGateEnabled: " + config.isNoiseGateEnabled() + "\n" +
                "noiseGateThreshold: " + config.getNoiseGateThreshold() + "\n" +
                "noiseGateReduction: " + config.getNoiseGateReduction() + "\n" +
                "agcMaxGain: " + config.getAgcMaxGain() + "\n" +
                "squelchTailRemovalEnabled: " + config.isSquelchTailRemovalEnabled() + "\n" +
                "squelchTailRemovalMs: " + config.getSquelchTailRemovalMs() + "\n" +
                "squelchHeadRemovalMs: " + config.getSquelchHeadRemovalMs() + "\n" +
                "noiseGateHoldTime: " + config.getNoiseGateHoldTime() + "\n\n" +
                "Return JSON with these exact fields: hissReductionEnabled (boolean), hissReductionDb (float), hissReductionCorner (double), lowPassEnabled (boolean), lowPassCutoff (double), deemphasisEnabled (boolean), bassBoostEnabled (boolean), bassBoostDb (float), agcEnabled (boolean), agcTargetLevel (float), noiseGateEnabled (boolean), noiseGateThreshold (float), noiseGateReduction (float), agcMaxGain (float), squelchTailRemovalEnabled (boolean), squelchTailRemovalMs (int), squelchHeadRemovalMs (int), noiseGateHoldTime (int), issuesFound (string), improvements (string), explanation (string).";

            Gson gson = new Gson();
            JsonObject payload = new JsonObject();
            JsonArray contents = new JsonArray();
            JsonObject contentObj = new JsonObject();
            JsonArray parts = new JsonArray();

            JsonObject textPart = new JsonObject();
            textPart.addProperty("text", promptText);
            parts.add(textPart);

            for (List<float[]> event : audioEvents) {
                byte[] wavBytes = WavUtil.floatsToWav(event, 48000);
                String base64Data = Base64.getEncoder().encodeToString(wavBytes);
                
                JsonObject inlineDataObj = new JsonObject();
                JsonObject inlineData = new JsonObject();
                inlineData.addProperty("mimeType", "audio/wav");
                inlineData.addProperty("data", base64Data);
                inlineDataObj.add("inlineData", inlineData);
                parts.add(inlineDataObj);
            }

            contentObj.add("parts", parts);
            contents.add(contentObj);
            payload.add("contents", contents);

            JsonObject generationConfig = new JsonObject();
            generationConfig.addProperty("responseMimeType", "application/json");
            payload.add("generationConfig", generationConfig);

            String jsonPayload = gson.toJson(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = mHttpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                if (response.statusCode() == 429 || response.body().contains("RESOURCE_EXHAUSTED")) {
                    String fallbackModel = getFallbackModel(model);
                    if (fallbackModel != null) {
                        mUserPreferences.getAIPreference().setGeminiModel(fallbackModel);
                        throw new Exception("Gemini API quota exhausted for " + model + ". The system has automatically downgraded to " + fallbackModel + " for the next request. Please retry.");
                    } else {
                        throw new Exception("Gemini API quota exhausted for " + model + " and no further fallback models are available. Please check your API quotas.");
                    }
                }
                throw new Exception("Gemini API error: " + response.statusCode() + " " + response.body());
            }

            JsonObject responseJson = gson.fromJson(response.body(), JsonObject.class);
            JsonArray candidates = responseJson.getAsJsonArray("candidates");
            if (candidates == null || candidates.size() == 0) {
                throw new Exception("No candidates returned from Gemini");
            }
            JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
            JsonObject content = firstCandidate.getAsJsonObject("content");
            JsonArray responseParts = content.getAsJsonArray("parts");
            String resultText = responseParts.get(0).getAsJsonObject().get("text").getAsString();

            AIAnalysisResult result = gson.fromJson(resultText, AIAnalysisResult.class);
            mRawAnalysisCache.put(cacheKey, result);
            if (mRawAnalysisCache.size() > 100) {
                mRawAnalysisCache.clear();
            }
            recordApiOutcome(true);
            return result;

        } catch (Exception e) {
            recordApiOutcome(false);
            mLog.error("Error calling Gemini API: " + e.getMessage(), e);
            throw new Exception("Error calling Gemini API: " + e.getMessage(), e);
        }
    }

    /**
     * Normalizes a Gemini model id into the exactly-one-"models/"-prefix path segment the REST API expects,
     * regardless of whether the stored preference already includes the prefix. Avoids 404s caused by either
     * a missing prefix or a doubled "models/models/" path.
     */
    private static String normalizeModelPath(String model) {
        if (model == null || model.isEmpty()) {
            model = "gemini-2.5-pro";
        }
        while (model.startsWith("models/")) {
            model = model.substring("models/".length());
        }
        return "models/" + model;
    }

    private String getFallbackModel(String currentModel) {
        if (currentModel == null) return null;
        if (currentModel.contains("-pro")) {
            return currentModel.replace("-pro", "-thinking");
        } else if (currentModel.contains("-thinking")) {
            return currentModel.replace("-thinking", "-flash");
        }
        return null;
    }
}