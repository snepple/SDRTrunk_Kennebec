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
import io.github.dsheirer.audio.IAudioController;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.settings.SettingsManager;

import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Displays one or more audio channel panels.
 */
public class AudioChannelsPanel extends HBox
{
    private static final Logger mLog = LoggerFactory.getLogger(AudioChannelsPanel.class);

    /**
     * Constructs an instance
     * @param iconModel for icon access
     * @param userPreferences to monitor for audio playback changes
     * @param settingsManager for tone insertion settings
     * @param controller for the audio channels
     * @param aliasModel for accessing aliases
     */
    public AudioChannelsPanel(IconModel iconModel, UserPreferences userPreferences, SettingsManager settingsManager,
                              IAudioController controller, AliasModel aliasModel, BroadcastModel broadcastModel)
    {
        setSpacing(5);
        setAlignment(Pos.CENTER_LEFT);

        Platform.runLater(() -> {
            getChildren().clear();
            getChildren().add(new Separator(Orientation.VERTICAL));

            for (int x = 0; x < controller.getAudioChannels().size(); x++) {
                AudioChannelPanel javaFxPanel = new AudioChannelPanel(controller.getAudioChannels().get(x), aliasModel, iconModel, settingsManager, userPreferences, broadcastModel);
                HBox.setHgrow(javaFxPanel, Priority.ALWAYS);
                getChildren().add(javaFxPanel);

                if (x < controller.getAudioChannels().size() - 1) {
                    getChildren().add(new Separator(Orientation.VERTICAL));
                }
            }

            if (controller.getAudioChannels().size() == 1) {
                getChildren().add(new Separator(Orientation.VERTICAL));
                AudioChannelPanel javaFxPanel = new AudioChannelPanel(null, aliasModel, iconModel, settingsManager, userPreferences, broadcastModel);
                HBox.setHgrow(javaFxPanel, Priority.ALWAYS);
                getChildren().add(javaFxPanel);
            }
        });
    }

    /**
     * Prepares for dispose to allow deregistering from services
     */
    public void dispose()
    {
        Platform.runLater(() -> {
            for (Node node : getChildren()) {
                if (node instanceof AudioChannelPanel) {
                    ((AudioChannelPanel)node).dispose();
                }
            }
        });
    }
}
