/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */
package io.github.dsheirer.util;

import io.github.dsheirer.controller.NamingThreadFactory;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.heartbeat.HeartbeatManager;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Threaded scheduled processor for receiving elements from a separate producer thread and forwarding those buffers to a
 * registered listener on this consumer/dispatcher thread.  Internally uses a single-thread thread pool to effect a
 * timer-based interval for processing to avoid excessive context switching inherent in a blocking queue.  Sizes the
 * thread pool to a single thread to ensure Garbage Collector can efficiently clean objects created on the thread.
 */
public class Dispatcher<E> implements Listener<E>
{
    private final static Logger mLog = LoggerFactory.getLogger(Dispatcher.class);
    private final LinkedTransferQueue<E> mQueue = new LinkedTransferQueue<>();
    private Listener<E> mListener;
    private final AtomicBoolean mRunning = new AtomicBoolean();
    private String mThreadName;
    private ScheduledExecutorService mExecutorService;
    private ScheduledFuture<?> mScheduledFuture;
    private final long mInterval;
    private HeartbeatManager mHeartbeatManager;

    //Optional overflow protection.  When mMaximumQueueSize > 0 the queue is bounded: once the backlog reaches the
    //maximum, inbound elements are dropped (drop-newest) until the backlog drains below mResetThreshold.  This
    //converts a consumer that cannot keep up (e.g. the polyphase channelizer at high sample rates) from an
    //unbounded memory-growth freeze into survivable, logged sample dropping.  Default 0 = unbounded (legacy
    //behavior, no change for existing callers).
    private volatile int mMaximumQueueSize = 0;
    private int mResetThreshold = 0;
    private final AtomicInteger mQueueSize = new AtomicInteger(0);
    private final AtomicBoolean mOverflow = new AtomicBoolean(false);
    private long mDroppedSinceLastLog = 0;
    private long mDroppedTotal = 0;
    private long mLastOverflowLogTimestamp = 0;
    private static final long OVERFLOW_LOG_INTERVAL_MS = 5000;

    /**
     * Constructs an instance of a Dispatcher with integrated heartbeat support.
     * @param threadName to name the dispatcher thread
     * @param interval for processing each batch in milliseconds.
     * @param heartbeatManager to receive a heartbeat command at each processing interval.
     */
    public Dispatcher(String threadName, long interval, HeartbeatManager heartbeatManager)
    {
        this(threadName, interval);
        mHeartbeatManager = heartbeatManager;
    }

    /**
     * Constructs an instance
     * @param threadName to name the dispatcher thread
     * @param interval for processing each batch in milliseconds.
     */
    public Dispatcher(String threadName, long interval)
    {
        mThreadName = threadName;
        mInterval = interval;
    }

    /**
     * Sets the thread name.  If this dispatcher is already started, this has no effect.
     * @param threadName to use for this dispatcher.
     */
    public void setThreadName(String threadName)
    {
        mThreadName = threadName;
    }

    /**
     * Sets or changes the listener to receive buffers from this processor.
     * @param listener to receive buffers
     */
    public void setListener(Listener<E> listener)
    {
        mListener = listener;
    }

    /**
     * Bounds the internal queue to enable overflow protection.  When the backlog reaches maximumQueueSize, inbound
     * elements are dropped until the backlog drains below half of that value, and a throttled warning is logged.
     * This prevents an overwhelmed consumer (e.g. the polyphase channelizer at high sample rates) from growing the
     * queue without limit and freezing the application.  Set to 0 (default) for an unbounded queue.
     * @param maximumQueueSize maximum number of queued elements, or 0 for unbounded.
     */
    public void setMaximumQueueSize(int maximumQueueSize)
    {
        mMaximumQueueSize = Math.max(0, maximumQueueSize);
        mResetThreshold = mMaximumQueueSize / 2;
    }

    /**
     * Primary input method for adding buffers to this processor.  Note: incoming buffers will be ignored if this
     * processor is in a stopped state.  You must invoke start() to allow incoming buffers and initiate buffer
     * processing.
     *
     * @param e to enqueue for distribution to a registered listener
     */
    public void receive(E e)
    {
        if(mRunning.get())
        {
            if(mMaximumQueueSize > 0)
            {
                int size = mQueueSize.get();

                if(mOverflow.get())
                {
                    //Shedding load: keep dropping until the backlog drains below the reset threshold (hysteresis).
                    if(size <= mResetThreshold)
                    {
                        mOverflow.set(false);
                        mLog.warn("[{}] recovered from processor overload - resumed after dropping {} element(s) total",
                                mThreadName, mDroppedTotal);
                    }
                    else
                    {
                        recordDrop();
                        return;
                    }
                }
                else if(size >= mMaximumQueueSize)
                {
                    mOverflow.set(true);
                    recordDrop();
                    return;
                }

                mQueue.add(e);
                mQueueSize.incrementAndGet();
            }
            else
            {
                mQueue.add(e);
            }
        }
    }

    /**
     * Records a dropped element and emits a throttled overload warning.  Only ever called from the producer thread
     * (via {@link #receive(Object)}), so the drop counters require no additional synchronization.
     */
    private void recordDrop()
    {
        mDroppedTotal++;
        mDroppedSinceLastLog++;

        long now = System.currentTimeMillis();

        if(now - mLastOverflowLogTimestamp >= OVERFLOW_LOG_INTERVAL_MS)
        {
            mLastOverflowLogTimestamp = now;
            mLog.warn("[{}] processor overloaded - dropped {} element(s) in the last ~{}ms (queue limit {}). The " +
                    "consumer cannot keep up with the input rate - reduce sample rate or channel count, or free up CPU.",
                    mThreadName, mDroppedSinceLastLog, OVERFLOW_LOG_INTERVAL_MS, mMaximumQueueSize);
            mDroppedSinceLastLog = 0;
        }
    }

    /**
     * Starts this buffer processor and allows queuing of incoming buffers.
     */
    public void start()
    {
        if(mRunning.compareAndSet(false, true))
        {
            if(mScheduledFuture != null)
            {
                //Note: this has to be false because downstream implementations may have acquired locks and they must
                //be able to release those locks or we'll get a deadlock situation.
                mScheduledFuture.cancel(false);
            }

            if(mExecutorService != null)
            {
                mExecutorService.shutdown();
                mExecutorService = null;
            }

            mQueue.clear();
            mQueueSize.set(0);
            mOverflow.set(false);
            mExecutorService = Executors.newSingleThreadScheduledExecutor(new NamingThreadFactory(mThreadName));

            Runnable r = (mHeartbeatManager != null ? new ProcessorWithHeartbeat() : new Processor());
            mScheduledFuture = mExecutorService.scheduleAtFixedRate(r, 0, mInterval, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Stops this buffer processor and waits up to two seconds for the processing thread to terminate.
     */
    public void stop()
    {
        if(mRunning.compareAndSet(true, false))
        {
            if(mScheduledFuture != null)
            {
                //Note: this has to be false because downstream implementations may have acquired locks and they must
                //be able to release those locks or we'll get a deadlock situation.
                mScheduledFuture.cancel(false);
                mScheduledFuture = null;
                mQueue.clear();
                mQueueSize.set(0);
                mOverflow.set(false);
            }

            if(mExecutorService != null)
            {
                mExecutorService.shutdown();
                mExecutorService = null;
            }
        }
    }

    /**
     * Stops this buffer processor and flushes the queue to the listener
     */
    public void flushAndStop()
    {
        if(mRunning.compareAndSet(true, false))
        {
            if(mScheduledFuture != null)
            {
                //Note: this has to be false because downstream implementations may have acquired locks and they must
                //be able to release those locks or we'll get a deadlock situation.
                mScheduledFuture.cancel(false);
                mScheduledFuture = null;
            }

            if(mExecutorService != null)
            {
                mExecutorService.shutdown();
                mExecutorService = null;
            }

            List<E> elements = new ArrayList<>();

            mQueue.drainTo(elements);

            if(mMaximumQueueSize > 0 && !elements.isEmpty())
            {
                mQueueSize.addAndGet(-elements.size());
            }

            for(E element: elements)
            {
                if(mListener != null)
                {
                    try
                    {
                        mListener.receive(element);
                    }
                    catch(Throwable t)
                    {
                        mLog.error("Error while flusing and dispatching element [" + element.getClass() + "] to listener [" +
                                mListener.getClass() + "]", t);
                    }
                }
            }
        }
    }

    /**
     * Indicates if this processor is currently running
     */
    public boolean isRunning()
    {
        return mRunning.get();
    }

    /**
     * Processes elements from the queue.  Note: this should only be invoked on the Processor thread.
     */
    private void process()
    {
        List<E> elements = new ArrayList<>();

        mQueue.drainTo(elements);

        if(mMaximumQueueSize > 0 && !elements.isEmpty())
        {
            mQueueSize.addAndGet(-elements.size());
        }

        for(E element: elements)
        {
            if(mRunning.get() && mListener != null)
            {
                try
                {
                    mListener.receive(element);
                }
                catch(Throwable t)
                {
                    mLog.error("Error while dispatching element [" + element.getClass() + "] to listener [" +
                            mListener.getClass() + "]", t);
                }
            }
        }
    }

    /**
     * Processor to service the buffer queue and distribute the buffers to the registered listener
     */
    class Processor implements Runnable
    {
        private final AtomicBoolean mRunning = new AtomicBoolean();

        @Override
        public void run()
        {
            if(mRunning.compareAndSet(false, true))
            {
                process();
                mRunning.set(false);
            }
        }
    }

    /**
     * Processor to service the buffer queue and distribute the buffers to the registered listener.  Includes a
     * support for commanding a heart beat with each processing interval.
     */
    class ProcessorWithHeartbeat implements Runnable
    {
        private final AtomicBoolean mRunning = new AtomicBoolean();

        @Override
        public void run()
        {
            if(mRunning.compareAndSet(false, true))
            {
                process();

                try
                {
                    mHeartbeatManager.broadcast();
                }
                catch(Throwable t)
                {
                    mLog.error("Error broadcasting heartbeat during Dispatcher processing interval", t);
                }

                mRunning.set(false);
            }
        }
    }
}
