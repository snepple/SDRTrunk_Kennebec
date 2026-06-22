package io.github.dsheirer.module.ai;

import io.github.dsheirer.dsp.squelch.NoiseSquelch;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link SquelchAIAdvisor#calibrate()} verifying the NBFM squelch calibration methodology: thresholds are
 * anchored to the measured noise floor, the open threshold sits below the noise floor (so ambient noise can't open
 * the squelch), open is always &lt;= close, the recommended values are valid {@link NoiseSquelch} settings, and the
 * temporal hysteresis follows the documented baseline / picket-fencing behavior.
 */
public class SquelchAIAdvisorTest
{
    private static SquelchAIAdvisor withSamples(float... samples)
    {
        SquelchAIAdvisor advisor = new SquelchAIAdvisor();
        for(float s : samples)
        {
            advisor.recordVarianceSample(s);
        }
        return advisor;
    }

    private static void assertValidNoiseSquelchSettings(SquelchAIAdvisor.Recommendation rec)
    {
        assertNotNull(rec);
        assertTrue(rec.openThreshold() <= rec.closeThreshold(),
                "open (" + rec.openThreshold() + ") must be <= close (" + rec.closeThreshold() + ")");
        assertTrue(rec.openThreshold() >= NoiseSquelch.MINIMUM_NOISE_THRESHOLD);
        assertTrue(rec.closeThreshold() <= NoiseSquelch.MAXIMUM_NOISE_THRESHOLD);
        assertTrue(rec.hysteresisOpen() >= 1 && rec.hysteresisOpen() <= 10);
        assertTrue(rec.hysteresisClose() >= rec.hysteresisOpen() && rec.hysteresisClose() <= 10);

        //The ultimate validity check: NoiseSquelch must accept the recommended values without throwing.
        NoiseSquelch squelch = new NoiseSquelch(0.1f, 0.19f, 4, 6);
        assertDoesNotThrow(() -> squelch.setNoiseThreshold(rec.openThreshold(), rec.closeThreshold()));
        assertDoesNotThrow(() -> squelch.setHysteresisThreshold(rec.hysteresisOpen(), rec.hysteresisClose()));
    }

    @Test
    public void returnsNullWithoutEnoughSamples()
    {
        SquelchAIAdvisor advisor = new SquelchAIAdvisor();
        for(int i = 0; i < SquelchAIAdvisor.MIN_CALIBRATION_SAMPLES - 1; i++)
        {
            advisor.recordVarianceSample(0.21f);
        }
        assertFalse(advisor.hasSufficientCalibrationData());
        assertNull(advisor.calibrate());
    }

    @Test
    public void idleNoiseFloor_setsOpenBelowTheFloor()
    {
        //Idle channel: a tight band of high (noise) variance around 0.200-0.220 (no signal cluster).
        float[] samples = new float[41];
        float min = Float.MAX_VALUE;
        for(int i = 0; i < samples.length; i++)
        {
            samples[i] = 0.200f + (i * 0.0005f); //0.200 .. 0.220
            min = Math.min(min, samples[i]);
        }

        SquelchAIAdvisor advisor = withSamples(samples);
        SquelchAIAdvisor.Recommendation rec = advisor.calibrate();

        assertValidNoiseSquelchSettings(rec);
        //The open threshold must sit below the noise floor so ambient noise never opens the squelch.
        assertTrue(rec.openThreshold() < min,
                "open (" + rec.openThreshold() + ") must be below the noise floor (" + min + ")");
        //Schmitt-trigger gap: close must be strictly above open.
        assertTrue(rec.closeThreshold() > rec.openThreshold());
    }

    @Test
    public void idleNoiseFloor_usesDocumentedBaselineHysteresis()
    {
        //Stable, tight idle noise -> documented baseline temporal hysteresis (open=3 / close=6).
        float[] samples = new float[41];
        for(int i = 0; i < samples.length; i++)
        {
            samples[i] = 0.200f + (i * 0.0005f);
        }

        SquelchAIAdvisor.Recommendation rec = withSamples(samples).calibrate();
        assertNotNull(rec);
        assertEquals(3, rec.hysteresisOpen(), "stable idle capture should use the documented open hold of 3 (30 ms)");
        assertEquals(6, rec.hysteresisClose(), "stable idle capture should use the documented close hold of 6 (60 ms)");
    }

    @Test
    public void signalAndNoise_placesThresholdsBetweenClusters()
    {
        //A clean-signal cluster (~0.02) and a noise-floor cluster (~0.30).
        float[] samples = new float[60];
        for(int i = 0; i < 20; i++) samples[i] = 0.02f;
        for(int i = 20; i < 60; i++) samples[i] = 0.30f;

        SquelchAIAdvisor.Recommendation rec = withSamples(samples).calibrate();
        assertValidNoiseSquelchSettings(rec);
        //Open sits above the signal cluster and below the noise cluster; close above open and below the noise floor.
        assertTrue(rec.openThreshold() < 0.30f);
        assertTrue(rec.closeThreshold() < 0.30f);
        assertTrue(rec.closeThreshold() > rec.openThreshold());
    }

    @Test
    public void picketFencing_lengthensCloseHold()
    {
        //A widely fluctuating capture (fading / picket-fencing) -> longer close hold to bridge dropouts.
        float[] samples = new float[60];
        for(int i = 0; i < samples.length; i++)
        {
            samples[i] = 0.05f + ((i % 9) * 0.05f); //0.05 .. 0.45, large relative spread
        }

        SquelchAIAdvisor.Recommendation rec = withSamples(samples).calibrate();
        assertValidNoiseSquelchSettings(rec);
        assertTrue(rec.hysteresisClose() >= 7,
                "picket-fencing capture should lengthen the close hold (was " + rec.hysteresisClose() + ")");
    }
}
