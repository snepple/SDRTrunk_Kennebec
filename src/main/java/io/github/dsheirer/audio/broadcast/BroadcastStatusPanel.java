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
package io.github.dsheirer.audio.broadcast;

import io.github.dsheirer.preference.UserPreferences;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Table of broadcast streams and statuses.
 */
public class BroadcastStatusPanel extends javafx.scene.layout.StackPane
{
    private static final Logger mLog = LoggerFactory.getLogger(BroadcastStatusPanel.class);
    private BroadcastStatusPanelController mController;

    /**
     * Constructs an instance
     * @param broadcastModel to access the streams
     * @param userPreferences for configuring the panel
     * @param preferenceKey to store column preferences for this panel.
     */
    public BroadcastStatusPanel(BroadcastModel broadcastModel, UserPreferences userPreferences, String preferenceKey)
    {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/audio/broadcast/BroadcastStatusPanel.fxml"));
                Parent root = loader.load();
                mController = loader.getController();
                mController.init(broadcastModel, userPreferences, preferenceKey);

                java.net.URL cssUrl = getClass().getResource("/sdrtrunk_style.css");
                if (cssUrl != null) {
                    root.getStylesheets().add(cssUrl.toExternalForm());
                }
                getChildren().add(root);
            } catch (IOException e) {
                mLog.error("Error loading BroadcastStatusPanel.fxml", e);
            }
        });
    }

    public TableView<BroadcastStatusPanelController.BroadcastModelRow> getTable()
    {
        return mController != null ? mController.getTable() : null;
    }

    public void setDisablePanel(boolean disable) {
        if (mController != null && mController.getTable() != null) {
            Platform.runLater(() -> mController.getTable().setDisable(disable));
        } else {
            Platform.runLater(() -> {
                if (mController != null && mController.getTable() != null) {
                    mController.getTable().setDisable(disable);
                }
            });
        }
    }
}
