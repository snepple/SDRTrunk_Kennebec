package io.github.dsheirer.transcription;

import io.github.dsheirer.audio.AudioSegment;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.ai.AIPreference;
import io.github.dsheirer.module.log.EventLogManager;
import io.github.dsheirer.audio.AudioFormats;
import io.github.dsheirer.sample.ConversionUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.io.InputStream;
import java.util.Scanner;
import java.util.Base64;

public class TranscriptionEngine {
    private static final Logger mLog = LoggerFactory.getLogger(TranscriptionEngine.class);
    private static final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    public static void transcribeAsync(AudioSegment audioSegment, UserPreferences userPreferences) {
        if (audioSegment == null || !audioSegment.hasAudio() || userPreferences == null) {
            return;
        }

        AIPreference aiPreference = userPreferences.getAIPreference();
        if (aiPreference == null || !aiPreference.isTranscriptionEnabled()) {
            return;
        }

        // Copy audio buffers because audioSegment might be cleared or modified asynchronously
        final java.util.List<float[]> audioBuffers = new java.util.ArrayList<>();
        for (float[] buffer : audioSegment.getAudioBuffers()) {
            audioBuffers.add(buffer.clone());
        }

        mExecutor.submit(() -> {
            try {
                String engine = aiPreference.getTranscriptionEngine();
                String transcript = "";

                byte[] wavData = createWavData(audioBuffers);

                if ("GOOGLE".equalsIgnoreCase(engine)) {
                    String apiKey = aiPreference.getGoogleSttApiKey();
                    if (apiKey != null && !apiKey.isEmpty()) {
                        transcript = transcribeGoogle(wavData, apiKey);
                    } else {
                        mLog.warn("Google STT API Key is empty");
                    }
                } else if ("WHISPER".equalsIgnoreCase(engine)) {
                    String apiKey = aiPreference.getWhisperApiKey();
                    if (apiKey != null && !apiKey.isEmpty()) {
                        transcript = transcribeWhisper(wavData, apiKey);
                    } else {
                        mLog.warn("Whisper API Key is empty");
                    }
                }

                if (transcript != null && !transcript.trim().isEmpty()) {
                    mLog.info("Transcription completed: " + transcript);
                    // Emit transcript to event bus or save it somewhere.
                    // For now, logging it as an info.
                    io.github.dsheirer.eventbus.MyEventBus.getGlobalEventBus().post(new TranscriptionEvent(audioSegment, transcript));
                }
            } catch (Exception e) {
                mLog.error("Error during audio transcription", e);
            }
        });
    }

    private static String transcribeGoogle(byte[] wavData, String apiKey) throws Exception {
        String urlString = "https://speech.googleapis.com/v1/speech:recognize?key=" + apiKey;
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String audioBase64 = Base64.getEncoder().encodeToString(wavData);
        String jsonPayload = "{" +
                "\"config\": {" +
                "\"encoding\":\"LINEAR16\"," +
                "\"sampleRateHertz\": 8000," +
                "\"languageCode\": \"en-US\"" +
                "}," +
                "\"audio\": {" +
                "\"content\": \"" + audioBase64 + "\"" +
                "}" +
                "}";

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonPayload.getBytes("UTF-8"));
        }

        int responseCode = conn.getResponseCode();
        InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
        try (Scanner scanner = new Scanner(is, "UTF-8")) {
            String response = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
            if (responseCode >= 200 && responseCode < 300) {
                // simple json parsing
                if (response.contains("\"transcript\":")) {
                    String[] parts = response.split("\"transcript\":");
                    if (parts.length > 1) {
                        String trans = parts[1].split("\"")[1];
                        return trans;
                    }
                }
            } else {
                mLog.error("Google STT API Error: " + response);
            }
        }
        return "";
    }

    private static String transcribeWhisper(byte[] wavData, String apiKey) throws Exception {
        String urlString = "https://api.openai.com/v1/audio/transcriptions";
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        
        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(("--" + boundary + "\r\n").getBytes("UTF-8"));
            os.write(("Content-Disposition: form-data; name=\"file\"; filename=\"audio.wav\"\r\n").getBytes("UTF-8"));
            os.write(("Content-Type: audio/wav\r\n\r\n").getBytes("UTF-8"));
            os.write(wavData);
            os.write(("\r\n--" + boundary + "\r\n").getBytes("UTF-8"));
            os.write(("Content-Disposition: form-data; name=\"model\"\r\n\r\nwhisper-1\r\n").getBytes("UTF-8"));
            os.write(("--" + boundary + "--\r\n").getBytes("UTF-8"));
        }

        int responseCode = conn.getResponseCode();
        InputStream is = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
        try (Scanner scanner = new Scanner(is, "UTF-8")) {
            String response = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
            if (responseCode >= 200 && responseCode < 300) {
                if (response.contains("\"text\":")) {
                    String[] parts = response.split("\"text\":");
                    if (parts.length > 1) {
                        String trans = parts[1].split("\"")[1];
                        return trans;
                    }
                }
            } else {
                mLog.error("Whisper API Error: " + response);
            }
        }
        return "";
    }

    private static byte[] createWavData(java.util.List<float[]> audioBuffers) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        int totalSamples = 0;
        for (float[] buffer : audioBuffers) {
            totalSamples += buffer.length;
        }
        
        int dataSize = totalSamples * 2; // 16-bit
        int chunkSize = 36 + dataSize;
        
        ByteBuffer header = ByteBuffer.allocate(44);
        header.order(ByteOrder.LITTLE_ENDIAN);
        header.put("RIFF".getBytes());
        header.putInt(chunkSize);
        header.put("WAVE".getBytes());
        header.put("fmt ".getBytes());
        header.putInt(16); // format chunk size
        header.putShort((short) 1); // PCM
        header.putShort((short) 1); // Channels (Mono)
        header.putInt(8000); // Sample rate
        header.putInt(16000); // Byte rate
        header.putShort((short) 2); // Block align
        header.putShort((short) 16); // Bits per sample
        header.put("data".getBytes());
        header.putInt(dataSize);
        
        baos.write(header.array());
        
        for (float[] buffer : audioBuffers) {
            byte[] pcmBytes = ConversionUtils.convertToSigned16BitSamples(buffer).array();
            baos.write(pcmBytes);
        }
        
        return baos.toByteArray();
    }
}
