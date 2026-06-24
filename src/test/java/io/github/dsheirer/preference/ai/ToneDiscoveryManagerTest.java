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
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertEquals("Pending User Review (Tones: 1006.9/832.5 on 154.1450)",
            ToneDiscoveryManager.buildUnknownPlaceholder(1006.9, 832.5, 154_145_000.0));
    }

    @Test
    void placeholderOmitsChannelWhenUnknown()
    {
        assertEquals("Pending User Review (Tones: 600.9/707.3)",
            ToneDiscoveryManager.buildUnknownPlaceholder(600.9, 707.3, 0.0));
    }

    @Test
    void placeholderHandlesSingleToneLongAPage()
    {
        String placeholder = ToneDiscoveryManager.buildUnknownPlaceholder(1153.0, 0.0, 154_145_000.0);
        assertTrue(placeholder.startsWith("Pending User Review (Tones: 1153.0 on 154.1450)"), placeholder);
    }
}
