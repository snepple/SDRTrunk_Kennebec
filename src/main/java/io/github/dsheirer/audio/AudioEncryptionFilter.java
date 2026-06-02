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

package io.github.dsheirer.audio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Filters encrypted audio segments before they reach the playback buffer, preventing garbled
 * encrypted audio from being heard by the user. Uses the existing AudioSegment.isEncrypted() API.
 */
public class AudioEncryptionFilter
{
    private static final Logger mLog = LoggerFactory.getLogger(AudioEncryptionFilter.class);
    private final AtomicLong mSuppressedCount = new AtomicLong(0);
    private boolean mEnabled = true;

    /**
     * Determines whether the given audio segment should be suppressed from playback.
     *
     * @param segment the audio segment to evaluate
     * @return true if the segment is encrypted and the filter is enabled, meaning it should be suppressed
     */
    public boolean shouldSuppress(AudioSegment segment)
    {
        if(!mEnabled)
        {
            return false;
        }

        if(segment != null && segment.isEncrypted())
        {
            mSuppressedCount.incrementAndGet();
            return true;
        }

        return false;
    }

    /**
     * Enables or disables the encryption filter.
     *
     * @param enabled true to enable filtering, false to allow all segments through
     */
    public void setEnabled(boolean enabled)
    {
        mEnabled = enabled;
    }

    /**
     * Indicates if the encryption filter is currently enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled()
    {
        return mEnabled;
    }

    /**
     * Returns the total number of encrypted audio segments that have been suppressed.
     *
     * @return the suppressed segment count
     */
    public long getSuppressedCount()
    {
        return mSuppressedCount.get();
    }
}
