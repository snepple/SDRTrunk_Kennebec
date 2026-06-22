/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */
package io.github.dsheirer.transcription;

import com.google.common.eventbus.Subscribe;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.alias.id.radio.Radio;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.util.ThreadPool;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Learns friendly names for radio IDs (subscriber units) from audio transcriptions.
 *
 * Radio procedure means units usually self-identify at the start of a transmission
 * ("Dispatch, Engine 5, responding ...").  This learner accumulates transcripts per FROM radio ID
 * and applies a two-stage strategy:
 *
 *   1. Pattern heuristics: a unit designator (Engine/Medic/Ladder/Unit/Car/etc. + number) found
 *      near the start of a transmission counts as a name observation.  A name is only assigned
 *      after the SAME designator is observed in multiple transmissions from the same radio,
 *      because a single transmission often contains the callee's name rather than the caller's.
 *   2. Gemini fallback (optional, when a Gemini API key is configured): radios with several
 *      transcripts but no heuristic winner are resolved by asking the model to identify the
 *      speaker's self-identification across all transmissions, gated on confidence and a daily
 *      call budget.
 *
 * Names are applied conservatively: only auto-populated aliases (e.g. "[1234567]") or missing
 * aliases are named - user-entered alias names are never overwritten.  Assigned names carry an
 * "(auto)" suffix so they are recognizable and correctable.
 *
 * NBFM and other analog channels are excluded: they have no radio IDs, so transcription events
 * without a FROM radio ID are ignored.
 *
 * Opt out with -Dsdrtrunk.transcription.autoname=false or SDRTRUNK_TRANSCRIPTION_AUTONAME=false.
 */
public class RadioIdNameLearner
{
    private static final Logger mLog = LoggerFactory.getLogger(RadioIdNameLearner.class);

    private static final int HEURISTIC_CONFIRMATION_COUNT = 3;
    private static final int GEMINI_MINIMUM_TRANSCRIPTS = 5;
    private static final int MAX_TRANSCRIPTS_PER_RADIO = 8;
    private static final int MAX_TRACKED_RADIOS = 500;
    private static final int GEMINI_DAILY_BUDGET = 20;
    private static final double GEMINI_CONFIDENCE_THRESHOLD = 0.7;
    private static final String AUTO_NAME_SUFFIX = " (auto)";

    //Unit designators commonly used for self-identification, matched near the start of a transmission
    private static final Pattern UNIT_PATTERN = Pattern.compile(
        "\\b(engine|medic|ladder|truck|rescue|squad|battalion|chief|ambulance|tower|brush|tanker|tender|" +
        "utility|marine|car|unit|station|patrol|deputy|adam|baker|charlie|david|edward|frank|george|henry|" +
        "ida|john|king|lincoln|mary|nora|ocean|paul|queen|robert|sam|tom|union|victor|william|x-ray|young|zebra)" +
        "[\\s-]*(\\d{1,4})\\b", Pattern.CASE_INSENSITIVE);

    //Only consider self-identification near the start of the transmission
    private static final int SELF_ID_WINDOW_CHARACTERS = 80;

    private final AliasModel mAliasModel;
    private final UserPreferences mUserPreferences;
    private final Map<String, RadioNameCandidate> mCandidates = new ConcurrentHashMap<>();
    private final HttpClient mHttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10)).build();
    private int mGeminiCallsToday = 0;
    private long mGeminiBudgetDayStart = System.currentTimeMillis();

    public RadioIdNameLearner(AliasModel aliasModel, UserPreferences userPreferences)
    {
        mAliasModel = aliasModel;
        mUserPreferences = userPreferences;
    }

    public void start()
    {
        //Always register so the toggle can take effect at runtime; enablement is checked per-event in
        //handleTranscription() via the AI preference (and an optional system-property hard override).
        MyEventBus.getGlobalEventBus().register(this);
        mLog.info("Radio ID name learner started - friendly names will be learned from transcriptions when enabled");
    }

    private boolean isEnabled()
    {
        //A system property / env var can hard-disable the feature (set to "false"); otherwise the AI
        //preference toggle controls it (default on).
        String override = System.getProperty("sdrtrunk.transcription.autoname",
            System.getenv("SDRTRUNK_TRANSCRIPTION_AUTONAME"));
        if(override != null && override.equalsIgnoreCase("false"))
        {
            return false;
        }
        return mUserPreferences.getAIPreference().isRadioIdNamingEnabled();
    }

    @Subscribe
    public void handleTranscription(TranscriptionEvent event)
    {
        if(!isEnabled())
        {
            return;
        }

        try
        {
            //No FROM radio ID means an analog (NBFM) or unidentified transmission - nothing to name
            if(event.getFromRadioId() == null || event.getProtocol() == null ||
               event.getAliasListName() == null || event.getAliasListName().isEmpty() ||
               event.getTranscript() == null || event.getTranscript().isBlank())
            {
                return;
            }

            String key = event.getProtocol() + ":" + event.getAliasListName() + ":" + event.getFromRadioId();

            if(mCandidates.size() >= MAX_TRACKED_RADIOS && !mCandidates.containsKey(key))
            {
                return;
            }

            RadioNameCandidate candidate = mCandidates.computeIfAbsent(key,
                k -> new RadioNameCandidate(event.getProtocol(), event.getFromRadioId(), event.getAliasListName()));

            if(candidate.mResolved)
            {
                return;
            }

            synchronized(candidate)
            {
                if(candidate.mTranscripts.size() < MAX_TRANSCRIPTS_PER_RADIO)
                {
                    candidate.mTranscripts.add(event.getTranscript());
                }

                String name = extractUnitName(event.getTranscript());

                if(name != null)
                {
                    int count = candidate.mNameCounts.merge(name, 1, Integer::sum);

                    if(count >= HEURISTIC_CONFIRMATION_COUNT)
                    {
                        candidate.mResolved = true;
                        assignName(candidate, name, "heuristic x" + count);
                        return;
                    }
                }

                //Gemini fallback for radios that talk a lot but never match the unit pattern
                if(!candidate.mGeminiAttempted && candidate.mTranscripts.size() >= GEMINI_MINIMUM_TRANSCRIPTS &&
                   noStrongCandidate(candidate))
                {
                    candidate.mGeminiAttempted = true;
                    ThreadPool.CACHED.submit(() -> resolveWithGemini(candidate));
                }
            }
        }
        catch(Exception e)
        {
            mLog.error("Error learning radio name from transcription", e);
        }
    }

    private boolean noStrongCandidate(RadioNameCandidate candidate)
    {
        for(int count : candidate.mNameCounts.values())
        {
            if(count >= 2)
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Extracts a unit designator from the start of a transcript, normalized to title case.
     */
    static String extractUnitName(String transcript)
    {
        String window = transcript.length() > SELF_ID_WINDOW_CHARACTERS ?
            transcript.substring(0, SELF_ID_WINDOW_CHARACTERS) : transcript;

        Matcher matcher = UNIT_PATTERN.matcher(window);

        if(matcher.find())
        {
            String designator = matcher.group(1).toLowerCase(Locale.US);
            designator = Character.toUpperCase(designator.charAt(0)) + designator.substring(1);
            return designator + " " + matcher.group(2);
        }

        return null;
    }

    /**
     * Applies the learned name to the radio's alias: renames an auto-populated alias or creates a
     * new alias.  Never overwrites a user-entered name.
     */
    private void assignName(RadioNameCandidate candidate, String name, String basis)
    {
        Platform.runLater(() -> {
            try
            {
                Radio radioId = new Radio(candidate.mProtocol, candidate.mRadioId);
                Alias existing = null;

                for(Alias alias : mAliasModel.getAliases())
                {
                    if(candidate.mAliasListName.equals(alias.getAliasListName()))
                    {
                        for(AliasID aliasID : alias.getAliasIdentifiers())
                        {
                            if(radioId.matches(aliasID))
                            {
                                existing = alias;
                                break;
                            }
                        }
                    }

                    if(existing != null)
                    {
                        break;
                    }
                }

                String newName = name + AUTO_NAME_SUFFIX;

                if(existing != null)
                {
                    String currentName = existing.getName();

                    //Only rename auto-populated ("[1234]") or previously auto-named aliases
                    boolean renameable = currentName == null || currentName.isBlank() ||
                        (currentName.startsWith("[") && currentName.endsWith("]")) ||
                        currentName.endsWith(AUTO_NAME_SUFFIX);

                    if(renameable && !newName.equals(currentName))
                    {
                        mLog.info("Learned radio name [" + newName + "] for radio [" + candidate.mRadioId +
                            "] via " + basis + " - renaming alias");
                        existing.setName(newName);
                    }
                }
                else
                {
                    mLog.info("Learned radio name [" + newName + "] for radio [" + candidate.mRadioId +
                        "] via " + basis + " - creating alias");
                    Alias alias = new Alias(newName);
                    alias.setAliasListName(candidate.mAliasListName);
                    alias.setGroup("Auto-Named");
                    alias.addAliasID(radioId);
                    mAliasModel.addAlias(alias);
                }
            }
            catch(Exception e)
            {
                mLog.error("Error assigning learned radio name", e);
            }
        });
    }

    /**
     * Asks Gemini to identify the speaker's self-identification across the radio's transcripts.
     */
    private void resolveWithGemini(RadioNameCandidate candidate)
    {
        try
        {
            String apiKey = mUserPreferences.getAIPreference().getGeminiApiKey();

            if(apiKey == null || apiKey.isEmpty() || !budgetAvailable())
            {
                return;
            }

            String model = mUserPreferences.getAIPreference().getGeminiModel();

            if(model == null || model.isEmpty())
            {
                model = "models/gemini-2.5-flash";
            }

            if(!model.startsWith("models/"))
            {
                model = "models/" + model;
            }

            StringBuilder prompt = new StringBuilder();
            prompt.append("These are radio transmission transcripts all spoken by the SAME radio unit on a public ")
                .append("safety system.  Units normally self-identify at the start of a transmission.  Identify the ")
                .append("unit designator of the SPEAKER (not units they are calling).  Respond with JSON only: ")
                .append("{\"name\": \"<designator like Engine 5, or empty string if unsure>\", \"confidence\": <0.0-1.0>}\n\n");

            List<String> transcripts;

            synchronized(candidate)
            {
                transcripts = new ArrayList<>(candidate.mTranscripts);
            }

            for(String transcript : transcripts)
            {
                prompt.append("- ").append(transcript).append("\n");
            }

            JsonObject textPart = new JsonObject();
            textPart.addProperty("text", prompt.toString());
            JsonArray parts = new JsonArray();
            parts.add(textPart);
            JsonObject content = new JsonObject();
            content.add("parts", parts);
            JsonArray contents = new JsonArray();
            contents.add(content);
            JsonObject generationConfig = new JsonObject();
            generationConfig.addProperty("responseMimeType", "application/json");
            JsonObject payload = new JsonObject();
            payload.add("contents", contents);
            payload.add("generationConfig", generationConfig);

            String baseUrl = System.getProperty("gemini.api.url", "https://generativelanguage.googleapis.com");
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1beta/" + model + ":generateContent?key=" + apiKey))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.ofString(new Gson().toJson(payload)))
                .build();

            HttpResponse<String> response = mHttpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if(response.statusCode() != 200)
            {
                mLog.warn("Gemini radio name resolution failed [" + response.statusCode() + "]");
                return;
            }

            JsonObject root = parseJsonObjectLenient(response.body());

            if(root == null)
            {
                mLog.warn("Gemini radio name resolution returned an unparseable response body");
                return;
            }

            JsonArray candidates = root.getAsJsonArray("candidates");

            if(candidates == null || candidates.size() == 0)
            {
                return;
            }

            String text = candidates.get(0).getAsJsonObject().getAsJsonObject("content")
                .getAsJsonArray("parts").get(0).getAsJsonObject().get("text").getAsString();

            //Models frequently wrap the JSON in markdown fences or add stray prose even when asked for raw
            //JSON, so parse tolerantly rather than letting a MalformedJsonException abort the resolution.
            JsonObject result = parseJsonObjectLenient(text);

            if(result == null)
            {
                mLog.warn("Gemini radio name resolution returned unparseable JSON: " +
                    (text.length() > 120 ? text.substring(0, 120) + "..." : text));
                return;
            }

            String name = result.has("name") ? result.get("name").getAsString() : "";
            double confidence = result.has("confidence") ? result.get("confidence").getAsDouble() : 0.0;

            if(name != null && !name.isBlank() && confidence >= GEMINI_CONFIDENCE_THRESHOLD)
            {
                synchronized(candidate)
                {
                    if(!candidate.mResolved)
                    {
                        candidate.mResolved = true;
                        assignName(candidate, name.trim(), "Gemini confidence " + confidence);
                    }
                }
            }
        }
        catch(Exception e)
        {
            mLog.warn("Error resolving radio name with Gemini - " + e.getMessage());
        }
    }

    /**
     * Parses a JSON object from model output, tolerating the markdown code fences and stray prose that some
     * models emit even when asked for raw JSON (which made strict GSON parsing throw MalformedJsonException).
     * Returns null if no JSON object can be recovered.
     */
    static JsonObject parseJsonObjectLenient(String raw)
    {
        if(raw == null || raw.isBlank())
        {
            return null;
        }

        String cleaned = raw.trim();

        //Strip a wrapping markdown code fence: ```json ... ``` or ``` ... ```
        if(cleaned.startsWith("```"))
        {
            int firstNewline = cleaned.indexOf('\n');

            if(firstNewline >= 0)
            {
                cleaned = cleaned.substring(firstNewline + 1);
            }

            if(cleaned.endsWith("```"))
            {
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            }

            cleaned = cleaned.trim();
        }

        //Isolate the first {...} block when there is leading/trailing prose around it.
        if(!cleaned.startsWith("{"))
        {
            int start = cleaned.indexOf('{');
            int end = cleaned.lastIndexOf('}');

            if(start >= 0 && end > start)
            {
                cleaned = cleaned.substring(start, end + 1);
            }
        }

        try
        {
            JsonReader reader = new JsonReader(new StringReader(cleaned));
            reader.setStrictness(Strictness.LENIENT);
            return new Gson().fromJson(reader, JsonObject.class);
        }
        catch(Exception e)
        {
            return null;
        }
    }

    private synchronized boolean budgetAvailable()
    {
        long now = System.currentTimeMillis();

        if(now - mGeminiBudgetDayStart > 24 * 60 * 60 * 1000L)
        {
            mGeminiBudgetDayStart = now;
            mGeminiCallsToday = 0;
        }

        if(mGeminiCallsToday >= GEMINI_DAILY_BUDGET)
        {
            return false;
        }

        mGeminiCallsToday++;
        return true;
    }

    /**
     * Tracking state for one radio ID.
     */
    private static class RadioNameCandidate
    {
        private final Protocol mProtocol;
        private final int mRadioId;
        private final String mAliasListName;
        private final Map<String, Integer> mNameCounts = new HashMap<>();
        private final List<String> mTranscripts = new ArrayList<>();
        private volatile boolean mResolved = false;
        private boolean mGeminiAttempted = false;

        private RadioNameCandidate(Protocol protocol, int radioId, String aliasListName)
        {
            mProtocol = protocol;
            mRadioId = radioId;
            mAliasListName = aliasListName;
        }
    }
}
