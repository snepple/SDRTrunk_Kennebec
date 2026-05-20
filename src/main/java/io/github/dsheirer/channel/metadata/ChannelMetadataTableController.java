package io.github.dsheirer.channel.metadata;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.channel.state.State;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.configuration.FrequencyConfigurationIdentifier;
import io.github.dsheirer.identifier.decoder.DecoderLogicalChannelNameIdentifier;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.identifier.TalkgroupFormatPreference;
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
import java.util.List;

public class ChannelMetadataTableController {
    @FXML private TableView<ChannelMetadata> metadataTable;
    @FXML private TableColumn<ChannelMetadata, ChannelMetadata> statusColumn;
    @FXML private TableColumn<ChannelMetadata, Identifier> decoderColumn;
    @FXML private TableColumn<ChannelMetadata, ChannelMetadata> fromIdColumn;
    @FXML private TableColumn<ChannelMetadata, List<Alias>> fromAliasColumn;
    @FXML private TableColumn<ChannelMetadata, ChannelMetadata> toIdColumn;
    @FXML private TableColumn<ChannelMetadata, List<Alias>> toAliasColumn;
    @FXML private TableColumn<ChannelMetadata, DecoderLogicalChannelNameIdentifier> channelNameColumn;
    @FXML private TableColumn<ChannelMetadata, Identifier> frequencyColumn;
    @FXML private TableColumn<ChannelMetadata, Identifier> channelColumn;

    private FilteredList<ChannelMetadata> mFilteredItems;
    private UserPreferences mUserPreferences;
    private IconModel mIconModel;

    public void initialize(UserPreferences userPreferences, IconModel iconModel) {
        mUserPreferences = userPreferences;
        mIconModel = iconModel;

        statusColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue()));
        statusColumn.setCellFactory(col -> new TableCell<ChannelMetadata, ChannelMetadata>() {
            @Override
            protected void updateItem(ChannelMetadata item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.getChannelStateIdentifier().getValue() == null) {
                    setText(null);
                    setStyle("");
                } else {
                    State state = item.getChannelStateIdentifier().getValue();
                    setText(state.toString());

                    String bgColor = "#ffffff";
                    String fgColor = "#000000";
                    switch(state) {
                        case ACTIVE: bgColor = "cyan"; fgColor = "blue"; break;
                        case CALL: bgColor = "blue"; fgColor = "yellow"; break;
                        case CONTROL: bgColor = "orange"; fgColor = "blue"; break;
                        case DATA: bgColor = "green"; fgColor = "blue"; break;
                        case ENCRYPTED: bgColor = "magenta"; fgColor = "white"; break;
                        case FADE: bgColor = "lightgray"; fgColor = "darkgray"; break;
                        case IDLE: bgColor = "white"; fgColor = "darkgray"; break;
                        case RESET: bgColor = "pink"; fgColor = "yellow"; break;
                        case TEARDOWN: bgColor = "darkgray"; fgColor = "white"; break;
                    }
                    setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: " + fgColor + "; -fx-alignment: center;");
                }
            }
        });

        decoderColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getDecoderTypeConfigurationIdentifier()));
        decoderColumn.setCellFactory(col -> createIdentifierCell());

        fromIdColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue()));
        fromIdColumn.setCellFactory(col -> new TableCell<ChannelMetadata, ChannelMetadata>() {
            @Override
            protected void updateItem(ChannelMetadata item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getFromIdentifier() != null ? item.getFromIdentifier().toString() : "");
                }
            }
        });

        fromAliasColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getFromIdentifierAliases()));
        fromAliasColumn.setCellFactory(col -> createAliasCell());

        toIdColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue()));
        toIdColumn.setCellFactory(col -> new TableCell<ChannelMetadata, ChannelMetadata>() {
            @Override
            protected void updateItem(ChannelMetadata item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getToIdentifier() != null ? item.getToIdentifier().toString() : "");
                }
            }
        });

        toAliasColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getToIdentifierAliases()));
        toAliasColumn.setCellFactory(col -> createAliasCell());

        channelNameColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getDecoderLogicalChannelNameIdentifier()));
        channelNameColumn.setCellFactory(col -> new TableCell<ChannelMetadata, DecoderLogicalChannelNameIdentifier>() {
            @Override
            protected void updateItem(DecoderLogicalChannelNameIdentifier item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item.toString());
            }
        });

        frequencyColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getFrequencyConfigurationIdentifier()));
        frequencyColumn.setCellFactory(col -> new TableCell<ChannelMetadata, Identifier>() {
            private final DecimalFormat FREQUENCY_FORMATTER = new DecimalFormat("#.00000");
            @Override
            protected void updateItem(Identifier item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else if (item instanceof FrequencyConfigurationIdentifier) {
                    long freq = ((FrequencyConfigurationIdentifier)item).getValue();
                    setText(FREQUENCY_FORMATTER.format(freq / 1e6d));
                } else {
                    setText(item.toString());
                }
            }
        });

        channelColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getChannelNameConfigurationIdentifier()));
        channelColumn.setCellFactory(col -> createIdentifierCell());
    }

    private TableCell<ChannelMetadata, Identifier> createIdentifierCell() {
        return new TableCell<ChannelMetadata, Identifier>() {
            @Override
            protected void updateItem(Identifier item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item.toString());
            }
        };
    }

    private TableCell<ChannelMetadata, List<Alias>> createAliasCell() {
        return new TableCell<ChannelMetadata, List<Alias>>() {
            @Override
            protected void updateItem(List<Alias> aliases, boolean empty) {
                super.updateItem(aliases, empty);
                if (empty || aliases == null || aliases.isEmpty()) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Alias first = aliases.get(0);
                    setText(first.getName());
                }
            }
        };
    }

    public void setItems(ObservableList<ChannelMetadata> items) {
        mFilteredItems = new FilteredList<>(items, p -> true);
        SortedList<ChannelMetadata> sortedData = new SortedList<>(mFilteredItems);
        sortedData.comparatorProperty().bind(metadataTable.comparatorProperty());
        metadataTable.setItems(sortedData);
    }

    public TableView<ChannelMetadata> getTableView() {
        return metadataTable;
    }
}
