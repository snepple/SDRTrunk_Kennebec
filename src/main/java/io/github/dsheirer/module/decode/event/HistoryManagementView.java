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

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import java.util.function.Consumer;

/**
 * JavaFX View for the History Management Panel.
 */
public class HistoryManagementView {
    private HBox root;
    private Button filterButton;
    private Button clearButton;
    private Slider historySlider;
    private Label historyValueLabel;

    public HistoryManagementView(int initialHistorySize, Runnable onFilterClick, Runnable onClearClick, Consumer<Integer> onHistorySizeChanged) {
        root = new HBox(12);
        root.setPadding(new Insets(6, 12, 6, 12));
        root.setAlignment(Pos.CENTER_LEFT);

        filterButton = new Button("Filters");
        filterButton.setTooltip(new Tooltip("Edit filters"));
        filterButton.setOnAction(e -> onFilterClick.run());

        clearButton = new Button("Clear");
        clearButton.setTooltip(new Tooltip("Clears the history"));
        clearButton.setOnAction(e -> onClearClick.run());

        Label historyTitleLabel = new Label("History:");

        historySlider = new Slider(0, 2000, initialHistorySize);
        historySlider.setBlockIncrement(25);
        historySlider.setMajorTickUnit(500);
        historySlider.setShowTickMarks(false);
        historySlider.setShowTickLabels(false);
        historySlider.setTooltip(new Tooltip("Adjust history size. Double-click to reset to default 200"));

        historySlider.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                historySlider.setValue(ClearableHistoryModel.DEFAULT_HISTORY_SIZE);
            }
        });

        historyValueLabel = new Label(String.valueOf(initialHistorySize));

        historySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int size = newVal.intValue();
            historyValueLabel.setText(String.valueOf(size));
            onHistorySizeChanged.accept(size);
        });

        root.getChildren().addAll(filterButton, clearButton, historyTitleLabel, historySlider, historyValueLabel);
    }

    public HBox getRoot() {
        return root;
    }

    public void setDisable(boolean disable) {
        root.setDisable(disable);
    }
}
