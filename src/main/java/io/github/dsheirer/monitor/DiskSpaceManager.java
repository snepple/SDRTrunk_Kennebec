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
package io.github.dsheirer.monitor;

import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.health.SystemHealthAlertEvent;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.util.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Monitors free disk space and automatically prunes the oldest audio recordings when space runs low, so
 * that unattended systems do not silently stop recording and streaming when the disk fills.
 *
 * Recordings are treated as expendable artifacts: when free space on the recording volume falls below the
 * minimum threshold, the oldest recording files are deleted until free space reaches the target threshold
 * (or no recordings remain).  A system health alert is posted whenever pruning occurs, and when the disk
 * remains low after pruning.
 *
 * Configuration (system property, or environment variable in parentheses):
 *   sdrtrunk.disk.autoPrune    (SDRTRUNK_DISK_AUTOPRUNE)     - "false" disables pruning (monitoring/alerts continue)
 *   sdrtrunk.disk.minFreeGb    (SDRTRUNK_DISK_MIN_FREE_GB)   - free-space threshold that triggers pruning, default 2
 *   sdrtrunk.disk.targetFreeGb (SDRTRUNK_DISK_TARGET_FREE_GB)- pruning stops once free space reaches this, default 5
 */
public class DiskSpaceManager
{
    private static final Logger mLog = LoggerFactory.getLogger(DiskSpaceManager.class);
    private static final long CHECK_INTERVAL_MINUTES = 15;
    private static final long GB = 1024L * 1024L * 1024L;

    private final UserPreferences mUserPreferences;
    private ScheduledFuture<?> mMonitorFuture;
    private boolean mLowSpaceAlerted = false;

    public DiskSpaceManager(UserPreferences userPreferences)
    {
        mUserPreferences = userPreferences;
    }

    public void start()
    {
        if(mMonitorFuture == null)
        {
            mMonitorFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(this::checkDiskSpace, 1,
                CHECK_INTERVAL_MINUTES, TimeUnit.MINUTES);
            mLog.info("Disk space manager started - minimum free [" + getMinFreeBytes() / GB +
                "GB] target free [" + getTargetFreeBytes() / GB + "GB] auto-prune [" + isAutoPruneEnabled() + "]");
        }
    }

    public void stop()
    {
        if(mMonitorFuture != null)
        {
            mMonitorFuture.cancel(false);
            mMonitorFuture = null;
        }
    }

    private static String getConfig(String property, String envVar)
    {
        return System.getProperty(property, System.getenv(envVar));
    }

    private static long getGbConfig(String property, String envVar, long defaultGb)
    {
        String value = getConfig(property, envVar);

        if(value != null)
        {
            try
            {
                return Long.parseLong(value.trim()) * GB;
            }
            catch(NumberFormatException e)
            {
                mLog.warn("Invalid disk space threshold [" + value + "] for " + property + " - using default " + defaultGb + "GB");
            }
        }

        return defaultGb * GB;
    }

    private boolean isAutoPruneEnabled()
    {
        String value = getConfig("sdrtrunk.disk.autoPrune", "SDRTRUNK_DISK_AUTOPRUNE");
        return value == null || !value.equalsIgnoreCase("false");
    }

    private long getMinFreeBytes()
    {
        return getGbConfig("sdrtrunk.disk.minFreeGb", "SDRTRUNK_DISK_MIN_FREE_GB", 2);
    }

    private long getTargetFreeBytes()
    {
        return Math.max(getGbConfig("sdrtrunk.disk.targetFreeGb", "SDRTRUNK_DISK_TARGET_FREE_GB", 5), getMinFreeBytes());
    }

    private void checkDiskSpace()
    {
        try
        {
            Path recordingDirectory = mUserPreferences.getDirectoryPreference().getDirectoryRecording();

            if(recordingDirectory == null || !Files.exists(recordingDirectory))
            {
                return;
            }

            long freeSpace = Files.getFileStore(recordingDirectory).getUsableSpace();
            long minFree = getMinFreeBytes();

            if(freeSpace >= minFree)
            {
                mLowSpaceAlerted = false;
                return;
            }

            mLog.warn("Low disk space on recording volume - free [" + (freeSpace / GB) + "GB] minimum [" +
                (minFree / GB) + "GB]");

            int pruned = 0;
            long target = getTargetFreeBytes();

            if(isAutoPruneEnabled())
            {
                pruned = pruneOldestRecordings(recordingDirectory, target);
                freeSpace = Files.getFileStore(recordingDirectory).getUsableSpace();
            }

            if(pruned > 0)
            {
                mLog.info("Pruned [" + pruned + "] oldest recordings - free space now [" + (freeSpace / GB) + "GB]");
                MyEventBus.getGlobalEventBus().post(new SystemHealthAlertEvent(
                    SystemHealthAlertEvent.AlertType.SYSTEM, "Disk Space - Recordings Pruned",
                    "Free disk space fell below " + (minFree / GB) + "GB - automatically deleted " + pruned +
                        " oldest recording(s)."));
            }

            if(freeSpace < minFree && !mLowSpaceAlerted)
            {
                mLowSpaceAlerted = true;
                MyEventBus.getGlobalEventBus().post(new SystemHealthAlertEvent(
                    SystemHealthAlertEvent.AlertType.SYSTEM, "Disk Space Low",
                    "Free disk space is " + (freeSpace / GB) + "GB" +
                        (isAutoPruneEnabled() ? " and no further recordings can be pruned" : " (auto-prune disabled)") +
                        " - recording and streaming may fail."));
            }
        }
        catch(Exception e)
        {
            mLog.error("Error checking disk space", e);
        }
    }

    /**
     * Deletes the oldest audio recordings until the target free space is reached.
     * @return number of files deleted
     */
    private int pruneOldestRecordings(Path recordingDirectory, long targetFreeBytes) throws IOException
    {
        List<Path> recordings;

        try(Stream<Path> paths = Files.list(recordingDirectory))
        {
            recordings = paths.filter(Files::isRegularFile)
                .filter(p -> {
                    String name = p.getFileName().toString().toLowerCase();
                    return name.endsWith(".mp3") || name.endsWith(".wav");
                })
                .sorted(Comparator.comparingLong(p -> p.toFile().lastModified()))
                .toList();
        }

        int pruned = 0;

        for(Path recording : recordings)
        {
            if(Files.getFileStore(recordingDirectory).getUsableSpace() >= targetFreeBytes)
            {
                break;
            }

            try
            {
                Files.deleteIfExists(recording);
                pruned++;
            }
            catch(IOException e)
            {
                mLog.warn("Unable to delete recording [" + recording + "] - " + e.getMessage());
            }
        }

        return pruned;
    }
}
