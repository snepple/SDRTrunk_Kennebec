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

package io.github.dsheirer.source.tuner.manager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks USB tuner start failures that crash or hang the entire application so a persistently-bad device can be
 * quarantined (skipped) on subsequent launches instead of crashing the application again and again.
 *
 * Why this is needed: opening/streaming a faulted USB tuner can trip a native assertion inside libusb
 * (poll_windows.c "assert(fd != NULL)") that pops a modal Windows dialog and hangs/aborts the whole process.  A
 * native assertion cannot be caught or recovered from in Java, so the only robust defense for an unattended/remote
 * installation is to stop initializing the offending device entirely.  Because the crash happens during the device's
 * start - before any normal error handling runs - the decision has to survive restarts.
 *
 * Mechanism (crash attribution): immediately before a tuner's start() is attempted, the tuner's id is written to a
 * small "pending" marker file and flushed to disk.  If start() returns (success or a handled error), the marker is
 * cleared.  If instead the JVM dies/hangs during start(), the marker survives; on the next launch its presence is
 * attributed as a crash for that tuner id and its crash counter is incremented.  Once a tuner reaches the crash
 * threshold the manager disables it (persisted), so it is skipped on all later launches until a human re-enables it.
 *
 * Configuration (system property):
 *   sdrtrunk.tuner.fault.quarantine.threshold - consecutive crashes before quarantine (default 3, minimum 1)
 */
public class TunerFaultTracker
{
    private static final Logger mLog = LoggerFactory.getLogger(TunerFaultTracker.class);
    private static final String FILE_NAME = "tuner_faults.properties";
    private static final String KEY_PENDING_ID = "pending.id";
    private static final String KEY_PENDING_TIME = "pending.time";
    private static final String CRASH_PREFIX = "crashes.";
    private static final String QUARANTINE_PREFIX = "quarantine.";
    private static final int DEFAULT_THRESHOLD = 3;

    private final Path mFile;
    private final Properties mProperties = new Properties();
    private final int mThreshold;

    /**
     * Creates the tracker, loads any persisted state, and resolves a pending marker left by a crash on the previous
     * run (attributing the crash to the tuner that was mid-start when the JVM died).
     *
     * @param configurationDirectory directory in which to persist the fault file
     */
    public TunerFaultTracker(Path configurationDirectory)
    {
        mFile = configurationDirectory != null ? configurationDirectory.resolve(FILE_NAME) : null;
        mThreshold = Math.max(1, Integer.getInteger("sdrtrunk.tuner.fault.quarantine.threshold", DEFAULT_THRESHOLD));
        load();
        resolvePendingCrash();
    }

    /**
     * Marks that a tuner start is about to be attempted, flushing the marker to disk so it survives a crash/hang
     * during start().  Must be called immediately before the start() that can trip the native assertion.
     * @param tunerId of the tuner being started
     */
    public synchronized void beginStartAttempt(String tunerId)
    {
        if(tunerId == null)
        {
            return;
        }

        mProperties.setProperty(KEY_PENDING_ID, tunerId);
        mProperties.setProperty(KEY_PENDING_TIME, Long.toString(System.currentTimeMillis()));
        save();
    }

    /**
     * Marks that a tuner start completed without crashing the JVM (whether it succeeded or failed gracefully), so the
     * pending marker is cleared and the crash cannot be misattributed.  On a successful start the crash counter for
     * the tuner is also reset, since the device is currently healthy.
     * @param tunerId of the tuner whose start attempt finished
     * @param started true if the tuner actually started (so its crash history can be cleared)
     */
    public synchronized void endStartAttempt(String tunerId, boolean started)
    {
        if(tunerId != null && tunerId.equals(mProperties.getProperty(KEY_PENDING_ID)))
        {
            mProperties.remove(KEY_PENDING_ID);
            mProperties.remove(KEY_PENDING_TIME);
        }

        if(started && tunerId != null)
        {
            mProperties.remove(crashKey(tunerId));
        }

        save();
    }

    /**
     * @return number of consecutive crashes attributed to the tuner id.
     */
    public synchronized int getCrashCount(String tunerId)
    {
        if(tunerId == null)
        {
            return 0;
        }

        try
        {
            return Integer.parseInt(mProperties.getProperty(crashKey(tunerId), "0"));
        }
        catch(NumberFormatException e)
        {
            return 0;
        }
    }

    /**
     * @return true if the tuner has crashed the application at least the configured threshold number of times and
     * should be quarantined (skipped) rather than started again.
     */
    public synchronized boolean shouldQuarantine(String tunerId)
    {
        return getCrashCount(tunerId) >= mThreshold;
    }

    /**
     * @return the configured crash threshold for quarantine.
     */
    public int getThreshold()
    {
        return mThreshold;
    }

    /**
     * Clears the crash history for a tuner - e.g. after a user re-enables a quarantined tuner so it gets a fresh set
     * of attempts.
     */
    public synchronized void clearCrashHistory(String tunerId)
    {
        if(tunerId != null)
        {
            mProperties.remove(crashKey(tunerId));
            save();
        }
    }

    /**
     * Records that a tuner has been quarantined (auto-disabled because it repeatedly crashed the application during
     * start), capturing why and when so the user can be reminded on future launches while the device is still
     * connected.  The transient crash counter is cleared since the quarantine record now governs the device.
     * @param tunerId of the quarantined tuner
     * @param crashCount number of crashes that led to the quarantine
     * @param reason human-readable explanation
     */
    public synchronized void markQuarantined(String tunerId, int crashCount, String reason)
    {
        if(tunerId == null)
        {
            return;
        }

        String key = quarantineKey(tunerId);
        mProperties.setProperty(key + ".flag", "true");
        mProperties.setProperty(key + ".id", tunerId);
        mProperties.setProperty(key + ".reason", reason != null ? reason : "");
        mProperties.setProperty(key + ".time", Long.toString(System.currentTimeMillis()));
        mProperties.setProperty(key + ".count", Integer.toString(crashCount));
        mProperties.remove(crashKey(tunerId));
        save();
    }

    /**
     * @return true if the tuner has a standing quarantine record (it was previously auto-disabled for repeated
     * crashes and has not yet been cleared by a successful (re)start).
     */
    public synchronized boolean isQuarantined(String tunerId)
    {
        return tunerId != null && Boolean.parseBoolean(mProperties.getProperty(quarantineKey(tunerId) + ".flag", "false"));
    }

    /**
     * @return the reason a tuner was quarantined, or empty string if none recorded.
     */
    public synchronized String getQuarantineReason(String tunerId)
    {
        return tunerId != null ? mProperties.getProperty(quarantineKey(tunerId) + ".reason", "") : "";
    }

    /**
     * @return epoch milliseconds when the tuner was quarantined, or 0 if not recorded.
     */
    public synchronized long getQuarantineTime(String tunerId)
    {
        try
        {
            return tunerId != null ? Long.parseLong(mProperties.getProperty(quarantineKey(tunerId) + ".time", "0")) : 0;
        }
        catch(NumberFormatException e)
        {
            return 0;
        }
    }

    /**
     * Clears both the quarantine record and the crash history for a tuner - called when the tuner (re)starts
     * successfully, so a re-enabled device gets a clean slate and is no longer reported as quarantined.
     */
    public synchronized void clearQuarantineAndCrashHistory(String tunerId)
    {
        if(tunerId == null)
        {
            return;
        }

        String key = quarantineKey(tunerId);
        boolean changed = mProperties.remove(key + ".flag") != null;
        mProperties.remove(key + ".id");
        mProperties.remove(key + ".reason");
        mProperties.remove(key + ".time");
        mProperties.remove(key + ".count");

        if(mProperties.remove(crashKey(tunerId)) != null)
        {
            changed = true;
        }

        if(changed)
        {
            save();
        }
    }

    /**
     * If the previous run left a pending start marker, the JVM died during that tuner's start(); attribute a crash to
     * it and clear the marker.
     */
    private void resolvePendingCrash()
    {
        String pendingId = mProperties.getProperty(KEY_PENDING_ID);

        if(pendingId != null && !pendingId.isBlank())
        {
            int count = getCrashCount(pendingId) + 1;
            mProperties.setProperty(crashKey(pendingId), Integer.toString(count));
            mProperties.remove(KEY_PENDING_ID);
            mProperties.remove(KEY_PENDING_TIME);
            save();

            mLog.warn("Detected that the previous session crashed/hung while starting tuner [{}] - crash count is now " +
                    "[{}] (quarantine at [{}]). A native libusb fault during device start cannot be recovered in-process; " +
                    "the device will be disabled if it continues to crash startup.", pendingId, count, mThreshold);
        }
    }

    private static String crashKey(String tunerId)
    {
        //Tuner ids can contain spaces/colons; encode to a stable, property-key-safe token.
        return CRASH_PREFIX + Integer.toHexString(tunerId.hashCode());
    }

    private static String quarantineKey(String tunerId)
    {
        return QUARANTINE_PREFIX + Integer.toHexString(tunerId.hashCode());
    }

    private void load()
    {
        if(mFile == null || !Files.exists(mFile))
        {
            return;
        }

        try(InputStream in = Files.newInputStream(mFile))
        {
            mProperties.load(in);
        }
        catch(IOException e)
        {
            mLog.warn("Unable to read tuner fault tracking file [{}] - starting fresh", mFile, e);
        }
    }

    private void save()
    {
        if(mFile == null)
        {
            return;
        }

        try
        {
            if(mFile.getParent() != null)
            {
                Files.createDirectories(mFile.getParent());
            }

            try(OutputStream out = Files.newOutputStream(mFile))
            {
                mProperties.store(out, "sdrtrunk tuner fault tracking - crashes attributed to a tuner during start()");
            }
        }
        catch(IOException e)
        {
            mLog.warn("Unable to persist tuner fault tracking file [{}]", mFile, e);
        }
    }
}
