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

package io.github.dsheirer.gui.preference.playback;

import io.github.dsheirer.audio.playback.AudioPlaybackDeviceDescriptor;
import io.github.dsheirer.audio.playback.AudioPlaybackDeviceManager;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.playback.PlayTestAudioRequest;
import io.github.dsheirer.preference.playback.PlaybackPreference;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import io.github.dsheirer.gui.preference.layout.SettingsCard;
import io.github.dsheirer.gui.preference.layout.SettingsRow;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;
import org.controlsfx.control.ToggleSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Preference settings for audio playback
 */
public class PlaybackPreferenceEditor extends VBox
{
    private final static Logger mLog = LoggerFactory.getLogger(PlaybackPreferenceEditor.class);
    private final PlaybackPreference mPlaybackPreference;
    private ComboBox<AudioPlaybackDeviceDescriptor> mAudioPlaybackDevicesCombo;
    private Button mPlaybackDeviceTestButton;
    private ToggleSwitch mUseAudioSegmentStartToneSwitch;
    private Button mTestStartToneButton;
    private ToggleSwitch mUseAudioSegmentDropToneSwitch;
    private Button mTestDropToneButton;
    private ComboBox<ToneFrequency> mStartToneFrequencyComboBox;
    private ComboBox<ToneVolume> mStartToneVolumeComboBox;
    private ComboBox<ToneFrequency> mDropToneFrequencyComboBox;
    private ComboBox<ToneVolume> mDropToneVolumeComboBox;

    public PlaybackPreferenceEditor(UserPreferences userPreferences)
    {
        mPlaybackPreference = userPreferences.getPlaybackPreference();
        setPadding(new javafx.geometry.Insets(10, 10, 10, 10));
        setSpacing(20);

        Label outputHeader = new Label("Audio Playback Device");
        outputHeader.getStyleClass().add("hig-section-header");

        SettingsCard deviceCard = new SettingsCard();
        deviceCard.getChildren().add(new SettingsRow("Output Device", getAudioPlaybackDevicesCombo(), getPlaybackDeviceTestButton()));

        Label insertHeader = new Label("Audio Playback Insert Tones");
        insertHeader.getStyleClass().add("hig-section-header");

        SettingsCard startToneCard = new SettingsCard();
        HBox startToneOptions = new HBox(10);
        startToneOptions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        startToneOptions.getChildren().addAll(new Label("Frequency:"), getStartToneFrequencyComboBox(), new Label("Volume:"), getStartToneVolumeComboBox(), getTestStartToneButton());
        startToneCard.getChildren().add(new SettingsRow("Play Start Tone", getUseAudioSegmentStartToneSwitch()));
        startToneCard.getChildren().add(new SettingsRow("", startToneOptions));

        SettingsCard dropToneCard = new SettingsCard();
        HBox dropToneOptions = new HBox(10);
        dropToneOptions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        dropToneOptions.getChildren().addAll(new Label("Frequency:"), getDropToneFrequencyComboBox(), new Label("Volume:"), getDropToneVolumeComboBox(), getTestDropToneButton());
        dropToneCard.getChildren().add(new SettingsRow("Drop Tone - Do Not Monitor", getUseAudioSegmentDropToneSwitch()));
        dropToneCard.getChildren().add(new SettingsRow("", dropToneOptions));

        getChildren().addAll(outputHeader, deviceCard, insertHeader, startToneCard, dropToneCard);
    }

private ComboBox<AudioPlaybackDeviceDescriptor> getAudioPlaybackDevicesCombo()
    {
        if(mAudioPlaybackDevicesCombo == null)
        {
            mAudioPlaybackDevicesCombo = new ComboBox<>();
            mAudioPlaybackDevicesCombo.getItems().addAll(AudioPlaybackDeviceManager.getAudioPlaybackDevices());
            mAudioPlaybackDevicesCombo.getSelectionModel().select(mPlaybackPreference.getAudioPlaybackDevice());
            mAudioPlaybackDevicesCombo.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue,
                              newValue) -> mPlaybackPreference.setAudioPlaybackDevice(newValue));
            mAudioPlaybackDevicesCombo.setTooltip(new Tooltip("Selects the primary device for playing live audio."));
        }

        return mAudioPlaybackDevicesCombo;
    }

    public Button getPlaybackDeviceTestButton()
    {
        if(mPlaybackDeviceTestButton == null)
        {
            mPlaybackDeviceTestButton = new Button("Test");
            IconNode iconNode = new IconNode(FontAwesome.PLAY);
            iconNode.setFill(Color.CORNFLOWERBLUE);
            mPlaybackDeviceTestButton.setGraphic(iconNode);
            mPlaybackDeviceTestButton.setTooltip(new Tooltip("Test the selected audio playback device"));
            mPlaybackDeviceTestButton.setOnAction(event ->
                        play(mPlaybackPreference.getAudioPlaybackTestTone(), PlayTestAudioRequest.ALL_CHANNELS));
        }

        return mPlaybackDeviceTestButton;
    }

    private ToggleSwitch getUseAudioSegmentStartToneSwitch()
    {
        if(mUseAudioSegmentStartToneSwitch == null)
        {
            mUseAudioSegmentStartToneSwitch = new ToggleSwitch();
            mUseAudioSegmentStartToneSwitch.setAlignment(Pos.BASELINE_RIGHT);
            mUseAudioSegmentStartToneSwitch.setSelected(mPlaybackPreference.getUseAudioSegmentStartTone());
            mUseAudioSegmentStartToneSwitch.selectedProperty().addListener((observable, oldValue, newValue) ->
                    mPlaybackPreference.setUseAudioSegmentStartTone(newValue));
            mUseAudioSegmentStartToneSwitch.setTooltip(new Tooltip("Plays a tone at the beginning of each transmission segment."));
        }

        return mUseAudioSegmentStartToneSwitch;
    }

    private ToggleSwitch getUseAudioSegmentDropToneSwitch()
    {
        if(mUseAudioSegmentDropToneSwitch == null)
        {
            mUseAudioSegmentDropToneSwitch = new ToggleSwitch();
            mUseAudioSegmentDropToneSwitch.setSelected(mPlaybackPreference.getUseAudioSegmentDropTone());
            mUseAudioSegmentDropToneSwitch.selectedProperty().addListener((observable, oldValue, newValue) ->
                    mPlaybackPreference.setUseAudioSegmentDropTone(newValue));
            mUseAudioSegmentDropToneSwitch.setTooltip(new Tooltip("Plays a tone when a transmission segment drops or ends."));
        }

        return mUseAudioSegmentDropToneSwitch;
    }

    public Button getTestStartToneButton()
    {
        if(mTestStartToneButton == null)
        {
            mTestStartToneButton = new Button("Test");
            IconNode iconNode = new IconNode(FontAwesome.PLAY);
            iconNode.setFill(Color.CORNFLOWERBLUE);
            mTestStartToneButton.setGraphic(iconNode);
            mTestStartToneButton.setTooltip(new Tooltip("Play a sample of the start tone with current settings"));
            mTestStartToneButton.setOnAction(_ ->
                play(mPlaybackPreference.getStartTone(PlaybackPreference.TONE_LENGTH_SAMPLES * 3),
                        PlayTestAudioRequest.ALL_CHANNELS));
            mTestStartToneButton.disableProperty().bind(getUseAudioSegmentStartToneSwitch().selectedProperty().not());
        }

        return mTestStartToneButton;
    }

    public Button getTestDropToneButton()
    {
        if(mTestDropToneButton == null)
        {
            mTestDropToneButton = new Button("Test");
            IconNode iconNode = new IconNode(FontAwesome.PLAY);
            iconNode.setFill(Color.CORNFLOWERBLUE);
            mTestDropToneButton.setGraphic(iconNode);
            mTestDropToneButton.setTooltip(new Tooltip("Play a sample of the drop tone with current settings"));
            mTestDropToneButton.setOnAction(_ ->
                    play(mPlaybackPreference.getDropTone(PlaybackPreference.TONE_LENGTH_SAMPLES * 3),
                            PlayTestAudioRequest.ALL_CHANNELS));
            mTestDropToneButton.disableProperty().bind(getUseAudioSegmentDropToneSwitch().selectedProperty().not());
        }

        return mTestDropToneButton;
    }

    public ComboBox<ToneFrequency> getDropToneFrequencyComboBox()
    {
        if(mDropToneFrequencyComboBox == null)
        {
            mDropToneFrequencyComboBox = new ComboBox<>();
            mDropToneFrequencyComboBox.getItems().addAll(ToneFrequency.values());
            mDropToneFrequencyComboBox.getSelectionModel().select(mPlaybackPreference.getDropToneFrequency());
            mDropToneFrequencyComboBox.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> mPlaybackPreference.setDropToneFrequency(newValue));
            mDropToneFrequencyComboBox.disableProperty().bind(getUseAudioSegmentDropToneSwitch().selectedProperty().not());
            mDropToneFrequencyComboBox.setTooltip(new Tooltip("Selects the frequency (pitch) of the drop tone."));
        }

        return mDropToneFrequencyComboBox;
    }

    public ComboBox<ToneVolume> getDropToneVolumeComboBox()
    {
        if(mDropToneVolumeComboBox == null)
        {
            mDropToneVolumeComboBox = new ComboBox<>();
            mDropToneVolumeComboBox.getItems().addAll(ToneVolume.values());
            mDropToneVolumeComboBox.getSelectionModel().select(mPlaybackPreference.getDropToneVolume());
            mDropToneVolumeComboBox.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> mPlaybackPreference.setDropToneVolume(newValue));
            mDropToneVolumeComboBox.disableProperty().bind(getUseAudioSegmentDropToneSwitch().selectedProperty().not());
            mDropToneVolumeComboBox.setTooltip(new Tooltip("Selects the volume level of the drop tone."));
        }

        return mDropToneVolumeComboBox;
    }

    public ComboBox<ToneFrequency> getStartToneFrequencyComboBox()
    {
        if(mStartToneFrequencyComboBox == null)
        {
            mStartToneFrequencyComboBox = new ComboBox<>();
            mStartToneFrequencyComboBox.getItems().addAll(ToneFrequency.values());
            mStartToneFrequencyComboBox.getSelectionModel().select(mPlaybackPreference.getStartToneFrequency());
            mStartToneFrequencyComboBox.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> mPlaybackPreference.setStartToneFrequency(newValue));
            mStartToneFrequencyComboBox.disableProperty().bind(getUseAudioSegmentStartToneSwitch().selectedProperty().not());
            mStartToneFrequencyComboBox.setTooltip(new Tooltip("Selects the frequency (pitch) of the start tone."));
        }

        return mStartToneFrequencyComboBox;
    }

    public ComboBox<ToneVolume> getStartToneVolumeComboBox()
    {
        if(mStartToneVolumeComboBox == null)
        {
            mStartToneVolumeComboBox = new ComboBox<>();
            mStartToneVolumeComboBox.getItems().addAll(ToneVolume.values());
            mStartToneVolumeComboBox.getSelectionModel().select(mPlaybackPreference.getStartToneVolume());
            mStartToneVolumeComboBox.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> mPlaybackPreference.setStartToneVolume(newValue));
            mStartToneVolumeComboBox.disableProperty().bind(getUseAudioSegmentStartToneSwitch().selectedProperty().not());
            mStartToneVolumeComboBox.setTooltip(new Tooltip("Selects the volume level of the start tone."));
        }

        return mStartToneVolumeComboBox;
    }

    /**
     * Sends a request to play the audio samples over the specified audio playback channel
     * @param audioSamples with 8 kHz mono PCM samples
     * @param channel number (0=mono/left, 1=right, etc.)
     */
    private void play(float[] audioSamples, int channel)
    {
        MyEventBus.getGlobalEventBus().post(new PlayTestAudioRequest(audioSamples, channel));
    }
}
