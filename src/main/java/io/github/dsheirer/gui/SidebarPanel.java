package io.github.dsheirer.gui;

import javafx.application.Platform;
import javafx.scene.layout.VBox;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;
import io.github.dsheirer.gui.sidebar.SidebarController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SidebarPanel extends VBox {
    private static final Logger mLog = LoggerFactory.getLogger(SidebarPanel.class);
    private SidebarController controller;

    public interface SidebarListener {
        void onItemSelected(String id);
        void onActionRequested(String actionId);
    }

    public SidebarPanel(SidebarListener listener) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Sidebar.fxml"));
            VBox root = loader.load();
            this.getChildren().add(root);
            javafx.scene.layout.VBox.setVgrow(root, javafx.scene.layout.Priority.ALWAYS);
            controller = loader.getController();
            controller.setListener(listener);
        } catch (IOException e) {
            mLog.error("Error loading Sidebar.fxml", e);
        }
    }

    public void setActive(String id) {
        Platform.runLater(() -> {
            if (controller != null) {
                controller.setActive(id);
            }
        });
    }
}
