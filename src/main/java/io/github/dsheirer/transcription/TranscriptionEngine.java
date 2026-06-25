package io.github.dsheirer.transcription;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
    private static final int READ_TIMEOUT_MS = 90_000;

    //Skip very short segments (squelch taps, noise bursts) - they cost API money and transcribe poorly
    //Minimum audio length to send to the STT API. Lowered to 0.8s so short radio calls (acks, unit IDs, brief
    //dispatches) are still transcribed instead of being silently dropped - a major reason transcripts appeared for
    //well under half of calls. Calls shorter than this are labeled [unintelligible] rather than skipped (see below).
    private static final int MINIMUM_SAMPLES = (int)(0.8 * 8000);

    //Sentinel transcript used when a call cannot be transcribed (API returned nothing, or the call was too short to
    //send) so that EVERY call still receives a transcript entry in the events table rather than appearing blank.
    public static final String UNINTELLIGIBLE = "[unintelligible]";

    //Circuit breaker: after consecutive API failures, stop transcribing for a cooldown period so a
    //failed/expired key or API outage doesn't stall the pipeline or burn quota indefinitely.
    private static final int FAILURE_THRESHOLD = 3;
    private static final long FAILURE_COOLDOWN_MS = 10 * 60 * 1000;
    //A persistent failure - an exhausted billing quota or an invalid/expired API key - will NOT clear within the
    //normal transient cooldown.  Retrying every 10 minutes simply re-fails, floods the log with identical errors and
    //(for live-balance keys) keeps poking a billing endpoint.  Back off for hours instead and log it once, concisely.
    private static final long QUOTA_COOLDOWN_MS = 6 * 60 * 60 * 1000;
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

        //Too short to send to the API, but it still reached audio output as a call, so emit an [unintelligible]
        //placeholder (after capturing identifiers below) so the call is not left blank in the events table.
        final boolean tooShortToTranscribe = totalSamples < MINIMUM_SAMPLES;

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
        long frequency = 0L;

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

                //Capture the channel frequency - the most reliable correlation key for conventional channels, which
                //may share a default talkgroup but never a frequency.
                for (Identifier id : audioSegment.getIdentifierCollection().getIdentifiers()) {
                    if (id.getForm() == Form.CHANNEL_FREQUENCY && id.getValue() instanceof Number) {
                        frequency = ((Number) id.getValue()).longValue();
                        break;
                    }
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
        final long finalFrequency = frequency;

        mExecutor.submit(() -> {
            try {
                //Too short to transcribe: still emit a placeholder so the call has an entry.
                if (tooShortToTranscribe) {
                    postTranscription(audioSegment, UNINTELLIGIBLE, finalFromRadioId, finalProtocol,
                        finalAliasListName, finalStartTimestamp, finalToId, finalFrequency);
                    return;
                }

                String engine = aiPreference.getTranscriptionEngine();
                String transcript = "";
                boolean attempted = false;

                byte[] wavData = createWavData(audioBuffers);

                if ("GOOGLE".equalsIgnoreCase(engine)) {
                    String apiKey = aiPreference.getGoogleSttApiKey();
                    if (apiKey != null && !apiKey.isEmpty()) {
                        attempted = true;
                        transcript = transcribeGoogle(wavData, apiKey);
                    } else {
                        mLog.warn("Google STT API Key is empty");
                    }
                } else if ("WHISPER".equalsIgnoreCase(engine)) {
                    String apiKey = aiPreference.getWhisperApiKey();
                    if (apiKey != null && !apiKey.isEmpty()) {
                        attempted = true;
                        transcript = transcribeWhisper(wavData, apiKey);
                    } else {
                        mLog.warn("Whisper API Key is empty");
                    }
                }

                sConsecutiveFailures.set(0);

                //The API succeeded. If it returned text, use it; if it returned nothing (no intelligible speech in
                //noisy/weak audio), emit [unintelligible] so the call still gets a transcript entry. We only reach
                //here on success - a genuine API error throws and is handled below, so good audio is never mislabeled
                //as unintelligible due to a transient outage.
                if (attempted) {
                    String result = (transcript != null && !transcript.trim().isEmpty())
                        ? transcript.trim() : UNINTELLIGIBLE;
                    mLog.info("Transcription completed: " + result);
                    postTranscription(audioSegment, result, finalFromRadioId, finalProtocol,
                        finalAliasListName, finalStartTimestamp, finalToId, finalFrequency);
                }
            } catch (QuotaExhaustedException qee) {
                //Persistent billing/credential problem (exhausted quota, invalid/expired key).  It will not resolve
                //within the normal cooldown, so disable transcription for a long window and log ONE concise warning -
                //no ERROR, no stack trace - rather than re-failing and re-logging the same trace on every call.
                sConsecutiveFailures.set(0);
                sDisabledUntil = System.currentTimeMillis() + QUOTA_COOLDOWN_MS;
                mLog.warn("Transcription disabled for {} hours - {}", (QUOTA_COOLDOWN_MS / 3600000), qee.getMessage());
            } catch (Exception e) {
                //Transient failure (network blip, read timeout, 5xx, plain rate-limit).  Log concisely; keep the full
                //stack trace at DEBUG for diagnosis without spamming the normal log.  Trip the circuit breaker after a
                //few in a row so a short outage doesn't stall the pipeline or burn quota.
                mLog.warn("Transcription failed: {}", e.toString());
                mLog.debug("Transcription failure detail", e);

                if (sConsecutiveFailures.incrementAndGet() >= FAILURE_THRESHOLD) {
                    sConsecutiveFailures.set(0);
                    sDisabledUntil = System.currentTimeMillis() + FAILURE_COOLDOWN_MS;
                    mLog.warn("Transcription disabled for " + (FAILURE_COOLDOWN_MS / 60000) +
                        " minutes after repeated API failures");
                }
            }
        });
    }

    private static void postTranscription(AudioSegment audioSegment, String transcript, Integer fromRadioId,
            Protocol protocol, String aliasListName, long startTimestamp, Integer toId, long frequency) {
        io.github.dsheirer.eventbus.MyEventBus.getGlobalEventBus().post(
            new TranscriptionEvent(audioSegment, transcript, fromRadioId, protocol, aliasListName, startTimestamp,
                toId, frequency));
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
        //useEnhanced=true selects Google's enhanced model for the phone_call domain, which is noticeably more accurate
        //on the narrowband, noisy audio typical of radio traffic. enableAutomaticPunctuation improves readability.
        String jsonPayload = "{" +
                "\"config\": {" +
                "\"encoding\":\"LINEAR16\"," +
                "\"sampleRateHertz\": 8000," +
                "\"languageCode\": \"en-US\"," +
                "\"model\":\"phone_call\"," +
                "\"useEnhanced\":true," +
                "\"enableAutomaticPunctuation\":true" +
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
            } else if (isPersistentFailure(responseCode, response)) {
                throw new QuotaExhaustedException("Google STT quota/credential failure (HTTP " + responseCode +
                        "): " + extractApiErrorMessage(response));
            } else {
                throw new IOException("Google STT API error (HTTP " + responseCode + "): " +
                        extractApiErrorMessage(response));
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
            //Constrain Whisper to English and a deterministic decode (temperature 0) to reduce hallucination on noisy
            //radio audio, and give it a domain prompt so it favors public-safety dispatch phrasing.
            os.write(("--" + boundary + "\r\n").getBytes("UTF-8"));
            os.write(("Content-Disposition: form-data; name=\"language\"\r\n\r\nen\r\n").getBytes("UTF-8"));
            os.write(("--" + boundary + "\r\n").getBytes("UTF-8"));
            os.write(("Content-Disposition: form-data; name=\"temperature\"\r\n\r\n0\r\n").getBytes("UTF-8"));
            os.write(("--" + boundary + "\r\n").getBytes("UTF-8"));
            os.write(("Content-Disposition: form-data; name=\"prompt\"\r\n\r\n").getBytes("UTF-8"));
            os.write(("Public-safety radio dispatch: fire, EMS, police units, street addresses, unit numbers, " +
                    "ten-codes and signals.\r\n").getBytes("UTF-8"));
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
            } else if (isPersistentFailure(responseCode, response)) {
                throw new QuotaExhaustedException("Whisper API quota/credential failure (HTTP " + responseCode +
                        "): " + extractApiErrorMessage(response));
            } else {
                throw new IOException("Whisper API error (HTTP " + responseCode + "): " +
                        extractApiErrorMessage(response));
            }
        }
        return "";
    }

    /**
     * Determines whether an HTTP error from an STT provider represents a PERSISTENT condition that will not clear on
     * its own within the normal transient cooldown - specifically an exhausted billing quota or an invalid/expired
     * API key. These warrant a long back-off rather than retrying every few minutes.
     *
     * Note that a 429 is ambiguous: OpenAI returns it both for a transient rate-limit (resolves on its own) and for an
     * exhausted/unpaid balance (code "insufficient_quota" - does NOT resolve until the user adds credit). Only the
     * latter is treated as persistent, so genuine rate-limiting still retries.
     *
     * @param responseCode HTTP status code returned by the provider
     * @param responseBody response body (JSON error payload), may be null
     * @return true if the failure is persistent (quota/credential), false if it may be transient
     */
    private static boolean isPersistentFailure(int responseCode, String responseBody) {
        if (responseCode == 401 || responseCode == 403) {
            return true; //invalid/expired key or unauthorized - will not fix itself
        }
        if (responseCode == 429 && responseBody != null) {
            String lower = responseBody.toLowerCase();
            return lower.contains("insufficient_quota") || lower.contains("billing") ||
                    (lower.contains("quota") && lower.contains("exceeded"));
        }
        return false;
    }

    /**
     * Extracts a concise, human-readable message from a provider's JSON error body (OpenAI and Google both nest it
     * under "error"/"message") so the log shows the cause without dumping the full raw payload.
     */
    private static String extractApiErrorMessage(String responseBody) {
        if (responseBody == null || responseBody.isEmpty()) {
            return "(no response body)";
        }
        try {
            JsonObject root = new Gson().fromJson(responseBody, JsonObject.class);
            if (root != null && root.has("error")) {
                JsonElement error = root.get("error");
                if (error.isJsonObject() && error.getAsJsonObject().has("message")) {
                    return error.getAsJsonObject().get("message").getAsString();
                }
                if (error.isJsonPrimitive()) {
                    return error.getAsString();
                }
            }
        } catch (Exception ignore) {
            //Not JSON (or unexpected shape) - fall through and return the raw body, trimmed.
        }
        return responseBody.length() > 300 ? responseBody.substring(0, 300) + "..." : responseBody;
    }

    /**
     * Signals a persistent STT failure (exhausted quota or invalid/expired credential) so the caller can apply a long
     * back-off and a single concise log entry, distinct from transient failures that warrant a short retry cooldown.
     */
    private static class QuotaExhaustedException extends IOException {
        QuotaExhaustedException(String message) {
            super(message);
        }
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
