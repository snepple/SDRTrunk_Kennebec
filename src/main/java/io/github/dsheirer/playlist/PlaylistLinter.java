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
package io.github.dsheirer.playlist;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.id.broadcast.BroadcastChannel;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.health.SystemHealthAlertEvent;
import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.source.config.SourceConfigTunerMultipleFrequency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates the loaded playlist for configuration errors that would otherwise fail silently at
 * runtime: channels referencing alias lists that don't exist, auto-start channels with no
 * frequency, and aliases assigned to streams that are no longer configured (typo'd or deleted
 * stream names mean audio silently never reaches the streaming service).
 *
 * Findings are logged in detail and summarized in a single system health alert.
 */
public class PlaylistLinter
{
    private static final Logger mLog = LoggerFactory.getLogger(PlaylistLinter.class);

    private PlaylistLinter()
    {
    }

    /**
     * Lints the playlist and reports findings via log and a system health alert.
     * @param playlistManager providing channels, aliases and broadcast configurations
     * @return number of findings
     */
    public static int lint(PlaylistManager playlistManager)
    {
        List<String> findings = new ArrayList<>();

        try
        {
            Set<String> aliasListNames = new HashSet<>(playlistManager.getAliasModel().getListNames());
            Set<String> streamNames = new HashSet<>(playlistManager.getBroadcastModel().getBroadcastConfigurationNames());

            for(Channel channel : playlistManager.getChannelModel().getChannels())
            {
                String aliasListName = channel.getAliasListName();

                if(aliasListName != null && !aliasListName.isEmpty() && !aliasListNames.contains(aliasListName))
                {
                    findings.add("Channel '" + channel.getName() + "' references alias list '" + aliasListName +
                        "' which does not exist - talkgroup names, priorities and stream assignments will not apply");
                }

                if(channel.isAutoStart() && !hasFrequency(channel))
                {
                    findings.add("Auto-start channel '" + channel.getName() +
                        "' has no frequency configured and cannot start");
                }
            }

            Set<String> reportedStreams = new HashSet<>();

            for(Alias alias : playlistManager.getAliasModel().getAliases())
            {
                for(BroadcastChannel broadcastChannel : alias.getBroadcastChannels())
                {
                    String streamName = broadcastChannel.getChannelName();

                    if(streamName != null && !streamNames.contains(streamName) && reportedStreams.add(streamName))
                    {
                        findings.add("One or more aliases are assigned to stream '" + streamName +
                            "' which does not exist - audio for those talkgroups will not be streamed");
                    }
                }
            }
        }
        catch(Exception e)
        {
            mLog.error("Error linting playlist", e);
            return 0;
        }

        if(!findings.isEmpty())
        {
            for(String finding : findings)
            {
                mLog.warn("Playlist lint: " + finding);
            }

            MyEventBus.getGlobalEventBus().post(new SystemHealthAlertEvent(
                SystemHealthAlertEvent.AlertType.SYSTEM,
                "Playlist Configuration Issues",
                findings.size() + " playlist configuration issue(s) detected - see application log " +
                    "for details.  First issue: " + findings.get(0)));
        }
        else
        {
            mLog.info("Playlist lint: no configuration issues detected");
        }

        return findings.size();
    }

    private static boolean hasFrequency(Channel channel)
    {
        if(channel.getSourceConfiguration() instanceof SourceConfigTuner tuner)
        {
            return tuner.getFrequency() > 0;
        }

        if(channel.getSourceConfiguration() instanceof SourceConfigTunerMultipleFrequency multiple)
        {
            return multiple.getFrequencies() != null && !multiple.getFrequencies().isEmpty();
        }

        //Other source types (recordings, etc.) are out of scope
        return true;
    }
}
