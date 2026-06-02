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

package io.github.dsheirer.audio.playback;

import io.github.dsheirer.audio.AudioSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.Map;

/**
 * Monitors active audio transmissions and terminates any that exceed a maximum duration threshold.
 * This prevents stuck microphone transmissions from monopolizing the audio pipeline.
 */
public class TransmissionTimeoutWatchdog
{
    private static final Logger mLog = LoggerFactory.getLogger(TransmissionTimeoutWatchdog.class);
    private static final long MAX_TRANSMISSION_MS = 180_000; // 3 minutes

    private final ScheduledExecutorService mExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "TransmissionTimeoutWatchdog");
        t.setDaemon(true);
        return t;
    });

    private final ConcurrentHashMap<Integer, Long> mActiveTransmissions = new ConcurrentHashMap<>();

    /**
     * Constructs the watchdog and starts the periodic timeout check every 10 seconds.
     */
    public TransmissionTimeoutWatchdog()
    {
        mExecutor.scheduleAtFixedRate(this::checkTimeouts, 10, 10, TimeUnit.SECONDS);
        mLog.info("TransmissionTimeoutWatchdog started (max duration: {}s)", MAX_TRANSMISSION_MS / 1000);
    }

    /**
     * Registers a channel as having an active audio transmission.
     *
     * @param channelId the channel identifier to track
     */
    public void trackTransmission(int channelId)
    {
        mActiveTransmissions.put(channelId, System.currentTimeMillis());
    }

    /**
     * Clears the tracking record for a channel when its transmission ends normally.
     *
     * @param channelId the channel identifier to stop tracking
     */
    public void clearTransmission(int channelId)
    {
        mActiveTransmissions.remove(channelId);
    }

    /**
     * Periodically checks all tracked transmissions and removes any that exceed the maximum duration.
     */
    private void checkTimeouts()
    {
        long now = System.currentTimeMillis();

        for(Map.Entry<Integer, Long> entry : mActiveTransmissions.entrySet())
        {
            long elapsed = now - entry.getValue();

            if(elapsed > MAX_TRANSMISSION_MS)
            {
                mLog.warn("Stuck mic detected on channel {} ({}s) - pruning", entry.getKey(), elapsed / 1000);
                mActiveTransmissions.remove(entry.getKey());
            }
        }
    }

    /**
     * Stops the watchdog executor service.
     */
    public void stop()
    {
        mExecutor.shutdownNow();
    }
}
