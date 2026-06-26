package io.github.dsheirer.playlist;

import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.broadcast.BroadcastModel;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelModel;
import io.github.dsheirer.module.decode.config.ChannelToneFilter;
import io.github.dsheirer.module.decode.nbfm.DecodeConfigNBFM;
import io.github.dsheirer.module.decode.p25.phase1.DecodeConfigP25Phase1;
import io.github.dsheirer.source.config.SourceConfigTuner;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlaylistLinterTest
{
    private static final long FREQUENCY = 155_250_000L;

    @Test
    void sameFrequencyDifferentDecoderIsNotDuplicate()
    {
        assertNotEquals(PlaylistLinter.getFrequencyUse(nbfmChannel("NBFM", FREQUENCY, "TONE_1B")),
            PlaylistLinter.getFrequencyUse(p25Channel("P25", FREQUENCY)));
    }

    @Test
    void sameFrequencyDifferentToneFiltersAreNotDuplicate()
    {
        assertNotEquals(PlaylistLinter.getFrequencyUse(nbfmChannel("Fire", FREQUENCY, "TONE_1B")),
            PlaylistLinter.getFrequencyUse(nbfmChannel("Public Works", FREQUENCY, "TONE_2Z")));
    }

    @Test
    void sameFrequencySameDecoderAndToneFilterAreDuplicate()
    {
        assertEquals(PlaylistLinter.getFrequencyUse(nbfmChannel("Fire 1", FREQUENCY, "TONE_1B")),
            PlaylistLinter.getFrequencyUse(nbfmChannel("Fire 2", FREQUENCY, "TONE_1B")));
    }

    @Test
    void linterDoesNotAlertForSameFrequencyWithDifferentDecoder()
    {
        assertEquals(0, lint(List.of(nbfmChannel("NBFM", FREQUENCY, "TONE_1B"), p25Channel("P25", FREQUENCY))));
    }

    @Test
    void linterAlertsForSameEffectiveFrequencyUse()
    {
        assertEquals(1, lint(List.of(nbfmChannel("Fire 1", FREQUENCY, "TONE_1B"),
            nbfmChannel("Fire 2", FREQUENCY, "TONE_1B"))));
    }

    private int lint(List<Channel> channels)
    {
        PlaylistManager playlistManager = mock(PlaylistManager.class);
        AliasModel aliasModel = mock(AliasModel.class);
        BroadcastModel broadcastModel = mock(BroadcastModel.class);
        ChannelModel channelModel = mock(ChannelModel.class);

        when(aliasModel.getListNames()).thenReturn(List.of("Default"));
        when(aliasModel.getAliases()).thenReturn(Collections.emptyList());
        when(broadcastModel.getBroadcastConfigurationNames()).thenReturn(Collections.emptyList());
        when(channelModel.getChannels()).thenReturn(channels);
        when(playlistManager.getAliasModel()).thenReturn(aliasModel);
        when(playlistManager.getBroadcastModel()).thenReturn(broadcastModel);
        when(playlistManager.getChannelModel()).thenReturn(channelModel);

        return PlaylistLinter.lint(playlistManager);
    }

    private static Channel nbfmChannel(String name, long frequency, String toneValue)
    {
        Channel channel = new Channel(name);
        channel.setAliasListName("Default");
        channel.setSourceConfiguration(source(frequency));

        DecodeConfigNBFM decodeConfig = new DecodeConfigNBFM();

        if(toneValue != null)
        {
            decodeConfig.setToneFilterEnabled(true);
            decodeConfig.addToneFilter(new ChannelToneFilter(ChannelToneFilter.ToneType.CTCSS, toneValue, toneValue));
        }

        channel.setDecodeConfiguration(decodeConfig);
        return channel;
    }

    private static Channel p25Channel(String name, long frequency)
    {
        Channel channel = new Channel(name);
        channel.setAliasListName("Default");
        channel.setSourceConfiguration(source(frequency));
        channel.setDecodeConfiguration(new DecodeConfigP25Phase1());
        return channel;
    }

    private static SourceConfigTuner source(long frequency)
    {
        SourceConfigTuner source = new SourceConfigTuner();
        source.setFrequency(frequency);
        return source;
    }
}
