package io.github.dsheirer.sample.complex;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class InterleavedComplexSamplesTest {

    @Test
    public void testToDeinterleaved() {
        float[] samples = {1.0f, 2.0f, 3.0f, 4.0f};
        long timestamp = 12345L;
        InterleavedComplexSamples interleaved = new InterleavedComplexSamples(samples, timestamp);
        ComplexSamples result = interleaved.toDeinterleaved();

        assertEquals(2, result.length());
        assertArrayEquals(new float[]{1.0f, 3.0f}, result.i(), "In-phase samples should be 1.0 and 3.0");
        assertArrayEquals(new float[]{2.0f, 4.0f}, result.q(), "Quadrature samples should be 2.0 and 4.0");
        assertEquals(timestamp, result.timestamp());
    }
}
