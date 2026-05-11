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

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Filter editor
 * @param <T> item type for editing
 */
public class FilterEditor<T> extends Stage
{
    private static final Logger mLog = LoggerFactory.getLogger(FilterEditor.class);
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

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/FilterEditor.fxml"));
            Parent root = loader.load();
            mController = loader.getController();
            mController.setFilterSet(filterSet);

            Scene scene = new Scene(root, 600, 400);

            java.net.URL cssUrl = getClass().getResource("/sdrtrunk_style.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            setScene(scene);
        } catch (IOException e) {
            mLog.error("Error loading FilterEditor.fxml", e);
        }
    }

    /**
     * Constructor compatible with old signature.
     * @param title for the editor window frame
     * @param owner legacy owner, ignored in JavaFX
     * @param filterSet to use initially
     */
    public FilterEditor(String title, Object owner, FilterSet<T> filterSet)
    {
        this(title, filterSet);
    }

    /**
     * Updates this editor with the filterset.
     * @param filterSet to use in this editor.
     */
    public void updateFilterSet(FilterSet<T> filterSet)
    {
        if (mController != null) {
            mController.setFilterSet(filterSet);
        }
    }
}
