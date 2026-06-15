package io.github.dsheirer.preference.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class GeminiApiHelper {
    private static final Logger mLog = LoggerFactory.getLogger(GeminiApiHelper.class);

    /**
     * Fetches and filters available Gemini models based on the provided API key.
     * Returns an empty list if the API key is invalid or if a network error occurs.
     */
    public static List<GeminiModel> fetchAvailableModels(String apiKey) {
        List<GeminiModel> modelsList = new ArrayList<>();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return modelsList;
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models?key=" + apiKey.trim()))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response.body());
                JsonNode models = root.get("models");

                if (models != null && models.isArray()) {
                    for (JsonNode modelNode : models) {
                        String name = modelNode.has("name") ? modelNode.get("name").asText() : "";
                        String version = modelNode.has("version") ? modelNode.get("version").asText() : "";
                        String displayName = modelNode.has("displayName") ? modelNode.get("displayName").asText() : "";
                        String description = modelNode.has("description") ? modelNode.get("description").asText() : "";
                        
                        List<String> supportedMethods = new ArrayList<>();
                        if (modelNode.has("supportedGenerationMethods") && modelNode.get("supportedGenerationMethods").isArray()) {
                            for (JsonNode methodNode : modelNode.get("supportedGenerationMethods")) {
                                supportedMethods.add(methodNode.asText());
                            }
                        }

                        // Filtering logic: must support generateContent, and exclude embedding/vision/aqa specific models
                        if (supportedMethods.contains("generateContent") && 
                            name.startsWith("models/gemini") && 
                            !name.contains("vision") && 
                            !name.contains("embedding") &&
                            !name.contains("aqa")) {
                            
                            modelsList.add(new GeminiModel(name, version, displayName, description, supportedMethods));
                        }
                    }
                }
            } else {
                mLog.warn("Gemini API test failed with HTTP " + response.statusCode() + " - " + response.body());
            }
        } catch (Exception e) {
            mLog.error("Error fetching Gemini models", e);
        }

        return modelsList;
    }

    /**
     * Queries Gemini to identify the paged agency from a transcript.
     * Returns the agency name or "UNKNOWN".
     */
    public static String extractAgencyFromTranscript(String apiKey, String modelName, String transcript) {
        if (apiKey == null || apiKey.trim().isEmpty() || transcript == null || transcript.trim().isEmpty()) {
            return "UNKNOWN";
        }
        if (modelName == null || modelName.trim().isEmpty()) {
            modelName = "models/gemini-1.5-flash";
        }
        
        try {
            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            String prompt = "A dispatch paging tone was just transmitted. The resulting transcript is: \\\"" + transcript.replace("\"", "\\\"") + "\\\". What specific fire, EMS, or police unit/agency is being dispatched? Respond with ONLY the unit/agency name (e.g., 'Engine 52' or 'Springfield Fire'), or 'UNKNOWN' if unclear.";
            
            String jsonPayload = "{\"contents\":[{\"parts\":[{\"text\":\"" + prompt + "\"}]}]}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/" + modelName + ":generateContent?key=" + apiKey.trim()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response.body());
                JsonNode candidates = root.get("candidates");
                if (candidates != null && candidates.isArray() && candidates.size() > 0) {
                    JsonNode content = candidates.get(0).get("content");
                    if (content != null) {
                        JsonNode parts = content.get("parts");
                        if (parts != null && parts.isArray() && parts.size() > 0) {
                            String text = parts.get(0).get("text").asText();
                            if (text != null) {
                                return text.trim();
                            }
                        }
                    }
                }
            } else {
                mLog.warn("Failed to extract agency from Gemini. HTTP " + response.statusCode() + " - " + response.body());
            }
        } catch (Exception e) {
            mLog.error("Error communicating with Gemini for agency extraction", e);
        }
        return "UNKNOWN";
    }
}
