package io.github.dsheirer.filter;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.stage.Stage;

public class FilterEditorController<T> {

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private Button closeButton;

    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setPanel(FilterEditorPanel<T> panel) {
        scrollPane.setContent(panel);
    }

    @FXML
    private void handleClose() {
        if (stage != null) {
            stage.close();
        }
    }
}
