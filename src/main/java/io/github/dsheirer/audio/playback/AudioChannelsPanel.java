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
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Displays one or more audio channel panels.
 */
public class AudioChannelsPanel extends javafx.scene.layout.StackPane
{
    private static final Logger mLog = LoggerFactory.getLogger(AudioChannelsPanel.class);
    private AudioChannelsPanelController mController;

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
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/audio/playback/AudioChannelsPanel.fxml"));
                Parent root = loader.load();
                mController = loader.getController();
                mController.init(iconModel, userPreferences, settingsManager, controller, aliasModel, broadcastModel);

                java.net.URL cssUrl = getClass().getResource("/sdrtrunk_style.css");
                if (cssUrl != null) {
                    root.getStylesheets().add(cssUrl.toExternalForm());
                }
                getChildren().add(root);
            } catch (IOException e) {
                mLog.error("Error loading AudioChannelsPanel.fxml", e);
            }
        });
    }

    /**
     * Prepares for dispose to allow deregistering from services
     */
    public void dispose()
    {
        Platform.runLater(() -> {
            if (mController != null) {
                mController.dispose();
            }
        });
    }
}
