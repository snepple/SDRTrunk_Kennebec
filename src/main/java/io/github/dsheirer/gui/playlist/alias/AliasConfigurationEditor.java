
/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;


import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasFactory;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.gui.control.MaxLengthUnaryOperator;
import io.github.dsheirer.gui.playlist.Editor;
import io.github.dsheirer.gui.playlist.IAliasListRefreshListener;

import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.UserPreferences;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.control.MenuButton;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableRow;

import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.ColorPicker;

import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import javafx.scene.shape.Rectangle;
import javafx.util.Callback;
import javafx.scene.layout.Region;
import jiconfont.IconCode;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;
import org.controlsfx.control.textfield.TextFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Editor for aliases
 */
public class AliasConfigurationEditor extends VBox implements IAliasListRefreshListener
{
    private static final Logger mLog = LoggerFactory.getLogger(AliasConfigurationEditor.class);

    private PlaylistManager mPlaylistManager;
    private UserPreferences mUserPreferences;
    private AliasItemEditor mAliasItemEditor;
    private AliasBulkEditor mAliasBulkEditor;
    private Editor mCurrentEditor;
    private TableView<Alias> mAliasTableView;
    private Label mPlaceholderLabel;
    private Button mNewAliasButton;
    private Button mDeleteAliasButton;
    private Button mRenameAliasButton;
    private Button mCloneAliasButton;
    private MenuButton mMoveToAliasButton;
    private HBox mButtonBox;
    private VBox mSearchAndListSelectionBox;
    private TextField mSearchField;
    private ComboBox<String> mAliasListNameComboBox;
    private Button mNewAliasListButton;
    private Button mDeleteAliasListButton;
    private FilteredList<Alias> mAliasFilteredList;
    private SortedList<Alias> mAliasSortedList;
    private AliasPredicate mAliasPredicate;

    /**
     * Constructs an instance
     * @param playlistManager for playlist operations
     * @param userPreferences for user preferences
     */
    private SplitPane mSplitPane;

    public AliasConfigurationEditor(PlaylistManager playlistManager, UserPreferences userPreferences)
    {
        mPlaylistManager = playlistManager;
        mPlaylistManager.addAliasListRefreshListener(this);
        mUserPreferences = userPreferences;

        // Top Toolbar
        getChildren().add(getSearchAndListSelectionBox());

        // Split Pane Content
        mSplitPane = new SplitPane();
        mSplitPane.setOrientation(Orientation.VERTICAL);
        mSplitPane.setDividerPositions(0.6); // 60% list, 40% detail
        VBox.setVgrow(mSplitPane, Priority.ALWAYS);

        // Left Pane (List)
        VBox leftBox = new VBox();
        VBox.setVgrow(getAliasTableView(), Priority.ALWAYS);
        leftBox.getChildren().addAll(getAliasTableView());

        mCurrentEditor = getAliasItemEditor();
        mSplitPane.getItems().addAll(leftBox, mCurrentEditor);

        getChildren().add(mSplitPane);
    }

    /**
     * Prepares for an alias list refresh by clearing the currently selected alias item from the editor.
     */
    @Override
    public void prepareForAliasListRefresh()
    {
        getAliasTableView().getSelectionModel().select(null);
    }

    /**
     * Request to show the specified alias in the editor.
     * <p>
     * Note: this must be called on the FX platform thread
     *
     * @param alias to show
     */
    public void show(Alias alias)
    {
        if(alias != null)
        {
            String aliasList = alias.getAliasListName();

            if(aliasList == null || aliasList.isEmpty())
            {
                aliasList = AliasModel.NO_ALIAS_LIST;
            }

            getAliasListNameComboBox().getSelectionModel().select(aliasList);
            getAliasTableView().getSelectionModel().clearSelection();
            getAliasTableView().getSelectionModel().select(alias);
            getAliasTableView().scrollTo(alias);
        }
    }

    /**
     * Sets the editor as the bottom alias editor, either single alias or bulk alias editor.
     */
    private void setEditor(Editor editor)
    {
        if(editor != mCurrentEditor)
        {
            mSplitPane.getItems().remove(mCurrentEditor);
            mCurrentEditor = editor;
            mSplitPane.getItems().add(mCurrentEditor);
        }
    }

    private void setAliases(List<Alias> aliases)
    {
        //Prompt the user to save if the contents of the current channel editor have been modified
        if(getAliasItemEditor().modifiedProperty().get())
        {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION); io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane());
            alert.getButtonTypes().clear();
            alert.getButtonTypes().addAll(ButtonType.NO, ButtonType.YES);
            alert.setTitle("Save Changes");
            alert.setHeaderText("Alias configuration has been modified");
            alert.setContentText("Do you want to save these changes?");
            alert.initOwner((getButtonBox()).getScene().getWindow());

            //Workaround for JavaFX KDE on Linux bug in FX 10/11: https://bugs.openjdk.java.net/browse/JDK-8179073
            alert.setResizable(true);
            alert.onShownProperty().addListener(e -> {
                Platform.runLater(() -> alert.setResizable(false));
            });

            Optional<ButtonType> result = alert.showAndWait();

            if(result.isPresent() && result.get() == ButtonType.YES)
            {
                getAliasItemEditor().save();
            }
        }

        if(aliases.size() <= 1)
        {
            setEditor(getAliasItemEditor());
            if(aliases.size() == 1)
            {
                getAliasItemEditor().setItem(aliases.get(0));
            }
            else
            {
                getAliasItemEditor().setItem(null);
            }
        }
        else
        {
            setEditor(getAliasBulkEditor());
            getAliasBulkEditor().setItem(aliases);
        }

        getCloneAliasButton().setDisable(aliases.size() != 1);
        getDeleteAliasButton().setDisable(aliases.isEmpty());
        getMoveToAliasButton().setDisable(aliases.isEmpty());
    }

    private AliasItemEditor getAliasItemEditor()
    {
        if(mAliasItemEditor == null)
        {
            mAliasItemEditor = new AliasItemEditor(mPlaylistManager, mUserPreferences);
            mAliasItemEditor.setPadding(new Insets(16, 16, 16, 16));
        }

        return mAliasItemEditor;
    }

    private AliasBulkEditor getAliasBulkEditor()
    {
        if(mAliasBulkEditor == null)
        {
            mAliasBulkEditor = new AliasBulkEditor(mPlaylistManager);
        }

        return mAliasBulkEditor;
    }

    private VBox getSearchAndListSelectionBox()
    {
        if(mSearchAndListSelectionBox == null)
        {
            // Row 1: Alias list management + search
            HBox row1 = new HBox();
            row1.setAlignment(Pos.CENTER_LEFT);
            row1.setPadding(new Insets(8, 10, 4, 10));
            row1.setSpacing(8);

            Label listLabel = new Label("Alias List:");
            listLabel.setMinWidth(Region.USE_PREF_SIZE);
            Label searchLabel = new Label("Search:");
            searchLabel.setMinWidth(Region.USE_PREF_SIZE);

            getAliasListNameComboBox().setMinWidth(Region.USE_PREF_SIZE);
            getNewAliasListButton().setMinWidth(Region.USE_PREF_SIZE);
            getRenameAliasListButton().setMinWidth(Region.USE_PREF_SIZE);
            getDeleteAliasListButton().setMinWidth(Region.USE_PREF_SIZE);

            Region spacer1 = new Region();
            HBox.setHgrow(spacer1, Priority.ALWAYS);

            HBox searchBox = new HBox(8);
            searchBox.setAlignment(Pos.CENTER);
            searchBox.setMinWidth(Region.USE_PREF_SIZE);
            searchBox.getChildren().addAll(searchLabel, getSearchField());

            row1.getChildren().addAll(listLabel, getAliasListNameComboBox(),
                getNewAliasListButton(), getRenameAliasListButton(), getDeleteAliasListButton(),
                spacer1, searchBox);

            // Row 2: Alias actions (move, new, clone, delete)
            HBox row2 = new HBox();
            row2.setAlignment(Pos.CENTER_RIGHT);
            row2.setPadding(new Insets(4, 10, 8, 10));
            row2.setSpacing(8);

            getMoveToAliasButton().setMinWidth(Region.USE_PREF_SIZE);
            getNewAliasButton().setMinWidth(Region.USE_PREF_SIZE);
            getCloneAliasButton().setMinWidth(Region.USE_PREF_SIZE);
            getDeleteAliasButton().setMinWidth(Region.USE_PREF_SIZE);

            Region spacer2 = new Region();
            HBox.setHgrow(spacer2, Priority.ALWAYS);

            row2.getChildren().addAll(spacer2, getMoveToAliasButton(), getNewAliasButton(),
                getCloneAliasButton(), getDeleteAliasButton());

            mSearchAndListSelectionBox = new VBox();
            mSearchAndListSelectionBox.getStyleClass().addAll("context-toolbar", "kennebec-filter-toolbar");
            mSearchAndListSelectionBox.getChildren().addAll(row1, row2);
        }

        return mSearchAndListSelectionBox;
    }

    private TextField getSearchField()
    {
        if(mSearchField == null)
        {
            mSearchField = TextFields.createClearableTextField();
            mSearchField.setPromptText("Filter aliases\u2026");
            mSearchField.setPrefWidth(200);
            mSearchField.getStyleClass().add("kennebec-search-field");
            mSearchField.textProperty().addListener((observable, oldValue, newValue) -> update());
        }

        return mSearchField;
    }

    private void update()
    {
        getAliasFilteredList().setPredicate(null);
        getAliasPredicate().setAliasListName(getAliasListNameComboBox().getSelectionModel().getSelectedItem());
        getAliasPredicate().setSearchText(getSearchField().getText());
        getAliasFilteredList().setPredicate(getAliasPredicate());
    }

    private AliasPredicate getAliasPredicate()
    {
        if(mAliasPredicate == null)
        {
            mAliasPredicate = new AliasPredicate();
            mAliasPredicate.setAliasListName(getAliasListNameComboBox().getSelectionModel().getSelectedItem());
        }

        return mAliasPredicate;
    }

    private ComboBox<String> getAliasListNameComboBox()
    {
        if(mAliasListNameComboBox == null)
        {
            mAliasListNameComboBox = new ComboBox<>(mPlaylistManager.getAliasModel().aliasListNames());
            mAliasListNameComboBox.getSelectionModel().selectedItemProperty()
                    .addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) ->
                    {
                        getNewAliasButton().setDisable(newValue == null || newValue.contentEquals(AliasModel.NO_ALIAS_LIST));
                        update();
                    });

            if(mAliasListNameComboBox.getItems().size() > 1)
            {
                if(!mAliasListNameComboBox.getItems().get(0).contentEquals(AliasModel.NO_ALIAS_LIST))
                {
                    mAliasListNameComboBox.getSelectionModel().select(0);
                }
                else
                {
                    mAliasListNameComboBox.getSelectionModel().select(1);
                }
            }
            else if(mAliasListNameComboBox.getItems().size() == 1)
            {
                mAliasListNameComboBox.getSelectionModel().select(0);
            }
        }

        return mAliasListNameComboBox;
    }

    private Button getNewAliasListButton()
    {
        if(mNewAliasListButton == null)
        {
            mNewAliasListButton = new Button("New Alias List");
            mNewAliasListButton.getStyleClass().add("kennebec-toolbar-button");
            mNewAliasListButton.setTooltip(new Tooltip("Create a new alias list"));
            mNewAliasListButton.setOnAction(event ->
            {
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Create New Alias List");
                dialog.setHeaderText("Please enter an alias list name (max 25 chars).");
                dialog.setContentText("Name:");
                dialog.getEditor().setTextFormatter(new TextFormatter<String>(new MaxLengthUnaryOperator(25)));
                Optional<String> result = dialog.showAndWait();

                result.ifPresent(s ->
                {
                    String name = result.get();

                    if(name != null && !name.isEmpty())
                    {
                        name = name.trim();
                        mPlaylistManager.getAliasModel().addAliasList(name);
                        getAliasListNameComboBox().getSelectionModel().select(name);
                    }
                });
            });
        }

        return mNewAliasListButton;
    }

    private Button getRenameAliasListButton() {

        if (mRenameAliasButton == null) {
            mRenameAliasButton = new Button("Rename");
            mRenameAliasButton.getStyleClass().add("kennebec-toolbar-button");
            mRenameAliasButton.setTooltip(new Tooltip("Rename the current alias list"));
            mRenameAliasButton.setOnAction(event -> {
                String aliasListName = getAliasListNameComboBox().getSelectionModel().getSelectedItem();

                if (aliasListName.equals(AliasModel.NO_ALIAS_LIST)) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "", ButtonType.OK); io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane());
                    alert.setTitle("Rename Alias List");
                    alert.setHeaderText("You cannot rename " + aliasListName + ".");
                    alert.initOwner((getRenameAliasListButton()).getScene().getWindow());
                    alert.showAndWait();
                    return;
                }

                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Renaming Alias List: " + aliasListName);
                dialog.setHeaderText("Please enter the new alias list name (max 25 chars).");
                dialog.setContentText("Name:");
                dialog.getEditor().setTextFormatter(new TextFormatter<String>(new MaxLengthUnaryOperator(25)));
                Optional<String> result = dialog.showAndWait();
                result.ifPresent(newAliasListName -> {
                    mPlaylistManager.renameAliasList(aliasListName, newAliasListName);
                    getAliasListNameComboBox().getSelectionModel().select(newAliasListName);
                });
            });

        }
        return mRenameAliasButton;
    }

    private Button getDeleteAliasListButton() {

        if (mDeleteAliasListButton == null) {
            mDeleteAliasListButton = new Button("Delete");
            mDeleteAliasListButton.getStyleClass().add("kennebec-toolbar-button");
            mDeleteAliasListButton.setTooltip(new Tooltip("Delete the current alias list"));
            mDeleteAliasListButton.setOnAction(event -> {
                String aliasListName = getAliasListNameComboBox().getSelectionModel().getSelectedItem();
                if (aliasListName.equals(AliasModel.NO_ALIAS_LIST)) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "", ButtonType.OK); io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane());
                    alert.setTitle("Delete Alias List");
                    alert.setHeaderText("You cannot delete " + aliasListName + ".");
                    alert.initOwner((getDeleteAliasListButton()).getScene().getWindow());
                    alert.showAndWait();
                    return;
                }

                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "", ButtonType.NO, ButtonType.YES); io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane());
                alert.setTitle("Delete Alias List");
                alert.setHeaderText("Are you sure you want to delete the alias list " + aliasListName + " and all associated aliases?");
                alert.initOwner((getDeleteAliasListButton()).getScene().getWindow());

                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.YES)
                {
                    mPlaylistManager.deleteAliasList(aliasListName);
                }
            });
        }

        return mDeleteAliasListButton;
    }


    private TableView<Alias> getAliasTableView()
    {
        if(mAliasTableView == null)
        {
            mAliasTableView = new TableView<>();
            mAliasTableView.getStyleClass().add("preferences-table");

            TableColumn nameColumn = new TableColumn();
            nameColumn.setText("Alias");
            nameColumn.setCellValueFactory(new PropertyValueFactory<Alias, String>("name"));
            nameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
            nameColumn.setOnEditCommit(new javafx.event.EventHandler<TableColumn.CellEditEvent<Alias, String>>() {
                @Override
                public void handle(TableColumn.CellEditEvent<Alias, String> event) {
                    Alias alias = event.getRowValue();
                    alias.setName(event.getNewValue());
                    if (mAliasTableView.getSelectionModel().getSelectedItem() == alias) {
                        getAliasItemEditor().setItem(alias);
                    }
                }
            });
            nameColumn.setPrefWidth(200);
            nameColumn.setMinWidth(140);
            nameColumn.setId("alias.name");

            TableColumn groupColumn = new TableColumn();
            groupColumn.setText("Group");
            groupColumn.setCellValueFactory(new PropertyValueFactory<>("group"));
            groupColumn.setCellFactory(TextFieldTableCell.forTableColumn());
            groupColumn.setOnEditCommit(new javafx.event.EventHandler<TableColumn.CellEditEvent<Alias, String>>() {
                @Override
                public void handle(TableColumn.CellEditEvent<Alias, String> event) {
                    Alias alias = event.getRowValue();
                    alias.setGroup(event.getNewValue());
                    if (mAliasTableView.getSelectionModel().getSelectedItem() == alias) {
                        getAliasItemEditor().setItem(alias);
                    }
                }
            });
            groupColumn.setPrefWidth(160);
            groupColumn.setMinWidth(120);
            groupColumn.setId("alias.group");

            TableColumn<Alias, Integer> colorColumn = new TableColumn("Color");
            colorColumn.setCellValueFactory(new PropertyValueFactory<>("color"));
            colorColumn.setCellFactory(new ColorizedCell());
            colorColumn.setOnEditCommit(event -> {
                Alias alias = event.getRowValue();
                alias.setColor(event.getNewValue());
            });
            colorColumn.setPrefWidth(60);
            colorColumn.setMinWidth(60);
            colorColumn.setId("alias.color");

            TableColumn<Alias, String> iconColumn = new TableColumn("Icon");
            iconColumn.setCellValueFactory(new PropertyValueFactory<>("iconName"));
            iconColumn.setCellFactory(new IconTableCellFactory());
            iconColumn.setOnEditCommit(event -> {
                Alias alias = event.getRowValue();
                alias.setIconName(event.getNewValue());
                if (mAliasTableView.getSelectionModel().getSelectedItem() == alias) {
                    getAliasItemEditor().setItem(alias);
                }
            });
            iconColumn.setPrefWidth(60);
            iconColumn.setMinWidth(60);
            iconColumn.setId("alias.icon");

            TableColumn<Alias, Boolean> listenColumn = new TableColumn("Listen");
            listenColumn.setCellValueFactory(param -> javafx.beans.binding.Bindings.createObjectBinding(() -> param.getValue().priorityProperty().get() != io.github.dsheirer.alias.id.priority.Priority.DO_NOT_MONITOR, param.getValue().priorityProperty()));
            listenColumn.setCellFactory(new ListenCellFactory());
            listenColumn.setOnEditCommit(event -> {
                Alias alias = event.getRowValue();
                boolean listen = event.getNewValue();
                if (listen) {
                    if (alias.getPlaybackPriority() == io.github.dsheirer.alias.id.priority.Priority.DO_NOT_MONITOR) {
                        alias.setCallPriority(io.github.dsheirer.alias.id.priority.Priority.DEFAULT_PRIORITY);
                    }
                } else {
                    alias.setCallPriority(io.github.dsheirer.alias.id.priority.Priority.DO_NOT_MONITOR);
                }

                if (mAliasTableView.getSelectionModel().getSelectedItem() == alias) {
                    getAliasItemEditor().setItem(alias);
                }
            });
            listenColumn.setPrefWidth(60);
            listenColumn.setMinWidth(60);
            listenColumn.setId("alias.listen");

            TableColumn<Alias, Integer> priorityColumn = new TableColumn("Priority");
            priorityColumn.setCellFactory(new PriorityCellFactory());
            priorityColumn.setCellValueFactory(new PropertyValueFactory<>("priority"));
            priorityColumn.setOnEditCommit(event -> {
                Alias alias = event.getRowValue();
                if (event.getNewValue() != null) {
                    alias.setCallPriority(event.getNewValue());
                }
                if (mAliasTableView.getSelectionModel().getSelectedItem() == alias) {
                    getAliasItemEditor().setItem(alias);
                }
            });
            priorityColumn.setPrefWidth(70);
            priorityColumn.setMinWidth(70);
            priorityColumn.setId("alias.priority");

            TableColumn<Alias, Boolean> recordColumn = new TableColumn("Record");
            recordColumn.setCellValueFactory(new PropertyValueFactory<>("recordable"));
            recordColumn.setCellFactory(new IconCell(FontAwesome.SQUARE, Color.RED));
            recordColumn.setPrefWidth(65);
            recordColumn.setMinWidth(65);
            recordColumn.setId("alias.record");

            TableColumn<Alias, Boolean> streamColumn = new TableColumn("Stream");
            streamColumn.setCellValueFactory(new PropertyValueFactory<>("streamable"));
            streamColumn.setCellFactory(new IconCell(FontAwesome.VOLUME_UP, Color.DARKBLUE));
            streamColumn.setPrefWidth(65);
            streamColumn.setMinWidth(65);
            streamColumn.setId("alias.stream");

            TableColumn<Alias, Integer> idsColumn = new TableColumn("IDs");
            idsColumn.setCellValueFactory(new IdentifierCountCell());
            idsColumn.setPrefWidth(50);
            idsColumn.setMinWidth(50);
            idsColumn.setId("alias.ids");

            TableColumn<Alias, Boolean> errorsColumn = new TableColumn<>("Error");
            errorsColumn.setPrefWidth(120);
            errorsColumn.setId("alias.errors");
            errorsColumn.setCellValueFactory(new PropertyValueFactory<>("overlap"));
            errorsColumn.setCellFactory(param ->
            {
                TableCell<Alias, Boolean> tableCell = new TableCell<>()
                {
                    private IconNode iconNode;

                    @Override
                    protected void updateItem(Boolean item, boolean empty)
                    {
                        super.updateItem(item, empty);
                        setAlignment(Pos.CENTER);
                        setText(null);

                        if(empty || item == null || !item)
                        {
                            setGraphic(null);
                        }
                        else
                        {
                            if (iconNode == null) {
                                iconNode = new IconNode(FontAwesome.EXCLAMATION_CIRCLE);
                                iconNode.setFill(Color.RED);
                            }
                            setGraphic(iconNode);
                            setText("Identifier Overlap");
                        }
                    }
                };

                return tableCell;
            });


            mAliasTableView.getColumns().addAll(nameColumn, groupColumn, colorColumn, iconColumn, listenColumn,
                    priorityColumn, recordColumn, streamColumn, idsColumn, errorsColumn);

            mAliasTableView.setPlaceholder(getPlaceholderLabel());
            mAliasTableView.setRowFactory(tableView -> {
                TableRow<Alias> row = new TableRow<>();
                ContextMenu contextMenu = new ContextMenu();

                MenuItem toggleRecordItem = new MenuItem("Toggle Record");
                toggleRecordItem.setOnAction(event -> {
                    Alias alias = row.getItem();
                    if (alias != null) {
                        alias.setRecordable(!alias.isRecordable());
                    }
                });

                MenuItem toggleListenItem = new MenuItem("Toggle Listen");
                toggleListenItem.setOnAction(event -> {
                    Alias alias = row.getItem();
                    if (alias != null) {
                        if (alias.getPlaybackPriority() == io.github.dsheirer.alias.id.priority.Priority.DO_NOT_MONITOR) {
                            alias.setCallPriority(io.github.dsheirer.alias.id.priority.Priority.DEFAULT_PRIORITY);
                        } else {
                            alias.setCallPriority(io.github.dsheirer.alias.id.priority.Priority.DO_NOT_MONITOR);
                        }
                    }
                });

                contextMenu.getItems().addAll(toggleRecordItem, toggleListenItem);

                contextMenu.setOnShowing(event -> {
                    Alias alias = row.getItem();
                    if (alias != null) {
                        toggleRecordItem.setText(alias.isRecordable() ? "Disable Record" : "Enable Record");
                        toggleListenItem.setText(alias.getPlaybackPriority() != io.github.dsheirer.alias.id.priority.Priority.DO_NOT_MONITOR ? "Disable Listen" : "Enable Listen");
                    }
                });

                // Only display context menu for non-empty rows
                row.contextMenuProperty().bind(
                        javafx.beans.binding.Bindings.when(row.emptyProperty())
                        .then((ContextMenu) null)
                        .otherwise(contextMenu));

                return row;
            });

            mAliasTableView.setEditable(true);
            mAliasTableView.setItems(getAliasSortedList());
            mAliasTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            mAliasTableView.getSelectionModel().getSelectedItems().addListener((ListChangeListener<Alias>)c -> {
                Platform.runLater(() -> setAliases(mAliasTableView.getSelectionModel().getSelectedItems()));
            });

            ContextMenu contextMenu = new ContextMenu();
            for (TableColumn<Alias, ?> column : mAliasTableView.getColumns()) {
                CheckMenuItem checkMenuItem = new CheckMenuItem(column.getText());
                checkMenuItem.selectedProperty().bindBidirectional(column.visibleProperty());
                contextMenu.getItems().add(checkMenuItem);
            }

            for (TableColumn<Alias, ?> column : mAliasTableView.getColumns()) {
                column.setContextMenu(contextMenu);
            }

            mAliasTableView.setTableMenuButtonVisible(true);

            new io.github.dsheirer.preference.javafx.FxTableColumnMonitor(mUserPreferences, mAliasTableView, "aliasTable");
        }

        return mAliasTableView;
    }

    private FilteredList<Alias> getAliasFilteredList()
    {
        if(mAliasFilteredList == null)
        {
            mAliasFilteredList = new FilteredList<>(mPlaylistManager.getAliasModel().aliasList(), getAliasPredicate());
        }

        return mAliasFilteredList;
    }

    private SortedList<Alias> getAliasSortedList()
    {
        if(mAliasSortedList == null)
        {
            mAliasSortedList = new SortedList<>(getAliasFilteredList());
            mAliasSortedList.comparatorProperty().bind(getAliasTableView().comparatorProperty());

            //Don't re-sort while the bulk editor is still applying changes to aliases
            getAliasBulkEditor().changeInProgressProperty().addListener((observable, oldValue, newValue) ->
            {
                if(newValue)
                {
                    mAliasSortedList.comparatorProperty().unbind();
                    mAliasSortedList.setComparator(null);
                }
                else
                {
                    mAliasSortedList.comparatorProperty().bind(getAliasTableView().comparatorProperty());
                }
            });
        }

        return mAliasSortedList;
    }

    private Label getPlaceholderLabel()
    {
        if(mPlaceholderLabel == null)
        {
            mPlaceholderLabel = new Label("Select an Alias List and click the New button to create new aliases");
        }

        return mPlaceholderLabel;
    }

    private HBox getButtonBox()
    {
        if(mButtonBox == null)
        {
            mButtonBox = new HBox();
            mButtonBox.setAlignment(Pos.CENTER);
            mButtonBox.setPadding(new Insets(8));
            mButtonBox.setSpacing(8);
            mButtonBox.getStyleClass().addAll("kennebec-grouped-bg", "kennebec-border-right");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            mButtonBox.getChildren().addAll(getNewAliasButton(), getCloneAliasButton(),
                    spacer, getMoveToAliasButton(), getDeleteAliasButton());
        }

        return mButtonBox;
    }

    private Button getNewAliasButton()
    {
        if(mNewAliasButton == null)
        {
            mNewAliasButton = new Button("New");
            mNewAliasButton.getStyleClass().add("kennebec-toolbar-button-primary");
            mNewAliasButton.setDisable(true);
            mNewAliasButton.setAlignment(Pos.CENTER);
            mNewAliasButton.setMaxWidth(Double.MAX_VALUE);
            mNewAliasButton.setTooltip(new Tooltip("Create a new alias"));
            mNewAliasButton.setOnAction(event ->
            {
                Alias alias = new Alias("New Alias");
                alias.setAliasListName(getAliasListNameComboBox().getSelectionModel().getSelectedItem());
                mPlaylistManager.getAliasModel().addAlias(alias);

                //Queue a select alias action to allow table to update filter predicate and display the alias
                Platform.runLater(() ->
                {
                    getAliasTableView().getSelectionModel().clearSelection();
                    getAliasTableView().getSelectionModel().select(alias);
                    getAliasTableView().scrollTo(alias);
                });
            });
        }

        return mNewAliasButton;
    }

    private Button getDeleteAliasButton()
    {
        if(mDeleteAliasButton == null)
        {
            mDeleteAliasButton = new Button("Delete");
            mDeleteAliasButton.getStyleClass().add("kennebec-toolbar-button");
            mDeleteAliasButton.setDisable(true);
            mDeleteAliasButton.setMaxWidth(Double.MAX_VALUE);
            mDeleteAliasButton.setTooltip(new Tooltip("Delete the currently selected aliases"));
            mDeleteAliasButton.setOnAction(event ->
            {
                int count = getAliasTableView().getSelectionModel().getSelectedItems().size();

                Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                        "Do you want to delete [" + count + "] selected alias" + ((count > 1) ? "es?" : "?"),
                        ButtonType.NO, ButtonType.YES);
                alert.setTitle("Delete Alias");
                alert.setHeaderText("Are you sure?");
                alert.initOwner((getDeleteAliasButton()).getScene().getWindow());

                Optional<ButtonType> result = alert.showAndWait();

                if(result.isPresent() && result.get() == ButtonType.YES)
                {
                    List<Alias> selectedAliases = new ArrayList<>(getAliasTableView().getSelectionModel().getSelectedItems());
                    mPlaylistManager.getAliasModel().removeAliases(selectedAliases);
                }
            });
        }

        return mDeleteAliasButton;
    }

    private Button getCloneAliasButton()
    {
        if(mCloneAliasButton == null)
        {
            mCloneAliasButton = new Button("Clone");
            mCloneAliasButton.getStyleClass().add("kennebec-toolbar-button");
            mCloneAliasButton.setDisable(true);
            mCloneAliasButton.setMaxWidth(Double.MAX_VALUE);
            mCloneAliasButton.setTooltip(new Tooltip("Create a clone (copy) of the currently selected alias"));
            mCloneAliasButton.setOnAction(event ->
            {
                Alias original = getAliasTableView().getSelectionModel().getSelectedItem();
                Alias copy = AliasFactory.shallowCopyOf(original);

                String baseName = original.getName() + "-Copy";
                String newName = baseName;
                int counter = 1;
                java.util.List<Alias> existingAliases = mPlaylistManager.getAliasModel().getAliases();
                boolean nameExists = true;
                while (nameExists) {
                    nameExists = false;
                    for (Alias a : existingAliases) {
                        if (a.getName().equals(newName)) {
                            nameExists = true;
                            break;
                        }
                    }
                    if (nameExists) {
                        newName = baseName + counter;
                        counter++;
                    }
                }
                copy.setName(newName);

                mPlaylistManager.getAliasModel().addAlias(copy);
                getAliasTableView().getSelectionModel().clearSelection();
                getAliasTableView().getSelectionModel().select(copy);
                getAliasTableView().scrollTo(copy);
            });
        }

        return mCloneAliasButton;
    }

    private MenuButton getMoveToAliasButton()
    {
        if(mMoveToAliasButton == null)
        {
            mMoveToAliasButton = new MenuButton("Move To");
            mMoveToAliasButton.getStyleClass().add("kennebec-toolbar-button");
            mMoveToAliasButton.setDisable(true);
            mMoveToAliasButton.setTooltip(new Tooltip("Move the currently selected aliases to another list"));
            mMoveToAliasButton.setOnShowing(event ->
            {
                mMoveToAliasButton.getItems().clear();

                MenuItem emptyItem = new MenuItem("Alias Lists");
                emptyItem.setDisable(true);
                mMoveToAliasButton.getItems().addAll(emptyItem, new SeparatorMenuItem());

                List<String> aliasLists = mPlaylistManager.getAliasModel().getListNames();

                for(String aliasList : aliasLists)
                {
                    if(!aliasList.contentEquals(AliasModel.NO_ALIAS_LIST) &&
                            !aliasList.contentEquals(getAliasListNameComboBox().getSelectionModel().getSelectedItem()))
                    {
                        mMoveToAliasButton.getItems().add(new MoveToAliasListItem(aliasList));
                    }
                }
            });
        }

        return mMoveToAliasButton;
    }

    public class MoveToAliasListItem extends MenuItem
    {
        public MoveToAliasListItem(String aliasList)
        {
            super(aliasList);

            setOnAction(event ->
            {
                List<Alias> selectedAliases = new ArrayList<>(getAliasTableView().getSelectionModel().getSelectedItems());
                for(Alias selected : selectedAliases)
                {
                    AliasList existing = mPlaylistManager.getAliasModel().getAliasList(selected.getAliasListName());
                    existing.removeAlias(selected);

                    selected.setAliasListName(getText());
                    AliasList moveToList = mPlaylistManager.getAliasModel().getAliasList(selected.getAliasListName());
                    moveToList.addAlias(selected);
                }
            });
        }
    }

    public class ColorizedCell implements Callback<TableColumn<Alias, Integer>, TableCell<Alias, Integer>>
    {
        @Override
        public TableCell<Alias, Integer> call(TableColumn<Alias, Integer> param)
        {
            return new TableCell<Alias, Integer>()
            {
                private final Rectangle rectangle = new Rectangle(20, 20);
                private ColorPicker colorPicker;

                {
                    rectangle.setArcHeight(10);
                    rectangle.setArcWidth(10);
                    setAlignment(Pos.CENTER);
                }

                @Override
                public void startEdit()
                {
                    if (!isEmpty())
                    {
                        super.startEdit();
                        if (colorPicker == null)
                        {
                            createColorPicker();
                        }
                        Alias alias = getTableRow().getItem();
                        if (alias != null) {
                            colorPicker.setValue(ColorUtil.fromInteger(alias.getColor()));
                        }
                        setGraphic(colorPicker);
                        colorPicker.requestFocus();
                        colorPicker.show();
                    }
                }

                @Override
                public void cancelEdit()
                {
                    super.cancelEdit();
                    updateItem(getItem(), false);
                }

                @Override
                protected void updateItem(Integer item, boolean empty)
                {
                    super.updateItem(item, empty);

                    if(empty || getTableRow() == null || getTableRow().getItem() == null)
                    {
                        setGraphic(null);
                    }
                    else if (isEditing())
                    {
                        setGraphic(colorPicker);
                    }
                    else
                    {
                        Alias alias = getTableRow().getItem();
                        rectangle.setVisible(true);
                        rectangle.setFill(ColorUtil.fromInteger(alias.getColor()));
                        setGraphic(rectangle);
                    }
                }

                private void createColorPicker()
                {
                    colorPicker = new ColorPicker();
                    colorPicker.setOnAction(event -> {
                        if (isEditing()) {
                            commitEdit(ColorUtil.toInteger(colorPicker.getValue()));
                        }
                    });
                }
            };
        }
    }

    /**
     * Boolean table cell with an icon visibility bound to the boolean value
     */
    public class IconCell implements Callback<TableColumn<Alias, Boolean>, TableCell<Alias, Boolean>>
    {
        private IconCode mIconCode;
        private Color mColor;

        public IconCell(IconCode iconCode, Color color)
        {
            mIconCode = iconCode;
            mColor = color;
        }

        @Override
        public TableCell<Alias, Boolean> call(TableColumn<Alias, Boolean> param)
        {
            final IconNode iconNode = new IconNode(mIconCode);
            iconNode.setIconSize(20);
            iconNode.setFill(mColor);

            TableCell<Alias, Boolean> tableCell = new TableCell<>()
            {
                @Override
                protected void updateItem(Boolean item, boolean empty)
                {
                    super.updateItem(item, empty);

                    if(!empty && getTableRow() != null)
                    {
                        iconNode.setVisible(item);
                    }
                    else
                    {
                        iconNode.setVisible(false);
                    }
                }
            };
            tableCell.setAlignment(Pos.CENTER);
            tableCell.setGraphic(iconNode);
            return tableCell;
        }
    }

    public class IdentifierCountCell implements Callback<TableColumn.CellDataFeatures<Alias, Integer>, ObservableValue<Integer>>
    {
        @Override
        public ObservableValue<Integer> call(TableColumn.CellDataFeatures<Alias, Integer> param)
        {
            if(param.getValue() != null)
            {
                return param.getValue().nonAudioIdentifierCountProperty().asObject();
            }

            return null;
        }
    }

    public class ActionCountCell implements Callback<TableColumn.CellDataFeatures<Alias, Integer>, ObservableValue<Integer>>
    {
        @Override
        public ObservableValue<Integer> call(TableColumn.CellDataFeatures<Alias, Integer> param)
        {
            Integer count = null;

            if(param.getValue() != null && param.getValue().getAliasActions().size() > 0)
            {
                count = param.getValue().getAliasActions().size();
            }

            return new ReadOnlyObjectWrapper<>(count);
        }
    }

    public class CenteredCountCellFactory implements Callback<TableColumn<Alias, Integer>, TableCell<Alias, Integer>>
    {
        @Override
        public TableCell<Alias, Integer> call(TableColumn<Alias, Integer> param)
        {
            return new CenteredCountCell();
        }
    }

    public class CenteredCountCell extends TableCell<Alias, Integer>
    {
        public CenteredCountCell()
        {
            setAlignment(Pos.CENTER);
        }
    }

    public class ListenCellFactory implements Callback<TableColumn<Alias, Boolean>, TableCell<Alias, Boolean>>
    {
        @Override
        public TableCell<Alias, Boolean> call(TableColumn<Alias, Boolean> param)
        {
            return new TableCell<Alias, Boolean>()
            {
                private ComboBox<Boolean> comboBox;

                {
                    setAlignment(Pos.CENTER);
                }

                @Override
                public void startEdit()
                {
                    if (!isEmpty())
                    {
                        super.startEdit();
                        if (comboBox == null)
                        {
                            createComboBox();
                        }
                        comboBox.getSelectionModel().select(getItem());
                        setGraphic(comboBox);
                        setText(null);
                        comboBox.requestFocus();
                        comboBox.show();
                    }
                }

                @Override
                public void cancelEdit()
                {
                    super.cancelEdit();
                    updateItem(getItem(), false);
                }

                private void createComboBox()
                {
                    comboBox = new ComboBox<>();
                    comboBox.getItems().addAll(Boolean.TRUE, Boolean.FALSE);

                    Callback<ListView<Boolean>, ListCell<Boolean>> cellFactory = new Callback<>() {
                        @Override
                        public ListCell<Boolean> call(ListView<Boolean> param) {
                            return new ListCell<>() {
                                @Override
                                protected void updateItem(Boolean item, boolean empty) {
                                    super.updateItem(item, empty);
                                    if (empty || item == null) {
                                        setText(null);
                                    } else {
                                        setText(item ? "Listen" : "Mute");
                                    }
                                }
                            };
                        }
                    };
                    comboBox.setCellFactory(cellFactory);
                    comboBox.setButtonCell(cellFactory.call(null));

                    comboBox.setOnAction(event -> {
                        if (isEditing()) {
                            commitEdit(comboBox.getSelectionModel().getSelectedItem());
                        }
                    });
                }

                private IconNode iconNode;

                @Override
                protected void updateItem(Boolean item, boolean empty)
                {
                    super.updateItem(item, empty);

                    if(empty || getTableRow() == null || getTableRow().getItem() == null)
                    {
                        setText(null);
                        setGraphic(null);
                    }
                    else if (isEditing())
                    {
                        setGraphic(comboBox);
                        setText(null);
                    }
                    else
                    {
                        if(item == null) {
                            setText(null);
                            setGraphic(null);
                            return;
                        }

                        if (iconNode == null) {
                            iconNode = new IconNode(FontAwesome.VOLUME_UP);
                            iconNode.setIconSize(20);
                        }

                        if(item)
                        {
                            setText("Listen");
                            iconNode.setIconCode(FontAwesome.VOLUME_UP);
                            iconNode.setFill(Color.GREEN);
                        }
                        else
                        {
                            setText("Mute");
                            iconNode.setIconCode(FontAwesome.VOLUME_OFF);
                            iconNode.setFill(Color.RED);
                        }
                        setGraphic(iconNode);
                    }
                }
            };
        }
    }

    public class PriorityCellFactory implements Callback<TableColumn<Alias, Integer>, TableCell<Alias, Integer>>
    {
        @Override
        public TableCell<Alias, Integer> call(TableColumn<Alias, Integer> param)
        {
            return new TableCell<Alias, Integer>()
            {
                private ComboBox<Integer> comboBox;

                {
                    setAlignment(Pos.CENTER);
                }

                @Override
                public void startEdit()
                {
                    if (!isEmpty())
                    {
                        super.startEdit();
                        if (comboBox == null)
                        {
                            createComboBox();
                        }
                        comboBox.getSelectionModel().select(getItem());
                        setGraphic(comboBox);
                        setText(null);
                        comboBox.requestFocus();
                        comboBox.show();
                    }
                }

                @Override
                public void cancelEdit()
                {
                    super.cancelEdit();
                    updateItem(getItem(), false);
                }

                private void createComboBox()
                {
                    comboBox = new ComboBox<>();
                    comboBox.getItems().add(io.github.dsheirer.alias.id.priority.Priority.DEFAULT_PRIORITY);
                    for(int x = io.github.dsheirer.alias.id.priority.Priority.MIN_PRIORITY; x < io.github.dsheirer.alias.id.priority.Priority.MAX_PRIORITY; x++)
                    {
                        comboBox.getItems().add(x);
                    }

                    Callback<ListView<Integer>, ListCell<Integer>> cellFactory = new Callback<>() {
                        @Override
                        public ListCell<Integer> call(ListView<Integer> param) {
                            return new ListCell<>() {
                                @Override
                                protected void updateItem(Integer item, boolean empty) {
                                    super.updateItem(item, empty);
                                    if (empty || item == null) {
                                        setText(null);
                                    } else if (item == io.github.dsheirer.alias.id.priority.Priority.DEFAULT_PRIORITY) {
                                        setText("Default");
                                    } else {
                                        setText(item.toString());
                                    }
                                }
                            };
                        }
                    };
                    comboBox.setCellFactory(cellFactory);
                    comboBox.setButtonCell(cellFactory.call(null));

                    comboBox.setOnAction(event -> {
                        if (isEditing()) {
                            commitEdit(comboBox.getSelectionModel().getSelectedItem());
                        }
                    });
                }

                @Override
                protected void updateItem(Integer item, boolean empty)
                {
                    super.updateItem(item, empty);

                    if(empty || getTableRow() == null || getTableRow().getItem() == null)
                    {
                        setText(null);
                        setGraphic(null);
                    }
                    else if (isEditing())
                    {
                        setGraphic(comboBox);
                        setText(null);
                    }
                    else
                    {
                        if(item == null) {
                            setText(null);
                            setGraphic(null);
                            return;
                        }

                        if(item == io.github.dsheirer.alias.id.priority.Priority.DO_NOT_MONITOR)
                        {
                            setText("");
                            setGraphic(null);
                        }
                        else if(item == io.github.dsheirer.alias.id.priority.Priority.DEFAULT_PRIORITY)
                        {
                            setText("Default");
                            setGraphic(null);
                        }
                        else
                        {
                            setText(item.toString());
                            setGraphic(null);
                        }
                    }
                }
            };
        }
    }

    public class IconTableCellFactory implements Callback<TableColumn<Alias, String>, TableCell<Alias, String>>
    {
        @Override
        public TableCell<Alias, String> call(TableColumn<Alias, String> param)
        {
            return new TableCell<Alias, String>()
            {
                private ComboBox<io.github.dsheirer.icon.Icon> comboBox;
                private ImageView imageView;

                {
                    setAlignment(Pos.CENTER);
                    imageView = new ImageView();
                    imageView.setFitHeight(16);
                    imageView.setFitWidth(16);
                }

                @Override
                public void startEdit()
                {
                    if (!isEmpty())
                    {
                        super.startEdit();
                        if (comboBox == null)
                        {
                            createComboBox();
                        }
                        Alias alias = getTableRow().getItem();
                        if (alias != null) {
                            io.github.dsheirer.icon.Icon icon = mPlaylistManager.getIconModel().getIcon(alias.getIconName());
// //                             comboBox.getSelectionModel().select(icon);
                        }
                        setGraphic(comboBox);
                        comboBox.requestFocus();
                        comboBox.show();
                    }
                }

                @Override
                public void cancelEdit()
                {
                    super.cancelEdit();
                    updateItem(getItem(), false);
                }

                private void createComboBox()
                {
                    comboBox = new ComboBox<>();
// //                     comboBox.setItems(new javafx.collections.transformation.SortedList<>(mPlaylistManager.getIconModel().iconsProperty(), com.google.common.collect.Ordering.natural()));
                    Callback<ListView<io.github.dsheirer.icon.Icon>, ListCell<io.github.dsheirer.icon.Icon>> cellFactory = new Callback<>() {
                        @Override
                        public ListCell<io.github.dsheirer.icon.Icon> call(ListView<io.github.dsheirer.icon.Icon> param) {
                            return new ListCell<>() {
                                private ImageView iv = new ImageView();
                                {
                                    iv.setFitWidth(16);
                                    iv.setFitHeight(16);
                                }
                                @Override
                                protected void updateItem(io.github.dsheirer.icon.Icon item, boolean empty) {
                                    super.updateItem(item, empty);
                                    if (empty || item == null) {
                                        setText(null);
                                        setGraphic(null);
                                    } else {
                                        setText(item.getName());
                                        iv.setImage(null);
                                        setGraphic(iv);
                                    }
                                }
                            };
                        }
                    };
                    comboBox.setCellFactory(cellFactory);
                    comboBox.setButtonCell(cellFactory.call(null));

                    comboBox.setOnAction(event -> {
                        if (isEditing()) {
                            io.github.dsheirer.icon.Icon selected = comboBox.getValue();
                            commitEdit(selected != null ? selected.getName() : null);
                        }
                    });
                }

                @Override
                protected void updateItem(String item, boolean empty)
                {
                    super.updateItem(item, empty);

                    if(empty || getTableRow() == null || getTableRow().getItem() == null)
                    {
                        setGraphic(null);
                    }
                    else if (isEditing())
                    {
                        setGraphic(comboBox);
                    }
                    else
                    {
                        Alias alias = getTableRow().getItem();
                        if(alias != null)
                        {
                            io.github.dsheirer.icon.Icon icon = mPlaylistManager.getIconModel().getIcon(alias.getIconName());

                            if(icon != null && icon != null)
                            {
                                imageView.setImage(icon.getFxImage());
                                setGraphic(imageView);
                            }
                            else
                            {
                                setGraphic(null);
                            }
                        }
                    }
                }
            };
        }
    }

    /**
     * Alias filter predicate
     */
    public class AliasPredicate implements Predicate<Alias>
    {
        private String mAliasListName;
        private String mSearchText;

        @Override
        public boolean test(Alias alias)
        {
            if(mAliasListName == null)
            {
                return false;
            }
            else if(mAliasListName.equals(alias.getAliasListName()))
            {
                if(alias.getName() == null)
                {
                    return true;
                }
                else if(mSearchText == null || mSearchText.isEmpty())
                {
                    return true;
                }
                else if(alias.getName().toLowerCase().contains(mSearchText))
                {
                    return true;
                }
                else if(alias.getGroup() != null && alias.getGroup().toLowerCase().contains(mSearchText))
                {
                    return true;
                }
            }

            return false;
        }

        public void setAliasListName(String aliasListName)
        {
            if(aliasListName != null)
            {
                mAliasListName = aliasListName;
            }
        }

        public void setSearchText(String searchText)
        {
            if(searchText != null)
            {
                mSearchText = searchText.toLowerCase();
            }
            else
            {
                mSearchText = null;
            }
        }
    }
}
