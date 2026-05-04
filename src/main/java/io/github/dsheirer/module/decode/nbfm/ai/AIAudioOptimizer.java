package io.github.dsheirer.module.decode.nbfm.ai;

import io.github.dsheirer.module.decode.nbfm.DecodeConfigNBFM;
import io.github.dsheirer.preference.UserPreferences;
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

        mLog.info("Analyzing audio with Gemini...");

        try {
            String model = mUserPreferences.getAIPreference().getGeminiModel();
            String url = "https://generativelanguage.googleapis.com/v1beta/" + model + ":generateContent?key=" + apiKey;

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
            return result;

        } catch (Exception e) {
            mLog.error("Error calling Gemini API: " + e.getMessage(), e);
            throw new Exception("Error calling Gemini API: " + e.getMessage(), e);
        }
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