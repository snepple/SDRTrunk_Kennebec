

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
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;
import javafx.stage.Stage;

import javafx.scene.control.ScrollPane;

import javafx.scene.control.Button;
import javafx.scene.control.Label;

import io.github.dsheirer.filter.FilterEditorPanel;
import io.github.dsheirer.filter.FilterSet;
import javafx.application.Platform;
import javafx.event.ActionEvent;





/**
 * Event filter button that includes split button functionality to allow user to select filter items.
 *
 * @param <T> type of filter.
 */
public class EventFilterButton<T> extends javafx.scene.control.Button
{
    private static final long serialVersionUID = 1L;

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
        setOnAction(new EventFilterActionHandler(dialogTitle, filterSet));
    }

    /**
     * Action handler for the button
     */
    public class EventFilterActionHandler implements javafx.event.EventHandler<javafx.event.ActionEvent> {
        private String mTitle;
        private FilterSet<T> mFilterSet;

        /**
         * Constructs an instance
         * @param title for this panel
         * @param filterSet to edit
         */
        public EventFilterActionHandler(String title, FilterSet<T> filterSet)
        {
            mTitle = title;
            mFilterSet = filterSet;
        }

// //         @Override
        public void handle(javafx.event.ActionEvent e)
        {
            final Stage editor = new Stage();
            editor.setTitle(mTitle);
            // editor.setLocationRelativeTo(EventFilterButton.this);
            // editor.setSize(600, 400);
            // editor.setDefaultCloseOperation(Stage.DISPOSE_ON_CLOSE);
            // editor.setLayout(new javafx.scene.layout.HBox(4));
            FilterEditorPanel<T> panel = new FilterEditorPanel<T>(mFilterSet);
            ScrollPane scroller = new ScrollPane(panel);
            // scroller.setViewportView(panel);
            // editor.getChildren().add(scroller, "wrap");
            Button close = new Button("Close");
            close.setTooltip(new javafx.scene.control.Tooltip("Close the filter editor"));
            close.accessibleTextProperty().set("Close Event Filter Editor");
            close.accessibleHelpProperty().set("Closes the event filter editor dialog");
            close.setOnAction(e1 -> editor.close());
            // editor.getChildren().add(close);
            Platform.runLater(() -> editor.show());
        }
    }
}