/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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
package io.github.dsheirer.audio.broadcast.iamresponding;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.dsheirer.audio.broadcast.BroadcastConfiguration;
import io.github.dsheirer.audio.broadcast.BroadcastFormat;
import io.github.dsheirer.audio.broadcast.BroadcastServerType;

public class IAmRespondingConfiguration extends BroadcastConfiguration
{
    public IAmRespondingConfiguration()
    {
        super(BroadcastFormat.MP3);
    }

    @Override
    public BroadcastConfiguration copyOf()
    {
        IAmRespondingConfiguration config = new IAmRespondingConfiguration();
        config.setName(getName());
        config.setHost(getHost());
        config.setPort(getPort());
        config.setPassword(getPassword());
        config.setDelay(getDelay());
        config.setMaximumRecordingAge(getMaximumRecordingAge());
        config.setEnabled(isEnabled());
        return config;
    }

    @Override
    public BroadcastServerType getBroadcastServerType()
    {
        return BroadcastServerType.IAMRESPONDING;
    }

    @JsonIgnore
    @Override
    public boolean isValid()
    {
        return hasHost() && hasPort();
    }
}
