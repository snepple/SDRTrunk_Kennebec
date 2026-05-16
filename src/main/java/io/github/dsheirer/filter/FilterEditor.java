package io.github.dsheirer.filter;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class FilterEditor<T> extends Stage {

    private final static Logger mLog = LoggerFactory.getLogger(FilterEditor.class);
    private FilterEditorController<T> mController;

    public FilterEditor(String title, Object unusedOwnerOrAnchor, FilterSet<T> filterSet) {
        if (filterSet == null) {
            throw new IllegalArgumentException("Unable to construct FilterEditor - FilterSet cannot be null");
        }
        setTitle(title);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/filter/FilterEditor.fxml"));
            Scene scene = new Scene(loader.load(), 600, 400);
            setScene(scene);
            mController = loader.getController();
            mController.setFilterSet(filterSet);
            mController.setStage(this);
        } catch (IOException e) {
            mLog.error("Error loading FilterEditor FXML", e);
        }
    }

    public void updateFilterSet(FilterSet<T> filterSet) {
        if (mController != null) {
            mController.setFilterSet(filterSet);
        }
    }
}
