package io.github.dsheirer.transcription;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.dsheirer.audio.AudioSegment;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.ai.AIPreference;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.sample.ConversionUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TranscriptionEngine {
    private static final Logger mLog = LoggerFactory.getLogger(TranscriptionEngine.class);

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 30_000;

    //Skip very short segments (squelch taps, noise bursts) - they cost API money and transcribe poorly
    private static final int MINIMUM_SAMPLES = 2 * 8000; //2 seconds at 8 kHz

    //Circuit breaker: after consecutive API failures, stop transcribing for a cooldown period so a
    //failed/expired key or API outage doesn't stall the pipeline or burn quota indefinitely.
    private static final int FAILURE_THRESHOLD = 3;
    private static final long FAILURE_COOLDOWN_MS = 10 * 60 * 1000;
    private static final AtomicInteger sConsecutiveFailures = new AtomicInteger();
    private static volatile long sDisabledUntil = 0;

    //Transcription is intentionally LOW priority: a transcript only needs to be produced after the audio
    //has been received, played and streamed, and it may be delayed behind newer calls without ever blocking
    //the core tuner -> demod -> audio -> stream path.  Cloud STT calls are network/IO bound, so a few
    //minimum-OS-priority worker threads drain the backlog (and any AI feature waiting on transcripts via the
    //event bus) without stealing CPU from decoding/playback/streaming.
    //
    //A large queue lets transcripts WAIT (be delayed) rather than being dropped.  The previous design - one
    //worker, a 20-deep queue and DiscardOldestPolicy - silently discarded the OLDEST queued segments under
    //load.  That is why transcripts "almost never" appeared on busy multi-channel systems and, for calls
    //split into multiple 60-second segments, dropped the earliest segment first (the beginning of the
    //message).  Dropping is now a last resort that is logged so it is diagnosable rather than silent, and it
    //sheds the NEWEST submission so audio already queued (including the start of in-progress calls) survives.
    private static final int WORKER_THREADS = 3;
    private static final int QUEUE_CAPACITY = 500;
    private static final AtomicInteger sThreadCounter = new AtomicInteger();
    private static final ExecutorService mExecutor = new ThreadPoolExecutor(WORKER_THREADS, WORKER_THREADS,
        0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(QUEUE_CAPACITY), r -> {
            Thread t = new Thread(r, "sdrtrunk transcription " + sThreadCounter.incrementAndGet());
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        }, (rejected, executor) -> mLog.warn("Transcription backlog full (" + QUEUE_CAPACITY +
            " queued); skipping transcription for the newest segment - the system cannot keep up with the " +
            "call volume at the configured STT throughput"));

    public static void transcribeAsync(AudioSegment audioSegment, UserPreferences userPreferences) {
        if (audioSegment == null || !audioSegment.hasAudio() || userPreferences == null) {
            return;
        }

        AIPreference aiPreference = userPreferences.getAIPreference();
        if (aiPreference == null || !aiPreference.isTranscriptionEnabled()) {
            return;
        }

        //Transcribe a given segment at most once.  Transcription is now requested for every completed
        //segment by the audio recording manager (so it works without audio recording enabled), while the
        //recorder/streaming paths may also request it - this guard makes it exactly-once.
        if (!audioSegment.markTranscriptionRequested()) {
            return;
        }

        if (System.currentTimeMillis() < sDisabledUntil) {
            return;
        }

        // Copy audio buffers because audioSegment might be cleared or modified asynchronously
        final java.util.List<float[]> audioBuffers = new java.util.ArrayList<>();
        int totalSamples = 0;
        for (float[] buffer : audioSegment.getAudioBuffers()) {
            audioBuffers.add(buffer.clone());
            totalSamples += buffer.length;
        }

        if (totalSamples < MINIMUM_SAMPLES) {
            return;
        }

        // Capture identifier values now - the segment is recycled asynchronously and may not be
        // valid when downstream subscribers process the transcription event.  The FROM radio ID
        // only exists for trunked/digital protocols (P25/DMR/etc.) - NBFM has none.
        Integer fromRadioId = null;
        Protocol protocol = null;
        String aliasListName = null;

        //Capture the start timestamp and TO talkgroup now (while the segment is valid) so the events table
        //can correlate the transcript to its decode event after the segment is recycled.
        long startTimestamp = audioSegment.getStartTimestamp();
        Integer toId = null;

        try {
            if (audioSegment.getIdentifierCollection() != null) {
                Identifier from = audioSegment.getIdentifierCollection().getFromIdentifier();

                if (from != null && from.getForm() == Form.RADIO && from.getValue() instanceof Number) {
                    fromRadioId = ((Number) from.getValue()).intValue();
                    protocol = from.getProtocol();
                }

                Identifier to = audioSegment.getIdentifierCollection().getToIdentifier();
                if (to != null && to.getValue() instanceof Number) {
                    toId = ((Number) to.getValue()).intValue();
                }

                if (audioSegment.getIdentifierCollection().getAliasListConfiguration() != null) {
                    aliasListName = audioSegment.getIdentifierCollection().getAliasListConfiguration().getValue();
                }
            }
        } catch (Exception e) {
            mLog.debug("Unable to capture identifiers for transcription event - " + e.getMessage());
        }

        final Integer finalFromRadioId = fromRadioId;
        final Protocol finalProtocol = protocol;
        final String finalAliasListName = aliasListName;
        final long finalStartTimestamp = startTimestamp;
        final Integer finalToId = toId;

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

                sConsecutiveFailures.set(0);

                if (transcript != null && !transcript.trim().isEmpty()) {
                    mLog.info("Transcription completed: " + transcript);
                    io.github.dsheirer.eventbus.MyEventBus.getGlobalEventBus().post(
                        new TranscriptionEvent(audioSegment, transcript, finalFromRadioId, finalProtocol,
                            finalAliasListName, finalStartTimestamp, finalToId));
                }
            } catch (Exception e) {
                mLog.error("Error during audio transcription", e);

                if (sConsecutiveFailures.incrementAndGet() >= FAILURE_THRESHOLD) {
                    sConsecutiveFailures.set(0);
                    sDisabledUntil = System.currentTimeMillis() + FAILURE_COOLDOWN_MS;
                    mLog.warn("Transcription disabled for " + (FAILURE_COOLDOWN_MS / 60000) +
                        " minutes after repeated API failures");
                }
            }
        });
    }

    private static String transcribeGoogle(byte[] wavData, String apiKey) throws Exception {
        String urlString = "https://speech.googleapis.com/v1/speech:recognize?key=" + apiKey;
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String audioBase64 = Base64.getEncoder().encodeToString(wavData);
        String jsonPayload = "{" +
                "\"config\": {" +
                "\"encoding\":\"LINEAR16\"," +
                "\"sampleRateHertz\": 8000," +
                "\"languageCode\": \"en-US\"," +
                "\"model\": \"phone_call\"," +
                "\"enableAutomaticPunctuation\": true" +
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
                //Google response: {"results":[{"alternatives":[{"transcript":"...","confidence":...}]}]}
                try {
                    JsonObject root = new Gson().fromJson(response, JsonObject.class);
                    JsonArray results = root != null ? root.getAsJsonArray("results") : null;
                    StringBuilder transcript = new StringBuilder();

                    if (results != null) {
                        for (int i = 0; i < results.size(); i++) {
                            JsonArray alternatives = results.get(i).getAsJsonObject().getAsJsonArray("alternatives");

                            if (alternatives != null && alternatives.size() > 0) {
                                JsonObject best = alternatives.get(0).getAsJsonObject();

                                if (best.has("transcript")) {
                                    transcript.append(best.get("transcript").getAsString()).append(" ");
                                }
                            }
                        }
                    }

                    return transcript.toString().trim();
                } catch (Exception e) {
                    mLog.error("Error parsing Google STT response: " + e.getMessage());
                }
            } else {
                mLog.error("Google STT API Error: " + response);
                throw new IOException("Google STT API error response code " + responseCode);
            }
        }
        return "";
    }

    private static String transcribeWhisper(byte[] wavData, String apiKey) throws Exception {
        String urlString = "https://api.openai.com/v1/audio/transcriptions";
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
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
                //Whisper response: {"text":"..."}
                try {
                    JsonObject root = new Gson().fromJson(response, JsonObject.class);

                    if (root != null && root.has("text")) {
                        return root.get("text").getAsString();
                    }
                } catch (Exception e) {
                    mLog.error("Error parsing Whisper response: " + e.getMessage());
                }
            } else {
                mLog.error("Whisper API Error: " + response);
                throw new IOException("Whisper API error response code " + responseCode);
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
