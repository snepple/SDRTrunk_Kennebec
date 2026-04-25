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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationEvent;
import io.github.dsheirer.util.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class SettingsManager implements Listener<TunerConfigurationEvent>
{
    private final static Logger mLog = LoggerFactory.getLogger(SettingsManager.class);

    private SettingsModel mSettingsModel = new SettingsModel();
    private boolean mLoadingSettings = false;
    private AtomicBoolean mSettingsSavePending = new AtomicBoolean();

    public SettingsManager()
    {
        mSettingsModel.addListener(new SettingChangeListener()
        {
            @Override
            public void settingChanged(Setting setting)
            {
                scheduleSettingsSave();
            }

            @Override
            public void settingDeleted(Setting setting)
            {
                scheduleSettingsSave();
            }
        });

        init();
    }

    /**
     * Loads settings from the current settings file, or the default settings file,
     * as specified in the current SDRTrunk system settings
     */
    private void init()
    {
        SystemProperties props = SystemProperties.getInstance();

        Path settingsFolder = props.getApplicationFolder("settings");

        String defaultSettingsFile =
            props.get("settings.defaultFilename", "settings.xml");

        String settingsFile =
            props.get("settings.currentFilename", defaultSettingsFile);

        load(settingsFolder.resolve(settingsFile));
    }

    @Override
    public void receive(TunerConfigurationEvent t)
    {
        if(!mLoadingSettings)
        {
            scheduleSettingsSave();
        }
    }

    public SettingsModel getSettingsModel()
    {
        return mSettingsModel;
    }

    private void save()
    {
        SystemProperties props = SystemProperties.getInstance();

        Path settingsFolder = props.getApplicationFolder("settings");

        String settingsDefault = props.get("settings.defaultFilename",
            "settings.xml");

        String settingsCurrent = props.get("settings.currentFilename",
            settingsDefault);

        Path settingsPath = settingsFolder.resolve(settingsCurrent);

        try(OutputStream out = Files.newOutputStream(settingsPath))
        {
            JacksonXmlModule xmlModule = new JacksonXmlModule();
            xmlModule.setDefaultUseWrapper(false);
            ObjectMapper objectMapper = new XmlMapper(xmlModule);
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            objectMapper.writeValue(out, mSettingsModel.getSettings());
            out.flush();
        }
        catch(IOException ioe)
        {
            mLog.error("IO error while writing the settings to a file [" + settingsPath + "]", ioe);
        }
        catch(Exception e)
        {
            mLog.error("Error while saving settings file [" + settingsPath + "]", e);
        }
    }

    /**
     * Erases current settings and loads settings from the settingsPath filename,
     * if it exists.
     */
    public void load(Path settingsPath)
    {
        mLoadingSettings = true;

        if(Files.exists(settingsPath))
        {
            mLog.info("SettingsManager - loading settings file [" + settingsPath.toString() + "]");

            JacksonXmlModule xmlModule = new JacksonXmlModule();
            xmlModule.setDefaultUseWrapper(false);
            ObjectMapper objectMapper = new XmlMapper(xmlModule)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            try(InputStream in = Files.newInputStream(settingsPath))
            {
                Settings settings = objectMapper.readValue(in, Settings.class);
                mSettingsModel.setSettings(settings);
            }
            catch(IOException ioe)
            {
                mLog.error("IO error while reading settings file", ioe);
            }
        }
        else
        {
            mLog.info("SettingsManager - settings does not exist [" +
                settingsPath.toString() + "]");
        }

        if(mSettingsModel.getSettings() == null)
        {
            mSettingsModel.setSettings(new Settings());
        }

        mLoadingSettings = false;
    }

    /**
     * Schedules a settings save task.  Subsequent calls to this method will be ignored until the
     * save event occurs, thus limiting repetitive saving to a minimum.
     */
    private void scheduleSettingsSave()
    {
        if(!mLoadingSettings)
        {
            if(mSettingsSavePending.compareAndSet(false, true))
            {
                ThreadPool.SCHEDULED.schedule(new SettingsSaveTask(), 2, TimeUnit.SECONDS);
            }
        }
    }

    /**
     * Resets the settings save pending flag to false and proceeds to save the
     * settings.
     */
    public class SettingsSaveTask implements Runnable
    {
        @Override
        public void run()
        {
            mSettingsSavePending.set(false);

            save();
        }
    }
}
