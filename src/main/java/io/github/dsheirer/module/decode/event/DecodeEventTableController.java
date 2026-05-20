package io.github.dsheirer.module.decode.event;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.preference.UserPreferences;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;

public class DecodeEventTableController {
    @FXML private TableView<IDecodeEvent> eventTable;
    @FXML private TableColumn<IDecodeEvent, Long> timeColumn;
    @FXML private TableColumn<IDecodeEvent, Long> durationColumn;
    @FXML private TableColumn<IDecodeEvent, String> eventColumn;
    @FXML private TableColumn<IDecodeEvent, IdentifierCollection> fromIdColumn;
    @FXML private TableColumn<IDecodeEvent, IdentifierCollection> fromAliasColumn;
    @FXML private TableColumn<IDecodeEvent, IdentifierCollection> toIdColumn;
    @FXML private TableColumn<IDecodeEvent, IdentifierCollection> toAliasColumn;
    @FXML private TableColumn<IDecodeEvent, String> channelColumn;
    @FXML private TableColumn<IDecodeEvent, String> frequencyColumn;
    @FXML private TableColumn<IDecodeEvent, String> detailsColumn;

    private FilteredList<IDecodeEvent> mFilteredItems;
    private SimpleDateFormat mTimeFormatter = new SimpleDateFormat("HH:mm:ss");
    private DecimalFormat mDurationFormatter = new DecimalFormat("0.0 s");
    private UserPreferences mUserPreferences;
    private AliasModel mAliasModel;

    public void initialize(UserPreferences userPreferences, AliasModel aliasModel) {
        mUserPreferences = userPreferences;
        mAliasModel = aliasModel;

        timeColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getTimeStart()));
        timeColumn.setCellFactory(col -> new TableCell<IDecodeEvent, Long>() {
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    if (mUserPreferences != null && false) {
                        setText(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date(item)));
                    } else {
                        setText(mTimeFormatter.format(new Date(item)));
                    }
                }
            }
        });

        durationColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getDuration()));
        durationColumn.setCellFactory(col -> new TableCell<IDecodeEvent, Long>() {
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(mDurationFormatter.format(item / 1000.0));
                }
            }
        });

        eventColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getEventType().getLabel()));

        fromIdColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getIdentifierCollection()));
        fromIdColumn.setCellFactory(col -> createIdCell(Role.FROM));

        fromAliasColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getIdentifierCollection()));
        fromAliasColumn.setCellFactory(col -> createAliasCell(Role.FROM));

        toIdColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getIdentifierCollection()));
        toIdColumn.setCellFactory(col -> createIdCell(Role.TO));

        toAliasColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getIdentifierCollection()));
        toAliasColumn.setCellFactory(col -> createAliasCell(Role.TO));

        channelColumn.setCellValueFactory(cellData -> {
            IDecodeEvent event = cellData.getValue();
            IChannelDescriptor cd = event.getChannelDescriptor();
            if (cd != null) {
                if (event.hasTimeslot() && !event.toString().contains("TS")) {
                    return new SimpleStringProperty(cd.toString() + " TS" + event.getTimeslot());
                }
                return new SimpleStringProperty(cd.toString());
            } else if (event.hasTimeslot() && !event.toString().contains("TS")) {
                return new SimpleStringProperty("TS" + event.getTimeslot());
            }
            return new SimpleStringProperty("");
        });

        frequencyColumn.setCellValueFactory(cellData -> {
            IChannelDescriptor cd = cellData.getValue().getChannelDescriptor();
            if (cd != null) {
                return new SimpleStringProperty(cd.toString());
            }
            return new SimpleStringProperty("");
        });

        detailsColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDetails()));
    }

    private TableCell<IDecodeEvent, IdentifierCollection> createIdCell(Role role) {
        return new TableCell<IDecodeEvent, IdentifierCollection>() {
            @Override
            protected void updateItem(IdentifierCollection item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    if (mUserPreferences != null) {
                        setText(item.toString());
                    } else {
                        setText(item.toString());
                    }
                }
            }
        };
    }

    private TableCell<IDecodeEvent, IdentifierCollection> createAliasCell(Role role) {
        return new TableCell<IDecodeEvent, IdentifierCollection>() {
            @Override
            protected void updateItem(IdentifierCollection item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    if (mAliasModel != null) {
                        List<Alias> aliases = java.util.Collections.emptyList();
                        if (!aliases.isEmpty()) {
                            setText(aliases.get(0).getName());
                        } else {
                            setText(null);
                        }
                    } else {
                        setText(null);
                    }
                }
            }
        };
    }

    public void setItems(ObservableList<IDecodeEvent> items) {
        mFilteredItems = new FilteredList<>(items, p -> true);
        SortedList<IDecodeEvent> sortedData = new SortedList<>(mFilteredItems);
        sortedData.comparatorProperty().bind(eventTable.comparatorProperty());
        eventTable.setItems(sortedData);
    }

    public void setFilterPredicate(Predicate<IDecodeEvent> predicate) {
        if (mFilteredItems != null) {
            mFilteredItems.setPredicate(predicate);
        }
    }

    public void updateFilter() {
        if (mFilteredItems != null) {
            mFilteredItems.setPredicate(mFilteredItems.getPredicate());
        }
        eventTable.refresh();
    }
}
