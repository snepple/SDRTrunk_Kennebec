package io.github.dsheirer.audio.playback;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;

import javafx.fxml.FXML;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.control.Separator;
import javafx.geometry.Orientation;


import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.broadcast.BroadcastModel;
import io.github.dsheirer.audio.IAudioController;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.settings.SettingsManager;



public class AudioChannelsPanelController {

    @FXML
    private HBox mainContainer;

    public void init(IconModel iconModel, UserPreferences userPreferences, SettingsManager settingsManager,
                     IAudioController controller, AliasModel aliasModel, BroadcastModel broadcastModel, io.github.dsheirer.playlist.PlaylistManager playlistManager) {

        mainContainer.getChildren().clear();
        mainContainer.getChildren().add(new Separator(Orientation.VERTICAL));

        for (int x = 0; x < controller.getAudioChannels().size(); x++) {
            AudioChannelPanel panel = new AudioChannelPanel(controller.getAudioChannels().get(x), aliasModel, iconModel, settingsManager, userPreferences, broadcastModel, playlistManager);
            HBox.setHgrow(panel, Priority.ALWAYS);
            mainContainer.getChildren().add(panel);

            if (x < controller.getAudioChannels().size() - 1) {
                mainContainer.getChildren().add(new Separator(Orientation.VERTICAL));
            }
        }

        if (controller.getAudioChannels().size() == 1) {
            mainContainer.getChildren().add(new Separator(Orientation.VERTICAL));
            AudioChannelPanel panel = new AudioChannelPanel(null, aliasModel, iconModel, settingsManager, userPreferences, broadcastModel, playlistManager);
            HBox.setHgrow(panel, Priority.ALWAYS);
            mainContainer.getChildren().add(panel);
        }
    }

    public void dispose() {
        for (javafx.scene.Node node : mainContainer.getChildren()) {
            if (node instanceof AudioChannelPanel) {
                ((AudioChannelPanel)node).dispose();
            }
        }
    }
}
