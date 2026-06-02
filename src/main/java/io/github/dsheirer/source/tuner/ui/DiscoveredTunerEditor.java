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

package io.github.dsheirer.source.tuner.ui;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;
import javafx.scene.control.ScrollPane;


import io.github.dsheirer.gui.editor.Editor;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.TunerFactory;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.IDiscoveredTunerStatusListener;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.manager.TunerStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;


/**
 * Editor for Discovered Tuners that displays custom tuner editor for the selected discovered tuner
 */
public class DiscoveredTunerEditor extends Editor<DiscoveredTuner> implements IDiscoveredTunerStatusListener
{
    private static final long serialVersionUID = 1L;
    private static final Logger mLog = LoggerFactory.getLogger(DiscoveredTunerEditor.class);
    private UserPreferences mUserPreferences;
    private TunerManager mTunerManager;
    private javafx.scene.layout.Region mEmptyEditor = new EmptyTunerEditor();
    private javafx.scene.layout.Region mEditor = mEmptyEditor;
    private javafx.scene.control.ScrollPane mEditorScroller;

    /**
     * Constructs an instance
     * @param userPreferences to use for starting wideband recordings sourced by a tuner
     * @param tunerManager for creating tuner editors
     */
    public DiscoveredTunerEditor(UserPreferences userPreferences, TunerManager tunerManager)
    {
        mUserPreferences = userPreferences;
        mTunerManager = tunerManager;
        init();
    }

    /**
     * Updates the lock state of the tuner editor so that lockable controls are updated
     * @param locked true if the tuner is locked and the controls should be locked.
     */
    public void setTunerLockState(boolean locked)
    {
        if(mEditor instanceof TunerEditor tunerEditor)
        {
            tunerEditor.setTunerLockState(locked);
        }
    }

    public void init()
    {
        
        mEditorScroller = new javafx.scene.control.ScrollPane(mEditor);
        getChildren().add(mEditorScroller);
    }

    @Override
    public void save()
    {
        //Unused
    }

    @Override
    public void setItem(DiscoveredTuner tuner)
    {
        if(hasItem())
        {
            getItem().removeTunerStatusListener(this);
        }

        //Cleanup previously selected tuner
        if(hasItem() && mEditor instanceof TunerEditor tunerEditor)
        {
            tunerEditor.dispose();
        }

        super.setItem(tuner);

        mEditorScroller.setContent(null);

        if(hasItem())
        {
            mEditor = TunerFactory.getEditor(mUserPreferences, getItem(), mTunerManager);
            getItem().addTunerStatusListener(this);
        }
        else
        {
            mEditor = mEmptyEditor;
        }

        getChildren().remove(mEditorScroller);
        mEditorScroller = new javafx.scene.control.ScrollPane(mEditor);
        getChildren().add(mEditorScroller);
        requestLayout();
        requestLayout();
    }

    @Override
    public void tunerStatusUpdated(DiscoveredTuner discoveredTuner, TunerStatus previous, TunerStatus current)
    {
        //If this is the currently displayed tuner, set it again to re-render the editor
        if(hasItem() && getItem().equals(discoveredTuner))
        {
            if(current == TunerStatus.REMOVED)
            {
                setItem(null);
            }
            else
            {
                setItem(discoveredTuner);
            }
        }
    }
}