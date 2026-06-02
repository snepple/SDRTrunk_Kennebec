/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.spectrum.menu;

import io.github.dsheirer.dsp.filter.smoothing.SmoothingFilter;
import io.github.dsheirer.spectrum.SpectralDisplayAdjuster;

import javafx.scene.control.Slider;
import javafx.scene.input.MouseButton;

public class SmoothingItem extends Slider
{
    private SpectralDisplayAdjuster mAdjuster;
    private int mDefaultValue;

    public SmoothingItem(SpectralDisplayAdjuster adjuster, int defaultValue)
    {
        super(SmoothingFilter.SMOOTHING_MINIMUM,
            SmoothingFilter.SMOOTHING_MAXIMUM,
            adjuster.getSmoothing());

        mDefaultValue = defaultValue;
        mAdjuster = adjuster;

        setSnapToTicks(true);
        setMajorTickUnit(6);
        setMinorTickCount(2);
        setShowTickMarks(true);
        setShowTickLabels(true);

        valueProperty().addListener((obs, oldVal, newVal) -> {
            int value = newVal.intValue();
            if(value % 2 == 1)
            {
                mAdjuster.setSmoothing(value);
            }
        });

        setOnMouseClicked(event -> {
            if(event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2)
            {
                setValue(mDefaultValue);
            }
        });
    }
}
