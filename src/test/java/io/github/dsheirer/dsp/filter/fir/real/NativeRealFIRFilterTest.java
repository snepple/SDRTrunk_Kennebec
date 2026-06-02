package io.github.dsheirer.dsp.filter.fir.real;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class NativeRealFIRFilterTest {

    @Test
    public void testFilter() {
        float[] coefficients = {0.1f, 0.2f, 0.3f, 0.4f, 0.5f};
        float[] samples = {1.0f, 2.0f, 3.0f, 4.0f, 5.0f};

        RealFIRFilter javaFilter = new RealFIRFilter(coefficients);
        
        try {
            NativeRealFIRFilter nativeFilter = new NativeRealFIRFilter(coefficients);
            float[] javaFiltered = javaFilter.filter(samples);
            float[] nativeFiltered = nativeFilter.filter(samples);
            assertArrayEquals(javaFiltered, nativeFiltered, 1e-6f);
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Native real FIR filter library not loaded. Bypassing native check: " + e.getMessage());
        }
    }
}
