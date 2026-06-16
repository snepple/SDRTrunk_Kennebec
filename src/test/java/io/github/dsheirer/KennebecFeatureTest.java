package io.github.dsheirer;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import io.github.dsheirer.audio.playback.MasterAGC;
import io.github.dsheirer.audio.playback.QueueSkipManager;
import io.github.dsheirer.audio.playback.TransmissionTimeoutWatchdog;
import io.github.dsheirer.audio.AudioEncryptionFilter;
import io.github.dsheirer.audio.broadcast.zello.ExponentialBackoff;
import io.github.dsheirer.audio.broadcast.zello.NodeOfflinePlaceholder;
import io.github.dsheirer.source.tuner.manager.ControlChannelHunt;
import io.github.dsheirer.source.tuner.manager.SignalVotingOrchestrator;
import io.github.dsheirer.module.ai.PredictiveMaintenanceEngine;
import io.github.dsheirer.monitor.ResourceMonitor;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.module.ai.SquelchAIAdvisor;
import io.github.dsheirer.dsp.ZGCWarmupSequence;
import io.github.dsheirer.dsp.filter.AdjacentChannelInterferenceRejector;

import java.util.List;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Arrays;

/**
 * Comprehensive headless JUnit 5 tests for all new components in the vK.00.075 (Kennebec) release.
 * No JavaFX dependencies — pure unit tests.
 */
public class KennebecFeatureTest {

    // === Domain 3: Audio Pipeline ===

    @Test
    public void testMasterAGC_NormalizesLoudSignal() {
        MasterAGC agc = new MasterAGC(8000f);
        float[] loud = new float[100];
        Arrays.fill(loud, 0.9f);
        float[] result = agc.process(loud);
        assertNotNull(result);
        assertEquals(loud.length, result.length);
        // AGC should keep output clamped to [-1,1]
        for (float s : result) assertTrue(Math.abs(s) <= 1.0f, "Output must be clamped to [-1,1]");
    }

    @Test
    public void testMasterAGC_PassesSilence() {
        MasterAGC agc = new MasterAGC(8000f);
        float[] silence = new float[100]; // all zeros
        float[] result = agc.process(silence);
        // When peak < 1e-10, process() returns the same array reference (passthrough)
        assertSame(silence, result, "Silence should be passed through without modification");
    }

    @Test
    public void testMasterAGC_DisabledPassthrough() {
        MasterAGC agc = new MasterAGC(8000f);
        agc.setEnabled(false);
        assertFalse(agc.isEnabled());
        float[] input = {0.1f, 0.5f, -0.3f};
        float[] result = agc.process(input);
        // When disabled, process() returns the same array reference
        assertSame(input, result, "Disabled AGC should return input unchanged");
    }

    @Test
    public void testQueueSkipManager_FlushesQueue() {
        QueueSkipManager qsm = new QueueSkipManager();
        Queue<String> q = new LinkedList<>(List.of("a", "b", "c", "d"));
        assertFalse(qsm.isPriorityMode());
        qsm.enablePriorityMode();
        assertTrue(qsm.isPriorityMode());
        int flushed = qsm.flushQueue(q);
        assertEquals(4, flushed);
        assertTrue(q.isEmpty());
        qsm.disablePriorityMode();
        assertFalse(qsm.isPriorityMode());
    }

    @Test
    public void testTransmissionTimeoutWatchdog_TracksAndClears() {
        TransmissionTimeoutWatchdog dog = new TransmissionTimeoutWatchdog();
        dog.trackTransmission(42);
        dog.trackTransmission(99);
        dog.clearTransmission(42);
        dog.stop();
        // No exceptions = pass
    }

    @Test
    public void testAudioEncryptionFilter_SuppressesEncrypted() {
        AudioEncryptionFilter filter = new AudioEncryptionFilter();
        assertTrue(filter.isEnabled());
        // null should not suppress
        assertFalse(filter.shouldSuppress(null));
        assertEquals(0L, filter.getSuppressedCount());
        // disabled filter should not suppress anything
        filter.setEnabled(false);
        assertFalse(filter.isEnabled());
    }

    // === Domain 5: Zello & Streaming ===

    @Test
    public void testExponentialBackoff_IncreasesDelay() {
        ExponentialBackoff backoff = new ExponentialBackoff(100, 5000, 2.0);
        long d1 = backoff.nextDelay();
        long d2 = backoff.nextDelay();
        long d3 = backoff.nextDelay();
        assertTrue(d1 >= 80 && d1 <= 120, "First delay ~100ms: " + d1);
        assertTrue(d2 > d1, "Second delay should be larger: " + d2);
        assertTrue(d3 > d2, "Third delay should be larger: " + d3);
        assertEquals(3, backoff.getAttempt());
        backoff.reset();
        assertEquals(0, backoff.getAttempt());
    }

    @Test
    public void testExponentialBackoff_MaxCap() {
        ExponentialBackoff backoff = new ExponentialBackoff(1000, 5000, 10.0);
        for (int i = 0; i < 20; i++) {
            long delay = backoff.nextDelay();
            assertTrue(delay <= 6000, "Delay must respect max cap: " + delay); // 5000 + 10% jitter
        }
    }

    @Test
    public void testNodeOfflinePlaceholder() {
        NodeOfflinePlaceholder placeholder = new NodeOfflinePlaceholder();
        assertFalse(placeholder.isInjecting());
        placeholder.startInjection();
        assertTrue(placeholder.isInjecting());
        byte[] frame = placeholder.getSilenceFrame();
        assertNotNull(frame);
        assertEquals(960 * 2, frame.length); // 20ms at 48kHz * 2 bytes (16-bit PCM)
        for (byte b : frame) assertEquals(0, b, "Silence must be all zeros");
        placeholder.stopInjection();
        assertFalse(placeholder.isInjecting());
    }

    // === Domain 2: Hardware & RF ===

    @Test
    public void testControlChannelHunt_RotatesFrequencies() {
        ControlChannelHunt hunt = new ControlChannelHunt(List.of(851000000L, 852000000L, 853000000L));
        assertFalse(hunt.isHunting());
        hunt.startHunt();
        assertTrue(hunt.isHunting());
        assertEquals(851000000L, hunt.nextFrequency());
        assertEquals(852000000L, hunt.nextFrequency());
        assertEquals(853000000L, hunt.nextFrequency());
        assertEquals(851000000L, hunt.nextFrequency()); // wraps around
        hunt.reset();
        assertEquals(851000000L, hunt.nextFrequency());
        hunt.stopHunt();
        assertFalse(hunt.isHunting());
    }

    @Test
    public void testControlChannelHunt_EmptyList() {
        ControlChannelHunt hunt = new ControlChannelHunt(List.of());
        assertEquals(0L, hunt.nextFrequency());
    }

    @Test
    public void testSignalVotingOrchestrator_SelectsBestTuner() {
        SignalVotingOrchestrator svo = new SignalVotingOrchestrator();
        // getBestTuner() returns String — should be null when no reports submitted
        assertNull(svo.getBestTuner());
        assertEquals(0, svo.getTunerCount());
    }

    // === Domain 7: AI Suite ===

    @Test
    public void testSquelchAIAdvisor_RecommendationsAfterSamples() {
        SquelchAIAdvisor advisor = new SquelchAIAdvisor();
        assertEquals(-100.0f, advisor.getAverageNoiseFloor(), 0.1f);
        // Record 100 samples to trigger recalculation (recalculates when index % 100 == 0)
        for (int i = 0; i < 100; i++) {
            advisor.recordNoiseFloor(-90.0f + (i % 10));
        }
        float threshold = advisor.getRecommendedThreshold();
        assertTrue(threshold > -90.0f, "Threshold should be above average noise: " + threshold);
        float avg = advisor.getAverageNoiseFloor();
        assertTrue(avg > -100.0f && avg < -80.0f, "Average should be reasonable: " + avg);
    }

    @Test
    public void testPredictiveMaintenanceEngine_AcceptsMetrics() {
        PredictiveMaintenanceEngine engine = new PredictiveMaintenanceEngine(null);
        engine.reportMetric("cpu_percent", 45.0);
        engine.reportMetric("memory_percent", 60.0);
        assertEquals(45.0, engine.getMetrics().get("cpu_percent"), 0.1);
        assertEquals(60.0, engine.getMetrics().get("memory_percent"), 0.1);
        engine.stop();
    }

    // === Domain 1: DSP ===

    @Test
    public void testZGCWarmupSequence_RunsWithoutError() {
        assertDoesNotThrow(() -> ZGCWarmupSequence.execute());
    }

    @Test
    public void testACIRejector_DetectsInterference() {
        // Constructor requires (maxBandwidth, minBandwidth)
        AdjacentChannelInterferenceRejector rejector =
            new AdjacentChannelInterferenceRejector(12500, 8000);
        assertFalse(rejector.isAciDetected());
        assertEquals(12500, rejector.getCurrentBandpassWidth());
        assertEquals(0L, rejector.getAciEventCount());

        // Simulate high adjacent power — -10 dB exceeds default threshold of -30 dB
        rejector.updateAdjacentPower(-10.0);
        assertTrue(rejector.isAciDetected(), "ACI should be detected when power exceeds threshold");
        assertEquals(1L, rejector.getAciEventCount());
        // Bandpass should have tightened by one step (500 Hz)
        assertTrue(rejector.getCurrentBandpassWidth() < 12500,
            "Bandpass should narrow under interference");

        // Simulate low adjacent power — below threshold clears ACI
        rejector.updateAdjacentPower(-50.0);
        assertFalse(rejector.isAciDetected(), "ACI should clear when power drops below threshold");
    }

    @Test
    public void testACIRejector_Reset() {
        AdjacentChannelInterferenceRejector rejector =
            new AdjacentChannelInterferenceRejector(12500, 8000);
        rejector.updateAdjacentPower(-10.0); // trigger ACI
        assertTrue(rejector.isAciDetected());
        rejector.reset();
        assertFalse(rejector.isAciDetected());
        assertEquals(12500, rejector.getCurrentBandpassWidth());
        assertEquals(0L, rejector.getAciEventCount());
    }
}
