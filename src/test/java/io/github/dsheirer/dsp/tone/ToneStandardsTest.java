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

package io.github.dsheirer.dsp.tone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests {@link ToneStandards} - analog tolerance matching and standardized-frequency snapping.
 */
class ToneStandardsTest
{
    @Test
    void withinToleranceAcceptsSmallDeviation()
    {
        //600.9 Hz +/-1.5% allows ~9 Hz of swing; a 0.9 Hz deviation must match.
        assertTrue(ToneStandards.withinTolerance(600.9, 601.8));
        assertTrue(ToneStandards.withinTolerance(600.9, 592.0));
    }

    @Test
    void withinToleranceRejectsLargeDeviationAndNonPositiveReference()
    {
        assertFalse(ToneStandards.withinTolerance(600.9, 650.0));
        assertFalse(ToneStandards.withinTolerance(0.0, 0.0));
        assertFalse(ToneStandards.withinTolerance(-1.0, -1.0));
    }

    @Test
    void toneSequencesMatchRequiresBothTones()
    {
        assertTrue(ToneStandards.toneSequencesMatch(349.0, 600.9, 350.0, 605.0));
        assertFalse(ToneStandards.toneSequencesMatch(349.0, 600.9, 349.0, 700.0));
        assertFalse(ToneStandards.toneSequencesMatch(349.0, 600.9, 400.0, 600.9));
    }

    @Test
    void toneSequencesMatchHandlesSingleToneLongAPages()
    {
        //Long A / All-Call pages have no B tone (<=0): only the A tones are compared.
        assertTrue(ToneStandards.toneSequencesMatch(1153.0, 0, 1151.4, 0));
        assertFalse(ToneStandards.toneSequencesMatch(1153.0, 0, 1300.0, 0));
    }

    @Test
    void snapMovesNearStandardToneOntoMatrixValue()
    {
        //601.8 Hz is within tolerance of the QCII 600.9 Hz tone (nearer than GE 607.5) -> snaps to 600.9.
        assertEquals(600.9, ToneStandards.snap(601.8), 0.0001);
        assertEquals(600.9, ToneStandards.snap(600.9), 0.0001);
    }

    @Test
    void snapLeavesNonStandardToneRoundedToOneDecimal()
    {
        //1234.5 Hz is not within tolerance of any standard tone, so it is returned rounded to one decimal.
        assertEquals(1234.5, ToneStandards.snap(1234.47), 0.0001);
        assertEquals(0.0, ToneStandards.snap(0.0), 0.0001);
    }
}
