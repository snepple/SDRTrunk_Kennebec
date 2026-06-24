package io.github.dsheirer.dsp.tone;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.playlist.PlaylistV2;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.ai.AIPreference;
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
}
