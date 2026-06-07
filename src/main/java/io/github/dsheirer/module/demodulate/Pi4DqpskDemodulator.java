package io.github.dsheirer.module.demodulate;

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.sample.complex.IComplexSamplesListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Pi4DqpskDemodulator implements IComplexSamplesListener {
    private static final Logger mLog = LoggerFactory.getLogger(Pi4DqpskDemodulator.class);

    private int mSampleRate;
    private int mSymbolRate;
    private Listener<ComplexSamples> mComplexSamplesListener;

    public Pi4DqpskDemodulator(int sampleRate, int symbolRate) {
        mSampleRate = sampleRate;
        mSymbolRate = symbolRate;
        mLog.info("Initialized Pi/4 DQPSK Demodulator at {} symbols/sec", mSymbolRate);
        mComplexSamplesListener = new Listener<ComplexSamples>() {
            @Override
            public void receive(ComplexSamples samples) {
                // demodulate
            }
        };
    }

    @Override
    public Listener<ComplexSamples> getComplexSamplesListener() {
        return mComplexSamplesListener;
    }

    public void reset() {
    }
}
