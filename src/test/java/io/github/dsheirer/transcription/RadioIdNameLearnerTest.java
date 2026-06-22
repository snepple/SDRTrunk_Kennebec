package io.github.dsheirer.transcription;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the public-safety transcript parsing / speaker-scoring used by {@link RadioIdNameLearner} to disambiguate the
 * transmitting unit (speaker) from the unit being called (recipient), and to preserve compound identifiers.
 */
public class RadioIdNameLearnerTest
{
    private static final int SELF_ID = 40;
    private static final int DIRECTIONAL = 25;
    private static final int ACK = 15;

    @Test
    public void selfIdentificationWithStatusIsStrongest()
    {
        Map<String, Integer> s = RadioIdNameLearner.extractScoredCandidates("Engine 7 is en route");
        assertEquals(SELF_ID, s.get("Engine 7"));
    }

    @Test
    public void selfIdentificationWithTenCode()
    {
        assertEquals(SELF_ID, RadioIdNameLearner.extractScoredCandidates("Medic 12 is 10-8").get("Medic 12"));
        //"County" has no numeric id and isn't matched; the speaker reporting on-scene is Patrol 44.
        Map<String, Integer> s = RadioIdNameLearner.extractScoredCandidates("County, Patrol 44 is on scene");
        assertEquals(SELF_ID, s.get("Patrol 44"));
    }

    @Test
    public void fromToStructureScoresSpeakerBeforeTo_andExcludesRecipient()
    {
        Map<String, Integer> s = RadioIdNameLearner.extractScoredCandidates("Rescue 4 to Command 2");
        assertEquals(DIRECTIONAL, s.get("Rescue 4"));
        assertFalse(s.containsKey("Command 2"), "the unit after 'to' is the recipient, not the speaker");
    }

    @Test
    public void toFromStructureScoresTheSecondParty()
    {
        Map<String, Integer> s = RadioIdNameLearner.extractScoredCandidates("Dispatch, Engine 7");
        assertEquals(DIRECTIONAL, s.get("Engine 7"));
    }

    @Test
    public void acknowledgementIsWeak()
    {
        assertEquals(ACK, RadioIdNameLearner.extractScoredCandidates("Engine 7, 10-4").get("Engine 7"));
    }

    @Test
    public void unitGivenAnOrderIsTheRecipientNotTheSpeaker()
    {
        //Dispatch ordering Engine 4 to respond: Engine 4 must NOT be scored as the speaker.
        Map<String, Integer> s = RadioIdNameLearner.extractScoredCandidates("Engine 4, respond to a structure fire");
        assertFalse(s.containsKey("Engine 4"));
    }

    @Test
    public void compoundIdentifiersArePreservedAndDistinct()
    {
        assertEquals("Engine 9-2", RadioIdNameLearner.extractUnitName("Engine 9-2 is on scene"));
        assertEquals("Engine 9", RadioIdNameLearner.extractUnitName("Engine 9 is on scene"));
        assertNotEquals(
            RadioIdNameLearner.extractUnitName("Engine 9-2 responding"),
            RadioIdNameLearner.extractUnitName("Engine 9 responding"));
    }

    @Test
    public void extractUnitNameReturnsStrongestCandidate()
    {
        //"Engine 4" is the recipient (excluded); the self-identifying "Rescue 9 is en route" wins.
        assertEquals("Rescue 9", RadioIdNameLearner.extractUnitName("Engine 4, Rescue 9 is en route"));
    }

    @Test
    public void noUnitYieldsNoCandidates()
    {
        assertTrue(RadioIdNameLearner.extractScoredCandidates("dispatch go ahead").isEmpty());
        assertNull(RadioIdNameLearner.extractUnitName(""));
        assertNull(RadioIdNameLearner.extractUnitName(null));
    }
}
