package io.github.dsheirer.gui.playlist.channel;

import io.github.dsheirer.controller.channel.Channel;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

public class TetraConfigPanel extends JFXPanel {
    private io.github.dsheirer.controller.channel.Channel mChannel;

    public TetraConfigPanel(io.github.dsheirer.controller.channel.Channel channel) {
        mChannel = channel;
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/playlist/channel/TetraConfigPanel.fxml"));
                Parent root = loader.load();
                setScene(new Scene(root));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
