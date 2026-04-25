/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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
package io.github.dsheirer.settings;

import org.jdesktop.swingx.mapviewer.GeoPosition;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SettingsModel
{
    private Settings mSettings = new Settings();
    private List<SettingChangeListener> mListeners = new ArrayList<>();

    public Settings getSettings()
    {
        return mSettings;
    }

    public void setSettings(Settings settings)
    {
        mSettings = settings;
    }

    public Setting getSetting(String name)
    {
        return mSettings.getSetting(name);
    }

    /**
     * Returns the current setting, or if the setting doesn't exist
     * returns a newly created setting with the specified parameters
     */
    public ColorSetting getColorSetting(ColorSetting.ColorSettingName name)
    {
        ColorSetting setting = mSettings.getColorSetting(name);

        if(setting == null)
        {
            setting = new ColorSetting(name);

            addSetting(setting);
        }

        return setting;
    }


    /**
     * Fetches the current setting and applies the parameter(s) to it.  Creates
     * the setting if it does not exist
     */
    public void setColorSetting(ColorSetting.ColorSettingName name, Color color)
    {
        ColorSetting setting = getColorSetting(name);

        setting.setColor(color);

        broadcastSettingChange(setting);
    }

    public void resetColorSetting(ColorSetting.ColorSettingName name)
    {
        setColorSetting(name, name.getDefaultColor());
    }

    public void resetAllColorSettings()
    {
        for(ColorSetting color : mSettings.getColorSettings())
        {
            resetColorSetting(color.getColorSettingName());
        }
    }

    /**
     * Returns the current setting, or if the setting doesn't exist
     * returns a newly created setting with the specified parameters
     */
    public FileSetting getFileSetting(String name, String defaultPath)
    {
        FileSetting setting = mSettings.getFileSetting(name);

        if(setting == null)
        {
            setting = new FileSetting(name, defaultPath);

            addSetting(setting);
        }

        return setting;
    }

    /**
     * Fetches the current setting and applies the parameter(s) to it.  Creates
     * the setting if it does not exist
     */
    public void setFileSetting(String name, String path)
    {
        FileSetting setting = getFileSetting(name, path);

        setting.setPath(path);

        broadcastSettingChange(setting);
    }

    /**
     * Adds the setting and stores the set of settings
     *
     * @param setting
     */
    public void addSetting(Setting setting)
    {
        mSettings.addSetting(setting);

        broadcastSettingChange(setting);
    }

    public MapViewSetting getMapViewSetting(String name, GeoPosition position, int zoom)
    {
        MapViewSetting loc = mSettings.getMapViewSetting(name);

        if(loc != null)
        {
            return loc;
        }
        else
        {
            MapViewSetting newLoc = new MapViewSetting(name, position, zoom);

            addSetting(newLoc);

            return newLoc;
        }
    }

    public void setMapViewSetting(String name, GeoPosition position, int zoom)
    {
        MapViewSetting loc = getMapViewSetting(name, position, zoom);

        loc.setGeoPosition(position);
        loc.setZoom(zoom);

        broadcastSettingChange(loc);
    }

    public void broadcastSettingChange(Setting setting)
    {
        Iterator<SettingChangeListener> it = mListeners.iterator();

        while(it.hasNext())
        {
            SettingChangeListener listener = it.next();

            if(listener == null)
            {
                it.remove();
            }
            else
            {
                listener.settingChanged(setting);
            }
        }
    }

    public void broadcastSettingDeleted(Setting setting)
    {
        Iterator<SettingChangeListener> it = mListeners.iterator();

        while(it.hasNext())
        {
            SettingChangeListener listener = it.next();

            if(listener == null)
            {
                it.remove();
            }
            else
            {
                listener.settingDeleted(setting);
            }
        }
    }

    public void addListener(SettingChangeListener listener)
    {
        mListeners.add(listener);
    }

    public void removeListener(SettingChangeListener listener)
    {
        mListeners.remove(listener);
    }
}
