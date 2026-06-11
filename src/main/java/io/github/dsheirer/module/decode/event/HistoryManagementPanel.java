
/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.module.decode.event;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;
import javafx.scene.layout.VBox;

import io.github.dsheirer.filter.FilterEditor;
import io.github.dsheirer.filter.FilterSet;
import javafx.application.Platform;
import javafx.scene.layout.Pane;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import org.slf4j.LoggerFactory;




import java.awt.Dimension;
import java.util.function.Consumer;

/**
 * History management panel with controls for managing item histories.
 */
public class HistoryManagementPanel<T> extends javafx.scene.layout.StackPane
{
    private ClearableHistoryModel mModel;
    private FilterSet<T> mFilterSet;
    private FilterEditor<T> mFilterEditor;
    private String mFilterEditorTitle;

    /**
     * Optional callback invoked (on the slider's change listener) each time
     * the user moves the slider, so the caller can persist the new value.
     */
    private Consumer<Integer> mHistorySizeChangedCallback;

    private HistoryManagementPanelController mController;

    // For Swing interoperability when showing the filter editor
    private VBox mDummyAnchor;

    /**
     * Constructs an instance using the model's current history size as the
     * initial slider position.  No persistence callback is installed.
     *
     * @param model             to manage
     * @param filterEditorTitle title for the filter editor dialog
     */
    public HistoryManagementPanel(ClearableHistoryModel model, String filterEditorTitle)
    {
        this(model, filterEditorTitle, model.getHistorySize(), null);
    }

    /**
     * Constructs an instance with a caller-supplied initial slider value and
     * an optional callback for persistence.
     *
     * @param model                      to manage
     * @param filterEditorTitle          title for the filter editor dialog
     * @param initialHistorySize         slider starting position (from saved prefs)
     * @param historySizeChangedCallback called with the new value every time
     *                                   the slider moves; may be {@code null}
     */
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

        mDummyAnchor = new VBox();
        // Since we are now a javafx.scene.layout.Pane, we don't add the dummy anchor directly.
        // It's just used as a component reference for the FilterEditor dialog location.

        setPrefSize(300, 38);

        Platform.runLater(this::initJavaFX);

        setEnabled(false);
    }

    private void initJavaFX() {
        HBox root = new HBox(8);
        root.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        root.setPadding(new javafx.geometry.Insets(4, 10, 4, 10));
        root.setStyle("-fx-background-color: #F9F9FB; -fx-border-color: transparent transparent #E5E5EA transparent; -fx-border-width: 0 0 1 0;");
        
        Button filterButton = new Button("Filter");
        filterButton.getStyleClass().add("flat-button");
        filterButton.setOnAction(e -> handleFilterClick());
        
        Button clearButton = new Button("Clear");
        clearButton.getStyleClass().add("flat-button");
        clearButton.setOnAction(e -> handleClearClick());
        
        Label historyLabel = new Label("Max Events:");
        historyLabel.setStyle("-fx-text-fill: #48484A; -fx-font-weight: bold; -fx-font-size: 12px;");
        historyLabel.setTooltip(new javafx.scene.control.Tooltip("Maximum number of events to retain in the history buffer"));
        
        Slider historySlider = new Slider(0, 5000, mModel.getHistorySize());
        historySlider.setShowTickMarks(true);
        historySlider.setShowTickLabels(true);
        historySlider.setMajorTickUnit(1000);
        historySlider.setMinorTickCount(4);
        historySlider.setBlockIncrement(100);
        historySlider.setPrefWidth(200);
        
        Label historyValueLabel = new Label(String.valueOf(mModel.getHistorySize()));
        historyValueLabel.setPrefWidth(50);
        historyValueLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        
        historySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int size = newVal.intValue();
            historyValueLabel.setText(String.valueOf(size));
            handleHistorySizeChanged(size);
        });
        
        historySlider.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                historySlider.setValue(ClearableHistoryModel.DEFAULT_HISTORY_SIZE);
            }
        });
        
        root.getChildren().addAll(filterButton, clearButton, historyLabel, historySlider, historyValueLabel);
        
        getChildren().add(root);
    }

    private void handleFilterClick() {
        Platform.runLater(() -> javafx.application.Platform.runLater(() -> getFilterEditor().show()));
    }

    private void handleClearClick() {
        Platform.runLater(() -> mModel.clear());
    }

    private void handleHistorySizeChanged(int size) {
        Platform.runLater(() -> {
            mModel.setHistorySize(size);
            if (mHistorySizeChangedCallback != null) {
                mHistorySizeChangedCallback.accept(size);
            }
        });
    }

    /**
     * Updates the filter set
     * @param filterSet to use
     */
    public void updateFilterSet(FilterSet<T> filterSet)
    {
        mFilterSet = filterSet;

        if(mFilterEditor != null)
        {
            mFilterEditor.updateFilterSet(filterSet);
        }
    }

    /**
     * Overrides the panel method to also set the enabled state for the child controls.
     * @param enabled true if this component should be enabled, false otherwise
     */
    // @Override
    public void setEnabled(boolean enabled)
    {
        super.setDisable(!enabled);
    }

    /**
     * Filter editor
     * @return editor lazily constructed
     */
    private FilterEditor<T> getFilterEditor()
    {
        if(mFilterEditor == null)
        {
            mFilterEditor = new FilterEditor(mFilterEditorTitle, mFilterSet);
        }

        return mFilterEditor;
    }
}

