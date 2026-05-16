package io.github.dsheirer.gui;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import java.awt.Dimension;

public class SidebarPanelWrapper extends JFXPanel {
    public javafx.scene.Node getNode() { return sidebarPanel; }
    private SidebarPanel sidebarPanel;

    public SidebarPanelWrapper(SidebarPanel.SidebarListener listener) {
        setPreferredSize(new Dimension(250, 0));
        sidebarPanel = new SidebarPanel(listener);
        Platform.runLater(() -> {
            Scene scene = new Scene(sidebarPanel);
            setScene(scene);
        });
    }

    public void setActive(String id) {
        sidebarPanel.setActive(id);
    }
}
