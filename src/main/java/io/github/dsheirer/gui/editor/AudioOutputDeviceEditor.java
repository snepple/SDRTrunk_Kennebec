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
package io.github.dsheirer.gui.editor;

import io.github.dsheirer.alias.Alias;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import java.io.IOException;

/**
 * Audio Output Device Editor - GUI component for selecting VAC output per alias
 * Shows RAW device names (without DirectSound wrapper) to match what's saved in XML
 */
public class AudioOutputDeviceEditor extends HBox
{
    private final static Logger mLog = LoggerFactory.getLogger(AudioOutputDeviceEditor.class);

    @FXML private ComboBox<String> mDeviceComboBox;
    @FXML private CheckBox mRecordableCheckBox;
    @FXML private Slider mPrioritySlider;

    private Alias mAlias;
    private StringProperty mUiDeviceProperty = new SimpleStringProperty("System Default");

    /**
     * Constructor
     */
    public AudioOutputDeviceEditor()
    {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/AudioOutputDeviceEditor.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try
        {
            fxmlLoader.load();
        }
        catch (IOException exception)
        {
            throw new RuntimeException("Error loading FXML for AudioOutputDeviceEditor", exception);
        }

        // Populate with RAW device names (what gets saved in XML)
        populateDevices();

        // Add default/system option at top
        mDeviceComboBox.getItems().add(0, "System Default");

        // Bind the UI selection to our intermediary property
        mDeviceComboBox.valueProperty().bindBidirectional(mUiDeviceProperty);
    }

    /**
     * Populates the combo box with clean device names
     */
    private void populateDevices()
    {
        try
        {
            Mixer.Info[] mixers = AudioSystem.getMixerInfo();

            for(Mixer.Info mixerInfo : mixers)
            {
                String mixerName = mixerInfo.getName();
                String displayName = mixerName;

                // Strip "DirectSound Playback(...)" wrapper to get raw device name
                if(mixerName.startsWith("DirectSound Playback(") && mixerName.endsWith(")"))
                {
                    int start = mixerName.indexOf('(');
                    int end = mixerName.lastIndexOf(')');
                    if(start > 0 && end > start)
                    {
                        displayName = mixerName.substring(start + 1, end);
                    }
                }

                // Strip "Port " prefix if present
                if(displayName.startsWith("Port "))
                {
                    displayName = displayName.substring(5); // Remove "Port "
                }

                // Only add if not already in list (avoid duplicates)
                if(!mDeviceComboBox.getItems().contains(displayName))
                {
                    mDeviceComboBox.getItems().add(displayName);
                }
            }

            mLog.info("Populated " + (mDeviceComboBox.getItems().size() - 1) + " audio output devices");
        }
        catch(Exception e)
        {
            mLog.error("Error populating audio devices", e);
        }
    }

    /**
     * Sets the alias to edit
     * @param alias to edit
     */
    public void setAlias(Alias alias)
    {
        if (mAlias != null)
        {
            Bindings.unbindBidirectional(mUiDeviceProperty, mAlias.audioOutputDeviceProperty());
            mRecordableCheckBox.selectedProperty().unbindBidirectional(mAlias.recordableProperty());
            mPrioritySlider.valueProperty().unbindBidirectional(mAlias.priorityProperty());
        }

        mAlias = alias;

        if(mAlias != null)
        {
            String deviceName = mAlias.getAudioOutputDevice();

            if(deviceName != null && !deviceName.isEmpty() && !mDeviceComboBox.getItems().contains(deviceName))
            {
                // Device not found, add it
                mDeviceComboBox.getItems().add(deviceName);
                mLog.warn("Device [" + deviceName + "] not found in available devices, added anyway");
            }

            // Bind intermediary property bidirectionally to the model property with null conversion
            Bindings.bindBidirectional(mUiDeviceProperty, mAlias.audioOutputDeviceProperty(), new StringConverter<String>()
            {
                @Override
                public String toString(String aliasValue)
                {
                    return (aliasValue == null || aliasValue.isEmpty()) ? "System Default" : aliasValue;
                }

                @Override
                public String fromString(String uiValue)
                {
                    return "System Default".equals(uiValue) ? null : uiValue;
                }
            });

            mRecordableCheckBox.selectedProperty().bindBidirectional(mAlias.recordableProperty());
            mPrioritySlider.valueProperty().bindBidirectional(mAlias.priorityProperty());
        }
    }

    /**
     * Refreshes the device list
     */
    public void refresh()
    {
        String currentSelection = mDeviceComboBox.getSelectionModel().getSelectedItem();
        mDeviceComboBox.getItems().clear();
        populateDevices();
        mDeviceComboBox.getItems().add(0, "System Default");

        // Try to restore previous selection
        if(currentSelection != null)
        {
            mDeviceComboBox.getSelectionModel().select(currentSelection);
        }
        else
        {
            mDeviceComboBox.getSelectionModel().select(0);
        }
    }
}
