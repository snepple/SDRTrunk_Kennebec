/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.gui.preference.layout;

import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * A standardized bottom-pinned action bar for Save/Cancel/Apply/Delete buttons.
 * Follows Apple HIG guidelines:
 * - Cancel/secondary actions on the left (leading)
 * - Primary action (Save/Apply) on the right (trailing)
 * - Destructive actions (Delete) get red styling
 * - Consistent height, padding, and border styling
 *
 * Usage:
 * <pre>
 *   ActionBar actionBar = new ActionBar();
 *   actionBar.addLeading(cancelButton);       // Cancel goes left
 *   actionBar.addTrailing(saveButton);         // Save goes right
 *   actionBar.addTrailingDestructive(deleteButton); // Delete gets red styling
 *   borderPane.setBottom(actionBar);           // Pin to bottom of layout
 * </pre>
 */
public class ActionBar extends HBox
{
    private final HBox mLeadingBox;
    private final HBox mTrailingBox;

    public ActionBar()
    {
        getStyleClass().add("hig-action-bar");

        mLeadingBox = new HBox(8);
        mTrailingBox = new HBox(8);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        getChildren().addAll(mLeadingBox, spacer, mTrailingBox);
    }

    /**
     * Adds a button to the left (leading) side of the action bar.
     * Typically used for Cancel or secondary actions.
     */
    public void addLeading(Button button)
    {
        button.getStyleClass().add("flat-button");
        mLeadingBox.getChildren().add(button);
    }

    /**
     * Adds a button to the right (trailing) side of the action bar.
     * Typically used for Save/Apply (primary actions).
     */
    public void addTrailing(Button button)
    {
        button.getStyleClass().add("hig-primary-action");
        mTrailingBox.getChildren().add(button);
    }

    /**
     * Adds a destructive action button (e.g., Delete) to the right side
     * with red destructive styling.
     */
    public void addTrailingDestructive(Button button)
    {
        button.getStyleClass().add("hig-destructive-action");
        mTrailingBox.getChildren().add(button);
    }
}
