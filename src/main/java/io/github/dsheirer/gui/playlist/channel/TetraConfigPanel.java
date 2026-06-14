package io.github.dsheirer.gui.playlist.channel;

import io.github.dsheirer.controller.channel.Channel;

public class TetraConfigPanel extends javafx.embed.swing.JFXPanel {
    private Channel mChannel;

    public TetraConfigPanel(Channel channel) {
        mChannel = channel;
        javafx.application.Platform.runLater(() -> {
            javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox();
            vbox.setPadding(new javafx.geometry.Insets(16));
            vbox.getChildren().add(new javafx.scene.control.Label("TETRA Configuration (Placeholder)"));
            setScene(new javafx.scene.Scene(vbox));
        });
    }
}
