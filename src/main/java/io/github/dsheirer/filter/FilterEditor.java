package io.github.dsheirer.filter;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.LoggerFactory;

/**
 * Filter editor
 * @param <T> item type for editing
 */
public class FilterEditor<T> extends Stage
{
    private FilterEditorPanel<T> mEditorPanel;
    private FilterEditorController<T> mController;

    /**
     * Constructor
     * @param title for the editor window frame
     * @param filterSet to use initially
     */
    public FilterEditor(String title, FilterSet<T> filterSet)
    {
        if(filterSet == null)
        {
            throw new IllegalArgumentException("Unable to construct FilterEditor - FilterSet cannot be null");
        }
        setTitle(title);

        mEditorPanel = new FilterEditorPanel<>(filterSet);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/filter/FilterEditor.fxml"));
            Parent root = loader.load();
            mController = loader.getController();
            mController.setStage(this);
            mController.setPanel(mEditorPanel);

            Scene scene = new Scene(root, 600, 400);
            setScene(scene);
        } catch (Exception e) {
            LoggerFactory.getLogger(FilterEditor.class).error("Error loading FilterEditor FXML", e);
        }
    }

    /**
     * Updates this editor with the filterset.
     * @param filterSet to use in this editor.
     */
    public void updateFilterSet(FilterSet<T> filterSet)
    {
        mEditorPanel.updateFilterSet(filterSet);
    }
}
