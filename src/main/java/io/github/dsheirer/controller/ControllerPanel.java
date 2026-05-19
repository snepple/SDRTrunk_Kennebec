package io.github.dsheirer.controller;

import io.github.dsheirer.gui.VisibilityListener;
import io.github.dsheirer.audio.playback.AudioPanel;
import io.github.dsheirer.audio.playback.AudioPlaybackManager;
import io.github.dsheirer.channel.metadata.NowPlayingPanel;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.map.MapPanel;
import javafx.application.Platform;
import javafx.scene.layout.BorderPane;
import javafx.embed.swing.SwingNode;
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

import javax.swing.SwingUtilities;
import java.io.IOException;

public class ControllerPanel extends BorderPane {
    private final static Logger mLog = LoggerFactory.getLogger(ControllerPanel.class);

    private AudioPanel mAudioPanel;
    private NowPlayingPanel mNowPlayingPanel;
    private MapPanel mMapPanel;
    private TunerViewPanel mTunerManagerPanel;
    private AudioRecordingsPanel mAudioRecordingsPanel;

    private ControllerPanelController mController;

    public ControllerPanel(PlaylistManager playlistManager, AudioPlaybackManager audioPlaybackManager,
                           IconModel iconModel, MapService mapService, SettingsManager settingsManager,
                           TunerManager tunerManager, UserPreferences userPreferences, boolean detailTabsVisible, VisibilityListener visibilityListener) {

        mAudioPanel = new AudioPanel(iconModel, userPreferences, settingsManager, audioPlaybackManager,
            playlistManager.getAliasModel(), playlistManager.getBroadcastModel());
        mNowPlayingPanel = new NowPlayingPanel(playlistManager, iconModel, userPreferences, settingsManager, tunerManager, detailTabsVisible, visibilityListener);
        mMapPanel = new MapPanel(mapService, playlistManager.getAliasModel(), iconModel, settingsManager);
        mTunerManagerPanel = new TunerViewPanel(tunerManager, userPreferences, visibilityListener);
        mAudioRecordingsPanel = new AudioRecordingsPanel(userPreferences, playlistManager);

        mAudioPanel.setManageWidgetsButton(mNowPlayingPanel.getManageWidgetsButton());

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ControllerPanel.fxml"));
            Parent root = loader.load();
            this.setCenter(root);
            mController = loader.getController();

            // Add Swing views by wrapping them in SwingNodes
            mController.addView("now_playing", wrapSwingComponent(mNowPlayingPanel));
            mController.addView("map", mMapPanel);
            mController.addView("tuners", wrapSwingComponent(mTunerManagerPanel));
            mController.addView("audio_recordings", mAudioRecordingsPanel);

            // Add HelpViewer natively without SwingNode
            mController.addView("help_viewer", new io.github.dsheirer.gui.help.HelpViewer());
        } catch (IOException e) {
            mLog.error("Error loading ControllerPanel.fxml", e);
        }
    }

    private SwingNode wrapSwingComponent(javax.swing.JComponent component) {
        SwingNode swingNode = new SwingNode();
        SwingUtilities.invokeLater(() -> swingNode.setContent(component));
        return swingNode;
    }

    public AudioPanel getAudioPanel() {
        return mAudioPanel;
    }

    public NowPlayingPanel getNowPlayingPanel() {
        return mNowPlayingPanel;
    }

    public void addView(String id, java.awt.Component view) {
        Platform.runLater(() -> {
            if (mController != null) {
                mController.addView(id, wrapSwingComponent((javax.swing.JComponent) view));
            }
        });
    }

    public void showView(String id) {
        SwingUtilities.invokeLater(() -> mNowPlayingPanel.getManageWidgetsButton().setVisible("now_playing".equals(id)));

        Platform.runLater(() -> {
            if (mController != null) {
                mController.showView(id);
            }
        });
    }

    public void setResourcePanel(javax.swing.JComponent resourcePanel) {
        Platform.runLater(() -> {
            if (mController != null) {
                mController.setResourceNode(wrapSwingComponent(resourcePanel));
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
