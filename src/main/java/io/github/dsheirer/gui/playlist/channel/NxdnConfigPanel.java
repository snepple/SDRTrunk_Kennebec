package io.github.dsheirer.gui.playlist.channel;

import io.github.dsheirer.controller.channel.Channel;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.io.IOException;
import java.net.URL;

public class NxdnConfigPanel extends JPanel {
    private Channel mChannel;
    private JFXPanel jfxPanel;
    private NxdnConfigController controller;

    public NxdnConfigPanel(Channel channel) {
        mChannel = channel;
        setLayout(new BorderLayout());

        jfxPanel = new JFXPanel();
        add(jfxPanel, BorderLayout.CENTER);

        Platform.runLater(this::createScene);
    }

    private void createScene() {
        try {
            URL fxmlUrl = getClass().getResource("NxdnConfigPanel.fxml");
            if (fxmlUrl == null) {
                System.err.println("Cannot find NxdnConfigPanel.fxml");
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            controller = loader.getController();
            controller.setChannel(mChannel);

            Scene scene = new Scene(root);
            jfxPanel.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
