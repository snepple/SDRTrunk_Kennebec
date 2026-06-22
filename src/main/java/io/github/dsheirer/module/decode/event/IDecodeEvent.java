/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.protocol.Protocol;

/**
 * Decode event interface
 */
public interface IDecodeEvent
{
    /**
     * Event start time
     *
     * @return timestamp in milliseconds
     */
    long getTimeStart();

    /**
     * Event end time
     * @return timestamp in milliseconds
     */
    long getTimeEnd();

    /**
     * Event duration.
     *
     * @return event duration in milliseconds or 0 if there is no duration
     */
    long getDuration();

    /**
     * Collection of identifiers associated with the event.  This collection should contain a
     * Role.FROM and a Role.TO identifier, a Decoder Type identifier, and (optionally) an Alias List
     * Configuration identifier.
     */
    IdentifierCollection getIdentifierCollection();

    /**
     * Channel descriptor for the channel where the event occurred
     *
     * @return descriptor or null
     */
    IChannelDescriptor getChannelDescriptor();

    /**
     * Optional Details about the event
     */
    String getDetails();

    /**
     * Optional AI transcription of the event's audio, or null if none.  Default no-op for event types that
     * don't carry a transcription.
     */
    default String getTranscription()
    {
        return null;
    }

    /**
     * Sets the AI transcription of the event's audio.  Default no-op.
     */
    default void setTranscription(String transcription)
    {
    }

    /**
     * Adds a transcribed audio segment to this event, keyed by the segment's audio start timestamp.  A single
     * call can be split into several audio segments (e.g. transmissions longer than 60 seconds), each
     * transcribed separately and arriving out of order.  Implementations should join the fragments in
     * chronological order so the displayed transcription reads from the beginning of the call - rather than
     * overwriting earlier text with whichever fragment arrives last.  Keying by start timestamp also makes
     * re-delivery of the same segment idempotent.  Default falls back to {@link #setTranscription(String)}.
     *
     * @param startTimestamp audio segment start time (epoch millis) used for ordering/de-duplication
     * @param transcription transcribed text for the segment
     */
    default void addTranscriptionSegment(long startTimestamp, String transcription)
    {
        setTranscription(transcription);
    }

    /**
     * Protocol for the decoder that produced the event
     */
    Protocol getProtocol();

    /**
     * {@link DecodeEventType} for the event produced
     */
    DecodeEventType getEventType();

    /**
     * Timeslot for the event.
     * @return timeslot or default of 0
     */
    int getTimeslot();

    /**
     * Indicates if the event has a timeslot specified
     */
    boolean hasTimeslot();
}
