package io.github.dsheirer.gui.playlist.channel;

import io.github.dsheirer.controller.channel.Channel;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

public class NxdnConfigPanel extends JFXPanel {
    private static final Logger mLog = LoggerFactory.getLogger(NxdnConfigPanel.class);
    private Channel mChannel;

    public NxdnConfigPanel(Channel channel) {
        mChannel = channel;

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/channel/NxdnConfigPanel.fxml"));
                Parent root = loader.load();

                NxdnConfigController controller = loader.getController();
                controller.setChannel(mChannel);

                Scene scene = new Scene(root);
                setScene(scene);
            } catch (IOException e) {
                mLog.error("Failed to load NxdnConfigPanel FXML", e);
            }
        });
    }
}
