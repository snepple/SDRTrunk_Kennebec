package io.github.dsheirer.filter;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class FilterEditorPanel<T> extends VBox {

    private final static Logger mLog = LoggerFactory.getLogger(FilterEditorPanel.class);
    private FilterEditorPanelController<T> mController;

    public FilterEditorPanel(FilterSet<T> filterSet) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/filter/FilterEditorPanel.fxml"));
            VBox root = loader.load();
            mController = loader.getController();
            mController.setFilterSet(filterSet);

            this.getChildren().add(root);
            javafx.scene.layout.VBox.setVgrow(root, javafx.scene.layout.Priority.ALWAYS);
            this.setFillWidth(true);
        } catch (IOException e) {
            mLog.error("Error loading FilterEditorPanel FXML", e);
        }
    }

    public void updateFilterSet(FilterSet<T> filterSet) {
        if (mController != null) {
            mController.setFilterSet(filterSet);
        }
    }
}
