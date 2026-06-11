package io.github.dsheirer.gui.playlist.channel;

import io.github.dsheirer.controller.channel.Channel;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class NxdnConfigPanel extends JFXPanel {
    private Channel mChannel;

    public NxdnConfigPanel(Channel channel) {
        mChannel = channel;
        Platform.runLater(() -> {
            VBox vbox = new VBox();
            vbox.setPadding(new Insets(16));
            vbox.getChildren().add(new Label("NXDN Configuration (Placeholder)"));
            Scene scene = new Scene(vbox);
            setScene(scene);
        });
    }
}
