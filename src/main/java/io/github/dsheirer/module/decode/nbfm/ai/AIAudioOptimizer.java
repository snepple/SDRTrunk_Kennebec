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

            String promptText = "Analyze this NBFM radio audio. Return JSON with recommended settings. " +
                "Adjust hissReductionEnabled (boolean), hissReductionDb (float), hissReductionCorner (double), lowPassEnabled (boolean), lowPassCutoff (float), deemphasisEnabled (boolean), bassBoostEnabled (boolean), bassBoostDb (float), agcEnabled (boolean), agcTargetLevel (float), noiseGateEnabled (boolean), noiseGateThreshold (float), noiseGateReduction (float), and agcMaxGain (float) based on SNR and voice clarity. " +
                "Provide a brief plain-English explanation for the adjustments in an 'explanation' field. Also provide 'issuesFound' and 'improvements' fields.";

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
}
