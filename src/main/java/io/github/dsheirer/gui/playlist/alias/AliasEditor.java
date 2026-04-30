/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.gui.playlist.alias;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.UserPreferences;
import javafx.geometry.Insets;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * Primary alias editor with tabbed panes for view-by alias editing support
 */
public class AliasEditor extends BorderPane
{
    private PlaylistManager mPlaylistManager;
    private UserPreferences mUserPreferences;
    private AliasConfigurationEditor mAliasConfigurationEditor;
    private AliasViewByIdentifierEditor mAliasViewByIdentifierEditor;
    private AliasViewByRecordingEditor mAliasRecordingEditor;

    private ToggleButton mAliasButton;
    private ToggleButton mIdentifierButton;
    private ToggleButton mRecordButton;
    private BooleanProperty mIdentifierTabSelected = new SimpleBooleanProperty(false);

    /**
     * Constructs an instance
     * @param playlistManager for alias model access
     * @param userPreferences for settings
     */
    public AliasEditor(PlaylistManager playlistManager, UserPreferences userPreferences)
    {
        mPlaylistManager = playlistManager;
        mUserPreferences = userPreferences;

        setPadding(new Insets(4,0,0,0));

        HBox topToolbar = new HBox(10);
        topToolbar.setAlignment(Pos.CENTER);
        topToolbar.setPadding(new Insets(8));
        topToolbar.setStyle("-fx-background-color: #F2F2F7; -fx-border-color: #E5E5EA; -fx-border-width: 0 0 1 0;");

        Label viewByLabel = new Label("View By:");
        viewByLabel.setStyle("-fx-text-fill: #8E8E93; -fx-font-weight: bold;");

        ToggleGroup group = new ToggleGroup();

        mAliasButton = new ToggleButton("Alias");
        mAliasButton.setToggleGroup(group);
        mAliasButton.setSelected(true);
        mAliasButton.setOnAction(e -> showAliasEditor());

        mIdentifierButton = new ToggleButton("Identifier");
        mIdentifierButton.setToggleGroup(group);
        mIdentifierButton.setOnAction(e -> showIdentifierEditor());

        mRecordButton = new ToggleButton("Record");
        mRecordButton.setToggleGroup(group);
        mRecordButton.setOnAction(e -> showRecordingEditor());

        // HIG Segmented Control styling (simplified)
        String segmentStyle = "-fx-background-radius: 4; -fx-padding: 4 12 4 12;";
        mAliasButton.setStyle(segmentStyle);
        mIdentifierButton.setStyle(segmentStyle);
        mRecordButton.setStyle(segmentStyle);

        HBox segmentedControl = new HBox(mAliasButton, mIdentifierButton, mRecordButton);

        topToolbar.getChildren().addAll(viewByLabel, segmentedControl);

        setTop(topToolbar);
        showAliasEditor();
    }

    private void showAliasEditor() {
        mIdentifierTabSelected.set(false);
        setCenter(getAliasConfigurationEditor());
    }

    private void showIdentifierEditor() {
        mIdentifierTabSelected.set(true);
        setCenter(getAliasViewByIdentifierEditor());
    }

    private void showRecordingEditor() {
        mIdentifierTabSelected.set(false);
        setCenter(getAliasRecordingEditor());
    }

    /**
     * Processes the alias view request.
     *
     * Note: this method must be invoked on the JavaFX platform thread
     *
     * @param aliasTabRequest to process
     */
    public void process(AliasTabRequest aliasTabRequest)
    {
        if(aliasTabRequest instanceof ViewAliasRequest)
        {
            Alias alias = ((ViewAliasRequest)aliasTabRequest).getAlias();

            if(alias != null)
            {
                mAliasButton.setSelected(true);
                showAliasEditor();
                getAliasConfigurationEditor().show(alias);
            }
        }
        else if(aliasTabRequest instanceof ViewAliasIdentifierRequest)
        {
            AliasID aliasID = ((ViewAliasIdentifierRequest)aliasTabRequest).getAliasId();

            if(aliasID != null)
            {
                mIdentifierButton.setSelected(true);
                showIdentifierEditor();
                getAliasViewByIdentifierEditor().show(aliasID);
            }
        }
    }

    private AliasConfigurationEditor getAliasConfigurationEditor()
    {
        if(mAliasConfigurationEditor == null)
        {
            mAliasConfigurationEditor = new AliasConfigurationEditor(mPlaylistManager, mUserPreferences);
        }

        return mAliasConfigurationEditor;
    }

    private AliasViewByIdentifierEditor getAliasViewByIdentifierEditor()
    {
        if(mAliasViewByIdentifierEditor == null)
        {
            mAliasViewByIdentifierEditor = new AliasViewByIdentifierEditor(mPlaylistManager, mIdentifierTabSelected);
        }

        return mAliasViewByIdentifierEditor;
    }

    private AliasViewByRecordingEditor getAliasRecordingEditor()
    {
        if(mAliasRecordingEditor == null)
        {
            mAliasRecordingEditor = new AliasViewByRecordingEditor(mPlaylistManager);
        }

        return mAliasRecordingEditor;
    }
}
