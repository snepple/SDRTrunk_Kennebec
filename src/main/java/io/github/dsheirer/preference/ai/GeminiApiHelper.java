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

            String cleanTranscript = transcript.replace("\\", "\\\\").replace("\"", "'").replace("\n", " ").replace("\r", " ");

            String prompt = "You are a specialized Named Entity Recognition model calibrated exclusively for U.S. public-safety " +
                    "two-tone paging dispatch. A two-tone sequential page was just transmitted on a radio channel, " +
                    "immediately followed by a voice dispatch. The (often noisy, band-limited) speech-to-text transcript of " +
                    "that dispatch is: '" + cleanTranscript + "'. " +
                    "Your job: extract the specific field unit(s), station(s), apparatus, or department(s) being paged or " +
                    "dispatched, and return a clean, human-readable name. " +
                    "RULE 1 - Ignore dispatch centers. Dispatchers constantly identify themselves; never extract or output a " +
                    "dispatch-center name. Ignore identifiers such as: Dispatch, Fire Alarm / Alarm, Comm Center, " +
                    "Communications, Med-Comm, Control / County Control, Central / Central Dispatch, Base, and bare " +
                    "geographic names acting as the dispatch entity. " +
                    "RULE 2 - Recognize dispatch syntax. The paged entity is typically wrapped in rigid phrasing: " +
                    "Direct address/callout ('Fire Alarm to Yarmouth Chief 801, respond...' -> 'Yarmouth Chief 801'); " +
                    "Direct assignment of several units ('Engine 4, Rescue 1, respond to 123 Main Street...' -> 'Engine 4, Rescue 1'); " +
                    "Test announcement ('This is the nightly pager test for the Centerville Volunteer Fire Department.' -> 'Centerville Volunteer Fire Department'); " +
                    "Clearance/status ('County Dispatch clearing Station 7 EMS.' -> 'Station 7 EMS'); " +
                    "Nature-type driven ('Structure fire box 44, Engine 1 and Truck 2, respond.' -> 'Engine 1, Truck 2'). " +
                    "RULE 3 - Correct phonetic hallucinations using incident context. Band-limited radio audio mis-transcribes " +
                    "unit names; use the surrounding words to repair them. e.g. 'Has Matt Two, respond to the chemical spill' " +
                    "-> 'Hazmat 2'; 'Clint Four' near firefighting context -> 'Quint 4'. " +
                    "RULE 4 - Reconstruct truncated names. A slow squelch or late mic key often clips the first syllable. Use " +
                    "syntax to rebuild the most probable name: '...escue 4, respond to the motor vehicle accident' -> 'Rescue 4'. " +
                    "RULE 5 - Stacked paging. If the dispatch clearly names multiple distinct units for one incident, combine " +
                    "them into a single composite name joined with ' / ' and suffixed with ' Dispatch', e.g. " +
                    "'Engine 1, Engine 2, and Rescue 5, respond...' -> 'Engine 1 / Engine 2 / Rescue 5 Dispatch'. " +
                    "RULE 6 - Graceful degradation. If the transcript is unintelligible, contains only a general bulletin with no " +
                    "nameable unit (e.g. 'Attention all units, severe weather warning...'), or your confidence is low, respond " +
                    "with exactly 'UNKNOWN' rather than guessing a hallucinated name. " +
                    "OUTPUT: Respond with ONLY the standardized, descriptive unit/agency name (e.g. 'Engine 52', " +
                    "'Springfield Fire', or 'Engine 1 / Engine 2 / Rescue 5 Dispatch'), or 'UNKNOWN'. No quotes, no extra words.";
            
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
