package io.github.dsheirer.module.decode.event;

import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.filter.FilterSet;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.embed.swing.SwingNode;
import javafx.application.Platform;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

public class MessageActivityPanelController {

    @FXML
    private VBox mainContainer;

    @FXML
    private SwingNode historyManagementNode;

    @FXML
    private TableView<MessageItem> tableView;

    private MessageActivityModel mMessageModel;
    private ObservableList<MessageItem> rowData = FXCollections.observableArrayList();
    private FilteredList<MessageItem> filteredData;
    private FilterSet<IMessage> mMessageFilterSet;

    public void init(MessageActivityModel messageModel, HistoryManagementPanel<IMessage> historyManagementPanel) {
        mMessageModel = messageModel;

        javax.swing.SwingUtilities.invokeLater(() -> {
            historyManagementNode.setContent(historyManagementPanel);
        });

        for (int i = 0; i < mMessageModel.getRowCount(); i++) {
            rowData.add(mMessageModel.getItem(i));
        }

        mMessageModel.addTableModelListener(e -> {
            Platform.runLater(() -> {
                if (e.getType() == TableModelEvent.INSERT) {
                    for (int i = e.getFirstRow(); i <= e.getLastRow(); i++) {
                        MessageItem item = mMessageModel.getItem(i);
                        if (item != null) {
                            rowData.add(i, item);
                        }
                    }
                } else if (e.getType() == TableModelEvent.DELETE) {
                    for (int i = e.getLastRow(); i >= e.getFirstRow(); i--) {
                        if (i < rowData.size() && i >= 0) {
                            rowData.remove(i);
                        }
                    }
                } else if (e.getType() == TableModelEvent.UPDATE) {
                    if (e.getFirstRow() == 0 && e.getLastRow() == Integer.MAX_VALUE) {
                        // Complete refresh
                        rowData.clear();
                        for (int i = 0; i < mMessageModel.getRowCount(); i++) {
                            rowData.add(mMessageModel.getItem(i));
                        }
                    } else {
                        for (int i = e.getFirstRow(); i <= e.getLastRow(); i++) {
                            if (i < rowData.size() && i >= 0) {
                                MessageItem item = mMessageModel.getItem(i);
                                if (item != null) {
                                    rowData.set(i, item);
                                }
                            }
                        }
                    }
                    tableView.refresh();
                }
            });
        });

        filteredData = new FilteredList<>(rowData, p -> checkFilter(p));
        SortedList<MessageItem> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableView.comparatorProperty());
        tableView.setItems(sortedData);

        setupColumns();
    }

    private void setupColumns() {
        TableColumn<MessageItem, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(cellData -> {
            MessageItem item = cellData.getValue();
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
            return new SimpleStringProperty(item.getTimestamp(sdf));
        });

        TableColumn<MessageItem, String> protoCol = new TableColumn<>("Protocol");
        protoCol.setCellValueFactory(cellData -> {
            MessageItem item = cellData.getValue();
            return new SimpleStringProperty(item.getProtocol());
        });

        TableColumn<MessageItem, String> tsCol = new TableColumn<>("Timeslot");
        tsCol.setCellValueFactory(cellData -> {
            MessageItem item = cellData.getValue();
            return new SimpleStringProperty(String.valueOf(item.getTimeslot()));
        });

        TableColumn<MessageItem, String> msgCol = new TableColumn<>("Message");
        msgCol.setCellValueFactory(cellData -> {
            MessageItem item = cellData.getValue();
            return new SimpleStringProperty(item.getText());
        });

        tableView.getColumns().add(timeCol);
        tableView.getColumns().add(protoCol);
        tableView.getColumns().add(tsCol);
        tableView.getColumns().add(msgCol);
    }

    public void setMessageFilterSet(FilterSet<IMessage> filterSet) {
        mMessageFilterSet = filterSet;
        filteredData.setPredicate(p -> checkFilter(p));
    }

    public void refreshFilter() {
        filteredData.setPredicate(p -> checkFilter(p));
    }

    private boolean checkFilter(MessageItem item) {
        if (mMessageFilterSet == null) return true;

        if (item != null && item.getMessage() != null) {
            IMessage message = item.getMessage();
            return mMessageFilterSet.canProcess(message) && mMessageFilterSet.passes(message);
        }
        return false;
    }

    public TableView<MessageItem> getTable() {
        return tableView;
    }
}
