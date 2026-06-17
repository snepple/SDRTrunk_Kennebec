/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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
package io.github.dsheirer.controller.channel;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.prefs.Preferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks per-channel decode activity (a running count of decode events) so that auto-start ordering can
 * favour the channels that have historically been most active.
 *
 * Counts are kept in memory and incremented cheaply on the decode thread (a single atomic increment per
 * event).  They are persisted to the user {@link Preferences} store on a throttled basis and via a JVM
 * shutdown hook, and reloaded on first use, so a channel's accumulated activity survives across sessions
 * and is available for ordering at the next start-up.
 */
public class ChannelActivityTracker
{
    private static final Logger mLog = LoggerFactory.getLogger(ChannelActivityTracker.class);
    private static final String KEY_PREFIX = "channel.activity.";
    private static final long FLUSH_INTERVAL_MS = 120_000; //Persist at most every two minutes.

    private static final ChannelActivityTracker INSTANCE = new ChannelActivityTracker();

    private final Preferences mPreferences = Preferences.userNodeForPackage(ChannelActivityTracker.class);
    private final ConcurrentHashMap<String,AtomicLong> mActivity = new ConcurrentHashMap<>();
    private final AtomicBoolean mDirty = new AtomicBoolean(false);
    private volatile long mLastFlush = System.currentTimeMillis();

    private ChannelActivityTracker()
    {
        Runtime.getRuntime().addShutdownHook(new Thread(this::flush, "sdrtrunk-channel-activity-flush"));
    }

    /**
     * @return the shared tracker instance.
     */
    public static ChannelActivityTracker getInstance()
    {
        return INSTANCE;
    }

    /**
     * Records one unit of decode activity for the named channel.  Cheap (one atomic increment) and safe to
     * call from the decode thread.
     * @param channelName to credit, ignored when null/empty.
     */
    public void recordActivity(String channelName)
    {
        if(channelName == null || channelName.isEmpty())
        {
            return;
        }

        currentCounter(channelName).incrementAndGet();
        mDirty.set(true);

        if((System.currentTimeMillis() - mLastFlush) >= FLUSH_INTERVAL_MS)
        {
            flush();
        }
    }

    /**
     * Returns the accumulated activity score for a channel (persisted history plus the current session).
     * @param channelName to look up.
     * @return activity score, or 0 when unknown.
     */
    public long getActivityScore(String channelName)
    {
        if(channelName == null || channelName.isEmpty())
        {
            return 0;
        }

        return currentCounter(channelName).get();
    }

    /**
     * Lazily loads the persisted count for a channel into the in-memory map and returns its counter.
     */
    private AtomicLong currentCounter(String channelName)
    {
        return mActivity.computeIfAbsent(channelName,
                name -> new AtomicLong(mPreferences.getLong(key(name), 0L)));
    }

    /**
     * Persists the in-memory counts to the preferences store when there are unsaved changes.
     */
    public synchronized void flush()
    {
        mLastFlush = System.currentTimeMillis();

        if(!mDirty.getAndSet(false))
        {
            return;
        }

        try
        {
            for(var entry : mActivity.entrySet())
            {
                mPreferences.putLong(key(entry.getKey()), entry.getValue().get());
            }
            mPreferences.flush();
        }
        catch(Exception e)
        {
            mLog.debug("Unable to persist channel activity counts", e);
        }
    }

    /**
     * Preferences keys are length-limited, so long channel names are hashed to stay within bounds.
     */
    private static String key(String channelName)
    {
        if((KEY_PREFIX.length() + channelName.length()) <= Preferences.MAX_KEY_LENGTH)
        {
            return KEY_PREFIX + channelName;
        }

        return KEY_PREFIX + Integer.toHexString(channelName.hashCode());
    }
}
