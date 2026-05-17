/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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
package io.github.dsheirer.controller;

import io.github.dsheirer.gui.SidebarPanel;
import io.github.dsheirer.gui.VisibilityListener;
import io.github.dsheirer.audio.playback.AudioPanel;
import io.github.dsheirer.audio.playback.AudioPlaybackManager;
import io.github.dsheirer.channel.metadata.NowPlayingPanel;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.map.MapPanel;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.embed.swing.SwingNode;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.Node;
import io.github.dsheirer.map.MapService;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.settings.SettingsManager;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.ui.TunerViewPanel;
import io.github.dsheirer.gui.recordings.AudioRecordingsPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import java.util.HashMap;
import java.util.Map;

public class ControllerPanel extends JFXPanel
{
    private final static Logger mLog = LoggerFactory.getLogger(ControllerPanel.class);

    private AudioPanel mAudioPanel;
    private NowPlayingPanel mNowPlayingPanel;
    private MapPanel mMapPanel;
    private TunerViewPanel mTunerManagerPanel;
    private AudioRecordingsPanel mAudioRecordingsPanel;

    private BorderPane mRootPane;
    private StackPane mCardPane;
    private SwingNode mResourceNode;

    private Map<String, Node> mViews = new HashMap<>();

    public ControllerPanel(PlaylistManager playlistManager, AudioPlaybackManager audioPlaybackManager,
                           IconModel iconModel, MapService mapService, SettingsManager settingsManager,
                           TunerManager tunerManager, UserPreferences userPreferences, boolean detailTabsVisible, VisibilityListener visibilityListener)
    {
        mAudioPanel = new AudioPanel(iconModel, userPreferences, settingsManager, audioPlaybackManager,
            playlistManager.getAliasModel(), playlistManager.getBroadcastModel());
        mNowPlayingPanel = new NowPlayingPanel(playlistManager, iconModel, userPreferences, settingsManager, tunerManager, detailTabsVisible, visibilityListener);
        mMapPanel = new MapPanel(mapService, playlistManager.getAliasModel(), iconModel, settingsManager);
        mTunerManagerPanel = new TunerViewPanel(tunerManager, userPreferences, visibilityListener);
        mAudioRecordingsPanel = new AudioRecordingsPanel(userPreferences, playlistManager);

        mAudioPanel.setManageWidgetsButton(mNowPlayingPanel.getManageWidgetsButton());

        init();
    }

    public AudioPanel getAudioPanel()
    {
        return mAudioPanel;
    }

    public NowPlayingPanel getNowPlayingPanel()
    {
        return mNowPlayingPanel;
    }

    private void init()
    {
        Platform.runLater(() -> {
            mRootPane = new BorderPane();
            mCardPane = new StackPane();
            mRootPane.setCenter(mCardPane);

            Scene scene = new Scene(mRootPane);
            setScene(scene);

            addView("now_playing", mNowPlayingPanel);
            addView("map", mMapPanel);
            addView("tuners", mTunerManagerPanel);
            addView("audio_recordings", mAudioRecordingsPanel);

            // HelpViewer is already a JFXPanel, so we can embed it as a Node directly?
            // Actually, JFXPanel is a Swing component, so we must wrap it in a SwingNode anyway
            // unless we refactor it completely to be a pure JavaFX Node.
            // Since we made HelpViewer extend JFXPanel, it's a Component.
            addJavaFXView("help_viewer", loadHelpViewer());
        });
    }


    private javafx.scene.Node loadHelpViewer() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/HelpView.fxml"));
            javafx.scene.Parent root = loader.load();
            java.net.URL cssUrl = getClass().getResource("/sdrtrunk_style.css");
            if (cssUrl != null) {
                root.getStylesheets().add(cssUrl.toExternalForm());
            }
            return root;
        } catch (java.io.IOException e) {
            mLog.error("Error loading HelpView.fxml", e);
            return new javafx.scene.layout.VBox();
        }
    }

    public void addJavaFXView(String id, javafx.scene.Node view) {
        Platform.runLater(() -> {
            view.setVisible(false);
            mCardPane.getChildren().add(view);
            mViews.put(id, view);
        });
    }

    public void addView(String id, java.awt.Component view) {
        Platform.runLater(() -> {
            SwingNode swingNode = new SwingNode();
            SwingUtilities.invokeLater(() -> swingNode.setContent((javax.swing.JComponent) view));
            swingNode.setVisible(false);
            mCardPane.getChildren().add(swingNode);
            mViews.put(id, swingNode);
        });
    }

    public void showView(String id) {
        SwingUtilities.invokeLater(() -> mNowPlayingPanel.getManageWidgetsButton().setVisible("now_playing".equals(id)));

        Platform.runLater(() -> {
            for (Map.Entry<String, Node> entry : mViews.entrySet()) {
                entry.getValue().setVisible(entry.getKey().equals(id));
            }
        });
    }

    public void setResourcePanel(javax.swing.JComponent resourcePanel) {
        Platform.runLater(() -> {
            if (mResourceNode == null) {
                mResourceNode = new SwingNode();
                mRootPane.setBottom(mResourceNode);
            }
            SwingUtilities.invokeLater(() -> mResourceNode.setContent(resourcePanel));
            mResourceNode.setVisible(false);
        });
    }

    public void setResourcePanelVisible(boolean visible) {
        Platform.runLater(() -> {
            if (mResourceNode != null) {
                mResourceNode.setVisible(visible);
            }
        });
    }

}
