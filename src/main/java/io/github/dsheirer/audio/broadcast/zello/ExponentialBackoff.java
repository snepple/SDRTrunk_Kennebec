package io.github.dsheirer.audio.broadcast.zello;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements exponential backoff with jitter for Zello WebSocket reconnection.
 * Prevents thundering herd on server recovery and respects rate limits.
 */
public class ExponentialBackoff
{
    private static final Logger mLog = LoggerFactory.getLogger(ExponentialBackoff.class);

    private final long mInitialDelayMs;
    private final long mMaxDelayMs;
    private final double mMultiplier;
    private int mAttempt = 0;

    public ExponentialBackoff(long initialMs, long maxMs, double multiplier)
    {
        mInitialDelayMs = initialMs;
        mMaxDelayMs = maxMs;
        mMultiplier = multiplier;
    }

    /**
     * Default: 1s initial, 60s max, 2x multiplier
     */
    public ExponentialBackoff()
    {
        this(1000, 60000, 2.0);
    }

    /**
     * Calculates the next delay with jitter (+/- 10%).
     * @return delay in milliseconds
     */
    public long nextDelay()
    {
        long delay = (long)(mInitialDelayMs * Math.pow(mMultiplier, mAttempt));
        delay = Math.min(delay, mMaxDelayMs);

        // Add jitter: +/- 10%
        delay += (long)(delay * (Math.random() * 0.2 - 0.1));
        mAttempt++;

        mLog.debug("Backoff attempt {}: {}ms", mAttempt, delay);
        return delay;
    }

    /**
     * Reset the backoff counter after a successful connection.
     */
    public void reset()
    {
        mAttempt = 0;
    }

    public int getAttempt()
    {
        return mAttempt;
    }
}
