package io.github.dsheirer.gui;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import io.github.dsheirer.gui.sidebar.SidebarController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.io.IOException;

public class SidebarPanel extends JFXPanel {
    private static final Logger mLog = LoggerFactory.getLogger(SidebarPanel.class);
    private SidebarController controller;

    public interface SidebarListener {
        void onItemSelected(String id);
        void onActionRequested(String actionId);
    }

    public SidebarPanel(SidebarListener listener) {
        setPreferredSize(new Dimension(250, 0));

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
