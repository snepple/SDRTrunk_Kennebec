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

package io.github.dsheirer.preference.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.dsheirer.dsp.tone.ToneDiscoveredEvent;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.playlist.TwoToneConfiguration;
import io.github.dsheirer.preference.UserPreferences;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests the graceful-degradation placeholder naming used when no confident unit name can be extracted.
 */
class ToneDiscoveryManagerTest
{
    @Test
    void placeholderPreservesTonesAndChannel()
    {
        //154145000 Hz -> 154.1450 MHz in standard channel notation.
        assertEquals("Unknown Unit (Tones: 1006.9/832.5 on 154.1450)",
            ToneDiscoveryManager.buildUnknownPlaceholder(1006.9, 832.5, 154_145_000.0));
    }

    @Test
    void placeholderOmitsChannelWhenUnknown()
    {
        assertEquals("Unknown Unit (Tones: 600.9/707.3)",
            ToneDiscoveryManager.buildUnknownPlaceholder(600.9, 707.3, 0.0));
    }

    @Test
    void placeholderHandlesSingleToneLongAPage()
    {
        String placeholder = ToneDiscoveryManager.buildUnknownPlaceholder(1153.0, 0.0, 154_145_000.0);
        assertTrue(placeholder.startsWith("Unknown Unit (Tones: 1153.0 on 154.1450)"), placeholder);
    }

    @Test
    void repeatedUnknownToneWithoutTranscriptCreatesReviewPendingPlaceholder()
    {
        UserPreferences userPreferences = mock(UserPreferences.class);
        AIPreference aiPreference = mock(AIPreference.class);
        PlaylistManager playlistManager = mock(PlaylistManager.class);
        List<TwoToneConfiguration> configurations = new ArrayList<>();

        when(userPreferences.getAIPreference()).thenReturn(aiPreference);
        when(aiPreference.isAIEnabled()).thenReturn(true);
        when(aiPreference.isAIToneDiscoveryEnabled()).thenReturn(true);
        when(playlistManager.getTwoToneConfigurations()).thenReturn(configurations);

        String originalHome = System.getProperty("user.home");

        try
        {
            java.nio.file.Path tempHome = java.nio.file.Files.createTempDirectory("tone-discovery-test");
            System.setProperty("user.home", tempHome.toString());

            ToneDiscoveryManager manager = new ToneDiscoveryManager(userPreferences, playlistManager);
            for(int x = 0; x < 4; x++)
            {
                manager.onToneDiscovered(new ToneDiscoveredEvent(1006.9, 832.5, null, 154_145_000.0));
            }

            assertEquals(1, configurations.size());
            TwoToneConfiguration config = configurations.get(0);
            assertTrue(config.isAutoDiscovered());
            assertFalse(config.isEnabled());
            assertTrue(config.getAlias().startsWith("[AI] Unknown Unit (Tones: 1006.9/832.5 on 154.1450)"));
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            if(originalHome != null)
            {
                System.setProperty("user.home", originalHome);
            }
        }
    }
}
