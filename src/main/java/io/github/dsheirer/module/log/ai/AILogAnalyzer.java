package io.github.dsheirer.module.log.ai;

import io.github.dsheirer.preference.UserPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class AILogAnalyzer {
    private static final Logger mLog = LoggerFactory.getLogger(AILogAnalyzer.class);
    private final UserPreferences mUserPreferences;
    private io.github.dsheirer.preference.notification.SelfHealingOrchestrator mOrchestrator;

    private static final java.util.Map<String, String> mAnalysisCache = new java.util.concurrent.ConcurrentHashMap<>();
    private static final int MAX_TOKENS_PER_REQUEST = 4000;
    private static final int CHARS_PER_TOKEN = 4; // rough estimate
    private static final int MAX_CHARS = MAX_TOKENS_PER_REQUEST * CHARS_PER_TOKEN;

    public AILogAnalyzer(UserPreferences userPreferences) {
        mUserPreferences = userPreferences;
    }

    public void setOrchestrator(io.github.dsheirer.preference.notification.SelfHealingOrchestrator orchestrator) {
        mOrchestrator = orchestrator;
    }

    public String analyze(String logContent) throws Exception {
        String apiKey = mUserPreferences.getAIPreference().getGeminiApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new Exception("Gemini API Key is missing. Cannot analyze log.");
        }

        if (mAnalysisCache.containsKey(logContent)) {
            mLog.info("Returning cached log analysis result.");
            return mAnalysisCache.get(logContent);
        }

        mLog.info("Analyzing log content with Gemini...");

        try {
            // Enforce token budget — truncate to MAX_TOKENS_PER_REQUEST
            String budgetedContent = enforceTokenBudget(logContent);
            String model = mUserPreferences.getAIPreference().getGeminiModel();
            String baseUrl = System.getProperty("gemini.api.url", "https://generativelanguage.googleapis.com");
            String url = baseUrl + "/v1beta/" + model + ":generateContent?key=" + apiKey;

            // Simple json string escape to prevent malformed json payloads when log file contains quotes or new lines.
            String escapedLogContent = budgetedContent.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");

            String promptText = "You are the SDRTrunk System Health analyst. Analyze the following SDRTrunk application log and translate cryptic Java stack traces and radio-frequency telemetry into concise, plain-English diagnostics, each with a specific, actionable fix. Context: SDRTrunk is a Java software-defined-radio app that decodes trunked public-safety radio (APCO P25 Phase 1/2, DMR, NBFM) from USB SDR tuners (RTL-SDR, Airspy) via LibUSB, using a thread-per-channel polyphase/heterodyne channelizer. "
                    + "Use this failure taxonomy when you recognize matching signatures: "
                    + "(1) LibUSB -4/LIBUSB_ERROR_NO_DEVICE or -99 during USB discovery => missing/incorrect WinUSB driver, unplugged device, or permissions; advise installing the WinUSB driver via Zadig (Options > List All Devices, select the tuner bulk interface) or moving to a different USB controller. "
                    + "(2) java.lang.OutOfMemoryError WITH 'unable to create native thread' => OS thread limit saturated; advise reducing Max Traffic Channels, closing heavy apps (e.g. Chrome), and raising kernel.threads-max / ulimit -u. A plain heap OutOfMemoryError => advise raising the JVM heap (-Xms4096m -Xmx4096m, or -Xmx6G) sized to available RAM, and/or disabling the spectrum/waterfall. "
                    + "(3) 'processor overloaded', 'data over run on USB', 'transfer buffers exhausted', or dropped samples => CPU or USB bus saturation that corrupts the bitstream (garbled audio, CRC/false decodes); advise lowering the sample rate, fewer simultaneous channels, a dedicated USB controller, preferring the Polyphase channelizer, and disabling the waterfall. "
                    + "(4) 'SYNC LOSS' or cyclic IDLE on the control channel => lost RF lock from PPM oscillator drift, Automatic Gain Control front-end overload in a noisy environment, or dropped samples; advise a fixed mid-range manual gain (disable AGC), PPM calibration, and a lower sample rate. "
                    + "(5) NullPointerException at p25...P25P1AudioModule.getAudioCodec or JMBE missing/null/incompatible => the JMBE vocoder library is not installed; direct the user to Preferences > JMBE library compiler wizard (which needs a single consistent JDK on JAVA_HOME/Path; 'javac not recognized' indicates conflicting Java versions). "
                    + "(6) NullPointerException at alias.AliasDirectory.getAliasList => an enabled auto-start channel has no alias list assigned; advise assigning an alias list or disabling the channel. "
                    + "(7) 'audio format not supported' / Mixer errors => the selected audio output (often a default HDMI device) is incompatible; advise selecting a compatible analog output or virtual audio cable in Preferences. Note SDRTrunk's internal audio is 8 kHz; flag non-8 kHz input rates as wasted resampling CPU. "
                    + "(8) decrementUserCount / AbstractReusableBuffer released more than its user count => audio buffer leak from JMBE codec desync; if frequent, advise an application restart to flush buffer state. "
                    + "(9) vector/SIMD operations uncalibrated or disabled => advise running vector calibration in Preferences. "
                    + "(10) empty/null Alternate Multi-Band Trunking Control (AMBTC) packet or getMessage() NPE => benign transient parsing drop; reassure the user, no action needed. "
                    + "Prioritize ERROR and WARN lines. For each distinct issue, output a short header, a one-sentence root cause, and a concrete fix. Also scan the startup logs for a fallback to the Prism software (SW) rendering pipeline; if software rendering is detected, advise that the GPU is not accelerating the application and provide graphics-driver update guidance. Output the review directly as if generated by the SDRTrunk system, with no conversational introductions:\\n\\n" + escapedLogContent;

            String jsonPayload = "{" +
                    "\"contents\": [{" +
                    "\"parts\":[{\"text\": \"" + promptText + "\"}]" +
                    "}]" +
                    "}";

            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

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
                throw new Exception("API request failed with status code: " + response.statusCode() + " - " + response.body());
            }

            // Extract the 'text' field from the Gemini JSON response
            // We use simple string matching here instead of bringing in a json library if possible
            // A typical response looks like:
            // { "candidates": [ { "content": { "parts": [ { "text": "The analysis text here..." } ] } } ] }
            String responseBody = response.body();
            String analysisResult = extractTextFromJson(responseBody);
            
            if (mOrchestrator != null) {
                io.github.dsheirer.preference.notification.SystemAlert alert = new io.github.dsheirer.preference.notification.SystemAlert(
                    io.github.dsheirer.preference.notification.AlertCategory.AI_DIAGNOSTICS,
                    analysisResult,
                    "AI_DIAGNOSTICS_FAULT"
                );
                mOrchestrator.intercept(alert);
            }
            
            mAnalysisCache.put(logContent, analysisResult);
            if (mAnalysisCache.size() > 100) {
                mAnalysisCache.clear();
            }

            return analysisResult;

        } catch (Exception e) {
            mLog.error("Error analyzing log with Gemini API: " + e.getMessage(), e);
            throw new Exception("Error calling Gemini API: " + e.getMessage(), e);
        }
    }

    private String extractTextFromJson(String json) {
        try {
            String textKey = "\"text\":";
            int textIndex = json.indexOf(textKey);
            if (textIndex != -1) {
                int startIndex = json.indexOf("\"", textIndex + textKey.length());
                if (startIndex != -1) {
                    int endIndex = json.indexOf("\"", startIndex + 1);
                    // Find the unescaped closing quote
                    while(endIndex != -1 && json.charAt(endIndex - 1) == '\\') {
                       endIndex = json.indexOf("\"", endIndex + 1);
                    }
                    if (endIndex != -1) {
                        String result = json.substring(startIndex + 1, endIndex);
                        // basic unescape
                        result = result.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\").replace("\\r", "\r").replace("\\t", "\t");
                        return result;
                    }
                }
            }
            throw new RuntimeException("Could not parse API response: " + json);
        } catch (Exception e) {
             throw new RuntimeException("Error parsing response: " + e.getMessage(), e);
        }
    }

    /**
     * Estimate token count for a string (~4 chars per token).
     */
    private int estimateTokens(String text) {
        return text == null ? 0 : (text.length() + CHARS_PER_TOKEN - 1) / CHARS_PER_TOKEN;
    }

    /**
     * Enforce token budget by truncating content if it exceeds MAX_TOKENS_PER_REQUEST.
     * Keeps the first and last portions of the log for context.
     */
    private String enforceTokenBudget(String content) {
        if (content == null) return "";
        int tokens = estimateTokens(content);
        if (tokens <= MAX_TOKENS_PER_REQUEST) return content;

        int headChars = MAX_CHARS * 3 / 4; // 75% from the start
        int tailChars = MAX_CHARS / 4;      // 25% from the end
        String truncated = content.substring(0, headChars)
            + "\n\n... [TRUNCATED: " + (content.length() - MAX_CHARS) + " chars removed for token budget] ...\n\n"
            + content.substring(content.length() - tailChars);
        mLog.info("Token budget enforced: {} estimated tokens -> {} chars truncated to {}",
            tokens, content.length(), truncated.length());
        return truncated;
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