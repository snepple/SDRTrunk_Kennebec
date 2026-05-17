package io.github.dsheirer.controller;

import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.Node;

import java.util.HashMap;
import java.util.Map;

public class ControllerPanelController {

    @FXML
    private BorderPane rootPane;

    @FXML
    private StackPane cardPane;

    @FXML
    private StackPane resourcePaneContainer;

    private Map<String, Node> views = new HashMap<>();

    @FXML
    public void initialize() {
        // Initialization if needed
    }

    public void addView(String id, Node view) {
        view.setVisible(false);
        cardPane.getChildren().add(view);
        views.put(id, view);
    }

    public void showView(String id) {
        for (Map.Entry<String, Node> entry : views.entrySet()) {
            entry.getValue().setVisible(entry.getKey().equals(id));
        }
    }

    public void setResourceNode(Node resourceNode) {
        resourcePaneContainer.getChildren().clear();
        if (resourceNode != null) {
            resourcePaneContainer.getChildren().add(resourceNode);
        }
    }

    public void setResourcePanelVisible(boolean visible) {
        resourcePaneContainer.setVisible(visible);
        resourcePaneContainer.setManaged(visible);
    }
}
