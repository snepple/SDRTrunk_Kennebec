
package io.github.dsheirer.audio.broadcast;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;
import javafx.scene.control.Button;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.audio.broadcast.BroadcastState;
import io.github.dsheirer.audio.broadcast.BroadcastServerType;
import io.github.dsheirer.audio.broadcast.BroadcastConfiguration;
import io.github.dsheirer.audio.broadcast.BroadcastEvent;
import io.github.dsheirer.eventbus.MyEventBus;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;

import java.util.ArrayList;
import java.util.List;

public class BroadcastStatusPanelController {

    @FXML
    private TableView<BroadcastModelRow> tableView;

    @FXML
    private VBox mainContainer;

    //Transient "connecting" progress indicator shown only while one or more streams are connecting.
    private HBox mConnectingIndicator;
    private ProgressIndicator mConnectingProgress;
    private Label mConnectingLabel;

    private BroadcastModel mBroadcastModel;
    private UserPreferences mUserPreferences;
    private String mPreferenceKey;

    private ObservableList<BroadcastModelRow> rowData = FXCollections.observableArrayList();
    private FilteredList<BroadcastModelRow> filteredData;
    private boolean hideDisabled = false;

    private final java.util.concurrent.atomic.AtomicBoolean mRefreshDirty = new java.util.concurrent.atomic.AtomicBoolean(false);
    private javafx.animation.Timeline mRefreshTimer;

    public static class BroadcastModelRow {
        private final String streamName;

        public BroadcastModelRow(String streamName) {
            this.streamName = streamName;
        }

        public String getStreamName() {
            return streamName;
        }

        public int getRowIndex(BroadcastModel model) {
            for (int i = 0; i < model.getRowCount(); i++) {
                if (streamName.equals(model.getValueAt(i, BroadcastModel.COLUMN_STREAM_NAME))) {
                    return i;
                }
            }
            return -1;
        }
    }

    public void init(BroadcastModel broadcastModel, UserPreferences userPreferences, String preferenceKey) {
        mBroadcastModel = broadcastModel;
        mUserPreferences = userPreferences;
        mPreferenceKey = preferenceKey;

        rebuildRows();

        //Build the (initially hidden) connecting-progress indicator above the table.
        mConnectingProgress = new ProgressIndicator();
        mConnectingProgress.setPrefSize(16, 16);
        mConnectingProgress.setMinSize(16, 16);
        mConnectingLabel = new Label("Connecting streams…");
        mConnectingLabel.getStyleClass().add("kennebec-secondary-text");
        mConnectingIndicator = new HBox(8, mConnectingProgress, mConnectingLabel);
        mConnectingIndicator.setAlignment(Pos.CENTER_LEFT);
        mConnectingIndicator.setPadding(new Insets(4, 10, 4, 10));
        mConnectingIndicator.setVisible(false);
        mConnectingIndicator.setManaged(false);

        if(mainContainer != null)
        {
            mainContainer.getChildren().add(0, mConnectingIndicator);
        }

        //Rebuild the row list only when streams are actually added/removed/reordered.  Property
        //updates (name, state, etc.) are handled by the throttled refresh below so we don't pay
        //the cost of clearing and re-adding every row on each event.
        mBroadcastModel.getConfiguredBroadcasts().addListener((javafx.collections.ListChangeListener<ConfiguredBroadcast>) c -> {
            boolean structural = false;
            while (c.next()) {
                if (c.wasAdded() || c.wasRemoved() || c.wasPermutated()) {
                    structural = true;
                }
            }
            if (structural) {
                Platform.runLater(this::rebuildRows);
            } else {
                scheduleRefresh();
            }
        });

        //The streamed/users/queue/error columns are read on-demand from the model and are not part
        //of the observable extractor, so the table is never told they changed.  Subscribe to the
        //model's broadcast events and refresh the visible cells on a throttled cadence.  Coalescing
        //to once per second keeps cost bounded no matter how many streams are reporting updates.
        mBroadcastModel.addListener((io.github.dsheirer.sample.Listener<BroadcastEvent>) event -> scheduleRefresh());

        mRefreshTimer = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> {
            if (mRefreshDirty.getAndSet(false) && tableView != null) {
                tableView.refresh();
            }
            updateConnectingIndicator();
        }));
        mRefreshTimer.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        mRefreshTimer.play();

        filteredData = new FilteredList<>(rowData, p -> true);
        SortedList<BroadcastModelRow> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableView.comparatorProperty());

        tableView.setItems(sortedData);

        setupColumns();
        setupContextMenu();
    }

    /**
     * Updates the connecting-progress indicator: visible only while one or more streams are in the CONNECTING state,
     * hidden once all streams have settled (connected or errored).
     */
    private void updateConnectingIndicator() {
        if (mConnectingIndicator == null || mBroadcastModel == null) {
            return;
        }

        int connecting = 0;

        for (ConfiguredBroadcast configuredBroadcast : mBroadcastModel.getConfiguredBroadcasts()) {
            try {
                if (configuredBroadcast.broadcastStateProperty().get() == BroadcastState.CONNECTING) {
                    connecting++;
                }
            } catch (Throwable t) {
                //Ignore an individual broadcaster whose state can't be read
            }
        }

        boolean show = connecting > 0;
        mConnectingIndicator.setVisible(show);
        mConnectingIndicator.setManaged(show);

        if (show) {
            mConnectingLabel.setText("Connecting streams… " + connecting + " remaining");
        }
    }

    private void setupColumns() {
        for (int i = 0; i < mBroadcastModel.getColumnCount(); i++) {
            final int colIndex = i;

            if (i == BroadcastModel.COLUMN_BROADCAST_SERVER_TYPE) {
                TableColumn<BroadcastModelRow, BroadcastServerType> col = new TableColumn<>(mBroadcastModel.getColumnName(i));
                col.setCellValueFactory(cellData -> {
                    int rowIndex = cellData.getValue().getRowIndex(mBroadcastModel);
                    if (rowIndex >= 0) {
                        return new SimpleObjectProperty<>((BroadcastServerType) mBroadcastModel.getValueAt(rowIndex, colIndex));
                    }
                    return new SimpleObjectProperty<>(null);
                });
                col.setCellFactory(column -> new TableCell<BroadcastModelRow, BroadcastServerType>() {
                    @Override
                    protected void updateItem(BroadcastServerType item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            setText(item.toString());
                            if (item.getIconPath() != null) {
                                try {
                                    java.net.URL url = getClass().getResource(item.getIconPath());
                                    if (url != null) {
                                        ImageView iv = new ImageView(new Image(url.toExternalForm(), 16, 16, true, true));
                                        iv.setFitWidth(16);
                                        iv.setFitHeight(16);
                                        setGraphic(iv);
                                    } else {
                                        setGraphic(null);
                                    }
                                } catch (Exception e) {
                                    setGraphic(null);
                                }
                            } else {
                                setGraphic(null);
                            }
                        }
                    }
                });
                tableView.getColumns().add(col);
            } else if (i == BroadcastModel.COLUMN_BROADCASTER_STATUS) {
                TableColumn<BroadcastModelRow, BroadcastState> col = new TableColumn<>(mBroadcastModel.getColumnName(i));
                col.setCellValueFactory(cellData -> {
                    int rowIndex = cellData.getValue().getRowIndex(mBroadcastModel);
                    if (rowIndex >= 0) {
                        return new SimpleObjectProperty<>((BroadcastState) mBroadcastModel.getValueAt(rowIndex, colIndex));
                    }
                    return new SimpleObjectProperty<>(null);
                });
                col.setCellFactory(column -> new TableCell<BroadcastModelRow, BroadcastState>() {
                    @Override
                    protected void updateItem(BroadcastState state, boolean empty) {
                        super.updateItem(state, empty);
                        if (empty || state == null) {
                            setText(null);
                            setStyle("");
                        } else {
                            setText(state.toString());
                            if (state == BroadcastState.CONNECTED) {
                                setStyle("-fx-background-color: #00FF00; -fx-text-fill: black;");
                            } else if (state == BroadcastState.DISABLED) {
                                setStyle("-fx-text-fill: lightgray;");
                            } else if (state == BroadcastState.INVALID_SETTINGS || state == BroadcastState.NETWORK_UNAVAILABLE) {
                                setStyle("-fx-background-color: yellow; -fx-text-fill: black;");
                            } else if (state.isErrorState()) {
                                setStyle("-fx-background-color: red; -fx-text-fill: black;");
                            } else {
                                setStyle("");
                            }
                        }
                    }
                });
                tableView.getColumns().add(col);
            } else {
                TableColumn<BroadcastModelRow, Comparable> col = new TableColumn<>(mBroadcastModel.getColumnName(i));
                col.setCellValueFactory(cellData -> {
                    int rowIndex = cellData.getValue().getRowIndex(mBroadcastModel);
                    if (rowIndex >= 0) {
                        Object val = mBroadcastModel.getValueAt(rowIndex, colIndex);
                        if (val instanceof Comparable) {
                            return new SimpleObjectProperty<>((Comparable)val);
                        }
                        return new SimpleObjectProperty<>(val != null ? val.toString() : "");
                    }
                    return new SimpleObjectProperty<>(null);
                });
                col.setCellFactory(column -> new TableCell<BroadcastModelRow, Comparable>() {
                    @Override
                    protected void updateItem(Comparable item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            setText(item.toString());
                        }
                    }
                });
                tableView.getColumns().add(col);
            }
        }
    }

    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        CheckMenuItem hideDisabledItem = new CheckMenuItem("Hide Disabled Streams");
        hideDisabledItem.setOnAction(e -> {
            hideDisabled = hideDisabledItem.isSelected();
            filteredData.setPredicate(row -> {
                if (!hideDisabled) return true;
                int rowIndex = row.getRowIndex(mBroadcastModel);
                if (rowIndex >= 0) {
                    BroadcastState state = (BroadcastState) mBroadcastModel.getValueAt(rowIndex, BroadcastModel.COLUMN_BROADCASTER_STATUS);
                    return state != BroadcastState.DISABLED;
                }
                return true;
            });
        });
        contextMenu.getItems().add(hideDisabledItem);

        tableView.setContextMenu(contextMenu);

        tableView.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                BroadcastModelRow selectedRow = tableView.getSelectionModel().getSelectedItem();
                if (selectedRow != null) {
                    String streamName = selectedRow.getStreamName();
                    BroadcastConfiguration config = mBroadcastModel.getBroadcastConfiguration(streamName);
                    if (config != null) {
                        ContextMenu rowMenu = new ContextMenu();
                        CheckMenuItem enableItem = new CheckMenuItem("Enable");
                        enableItem.setSelected(config.isEnabled());
                        enableItem.setOnAction(evt -> {
                            config.setEnabled(enableItem.isSelected());
                            mBroadcastModel.process(new BroadcastEvent(config, BroadcastEvent.Event.CONFIGURATION_CHANGE));
                        });
                        rowMenu.getItems().add(enableItem);
                        rowMenu.show(tableView, e.getScreenX(), e.getScreenY());
                    }
                }
            } else if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                BroadcastModelRow selectedRow = tableView.getSelectionModel().getSelectedItem();
                if (selectedRow != null) {
                    String streamName = selectedRow.getStreamName();
                    BroadcastConfiguration config = mBroadcastModel.getBroadcastConfiguration(streamName);
                    if (config != null) {
                        MyEventBus.getGlobalEventBus().post(new io.github.dsheirer.gui.playlist.streaming.ViewStreamRequest(config));
                    }
                }
            }
        });
    }

    public TableView<BroadcastModelRow> getTable() {
        return tableView;
    }

    /**
     * Rebuilds the backing row list from the model.  Called on initial load and whenever streams
     * are added, removed, or reordered.  Must run on the JavaFX Application Thread.
     */
    private void rebuildRows() {
        rowData.clear();
        for (int i = 0; i < mBroadcastModel.getRowCount(); i++) {
            String name = (String) mBroadcastModel.getValueAt(i, BroadcastModel.COLUMN_STREAM_NAME);
            rowData.add(new BroadcastModelRow(name));
        }
        if (tableView != null) {
            tableView.refresh();
        }
    }

    /**
     * Marks the table as needing a cell refresh.  The actual refresh is performed at most once per
     * second by the refresh timer, so a burst of model events from many streams collapses into a
     * single re-render rather than one per event.
     */
    private void scheduleRefresh() {
        mRefreshDirty.set(true);
    }
}
