

package io.github.dsheirer.controller;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;
import javafx.scene.layout.Region;
import javafx.scene.control.Button;

import io.github.dsheirer.gui.VisibilityListener;
import io.github.dsheirer.audio.playback.AudioPanel;
import io.github.dsheirer.audio.playback.AudioPlaybackManager;
import io.github.dsheirer.channel.metadata.NowPlayingPanel;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.map.MapPanel;
import javafx.application.Platform;
import javafx.scene.layout.Pane;
import javafx.embed.swing.SwingNode;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.fxml.FXMLLoader;
import io.github.dsheirer.map.MapService;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.settings.SettingsManager;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.ui.TunerViewPanel;
import io.github.dsheirer.gui.recordings.AudioRecordingsPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.IOException;

public class ControllerPanel extends javafx.scene.layout.StackPane {
    private final static Logger mLog = LoggerFactory.getLogger(ControllerPanel.class);

    private AudioPanel mAudioPanel;
    private NowPlayingPanel mNowPlayingPanel;
    private MapPanel mMapPanel;
    private TunerViewPanel mTunerManagerPanel;
    private AudioRecordingsPanel mAudioRecordingsPanel;

    private ControllerPanelController mController;
    private Runnable mOnContentReady;
    private boolean mContentReady;

    public ControllerPanel(PlaylistManager playlistManager, AudioPlaybackManager audioPlaybackManager,
                           IconModel iconModel, MapService mapService, SettingsManager settingsManager,
                           TunerManager tunerManager, UserPreferences userPreferences, boolean detailTabsVisible, VisibilityListener visibilityListener) {

        mAudioPanel = new AudioPanel(iconModel, userPreferences, settingsManager, audioPlaybackManager, playlistManager);
        mNowPlayingPanel = new NowPlayingPanel(playlistManager, iconModel, userPreferences, settingsManager, tunerManager, detailTabsVisible, visibilityListener);
        mMapPanel = new MapPanel(mapService, playlistManager.getAliasModel(), iconModel, settingsManager);
        mTunerManagerPanel = new TunerViewPanel(tunerManager, userPreferences, playlistManager, visibilityListener);
        mAudioRecordingsPanel = new AudioRecordingsPanel(userPreferences, playlistManager);

        mAudioPanel.setManageWidgetsButton(mNowPlayingPanel.getManageWidgetsButton());

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ControllerPanel.fxml"));
                Parent root = loader.load();
                mController = loader.getController();

                // Add Swing views by wrapping them in SwingNodes
                mController.addView("now_playing", mNowPlayingPanel);
                mController.addView("map", mMapPanel);
                mController.addView("tuners", mTunerManagerPanel);
                mController.addView("audio_recordings", mAudioRecordingsPanel);

                // Add HelpViewer natively without SwingNode
                mController.addView("help_viewer", new io.github.dsheirer.gui.help.HelpViewer());

                // Ensure the FXML root BorderPane fills the entire StackPane
                if (root instanceof Region) {
                    ((Region) root).setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                }
                getChildren().add(root);

                //Signal that the main content is now built and on-screen, so the splash can be hidden
                //without showing an empty window first.
                mContentReady = true;
                if (mOnContentReady != null) {
                    mOnContentReady.run();
                }
            } catch (IOException e) {
                mLog.error("Error loading ControllerPanel.fxml", e);
            }
        });
    }

    /**
     * Registers a callback invoked (on the JavaFX thread) once the main content has been built and
     * added. If the content is already built, the callback runs immediately.
     */
    public void setOnContentReady(Runnable onContentReady) {
        mOnContentReady = onContentReady;
        if (mContentReady && onContentReady != null) {
            onContentReady.run();
        }
    }







    public AudioPanel getAudioPanel() {
        return mAudioPanel;
    }

    public NowPlayingPanel getNowPlayingPanel() {
        return mNowPlayingPanel;
    }

    public void addView(String id, javafx.scene.Node view) {
        Platform.runLater(() -> {
            if (mController != null) {
                mController.addView(id, (javafx.scene.Node) view);
            }
        });
    }

    public void showView(String id) {
        Platform.runLater(() -> mNowPlayingPanel.getManageWidgetsButton().setVisible("now_playing".equals(id)));

        Platform.runLater(() -> {
            if (mController != null) {
                mController.showView(id);
            }
        });
    }

    public void setResourcePanel(javafx.scene.Node resourcePanel) {
        Platform.runLater(() -> {
            if (mController != null) {
                mController.setResourceNode(resourcePanel);
                mController.setResourcePanelVisible(false);
            }
        });
    }

    public void setResourcePanelVisible(boolean visible) {
        Platform.runLater(() -> {
            if (mController != null) {
                mController.setResourcePanelVisible(visible);
            }
        });
    }
}
