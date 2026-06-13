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
     */
    public void resume()
    {
        if(!Files.exists(mStateFile))
        {
            return;
        }

        try
        {
            List<String> lines = Files.readAllLines(mStateFile, StandardCharsets.UTF_8);

            if(!lines.isEmpty())
            {
                mLog.info("Previous run ended unexpectedly - resuming [" + lines.size() + "] channel(s)");
            }

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
                        mLog.info("Resuming channel after unexpected shutdown: " + channel.getName());
                        mChannelProcessingManager.receive(ChannelEvent.requestEnable(channel));
                        break;
                    }
                }
            }
        }
        catch(Exception e)
        {
            mLog.error("Error resuming channels from previous session", e);
        }
    }

    @Override
    public void receive(ChannelEvent channelEvent)
    {
        //Rewrite the state file on every channel event - events are infrequent and the file is tiny
        try
        {
            List<String> lines = new ArrayList<>();

            for(Channel channel : mChannelProcessingManager.getProcessingChannels())
            {
                lines.add(emptyIfNull(channel.getSystem()) + FIELD_SEPARATOR +
                          emptyIfNull(channel.getSite()) + FIELD_SEPARATOR +
                          emptyIfNull(channel.getName()));
            }

            Path temp = mStateFile.resolveSibling(STATE_FILE + ".tmp");
            Files.write(temp, lines, StandardCharsets.UTF_8);
            Files.move(temp, mStateFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
        catch(Exception e)
        {
            mLog.warn("Unable to update channel resume state file - " + e.getMessage());
        }
    }

    private static String emptyIfNull(String value)
    {
        return value == null ? "" : value;
    }
}
