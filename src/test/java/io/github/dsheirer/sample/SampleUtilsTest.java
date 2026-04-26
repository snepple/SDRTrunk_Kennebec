package io.github.dsheirer.sample;

import io.github.dsheirer.sample.complex.Complex;
import io.github.dsheirer.sample.complex.ComplexSamples;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SampleUtilsTest {

    @Test
    public void testDeinterleave() {
        float[] samples = {1.0f, 2.0f, 3.0f, 4.0f};
        long timestamp = 12345L;
        ComplexSamples result = SampleUtils.deinterleave(samples, timestamp);

        assertEquals(2, result.length());
        assertArrayEquals(new float[]{1.0f, 3.0f}, result.i());
        assertArrayEquals(new float[]{2.0f, 4.0f}, result.q());
        assertEquals(timestamp, result.timestamp());
    }

    @Test
    public void testDeinterleaveEmpty() {
        float[] samples = {};
        ComplexSamples result = SampleUtils.deinterleave(samples, 0L);
        assertEquals(0, result.length());
        assertEquals(0, result.i().length);
        assertEquals(0, result.q().length);
    }

    @Test
    public void testDeinterleaveNull() {
        assertThrows(NullPointerException.class, () -> SampleUtils.deinterleave(null, 0L));
    }

    @Test
    public void testDeinterleaveOddLength() {
        float[] samples = {1.0f, 2.0f, 3.0f};
        // Current implementation uses samples.length / 2 for both I and Q arrays.
        // For length 3, 3/2 = 1. i.length = 1, q.length = 1.
        // It will only process the first pair.
        ComplexSamples result = SampleUtils.deinterleave(samples, 0L);
        assertEquals(1, result.length());
        assertArrayEquals(new float[]{1.0f}, result.i());
        assertArrayEquals(new float[]{2.0f}, result.q());
    }

    @Test
    public void testInterleave() {
        float[] i = {1.0f, 3.0f};
        float[] q = {2.0f, 4.0f};
        ComplexSamples complexSamples = new ComplexSamples(i, q, 0L);
        float[] result = SampleUtils.interleave(complexSamples);

        assertArrayEquals(new float[]{1.0f, 2.0f, 3.0f, 4.0f}, result);
    }

    @Test
    public void testMultiply() {
        Complex a = new Complex(1.0f, 2.0f);
        Complex b = new Complex(3.0f, 4.0f);
        // (1 + 2i) * (3 + 4i) = (3 - 8) + (4 + 6)i = -5 + 10i
        Complex result = SampleUtils.multiply(a, b);
        assertEquals(-5.0f, result.inphase(), 0.00001f);
        assertEquals(10.0f, result.quadrature(), 0.00001f);
    }

    @Test
    public void testMinus() {
        Complex a = new Complex(5.0f, 6.0f);
        Complex b = new Complex(1.0f, 2.0f);
        Complex result = SampleUtils.minus(a, b);
        assertEquals(4.0f, result.inphase(), 0.00001f);
        assertEquals(4.0f, result.quadrature(), 0.00001f);
    }

    @Test
    public void testMagnitude() {
        Complex a = new Complex(3.0f, 4.0f);
        double result = SampleUtils.magnitude(a);
        assertEquals(5.0, result, 0.00001);
    }

    @Test
    public void testMagnitudeSquared() {
        Complex a = new Complex(3.0f, 4.0f);
        int result = SampleUtils.magnitudeSquared(a);
        assertEquals(25, result);
    }
}
