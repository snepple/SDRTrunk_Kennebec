package io.github.dsheirer.source.tuner.manager;

import io.github.dsheirer.preference.UserPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

public class SignalHealthPredictionLogger {
    private static final Logger mLog = LoggerFactory.getLogger(SignalHealthPredictionLogger.class);

    private final UserPreferences mUserPreferences;
    private final ScheduledExecutorService mScheduler;
    private final HttpClient mHttpClient;
    private Connection mDbConnection;

    // Exponential Backoff Streaming Recovery Module State
    private int mRecoveryAttempt = 0;
    private static final int INITIAL_BACKOFF_MS = 1000;
    private static final int MAX_BACKOFF_MS = 60000;

    public SignalHealthPredictionLogger(UserPreferences prefs) {
        mUserPreferences = prefs;
        mHttpClient = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(10)).build();
        
        mScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SignalHealth-Thread");
            t.setDaemon(true);
            return t;
        });

        initDatabase();

        // Schedule weekly Gemini analyses
        mScheduler.scheduleAtFixedRate(this::performWeeklyAnalysis, 7, 7, TimeUnit.DAYS);
    }

    private void initDatabase() {
        try {
            Path dbPath = mUserPreferences.getDirectoryPreference().getDirectoryConfiguration().resolve("SignalHealthTrend.db");
            String url = "jdbc:sqlite:" + dbPath.toString();
            mDbConnection = DriverManager.getConnection(url);

            try (Statement stmt = mDbConnection.createStatement()) {
                String sql = "CREATE TABLE IF NOT EXISTS signal_health (\n"
                        + " id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                        + " timestamp INTEGER NOT NULL,\n"
                        + " snr REAL,\n"
                        + " ber REAL\n"
                        + ");";
                stmt.execute(sql);
            }
            mLog.info("SignalHealthTrend.db initialized successfully.");
        } catch (Exception e) {
            mLog.error("Error initializing SignalHealthTrend database", e);
        }
    }

    public void logHealthMetrics(double snr, double ber) {
        if (mDbConnection == null) return;
        mScheduler.execute(() -> {
            String sql = "INSERT INTO signal_health(timestamp, snr, ber) VALUES(?,?,?)";
            try (PreparedStatement pstmt = mDbConnection.prepareStatement(sql)) {
                pstmt.setLong(1, System.currentTimeMillis());
                pstmt.setDouble(2, snr);
                pstmt.setDouble(3, ber);
                pstmt.executeUpdate();
            } catch (Exception e) {
                mLog.error("Error logging signal health metrics", e);
            }
        });
    }

    private void performWeeklyAnalysis() {
        if (mDbConnection == null) return;
        mLog.info("Performing weekly AI signal health analysis...");

        try {
            // Extract recent trend (last 7 days)
            long oneWeekAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7);
            String sql = "SELECT snr, ber FROM signal_health WHERE timestamp >= ?";
            StringBuilder csv = new StringBuilder("SNR,BER\n");
            try (PreparedStatement pstmt = mDbConnection.prepareStatement(sql)) {
                pstmt.setLong(1, oneWeekAgo);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        csv.append(rs.getDouble("snr")).append(",")
                           .append(rs.getDouble("ber")).append("\n");
                    }
                }
            }

            if (csv.length() < 20) {
                mLog.info("Not enough data for weekly analysis.");
                return;
            }

            String apiKey = mUserPreferences.getAIPreference().getGeminiApiKey();
            if (apiKey == null || apiKey.isEmpty()) return;

            String model = mUserPreferences.getAIPreference().getGeminiModel();
            if (model == null || model.isEmpty()) model = "gemini-2.5-pro";
            
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

            Gson gson = new Gson();
            JsonObject payload = new JsonObject();
            JsonArray contents = new JsonArray();
            JsonObject contentObj = new JsonObject();
            JsonArray parts = new JsonArray();

            JsonObject textPart = new JsonObject();
            textPart.addProperty("text", "You are an RF engineer. Analyze this weekly SNR and BER data to predict hardware issues like antenna misalignment, water ingress, or LNA degradation. Respond with a concise report.\nData:\n" + csv.toString());
            parts.add(textPart);

            contentObj.add("parts", parts);
            contents.add(contentObj);
            payload.add("contents", contents);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
                    .build();

            HttpResponse<String> response = mHttpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject responseJson = gson.fromJson(response.body(), JsonObject.class);
                JsonArray candidates = responseJson.getAsJsonArray("candidates");
                if (candidates != null && candidates.size() > 0) {
                    JsonObject content = candidates.get(0).getAsJsonObject().getAsJsonObject("content");
                    String resultText = content.getAsJsonArray("parts").get(0).getAsJsonObject().get("text").getAsString().trim();
                    mLog.info("Weekly AI Signal Health Report:\n" + resultText);
                }
            } else {
                mLog.warn("Failed to get weekly analysis from Gemini. Status: " + response.statusCode());
            }

        } catch (Exception e) {
            mLog.error("Error during weekly AI analysis", e);
        }
    }

    /**
     * Exponential Backoff Streaming Recovery Module
     * Call this when a stream/signal drops to attempt reconnection/recovery automatically
     */
    public void triggerStreamingRecovery(Runnable recoveryAction) {
        int backoffMs = Math.min(INITIAL_BACKOFF_MS * (1 << mRecoveryAttempt), MAX_BACKOFF_MS);
        mLog.warn("Signal dropped. Triggering exponential backoff recovery. Attempt " + (mRecoveryAttempt + 1) + ", waiting " + backoffMs + "ms");
        
        mScheduler.schedule(() -> {
            try {
                mLog.info("Executing streaming recovery action...");
                recoveryAction.run();
                // If successful, we'd normally reset mRecoveryAttempt = 0.
                // Assuming it's the caller's responsibility or we just increment for now.
                mRecoveryAttempt++;
            } catch (Exception e) {
                mLog.error("Streaming recovery failed.", e);
            }
        }, backoffMs, TimeUnit.MILLISECONDS);
    }
    
    public void resetRecovery() {
        mRecoveryAttempt = 0;
    }
}
