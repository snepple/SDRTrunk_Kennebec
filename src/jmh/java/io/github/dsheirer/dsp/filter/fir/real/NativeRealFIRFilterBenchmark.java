package io.github.dsheirer.dsp.filter.fir.real;

import org.openjdk.jmh.annotations.*;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class NativeRealFIRFilterBenchmark {

    private float[] samples;
    private float[] coefficients;
    private RealFIRFilter javaFilter;
    private NativeRealFIRFilter nativeFilter;

    @Setup(Level.Trial)
    public void setup() {
        NativeLibraryLoader.isLoaded(); // Ensure it is loaded

        Random random = new Random();
        int sampleSize = 2048;
        samples = new float[sampleSize];
        for(int x = 0; x < samples.length; x++) {
            samples[x] = random.nextFloat() * 2.0f - 1.0f;
        }

        coefficients = new float[99];
        for (int i = 0; i < coefficients.length; i++) {
            coefficients[i] = random.nextFloat() * 2.0f - 1.0f;
        }

        javaFilter = new RealFIRFilter(coefficients);
        nativeFilter = new NativeRealFIRFilter(coefficients);
    }

    @Benchmark
    public float[] benchmarkJava() {
        return javaFilter.filter(samples);
    }

    @Benchmark
    public float[] benchmarkNative() {
        return nativeFilter.filter(samples);
    }
}
