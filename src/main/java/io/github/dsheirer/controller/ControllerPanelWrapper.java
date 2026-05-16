package io.github.dsheirer.controller;

import io.github.dsheirer.audio.playback.AudioPanel;
import io.github.dsheirer.channel.metadata.NowPlayingPanel;
import io.github.dsheirer.gui.VisibilityListener;
import io.github.dsheirer.audio.playback.AudioPlaybackManager;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.map.MapService;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.settings.SettingsManager;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;

public class ControllerPanelWrapper extends JFXPanel {
    public javafx.scene.Node getNode() { return controllerPanel; }
    private ControllerPanel controllerPanel;

    public ControllerPanelWrapper(PlaylistManager playlistManager, AudioPlaybackManager audioPlaybackManager,
                           IconModel iconModel, MapService mapService, SettingsManager settingsManager,
                           TunerManager tunerManager, UserPreferences userPreferences, boolean detailTabsVisible, VisibilityListener visibilityListener) {
        controllerPanel = new ControllerPanel(playlistManager, audioPlaybackManager, iconModel, mapService, settingsManager, tunerManager, userPreferences, detailTabsVisible, visibilityListener);
        Platform.runLater(() -> {
            Scene scene = new Scene(controllerPanel);
            setScene(scene);
        });
    }

    public ControllerPanel getControllerPanel() {
        return controllerPanel;
    }

    public AudioPanel getAudioPanel() {
        return controllerPanel.getAudioPanel();
    }

    public NowPlayingPanel getNowPlayingPanel() {
        return controllerPanel.getNowPlayingPanel();
    }

    public void addView(String id, javafx.scene.Node view) {
        controllerPanel.addView(id, view);
    }

    public void showView(String id) {
        controllerPanel.showView(id);
    }

    public void setResourcePanel(javafx.scene.Node resourcePanel) {
        controllerPanel.setResourcePanel(resourcePanel);
    }

    public void setResourcePanelVisible(boolean visible) {
        controllerPanel.setResourcePanelVisible(visible);
    }
}
