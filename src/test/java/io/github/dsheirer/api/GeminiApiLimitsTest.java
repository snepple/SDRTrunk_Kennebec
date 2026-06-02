package io.github.dsheirer.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.module.log.ai.AILogAnalyzer;
import io.github.dsheirer.module.decode.nbfm.ai.AIAudioOptimizer;
import io.github.dsheirer.module.decode.nbfm.DecodeConfigNBFM;
import io.github.dsheirer.controller.channel.AIAudioMonitorAnalyzer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class GeminiApiLimitsTest {
    private HttpServer mServer;
    private int mPort;
    private AtomicInteger mRequestCount = new AtomicInteger(0);
    private volatile int mResponseStatusCode = 200;
    private volatile String mResponseBody = "";
    private volatile long mServerDelayMs = 0;

    @BeforeEach
    public void setUp() throws IOException {
        mRequestCount.set(0);
        mResponseStatusCode = 200;
        mResponseBody = "";
        mServerDelayMs = 0;

        mServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        mServer.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                mRequestCount.incrementAndGet();
                if (mServerDelayMs > 0) {
                    try {
                        Thread.sleep(mServerDelayMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                byte[] responseBytes = mResponseBody.getBytes("UTF-8");
                exchange.sendResponseHeaders(mResponseStatusCode, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            }
        });
        mServer.start();
        mPort = mServer.getAddress().getPort();
        System.setProperty("gemini.api.url", "http://localhost:" + mPort);
    }

    @AfterEach
    public void tearDown() {
        if (mServer != null) {
            mServer.stop(0);
        }
        System.clearProperty("gemini.api.url");
    }

    @Test
    public void testCachingLayerInterceptsRedundantRequests() throws Exception {
        // Setup UserPreferences with dummy API key
        UserPreferences prefs = new UserPreferences();
        prefs.getAIPreference().setAIEnabled(true);
        prefs.getAIPreference().setGeminiApiKey("dummy-key");

        AILogAnalyzer analyzer = new AILogAnalyzer(prefs);
        String logContent = "ERROR: Failed to open tuner RTL-SDR";

        // Expected successful response from Gemini
        mResponseBody = "{ \"candidates\": [ { \"content\": { \"parts\": [ { \"text\": \"The tuner RTL-SDR failed to open. Please check USB connection.\" } ] } } ] }";

        // Call once
        String result1 = analyzer.analyze(logContent);
        assertNotNull(result1);
        assertEquals(1, mRequestCount.get());

        // Call again with identical content
        String result2 = analyzer.analyze(logContent);
        assertEquals(result1, result2);
        // Request count should still be 1 (intercepted by local cache!)
        assertEquals(1, mRequestCount.get());
    }

    @Test
    public void testSimulatedExhaustedTokenBudget() throws Exception {
        UserPreferences prefs = new UserPreferences();
        prefs.getAIPreference().setAIEnabled(true);
        prefs.getAIPreference().setGeminiApiKey("dummy-key");
        prefs.getAIPreference().setGeminiModel("gemini-1.5-pro");

        AILogAnalyzer analyzer = new AILogAnalyzer(prefs);
        String logContent = "Out of tokens log content";

        // Simulate 429 quota exhaustion / RESOURCE_EXHAUSTED
        mResponseStatusCode = 429;
        mResponseBody = "RESOURCE_EXHAUSTED: Resource has been exhausted (e.g. queries per minute quota exceeded).";

        Exception exception = assertThrows(Exception.class, () -> {
            analyzer.analyze(logContent);
        });

        assertTrue(exception.getMessage().contains("quota exhausted") || exception.getMessage().contains("429"));
        // Assert fallback model change was triggered in preferences
        assertEquals("gemini-1.5-thinking", prefs.getAIPreference().getGeminiModel());
    }

    @Test
    public void testApiTimeoutHandling() {
        UserPreferences prefs = new UserPreferences();
        prefs.getAIPreference().setAIEnabled(true);
        prefs.getAIPreference().setGeminiApiKey("dummy-key");

        AILogAnalyzer analyzer = new AILogAnalyzer(prefs);
        String logContent = "Timeout testing log content";

        // Make server delay 12 seconds (analyzer timeout is 10 seconds)
        mServerDelayMs = 12000;

        Exception exception = assertThrows(Exception.class, () -> {
            analyzer.analyze(logContent);
        });

        assertTrue(exception.getMessage().contains("Error calling Gemini API") || exception.getMessage().contains("timeout"));
    }

    @Test
    public void testMalformedJsonResponse() {
        UserPreferences prefs = new UserPreferences();
        prefs.getAIPreference().setAIEnabled(true);
        prefs.getAIPreference().setGeminiApiKey("dummy-key");

        AILogAnalyzer analyzer = new AILogAnalyzer(prefs);
        String logContent = "Malformed JSON testing";

        // Return malformed JSON body
        mResponseBody = "{ \"candidates\": [ { \"content\": "; // missing trailing parts

        Exception exception = assertThrows(Exception.class, () -> {
            analyzer.analyze(logContent);
        });

        // malformed json shouldn't crash, but fail gracefully with an exception
        assertNotNull(exception.getMessage());
    }
}
