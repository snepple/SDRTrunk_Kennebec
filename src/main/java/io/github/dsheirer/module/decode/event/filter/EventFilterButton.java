/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.module.decode.event.filter;

import io.github.dsheirer.filter.FilterEditorPanel;
import io.github.dsheirer.filter.FilterSet;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.stage.Stage;

/**
 * Event filter button that includes split button functionality to allow user to select filter items.
 *
 * @param <T> type of filter.
 */
public class EventFilterButton<T> extends Button
{
    /**
     * Constructs an instance
     * @param dialogTitle for the dialog/panel that appears
     * @param filterSet to include in the editor panel.
     */
    public EventFilterButton(String dialogTitle, FilterSet<T> filterSet)
    {
        this("Filter", dialogTitle, filterSet);
    }

    /**
     * Constructs an instance
     * @param buttonLabel to use on the button
     * @param dialogTitle for the dialog/panel that appears
     * @param filterSet to include in the editor panel.
     */
    public EventFilterButton(String buttonLabel, String dialogTitle, FilterSet<T> filterSet)
    {
        super(buttonLabel);
        setOnAction(e -> {
            FilterEditorPanel<T> panel = new FilterEditorPanel<T>(filterSet);
            ScrollPane scroller = new ScrollPane(panel);
            scroller.setFitToWidth(true);
            scroller.setFitToHeight(true);
            VBox.setVgrow(scroller, Priority.ALWAYS);

            Button close = new Button("Close");
            close.setTooltip(new javafx.scene.control.Tooltip("Close the filter editor"));

            Platform.runLater(() -> {
                Stage stage = new Stage();
                stage.setTitle(dialogTitle);
                stage.setWidth(600);
                stage.setHeight(400);

                close.setOnAction(e1 -> stage.close());

                VBox vbox = new VBox(10, scroller, close);
                vbox.setPadding(new Insets(10));
                vbox.setAlignment(Pos.CENTER_RIGHT);

                Scene scene = new Scene(vbox);
                stage.setScene(scene);
                stage.show();
            });
        });
    }
}
