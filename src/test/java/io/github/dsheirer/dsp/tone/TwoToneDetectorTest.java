package io.github.dsheirer.dsp.tone;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.alias.id.twotone.TwoToneDetectorID;
import io.github.dsheirer.audio.AudioSegment;
import io.github.dsheirer.identifier.configuration.AliasListConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.ChannelNameConfigurationIdentifier;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.playlist.PlaylistV2;
import io.github.dsheirer.playlist.TwoToneConfiguration;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.ai.AIPreference;
import java.util.List;
import org.junit.jupiter.api.Test;

class TwoToneDetectorTest
{
    @Test
    void aiToneDiscoveryPreferenceEnablesDetectorDiscovery()
    {
        PlaylistV2 playlist = new PlaylistV2();
        UserPreferences userPreferences = mock(UserPreferences.class);
        AIPreference aiPreference = mock(AIPreference.class);
        PlaylistManager playlistManager = mock(PlaylistManager.class);

        when(playlistManager.getUserPreferences()).thenReturn(userPreferences);
        when(userPreferences.getAIPreference()).thenReturn(aiPreference);
        when(aiPreference.isAIEnabled()).thenReturn(true);
        when(aiPreference.isAIToneDiscoveryEnabled()).thenReturn(true);

        assertTrue(TwoToneDetector.isDiscoveryEnabled(playlist, playlistManager));
    }

    @Test
    void discoveryDisabledWhenNeitherPlaylistNorAiPreferenceEnableIt()
    {
        PlaylistV2 playlist = new PlaylistV2();
        PlaylistManager playlistManager = mock(PlaylistManager.class);

        assertFalse(TwoToneDetector.isDiscoveryEnabled(playlist, playlistManager));
    }

    @Test
    void aliasScopedDetectorRunsWhenConventionalChannelHasNoTalkgroup()
    {
        AliasModel aliasModel = new AliasModel();
        Alias alias = new Alias("Oakland FD");
        alias.setAliasListName("Kennebec");
        alias.addAliasID(new TwoToneDetectorID("Oakland Fire"));
        aliasModel.addAlias(alias);

        PlaylistManager playlistManager = mock(PlaylistManager.class);
        when(playlistManager.getAliasModel()).thenReturn(aliasModel);

        TwoToneDetector detector = new TwoToneDetector(playlistManager);

        try
        {
            AliasList aliasList = aliasModel.getAliasList("Kennebec");
            AudioSegment segment = new AudioSegment(aliasList, 0);
            segment.addIdentifier(AliasListConfigurationIdentifier.create("Kennebec"));
            segment.addIdentifier(ChannelNameConfigurationIdentifier.create("Oakland FD"));

            TwoToneConfiguration config = new TwoToneConfiguration();
            config.setAlias("Oakland Fire");

            assertTrue(detector.getApplicableDetectorNames(segment, List.of(config)).contains("Oakland Fire"));
        }
        finally
        {
            detector.dispose();
        }
    }
}
