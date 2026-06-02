package io.github.dsheirer.source.tuner.manager;

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
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

public class AIFrequencyStabilizer {
    private static AIFrequencyStabilizer sInstance;
    public static AIFrequencyStabilizer getInstance(UserPreferences prefs) {
        if (sInstance == null) {
            sInstance = new AIFrequencyStabilizer(prefs);
        }
        return sInstance;
    }
    private static final Logger mLog = LoggerFactory.getLogger(AIFrequencyStabilizer.class);
    
    private final UserPreferences mUserPreferences;
    private final HttpClient mHttpClient;
    private final ScheduledExecutorService mScheduler;

    private int mApiRequestsToday = 0;
    private long mLastResetTime = System.currentTimeMillis();
    private static final int MAX_DAILY_REQUESTS = 5;

    // Cache
    private double mCachedPpmOffset = 0.0;
    private boolean mHasCache = false;

    // Drift data
    private final List<DataPoint> mDriftHistory = new ArrayList<>();

    private static class DataPoint {
        double temperature;
        double offsetHz;
        public DataPoint(double temp, double offset) {
            this.temperature = temp;
            this.offsetHz = offset;
        }
    }

    public AIFrequencyStabilizer(UserPreferences prefs) {
        mUserPreferences = prefs;
        mHttpClient = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(10)).build();
        mScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AIFreqStabilizer-Thread");
            t.setDaemon(true);
            return t;
        });

        // Run correlation analysis periodically
        mScheduler.scheduleAtFixedRate(this::generateCompensationCurve, 1, 1, TimeUnit.HOURS);
    }

    public void recordObservation(double temperature, double offsetHz) {
        synchronized(mDriftHistory) {
            mDriftHistory.add(new DataPoint(temperature, offsetHz));
            if (mDriftHistory.size() > 1000) {
                mDriftHistory.remove(0);
            }
        }
    }

    public double getCompensationPpm(double currentTemperature) {
        if (mHasCache) {
            return mCachedPpmOffset;
        }
        return 0.0;
    }

    public String getStabilityStatus() {
        if (!mHasCache) return "Calibrating (AI)";
        return String.format("AI Stabilized: %.1f PPM", mCachedPpmOffset);
    }

    private void generateCompensationCurve() {
        checkDailyLimitReset();
        if (mApiRequestsToday >= MAX_DAILY_REQUESTS) {
            mLog.warn("AIFrequencyStabilizer: Max daily Gemini API requests reached.");
            return;
        }

        List<DataPoint> historySnapshot;
        synchronized(mDriftHistory) {
            if (mDriftHistory.size() < 10) return; // Need enough data
            historySnapshot = new ArrayList<>(mDriftHistory);
        }

        mLog.info("Generating frequency compensation curve using Gemini...");
        mApiRequestsToday++;

        try {
            String apiKey = mUserPreferences.getAIPreference().getGeminiApiKey();
            if (apiKey == null || apiKey.isEmpty()) return;

            String model = mUserPreferences.getAIPreference().getGeminiModel();
            if (model == null || model.isEmpty()) model = "gemini-2.5-pro";
            
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

            StringBuilder csvData = new StringBuilder("Temperature,OffsetHz\n");
            for (DataPoint dp : historySnapshot) {
                csvData.append(dp.temperature).append(",").append(dp.offsetHz).append("\n");
            }

            Gson gson = new Gson();
            JsonObject payload = new JsonObject();
            JsonArray contents = new JsonArray();
            JsonObject contentObj = new JsonObject();
            JsonArray parts = new JsonArray();

            JsonObject textPart = new JsonObject();
            textPart.addProperty("text", "You are an RF engineer. Analyze this temperature vs frequency drift (Hz) data for an RTL-SDR tuner. Calculate the linear or polynomial compensation curve and return ONLY the recommended base PPM offset as a single double value (e.g. '1.5').\n\nData:\n" + csvData.toString());
            parts.add(textPart);

            contentObj.add("parts", parts);
            contents.add(contentObj);
            payload.add("contents", contents);

            String jsonPayload = gson.toJson(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = mHttpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject responseJson = gson.fromJson(response.body(), JsonObject.class);
                JsonArray candidates = responseJson.getAsJsonArray("candidates");
                if (candidates != null && candidates.size() > 0) {
                    JsonObject content = candidates.get(0).getAsJsonObject().getAsJsonObject("content");
                    String resultText = content.getAsJsonArray("parts").get(0).getAsJsonObject().get("text").getAsString().trim();
                    try {
                        mCachedPpmOffset = Double.parseDouble(resultText);
                        mHasCache = true;
                        mLog.info("AI determined compensation curve offset: " + mCachedPpmOffset + " PPM");
                    } catch (NumberFormatException e) {
                        mLog.warn("Failed to parse AI response as double: " + resultText);
                    }
                }
            } else {
                mLog.warn("Gemini API error during frequency stabilization");
            }
        } catch (Exception e) {
            mLog.error("Error generating compensation curve", e);
        }
    }

    private void checkDailyLimitReset() {
        long now = System.currentTimeMillis();
        if (now - mLastResetTime > TimeUnit.DAYS.toMillis(1)) {
            mApiRequestsToday = 0;
            mLastResetTime = now;
        }
    }
}
