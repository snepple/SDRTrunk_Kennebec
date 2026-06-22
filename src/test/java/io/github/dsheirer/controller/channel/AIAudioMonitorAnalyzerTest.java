package io.github.dsheirer.controller.channel;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.github.dsheirer.preference.UserPreferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class AIAudioMonitorAnalyzerTest
{
    private Path mTempAudioFile;

    @BeforeEach
    public void setUp() throws Exception
    {
        mTempAudioFile = Files.createTempFile("test-audio", ".wav");
        Files.write(mTempAudioFile, new byte[]{0x52, 0x49, 0x46, 0x46}); // minimal RIFF header bytes
    }

    @AfterEach
    public void tearDown() throws Exception
    {
        if(mTempAudioFile != null)
        {
            Files.deleteIfExists(mTempAudioFile);
        }
    }

    @Test
    public void testStructuredValidationModelFields()
    {
        JsonObject resultObj = new Gson().fromJson(
            "{\"is_valid_transmission\": true, \"contains_human_speech\": true, " +
            "\"audio_acoustic_profile\": \"clear_speech\", \"confidence_score\": 0.95, " +
            "\"transcript_summary\": \"Dispatch traffic\"}", JsonObject.class);

        RadioCallValidation validation = new RadioCallValidation(
            resultObj.get("is_valid_transmission").getAsBoolean(),
            resultObj.get("contains_human_speech").getAsBoolean(),
            resultObj.get("audio_acoustic_profile").getAsString(),
            resultObj.get("confidence_score").getAsDouble(),
            resultObj.get("transcript_summary").getAsString()
        );

        assertTrue(validation.isValidTransmission());
        assertTrue(validation.isContainsHumanSpeech());
        assertEquals("clear_speech", validation.getAudioAcousticProfile());
        assertEquals(0.95, validation.getConfidenceScore(), 0.001);
        assertEquals("Dispatch traffic", validation.getTranscriptSummary());
    }

    @Test
    public void testMissingApiKeyThrows() throws Exception
    {
        UserPreferences prefs = new UserPreferences();
        prefs.getAIPreference().setGeminiApiKey("");

        AIAudioMonitorAnalyzer analyzer = new AIAudioMonitorAnalyzer(prefs);

        Exception exception = assertThrows(Exception.class, () -> analyzer.analyze(mTempAudioFile));
        assertTrue(exception.getMessage().contains("Gemini API Key is missing"));
    }
}
