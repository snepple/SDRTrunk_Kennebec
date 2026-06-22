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

/**
 * A "tombstone" soft-deletion marker for an AI-discovered two-tone detector that a user rejected or deleted.
 *
 * <p>Rather than purging a rejected A/B tone pair entirely, the AI tone-discovery feature records a tombstone so the
 * tone combination is never regenerated.  This append-only exclusion list lets the reconciliation workflow check both
 * the active configuration <i>and</i> the set of past rejections (within analog tolerance) before spending CPU on the
 * transcription/NLP pipeline.</p>
 *
 * <p>The primary key is the tone pair plus the originating RF channel frequency.  A {@code channelFrequency} of 0
 * means "unknown / any channel", which matches regardless of the detected channel so a rejection still blocks
 * regeneration even when the discovery channel could not be determined.</p>
 */
public class ToneTombstone
{
    private double toneA;
    private double toneB;
    private double channelFrequency;
    private long timestamp;

    /**
     * Default constructor for JSON (Jackson) deserialization.
     */
    public ToneTombstone()
    {
    }

    public ToneTombstone(double toneA, double toneB, double channelFrequency, long timestamp)
    {
        this.toneA = toneA;
        this.toneB = toneB;
        this.channelFrequency = channelFrequency;
        this.timestamp = timestamp;
    }

    public double getToneA()
    {
        return toneA;
    }

    public void setToneA(double toneA)
    {
        this.toneA = toneA;
    }

    public double getToneB()
    {
        return toneB;
    }

    public void setToneB(double toneB)
    {
        this.toneB = toneB;
    }

    public double getChannelFrequency()
    {
        return channelFrequency;
    }

    public void setChannelFrequency(double channelFrequency)
    {
        this.channelFrequency = channelFrequency;
    }

    public long getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp(long timestamp)
    {
        this.timestamp = timestamp;
    }
}
