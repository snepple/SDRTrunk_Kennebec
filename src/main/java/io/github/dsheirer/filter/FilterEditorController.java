package io.github.dsheirer.filter;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class FilterEditorController<T> {

    @FXML
    private VBox mContentBox;

    @FXML
    private Button mCloseButton;

    private Stage mStage;
    private FilterEditorPanel<T> mPanel;

    public void setStage(Stage stage) {
        mStage = stage;
    }

    public void setFilterSet(FilterSet<T> filterSet) {
        if (mPanel == null) {
            mPanel = new FilterEditorPanel<>(filterSet);
            mContentBox.getChildren().add(mPanel);
        } else {
            mPanel.updateFilterSet(filterSet);
        }
    }

    @FXML
    private void handleClose() {
        if (mStage != null) {
            mStage.close();
        }
    }
}
