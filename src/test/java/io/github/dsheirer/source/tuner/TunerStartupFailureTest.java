package io.github.dsheirer.source.tuner;

import io.github.dsheirer.source.SourceException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class TunerStartupFailureTest
{
    @Test
    void failedStartStopsAndDisposesController() throws Exception
    {
        FailingTunerController controller = new FailingTunerController();
        TestTuner tuner = new TestTuner(controller);

        try
        {
            tuner.start();
            fail("Expected startup failure");
        }
        catch(SourceException expected)
        {
            //Expected
        }

        assertTrue(controller.isStopped());
        assertTrue(controller.isDisposed());
    }

    private static class TestTuner extends Tuner
    {
        TestTuner(TunerController controller)
        {
            super(controller, null);
        }

        @Override
        public String getUniqueID()
        {
            return "test";
        }

        @Override
        public int getMaximumUSBBitsPerSecond()
        {
            return 0;
        }

        @Override
        public TunerClass getTunerClass()
        {
            return TunerClass.TEST_TUNER;
        }

        @Override
        public String getPreferredName()
        {
            return "test";
        }

        @Override
        public double getSampleSize()
        {
            return 8.0;
        }
    }

    private static class FailingTunerController extends TunerController
    {
        private boolean mStopped;
        private boolean mDisposed;

        FailingTunerController()
        {
            super(null);
        }

        @Override
        public void start() throws SourceException
        {
            throw new SourceException("start failed");
        }

        @Override
        public void stop()
        {
            mStopped = true;
        }

        @Override
        protected void dispose()
        {
            mDisposed = true;
            super.dispose();
        }

        @Override
        public TunerType getTunerType()
        {
            return TunerType.TEST;
        }

        @Override
        public int getBufferSampleCount()
        {
            return 1024;
        }

        @Override
        public double getCurrentSampleRate()
        {
            return 0.0;
        }

        @Override
        public void setTunedFrequency(long frequency)
        {
        }

        @Override
        public long getTunedFrequency()
        {
            return 0;
        }

        boolean isStopped()
        {
            return mStopped;
        }

        boolean isDisposed()
        {
            return mDisposed;
        }
    }
}
