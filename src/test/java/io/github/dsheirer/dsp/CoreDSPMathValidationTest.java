package io.github.dsheirer.dsp;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import io.github.dsheirer.dsp.fm.*;
import io.github.dsheirer.dsp.psk.demod.*;
import java.util.Arrays;
import org.apache.commons.math3.util.FastMath;

public class CoreDSPMathValidationTest {

    private static final int BUFFER_SIZE = 1024;
    private static final float DELTA = 1e-4f;

    @Test
    public void testFmDemodulators() {
        float[] iBuffer = new float[BUFFER_SIZE];
        float[] qBuffer = new float[BUFFER_SIZE];
        
        // Synthetic I/Q reference buffer
        for (int i = 0; i < BUFFER_SIZE; i++) {
            iBuffer[i] = (float) Math.cos(2 * Math.PI * i / 100.0);
            qBuffer[i] = (float) Math.sin(2 * Math.PI * i / 100.0);
        }

        // Known pre-calculated standard for FM Demodulation
        float[] expected = new float[BUFFER_SIZE];
        float previousI = 0.0f;
        float previousQ = 0.0f;
        for (int x = 0; x < BUFFER_SIZE; x++) {
            float demodI = (iBuffer[x] * previousI) - (qBuffer[x] * -previousQ);
            float demodQ = (qBuffer[x] * previousI) + (iBuffer[x] * -previousQ);
            if (demodI != 0) {
                expected[x] = (float) FastMath.atan2(demodQ, demodI);
            } else {
                expected[x] = 0.0f;
            }
            previousI = iBuffer[x];
            previousQ = qBuffer[x];
        }

        ScalarFMDemodulator scalar = new ScalarFMDemodulator();
        float[] outScalar = scalar.demodulate(Arrays.copyOf(iBuffer, BUFFER_SIZE), Arrays.copyOf(qBuffer, BUFFER_SIZE));
        assertArrayEquals(expected, outScalar, DELTA, "ScalarFMDemodulator mismatch");

        VectorFMDemodulator64 v64 = new VectorFMDemodulator64();
        float[] out64 = v64.demodulate(Arrays.copyOf(iBuffer, BUFFER_SIZE), Arrays.copyOf(qBuffer, BUFFER_SIZE));
        assertArrayEquals(expected, out64, DELTA, "VectorFMDemodulator64 mismatch");

        VectorFMDemodulator128 v128 = new VectorFMDemodulator128();
        float[] out128 = v128.demodulate(Arrays.copyOf(iBuffer, BUFFER_SIZE), Arrays.copyOf(qBuffer, BUFFER_SIZE));
        assertArrayEquals(expected, out128, DELTA, "VectorFMDemodulator128 mismatch");

        VectorFMDemodulator256 v256 = new VectorFMDemodulator256();
        float[] out256 = v256.demodulate(Arrays.copyOf(iBuffer, BUFFER_SIZE), Arrays.copyOf(qBuffer, BUFFER_SIZE));
        assertArrayEquals(expected, out256, DELTA, "VectorFMDemodulator256 mismatch");

        VectorFMDemodulator512 v512 = new VectorFMDemodulator512();
        float[] out512 = v512.demodulate(Arrays.copyOf(iBuffer, BUFFER_SIZE), Arrays.copyOf(qBuffer, BUFFER_SIZE));
        assertArrayEquals(expected, out512, DELTA, "VectorFMDemodulator512 mismatch");
    }

    @Test
    public void testDifferentialDemodulators() {
        float[] iBuffer = new float[BUFFER_SIZE];
        float[] qBuffer = new float[BUFFER_SIZE];
        for (int i = 0; i < BUFFER_SIZE; i++) {
            iBuffer[i] = (float) Math.cos(2 * Math.PI * i / 50.0);
            qBuffer[i] = (float) Math.sin(2 * Math.PI * i / 50.0);
        }

        double sampleRate = 48000;
        int symbolRate = 4800;

        DifferentialDemodulatorFloatScalar scalar = new DifferentialDemodulatorFloatScalar(sampleRate, symbolRate);
        float[] baseline = scalar.demodulate(Arrays.copyOf(iBuffer, BUFFER_SIZE), Arrays.copyOf(qBuffer, BUFFER_SIZE));

        DifferentialDemodulatorFloatVector64 v64 = new DifferentialDemodulatorFloatVector64(sampleRate, symbolRate);
        float[] out64 = v64.demodulate(Arrays.copyOf(iBuffer, BUFFER_SIZE), Arrays.copyOf(qBuffer, BUFFER_SIZE));
        assertArrayEquals(baseline, out64, DELTA, "DifferentialDemodulatorFloatVector64 mismatch");

        DifferentialDemodulatorFloatVector128 v128 = new DifferentialDemodulatorFloatVector128(sampleRate, symbolRate);
        float[] out128 = v128.demodulate(Arrays.copyOf(iBuffer, BUFFER_SIZE), Arrays.copyOf(qBuffer, BUFFER_SIZE));
        assertArrayEquals(baseline, out128, DELTA, "DifferentialDemodulatorFloatVector128 mismatch");

        DifferentialDemodulatorFloatVector256 v256 = new DifferentialDemodulatorFloatVector256(sampleRate, symbolRate);
        float[] out256 = v256.demodulate(Arrays.copyOf(iBuffer, BUFFER_SIZE), Arrays.copyOf(qBuffer, BUFFER_SIZE));
        assertArrayEquals(baseline, out256, DELTA, "DifferentialDemodulatorFloatVector256 mismatch");

        DifferentialDemodulatorFloatVector512 v512 = new DifferentialDemodulatorFloatVector512(sampleRate, symbolRate);
        float[] out512 = v512.demodulate(Arrays.copyOf(iBuffer, BUFFER_SIZE), Arrays.copyOf(qBuffer, BUFFER_SIZE));
        assertArrayEquals(baseline, out512, DELTA, "DifferentialDemodulatorFloatVector512 mismatch");
    }
}
