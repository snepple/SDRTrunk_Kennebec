package io.github.dsheirer.gui.widget;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

public class WidgetController {

    @FXML
    public VBox rootBox;

    @FXML
    public HBox headerBox;

    @FXML
    public Label titleLabel;

    @FXML
    public Button minimizeButton;

    @FXML
    public Button closeButton;

    @FXML
    public VBox contentContainer;

    @FXML
    public Pane resizeHandle;

    @FXML
    public void initialize() {
        // Initialization handled in the custom Widget class
    }
}
