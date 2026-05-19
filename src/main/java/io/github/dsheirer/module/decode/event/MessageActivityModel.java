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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

/**
 * Observable Model for decoded IMessages.
 */
public class MessageActivityModel extends ClearableHistoryModel<MessageItem> implements Listener<IMessage>
{
    private static final long serialVersionUID = 1L;
    private ObservableList<MessageItem> mItems = FXCollections.observableArrayList();

    /**
     * Constructor
     */
    public MessageActivityModel()
    {
    }

    /**
     * Returns the observable list of items for binding to JavaFX UI.
     */
    public ObservableList<MessageItem> getItems() {
        return mItems;
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

        Platform.runLater(() -> add(new MessageItem(message)));
    }

    @Override
    public void add(MessageItem item) {
        if (!mItems.contains(item)) {
            mItems.add(0, item);

            while (mItems.size() > getHistorySize()) {
                mItems.remove(mItems.size() - 1);
            }
        }
    }

    @Override
    public void clear() {
        Platform.runLater(() -> mItems.clear());
    }

    @Override
    public void clearAndSet(List<MessageItem> items) {
        Platform.runLater(() -> {
            mItems.clear();
            for (MessageItem item : items) {
                add(item);
            }
        });
    }

    @Override
    public MessageItem getItem(int index) {
        if (index >= 0 && index < mItems.size()) {
            return mItems.get(index);
        }
        return null;
    }

    @Override
    public int getColumnCount()
    {
        return 4;
    }

    @Override
    public int getRowCount() {
        return mItems.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        return null;
    }
}
