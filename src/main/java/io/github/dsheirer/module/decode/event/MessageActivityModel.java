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
package io.github.dsheirer.module.decode.event;

import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.StuffBitsMessage;
import io.github.dsheirer.sample.Listener;
import javafx.application.Platform;
import java.text.SimpleDateFormat;

/**
 * Table Model for decoded IMessages.
 */
public class MessageActivityModel extends ClearableHistoryModel<MessageItem> implements Listener<IMessage>
{
    private static final long serialVersionUID = 1L;
    private static final int TIME = 0;
    private static final int PROTOCOL = 1;
    private static final int TIMESLOT = 2;
    private static final int MESSAGE = 3;

    private String[] mHeaders = new String[]{"Time", "Protocol", "Timeslot", "Message"};
    private SimpleDateFormat mSDFTime = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");

    /**
     * Constructor
     */
    public MessageActivityModel()
    {
    }

    /**
     * Implements the listener interface and wraps the IMessage in table-compatible message item wrapper.
     * @param message to add to the model
     */
    public void receive(final IMessage message)
    {
        //Don't process tail bits or stuff bits message fragments
        if(message instanceof StuffBitsMessage)
        {
            return;
        }

        //add() is thread-safe and coalesces UI updates internally; no Platform.runLater needed here.
        add(new MessageItem(message));
    }

    /**
     * @return A list of JavaFX TableColumns for the Message Activity table
     */
    public static java.util.List<javafx.scene.control.TableColumn<MessageItem, ?>> createColumns()
    {
        java.util.List<javafx.scene.control.TableColumn<MessageItem, ?>> columns = new java.util.ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");

        javafx.scene.control.TableColumn<MessageItem, String> timeCol = new javafx.scene.control.TableColumn<>("Time");
        timeCol.setCellValueFactory(cellData -> new javafx.beans.property.ReadOnlyObjectWrapper<>(cellData.getValue().getTimestamp(sdf)));
        columns.add(timeCol);

        javafx.scene.control.TableColumn<MessageItem, String> protocolCol = new javafx.scene.control.TableColumn<>("Protocol");
        protocolCol.setCellValueFactory(cellData -> new javafx.beans.property.ReadOnlyObjectWrapper<>(cellData.getValue().getProtocol()));
        columns.add(protocolCol);

        javafx.scene.control.TableColumn<MessageItem, Integer> timeslotCol = new javafx.scene.control.TableColumn<>("Timeslot");
        timeslotCol.setCellValueFactory(cellData -> new javafx.beans.property.ReadOnlyObjectWrapper<>(cellData.getValue().getTimeslot()));
        columns.add(timeslotCol);

        javafx.scene.control.TableColumn<MessageItem, String> messageCol = new javafx.scene.control.TableColumn<>("Message");
        messageCol.setCellValueFactory(cellData -> new javafx.beans.property.ReadOnlyObjectWrapper<>(cellData.getValue().getText()));
        columns.add(messageCol);

        return columns;
    }
}
