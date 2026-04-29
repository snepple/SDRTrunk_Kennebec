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

package io.github.dsheirer.gui.playlist.manager;

import com.google.common.eventbus.Subscribe;
import com.google.common.io.Files;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelException;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.preference.UserPreferences;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Editor for managing playlists via the playlist manager
 */
public class PlaylistManagerEditor extends BorderPane
{
    private static final Logger mLog = LoggerFactory.getLogger(PlaylistManagerEditor.class);

    private static final FileChooser.ExtensionFilter PLAYLIST_FILE_FILTER =
        new FileChooser.ExtensionFilter("Playlist Files (*.xml)", "*.xml");
    private static final FileChooser.ExtensionFilter ALL_FILES_FILE_FILTER =
        new FileChooser.ExtensionFilter("All Files (*.*)", "*.*");

    private PlaylistManager mPlaylistManager;
    private UserPreferences mUserPreferences;
    private ListView<Path> mPlaylistListView;

    private Button mSelectButton;
    private Button mAddButton;
    private Button mRemoveButton;
    private Button mCloneButton;
    private Button mNewButton;
    private Button mDeleteButton;

    /**
     * Constructs an instance
     * @param playlistManager for managing playlist
     * @param userPreferences for accessing preferences
     */
    public PlaylistManagerEditor(PlaylistManager playlistManager, UserPreferences userPreferences)
    {
        mPlaylistManager = playlistManager;
        mUserPreferences = userPreferences;

        //Register to receive preferences updates
        MyEventBus.getGlobalEventBus().register(this);

        setPadding(new Insets(16));
        setCenter(getPlaylistListView());
        setBottom(getActionBar());
        updateButtons();
    }

    public void dispose()
    {
        MyEventBus.getGlobalEventBus().unregister(this);
    }

    /**
     * Indicates if the path argument is the same as the current playlist path
     */
    private boolean isCurrent(Path path)
    {
        Path current = mUserPreferences.getPlaylistPreference().getPlaylist();
        return path != null && current != null && path.equals(current);
    }

    /**
     * Saves the current list of playlists to the user preferences.
     */
    private void savePlaylistsPreference()
    {
        mUserPreferences.getPlaylistPreference().setPlaylistList(getPlaylistListView().getItems());
    }

    private ListView<Path> getPlaylistListView()
    {
        if(mPlaylistListView == null)
        {
            mPlaylistListView = new ListView<>();
            mPlaylistListView.setStyle("-fx-background-insets: 0; -fx-padding: 0;");

            mPlaylistListView.setCellFactory(new Callback<ListView<Path>, ListCell<Path>>() {
                @Override
                public ListCell<Path> call(ListView<Path> param) {
                    return new ListCell<Path>() {
                        private HBox root = new HBox(12);
                        private VBox textContainer = new VBox(2);
                        private Label titleLabel = new Label();
                        private Label pathLabel = new Label();
                        private IconNode statusIcon = new IconNode(FontAwesome.CHECK);

                        {
                            titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
                            pathLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #8E8E93;");
                            statusIcon.setIconSize(16);

                            root.setAlignment(Pos.CENTER_LEFT);
                            root.setPadding(new Insets(8, 12, 8, 12));

                            textContainer.getChildren().addAll(titleLabel, pathLabel);
                            root.getChildren().addAll(statusIcon, textContainer);
                        }

                        @Override
                        protected void updateItem(Path item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty || item == null) {
                                setGraphic(null);
                            } else {
                                titleLabel.setText(item.getFileName().toString());
                                pathLabel.setText(item.getParent() != null ? item.getParent().toString() : "");

                                if (isCurrent(item)) {
                                    statusIcon.setIconCode(FontAwesome.CHECK);
                                    statusIcon.setFill(Color.web("#34C759"));
                                    statusIcon.setVisible(true);
                                } else if (!item.toFile().exists()) {
                                    statusIcon.setIconCode(FontAwesome.TIMES);
                                    statusIcon.setFill(Color.web("#FF3B30"));
                                    statusIcon.setVisible(true);
                                } else {
                                    statusIcon.setVisible(false);
                                }

                                setGraphic(root);
                            }
                        }
                    };
                }
            });

            mPlaylistListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> updateButtons());

            //User double-clicks on an entry - make that entry the selected playlist.
            mPlaylistListView.setOnMouseClicked(event ->
            {
                if(event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2)
                {
                    selectPlayist(getPlaylistListView().getSelectionModel().getSelectedItem());
                }
            });

            List<Path> playlistPaths = mUserPreferences.getPlaylistPreference().getPlaylistList();

            mPlaylistListView.getItems().addAll(playlistPaths);
        }

        return mPlaylistListView;
    }

    private void updateButtons()
    {
        Path selected = getPlaylistListView().getSelectionModel().getSelectedItem();

        boolean itemSelected = (selected != null);
        boolean isCurrent = isCurrent(selected);

        getSelectButton().setDisable(!itemSelected || isCurrent);
        getRemoveButton().setDisable(!itemSelected || isCurrent);
        getCloneButton().setDisable(!itemSelected || (selected != null && !selected.toFile().exists()));
        getDeleteButton().setDisable(!itemSelected || isCurrent || (selected != null && !selected.toFile().exists()));
    }

    private HBox getActionBar()
    {
        HBox actionBar = new HBox(12);
        actionBar.setPadding(new Insets(12, 0, 0, 0));
        actionBar.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        actionBar.getChildren().addAll(
            getSelectButton(),
            spacer,
            getNewButton(),
            getAddButton(),
            getCloneButton(),
            getRemoveButton(),
            getDeleteButton()
        );

        return actionBar;
    }

    /**
     * Selects the specified playlist and makes it the current playlist.
     * @param selected playlist.
     */
    private void selectPlayist(Path selected)
    {
        if(selected != null)
        {
            Path current = mUserPreferences.getPlaylistPreference().getPlaylist();

            try
            {
                mPlaylistManager.setPlaylist(selected);
            }
            catch(IOException ioe)
            {
                mLog.error("Error loading playlist [" + (selected != null ? selected.toString() : "null") + "]");

                new Alert(Alert.AlertType.ERROR, "Unable to load selected playlist.  " +
                        "Reverting to previous playlist", ButtonType.OK).show();

                try
                {
                    mPlaylistManager.setPlaylist(current);
                }
                catch(IOException ioe2)
                {
                    mLog.error("Error reverting to previous playlist [" +
                            (current != null ? current.toString() : "null") + "]");
                }
            }

            final List<Channel> autoStartChannels = mPlaylistManager.getChannelModel().getAutoStartChannels();

            if(autoStartChannels.size() > 0)
            {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                        "Would you like to auto-start your channels?", ButtonType.YES, ButtonType.NO);
                alert.setTitle("Auto-Start Channels");
                alert.setHeaderText("Discovered [" + autoStartChannels.size() + "] auto-start channel" +
                        (autoStartChannels.size() > 1 ? "s" : ""));
                alert.showAndWait().ifPresent(buttonType -> {
                    if(buttonType == ButtonType.YES)
                    {
                        boolean error = false;

                        for(Channel channel: autoStartChannels)
                        {
                            try
                            {
                                mPlaylistManager.getChannelProcessingManager().start(channel);
                            }
                            catch(ChannelException ce)
                            {
                                error = true;
                            }
                        }

                        if(error)
                        {
                            Alert errorAlert = new Alert(Alert.AlertType.ERROR,
                                    "Unable to start some or all of the auto-start channels",
                                    ButtonType.OK);
                            errorAlert.setTitle("Channel Auto-Start Error(s)");
                            errorAlert.setHeaderText("Auto-Start Error");
                            errorAlert.showAndWait();
                        }
                    }
                });
            }
        }
    }

    private Button getSelectButton()
    {
        if(mSelectButton == null)
        {
            mSelectButton = new Button("Select");
            mSelectButton.setTooltip(new Tooltip("Sets the selected playlist as the current playlist"));
            mSelectButton.setMaxWidth(Double.MAX_VALUE);
            mSelectButton.setOnAction(event -> {selectPlayist(getPlaylistListView().getSelectionModel().getSelectedItem());});
        }

        return mSelectButton;
    }

    private Button getAddButton()
    {
        if(mAddButton == null)
        {
            mAddButton = new Button("Add");
            mAddButton.setTooltip(new Tooltip("Add an existing playlist from the file system"));
            mAddButton.setMaxWidth(Double.MAX_VALUE);
            mAddButton.setOnAction(event -> {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Add Playlist");
                fileChooser.setInitialDirectory(mUserPreferences.getDirectoryPreference().getDirectoryPlaylist().toFile());
                fileChooser.getExtensionFilters().addAll(PLAYLIST_FILE_FILTER, ALL_FILES_FILE_FILTER);

                File playlistToAdd = fileChooser.showOpenDialog(null);

                if(playlistToAdd != null)
                {
                    if(PlaylistManager.isPlaylist(playlistToAdd.toPath()))
                    {
                        if(!getPlaylistListView().getItems().contains(playlistToAdd.toPath()))
                        {
                            getPlaylistListView().getItems().add(playlistToAdd.toPath());
                            savePlaylistsPreference();
                        }
                        else
                        {
                            new Alert(Alert.AlertType.INFORMATION, "Playlist already added", ButtonType.OK).show();
                        }
                    }
                    else
                    {
                        new Alert(Alert.AlertType.ERROR, "This file is not a valid playlist",
                            ButtonType.OK).show();
                    }
                }
            });
        }

        return mAddButton;
    }

    private Button getCloneButton()
    {
        if(mCloneButton == null)
        {
            mCloneButton = new Button("Clone");
            mCloneButton.setTooltip(new Tooltip("Create a clone (copy) of the currently selected playlist"));
            mCloneButton.setMaxWidth(Double.MAX_VALUE);
            mCloneButton.setOnAction(event -> {
                Path selected = getPlaylistListView().getSelectionModel().getSelectedItem();

                if(selected != null)
                {
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("Copy Playlist");
                    fileChooser.setInitialDirectory(mUserPreferences.getDirectoryPreference().getDirectoryPlaylist().toFile());
                    fileChooser.setInitialFileName(selected.getName(selected.getNameCount() - 1).toString());
                    fileChooser.getExtensionFilters().addAll(PLAYLIST_FILE_FILTER, ALL_FILES_FILE_FILTER);

                    File copyFile = fileChooser.showSaveDialog(null);

                    if(copyFile != null)
                    {
                        if(!copyFile.toString().endsWith(".xml"))
                        {
                            copyFile = new File(copyFile.toString() + ".xml");

                            if(copyFile.exists())
                            {
                                new Alert(Alert.AlertType.ERROR, "File already exists.  Please copy " +
                                    "to a new file name", ButtonType.OK).show();
                                return;
                            }
                        }

                        try
                        {
                            Files.copy(selected.toFile(), copyFile);
                            getPlaylistListView().getItems().add(copyFile.toPath());
                            savePlaylistsPreference();
                        }
                        catch(IOException ioe)
                        {
                            mLog.error("Error creating copy of playlist [" + selected.toString() + "] as [" + copyFile.toString() + "]", ioe);
                            Alert alert = new Alert(Alert.AlertType.ERROR, "Unable to create copy of playlist", ButtonType.OK);
                            alert.initOwner(((Node)getCloneButton()).getScene().getWindow());
                            alert.show();
                        }
                    }
                }
            });
        }

        return mCloneButton;
    }

    private Button getRemoveButton()
    {
        if(mRemoveButton == null)
        {
            mRemoveButton = new Button("Remove");
            mRemoveButton.setTooltip(new Tooltip("Remove the currently selected playlist from the application"));
            mRemoveButton.setMaxWidth(Double.MAX_VALUE);
            mRemoveButton.setOnAction(event -> {
                Path selected = getPlaylistListView().getSelectionModel().getSelectedItem();

                if(selected != null)
                {
                    getPlaylistListView().getItems().remove(selected);
                    savePlaylistsPreference();
                }
            });
        }

        return mRemoveButton;
    }

    private Button getNewButton()
    {
        if(mNewButton == null)
        {
            mNewButton = new Button("New");
            mNewButton.setTooltip(new Tooltip("Create a new playlist"));
            mNewButton.setMaxWidth(Double.MAX_VALUE);
            mNewButton.setOnAction(event -> {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("New Playlist");
                fileChooser.setInitialDirectory(mUserPreferences.getDirectoryPreference().getDirectoryPlaylist().toFile());
                fileChooser.setInitialFileName("*.xml");
                fileChooser.getExtensionFilters().addAll(PLAYLIST_FILE_FILTER);

                File newFile = fileChooser.showSaveDialog(null);

                if(newFile != null)
                {
                    try
                    {
                        Path toCreate = newFile.toPath();

                        if(!toCreate.toString().endsWith(".xml"))
                        {
                            toCreate = Paths.get(newFile.toString() + ".xml");

                            //Since we modified the file, we have to check for existence to avoid overwriting
                            if(toCreate.toFile().exists())
                            {
                                new Alert(Alert.AlertType.ERROR, "File already exists.  Please choose " +
                                    "a new file name", ButtonType.OK).show();
                                return;
                            }
                        }

                        mPlaylistManager.createEmptyPlaylist(toCreate);
                        getPlaylistListView().getItems().add(toCreate);
                        savePlaylistsPreference();
                    }
                    catch(IOException ioe)
                    {
                        mLog.error("Error creating new playlist file [" + newFile.toString() + "]");
                        new Alert(Alert.AlertType.ERROR, "Unable to create new playlist", ButtonType.OK).show();
                    }
                }
            });
        }

        return mNewButton;
    }

    private Button getDeleteButton()
    {
        if(mDeleteButton == null)
        {
            mDeleteButton = new Button("Delete");
            mDeleteButton.setTooltip(new Tooltip("Remove the currently selected playlist and delete it from the file system"));
            mDeleteButton.setMaxWidth(Double.MAX_VALUE);
            mDeleteButton.setOnAction(event -> {
                Path selected = getPlaylistListView().getSelectionModel().getSelectedItem();

                if(selected != null)
                {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete playlist from file system?",
                        ButtonType.YES, ButtonType.NO);
                    alert.setHeaderText("Are you sure?");
                    alert.initOwner(((Node)getDeleteButton()).getScene().getWindow());
                    Button noButton = (Button)alert.getDialogPane().lookupButton(ButtonType.NO);
                    noButton.setDefaultButton(true);
                    Button yesButton = (Button)alert.getDialogPane().lookupButton(ButtonType.YES);
                    yesButton.setDefaultButton(false);

                    Optional<ButtonType> optional = alert.showAndWait();

                    if(optional.get() == ButtonType.YES)
                    {
                        getPlaylistListView().getItems().remove(selected);
                        savePlaylistsPreference();
                        selected.toFile().delete();
                    }
                }
            });
        }

        return mDeleteButton;
    }

    /**
     * Receives preference update notifications via the event bus for playlist updates.  Use this to queue an update
     * to the playlist list view so that we can reflect changes to the current playlist.
     * @param preferenceType that was updated
     */
    @Subscribe
    public void preferenceUpdated(PreferenceType preferenceType)
    {
        if(preferenceType == PreferenceType.PLAYLIST)
        {
            Platform.runLater(new Runnable()
            {
                @Override
                public void run()
                {
                    getPlaylistListView().refresh();
                }
            });
        }
    }
}
