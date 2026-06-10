
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

package io.github.dsheirer.gui.playlist.streaming;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;


import io.github.dsheirer.audio.broadcast.BroadcastConfiguration;
import io.github.dsheirer.audio.broadcast.BroadcastFactory;
import io.github.dsheirer.audio.broadcast.BroadcastFormat;
import io.github.dsheirer.audio.broadcast.BroadcastServerType;
import io.github.dsheirer.audio.broadcast.ConfiguredBroadcast;
import io.github.dsheirer.audio.broadcast.broadcastify.BroadcastifyFeedConfiguration;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.rrapi.type.UserFeedBroadcast;
import io.github.dsheirer.util.ThreadPool;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.TableRow;
import io.github.dsheirer.audio.broadcast.BroadcastEvent;
import io.github.dsheirer.audio.broadcast.BroadcastState;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.controlsfx.control.SegmentedButton;
import io.github.dsheirer.preference.javafx.FxTableColumnMonitor;
import org.controlsfx.control.textfield.TextFields;

/**
 * Editor for broadcast audio stream configurations
 */
public class StreamingEditor extends SplitPane
{
    private final static Logger mLog = LoggerFactory.getLogger(StreamingEditor.class);

    private final PlaylistManager mPlaylistManager;
    private TableView<ConfiguredBroadcast> mConfiguredBroadcastTableView;
    private MenuButton mNewButton;
    private Button mDeleteButton;
    private Button mCloneButton;
    private Button mRefreshButton;
    private TabPane mTabPane;
    private VBox mConfigAreaBox;
    private Label mHeaderLabel;
    private javafx.scene.image.ImageView mHeaderIcon;
    private Tab mConfigurationTab;
    private Tab mAliasTab;
    private Label mRadioReferenceLoginLabel;
    private AbstractBroadcastEditor<?> mCurrentEditor;
    private final UnknownStreamEditor mUnknownEditor;
    private Map<BroadcastServerType, AbstractBroadcastEditor<?>> mEditorMap = new EnumMap<>(BroadcastServerType.class);
    private final List<UserFeedBroadcast> mBroadcastifyFeeds = new ArrayList<>();
    private ScrollPane mEditorScrollPane;
    private StreamAliasSelectionEditor mStreamAliasSelectionEditor;
    private final StreamConfigurationEditorModificationListener mStreamConfigurationEditorModificationListener =
        new StreamConfigurationEditorModificationListener();
    private SegmentedButton mViewSegmentedButton;
    private ToggleButton mAllToggleButton;
    private ToggleButton mEnabledOnlyToggleButton;
    private javafx.collections.transformation.FilteredList<ConfiguredBroadcast> mFilteredBroadcasts;
    private TextField mSearchField;



    public void process(StreamTabRequest request)
    {
        if(request instanceof ViewStreamRequest)
        {
            ViewStreamRequest viewRequest = (ViewStreamRequest)request;
            BroadcastConfiguration config = viewRequest.getBroadcastConfiguration();

            if(config != null)
            {
                ConfiguredBroadcast configuredBroadcast = mPlaylistManager.getBroadcastModel().getConfiguredBroadcast(config);
                getConfiguredBroadcastTableView().getSelectionModel().select(configuredBroadcast);
                getTabPane().getSelectionModel().select(getConfigurationTab());
            }
        }
        else
        {
            // Generic stream tab request, select default tab
            getTabPane().getSelectionModel().select(getConfigurationTab());
        }
    }

    /**
     * Constructs an instance
     * @param playlistManager for accessing streaming model
     */
    public StreamingEditor(PlaylistManager playlistManager)
    {
        mPlaylistManager = playlistManager;
        mFilteredBroadcasts = new javafx.collections.transformation.FilteredList<>(
            mPlaylistManager.getBroadcastModel().getConfiguredBroadcasts()
        );
        mUnknownEditor = new UnknownStreamEditor(mPlaylistManager);
        mPlaylistManager.getRadioReference().availableProperty()
            .addListener((observable, oldValue, newValue) -> refreshBroadcastifyStreams());
        refreshBroadcastifyStreams();

        HBox toolbar = new HBox();
        toolbar.getStyleClass().addAll("context-toolbar", "kennebec-filter-toolbar");
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(10, 10, 10, 10));
        toolbar.setSpacing(10);

        mAllToggleButton = new ToggleButton("All");
        mEnabledOnlyToggleButton = new ToggleButton("Enabled Only");
        mViewSegmentedButton = new SegmentedButton(mAllToggleButton, mEnabledOnlyToggleButton);
        mViewSegmentedButton.setMinWidth(Region.USE_PREF_SIZE);
        mViewSegmentedButton.getStyleClass().add(SegmentedButton.STYLE_CLASS_DARK);
        mAllToggleButton.setSelected(true);
        mViewSegmentedButton.getToggleGroup().selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue == null)
            {
                oldValue.setSelected(true);
            }
            else
            {
                updateFilter();
            }
        });
        
        HBox viewBox = new HBox(5);
        viewBox.setAlignment(Pos.CENTER);
        Label viewLabel = new Label("View Streams:");
        viewLabel.setMinWidth(Region.USE_PREF_SIZE);
        viewBox.getChildren().addAll(viewLabel, mViewSegmentedButton);

        Region leftSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);
        
        HBox searchBox = new HBox(5);
        searchBox.setAlignment(Pos.CENTER);
        Label searchLabel = new Label("Search:");
        searchLabel.setMinWidth(Region.USE_PREF_SIZE);
        searchLabel.setAlignment(Pos.CENTER_RIGHT);
        searchBox.getChildren().addAll(searchLabel, getSearchField());

        Region rightSpacer = new Region();
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);

        toolbar.getChildren().addAll(viewBox, leftSpacer, searchBox, rightSpacer, getNewButton(), getCloneButton(), getDeleteButton(), getRefreshButton());

        VBox tableAndLabelBox = new VBox();
        VBox.setVgrow(getConfiguredBroadcastTableView(), Priority.ALWAYS);
        tableAndLabelBox.getChildren().addAll(getConfiguredBroadcastTableView(), getRadioReferenceLoginLabel());

        VBox masterBox = new VBox();
        VBox.setVgrow(tableAndLabelBox, Priority.ALWAYS);
        masterBox.getChildren().addAll(toolbar, tableAndLabelBox);

        setOrientation(Orientation.VERTICAL);

        getItems().addAll(masterBox);
    }

    private TextField getSearchField() {
        if(mSearchField == null) {
            mSearchField = TextFields.createClearableTextField();
            mSearchField.getStyleClass().add("kennebec-search-field");
            mSearchField.textProperty().addListener((observable, oldValue, newValue) -> updateFilter());
        }
        return mSearchField;
    }

    private void updateFilter() {
        String searchText = getSearchField().getText();
        boolean showEnabledOnly = mEnabledOnlyToggleButton != null && mEnabledOnlyToggleButton.isSelected();
        mFilteredBroadcasts.setPredicate(cb -> {
            if (cb.getBroadcastConfiguration() == null) return false;
            if (showEnabledOnly && !cb.getBroadcastConfiguration().isEnabled()) return false;
            if (searchText != null && !searchText.isEmpty()) {
                String name = cb.getBroadcastConfiguration().getName();
                if (name == null || !name.toLowerCase().contains(searchText.toLowerCase())) return false;
            }
            return true;
        });
    }

    private void setEditor(AbstractBroadcastEditor<?> editor)
    {
        if(editor != getCurrentEditor())
        {
            if(mCurrentEditor != null)
            {
                mCurrentEditor.modifiedProperty().removeListener(mStreamConfigurationEditorModificationListener);
            }

            mCurrentEditor = editor;

            //Register a listener on the editor modified property to detect configuration changes and refresh the
            //aliases tab
            mCurrentEditor.modifiedProperty().addListener(mStreamConfigurationEditorModificationListener);

            getEditorScrollPane().setContent(getCurrentEditor());
        }
    }

    private ScrollPane getEditorScrollPane()
    {
        if(mEditorScrollPane == null)
        {
            mEditorScrollPane = new ScrollPane();
            mEditorScrollPane.setMaxWidth(Double.MAX_VALUE);
            mEditorScrollPane.setFitToWidth(true);
            mEditorScrollPane.setContent(getCurrentEditor());
        }

        return mEditorScrollPane;
    }

    private void setBroadcastConfiguration(ConfiguredBroadcast configuredBroadcast)
    {
        //Prompt the user to save if the contents of the current channel editor have been modified
        if(getCurrentEditor().modifiedProperty().get())
        {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION); io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane());
            alert.getButtonTypes().clear();
            alert.getButtonTypes().addAll(ButtonType.NO, ButtonType.YES);
            alert.setTitle("Save Changes");
            alert.setHeaderText("Streaming configuration has been modified");
            alert.setContentText("Do you want to save these changes?");
            alert.initOwner((getNewButton()).getScene().getWindow());

            //Workaround for JavaFX KDE on Linux bug in FX 10/11: https://bugs.openjdk.java.net/browse/JDK-8179073
            alert.setResizable(true);
            alert.onShownProperty().addListener(e -> Platform.runLater(() -> alert.setResizable(false)));
            alert.showAndWait().ifPresent(buttonType -> {
                if(buttonType == ButtonType.YES)
                {
                    getCurrentEditor().save();
                }
            });
        }

        getDeleteButton().setDisable(configuredBroadcast == null);
        getCloneButton().setDisable(configuredBroadcast == null);

        if(configuredBroadcast == null)
        {
            setEditor(mUnknownEditor);
        }
        else
        {
            BroadcastServerType configType = configuredBroadcast.getBroadcastServerType();

            if(configType == null)
            {
                setEditor(mUnknownEditor);
            }
            else
            {
                BroadcastServerType editorType = getCurrentEditor().getBroadcastServerType();

                if(editorType == null || editorType != configType)
                {
                    AbstractBroadcastEditor editor = mEditorMap.get(configType);

                    if(editor == null)
                    {
                        editor = StreamEditorFactory.getEditor(configType, mPlaylistManager);

                        if(editor != null)
                        {
                            mEditorMap.put(configType, editor);
                        }
                    }

                    if(editor == null)
                    {
                        editor = mUnknownEditor;
                    }

                    setEditor(editor);
                }
            }
        }

        BroadcastConfiguration broadcastConfiguration = configuredBroadcast != null ?
            configuredBroadcast.getBroadcastConfiguration() : null;

        getCurrentEditor().setItem(broadcastConfiguration);
        getStreamAliasSelectionEditor().setBroadcastConfiguration(broadcastConfiguration);

        if(configuredBroadcast == null)
        {
            getItems().remove(getConfigAreaBox());
        }
        else
        {
            getConfigAreaBox();
            updateConfigHeader(configuredBroadcast);
            if(!getItems().contains(getConfigAreaBox()))
            {
                getItems().add(getConfigAreaBox());
            }

            setDividerPositions(0.5);
        }
    }

    /**
     * Updates the list of broadcastify stream configurations if the service is logged in.
     */
    private void refreshBroadcastifyStreams()
    {
        if(mPlaylistManager.getRadioReference().availableProperty().get())
        {
            ThreadPool.CACHED.submit(() -> {
                try
                {
                    List<UserFeedBroadcast> feeds = mPlaylistManager.getRadioReference().getService().getUserFeeds();
                    mBroadcastifyFeeds.clear();
                    mBroadcastifyFeeds.addAll(feeds);
                }
                catch(Throwable t)
                {
                    mLog.error("Unable to refresh broadcastify stream configuration(s)");
                }
            });
        }
    }

    private StreamAliasSelectionEditor getStreamAliasSelectionEditor()
    {
        if(mStreamAliasSelectionEditor == null)
        {
            mStreamAliasSelectionEditor = new StreamAliasSelectionEditor(mPlaylistManager);
        }

        return mStreamAliasSelectionEditor;
    }

    private Label getRadioReferenceLoginLabel()
    {
        if(mRadioReferenceLoginLabel == null)
        {
            mRadioReferenceLoginLabel = new Label("Note: use Radio Reference tab to login and access Broadcastify stream configuration(s)");
            mRadioReferenceLoginLabel.visibleProperty().bind(mPlaylistManager.getRadioReference().availableProperty().not());
        }

        return mRadioReferenceLoginLabel;
    }

    private AbstractBroadcastEditor getCurrentEditor()
    {
        if(mCurrentEditor == null)
        {
            mCurrentEditor = mUnknownEditor;
        }

        return mCurrentEditor;
    }

    private VBox getConfigAreaBox()
    {
        if(mConfigAreaBox == null)
        {
            mConfigAreaBox = new VBox();

            mHeaderLabel = new Label();
            mHeaderLabel.setStyle("-fx-font-size: 1.5em; -fx-font-weight: bold;");

            mHeaderIcon = new javafx.scene.image.ImageView();
            mHeaderIcon.setFitWidth(24);
            mHeaderIcon.setFitHeight(24);

            HBox headerBox = new HBox(10, mHeaderIcon, mHeaderLabel);
            headerBox.setAlignment(Pos.CENTER_LEFT);
            headerBox.setPadding(new Insets(10, 10, 0, 10));

            VBox.setVgrow(getTabPane(), Priority.ALWAYS);
            mConfigAreaBox.getChildren().addAll(headerBox, getTabPane());
        }
        return mConfigAreaBox;
    }

    private void updateConfigHeader(ConfiguredBroadcast configuredBroadcast)
    {
        if(configuredBroadcast != null)
        {
            mHeaderLabel.setText(configuredBroadcast.getBroadcastConfiguration().getName());
            BroadcastServerType type = configuredBroadcast.getBroadcastServerType();
            if(type != null && type.getIconPath() != null)
            {
                io.github.dsheirer.icon.Icon icon = new io.github.dsheirer.icon.Icon("empty", type.getIconPath());
                javafx.scene.image.Image fxImage = icon.getFxImage();
                if(fxImage != null)
                {
                    mHeaderIcon.setImage(fxImage);
                }
                else
                {
                    mHeaderIcon.setImage(null);
                }
            }
            else
            {
                mHeaderIcon.setImage(null);
            }
        }
    }

    private TabPane getTabPane()
    {
        if(mTabPane == null)
        {
            mTabPane = new TabPane();
            mTabPane.setMaxHeight(Double.MAX_VALUE);
            mTabPane.setPadding(new Insets(16, 16, 16, 16));
            mTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
            mTabPane.getTabs().addAll(getConfigurationTab(), getAliasTab());
        }

        return mTabPane;
    }

    private Tab getConfigurationTab()
    {
        if(mConfigurationTab == null)
        {
            mConfigurationTab = new Tab("Configuration");
            mConfigurationTab.setContent(getEditorScrollPane());
        }

        return mConfigurationTab;
    }

    private Tab getAliasTab()
    {
        if(mAliasTab == null)
        {
            mAliasTab = new Tab("Aliases");
            mAliasTab.setContent(getStreamAliasSelectionEditor());
        }

        return mAliasTab;
    }

    private MenuButton getNewButton()
    {
        if(mNewButton == null)
        {
            mNewButton = new MenuButton("New");
            mNewButton.getStyleClass().add("kennebec-toolbar-button-primary");
            mNewButton.setMaxWidth(Double.MAX_VALUE);
            mNewButton.setTooltip(new Tooltip("Create a new stream configuration"));
            mNewButton.setOnShowing(event -> {
                mNewButton.getItems().clear();

                for(UserFeedBroadcast feed: mBroadcastifyFeeds)
                {
                    //Only show a menu item for the feed if it's not already defined
                    if(mPlaylistManager.getBroadcastModel().getBroadcastConfiguration(feed.getDescription()) == null)
                    {
                        mNewButton.getItems().add(new CreateBroadcastifyMenuItem(feed));
                    }
                }

                for(BroadcastServerType type: BroadcastServerType.values())
                {
                    if(type != BroadcastServerType.BROADCASTIFY && type != BroadcastServerType.UNKNOWN)
                    {
                        mNewButton.getItems().add(new CreateBroadcastConfigurationMenuItem(type));
                    }
                }
            });
        }

        return mNewButton;
    }

    /**
     * Refresh broadcastify feeds.
     * @return button to refresh.
     */
    private Button getRefreshButton()
    {
        if(mRefreshButton == null)
        {
            mRefreshButton = new Button("Refresh");
            mRefreshButton.getStyleClass().add("kennebec-toolbar-button");
            mRefreshButton.setTooltip(new Tooltip("Refresh streams available from Broadcastify"));
            mRefreshButton.setOnAction(event -> refreshBroadcastifyStreams());
        }

        return mRefreshButton;
    }

    private Button getDeleteButton()
    {
        if(mDeleteButton == null)
        {
            mDeleteButton = new Button("Delete");
            mDeleteButton.getStyleClass().add("kennebec-toolbar-button");
            mDeleteButton.setMaxWidth(Double.MAX_VALUE);
            mDeleteButton.setTooltip(new Tooltip("Delete the currently selected stream configuration"));
            mDeleteButton.setOnAction(event -> {
                BroadcastConfiguration config = getConfiguredBroadcastTableView().getSelectionModel()
                    .getSelectedItem().getBroadcastConfiguration();

                if(config != null)
                {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                        "Do you want to delete the selected stream?", ButtonType.NO, ButtonType.YES); io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane());
                    alert.setTitle("Delete Stream Configuration");
                    alert.setHeaderText("Are you sure?");
                    alert.initOwner(((Node)getDeleteButton()).getScene().getWindow());

                    Optional<ButtonType> result = alert.showAndWait();

                    if(result.get() == ButtonType.YES)
                    {
                        mPlaylistManager.getBroadcastModel().removeBroadcastConfiguration(config);
                    }
                }
            });
        }

        return mDeleteButton;
    }

    private Button getCloneButton()
    {
        if(mCloneButton == null)
        {
            mCloneButton = new Button("Clone");
            mCloneButton.getStyleClass().add("kennebec-toolbar-button");
            mCloneButton.setMaxWidth(Double.MAX_VALUE);
            mCloneButton.setTooltip(new Tooltip("Create a clone (copy) of the currently selected stream configuration"));
            mCloneButton.setOnAction(event -> {
                ConfiguredBroadcast configuredBroadcast = getConfiguredBroadcastTableView().getSelectionModel()
                    .getSelectedItem();

                if(configuredBroadcast != null && configuredBroadcast.getBroadcastConfiguration() != null)
                {
                    BroadcastConfiguration clonedConfig = configuredBroadcast.getBroadcastConfiguration().copyOf();
                    ConfiguredBroadcast newConfiguredBroadcast = mPlaylistManager.getBroadcastModel()
                        .addBroadcastConfiguration(clonedConfig);
                    getConfiguredBroadcastTableView().getSelectionModel().select(newConfiguredBroadcast);
                }
            });
        }

        return mCloneButton;
    }

    private TableView<ConfiguredBroadcast> getConfiguredBroadcastTableView()
    {
        if(mConfiguredBroadcastTableView == null)
        {
            mConfiguredBroadcastTableView = new TableView<>();
            mConfiguredBroadcastTableView.getStyleClass().add("preferences-table");
            mConfiguredBroadcastTableView.setTableMenuButtonVisible(true);
            mConfiguredBroadcastTableView.setPlaceholder(new Label("Click the New button to create a new " +
                "audio streaming configuration"));
            
            javafx.collections.transformation.SortedList<ConfiguredBroadcast> sortedList = 
                new javafx.collections.transformation.SortedList<>(mFilteredBroadcasts);
            sortedList.comparatorProperty().bind(mConfiguredBroadcastTableView.comparatorProperty());
            mConfiguredBroadcastTableView.setItems(sortedList);
            
            mConfiguredBroadcastTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            mConfiguredBroadcastTableView.setEditable(true);

            TableColumn<ConfiguredBroadcast,Boolean> enabledColumn = new TableColumn<>("Enabled");
            enabledColumn.setId("enabled");
            enabledColumn.setCellValueFactory(new PropertyValueFactory<>("enabled"));
            enabledColumn.setCellFactory(param -> {
                TableCell<ConfiguredBroadcast,Boolean> tableCell = new TableCell<>()
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
                                iconNode = new IconNode(FontAwesome.CHECK);
                                iconNode.setFill(Color.GREEN);
                            }
                            setGraphic(iconNode);
                        }
                    }
                };

                return tableCell;
            });

            TableColumn<ConfiguredBroadcast, String> nameColumn = new TableColumn<>("Name");
            nameColumn.setId("name");
            nameColumn.setPrefWidth(300);
            nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
            nameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
            nameColumn.setOnEditCommit(event -> {
                ConfiguredBroadcast broadcast = event.getRowValue();
                String oldName = event.getOldValue();
                String newName = event.getNewValue();

                if (broadcast != null && broadcast.getBroadcastConfiguration() != null && newName != null && !newName.isEmpty() && !newName.equals(oldName)) {
                    broadcast.getBroadcastConfiguration().setName(newName);

                    if (oldName != null && !oldName.isEmpty() && mPlaylistManager.getAliasModel().hasAliasesWithBroadcastChannel(oldName)) {
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION); io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane());
                        alert.getButtonTypes().clear();
                        alert.getButtonTypes().addAll(ButtonType.NO, ButtonType.YES);
                        alert.setTitle("Update Aliases");
                        alert.setHeaderText("Rename requires updating aliases for this stream");
                        alert.setContentText("Do you want to update aliases to new stream name?");

                        alert.showAndWait().ifPresent(buttonType -> {
                            if (buttonType == ButtonType.YES) {
                                mPlaylistManager.getAliasModel().updateBroadcastChannel(oldName, newName);
                            }
                        });
                    }

                    mPlaylistManager.getBroadcastModel().process(new BroadcastEvent(broadcast.getBroadcastConfiguration(), BroadcastEvent.Event.CONFIGURATION_CHANGE));

                    if (mConfiguredBroadcastTableView.getSelectionModel().getSelectedItem() == broadcast) {
                        setBroadcastConfiguration(broadcast);
                    }
                } else if (broadcast != null && broadcast.getBroadcastConfiguration() != null && (newName == null || newName.isEmpty())) {
                    broadcast.getBroadcastConfiguration().setName(oldName);
                    mConfiguredBroadcastTableView.refresh();
                }
            });

            TableColumn<ConfiguredBroadcast, BroadcastServerType> typeColumn = new TableColumn<>("Format");
            typeColumn.setId("format");
            typeColumn.setPrefWidth(125);
            typeColumn.setCellValueFactory(new PropertyValueFactory<>("broadcastServerType"));
            typeColumn.setCellFactory(param -> new TableCell<ConfiguredBroadcast, BroadcastServerType>() {
                @Override
                protected void updateItem(BroadcastServerType item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(item.toString());
                        if (item.getIconPath() != null) {
                            io.github.dsheirer.icon.Icon icon = new io.github.dsheirer.icon.Icon("empty", item.getIconPath());
                            javafx.scene.image.Image fxImage = icon.getFxImage();
                            if (fxImage != null) {
                                javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(fxImage);
                                imageView.setFitWidth(16);
                                imageView.setFitHeight(16);
                                setGraphic(imageView);
                            } else {
                                setGraphic(null);
                            }
                        } else {
                            setGraphic(null);
                        }
                    }
                }
            });

            TableColumn<ConfiguredBroadcast, BroadcastState> stateColumn = new TableColumn<>("Stream Status");
            stateColumn.setId("status");
            stateColumn.setCellValueFactory(new PropertyValueFactory<>("broadcastState"));
            stateColumn.setCellFactory(column -> new TableCell<>() {
                @Override
                protected void updateItem(BroadcastState item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item.toString());
                        if (item == BroadcastState.CONNECTED) {
                            setStyle("-fx-text-fill: #34C759; -fx-font-weight: bold;");
                        } else if (item == BroadcastState.CONNECTING) {
                            setStyle("-fx-text-fill: #FF9500; -fx-font-weight: bold;");
                        } else if (item.isErrorState()) {
                            setStyle("-fx-text-fill: #FF3B30; -fx-font-weight: bold;");
                        } else if (item.isWarningState()) {
                            setStyle("-fx-text-fill: #FF9500; -fx-font-weight: bold;");
                        } else if (item == BroadcastState.DISABLED) {
                            setStyle("-fx-text-fill: #8E8E93;");
                        } else {
                            setStyle("");
                        }
                    }
                }
            });

            TableColumn<ConfiguredBroadcast, String> errorColumn = new TableColumn<>("Last Error");
            errorColumn.setId("error");
            errorColumn.setPrefWidth(300);
            errorColumn.setCellValueFactory(new PropertyValueFactory<>("lastErrorDetail"));

            mConfiguredBroadcastTableView.getColumns().addAll(enabledColumn, nameColumn, typeColumn, stateColumn, errorColumn);
            new FxTableColumnMonitor(mPlaylistManager.getUserPreferences(), mConfiguredBroadcastTableView, "streamingEditorTable");

            mConfiguredBroadcastTableView.setRowFactory(tableView -> {
                TableRow<ConfiguredBroadcast> row = new TableRow<>();
                ContextMenu contextMenu = new ContextMenu();
                MenuItem enableMenuItem = new MenuItem("Enable");
                MenuItem disableMenuItem = new MenuItem("Disable");
                MenuItem reconnectMenuItem = new MenuItem("Reconnect");
                MenuItem configureMenuItem = new MenuItem("Configure");

                enableMenuItem.setOnAction(event -> {
                    ConfiguredBroadcast item = row.getItem();
                    if (item != null && item.getBroadcastConfiguration() != null) {
                        item.getBroadcastConfiguration().setEnabled(true);
                        mPlaylistManager.getBroadcastModel().process(
                            new BroadcastEvent(item.getBroadcastConfiguration(), BroadcastEvent.Event.CONFIGURATION_CHANGE));
                    }
                });

                disableMenuItem.setOnAction(event -> {
                    ConfiguredBroadcast item = row.getItem();
                    if (item != null && item.getBroadcastConfiguration() != null) {
                        item.getBroadcastConfiguration().setEnabled(false);
                        mPlaylistManager.getBroadcastModel().process(
                            new BroadcastEvent(item.getBroadcastConfiguration(), BroadcastEvent.Event.CONFIGURATION_CHANGE));
                    }
                });

                reconnectMenuItem.setOnAction(event -> {
                    ConfiguredBroadcast item = row.getItem();
                    if (item != null && item.getBroadcastConfiguration() != null) {
                        if (item.getBroadcastConfiguration().isEnabled()) {
                            mPlaylistManager.getBroadcastModel().process(
                                new BroadcastEvent(item.getBroadcastConfiguration(), BroadcastEvent.Event.CONFIGURATION_CHANGE));
                        }
                    }
                });

                configureMenuItem.setOnAction(event -> {
                    ConfiguredBroadcast item = row.getItem();
                    if (item != null) {
                        mConfiguredBroadcastTableView.getSelectionModel().select(item);
                        getTabPane().getSelectionModel().select(getConfigurationTab());
                    }
                });

                row.itemProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue == null || newValue.getBroadcastConfiguration() == null) {
                        row.contextMenuProperty().unbind();
                        row.setContextMenu(null);
                    } else {
                        row.contextMenuProperty().bind(
                            javafx.beans.binding.Bindings.when(row.emptyProperty())
                                .then((ContextMenu) null)
                                .otherwise(contextMenu)
                        );
                        enableMenuItem.disableProperty().bind(newValue.enabledProperty());
                        disableMenuItem.disableProperty().bind(newValue.enabledProperty().not());
                        reconnectMenuItem.disableProperty().bind(newValue.enabledProperty().not());
                    }
                });

                contextMenu.getItems().addAll(enableMenuItem, disableMenuItem, reconnectMenuItem, new SeparatorMenuItem(), configureMenuItem);
                return row;
            });

            mConfiguredBroadcastTableView.getSelectionModel().selectedItemProperty()
                    .addListener((observable, oldValue, newValue) -> setBroadcastConfiguration(newValue));
        }

        return mConfiguredBroadcastTableView;
    }

    /**
     * Menu item to create a broadcastify configuration
     */
    public class CreateBroadcastifyMenuItem extends MenuItem
    {
        private UserFeedBroadcast mUserFeedBroadcast;

        public CreateBroadcastifyMenuItem(UserFeedBroadcast userFeedBroadcast)
        {
            mUserFeedBroadcast = userFeedBroadcast;
            setText("Broadcastify Feed: " + mUserFeedBroadcast.getDescription());
            if (BroadcastServerType.BROADCASTIFY.getIconPath() != null) {
                io.github.dsheirer.icon.Icon icon = new io.github.dsheirer.icon.Icon("empty", BroadcastServerType.BROADCASTIFY.getIconPath());
                javafx.scene.image.Image fxImage = icon.getFxImage();
                if (fxImage != null) {
                    javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(fxImage);
                    imageView.setFitWidth(16);
                    imageView.setFitHeight(16);
                    setGraphic(imageView);
                }
            }
            setOnAction(event -> {
                BroadcastConfiguration configuration = BroadcastifyFeedConfiguration.from(mUserFeedBroadcast);

                if(configuration != null)
                {
                    ConfiguredBroadcast configuredBroadcast = mPlaylistManager.getBroadcastModel()
                        .addBroadcastConfiguration(configuration);
                    getConfiguredBroadcastTableView().getSelectionModel().select(configuredBroadcast);
                }
            });
        }
    }

    /**
     * Menu item to create a new broadcast configuration
     */
    public class CreateBroadcastConfigurationMenuItem extends MenuItem
    {
        private BroadcastServerType mBroadcastServerType;

        public CreateBroadcastConfigurationMenuItem(BroadcastServerType type)
        {
            setText(type.toString());
            mBroadcastServerType = type;

            if (mBroadcastServerType.getIconPath() != null) {
                io.github.dsheirer.icon.Icon icon = new io.github.dsheirer.icon.Icon("empty", mBroadcastServerType.getIconPath());
                javafx.scene.image.Image fxImage = icon.getFxImage();
                if (fxImage != null) {
                    javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(fxImage);
                    imageView.setFitWidth(16);
                    imageView.setFitHeight(16);
                    setGraphic(imageView);
                }
            }
            setOnAction(event -> {
                BroadcastConfiguration config = BroadcastFactory.getConfiguration(mBroadcastServerType, BroadcastFormat.MP3);
                ConfiguredBroadcast configuredBroadcast = mPlaylistManager.getBroadcastModel()
                    .addBroadcastConfiguration(config);
                getConfiguredBroadcastTableView().getSelectionModel().select(configuredBroadcast);
            });
        }
    }

    public class StreamConfigurationEditorModificationListener implements ChangeListener<Boolean>
    {
        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
        {
            //Only fire when the modification property changes from true to false.  Set the selection to null and then
            //reselect the broadcast to get the streams tab to refresh
            if(oldValue != null && newValue != null && oldValue && !newValue)
            {
                ConfiguredBroadcast configuredBroadcast = getConfiguredBroadcastTableView().getSelectionModel().getSelectedItem();

                if(configuredBroadcast != null)
                {
                    getConfiguredBroadcastTableView().getSelectionModel().select(null);
                    getConfiguredBroadcastTableView().getSelectionModel().select(configuredBroadcast);
                }
            }
        }
    }
}
