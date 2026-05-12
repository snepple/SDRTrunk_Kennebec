package io.github.dsheirer.audio.playback;

import javafx.fxml.FXML;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.control.Separator;
import javafx.geometry.Orientation;
import javafx.embed.swing.SwingNode;

import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.broadcast.BroadcastModel;
import io.github.dsheirer.audio.IAudioController;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.settings.SettingsManager;

import javax.swing.SwingUtilities;

public class AudioChannelsPanelController {

    @FXML
    private HBox mainContainer;

    public void init(IconModel iconModel, UserPreferences userPreferences, SettingsManager settingsManager,
                     IAudioController controller, AliasModel aliasModel, BroadcastModel broadcastModel) {

        mainContainer.getChildren().clear();
        mainContainer.getChildren().add(new Separator(Orientation.VERTICAL));

        for (int x = 0; x < controller.getAudioChannels().size(); x++) {
            AudioChannelPanel swingPanel = new AudioChannelPanel(controller.getAudioChannels().get(x), aliasModel, iconModel, settingsManager, userPreferences, broadcastModel);
            SwingNode swingNode = new SwingNode();
            SwingUtilities.invokeLater(() -> swingNode.setContent(swingPanel));
            HBox.setHgrow(swingNode, Priority.ALWAYS);
            mainContainer.getChildren().add(swingNode);

            if (x < controller.getAudioChannels().size() - 1) {
                mainContainer.getChildren().add(new Separator(Orientation.VERTICAL));
            }
        }

        if (controller.getAudioChannels().size() == 1) {
            mainContainer.getChildren().add(new Separator(Orientation.VERTICAL));
            AudioChannelPanel swingPanel = new AudioChannelPanel(null, aliasModel, iconModel, settingsManager, userPreferences, broadcastModel);
            SwingNode swingNode = new SwingNode();
            SwingUtilities.invokeLater(() -> swingNode.setContent(swingPanel));
            HBox.setHgrow(swingNode, Priority.ALWAYS);
            mainContainer.getChildren().add(swingNode);
        }
    }

    public void dispose() {
        for (javafx.scene.Node node : mainContainer.getChildren()) {
            if (node instanceof SwingNode) {
                javax.swing.JComponent comp = ((SwingNode)node).getContent();
                if (comp instanceof AudioChannelPanel) {
                    ((AudioChannelPanel)comp).dispose();
                }
            }
        }
    }
}
