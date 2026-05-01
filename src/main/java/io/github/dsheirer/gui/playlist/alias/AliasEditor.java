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
import javafx.scene.control.SplitPane;
import javafx.scene.control.ListView;
import javafx.scene.layout.StackPane;
import javafx.geometry.Orientation;
import javafx.scene.layout.VBox;
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

    private BooleanProperty mIdentifierTabSelected = new SimpleBooleanProperty(false);
    private SplitPane mSplitPane;
    private ListView<String> mSidebarList;
    private StackPane mContentPane;

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

        mSplitPane = new SplitPane();
        mSplitPane.setOrientation(Orientation.HORIZONTAL);
        mSplitPane.setDividerPositions(0.2);

        mSidebarList = new ListView<>();
        mSidebarList.getItems().addAll("Alias", "Identifier", "Record");
        mSidebarList.setMinWidth(150);
        mSidebarList.setPrefWidth(200);

        mContentPane = new StackPane();
        mContentPane.setPadding(new Insets(0, 0, 0, 10));

        mSplitPane.getItems().addAll(mSidebarList, mContentPane);

        mSidebarList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                switch (newValue) {
                    case "Alias":
                        showAliasEditor();
                        break;
                    case "Identifier":
                        showIdentifierEditor();
                        break;
                    case "Record":
                        showRecordingEditor();
                        break;
                }
            }
        });

        setCenter(mSplitPane);

        mSidebarList.getSelectionModel().select("Alias");
    }

    private void showAliasEditor() {
        mIdentifierTabSelected.set(false);
        mContentPane.getChildren().setAll(getAliasConfigurationEditor());
    }

    private void showIdentifierEditor() {
        mIdentifierTabSelected.set(true);
        mContentPane.getChildren().setAll(getAliasViewByIdentifierEditor());
    }

    private void showRecordingEditor() {
        mIdentifierTabSelected.set(false);
        mContentPane.getChildren().setAll(getAliasRecordingEditor());
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
                mSidebarList.getSelectionModel().select("Alias");
                getAliasConfigurationEditor().show(alias);
            }
        }
        else if(aliasTabRequest instanceof ViewAliasIdentifierRequest)
        {
            AliasID aliasID = ((ViewAliasIdentifierRequest)aliasTabRequest).getAliasId();

            if(aliasID != null)
            {
                mSidebarList.getSelectionModel().select("Identifier");
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
