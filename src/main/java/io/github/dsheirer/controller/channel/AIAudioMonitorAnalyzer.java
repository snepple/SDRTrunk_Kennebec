package io.github.dsheirer.controller.channel;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.dsheirer.preference.UserPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

public class AIAudioMonitorAnalyzer {
    private static final Logger mLog = LoggerFactory.getLogger(AIAudioMonitorAnalyzer.class);

    private UserPreferences mUserPreferences;
    private HttpClient mHttpClient;

    public AIAudioMonitorAnalyzer(UserPreferences userPreferences) {
        mUserPreferences = userPreferences;
        mHttpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public boolean analyze(List<Path> audioFiles) throws Exception {
        String apiKey = mUserPreferences.getAIPreference().getGeminiApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            throw new Exception("Gemini API Key is missing. Cannot perform AI audio monitoring.");
        }

        mLog.info("Analyzing audio with Gemini for AI Monitoring...");

        try {
            String model = mUserPreferences.getAIPreference().getGeminiModel();
            String url = "https://generativelanguage.googleapis.com/v1beta/" + model + ":generateContent?key=" + apiKey;

            String promptText = "Analyze these five radio audio recordings. Confirm if human voices or digital trunking data/control signals are clearly audible. Respond with a boolean-like state: functional or unintelligible. Return JSON with the exact field: functional (boolean).";

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

            // Parse the result JSON
            JsonObject resultObj = gson.fromJson(resultText, JsonObject.class);
            if (resultObj.has("functional")) {
                return resultObj.get("functional").getAsBoolean();
            } else {
                throw new Exception("Unexpected response format from Gemini: " + resultText);
            }

        } catch (Exception e) {
            mLog.error("Error calling Gemini API for monitoring: " + e.getMessage(), e);
            throw new Exception("Error calling Gemini API for monitoring: " + e.getMessage(), e);
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
