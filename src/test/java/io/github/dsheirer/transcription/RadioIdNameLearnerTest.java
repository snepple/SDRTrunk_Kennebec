package io.github.dsheirer.transcription;

import com.google.gson.JsonObject;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link RadioIdNameLearner#parseJsonObjectLenient(String)} - robust extraction of a JSON object from a
 * generative model's text response, which is frequently wrapped in markdown code fences or surrounding prose.
 */
public class RadioIdNameLearnerTest
{
    @Test
    public void parsesPlainJson()
    {
        JsonObject o = RadioIdNameLearner.parseJsonObjectLenient("{\"name\":\"Engine 5\",\"confidence\":0.9}");
        assertNotNull(o);
        assertEquals("Engine 5", o.get("name").getAsString());
        assertEquals(0.9, o.get("confidence").getAsDouble(), 0.0001);
    }

    @Test
    public void parsesJsonWrappedInJsonCodeFence()
    {
        String fenced = "```json\n{\"name\":\"Engine 5\",\"confidence\":0.8}\n```";
        JsonObject o = RadioIdNameLearner.parseJsonObjectLenient(fenced);
        assertNotNull(o);
        assertEquals("Engine 5", o.get("name").getAsString());
    }

    @Test
    public void parsesJsonWrappedInPlainCodeFence()
    {
        String fenced = "```\n{\"name\":\"Ladder 1\"}\n```";
        JsonObject o = RadioIdNameLearner.parseJsonObjectLenient(fenced);
        assertNotNull(o);
        assertEquals("Ladder 1", o.get("name").getAsString());
    }

    @Test
    public void parsesJsonSurroundedByProse()
    {
        String prose = "Sure! Here is the result:\n{\"name\":\"Medic 3\",\"confidence\":0.95} \nHope that helps.";
        JsonObject o = RadioIdNameLearner.parseJsonObjectLenient(prose);
        assertNotNull(o);
        assertEquals("Medic 3", o.get("name").getAsString());
    }

    @Test
    public void parsesLenientSingleQuotes()
    {
        //Models sometimes emit single-quoted JSON; lenient parsing accepts it.
        JsonObject o = RadioIdNameLearner.parseJsonObjectLenient("{'name':'Engine 5'}");
        assertNotNull(o);
        assertEquals("Engine 5", o.get("name").getAsString());
    }

    @Test
    public void returnsNullForNoJson()
    {
        assertNull(RadioIdNameLearner.parseJsonObjectLenient("I don't know the name."));
    }

    @Test
    public void returnsNullForNullInput()
    {
        assertNull(RadioIdNameLearner.parseJsonObjectLenient(null));
    }

    @Test
    public void recognizesCommonFireAndEmsUnitDesignators()
    {
        Map<String, Integer> scores = RadioIdNameLearner.extractScoredCandidates("Quint 3 responding, K9 12 on scene");

        assertTrue(scores.containsKey("Quint 3"));
        assertTrue(scores.containsKey("K9 12"));
    }

    @Test
    public void recognizesDispatchConsoleAsSpeakerWhenCallingAUnit()
    {
        Map<String, Integer> scores = RadioIdNameLearner.extractScoredCandidates("County to Engine 4, respond for an alarm");

        assertTrue(scores.containsKey("County"));
        assertFalse(scores.containsKey("Engine 4"));
    }
}
