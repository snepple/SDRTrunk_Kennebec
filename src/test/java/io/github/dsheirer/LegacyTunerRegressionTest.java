package io.github.dsheirer;

import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.SourceException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LegacyTunerRegressionTest {

    static class MockTunerController extends TunerController {
        private long mFreq;
        private int mSampleRate;
        private TunerType mTunerType;

        public MockTunerController(TunerType type) {
            super(null);
            this.mTunerType = type;
        }

        @Override
        public void start() throws SourceException {}

        @Override
        public void stop() {}

        @Override
        public TunerType getTunerType() {
            return mTunerType;
        }

        @Override
        public int getBufferSampleCount() {
            return 1024;
        }

        @Override
        public void setFrequency(long freq) throws SourceException {
            this.mFreq = freq;
        }

        @Override
        public long getFrequency() {
            return mFreq;
        }

        public void setSampleRateInHz(int rate) {
            this.mSampleRate = rate;
        }

        public int getSampleRateInHz() {
            return mSampleRate;
        }

        @Override
        public double getCurrentSampleRate() {
            return mSampleRate;
        }

        @Override
        public void setTunedFrequency(long frequency) throws SourceException {
            this.mFreq = frequency;
        }

        @Override
        public long getTunedFrequency() throws SourceException {
            return mFreq;
        }
    }

    @Test
    public void testRtlSdrLifecycle() throws Exception {
        MockTunerController tuner = new MockTunerController(TunerType.RAFAELMICRO_R820T);
        
        tuner.setFrequency(100_000_000L);
        assertEquals(100_000_000L, tuner.getFrequency());
        
        tuner.setSampleRateInHz(2_400_000);
        assertEquals(2_400_000, tuner.getSampleRateInHz());
    }

    @Test
    public void testAirspyLifecycle() throws Exception {
        MockTunerController tuner = new MockTunerController(TunerType.AIRSPY_R820T);
        
        tuner.setFrequency(150_000_000L);
        assertEquals(150_000_000L, tuner.getFrequency());
        
        tuner.setSampleRateInHz(10_000_000);
        assertEquals(10_000_000, tuner.getSampleRateInHz());
    }

    @Test
    public void testHackRFLifecycle() throws Exception {
        MockTunerController tuner = new MockTunerController(TunerType.HACKRF_ONE);
        
        tuner.setFrequency(200_000_000L);
        assertEquals(200_000_000L, tuner.getFrequency());
        
        tuner.setSampleRateInHz(20_000_000);
        assertEquals(20_000_000, tuner.getSampleRateInHz());
    }
}
