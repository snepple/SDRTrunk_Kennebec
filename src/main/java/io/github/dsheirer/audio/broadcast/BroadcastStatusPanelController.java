
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

    private BroadcastModel mBroadcastModel;
    private UserPreferences mUserPreferences;
    private String mPreferenceKey;

    private ObservableList<BroadcastModelRow> rowData = FXCollections.observableArrayList();
    private FilteredList<BroadcastModelRow> filteredData;
    private boolean hideDisabled = false;

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

        for (int i = 0; i < mBroadcastModel.getRowCount(); i++) {
            String name = (String) mBroadcastModel.getValueAt(i, BroadcastModel.COLUMN_STREAM_NAME);
            rowData.add(new BroadcastModelRow(name));
        }

        mBroadcastModel.getConfiguredBroadcasts().addListener((javafx.collections.ListChangeListener<ConfiguredBroadcast>) c -> {
            Platform.runLater(() -> {
                rowData.clear();
                for (int i = 0; i < mBroadcastModel.getRowCount(); i++) {
                    String name = (String) mBroadcastModel.getValueAt(i, BroadcastModel.COLUMN_STREAM_NAME);
                    rowData.add(new BroadcastModelRow(name));
                }
                tableView.refresh();
            });
        });

        filteredData = new FilteredList<>(rowData, p -> true);
        SortedList<BroadcastModelRow> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableView.comparatorProperty());

        tableView.setItems(sortedData);

        setupColumns();
        setupContextMenu();
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
}
