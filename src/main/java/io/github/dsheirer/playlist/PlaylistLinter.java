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
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.config.ChannelToneFilter;
import io.github.dsheirer.module.decode.nbfm.DecodeConfigNBFM;
import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.source.config.SourceConfigTunerMultipleFrequency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

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

            //Maps a single-frequency channel's effective RF use to the name of the first channel using it, so
            //subsequent channels on the same frequency/decoder/tone signature can be reported as potential duplicates.
            Map<FrequencyUse,String> frequencyUseToChannelName = new HashMap<>();

            for(Channel channel : playlistManager.getChannelModel().getChannels())
            {
                String aliasListName = channel.getAliasListName();

                if(aliasListName != null && !aliasListName.isEmpty() && !aliasListNames.contains(aliasListName))
                {
                    findings.add("Channel '" + channel.getName() + "' references alias list '" + aliasListName +
                        "' which does not exist - talkgroup names, priorities and stream assignments will not apply");
                }

                //Flag channels with no alias list assigned.  Without an alias list the channel cannot resolve
                //talkgroup names, priorities, or stream assignments, so audio will never be routed to Zello
                //or any other configured stream.
                if((aliasListName == null || aliasListName.isEmpty()) && hasFrequency(channel))
                {
                    if(channel.isAutoStart())
                    {
                        findings.add("Auto-start channel '" + channel.getName() + "' has no alias list assigned - " +
                            "talkgroup names will not appear and audio will not be routed to any stream. " +
                            "Assign an alias list in the channel editor.");
                    }
                    else
                    {
                        findings.add("Channel '" + channel.getName() + "' has no alias list assigned - decoded " +
                            "activity will show raw numeric identifiers with no names or stream routing");
                    }
                }

                if(channel.isAutoStart() && !hasFrequency(channel))
                {
                    findings.add("Auto-start channel '" + channel.getName() +
                        "' has no frequency configured and cannot start");
                }

                //Flag duplicate tuned frequencies across single-frequency channels, but do not warn for normal shared
                //frequency reuse where channels are separated by decoder type or PL/CTCSS/DCS/NAC tone squelch.
                FrequencyUse frequencyUse = getFrequencyUse(channel);
                if(frequencyUse != null)
                {
                    String existing = frequencyUseToChannelName.putIfAbsent(frequencyUse, channel.getName());

                    if(existing != null)
                    {
                        findings.add("Channel '" + channel.getName() + "' uses the same frequency (" +
                            String.format("%.5f MHz", frequencyUse.frequency() / 1.0E6) + ") and decoder/tone " +
                            "settings as channel '" + existing + "' - this is often a duplicate configuration");
                    }
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

            //Surface the actual findings (not just the first) so the user can see and act on each issue directly from
            //the alert instead of having to dig through the application log. Cap the list so the alert stays readable.
            final int maxToShow = 10;
            StringBuilder detail = new StringBuilder(findings.size() + " playlist configuration issue(s) detected:");

            for(int i = 0; i < findings.size() && i < maxToShow; i++)
            {
                detail.append("\n\u2022 ").append(findings.get(i));
            }

            if(findings.size() > maxToShow)
            {
                detail.append("\n\u2022 ...and ").append(findings.size() - maxToShow)
                    .append(" more (see application log for the full list)");
            }

            MyEventBus.getGlobalEventBus().post(new SystemHealthAlertEvent(
                SystemHealthAlertEvent.AlertType.SYSTEM,
                "Playlist Configuration Issues",
                detail.toString()));
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

    /**
     * Creates the effective frequency-use key for duplicate-channel linting.  Same RF frequency with different decoder
     * types is valid, and NBFM channels with different enabled tone filters are valid.
     */
    static FrequencyUse getFrequencyUse(Channel channel)
    {
        if(channel != null && channel.getSourceConfiguration() instanceof SourceConfigTuner tuner &&
            tuner.getFrequency() > 0 && channel.getDecodeConfiguration() != null)
        {
            DecoderType decoderType = channel.getDecodeConfiguration().getDecoderType();
            return new FrequencyUse(tuner.getFrequency(), decoderType, getToneSignature(channel));
        }

        return null;
    }

    private static String getToneSignature(Channel channel)
    {
        if(channel.getDecodeConfiguration() instanceof DecodeConfigNBFM nbfm && nbfm.hasToneFiltering())
        {
            TreeSet<String> tones = new TreeSet<>();

            for(ChannelToneFilter filter: nbfm.getToneFilters())
            {
                if(filter != null && filter.isValid())
                {
                    tones.add(filter.getToneType() + ":" + filter.getValue());
                }
            }

            if(!tones.isEmpty())
            {
                return String.join(",", tones);
            }
        }

        return "";
    }

    record FrequencyUse(long frequency, DecoderType decoderType, String toneSignature)
    {
        FrequencyUse
        {
            toneSignature = Objects.requireNonNullElse(toneSignature, "");
        }
    }
}
