/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */
package io.github.dsheirer.controller.channel.map;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChannelRangeModel
{
    private ObservableList<ChannelRange> mRanges = FXCollections.observableArrayList(ChannelRange.extractor());

    public ChannelRangeModel()
    {
        mRanges.addListener((javafx.collections.ListChangeListener<ChannelRange>) c -> {
            boolean updateNeeded = false;
            while (c.next()) {
                if (c.wasUpdated() || c.wasAdded() || c.wasRemoved()) {
                    updateNeeded = true;
                }
            }
            if (updateNeeded) {
                javafx.application.Platform.runLater(this::validate);
            }
        });
    }

    public void clear()
    {
        mRanges.clear();
    }

    public ObservableList<ChannelRange> getChannelRanges()
    {
        return mRanges;
    }

    public void addRanges(List<ChannelRange> ranges)
    {
        mRanges.addAll(ranges);
    }

    public void addRange(ChannelRange range)
    {
        if (range != null && !mRanges.contains(range))
        {
            mRanges.add(range);
        }
    }

    public void removeRange(ChannelRange range)
    {
        if (range != null)
        {
            mRanges.remove(range);
        }
    }

    private void validate()
    {
        Set<ChannelRange> overlappingRanges = new HashSet<>();

        for (int x = 0; x < mRanges.size(); x++)
        {
            for (int y = x + 1; y < mRanges.size(); y++)
            {
                if (mRanges.get(x).overlaps(mRanges.get(y)))
                {
                    overlappingRanges.add(mRanges.get(x));
                    overlappingRanges.add(mRanges.get(y));
                }
            }
        }

        for (ChannelRange range : mRanges) {
            boolean shouldBeOverlapping = overlappingRanges.contains(range);
            if (range.isOverlapping() != shouldBeOverlapping) {
                range.setOverlapping(shouldBeOverlapping);
            }
        }
    }
}
