/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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
package io.github.dsheirer.source.tuner;

import io.github.dsheirer.source.SourceException;
import java.util.List;

/**
 * Defines a tuner controller that can have its sample rate queried and configured.
 */
public interface ISampleRateConfigurable
{
    /**
     * Gets a list of available sample rates in Hertz.
     * The list should be ordered from lowest to highest.
     */
    List<Integer> getAvailableSampleRatesInHz();

    /**
     * Sets the sample rate in Hertz.
     * @param sampleRateHz The sample rate to set.
     * @throws SourceException if the sample rate could not be set.
     */
    void setSampleRateInHz(int sampleRateHz) throws SourceException;
}
