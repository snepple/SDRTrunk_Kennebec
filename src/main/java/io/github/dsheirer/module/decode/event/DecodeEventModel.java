
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
package io.github.dsheirer.module.decode.event;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;
import javafx.scene.control.Label;

import com.google.common.base.Joiner;
import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.sample.Listener;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decode event table model
 */
public class DecodeEventModel extends ClearableHistoryModel<IDecodeEvent> implements Listener<IDecodeEvent>
{
    private static final long serialVersionUID = 1L;
    private final static Logger mLog = LoggerFactory.getLogger(DecodeEventModel.class);
    public static final int COLUMN_TIME = 0;
    public static final int COLUMN_DURATION = 1;
    public static final int COLUMN_EVENT = 2;
    public static final int COLUMN_FROM_ID = 3;
    public static final int COLUMN_FROM_ALIAS = 4;
    public static final int COLUMN_TO_ID = 5;
    public static final int COLUMN_TO_ALIAS = 6;
    public static final int COLUMN_CHANNEL = 7;
    public static final int COLUMN_FREQUENCY = 8;
    public static final int COLUMN_DETAILS = 9;
    protected String[] mHeaders = new String[]{"Time", "Duration", "Event", "From", "Alias", "To", "Alias", "Channel", "Frequency", "Details"};

    public DecodeEventModel()
    {
        MyEventBus.getGlobalEventBus().register(this);
    }

    /**
     * Receives preference update notifications via the event bus
     * @param preferenceType that was updated
     */
    @Subscribe
    public void preferenceUpdated(PreferenceType preferenceType)
    {
        if(preferenceType == PreferenceType.DECODE_EVENT || preferenceType == PreferenceType.TALKGROUP_FORMAT)
        {
            // fireTableDataChanged();
        }
    }

    /**
     * Adds, updates or deletes the event from the model.  Producers can send
     * the same call event multiple times to indicate that information in the
     * event is updated.  Producers can also mark the event as invalid and the
     * event will be removed from the model.
     */
    public void receive(final IDecodeEvent event)
    {
        //add() is thread-safe and coalesces UI updates internally; no Platform.runLater needed here.
        add(event);
    }

    /**
     * Builds the JavaFX TableColumns for the Decode Events table with human-readable cell formatting:
     * <ul>
     *   <li>Time: MM/dd/yyyy HH:mm:ss</li>
     *   <li>Duration: mm:ss.s</li>
     *   <li>From/To: just the role's identifier(s) (radio id / talkgroup id), formatted plainly</li>
     *   <li>Alias columns: the alias name(s) resolved for the role's identifier(s)</li>
     *   <li>Frequency: downlink frequency in MHz</li>
     * </ul>
     *
     * @param aliasModel to resolve identifier aliases (may be null, in which case alias columns are blank)
     * @param userPreferences for talkgroup/radio id formatting (may be null, falls back to identifier text)
     */
    public static java.util.List<TableColumn<IDecodeEvent, ?>> createColumns(AliasModel aliasModel,
            UserPreferences userPreferences)
    {
        java.util.List<TableColumn<IDecodeEvent, ?>> columns = new java.util.ArrayList<>();

        //Time - MM/dd/yyyy HH:mm:ss
        TableColumn<IDecodeEvent, Long> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(cellData -> new javafx.beans.property.ReadOnlyObjectWrapper<>(cellData.getValue().getTimeStart()));
        timeCol.setCellFactory(col -> new TableCell<>()
        {
            private final SimpleDateFormat mFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            @Override
            protected void updateItem(Long value, boolean empty)
            {
                super.updateItem(value, empty);
                setText((empty || value == null || value == 0L) ? null : mFormat.format(new Date(value)));
            }
        });
        timeCol.setPrefWidth(160);
        columns.add(timeCol);

        //Duration - mm:ss.s
        TableColumn<IDecodeEvent, Long> durationCol = new TableColumn<>("Duration");
        durationCol.setCellValueFactory(cellData -> new javafx.beans.property.ReadOnlyObjectWrapper<>(cellData.getValue().getDuration()));
        durationCol.setCellFactory(col -> new TableCell<>()
        {
            @Override
            protected void updateItem(Long value, boolean empty)
            {
                super.updateItem(value, empty);
                if(empty || value == null || value <= 0L)
                {
                    setText(null);
                }
                else
                {
                    long minutes = value / 60000L;
                    double seconds = (value % 60000L) / 1000.0;
                    setText(String.format("%02d:%04.1f", minutes, seconds));
                }
            }
        });
        durationCol.setPrefWidth(75);
        columns.add(durationCol);

        TableColumn<IDecodeEvent, String> eventCol = new TableColumn<>("Event");
        eventCol.setCellValueFactory(cellData -> new javafx.beans.property.ReadOnlyObjectWrapper<>(cellData.getValue().getEventType().getLabel()));
        eventCol.setPrefWidth(60);
        columns.add(eventCol);

        //From identifier (radio id) and its alias
        TableColumn<IDecodeEvent, ?> fromCol = identifierColumn("From", Role.FROM, userPreferences);
        fromCol.setPrefWidth(80);
        columns.add(fromCol);
        TableColumn<IDecodeEvent, ?> fromAliasCol = aliasColumn("Alias", Role.FROM, aliasModel);
        fromAliasCol.setPrefWidth(100);
        columns.add(fromAliasCol);

        //To identifier (talkgroup id) and its alias
        TableColumn<IDecodeEvent, ?> toCol = identifierColumn("To", Role.TO, userPreferences);
        toCol.setPrefWidth(80);
        columns.add(toCol);
        TableColumn<IDecodeEvent, ?> toAliasCol = aliasColumn("Alias", Role.TO, aliasModel);
        toAliasCol.setPrefWidth(100);
        columns.add(toAliasCol);

        TableColumn<IDecodeEvent, String> channelCol = new TableColumn<>("Channel");
        channelCol.setCellValueFactory(cellData -> {
            IDecodeEvent event = cellData.getValue();
            IChannelDescriptor channelDescriptor = event.getChannelDescriptor();
            String value = null;
            if(channelDescriptor != null)
            {
                if(event.hasTimeslot() && !event.toString().contains("TS"))
                {
                    value = channelDescriptor + " TS" + event.getTimeslot();
                }
                else
                {
                    value = channelDescriptor.toString();
                }
            }
            else if(event.hasTimeslot() && !event.toString().contains("TS"))
            {
                value = "TS" + event.getTimeslot();
            }
            return new javafx.beans.property.ReadOnlyObjectWrapper<>(value);
        });
        channelCol.setPrefWidth(80);
        columns.add(channelCol);

        //Frequency - downlink frequency in MHz
        TableColumn<IDecodeEvent, IChannelDescriptor> freqCol = new TableColumn<>("Frequency");
        freqCol.setCellValueFactory(cellData -> new javafx.beans.property.ReadOnlyObjectWrapper<>(cellData.getValue().getChannelDescriptor()));
        freqCol.setCellFactory(col -> new TableCell<>()
        {
            private final DecimalFormat mFrequencyFormat = new DecimalFormat("0.00000");
            @Override
            protected void updateItem(IChannelDescriptor descriptor, boolean empty)
            {
                super.updateItem(descriptor, empty);
                if(empty || descriptor == null)
                {
                    setText(null);
                }
                else
                {
                    long frequency = descriptor.getDownlinkFrequency();
                    setText(frequency > 0 ? mFrequencyFormat.format(frequency / 1e6d) : null);
                }
            }
        });
        freqCol.setPrefWidth(90);
        columns.add(freqCol);

        TableColumn<IDecodeEvent, String> detailsCol = new TableColumn<>("Details");
        detailsCol.setCellValueFactory(cellData -> new javafx.beans.property.ReadOnlyObjectWrapper<>(cellData.getValue().getDetails()));
        detailsCol.setPrefWidth(100);
        columns.add(detailsCol);

        //AI transcription of the call audio (populated asynchronously when transcription is enabled).
        TableColumn<IDecodeEvent, String> transcriptionCol = new TableColumn<>("Transcription");
        transcriptionCol.setCellValueFactory(cellData -> new javafx.beans.property.ReadOnlyObjectWrapper<>(cellData.getValue().getTranscription()));
        transcriptionCol.setCellFactory(col -> {
            TableCell<IDecodeEvent, String> cell = new TableCell<>()
            {
                @Override
                protected void updateItem(String value, boolean empty)
                {
                    super.updateItem(value, empty);

                    if(empty || value == null || value.isBlank())
                    {
                        setText(null);
                        setTooltip(null);
                    }
                    else
                    {
                        setText(value);
                        setWrapText(true);
                        //Wrapping, longer-lived tooltip so long transcripts are readable on hover instead of being
                        //clipped to a single off-screen line.
                        Tooltip tooltip = new Tooltip(value);
                        tooltip.setWrapText(true);
                        tooltip.setMaxWidth(480);
                        tooltip.setShowDuration(javafx.util.Duration.seconds(60));
                        setTooltip(tooltip);
                    }
                }
            };

            //Double-click a transcription cell to open the full text in a scrollable, copyable dialog - the column
            //is too narrow to read long multi-sentence transcripts inline.
            cell.setOnMouseClicked(mouseEvent -> {
                if(mouseEvent.getClickCount() == 2 && !cell.isEmpty()
                        && cell.getItem() != null && !cell.getItem().isBlank())
                {
                    showTranscriptionDialog(cell.getItem());
                }
            });

            return cell;
        });
        transcriptionCol.setPrefWidth(280);
        columns.add(transcriptionCol);

        return columns;
    }

    /**
     * Opens a non-modal dialog displaying the full transcription text in a read-only, word-wrapped, selectable
     * TextArea so long transcripts can be read and copied.
     */
    private static void showTranscriptionDialog(String transcript)
    {
        TextArea textArea = new TextArea(transcript);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefRowCount(12);
        textArea.setPrefColumnCount(50);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Transcription");
        dialog.setHeaderText(null);
        dialog.getDialogPane().setContent(textArea);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.setResizable(true);
        dialog.initModality(javafx.stage.Modality.NONE);
        dialog.show();
    }

    /**
     * Builds a column showing only the identifier(s) for the given role (e.g. the source radio id for
     * Role.FROM, the talkgroup id for Role.TO), formatted plainly rather than dumping the whole collection.
     */
    private static TableColumn<IDecodeEvent, IdentifierCollection> identifierColumn(String name, Role role,
            UserPreferences userPreferences)
    {
        TableColumn<IDecodeEvent, IdentifierCollection> column = new TableColumn<>(name);
        column.setCellValueFactory(cellData -> new javafx.beans.property.ReadOnlyObjectWrapper<>(cellData.getValue().getIdentifierCollection()));
        column.setCellFactory(col -> new TableCell<>()
        {
            @Override
            protected void updateItem(IdentifierCollection collection, boolean empty)
            {
                super.updateItem(collection, empty);
                setText((empty || collection == null) ? null :
                        formatIdentifiers(collection.getIdentifiers(role), userPreferences));
            }
        });
        return column;
    }

    /**
     * Builds a column showing the alias name(s) resolved for the identifier(s) of the given role.
     */
    private static TableColumn<IDecodeEvent, IdentifierCollection> aliasColumn(String name, Role role,
            AliasModel aliasModel)
    {
        TableColumn<IDecodeEvent, IdentifierCollection> column = new TableColumn<>(name);
        column.setCellValueFactory(cellData -> new javafx.beans.property.ReadOnlyObjectWrapper<>(cellData.getValue().getIdentifierCollection()));
        column.setCellFactory(col -> new TableCell<>()
        {
            @Override
            protected void updateItem(IdentifierCollection collection, boolean empty)
            {
                super.updateItem(collection, empty);
                setText((empty || collection == null) ? null : formatAliases(collection, role, aliasModel));
            }
        });
        return column;
    }

    /**
     * Formats the role's identifiers as a comma-separated list of plain values (talkgroup/radio ids use the
     * user's talkgroup format preference when available).
     */
    private static String formatIdentifiers(List<Identifier> identifiers, UserPreferences userPreferences)
    {
        if(identifiers == null || identifiers.isEmpty())
        {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for(Identifier identifier : identifiers)
        {
            if(sb.length() > 0)
            {
                sb.append(", ");
            }

            if(userPreferences != null && (identifier.getForm() == Form.TALKGROUP ||
                    identifier.getForm() == Form.RADIO || identifier.getForm() == Form.PATCH_GROUP))
            {
                sb.append(userPreferences.getTalkgroupFormatPreference().format(identifier));
            }
            else
            {
                sb.append(identifier);
            }
        }

        return sb.length() > 0 ? sb.toString() : null;
    }

    /**
     * Resolves and formats the alias name(s) for the role's identifiers using the alias model.
     */
    private static String formatAliases(IdentifierCollection collection, Role role, AliasModel aliasModel)
    {
        if(aliasModel == null)
        {
            return null;
        }

        List<Identifier> identifiers = collection.getIdentifiers(role);
        if(identifiers == null || identifiers.isEmpty())
        {
            return null;
        }

        AliasList aliasList = aliasModel.getAliasList(collection);
        if(aliasList == null)
        {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for(Identifier identifier : identifiers)
        {
            List<Alias> aliases = aliasList.getAliases(identifier);
            if(aliases != null && !aliases.isEmpty())
            {
                if(sb.length() > 0)
                {
                    sb.append(", ");
                }
                sb.append(Joiner.on(", ").skipNulls().join(aliases));
            }
        }

        return sb.length() > 0 ? sb.toString() : null;
    }
}
