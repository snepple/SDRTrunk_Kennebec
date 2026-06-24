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

import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.module.decode.event.IDecodeEvent;

import java.util.Collection;

/**
 * Correlates a completed audio transcription to the decode event (call) it belongs to.
 *
 * The previous approach matched only on proximity to {@link IDecodeEvent#getTimeStart()} within 30 seconds.
 * That fails for calls longer than 30 seconds, for audio segments that arrive after the 60-second segment
 * rollover, and for decode events whose grant timestamp precedes voice audio by more than a few seconds.
 * This correlator instead treats the audio segment start as falling within the call's time span and prefers
 * voice-call events with matching talkgroup (and optionally FROM radio) identifiers.
 */
public final class TranscriptionCorrelator
{
    /** Decode grant can precede the first voice buffer by a few seconds. */
    private static final long PRE_AUDIO_SLACK_MS = 15_000;

    /** Ongoing calls without an end time can span multiple 60-second audio segments. */
    private static final long ONGOING_CALL_WINDOW_MS = 180_000;

    /** STT can finish after the call ends; allow late-arriving transcripts. */
    private static final long POST_EVENT_SLACK_MS = 90_000;

    /** Last-resort proximity match when no event contains the segment timestamp. */
    private static final long FALLBACK_MAX_DELTA_MS = 120_000;

    private TranscriptionCorrelator()
    {
    }

    /**
     * Stable key for pairing tone-discovery events with transcription events for the same audio segment.
     */
    public static long segmentCorrelationKey(long startTimestamp, Integer toId)
    {
        if(startTimestamp <= 0)
        {
            return 0L;
        }

        long key = startTimestamp;

        if(toId != null)
        {
            key ^= ((long)toId) << 32;
        }

        return key;
    }

    /**
     * Finds the best decode event for a transcription among the supplied history.
     *
     * @param events decode events to search (typically the global event history)
     * @param segmentStartTimestamp audio segment start (epoch millis)
     * @param toId TO talkgroup id captured at transcription time, or null
     * @param fromRadioId FROM radio id captured at transcription time, or null
     * @return the best matching event, or null if none found
     */
    public static IDecodeEvent findBestMatch(Collection<? extends IDecodeEvent> events, long segmentStartTimestamp,
                                             Integer toId, Integer fromRadioId)
    {
        if(events == null || events.isEmpty())
        {
            return null;
        }

        IDecodeEvent bestInWindow = null;
        int bestInWindowScore = Integer.MIN_VALUE;

        IDecodeEvent bestFallback = null;
        long bestFallbackDelta = Long.MAX_VALUE;

        for(IDecodeEvent event : events)
        {
            if(event == null)
            {
                continue;
            }

            if(toId != null && !talkgroupMatches(event, toId))
            {
                continue;
            }

            if(segmentStartTimestamp > 0)
            {
                long windowStart = event.getTimeStart() - PRE_AUDIO_SLACK_MS;
                long effectiveEnd = event.getTimeEnd() > event.getTimeStart()
                    ? event.getTimeEnd()
                    : event.getTimeStart() + ONGOING_CALL_WINDOW_MS;
                effectiveEnd += POST_EVENT_SLACK_MS;

                if(segmentStartTimestamp >= windowStart && segmentStartTimestamp <= effectiveEnd)
                {
                    int score = scoreEvent(event, segmentStartTimestamp, fromRadioId);

                    if(score > bestInWindowScore)
                    {
                        bestInWindowScore = score;
                        bestInWindow = event;
                    }

                    continue;
                }

                long delta = Math.abs(event.getTimeStart() - segmentStartTimestamp);

                if(delta <= FALLBACK_MAX_DELTA_MS)
                {
                    long adjustedDelta = delta;

                    if(!DecodeEventType.VOICE_CALLS.contains(event.getEventType()))
                    {
                        adjustedDelta += 30_000;
                    }

                    if(adjustedDelta < bestFallbackDelta)
                    {
                        bestFallbackDelta = adjustedDelta;
                        bestFallback = event;
                    }
                }
            }
            else if(bestInWindow == null)
            {
                //No segment timestamp - fall back to the most recent voice call on the talkgroup.
                int score = scoreEvent(event, 0, fromRadioId);

                if(score > bestInWindowScore)
                {
                    bestInWindowScore = score;
                    bestInWindow = event;
                }
            }
        }

        return bestInWindow != null ? bestInWindow : bestFallback;
    }

    private static int scoreEvent(IDecodeEvent event, long segmentStartTimestamp, Integer fromRadioId)
    {
        int score = 0;

        if(DecodeEventType.VOICE_CALL_EVENTS.contains(event.getEventType()))
        {
            score += 100;
        }
        else if(DecodeEventType.VOICE_CALLS.contains(event.getEventType()))
        {
            score += 50;
        }
        else if(event.getEventType() == DecodeEventType.PAGE)
        {
            score += 40;
        }

        if(fromRadioId != null && fromRadioMatches(event, fromRadioId))
        {
            score += 40;
        }

        if(segmentStartTimestamp > 0)
        {
            long effectiveEnd = event.getTimeEnd() > event.getTimeStart()
                ? event.getTimeEnd()
                : event.getTimeStart() + ONGOING_CALL_WINDOW_MS;
            long midpoint = (event.getTimeStart() + effectiveEnd) / 2;
            score -= (int)(Math.abs(segmentStartTimestamp - midpoint) / 1000);
        }

        return score;
    }

    private static boolean talkgroupMatches(IDecodeEvent event, int toId)
    {
        if(event.getIdentifierCollection() == null)
        {
            return false;
        }

        for(Identifier id : event.getIdentifierCollection().getIdentifiers(Role.TO))
        {
            if(id.getValue() instanceof Number && ((Number)id.getValue()).intValue() == toId)
            {
                return true;
            }
        }

        return false;
    }

    private static boolean fromRadioMatches(IDecodeEvent event, int fromRadioId)
    {
        if(event.getIdentifierCollection() == null)
        {
            return false;
        }

        for(Identifier id : event.getIdentifierCollection().getIdentifiers(Role.FROM))
        {
            if(id.getForm() == Form.RADIO && id.getValue() instanceof Number &&
                ((Number)id.getValue()).intValue() == fromRadioId)
            {
                return true;
            }
        }

        return false;
    }
}
