package io.github.dsheirer.gui.sidebar;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import io.github.dsheirer.gui.SidebarPanel.SidebarListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SidebarJFXPanel extends JFXPanel {
    private static final Logger mLog = LoggerFactory.getLogger(SidebarJFXPanel.class);
    private SidebarController controller;

    public SidebarJFXPanel(SidebarListener listener) {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Sidebar.fxml"));
                VBox root = loader.load();
                controller = loader.getController();
                controller.setListener(listener);
                Scene scene = new Scene(root);
                setScene(scene);
            } catch (IOException e) {
                mLog.error("Error loading Sidebar.fxml", e);
            }
        });
    }

    public void setActive(String id) {
        Platform.runLater(() -> {
            if (controller != null) {
                controller.setActive(id);
            }
        });
    }
}
