package io.github.dsheirer.module.decode.event;

import io.github.dsheirer.filter.FilterEditor;
import io.github.dsheirer.filter.FilterSet;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import org.slf4j.LoggerFactory;
import java.util.function.Consumer;

public class HistoryManagementPanel<T> extends javafx.scene.layout.VBox
{
    private ClearableHistoryModel mModel;
    private FilterSet<T> mFilterSet;
    private FilterEditor<T> mFilterEditor;
    private String mFilterEditorTitle;
    private Consumer<Integer> mHistorySizeChangedCallback;
    private HistoryManagementPanelController mController;

    public HistoryManagementPanel(ClearableHistoryModel model, String filterEditorTitle)
    {
        this(model, filterEditorTitle, model.getHistorySize(), null);
    }

    public HistoryManagementPanel(ClearableHistoryModel model, String filterEditorTitle,
                                  int initialHistorySize, Consumer<Integer> historySizeChangedCallback)
    {
        mModel = model;
        mFilterEditorTitle = filterEditorTitle;
        mHistorySizeChangedCallback = historySizeChangedCallback;

        if(initialHistorySize != model.getHistorySize())
        {
            model.setHistorySize(initialHistorySize);
        }

        setPrefSize(300, 38);
        Platform.runLater(this::initJavaFX);
        setEnabled(false);
    }

    private void initJavaFX() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/module/decode/event/HistoryManagementPanel.fxml"));
            Parent root = loader.load();

            mController = loader.getController();
            mController.setCallbacks(this::handleFilterClick, this::handleClearClick, this::handleHistorySizeChanged);
            mController.setInitialHistorySize(mModel.getHistorySize());
            mController.setDisable(isDisable());

            getChildren().add(root);
        } catch (Exception e) {
            LoggerFactory.getLogger(HistoryManagementPanel.class).error("Error loading HistoryManagementPanel FXML", e);
        }
    }

    private void handleFilterClick() {
        Platform.runLater(() -> getFilterEditor().show());
    }

    private void handleClearClick() {
        mModel.clear();
    }

    private void handleHistorySizeChanged(int size) {
        mModel.setHistorySize(size);
        if (mHistorySizeChangedCallback != null) {
            mHistorySizeChangedCallback.accept(size);
        }
    }

    public void updateFilterSet(FilterSet<T> filterSet)
    {
        mFilterSet = filterSet;
        if(mFilterEditor != null)
        {
            mFilterEditor.updateFilterSet(filterSet);
        }
    }

    public void setEnabled(boolean enabled)
    {
        setDisable(!enabled);
        if (mController != null) {
            Platform.runLater(() -> mController.setDisable(!enabled));
        } else {
            Platform.runLater(() -> {
                if (mController != null) {
                    mController.setDisable(!enabled);
                }
            });
        }
    }

    private FilterEditor<T> getFilterEditor()
    {
        if(mFilterEditor == null)
        {
            mFilterEditor = new FilterEditor<>(mFilterEditorTitle, null, mFilterSet);
        }
        return mFilterEditor;
    }
}
