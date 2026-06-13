package io.github.dsheirer.gui.playlist.channel;

import io.github.dsheirer.controller.channel.Channel;

public class NxdnConfigPanel extends javafx.embed.swing.JFXPanel {
    private Channel mChannel;

    public NxdnConfigPanel(Channel channel) {
        this.mChannel = channel;

        javafx.application.Platform.runLater(() -> {
            javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(8);
            vbox.setPadding(new javafx.geometry.Insets(16));
            vbox.setAlignment(javafx.geometry.Pos.CENTER);

            javafx.scene.control.Label label = new javafx.scene.control.Label("NXDN Configuration (Placeholder)");
            vbox.getChildren().add(label);

            javafx.scene.Scene scene = new javafx.scene.Scene(vbox);
            setScene(scene);
        });
    }
}
