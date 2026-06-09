package io.github.dsheirer.gui.playlist.channel;

import io.github.dsheirer.controller.channel.Channel;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class NxdnConfigPanel extends JFXPanel {
    private Channel mChannel;

    public NxdnConfigPanel(Channel channel) {
        mChannel = channel;
        Platform.runLater(() -> {
            Label placeholderLabel = new Label("NXDN Configuration (Placeholder)");
            placeholderLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333;");

            VBox root = new VBox(16);
            root.setPadding(new Insets(16));
            root.setAlignment(Pos.CENTER);
            root.getChildren().add(placeholderLabel);

            Scene scene = new Scene(root);
            setScene(scene);
        });
    }
}
