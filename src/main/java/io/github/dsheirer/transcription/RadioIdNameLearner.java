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
 * Only the transmitting Radio ID (subscriber unit) is aliased - never the Talkgroup (the candidate key uses the
 * FROM radio ID), so an individual radio is named, not an entire fleet.
 *
 * This learner accumulates transcripts per FROM radio ID and applies a two-stage strategy:
 *
 *   1. Confidence-scoring heuristics: each transmission is parsed for unit designators and a weight is awarded to
 *      the SPEAKER candidate(s) based on public-safety cadence - self-identification with a status or 10-/11-
 *      brevity code is strongest, directional "X to Y" / "Recipient, Speaker" is medium, a bare acknowledgement is
 *      weak, and a unit being addressed/commanded is ignored. Evidence accumulates across transmissions and a name
 *      is committed only once it crosses a confidence threshold; a conflicting self-identification penalizes prior
 *      candidates so shared/pooled radios or transcription errors do not lock in the wrong unit. Compound
 *      identifiers (e.g. "Engine 9-2") are preserved as distinct from "Engine 9".
 *   2. Gemini fallback (optional, when a Gemini API key is configured): radios with several transcripts but no
 *      confident heuristic winner (e.g. dispatch consoles, ambiguous cadences) are resolved by the model, which is
 *      prompted with the same speaker-vs-recipient rules, gated on confidence and a daily call budget.
 *
 * Non-destructive by design: a pre-execution audit skips any radio that already has a real human/imported alias,
 * and at commit time only auto-populated ("[1234567]"), blank, or prior "(auto)" aliases are (re)named - user,
 * CSV-imported, and RadioReference names are never overwritten. Assigned names carry an "(auto)" suffix so they are
 * recognizable and correctable.
 *
 * NBFM and other analog channels are excluded: they have no radio IDs, so transcription events without a FROM radio
 * ID are ignored. Encrypted transmissions produce no usable transcript and are likewise skipped.
 *
 * Opt out with -Dsdrtrunk.transcription.autoname=false or SDRTRUNK_TRANSCRIPTION_AUTONAME=false.
 */
public class RadioIdNameLearner
{
    private static final Logger mLog = LoggerFactory.getLogger(RadioIdNameLearner.class);

    //Confidence-scoring model: accumulate weighted evidence per candidate name across transmissions from the same
    //radio and only commit a name once it reaches COMMIT_THRESHOLD. This prevents a single misheard word from
    //creating a permanent wrong alias. Weights reflect how reliably each syntactic structure identifies the SPEAKER
    //(self-identification with a status/brevity code is the strongest signal; directional "X to Y" / "Dispatch, X" is
    //medium; a bare acknowledgement is weak). A conflicting self-identification penalizes prior candidates so a
    //pooled/shared radio isn't locked to one unit.
    private static final int COMMIT_THRESHOLD = 60;
    private static final int WEIGHT_SELF_ID = 40;       //"Engine 7 is en route" / status / 10-code adjacent
    private static final int WEIGHT_DIRECTIONAL = 25;   //"Rescue 4 to Command" or "Dispatch, Engine 7"
    private static final int WEIGHT_ACKNOWLEDGEMENT = 15; //"Engine 7, 10-4" / "Engine 7 copy"
    private static final int CONTRADICTION_PENALTY = 50;

    private static final int GEMINI_MINIMUM_TRANSCRIPTS = 5;
    private static final int MAX_TRANSCRIPTS_PER_RADIO = 8;
    private static final int MAX_TRACKED_RADIOS = 500;
    private static final int GEMINI_DAILY_BUDGET = 20;
    private static final double GEMINI_CONFIDENCE_THRESHOLD = 0.7;
    private static final String AUTO_NAME_SUFFIX = " (auto)";

    //Dedicated, bounded, lower-priority executor for Gemini name-resolution calls so this best-effort background work
    //can't spawn an unbounded number of concurrent HTTP calls (it previously used the shared cached pool) and yields
    //CPU to the core audio/streaming path. Excess requests under load are dropped rather than queued without bound.
    private static final java.util.concurrent.ExecutorService GEMINI_EXECUTOR =
        new java.util.concurrent.ThreadPoolExecutor(1, 2, 30L, java.util.concurrent.TimeUnit.SECONDS,
            new java.util.concurrent.LinkedBlockingQueue<>(50),
            new io.github.dsheirer.controller.NamingThreadFactory("sdrtrunk radioid-gemini", Thread.NORM_PRIORITY - 1),
            new java.util.concurrent.ThreadPoolExecutor.DiscardOldestPolicy());

    //Unit designators followed by a (possibly hyphen-compound) numeric identifier. The compound form preserves
    //identifiers like "Engine 9-2" / "Tanker 9-3" / "Utility 9-6" - "Engine 9" and "Engine 9-2" are DISTINCT radios
    //and must not be truncated to the same name.
    private static final Pattern UNIT_PATTERN = Pattern.compile(
        "\\b(engine|medic|ladder|truck|quint|rescue|squad|battalion|chief|command|ambulance|ems|tower|brush|" +
        "tanker|tender|utility|marine|car|unit|station|patrol|deputy|traffic|detective|hazmat|air|k-?9|" +
        "adam|baker|charlie|david|edward|frank|george|henry|" +
        "ida|john|king|lincoln|mary|nora|ocean|paul|queen|robert|sam|tom|union|victor|william|x-ray|young|zebra)" +
        "[\\s-]*(\\d{1,4}(?:-\\d{1,4})*)\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern DISPATCH_CONSOLE_PATTERN = Pattern.compile(
        "\\b(dispatch|county|control|base|center|comms|fire\\s+alarm)\\b", Pattern.CASE_INSENSITIVE);

    //Status declarations and APCO 10-/11- brevity codes. When one of these immediately follows a unit name, the
    //grammatical subject is almost universally the SPEAKER (the transmitting radio) - the strongest self-id signal.
    private static final Pattern STATUS_OR_CODE = Pattern.compile(
        "^[\\s,]*(?:is\\s+|are\\s+)?(?:en[\\s-]?route|enroute|responding|on[\\s-]?scene|on[\\s-]?location|" +
        "on[\\s-]?the[\\s-]?air|in[\\s-]?service|out[\\s-]?of[\\s-]?service|in[\\s-]?quarters|in[\\s-]?route|" +
        "available|clear(?:ing|ed)?|arriv(?:ed|ing)|returning|10[\\s-]?\\d{1,2}|11[\\s-]?\\d{1,2})\\b",
        Pattern.CASE_INSENSITIVE);

    //A short acknowledgement following a unit name (weaker self-id evidence).
    private static final Pattern ACKNOWLEDGEMENT = Pattern.compile(
        "^[\\s,]*(?:10[\\s-]?4|10[\\s-]?2|copy(?:\\s+that)?|roger|received|affirmative|negative)\\b",
        Pattern.CASE_INSENSITIVE);

    //Imperative/command words directed AT a unit (it is the recipient being called, not the speaker).
    private static final Pattern RECIPIENT_COMMAND = Pattern.compile(
        "^[\\s,]*(?:respond|be\\s+advised|go\\s+ahead|stand\\s+by|switch\\s+to|you'?re\\s+clear|" +
        "what'?s\\s+your|copy\\s+direct|disregard|do\\s+you|can\\s+you)\\b",
        Pattern.CASE_INSENSITIVE);

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
               event.getTranscript() == null || event.getTranscript().isBlank() ||
               TranscriptionEngine.UNINTELLIGIBLE.equals(event.getTranscript().trim()))
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

            //Fail-fast (pre-execution state audit): if this radio already has a real, human/imported alias, do no
            //further work. The AI must never touch human-curated data, and scoring an already-named radio just wastes
            //effort. Auto-populated placeholders ("[1234]") and prior "(auto)" names remain eligible for naming.
            if(!candidate.mAliasChecked)
            {
                candidate.mAliasChecked = true;

                if(hasExistingUserAlias(candidate))
                {
                    candidate.mResolved = true;
                    return;
                }
            }

            synchronized(candidate)
            {
                if(candidate.mTranscripts.size() < MAX_TRANSCRIPTS_PER_RADIO)
                {
                    candidate.mTranscripts.add(event.getTranscript());
                }

                //Score the SPEAKER candidates in this transmission by syntactic structure (self-id/status/brevity-code
                //strongest, directional medium, acknowledgement weak) and accumulate the evidence for this radio.
                Map<String, Integer> scored = extractScoredCandidates(event.getTranscript());
                String selfId = topName(scored);
                boolean strongSelfId = selfId != null && scored.get(selfId) >= WEIGHT_SELF_ID;

                for(Map.Entry<String, Integer> entry : scored.entrySet())
                {
                    candidate.mScores.merge(entry.getKey(), entry.getValue(), Integer::sum);
                }

                //Contradiction handling: a strong self-identification penalizes every OTHER candidate for this radio,
                //so a shared/pooled radio or a transcription error doesn't lock in the wrong unit.
                if(strongSelfId)
                {
                    for(Map.Entry<String, Integer> entry : candidate.mScores.entrySet())
                    {
                        if(!entry.getKey().equals(selfId))
                        {
                            entry.setValue(entry.getValue() - CONTRADICTION_PENALTY);
                        }
                    }
                }

                //Commit the strongest candidate once it crosses the confidence threshold.
                String best = topName(candidate.mScores);

                if(best != null && candidate.mScores.get(best) >= COMMIT_THRESHOLD)
                {
                    candidate.mResolved = true;
                    assignName(candidate, best, "confidence " + candidate.mScores.get(best));
                    return;
                }

                //Gemini fallback for radios that talk a lot but never reach a confident heuristic name (e.g. dispatch
                //consoles and ambiguous cadences the local parser can't disambiguate).
                if(!candidate.mGeminiAttempted && candidate.mTranscripts.size() >= GEMINI_MINIMUM_TRANSCRIPTS &&
                   noStrongCandidate(candidate))
                {
                    candidate.mGeminiAttempted = true;
                    GEMINI_EXECUTOR.submit(() -> resolveWithGemini(candidate));
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
        int max = 0;

        for(int score : candidate.mScores.values())
        {
            max = Math.max(max, score);
        }

        //Heuristics are weak (no candidate is close to committing) - let Gemini try.
        return max < (COMMIT_THRESHOLD / 2);
    }

    /**
     * Returns the highest-scoring name in a score map, or null if it is empty.
     */
    private static String topName(Map<String, Integer> scores)
    {
        String best = null;
        int bestScore = Integer.MIN_VALUE;

        for(Map.Entry<String, Integer> entry : scores.entrySet())
        {
            if(entry.getValue() > bestScore)
            {
                bestScore = entry.getValue();
                best = entry.getKey();
            }
        }

        return best;
    }

    /**
     * Extracts candidate SPEAKER unit names from a transcript with a syntax-derived weight for each, following public
     * safety cadence:
     *   - "Engine 7 is en route" / a status or 10-/11- brevity code adjacent to the name  -> strong self-identification
     *   - "Rescue 4 to Command" (speaker precedes "to") / "Dispatch, Engine 7" (To-From)  -> directional (medium)
     *   - "Engine 7, 10-4" / "Engine 7 copy"                                              -> acknowledgement (weak)
     * A unit that is the RECIPIENT (named after "to", or immediately commanded - "respond"/"be advised") is NOT
     * awarded speaker weight. Within a single transcript the strongest weight per name is kept. Compound identifiers
     * such as "Engine 9-2" are preserved (never truncated to "Engine 9").
     * @return map of candidate name -> weight (may be empty)
     */
    static Map<String, Integer> extractScoredCandidates(String transcript)
    {
        Map<String, Integer> scores = new HashMap<>();

        if(transcript == null || transcript.isBlank())
        {
            return scores;
        }

        Matcher consoleMatcher = DISPATCH_CONSOLE_PATTERN.matcher(transcript);

        while(consoleMatcher.find())
        {
            String name = normalizeConsoleName(consoleMatcher.group(1));
            String after = transcript.substring(consoleMatcher.end());
            String before = transcript.substring(Math.max(0, consoleMatcher.start() - 16), consoleMatcher.start())
                .toLowerCase(Locale.US);
            String afterLower = after.toLowerCase(Locale.US);

            if(before.matches("(?s).*\\bto\\s*$") || RECIPIENT_COMMAND.matcher(after).find())
            {
                continue;
            }

            int weight;

            if(STATUS_OR_CODE.matcher(after).find())
            {
                weight = WEIGHT_SELF_ID;
            }
            else if(afterLower.matches("(?s)^\\s*to\\s+\\S.*"))
            {
                weight = WEIGHT_DIRECTIONAL;
            }
            else if(afterLower.matches("(?s)^\\s*,\\s*" + UNIT_PATTERN.pattern() + ".*"))
            {
                continue;
            }
            else
            {
                weight = WEIGHT_ACKNOWLEDGEMENT;
            }

            scores.merge(name, weight, Math::max);
        }

        Matcher matcher = UNIT_PATTERN.matcher(transcript);

        while(matcher.find())
        {
            String name = normalizeName(matcher.group(1), matcher.group(2));
            String after = transcript.substring(matcher.end());
            String before = transcript.substring(Math.max(0, matcher.start() - 16), matcher.start())
                .toLowerCase(Locale.US);

            //Recipient: named right after "to" (the call target) or immediately given a command -> not the speaker.
            if(before.matches("(?s).*\\bto\\s*$") || RECIPIENT_COMMAND.matcher(after).find())
            {
                continue;
            }

            String afterLower = after.toLowerCase(Locale.US);
            int weight;

            if(ACKNOWLEDGEMENT.matcher(after).find())
            {
                weight = WEIGHT_ACKNOWLEDGEMENT;     //"Engine 7, 10-4" / "copy" - acknowledgement (weak)
            }
            else if(STATUS_OR_CODE.matcher(after).find())
            {
                weight = WEIGHT_SELF_ID;             //"Engine 7 is en route" / status / non-ack 10-code (strong)
            }
            else if(afterLower.matches("(?s)^\\s*to\\s+\\S.*"))
            {
                weight = WEIGHT_DIRECTIONAL;         //"<name> to <recipient>" - the name is the speaker
            }
            else if(before.matches("(?s).*\\w\\s*,\\s*$"))
            {
                weight = WEIGHT_DIRECTIONAL;         //"<recipient>, <name>" To-From - the name is the speaker
            }
            else
            {
                weight = WEIGHT_ACKNOWLEDGEMENT;     //bare mention - weak speaker evidence
            }

            scores.merge(name, weight, Math::max);
        }

        return scores;
    }

    /**
     * Normalizes a designator + (possibly compound) numeric identifier into a display name, e.g. ("engine","9-2") ->
     * "Engine 9-2". The numeric/compound portion is preserved verbatim so distinct radios stay distinct.
     */
    private static String normalizeName(String designator, String number)
    {
        String d = designator.toLowerCase(Locale.US);
        if(d.equals("k9") || d.equals("k-9"))
        {
            d = "K9";
        }
        else
        {
            d = Character.toUpperCase(d.charAt(0)) + d.substring(1);
        }
        return d + " " + number;
    }

    private static String normalizeConsoleName(String designator)
    {
        String lower = designator.toLowerCase(Locale.US);
        StringBuilder normalized = new StringBuilder();

        for(String part : lower.split("\\s+"))
        {
            if(!normalized.isEmpty())
            {
                normalized.append(' ');
            }

            normalized.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }

        return normalized.toString();
    }

    /**
     * Backwards-compatible single-name extraction: returns the strongest-weighted speaker candidate, or null.
     */
    static String extractUnitName(String transcript)
    {
        return topName(extractScoredCandidates(transcript));
    }

    /**
     * Best-effort check of whether the radio already has a real (non-auto, non-placeholder) alias in its alias list -
     * a human-entered, CSV-imported, or RadioReference name the AI must never touch. Reads a defensive snapshot of the
     * alias model; on any error it returns false so normal (commit-time protected) processing simply continues.
     */
    private boolean hasExistingUserAlias(RadioNameCandidate candidate)
    {
        try
        {
            Radio radioId = new Radio(candidate.mProtocol, candidate.mRadioId);

            for(Alias alias : new ArrayList<>(mAliasModel.getAliases()))
            {
                if(!candidate.mAliasListName.equals(alias.getAliasListName()))
                {
                    continue;
                }

                for(AliasID aliasID : alias.getAliasIdentifiers())
                {
                    if(radioId.matches(aliasID))
                    {
                        String name = alias.getName();
                        boolean autoOrPlaceholder = name == null || name.isBlank() ||
                            (name.startsWith("[") && name.endsWith("]")) || name.endsWith(AUTO_NAME_SUFFIX);
                        return !autoOrPlaceholder;   //a real user/imported name means it is already aliased
                    }
                }
            }
        }
        catch(Exception e)
        {
            //Concurrent modification or other transient read error - skip the optimization; assignName still
            //protects human aliases at commit time.
        }

        return false;
    }

    /**
     * Applies the learned name to the radio's alias: renames an auto-populated alias or creates a
     * new alias.  Never overwrites a user-entered name.
     */
    private void assignName(RadioNameCandidate candidate, String name, String basis)
    {
        runOnFxThread(() -> {
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

    private static void runOnFxThread(Runnable runnable)
    {
        try
        {
            if(Platform.isFxApplicationThread())
            {
                runnable.run();
            }
            else
            {
                Platform.runLater(runnable);
            }
        }
        catch(IllegalStateException e)
        {
            runnable.run();
        }
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
            prompt.append("These are public-safety radio transcripts ALL transmitted by the SAME radio (one physical ")
                .append("unit). Identify the unit designator of the SPEAKER (the transmitting radio), NOT the units it ")
                .append("is calling.\n\n")
                .append("How to find the speaker (public-safety cadence):\n")
                .append("- Self-identification with a status or brevity code is the STRONGEST signal: in ")
                .append("\"Engine 7 is en route\", \"Medic 12 is 10-8\", \"Patrol 44 on scene\" the subject is the speaker.\n")
                .append("- Directional \"X to Y\": the unit BEFORE \"to\" is the speaker (\"Rescue 4 to Command\" -> Rescue 4).\n")
                .append("- To-From \"Recipient, Speaker\": \"Dispatch, Engine 7\" -> the speaker is Engine 7 (Dispatch is called).\n")
                .append("- A unit being given an order is the RECIPIENT, not the speaker (\"Engine 4, respond...\" -> the ")
                .append("speaker is whoever is giving the order, often Dispatch).\n")
                .append("- 10-codes/11-codes (10-4, 10-8, 10-23, 10-20, 11-41...) mark routine status updates; the unit ")
                .append("next to the code is the speaker.\n")
                .append("- Common designators: Engine, Truck, Ladder, Tower, Quint, Tanker, Brush, Squad, Medic, ")
                .append("Ambulance, Rescue, EMS, Battalion, Car, Chief, Command, Patrol, Unit, K9, Traffic, Detective, ")
                .append("Marine, Utility, Hazmat, Air; dispatch/infrastructure: Dispatch, County, Control, Base, Center, ")
                .append("Comms, Fire Alarm.\n")
                .append("- A radio that addresses MANY different units but refers to itself with a static word like ")
                .append("\"Dispatch\"/\"County\"/\"Control\" is a dispatch console - name it that.\n")
                .append("- Preserve compound identifiers EXACTLY: \"Engine 9-2\" is NOT \"Engine 9\".\n")
                .append("- If the transcripts are garbled/encrypted/contradictory and you cannot be sure, return an ")
                .append("empty name with low confidence rather than guessing.\n\n")
                .append("Respond with JSON only: ")
                .append("{\"name\": \"<designator like Engine 5 or Dispatch, or empty string if unsure>\", ")
                .append("\"confidence\": <0.0-1.0>}\n\n");

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
        //Accumulated confidence score per candidate unit name (weighted by syntactic evidence across transmissions).
        private final Map<String, Integer> mScores = new HashMap<>();
        private final List<String> mTranscripts = new ArrayList<>();
        private volatile boolean mResolved = false;
        private boolean mGeminiAttempted = false;
        private boolean mAliasChecked = false;

        private RadioNameCandidate(Protocol protocol, int radioId, String aliasListName)
        {
            mProtocol = protocol;
            mRadioId = radioId;
            mAliasListName = aliasListName;
        }
    }
}
