package io.github.dsheirer.preference.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Lightweight validation helpers for the speech-to-text API keys (OpenAI Whisper and Google
 * Speech-to-Text).  Each method performs a minimal authenticated request and returns a short
 * human-readable result suitable for display next to the key field.
 */
public class SttApiHelper
{
    private static final Logger mLog = LoggerFactory.getLogger(SttApiHelper.class);
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 15_000;

    /**
     * Result of an API key test.
     */
    public static class TestResult
    {
        public final boolean success;
        public final String message;

        public TestResult(boolean success, String message)
        {
            this.success = success;
            this.message = message;
        }
    }

    /**
     * Validates an OpenAI (Whisper) API key by listing models (a lightweight authenticated GET).
     */
    public static TestResult testWhisperKey(String apiKey)
    {
        if(apiKey == null || apiKey.trim().isEmpty())
        {
            return new TestResult(false, "Please enter an API key.");
        }

        HttpURLConnection connection = null;

        try
        {
            connection = (HttpURLConnection) new URL("https://api.openai.com/v1/models").openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);

            int code = connection.getResponseCode();

            if(code == 200)
            {
                return new TestResult(true, "Passed");
            }
            if(code == 401)
            {
                return new TestResult(false, "Invalid API key (401 Unauthorized)");
            }

            return new TestResult(false, "Failed (HTTP " + code + ")");
        }
        catch(Exception e)
        {
            mLog.error("Error testing Whisper API key", e);
            return new TestResult(false, "Error: " + e.getMessage());
        }
        finally
        {
            if(connection != null)
            {
                connection.disconnect();
            }
        }
    }

    /**
     * Validates a Google Speech-to-Text API key with a minimal recognize request.  A valid key is
     * accepted (HTTP 200, or an HTTP 400 about audio content rather than the key); an invalid/unauthorized
     * key returns 400/401/403 with an API-key error message.
     */
    public static TestResult testGoogleSttKey(String apiKey)
    {
        if(apiKey == null || apiKey.trim().isEmpty())
        {
            return new TestResult(false, "Please enter an API key.");
        }

        HttpURLConnection connection = null;

        try
        {
            URL url = new URL("https://speech.googleapis.com/v1/speech:recognize?key=" + apiKey.trim());
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setDoOutput(true);
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);

            String body = "{\"config\":{\"encoding\":\"LINEAR16\",\"sampleRateHertz\":8000," +
                "\"languageCode\":\"en-US\"},\"audio\":{\"content\":\"\"}}";

            try(OutputStream os = connection.getOutputStream())
            {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int code = connection.getResponseCode();

            if(code == 200)
            {
                return new TestResult(true, "Passed");
            }

            String error = readStream(connection.getErrorStream());
            String lower = error == null ? "" : error.toLowerCase();
            boolean keyProblem = lower.contains("api key") || lower.contains("api_key") ||
                lower.contains("permission") || lower.contains("unauthenticated") || lower.contains("forbidden");

            //A 400 that is NOT about the key (e.g. empty/short audio) means the key was accepted.
            if(code == 400 && !keyProblem)
            {
                return new TestResult(true, "Passed");
            }
            if(code == 401 || code == 403 || keyProblem)
            {
                return new TestResult(false, "Invalid or unauthorized API key (HTTP " + code + ")");
            }

            return new TestResult(false, "Failed (HTTP " + code + ")");
        }
        catch(Exception e)
        {
            mLog.error("Error testing Google STT API key", e);
            return new TestResult(false, "Error: " + e.getMessage());
        }
        finally
        {
            if(connection != null)
            {
                connection.disconnect();
            }
        }
    }

    private static String readStream(InputStream stream)
    {
        if(stream == null)
        {
            return null;
        }

        try(Scanner scanner = new Scanner(stream, StandardCharsets.UTF_8).useDelimiter("\\A"))
        {
            return scanner.hasNext() ? scanner.next() : null;
        }
        catch(Exception e)
        {
            return null;
        }
    }
}
