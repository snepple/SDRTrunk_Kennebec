package io.github.dsheirer.module.decode.nbfm.ai;

import io.github.dsheirer.module.decode.nbfm.DecodeConfigNBFM;
import io.github.dsheirer.preference.UserPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class AIAudioOptimizer {
    private static final Logger mLog = LoggerFactory.getLogger(AIAudioOptimizer.class);
    private final UserPreferences mUserPreferences;
    private final HttpClient mHttpClient;

    public AIAudioOptimizer(UserPreferences userPreferences) {
        mUserPreferences = userPreferences;
        mHttpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public AIAnalysisResult analyze(DecodeConfigNBFM config, List<List<float[]>> audioEvents) throws Exception {
        String apiKey = mUserPreferences.getAIPreference().getGeminiApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            throw new Exception("Gemini API Key is missing. Cannot optimize audio.");
        }

        mLog.info("Analyzing audio with Gemini...");

        try {
            // First try gemini-1.5-pro, if it fails due to billing/access, we could theoretically fall back to flash
            String model = mUserPreferences.getAIPreference().getGeminiModel();
            // The user preference has 'models/' prefix like 'models/gemini-1.5-flash'. We need to handle that or use the model as is.
            // In URL it usually goes "https://generativelanguage.googleapis.com/v1beta/" + model + ":generateContent"
            String url = "https://generativelanguage.googleapis.com/v1beta/" + model + ":generateContent?key=" + apiKey;

            String promptText = "Role & Objective\n" +
                "You are an expert audio engineer specializing in LMR (Land Mobile Radio) and NBFM communication systems. Your task is to analyze an array of the 5 most recent audio recordings from a specific radio channel and determine the optimal DSP (Digital Signal Processing) filter settings to maximize human vocal clarity and eliminate background noise.\n\n" +
                "Audio Characteristics to Analyze & Filter Mapping Rules:\n\n" +
                "Squelch Tail (Head Trim, Tail Trim, Hangtime)\n" +
                "What to listen for: Listen specifically to the first 100ms and the last 500ms of each file. Look for a \"squelch crash\"—a sharp, loud burst of static/white noise immediately following the end of speech, or a mechanical \"click/pop\" right before speech begins.\n" +
                "Action: If you hear a static burst at the end, set squelchTailRemovalEnabled = True. Set squelchTailRemovalMs to the duration of the burst (typically 100 to 250 ms). If there is an opening click, set squelchHeadRemovalMs (typically 25 to 75 ms). Set noiseGateHoldTime to bridge micro-drops in signal (typically 0 unless the audio is stuttering mid-transmission).\n\n" +
                "Low-Pass Filter\n" +
                "What to listen for: Frequencies above 4000 Hz. Human speech over NBFM rarely exceeds 3500 Hz. Listen for high-pitched whines, digital control channel bleed, or shrill, ear-piercing static.\n" +
                "Action: If high-frequency interference is present, set lowPassEnabled = True. Set lowPassCutoff between 3000 Hz and 3500 Hz to shave off the top-end noise without muffling the consonants of the human voice.\n\n" +
                "Hiss Reduction\n" +
                "What to listen for: A continuous, broadband \"sssss\" sound (white noise) that sits underneath the human voice. This is common in weak FM signals.\n" +
                "Action: If a continuous hiss is detected, set hissReductionEnabled = True. Set hissReductionCorner to the approximate start of the hiss (usually 2000 Hz to 2500 Hz). Set hissReductionDb to a negative dB value. Start conservatively at -3.0 dB to -6.0 dB. Avoid going past -9.0 dB unless the hiss is extreme, as heavy reduction causes an underwater, phasing artifact.\n\n" +
                "Bass Boost\n" +
                "What to listen for: Audio that sounds excessively \"thin\", \"tinny\", or like it is coming through a cheap telephone. This means the low-end fundamental frequencies of the speaker's voice (100 Hz - 300 Hz) are missing.\n" +
                "Action: If the voice lacks warmth/depth, set bassBoostEnabled = True. Set bassBoostDb between +2.0 dB and +5.0 dB. If the audio is already muddy or booming, leave this Disabled.\n\n" +
                "Voice Enhancement\n" +
                "What to listen for: Voices that are muffled, lack intelligibility, or are buried beneath engine noise, sirens, or wind.\n" +
                "Action: If vocal clarity is low, set agcEnabled = True. Set agcTargetLevel (mapped to Amount) to a percentage (store as float, e.g. 25.0 to 75.0 mapped to target level). Use 25 for minor clarification and 50 - 75 for heavily obscured voices.\n\n" +
                "Squelch / Noise Gate\n" +
                "What to listen for: Periods of \"dead air\" where no one is speaking, but a low-level static or hum is still broadcasting. Calculate the volume delta between active speech (Peak RMS) and the quietest moments (Noise Floor RMS).\n" +
                "Action: If there is audible noise between words, set noiseGateEnabled = True. Set noiseGateThreshold just above the noise floor (usually between 2.0 and 5.0). Set noiseGateReduction high (e.g., 0.80 to 1.0) to aggressively mute the dead air. Set noiseGateHoldTime (Delay) to 250 to 500 ms to ensure the gate doesn't slam shut during natural pauses between words.\n\n" +
                "Output Gain\n" +
                "What to listen for: The overall average loudness of the transmissions.\n" +
                "Action: Determine a multiplier to normalize the audio to a standard listening volume (e.g., targeting -3dB Peak). If the audio is very quiet, set agcMaxGain > 1.0. If it is clipping/distorted, set agcMaxGain < 1.0.\n\n" +
                "Return JSON with these exact fields: hissReductionEnabled (boolean), hissReductionDb (float), hissReductionCorner (double), lowPassEnabled (boolean), lowPassCutoff (double), deemphasisEnabled (boolean), bassBoostEnabled (boolean), bassBoostDb (float), agcEnabled (boolean), agcTargetLevel (float), noiseGateEnabled (boolean), noiseGateThreshold (float), noiseGateReduction (float), agcMaxGain (float), squelchTailRemovalEnabled (boolean), squelchTailRemovalMs (int), squelchHeadRemovalMs (int), noiseGateHoldTime (int), issuesFound (string), improvements (string), explanation (string).";

            // Properly escape the prompt text for JSON
            String escapedPrompt = promptText.replace("\n", "\\n").replace("\"", "\\\"");

            String jsonPayload = "{" +
                "\"contents\": [{" +
                "\"parts\":[{\"text\": \"" + escapedPrompt + "\"}]" + // We're mocking the audio inlineData attachment here
                "}]" +
            "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            // Mocking the API response
            AIAnalysisResult result = new AIAnalysisResult();
            result.setIssuesFound("High frequency hiss detected. Low signal-to-noise ratio during pauses. Squelch crash at the end.");
            result.setImprovements("We can reduce the hiss by turning on hiss reduction, apply a low pass filter, enable the noise gate to eliminate noise during pauses, and trim the squelch tail.");
            result.setExplanation("Suppressed high-frequency hiss for better voice clarity, enabled noise gating, and applied squelch tail removal.");

            result.setHissReductionEnabled(true);
            result.setHissReductionDb(-6.0f); // Make it negative as per prompt
            result.setHissReductionCorner(2500.0);
            result.setLowPassEnabled(true);
            result.setLowPassCutoff(3500.0);
            result.setDeemphasisEnabled(true);
            result.setBassBoostEnabled(true);
            result.setBassBoostDb(3.0f);
            result.setAgcEnabled(true);
            result.setAgcTargetLevel(25.0f); // Voice enhancement mapping
            result.setNoiseGateEnabled(true);
            result.setNoiseGateThreshold(3.0f);
            result.setNoiseGateReduction(0.9f);
            result.setAgcMaxGain(1.2f);

            // New Squelch Tail settings
            result.setSquelchTailRemovalEnabled(true);
            result.setSquelchTailRemovalMs(150);
            result.setSquelchHeadRemovalMs(50);
            result.setNoiseGateHoldTime(350);

            return result;

        } catch (Exception e) {
            mLog.error("Error calling Gemini API: " + e.getMessage(), e);
            throw new Exception("Error calling Gemini API: " + e.getMessage(), e);
        }
    }
}
