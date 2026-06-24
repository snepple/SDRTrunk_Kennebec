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

package io.github.dsheirer.transcription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
import io.github.dsheirer.module.decode.event.DecodeEvent;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.module.decode.event.IDecodeEvent;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.protocol.Protocol;
import java.util.List;
import org.junit.jupiter.api.Test;

class TranscriptionCorrelatorTest
{
    private static DecodeEvent call(long start, long end, int talkgroup)
    {
        DecodeEvent event = new DecodeEvent(DecodeEventType.CALL, start);
        event.end(end);
        MutableIdentifierCollection collection = new MutableIdentifierCollection();
        collection.update(APCO25Talkgroup.create(talkgroup));
        event.setIdentifierCollection(collection);
        return event;
    }

    @Test
    void matchesSegmentInsideCallSpanEvenWhenFarFromGrantTime()
    {
        DecodeEvent event = call(1_000L, 125_000L, 101);
        long segmentAtMinuteTwo = 1_000L + 65_000L;

        IDecodeEvent match = TranscriptionCorrelator.findBestMatch(List.of(event), segmentAtMinuteTwo, 101, null);

        assertSame(event, match);
    }

    @Test
    void rejectsTalkgroupMismatch()
    {
        DecodeEvent event = call(1_000L, 30_000L, 101);

        IDecodeEvent match = TranscriptionCorrelator.findBestMatch(List.of(event), 5_000L, 202, null);

        assertNull(match);
    }

    @Test
    void prefersVoiceCallOverRegistration()
    {
        DecodeEvent registration = new DecodeEvent(DecodeEventType.REGISTER, 1_000L);
        MutableIdentifierCollection regIds = new MutableIdentifierCollection();
        regIds.update(APCO25Talkgroup.create(101));
        registration.setIdentifierCollection(regIds);

        DecodeEvent call = call(1_000L, 30_000L, 101);

        IDecodeEvent match = TranscriptionCorrelator.findBestMatch(List.of(registration, call), 5_000L, 101, null);

        assertSame(call, match);
    }

    private static DecodeEvent call(long start, long end, int talkgroup, Integer radioId)
    {
        DecodeEvent event = call(start, end, talkgroup);

        if(radioId != null)
        {
            MutableIdentifierCollection ids = new MutableIdentifierCollection(event.getIdentifierCollection().getIdentifiers());
            ids.update(APCO25RadioIdentifier.createFrom(radioId));
            event.setIdentifierCollection(ids);
        }

        return event;
    }

    @Test
    void prefersFromRadioMatchWhenPresent()
    {
        DecodeEvent withoutRadio = call(1_000L, 30_000L, 101);
        DecodeEvent withRadio = call(1_000L, 30_000L, 101, 42);

        IDecodeEvent match = TranscriptionCorrelator.findBestMatch(List.of(withoutRadio, withRadio), 5_000L, 101, 42);

        assertSame(withRadio, match);
    }

    @Test
    void segmentCorrelationKeyCombinesTimestampAndTalkgroup()
    {
        long keyA = TranscriptionCorrelator.segmentCorrelationKey(5_000L, 101);
        long keyB = TranscriptionCorrelator.segmentCorrelationKey(5_000L, 202);
        long keyC = TranscriptionCorrelator.segmentCorrelationKey(6_000L, 101);

        assertEquals(keyA, TranscriptionCorrelator.segmentCorrelationKey(5_000L, 101));
        org.junit.jupiter.api.Assertions.assertNotEquals(keyA, keyB);
        org.junit.jupiter.api.Assertions.assertNotEquals(keyA, keyC);
    }
}
