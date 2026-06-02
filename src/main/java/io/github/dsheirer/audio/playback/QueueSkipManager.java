package io.github.dsheirer.audio.playback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages live playback queue-skip for priority traffic.
 * When priority mode is enabled, non-priority audio segments are flushed from the buffer queue
 * to ensure immediate playback of high-priority transmissions (e.g. emergency calls).
 */
public class QueueSkipManager
{
    private static final Logger mLog = LoggerFactory.getLogger(QueueSkipManager.class);
    private final AtomicBoolean mPriorityMode = new AtomicBoolean(false);

    public void enablePriorityMode()
    {
        mPriorityMode.set(true);
        mLog.info("Queue-Skip: PRIORITY MODE enabled - flushing non-priority audio");
    }

    public void disablePriorityMode()
    {
        mPriorityMode.set(false);
        mLog.info("Queue-Skip: PRIORITY MODE disabled");
    }

    public boolean isPriorityMode()
    {
        return mPriorityMode.get();
    }

    /**
     * Flushes all items from the provided queue.
     * @param queue the audio queue to flush
     * @return number of items flushed
     */
    public <T> int flushQueue(Queue<T> queue)
    {
        int flushed = 0;
        while(!queue.isEmpty())
        {
            queue.poll();
            flushed++;
        }

        if(flushed > 0)
        {
            mLog.info("Queue-Skip: flushed {} queued audio segments", flushed);
        }

        return flushed;
    }
}
