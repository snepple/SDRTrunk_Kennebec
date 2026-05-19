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

import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.broadcast.BroadcastModel;
import io.github.dsheirer.audio.AudioEvent;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.preference.PreferenceEditorType;
import io.github.dsheirer.gui.preference.ViewUserPreferenceEditorRequest;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.icon.MyFontIcon;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.settings.SettingsManager;

import jiconfont.icons.font_awesome.FontAwesome;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.embed.swing.SwingFXUtils;
import javafx.embed.swing.SwingNode;

import javax.sound.sampled.FloatControl;
import javax.swing.SwingUtilities;

import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.swing.Icon;

/**
 * Audio playback panel
 */
public class AudioPanel extends HBox implements Listener<AudioEvent>
{
    private final AliasModel mAliasModel;
    private final AudioPlaybackManager mAudioPlaybackManager;
    private final IconModel mIconModel;
    private final SettingsManager mSettingsManager;
    private final UserPreferences mUserPreferences;
    private AudioChannelsPanel mAudioChannelsPanel;
    private final BroadcastModel mBroadcastModel;
    private MuteButton mMuteButton;

    /**
     * Constructs an instance
     * @param iconModel for icon lookup
     * @param userPreferences for preference lookup
     * @param settingsManager to monitor for changes
     * @param audioPlaybackManager for accessing the audio output
     * @param aliasModel for alias lookup
     */
    public AudioPanel(IconModel iconModel, UserPreferences userPreferences, SettingsManager settingsManager,
                      AudioPlaybackManager audioPlaybackManager, AliasModel aliasModel, BroadcastModel broadcastModel)
    {
        mIconModel = iconModel;
        mSettingsManager = settingsManager;
        mAudioPlaybackManager = audioPlaybackManager;
        mAliasModel = aliasModel;
        mUserPreferences = userPreferences;
        mBroadcastModel = broadcastModel;
        mAudioPlaybackManager.addAudioEventListener(this);
        init();
    }

    private Image createFxIcon(jiconfont.IconCode fontIcon, int size) {
        Icon swingIcon = new MyFontIcon(fontIcon, size, Color.BLACK);
        BufferedImage bImg = new BufferedImage(swingIcon.getIconWidth(), swingIcon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2d = bImg.createGraphics();
        swingIcon.paintIcon(null, g2d, 0, 0);
        g2d.dispose();
        return SwingFXUtils.toFXImage(bImg, null);
    }

    /**
     * Initialize the display
     */
    private void init()
    {
        setSpacing(5);
        setPadding(new Insets(2, 5, 2, 5));
        setAlignment(Pos.CENTER_LEFT);

        // Add bottom border via CSS
        setStyle("-fx-border-color: transparent transparent lightgray transparent; -fx-border-width: 0 0 1 0;");

        mMuteButton = new MuteButton();

        mAudioChannelsPanel = new AudioChannelsPanel(mIconModel, mUserPreferences, mSettingsManager, mAudioPlaybackManager, mAliasModel, mBroadcastModel);
        HBox.setHgrow(mAudioChannelsPanel, Priority.ALWAYS);

        getChildren().addAll(mMuteButton, mAudioChannelsPanel);

        setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                ContextMenu popup = new ContextMenu();

                MenuItem outputMenu = new MenuItem("Audio Playback Device ...");
                outputMenu.setGraphic(new ImageView(createFxIcon(FontAwesome.COG, 14)));
                outputMenu.setOnAction(e -> MyEventBus.getGlobalEventBus()
                        .post(new ViewUserPreferenceEditorRequest(PreferenceEditorType.AUDIO_OUTPUT)));
                popup.getItems().add(outputMenu);

                if(mAudioPlaybackManager.getAudioOutput() != null && mAudioPlaybackManager.getAudioOutput().hasGainControl())
                {
                    popup.getItems().add(new SeparatorMenuItem());

                    MenuItem volume = new MenuItem("Audio Volume");
                    volume.setDisable(true);
                    volume.setGraphic(new ImageView(createFxIcon(FontAwesome.VOLUME_UP, 14)));
                    popup.getItems().add(volume);

                    VolumeSlider slider = new VolumeSlider(mAudioPlaybackManager.getAudioOutput().getGainControl());
                    CustomMenuItem sliderItem = new CustomMenuItem(slider);
                    sliderItem.setHideOnClick(false);
                    popup.getItems().add(sliderItem);
                }

                popup.show(this, event.getScreenX(), event.getScreenY());
            }
        });
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
                    getChildren().remove(mAudioChannelsPanel);
                    mAudioChannelsPanel.dispose();
                    mAudioChannelsPanel = new AudioChannelsPanel(mIconModel, mUserPreferences, mSettingsManager, mAudioPlaybackManager, mAliasModel, mBroadcastModel);
                    HBox.setHgrow(mAudioChannelsPanel, Priority.ALWAYS);

                    if (getChildren().size() >= 1) {
                        getChildren().add(1, mAudioChannelsPanel);
                    } else {
                        getChildren().add(mAudioChannelsPanel);
                    }
                });
                break;
            default:
                break;
        }
    }

    /**
     * Audio output mute control menu item.
     */
    public static class AudioOutputMuteItem extends MenuItem
    {
        private final AudioOutput mAudioOutput;
        private final BooleanProperty mMutedProperty = new SimpleBooleanProperty(false);

        /**
         * Constructs an instance
         * @param audioOutput to mute/unmute
         */
        public AudioOutputMuteItem(AudioOutput audioOutput)
        {
            mAudioOutput = audioOutput;
            mMutedProperty.set(audioOutput.isMuted());

            textProperty().bind(Bindings.when(mMutedProperty).then("Unmute").otherwise("Mute"));

            setOnAction(e -> {
                mAudioOutput.setMuted(!mAudioOutput.isMuted());
                mMutedProperty.set(mAudioOutput.isMuted());
            });
        }
    }

    /**
     * Audio volume (gain) adjustment slider control
     */
    public static class VolumeSlider extends Slider
    {
        private final FloatControl mFloatControl;

        /**
         * Constructs an instance
         * @param control to be controlled
         */
        public VolumeSlider(FloatControl control)
        {
            super(0, 100, 50);
            setShowTickMarks(true);
            setShowTickLabels(true);
            setMajorTickUnit(25);
            setMinorTickCount(4);

            mFloatControl = control;
            setValue(getIntegerValue(mFloatControl.getValue()));

            valueProperty().addListener((observable, oldValue, newValue) -> {
                mFloatControl.shift(mFloatControl.getValue(),
                    getFloatValue(newValue.intValue()),
                    1000);
            });

            setOnMouseClicked(event -> {
                if(event.getClickCount() == 2)
                {
                    setValue(50);
                }
            });
        }

        /**
         * Converts the integer value to a floating point value to use in the
         * float control.  Assumes an integer value of 50 is the 0.0 dB mid
         * point (ie no gain ) value.
         */
        private int getIntegerValue(float value)
        {
            if(value == 0.0f)
            {
                return 50;
            }
            else if(value < 0.0f)
            {
                float ratio = value / mFloatControl.getMinimum();

                return 50 - (int) (ratio * 50.0f);
            }
            else
            {
                float ratio = value / mFloatControl.getMaximum();

                return 50 + (int) (ratio * 50.0f);
            }
        }

        private float getFloatValue(int value)
        {
            if(value == 50)
            {
                return 0.0f;
            }
            else if(value < 50)
            {
                return (float) (50 - value) / 50.0f * mFloatControl.getMinimum();
            }
            else
            {
                return (float) (value - 50) / 50.0f * mFloatControl.getMaximum();
            }
        }
    }

    /**
     * Mute button to mute all audio output channels exposed by the audio
     * controller
     */
    public void setManageWidgetsButton(javax.swing.JButton button)
    {
        Platform.runLater(() -> {
            SwingNode node = new SwingNode();
            SwingUtilities.invokeLater(() -> node.setContent(button));

            HBox.setMargin(node, new Insets(0, 0, 0, 5));
            getChildren().add(node);
        });
    }

    public class MuteButton extends Button
    {
        private final BooleanProperty mMutedProperty = new SimpleBooleanProperty(false);
        private final ImageView mIconView = new ImageView();

        public MuteButton()
        {
            setStyle("-fx-background-color: transparent; -fx-padding: 2;");

            Tooltip tooltip = new Tooltip();
            tooltip.textProperty().bind(Bindings.when(mMutedProperty).then("Unmute").otherwise("Mute"));
            setTooltip(tooltip);

            mIconView.setFitHeight(20);
            mIconView.setFitWidth(20);
            mIconView.setPreserveRatio(true);
            setGraphic(mIconView);

            mMutedProperty.addListener((obs, oldVal, newVal) -> updateIcons(newVal));
            updateIcons(false);

            setOnAction(e -> {
                boolean nextState = !mMutedProperty.get();
                mAudioPlaybackManager.getAudioOutput().setMuted(nextState);
                mMutedProperty.set(nextState);
            });
        }

        private void updateIcons(boolean muted) {
            jiconfont.IconCode iconCode = muted ? FontAwesome.VOLUME_OFF : FontAwesome.VOLUME_UP;
            mIconView.setImage(createFxIcon(iconCode, 20));
        }
    }
}
