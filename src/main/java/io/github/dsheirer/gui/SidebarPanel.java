package io.github.dsheirer.gui;

import io.github.dsheirer.gui.sidebar.SidebarController;
import javafx.application.Platform;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;
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
            controller = loader.getController();
            controller.setListener(listener);
            this.getChildren().add(root);
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
