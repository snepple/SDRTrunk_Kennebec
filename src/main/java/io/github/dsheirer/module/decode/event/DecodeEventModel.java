
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

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.sample.Listener;
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
        Platform.runLater(() -> add(event));
    }

    /**
     * @return A list of JavaFX TableColumns for the Decode Events table
     */
    public static java.util.List<TableColumn<IDecodeEvent, ?>> createColumns()
    {
        java.util.List<TableColumn<IDecodeEvent, ?>> columns = new java.util.ArrayList<>();

        TableColumn<IDecodeEvent, Long> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(cellData -> new javafx.beans.property.ReadOnlyObjectWrapper<>(cellData.getValue().getTimeStart()));
        columns.add(timeCol);

        TableColumn<IDecodeEvent, Long> durationCol = new TableColumn<>("Duration");
        durationCol.setCellValueFactory(cellData -> new javafx.beans.property.ReadOnlyObjectWrapper<>(cellData.getValue().getDuration()));
        columns.add(durationCol);

        TableColumn<IDecodeEvent, String> eventCol = new TableColumn<>("Event");
        eventCol.setCellValueFactory(cellData -> new javafx.beans.property.ReadOnlyObjectWrapper<>(cellData.getValue().getEventType().getLabel()));
        columns.add(eventCol);

        TableColumn<IDecodeEvent, IdentifierCollection> fromIdCol = new TableColumn<>("From");
        fromIdCol.setCellValueFactory(cellData -> new javafx.beans.property.ReadOnlyObjectWrapper<>(cellData.getValue().getIdentifierCollection()));
        columns.add(fromIdCol);

        TableColumn<IDecodeEvent, IdentifierCollection> fromAliasCol = new TableColumn<>("Alias");
        fromAliasCol.setCellValueFactory(cellData -> new javafx.beans.property.ReadOnlyObjectWrapper<>(cellData.getValue().getIdentifierCollection()));
        columns.add(fromAliasCol);

        TableColumn<IDecodeEvent, IdentifierCollection> toIdCol = new TableColumn<>("To");
        toIdCol.setCellValueFactory(cellData -> new javafx.beans.property.ReadOnlyObjectWrapper<>(cellData.getValue().getIdentifierCollection()));
        columns.add(toIdCol);

        TableColumn<IDecodeEvent, IdentifierCollection> toAliasCol = new TableColumn<>("Alias");
        toAliasCol.setCellValueFactory(cellData -> new javafx.beans.property.ReadOnlyObjectWrapper<>(cellData.getValue().getIdentifierCollection()));
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
            else
            {
                if(event.hasTimeslot() && !event.toString().contains("TS"))
                {
                    value = "TS" + event.getTimeslot();
                }
            }
            return new javafx.beans.property.ReadOnlyObjectWrapper<>(value);
        });
        columns.add(channelCol);

        TableColumn<IDecodeEvent, IChannelDescriptor> freqCol = new TableColumn<>("Frequency");
        freqCol.setCellValueFactory(cellData -> new javafx.beans.property.ReadOnlyObjectWrapper<>(cellData.getValue().getChannelDescriptor()));
        columns.add(freqCol);

        TableColumn<IDecodeEvent, String> detailsCol = new TableColumn<>("Details");
        detailsCol.setCellValueFactory(cellData -> new javafx.beans.property.ReadOnlyObjectWrapper<>(cellData.getValue().getDetails()));
        columns.add(detailsCol);

        return columns;
    }
}
