package io.github.dsheirer.source.tuner.manager;

import io.github.dsheirer.source.tuner.TunerClass;
import java.lang.reflect.Field;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DiscoveredTunerRecoveryTest
{
    @Test
    void failedRestartDuringActiveRecoveryRemainsRecovering() throws Exception
    {
        RecoveringTuner tuner = new RecoveringTuner();
        setPrivateField(tuner, "mTunerStatus", TunerStatus.RECOVERING);
        setPrivateField(tuner, "mRecoveryTask", new IncompleteScheduledFuture());

        tuner.restart();

        assertEquals(1, tuner.getStartAttempts());
        assertEquals(TunerStatus.RECOVERING, tuner.getTunerStatus());
        assertFalse(tuner.hasTuner());
    }

    @Test
    void failedEnableDoesNotPublishEnabledStatus() throws Exception
    {
        RecoveringTuner tuner = new RecoveringTuner();

        tuner.setEnabled(false);
        tuner.setEnabled(true);

        try
        {
            assertEquals(1, tuner.getStartAttempts());
            assertEquals(TunerStatus.RECOVERING, tuner.getTunerStatus());
            assertFalse(tuner.hasTuner());
        }
        finally
        {
            cancelRecoveryTask(tuner);
        }
    }

    private static void setPrivateField(DiscoveredTuner tuner, String fieldName, Object value) throws Exception
    {
        Field field = DiscoveredTuner.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(tuner, value);
    }

    private static void cancelRecoveryTask(DiscoveredTuner tuner) throws Exception
    {
        Field field = DiscoveredTuner.class.getDeclaredField("mRecoveryTask");
        field.setAccessible(true);

        if(field.get(tuner) instanceof ScheduledFuture<?> scheduledFuture)
        {
            scheduledFuture.cancel(false);
        }
    }

    private static class RecoveringTuner extends DiscoveredTuner
    {
        private int mStartAttempts;

        @Override
        public TunerClass getTunerClass()
        {
            return TunerClass.TEST_TUNER;
        }

        @Override
        public String getId()
        {
            return "recovery-test-tuner";
        }

        @Override
        public void start()
        {
            mStartAttempts++;
            setErrorMessage("start failed");
        }

        int getStartAttempts()
        {
            return mStartAttempts;
        }
    }

    private static class IncompleteScheduledFuture implements ScheduledFuture<Object>
    {
        @Override
        public long getDelay(TimeUnit unit)
        {
            return unit.convert(1, TimeUnit.DAYS);
        }

        @Override
        public int compareTo(Delayed other)
        {
            return Long.compare(getDelay(TimeUnit.MILLISECONDS), other.getDelay(TimeUnit.MILLISECONDS));
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning)
        {
            return true;
        }

        @Override
        public boolean isCancelled()
        {
            return false;
        }

        @Override
        public boolean isDone()
        {
            return false;
        }

        @Override
        public Object get() throws InterruptedException, ExecutionException
        {
            return null;
        }

        @Override
        public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
        {
            return null;
        }
    }
}
