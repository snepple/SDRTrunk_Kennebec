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
package io.github.dsheirer.source.tuner.recording;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.ui.TunerEditor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.control.Label;
import javafx.scene.control.Separator;

/**
 * Recording tuner configuration editor
 */
public class RecordingTunerEditor extends TunerEditor<RecordingTuner,RecordingTunerConfiguration>
{
    private static final long serialVersionUID = 1L;
    private final static Logger mLog = LoggerFactory.getLogger(RecordingTunerEditor.class);
    private Label mRecordingPath;

    /**
     * Constructs an instance
     * @param userPreferences for wide-band recordings
     * @param tunerManager for saving configurations
     * @param discoveredTuner for this configuration
     */
    public RecordingTunerEditor(UserPreferences userPreferences, TunerManager tunerManager, DiscoveredTuner discoveredTuner)
    {
        super(userPreferences, tunerManager, discoveredTuner);
        init();
        tunerStatusUpdated();
    }

    @Override
    public void setTunerLockState(boolean locked)
    {
        getFrequencyPanel().updateControls();
    }

    @Override
    public long getMinimumTunableFrequency()
    {
        return 0;
    }

    @Override
    public long getMaximumTunableFrequency()
    {
        return Long.MAX_VALUE;
    }

    @Override
    protected void tunerStatusUpdated()
    {
        setLoading(true);

        if(hasTuner())
        {
            getTunerIdLabel().setText(getTuner().getPreferredName() + getUsbInfo());
        }
        else
        {
            getTunerIdLabel().setText(null);
        }

        String status = getDiscoveredTuner().getTunerStatus().toString();
        if(getDiscoveredTuner().hasErrorMessage())
        {
            status += " - " + getDiscoveredTuner().getErrorMessage();
        }
        getTunerStatusLabel().setText(status);
        getButtonPanel().updateControls();
        getFrequencyPanel().updateControls();

        if(hasConfiguration())
        {
            getRecordingPath().setText(getConfiguration().getPath());
        }
        setLoading(false);
    }

    private void init()
    {
        // HIG inspired layout: standardized 8pt grid with gaps
        // setLayout(new javafx.scene.layout.HBox(4));

        getChildren().add(new Label("Tuner:"));
        getChildren().add(getTunerIdLabel());

        getChildren().add(new Label("Status:"));
        getChildren().add(getTunerStatusLabel());

        getChildren().add(new Label("File:"));
        getChildren().add(getRecordingPath());

        getChildren().add(getButtonPanel());

        // Use empty space instead of a separator for HIG deference
        getChildren().add(new Label(""));

        getChildren().add(new Label("Frequency (MHz):"));
        getChildren().add(getFrequencyPanel());
    }
    private Label getRecordingPath()
    {
        if(mRecordingPath == null)
        {
            mRecordingPath = new Label();
        }

        return mRecordingPath;
    }

    @Override
    public void save()
    {
        if(hasConfiguration() && !isLoading())
        {
            RecordingTunerConfiguration config = getConfiguration();
            config.setFrequency(getFrequencyControl().getFrequency());
            getConfiguration().setMinimumFrequency(getMinimumFrequencyTextField().getFrequency());
            getConfiguration().setMaximumFrequency(getMaximumFrequencyTextField().getFrequency());
            saveConfiguration();
        }
    }
}