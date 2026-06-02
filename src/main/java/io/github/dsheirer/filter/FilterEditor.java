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

package io.github.dsheirer.filter;


import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import javafx.scene.control.ScrollPane;

/**
 * Filter editor
 * @param <T> item type for editing
 */
public class FilterEditor<T> extends Stage
{
    private FilterEditorPanel<T> mEditorPanel;

    /**
     * Constructor
     * @param title for the editor window frame
     * @param owner to register the popup location
     * @param filterSet to use initially
     */
    public FilterEditor(String title, FilterSet<T> filterSet)
    {
        if(filterSet == null)
        {
            throw new IllegalArgumentException("Unable to construct FilterEditor - FilterSet cannot be null");
        }
        setTitle(title);
        setWidth(600);
        setHeight(400);

        mEditorPanel = new FilterEditorPanel<>(filterSet);

        ScrollPane scroller = new ScrollPane(mEditorPanel);
        scroller.setFitToWidth(true);
        scroller.setFitToHeight(true);

        Button close = new Button("Close");
        close.setOnAction(e -> close());

        VBox vbox = new VBox(10, scroller, close);
        vbox.setPadding(new Insets(10));
        vbox.setAlignment(Pos.CENTER_RIGHT);
        VBox.setVgrow(scroller, Priority.ALWAYS);

        Scene scene = new Scene(vbox);
        setScene(scene);
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

