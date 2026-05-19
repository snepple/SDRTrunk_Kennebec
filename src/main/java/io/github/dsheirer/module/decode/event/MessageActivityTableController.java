package io.github.dsheirer.module.decode.event;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.text.SimpleDateFormat;
import java.util.function.Predicate;

public class MessageActivityTableController {
    @FXML private TableView<MessageItem> messageTable;
    @FXML private TableColumn<MessageItem, String> timeColumn;
    @FXML private TableColumn<MessageItem, String> protocolColumn;
    @FXML private TableColumn<MessageItem, String> timeslotColumn;
    @FXML private TableColumn<MessageItem, String> messageColumn;

    private FilteredList<MessageItem> mFilteredItems;
    private SimpleDateFormat mSDFTime = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");

    public void initialize() {
        timeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTimestamp(mSDFTime)));
        protocolColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getProtocol()));
        timeslotColumn.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getTimeslot())));
        messageColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getText()));
    }

    public void setItems(ObservableList<MessageItem> items) {
        mFilteredItems = new FilteredList<>(items, p -> true);
        SortedList<MessageItem> sortedData = new SortedList<>(mFilteredItems);
        sortedData.comparatorProperty().bind(messageTable.comparatorProperty());
        messageTable.setItems(sortedData);
    }

    public void setFilterPredicate(Predicate<MessageItem> predicate) {
        if (mFilteredItems != null) {
            mFilteredItems.setPredicate(predicate);
        }
    }

    public void updateFilter() {
        if (mFilteredItems != null) {
            // trigger filter refresh
            mFilteredItems.setPredicate(mFilteredItems.getPredicate());
        }
    }
}
