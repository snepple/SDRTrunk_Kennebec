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

public class AIAudioMonitorAnalyzer
{
    private static final Logger mLog = LoggerFactory.getLogger(AIAudioMonitorAnalyzer.class);

    private static final String SYSTEM_INSTRUCTION =
        "You are an automated telecommunication health monitor for the Kennebec County public safety radio network. " +
        "Your role is strictly to classify incoming audio. If the audio is analog static, digital control channel " +
        "motorboating, or empty white noise, you must set is_valid_transmission to false.";

    private static final java.util.Map<String, RadioCallValidation> mAnalysisCache =
        new java.util.concurrent.ConcurrentHashMap<>();

    private final UserPreferences mUserPreferences;
    private final HttpClient mHttpClient;
    private final Gson mGson = new Gson();

    public AIAudioMonitorAnalyzer(UserPreferences userPreferences)
    {
        mUserPreferences = userPreferences;
        mHttpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    private String getCacheKey(Path audioFile)
    {
        return audioFile.toAbsolutePath() + ":" + audioFile.toFile().lastModified();
    }

    /**
     * Classifies a single recording using Gemini structured JSON output.
     */
    public RadioCallValidation analyze(Path audioFile) throws Exception
    {
        String apiKey = mUserPreferences.getAIPreference().getGeminiApiKey();
        if(apiKey == null || apiKey.isEmpty())
        {
            throw new Exception("Gemini API Key is missing. Cannot perform AI audio monitoring.");
        }

        String cacheKey = getCacheKey(audioFile);
        if(mAnalysisCache.containsKey(cacheKey))
        {
            mLog.info("Returning cached audio monitor result for {}", audioFile.getFileName());
            return mAnalysisCache.get(cacheKey);
        }

        mLog.info("Analyzing audio with Gemini for AI monitoring: {}", audioFile.getFileName());

        String model = mUserPreferences.getAIPreference().getGeminiModel();
        String baseUrl = System.getProperty("gemini.api.url", "https://generativelanguage.googleapis.com");
        String url = baseUrl + "/v1beta/" + model + ":generateContent?key=" + apiKey;

        JsonObject payload = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject contentObj = new JsonObject();
        JsonArray parts = new JsonArray();

        JsonObject textPart = new JsonObject();
        textPart.addProperty("text",
            "Classify this radio audio recording. Determine whether it contains valid voice communications or " +
            "appropriate dispatch signaling, or is only static, interference, or silence.");
        parts.add(textPart);

        byte[] fileBytes = Files.readAllBytes(audioFile);
        String base64Data = Base64.getEncoder().encodeToString(fileBytes);
        String mimeType = audioFile.toString().toLowerCase().endsWith(".mp3") ? "audio/mp3" : "audio/wav";

        JsonObject inlineDataObj = new JsonObject();
        JsonObject inlineData = new JsonObject();
        inlineData.addProperty("mimeType", mimeType);
        inlineData.addProperty("data", base64Data);
        inlineDataObj.add("inlineData", inlineData);
        parts.add(inlineDataObj);

        contentObj.add("parts", parts);
        contents.add(contentObj);
        payload.add("contents", contents);

        JsonObject systemInstruction = new JsonObject();
        JsonArray systemParts = new JsonArray();
        JsonObject systemText = new JsonObject();
        systemText.addProperty("text", SYSTEM_INSTRUCTION);
        systemParts.add(systemText);
        systemInstruction.add("parts", systemParts);
        payload.add("systemInstruction", systemInstruction);

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("responseMimeType", "application/json");
        generationConfig.addProperty("temperature", 0.0);
        generationConfig.add("responseSchema", buildResponseSchema());
        payload.add("generationConfig", generationConfig);

        String jsonPayload = mGson.toJson(payload);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(30))
            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
            .build();

        HttpResponse<String> response = mHttpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if(response.statusCode() != 200)
        {
            if(response.statusCode() == 429 || response.body().contains("RESOURCE_EXHAUSTED"))
            {
                String fallbackModel = getFallbackModel(model);
                if(fallbackModel != null)
                {
                    mUserPreferences.getAIPreference().setGeminiModel(fallbackModel);
                    throw new Exception("Gemini API quota exhausted for " + model +
                        ". The system has automatically downgraded to " + fallbackModel + " for the next request.");
                }
                throw new Exception("Gemini API quota exhausted for " + model +
                    " and no further fallback models are available.");
            }
            throw new Exception("Gemini API error: " + response.statusCode() + " " + response.body());
        }

        RadioCallValidation validation = parseResponse(response.body());
        mAnalysisCache.put(cacheKey, validation);
        if(mAnalysisCache.size() > 200)
        {
            mAnalysisCache.clear();
        }
        return validation;
    }

    /**
     * Legacy batch entry point retained for tests. Returns true when every analyzed file is valid.
     */
    public boolean analyze(List<Path> audioFiles) throws Exception
    {
        if(audioFiles == null || audioFiles.isEmpty())
        {
            return true;
        }

        for(Path path : audioFiles)
        {
            RadioCallValidation validation = analyze(path);
            if(!validation.isValidTransmission())
            {
                return false;
            }
        }
        return true;
    }

    private JsonObject buildResponseSchema()
    {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");

        JsonObject properties = new JsonObject();

        JsonObject validField = new JsonObject();
        validField.addProperty("type", "boolean");
        properties.add("is_valid_transmission", validField);

        JsonObject speechField = new JsonObject();
        speechField.addProperty("type", "boolean");
        properties.add("contains_human_speech", speechField);

        JsonObject profileField = new JsonObject();
        profileField.addProperty("type", "string");
        profileField.add("enum", mGson.toJsonTree(new String[]{
            "clear_speech", "heavy_static", "digital_noise", "silence"
        }));
        properties.add("audio_acoustic_profile", profileField);

        JsonObject confidenceField = new JsonObject();
        confidenceField.addProperty("type", "number");
        properties.add("confidence_score", confidenceField);

        JsonObject summaryField = new JsonObject();
        summaryField.addProperty("type", "string");
        properties.add("transcript_summary", summaryField);

        schema.add("properties", properties);

        JsonArray required = new JsonArray();
        required.add("is_valid_transmission");
        required.add("contains_human_speech");
        required.add("audio_acoustic_profile");
        required.add("confidence_score");
        required.add("transcript_summary");
        schema.add("required", required);

        return schema;
    }

    private RadioCallValidation parseResponse(String responseBody) throws Exception
    {
        JsonObject responseJson = mGson.fromJson(responseBody, JsonObject.class);
        JsonArray candidates = responseJson.getAsJsonArray("candidates");
        if(candidates == null || candidates.isEmpty())
        {
            throw new Exception("No candidates returned from Gemini");
        }

        JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
        JsonObject content = firstCandidate.getAsJsonObject("content");
        JsonArray responseParts = content.getAsJsonArray("parts");
        String resultText = responseParts.get(0).getAsJsonObject().get("text").getAsString();

        JsonObject resultObj = mGson.fromJson(resultText, JsonObject.class);

        if(resultObj.has("is_valid_transmission"))
        {
            return new RadioCallValidation(
                resultObj.get("is_valid_transmission").getAsBoolean(),
                resultObj.has("contains_human_speech") && resultObj.get("contains_human_speech").getAsBoolean(),
                resultObj.has("audio_acoustic_profile") ? resultObj.get("audio_acoustic_profile").getAsString() : "silence",
                resultObj.has("confidence_score") ? resultObj.get("confidence_score").getAsDouble() : 0.0,
                resultObj.has("transcript_summary") ? resultObj.get("transcript_summary").getAsString() : ""
            );
        }

        if(resultObj.has("functional"))
        {
            boolean functional = resultObj.get("functional").getAsBoolean();
            return new RadioCallValidation(functional, functional, functional ? "clear_speech" : "heavy_static",
                functional ? 1.0 : 0.0, "");
        }

        throw new Exception("Unexpected response format from Gemini: " + resultText);
    }

    private String getFallbackModel(String currentModel)
    {
        if(currentModel == null)
        {
            return null;
        }
        if(currentModel.contains("-pro"))
        {
            return currentModel.replace("-pro", "-thinking");
        }
        if(currentModel.contains("-thinking"))
        {
            return currentModel.replace("-thinking", "-flash");
        }
        return null;
    }
}
