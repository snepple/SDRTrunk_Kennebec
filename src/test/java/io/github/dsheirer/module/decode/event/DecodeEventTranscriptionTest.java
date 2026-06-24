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

package io.github.dsheirer.module.decode.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link DecodeEvent#addTranscriptionSegment(long, String)} - the ordered, append-not-overwrite
 * behavior that keeps the beginning of a multi-segment call's transcription.
 */
class DecodeEventTranscriptionTest
{
    private static DecodeEvent event()
    {
        return new DecodeEvent(DecodeEventType.CALL, 1000L);
    }

    @Test
    void singleSegmentIsReturnedVerbatim()
    {
        DecodeEvent e = event();
        e.addTranscriptionSegment(1000L, "Engine 4 respond");
        assertEquals("Engine 4 respond", e.getTranscription());
    }

    @Test
    void multipleSegmentsJoinInChronologicalOrderRegardlessOfArrival()
    {
        DecodeEvent e = event();
        //Later segment arrives first, then the earlier one - the displayed text must still read in order.
        e.addTranscriptionSegment(60_000L, "to the hospital 22.8");
        e.addTranscriptionSegment(1_000L, "Engine 4 transporting one patient");
        assertEquals("Engine 4 transporting one patient to the hospital 22.8", e.getTranscription());
    }

    @Test
    void resendingSameSegmentDoesNotDuplicate()
    {
        DecodeEvent e = event();
        e.addTranscriptionSegment(1_000L, "Engine 4 respond");
        e.addTranscriptionSegment(1_000L, "Engine 4 respond");
        assertEquals("Engine 4 respond", e.getTranscription());
    }

    @Test
    void blankAndNullSegmentsAreIgnored()
    {
        DecodeEvent e = event();
        e.addTranscriptionSegment(1_000L, null);
        e.addTranscriptionSegment(2_000L, "   ");
        assertNull(e.getTranscription());

        e.addTranscriptionSegment(3_000L, "  Rescue 1 on scene  ");
        assertEquals("Rescue 1 on scene", e.getTranscription());
    }

    @Test
    void transcriptSegmentInsideLongCallMatchesEventInterval()
    {
        IDecodeEvent event = mock(IDecodeEvent.class);
        when(event.getTimeStart()).thenReturn(1_000L);
        when(event.getTimeEnd()).thenReturn(121_000L);

        IDecodeEvent match = DecodeEventPanel.findBestTranscriptionMatch(List.of(event), 70_000L, null);

        assertSame(event, match);
    }
}
