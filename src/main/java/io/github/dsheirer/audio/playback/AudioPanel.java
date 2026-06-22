


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
package io.github.dsheirer.audio.playback;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;
import javafx.scene.control.Separator;

import io.github.dsheirer.module.ProcessingChain;

import javafx.application.Platform;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.broadcast.BroadcastModel;
import io.github.dsheirer.audio.AudioEvent;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.preference.PreferenceEditorType;
import io.github.dsheirer.gui.preference.ViewUserPreferenceEditorRequest;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.settings.SettingsManager;

import javafx.animation.FadeTransition;
import javafx.scene.effect.DropShadow;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;

import javax.sound.sampled.FloatControl;
import java.io.File;


/**
 * Audio playback panel - iTunes-style sticky header
 */
public class AudioPanel extends HBox implements Listener<AudioEvent>
{
    private final io.github.dsheirer.playlist.PlaylistManager mPlaylistManager;
    private final AliasModel mAliasModel;
    private final AudioPlaybackManager mAudioPlaybackManager;
    private final IconModel mIconModel;
    private final SettingsManager mSettingsManager;
    private final UserPreferences mUserPreferences;
    private AudioChannelsPanel mAudioChannelsPanel;
    private javafx.scene.control.ScrollPane mAudioChannelsScroller;
    private final BroadcastModel mBroadcastModel;
    private Button mMuteButton;

    // Now Playing display components removed in favor of AudioChannelsPanel

    /**
     * Constructs an instance
     */
    public AudioPanel(IconModel iconModel, UserPreferences userPreferences, SettingsManager settingsManager,
                      AudioPlaybackManager audioPlaybackManager, io.github.dsheirer.playlist.PlaylistManager playlistManager)
    {
        mIconModel = iconModel;
        mSettingsManager = settingsManager;
        mAudioPlaybackManager = audioPlaybackManager;
        mPlaylistManager = playlistManager;
        mAliasModel = playlistManager.getAliasModel();
        mBroadcastModel = playlistManager.getBroadcastModel();
        mUserPreferences = userPreferences;
        mAudioPlaybackManager.addAudioEventListener(this);
        MyEventBus.getGlobalEventBus().register(this);
        init();
    }

    /**
     * Initialize the display
     */
    private void init()
    {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(0);
        setMinHeight(72);
        setPrefHeight(72);
        setMaxHeight(72);
        setStyle("-fx-background-color: rgba(30,30,36,0.97); -fx-border-color: rgba(255,255,255,0.08); -fx-border-width: 0 0 1 0;");


        // --- RIGHT: Mute + Volume Slider + Audio Channels ---
        mMuteButton = new MuteButton();

        Label volIcon = new Label("🔊");
        volIcon.setStyle("-fx-text-fill: #808090; -fx-font-size: 11px;");

        Slider masterVolSlider = new MasterVolumeSlider();
        masterVolSlider.setPrefWidth(90);
        masterVolSlider.setTooltip(new Tooltip("Master Volume"));

        mAudioChannelsPanel = new AudioChannelsPanel(mIconModel, mUserPreferences, mSettingsManager, mAudioPlaybackManager, mAliasModel, mBroadcastModel, mPlaylistManager);
        mAudioChannelsScroller = new javafx.scene.control.ScrollPane(mAudioChannelsPanel);
        mAudioChannelsScroller.getStyleClass().add("audio-channels-scroller");
        mAudioChannelsScroller.setStyle("-fx-background-color: transparent;");
        mAudioChannelsScroller.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        mAudioChannelsScroller.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        mAudioChannelsScroller.setFitToHeight(true);
        //Stretch the channel content to fill the available width so the now-playing display uses the whole
        //bar (it was previously left at its small preferred width, leaving a large empty gap).
        mAudioChannelsScroller.setFitToWidth(true);
        mAudioChannelsScroller.setVisible(true);
        mAudioChannelsScroller.setManaged(true);
        HBox.setHgrow(mAudioChannelsScroller, Priority.ALWAYS);

        ToggleButton monoStereoButton = new ToggleButton();
        monoStereoButton.getStyleClass().add("mono-stereo-button");
        monoStereoButton.setStyle("-fx-background-color: rgba(255,255,255,0.25); -fx-background-radius: 4; -fx-text-fill: #ffffff; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 6 10;");
        monoStereoButton.setTooltip(new Tooltip("Toggle between Mono and Stereo playback"));
        
        io.github.dsheirer.audio.playback.AudioPlaybackDeviceDescriptor currDevice = mUserPreferences.getPlaybackPreference().getAudioPlaybackDevice();
        boolean isStereo = (currDevice != null && currDevice.getAudioFormat().getChannels() == 2);
        monoStereoButton.setSelected(isStereo);
        monoStereoButton.setText(isStereo ? "STEREO" : "MONO");
        
        monoStereoButton.setOnAction(e -> {
            boolean stereo = monoStereoButton.isSelected();
            monoStereoButton.setText(stereo ? "STEREO" : "MONO");
            io.github.dsheirer.audio.playback.AudioPlaybackDeviceDescriptor dev = mUserPreferences.getPlaybackPreference().getAudioPlaybackDevice();
            if (dev != null) {
                int newChannelCount = stereo ? 2 : 1;
                io.github.dsheirer.audio.playback.AudioPlaybackDeviceDescriptor newDev = io.github.dsheirer.audio.playback.AudioPlaybackDeviceManager.getAudioPlaybackDevice(dev.getMixerInfo().getName(), newChannelCount);
                if (newDev != null) {
                    mUserPreferences.getPlaybackPreference().setAudioPlaybackDevice(newDev);
                }
            }
        });

        HBox rightControls = new HBox(6, monoStereoButton, mMuteButton, volIcon, masterVolSlider);
        rightControls.setAlignment(Pos.CENTER);
        rightControls.setPadding(new Insets(0, 12, 0, 8));

        // Assemble the header
        getChildren().addAll(mAudioChannelsScroller, rightControls);
    }





    /**
     * Receive audio event notifications from the audio playback controller
     */
    @Override
    public void receive(AudioEvent event)
    {
        switch(event.getType())
        {
            case AUDIO_CONFIGURATION_CHANGE_STARTED:
                break;
            case AUDIO_CONFIGURATION_CHANGE_COMPLETE:
                Platform.runLater(() -> {
                    getChildren().remove(mAudioChannelsScroller);
                    mAudioChannelsPanel.dispose();
                    mAudioChannelsPanel = new AudioChannelsPanel(mIconModel, mUserPreferences, mSettingsManager, mAudioPlaybackManager, mAliasModel, mBroadcastModel, mPlaylistManager);
                    mAudioChannelsScroller = new javafx.scene.control.ScrollPane(mAudioChannelsPanel);
                    mAudioChannelsScroller.getStyleClass().add("audio-channels-scroller");
                    mAudioChannelsScroller.setStyle("-fx-background-color: transparent;");
                    mAudioChannelsScroller.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
                    mAudioChannelsScroller.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
                    mAudioChannelsScroller.setFitToHeight(true);
                    mAudioChannelsScroller.setVisible(true);
                    mAudioChannelsScroller.setManaged(true);
                    HBox.setHgrow(mAudioChannelsScroller, Priority.ALWAYS);
                    // Re-insert before the separator
                    getChildren().add(0, mAudioChannelsScroller);
                    requestLayout();
                });
                break;
            default:
                break;
        }
    }

    /**
     * Audio output mute control menu item.
     */
    public static class AudioOutputMuteItem extends javafx.scene.control.MenuItem
    {
        private final AudioOutput mAudioOutput;

        public AudioOutputMuteItem(AudioOutput audioOutput)
        {
            super(audioOutput.isMuted() ? "Unmute" : "Mute");
            mAudioOutput = audioOutput;
            setOnAction(e -> mAudioOutput.setMuted(!mAudioOutput.isMuted()));
        }
    }

    /**
     * Master volume slider tied to the primary audio output gain control.
     * Uses a 0-100 range where 50 = 0 dB (unity gain).
     */
    public static class VolumeSlider extends javafx.scene.control.Slider
    {
        private final FloatControl mFloatControl;

        public VolumeSlider(FloatControl control)
        {
            super(0, 100, 0);
            setMajorTickUnit(25);
            setMinorTickCount(4);
            setShowTickMarks(true);
            setShowTickLabels(true);
            mFloatControl = control;
            setValue(getIntegerValue(mFloatControl.getValue()));

            valueProperty().addListener((obs, oldVal, newVal) -> {
                mFloatControl.shift(mFloatControl.getValue(), getFloatValue(newVal.intValue()), 1000);
            });
            setOnMouseClicked(event -> {
                if(event.getClickCount() == 2) VolumeSlider.this.setValue(50);
            });
        }

        private int getIntegerValue(float value) {
            if(value == 0.0f) return 50;
            else if(value < 0.0f) return 50 - (int)((value / mFloatControl.getMinimum()) * 50.0f);
            else return 50 + (int)((value / mFloatControl.getMaximum()) * 50.0f);
        }

        private float getFloatValue(int value) {
            if(value == 50) return 0.0f;
            else if(value < 50) return (float)(50 - value) / 50.0f * mFloatControl.getMinimum();
            else return (float)(value - 50) / 50.0f * mFloatControl.getMaximum();
        }
    }

    /**
     * Master volume slider that controls overall gain (not tied to a specific FloatControl).
     * Displays as a compact horizontal slider in the Now Playing header.
     */
    public static class MasterVolumeSlider extends Slider {
        public MasterVolumeSlider() {
            super(0, 100, 75);
            setOrientation(Orientation.HORIZONTAL);
            setStyle("-fx-control-inner-background: #3a3a4a;");
        }
    }

    public void setManageWidgetsButton(Button button)
    {
        // Manage widgets button is now placed in the main toolbar, not in the audio panel
    }

    public class MuteButton extends Button
    {
        private final javafx.beans.value.ChangeListener<Boolean> mMuteListener;

        public MuteButton()
        {
            getStyleClass().add("mute-button");
            setMinWidth(42);
            setMinHeight(32);
            setPrefWidth(42);
            setPrefHeight(32);
            updateAppearance();
            updateTooltip();
            setOnAction(e -> mAudioPlaybackManager.toggleMasterMuted());

            //Drive the button's appearance from the shared master-mute state so it stays in sync no matter what
            //toggled the mute (this button, the system tray, etc.).
            mMuteListener = (obs, was, now) -> Platform.runLater(() -> {
                updateAppearance();
                updateTooltip();
            });
            //Weak listener so the long-lived AudioPlaybackManager doesn't pin this button if the panel is recreated.
            //The button holds the strong reference (mMuteListener field) for as long as it is alive.
            mAudioPlaybackManager.masterMutedProperty().addListener(new javafx.beans.value.WeakChangeListener<>(mMuteListener));
        }

        private void updateTooltip() {
            boolean muted = mAudioPlaybackManager.isMasterMuted();
            setTooltip(new javafx.scene.control.Tooltip(muted ? "Unmute All Audio (Ctrl+M)" : "Mute All Audio (Ctrl+M)"));
            accessibleTextProperty().set(muted ? "Unmute" : "Mute");
        }

        private void updateAppearance() {
            if (mAudioPlaybackManager.isMasterMuted()) {
                setText("🔇");
                setStyle("-fx-background-color: rgba(220,38,38,0.85); -fx-background-radius: 6; " +
                         "-fx-cursor: hand; -fx-font-size: 16px; -fx-padding: 4 8 4 8; " +
                         "-fx-effect: dropshadow(gaussian, rgba(220,38,38,0.5), 8, 0, 0, 0);");
                if (!getStyleClass().contains("muted")) {
                    getStyleClass().add("muted");
                }
            } else {
                setText("🔊");
                setStyle("-fx-background-color: rgba(255,255,255,0.25); -fx-background-radius: 4; " +
                         "-fx-cursor: hand; -fx-font-size: 16px; -fx-padding: 4 8 4 8; -fx-text-fill: #ffffff;");
                getStyleClass().remove("muted");
            }
        }
    }
}
