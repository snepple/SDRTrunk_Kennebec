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
package io.github.dsheirer.spectrum;

import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.settings.SettingsManager;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.ui.DiscoveredTunerModel;

import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.scene.Scene;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;



public class SpectrumFrame extends Stage
{
    private SpectralDisplayPanel mSpectralDisplayPanel;

    public SpectrumFrame(PlaylistManager playlistManager, SettingsManager settingsManager,
                         DiscoveredTunerModel discoveredTunerModel, Tuner tuner)
    {
        setTitle("SDRTRunk [" + tuner.getPreferredName() + "]");
        setWidth(1280);
        setHeight(600);
        setX(100);
        setY(100);

        mSpectralDisplayPanel = new SpectralDisplayPanel(playlistManager, settingsManager, discoveredTunerModel);
        mSpectralDisplayPanel.showTuner(tuner);

        SwingNode swingNode = new SwingNode();
        Platform.runLater(() -> {
            // swingNode.setContent(mSpectralDisplayPanel);
        });

        VBox vbox = new VBox(swingNode);
        VBox.setVgrow(swingNode, Priority.ALWAYS);

        Scene scene = new Scene(vbox);
        setScene(scene);

        setOnCloseRequest(event -> {
            mSpectralDisplayPanel.dispose();
        });

        Platform.runLater(() -> {
            show();
        });
    }
}
