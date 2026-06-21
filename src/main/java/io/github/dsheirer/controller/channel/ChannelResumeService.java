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

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.util.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Persists the set of currently-processing channels to disk and resumes them after a crash.
 *
 * While the application runs, every channel event triggers a rewrite of a small state file listing
 * the channels that are processing.  On graceful shutdown the file is deleted.  If the file exists
 * at the next startup, the previous run ended unexpectedly, and every channel listed - including
 * manually-started channels that are not flagged auto-start - is restarted, restoring the exact
 * pre-crash monitoring state.
 */
public class ChannelResumeService implements Listener<ChannelEvent>
{
    private static final Logger mLog = LoggerFactory.getLogger(ChannelResumeService.class);
    private static final String STATE_FILE = "running_channels.state";
    private static final String FIELD_SEPARATOR = "\u001F"; //Unit separator - won't occur in names

    //Crash-resume of many channels is CPU-heavy: each channel rebuilds a polyphase channelizer and a
    //decode chain. Starting them all back-to-back at launch pins every core and starves the JavaFX
    //Application thread, so the main window paints but shows "Not Responding" for tens of seconds. We
    //therefore wait for the UI to finish coming up, then start the channels one at a time with a gap
    //between them so the FX thread (and audio) get CPU to stay responsive while monitoring is restored.
    private static final long RESUME_INITIAL_DELAY_MS = 5000;
    private static final long RESUME_STAGGER_MS = 3000;

    private final ChannelProcessingManager mChannelProcessingManager;
    private final ChannelModel mChannelModel;
    private final Path mStateFile;

    public ChannelResumeService(ChannelProcessingManager channelProcessingManager, ChannelModel channelModel,
                                UserPreferences userPreferences)
    {
        mChannelProcessingManager = channelProcessingManager;
        mChannelModel = channelModel;
        mStateFile = userPreferences.getDirectoryPreference().getDirectoryApplicationRoot().resolve(STATE_FILE);
    }

    /**
     * Begins tracking channel processing state.  Call after resume() so the crash-state file isn't
     * overwritten before it is read.
     */
    public void start()
    {
        mChannelProcessingManager.addChannelEventListener(this);
    }

    /**
     * Deletes the state file at graceful shutdown so the next startup does not resume anything.
     */
    public void shutdown()
    {
        try
        {
            Files.deleteIfExists(mStateFile);
        }
        catch(IOException e)
        {
            mLog.warn("Unable to delete channel resume state file", e);
        }
    }

    /**
     * Restarts channels recorded as processing by a previous run that ended unexpectedly.
     *
     * The (small) state file is read synchronously so it reflects the pre-crash state before any new
     * channel events overwrite it, but the channels themselves are started on a background thread:
     * channel startup acquires a tuner source and builds a processing chain, which must not run on the
     * JavaFX Application thread (this method is invoked from the FX thread during application startup)
     * or the UI freezes until every channel has started.
     */
    public void resume()
    {
        if(!Files.exists(mStateFile))
        {
            return;
        }

        final List<String> lines;

        try
        {
            lines = Files.readAllLines(mStateFile, StandardCharsets.UTF_8);
        }
        catch(Exception e)
        {
            mLog.error("Error reading channel resume state from previous session", e);
            return;
        }

        if(lines.isEmpty())
        {
            return;
        }

        mLog.info("Previous run ended unexpectedly - resuming [" + lines.size() + "] channel(s)");

        Thread resumeThread = new Thread(() -> resumeChannels(lines), "sdrtrunk channel resume");
        //Below-normal priority so the channelizer/audio/UI threads win contention while we resume.
        resumeThread.setDaemon(true);
        resumeThread.setPriority(Thread.MIN_PRIORITY);
        resumeThread.start();
    }

    /**
     * Starts each previously-running channel on this background thread, deferring the first start until
     * the UI has had a chance to render and spacing the (CPU-heavy) starts apart so the JavaFX
     * Application thread is not starved into a "Not Responding" state during startup.
     */
    private void resumeChannels(List<String> lines)
    {
        if(!sleep(RESUME_INITIAL_DELAY_MS))
        {
            return;
        }

        boolean started = false;

        for(String line : lines)
        {
            String[] fields = line.split(FIELD_SEPARATOR, -1);

            if(fields.length != 3)
            {
                continue;
            }

            for(Channel channel : mChannelModel.getChannels())
            {
                if(Objects.equals(fields[0], channel.getSystem()) &&
                   Objects.equals(fields[1], channel.getSite()) &&
                   Objects.equals(fields[2], channel.getName()) &&
                   !mChannelProcessingManager.isProcessing(channel))
                {
                    //Space successive channel starts apart so each channelizer rebuild's CPU spike
                    //settles and the UI can repaint before the next one begins.
                    if(started && !sleep(RESUME_STAGGER_MS))
                    {
                        return;
                    }

                    mLog.info("Resuming channel after unexpected shutdown: " + channel.getName());

                    try
                    {
                        mChannelProcessingManager.receive(ChannelEvent.requestEnable(channel));
                        started = true;
                    }
                    catch(Throwable t)
                    {
                        mLog.error("Error resuming channel [" + channel.getName() + "]", t);
                    }

                    break;
                }
            }
        }
    }

    /**
     * Sleeps the resume thread, returning false if it was interrupted so the caller can stop cleanly.
     */
    private static boolean sleep(long millis)
    {
        try
        {
            Thread.sleep(millis);
            return true;
        }
        catch(InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void receive(ChannelEvent channelEvent)
    {
        //Rewrite the state file on every channel event - events are infrequent and the file is tiny.
        //Snapshot the processing channels on the calling thread (cheap list iteration), but perform the
        //file write on a background thread so disk I/O never runs on the JavaFX Application thread when a
        //channel event originates from the GUI.  Writes are serialized so the file is never corrupted.
        final List<String> lines = new ArrayList<>();

        for(Channel channel : mChannelProcessingManager.getProcessingChannels())
        {
            lines.add(emptyIfNull(channel.getSystem()) + FIELD_SEPARATOR +
                      emptyIfNull(channel.getSite()) + FIELD_SEPARATOR +
                      emptyIfNull(channel.getName()));
        }

        ThreadPool.CACHED.submit(() -> writeState(lines));
    }

    /**
     * Serializes the snapshot to disk via an atomic temp-file replace.  Synchronized so concurrent
     * channel events can't interleave writes and corrupt the state file.
     */
    private synchronized void writeState(List<String> lines)
    {
        try
        {
            Path temp = mStateFile.resolveSibling(STATE_FILE + ".tmp");
            Files.write(temp, lines, StandardCharsets.UTF_8);
            Files.move(temp, mStateFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
        catch(Exception e)
        {
            mLog.warn("Unable to update channel resume state file", e);
        }
    }

    private static String emptyIfNull(String value)
    {
        return value == null ? "" : value;
    }
}
