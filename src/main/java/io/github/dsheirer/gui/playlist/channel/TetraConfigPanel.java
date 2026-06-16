package io.github.dsheirer.gui.playlist.channel;

import io.github.dsheirer.controller.channel.Channel;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TetraConfigPanel extends JFXPanel {
    private final static Logger mLog = LoggerFactory.getLogger(TetraConfigPanel.class);
    private Channel mChannel;
    private TetraConfigController mController;

    public TetraConfigPanel(Channel channel) {
        mChannel = channel;
        initFX();
    }

    private void initFX() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/io/github/dsheirer/gui/playlist/channel/TetraConfig.fxml"));
                Parent root = loader.load();
                mController = loader.getController();
                if (mController != null) {
                    mController.setChannel(mChannel);
                }
                Scene scene = new Scene(root);
                scene.getStylesheets().add(getClass().getResource("/io/github/dsheirer/gui/playlist/channel/TetraConfig.css").toExternalForm());
                setScene(scene);
            } catch (Exception e) {
                mLog.error("Error loading TETRA Config FXML", e);
            }
        });
    }
}
