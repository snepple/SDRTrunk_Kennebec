


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

    // Now Playing display components
    private Label mChannelNameLabel;
    private Label mAliasLabel;
    private Label mStreamingLabel;
    private ImageView mArtworkView;
    private StackPane mArtworkContainer;
    private javafx.scene.image.Image mCustomArtwork = null;

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
        setMinHeight(64);
        setPrefHeight(64);
        setMaxHeight(64);
        setStyle("-fx-background-color: rgba(30,30,36,0.97); -fx-border-color: rgba(255,255,255,0.08); -fx-border-width: 0 0 1 0;");

        // --- LEFT: Artwork + Upload ---
        mArtworkView = new ImageView();
        mArtworkView.setFitWidth(46);
        mArtworkView.setFitHeight(46);
        mArtworkView.setPreserveRatio(false);
        mArtworkView.setSmooth(true);

        resetArtworkViewPlaceholder();

        // Clip to squircle (rounded rect ~20px radius)
        Rectangle artClip = new Rectangle(46, 46);
        artClip.setArcWidth(12);
        artClip.setArcHeight(12);
        mArtworkView.setClip(artClip);

        DropShadow artShadow = new DropShadow();
        artShadow.setRadius(8);
        artShadow.setColor(Color.rgb(0, 0, 0, 0.5));
        artShadow.setOffsetY(2);
        mArtworkView.setEffect(artShadow);

        mArtworkContainer = new StackPane(mArtworkView);
        mArtworkContainer.setPadding(new Insets(9, 10, 9, 12));

        // --- CENTER: Now Playing metadata display ---
        mChannelNameLabel = new Label("");
        mChannelNameLabel.setStyle("-fx-text-fill: #F0F0F5; -fx-font-size: 13px; -fx-font-weight: bold;");
        mChannelNameLabel.setMaxWidth(Double.MAX_VALUE);
        mChannelNameLabel.setEllipsisString("…");

        mAliasLabel = new Label("");
        mAliasLabel.setStyle("-fx-text-fill: #A0A0B0; -fx-font-size: 11px;");
        mAliasLabel.setMaxWidth(Double.MAX_VALUE);

        mStreamingLabel = new Label("");
        mStreamingLabel.setStyle("-fx-text-fill: #6060A0; -fx-font-size: 10px;");
        mStreamingLabel.setMaxWidth(Double.MAX_VALUE);

        VBox metaBox = new VBox(1, mChannelNameLabel, mAliasLabel, mStreamingLabel);
        metaBox.setAlignment(Pos.CENTER_LEFT);
        metaBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(metaBox, Priority.ALWAYS);

        // --- RIGHT: Mute + Volume Slider + Audio Channels ---
        mMuteButton = new MuteButton();

        Label volIcon = new Label("🔊");
        volIcon.setStyle("-fx-text-fill: #808090; -fx-font-size: 11px;");

        Slider masterVolSlider = new MasterVolumeSlider();
        masterVolSlider.setPrefWidth(90);
        masterVolSlider.setTooltip(new Tooltip("Master Volume"));

        mAudioChannelsPanel = new AudioChannelsPanel(mIconModel, mUserPreferences, mSettingsManager, mAudioPlaybackManager, mAliasModel, mBroadcastModel);
        mAudioChannelsScroller = new javafx.scene.control.ScrollPane(mAudioChannelsPanel);
        mAudioChannelsScroller.getStyleClass().add("audio-channels-scroller");
        mAudioChannelsScroller.setStyle("-fx-background-color: transparent;");
        mAudioChannelsScroller.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        mAudioChannelsScroller.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        mAudioChannelsScroller.setFitToHeight(true);
        mAudioChannelsScroller.setPrefWidth(200);
        mAudioChannelsScroller.setVisible(false);
        mAudioChannelsScroller.setManaged(false);

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
        getChildren().addAll(mArtworkContainer, metaBox, mAudioChannelsScroller, rightControls);
    }



    public static class ActiveCallUpdateEvent {
        private String mChannelName;
        private String mAliasName;
        
        public ActiveCallUpdateEvent(String channelName, String aliasName) {
            mChannelName = channelName;
            mAliasName = aliasName;
        }
        
        public String getChannelName() { return mChannelName; }
        public String getAliasName() { return mAliasName; }
    }

    @com.google.common.eventbus.Subscribe
    public void onActiveCallUpdate(ActiveCallUpdateEvent event) {
        updateNowPlaying(event.getChannelName(), event.getAliasName(), "");
    }

    @com.google.common.eventbus.Subscribe
    public void onTwoToneDetected(io.github.dsheirer.dsp.tone.TwoToneDetectedEvent event) {
        if (event.isShowNotification()) {
            Platform.runLater(() -> {
                mChannelNameLabel.setText("🚨 TWO-TONE ALERT 🚨");
                mChannelNameLabel.setStyle("-fx-text-fill: #FF3B30; -fx-font-size: 13px; -fx-font-weight: bold;");
                mAliasLabel.setText(event.getMessage());
                mAliasLabel.setStyle("-fx-text-fill: #FF453A; -fx-font-size: 11px;");
                mStreamingLabel.setText("Channel: " + event.getChannel());

                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(Duration.seconds(10));
                pause.setOnFinished(e -> {
                    mChannelNameLabel.setStyle("-fx-text-fill: #F0F0F5; -fx-font-size: 13px; -fx-font-weight: bold;");
                    mAliasLabel.setStyle("-fx-text-fill: #A0A0B0; -fx-font-size: 11px;");
                    mChannelNameLabel.setText("");
                    mAliasLabel.setText("");
                    mStreamingLabel.setText("");
                });
                pause.play();
            });
        }
    }

    /**
     * Updates the Now Playing display with channel information.
     * Called from the audio playback manager or channel metadata updates.
     */
    public void updateNowPlaying(String channelName, String aliasName, String streamingInfo) {
        Platform.runLater(() -> {
            if (channelName != null && !channelName.isEmpty() && !channelName.equals("No Active Channel")) {
                mChannelNameLabel.setText(channelName);
                
                // Lookup channel and load artwork
                io.github.dsheirer.controller.channel.Channel chan = null;
                if (mPlaylistManager != null && mPlaylistManager.getChannelModel() != null) {
                    for (io.github.dsheirer.controller.channel.Channel c : mPlaylistManager.getChannelModel().getChannels()) {
                        if (channelName.equals(c.getName())) {
                            chan = c;
                            break;
                        }
                    }
                }
                
                if (chan != null && chan.getImagePath() != null && !chan.getImagePath().isEmpty()) {
                    try {
                        java.io.File file = new java.io.File(chan.getImagePath());
                        if (file.exists()) {
                            javafx.scene.image.Image img = new javafx.scene.image.Image(file.toURI().toString(), 46, 46, false, true);
                            mArtworkView.setImage(img);
                            mArtworkContainer.setVisible(true);
                            mArtworkContainer.setManaged(true);
                        } else {
                            resetArtworkViewPlaceholder();
                        }
                    } catch (Exception ex) {
                        resetArtworkViewPlaceholder();
                    }
                } else {
                    resetArtworkViewPlaceholder();
                }
            } else {
                if (!mChannelNameLabel.getText().equals("🚨 TWO-TONE ALERT 🚨")) {
                    mChannelNameLabel.setText("");
                }
                resetArtworkViewPlaceholder();
            }
            if (!mChannelNameLabel.getText().equals("🚨 TWO-TONE ALERT 🚨")) {
                mAliasLabel.setText(aliasName != null && !aliasName.equals("—") ? aliasName : "");
                mStreamingLabel.setText(streamingInfo != null ? streamingInfo : "");
            }
        });
    }

    private void resetArtworkViewPlaceholder() {
        mArtworkView.setImage(null);
        mArtworkContainer.setVisible(false);
        mArtworkContainer.setManaged(false);
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
                    mAudioChannelsPanel = new AudioChannelsPanel(mIconModel, mUserPreferences, mSettingsManager, mAudioPlaybackManager, mAliasModel, mBroadcastModel);
                    mAudioChannelsScroller = new javafx.scene.control.ScrollPane(mAudioChannelsPanel);
                    mAudioChannelsScroller.getStyleClass().add("audio-channels-scroller");
                    mAudioChannelsScroller.setStyle("-fx-background-color: transparent;");
                    mAudioChannelsScroller.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
                    mAudioChannelsScroller.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
                    mAudioChannelsScroller.setFitToHeight(true);
                    mAudioChannelsScroller.setPrefWidth(200);
                    mAudioChannelsScroller.setVisible(false);
                    mAudioChannelsScroller.setManaged(false);
                    // Re-insert before the separator
                    int sepIndex = getChildren().size() - 2;
                    getChildren().add(Math.max(0, sepIndex), mAudioChannelsScroller);
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
        private boolean mMuted = false;

        public MuteButton()
        {
            getStyleClass().add("mute-button");
            setMinWidth(42);
            setMinHeight(32);
            setPrefWidth(42);
            setPrefHeight(32);
            updateAppearance();
            setTooltip(new javafx.scene.control.Tooltip("Mute All Audio (Ctrl+M)"));
            accessibleTextProperty().set("Mute");
            setOnAction(e -> {
                mMuted = !mMuted;
                mAudioPlaybackManager.getAudioOutput().setMuted(mMuted);
                Platform.runLater(() -> {
                    updateAppearance();
                    setTooltip(new javafx.scene.control.Tooltip(mMuted ? "Unmute All Audio (Ctrl+M)" : "Mute All Audio (Ctrl+M)"));
                    accessibleTextProperty().set(mMuted ? "Unmute" : "Mute");
                });
            });
        }

        private void updateAppearance() {
            if (mMuted) {
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
