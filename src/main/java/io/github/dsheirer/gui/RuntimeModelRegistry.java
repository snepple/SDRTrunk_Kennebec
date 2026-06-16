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
package io.github.dsheirer.gui;

import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.source.tuner.manager.TunerManager;

/**
 * Lightweight, read-only registry that exposes a few runtime models (tuners, channels, streams) to UI components
 * that are constructed without direct access to them — notably the application preferences editor, which uses these
 * counts to recommend a memory allocation.  All accessors are null-safe and exception-safe and return -1 when the
 * value is unavailable, so callers degrade gracefully if the registry was never populated.
 */
public final class RuntimeModelRegistry
{
    private static volatile TunerManager sTunerManager;
    private static volatile PlaylistManager sPlaylistManager;

    private RuntimeModelRegistry()
    {
    }

    /**
     * Registers the runtime models.  Called once during application startup.
     */
    public static void register(TunerManager tunerManager, PlaylistManager playlistManager)
    {
        sTunerManager = tunerManager;
        sPlaylistManager = playlistManager;
    }

    /**
     * Count of available (enabled and present) tuners, or -1 if unknown.
     */
    public static int getActiveTunerCount()
    {
        try
        {
            if(sTunerManager != null)
            {
                return sTunerManager.getDiscoveredTunerModel().getAvailableTuners().size();
            }
        }
        catch(Throwable t)
        {
            //Ignore - return unknown
        }

        return -1;
    }

    /**
     * Count of configured channels, or -1 if unknown.
     */
    public static int getTotalChannelCount()
    {
        try
        {
            if(sPlaylistManager != null)
            {
                return sPlaylistManager.getChannelModel().getChannels().size();
            }
        }
        catch(Throwable t)
        {
            //Ignore - return unknown
        }

        return -1;
    }

    /**
     * Count of channels currently processing, or -1 if unknown.
     */
    public static int getProcessingChannelCount()
    {
        try
        {
            if(sPlaylistManager != null)
            {
                return sPlaylistManager.getChannelProcessingManager().getProcessingChannelCount();
            }
        }
        catch(Throwable t)
        {
            //Ignore - return unknown
        }

        return -1;
    }

    /**
     * Count of configured streaming/broadcast configurations, or -1 if unknown.
     */
    public static int getStreamCount()
    {
        try
        {
            if(sPlaylistManager != null)
            {
                return sPlaylistManager.getBroadcastModel().getBroadcastConfigurations().size();
            }
        }
        catch(Throwable t)
        {
            //Ignore - return unknown
        }

        return -1;
    }
}
