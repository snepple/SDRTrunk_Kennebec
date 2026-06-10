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
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;
import javafx.scene.control.ScrollPane;

import javafx.scene.control.TableView;


import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.List;

/**
 * Model implementation supporting clearable history for JavaFX TableView.
 */
public abstract class ClearableHistoryModel<T> 
{
    public static final int DEFAULT_HISTORY_SIZE = 200;
    private ObservableList<T> mItems = FXCollections.observableArrayList();
    private int mHistorySize = DEFAULT_HISTORY_SIZE;

    /**
     * @return the observable list of items for UI binding
     */
    public ObservableList<T> getItems()
    {
        return mItems;
    }

    /**
     * Access an item/row by the model index value.
     * @param index to retrieve
     * @return item or null
     */
    public T getItem(int index)
    {
        if(index >= 0 && index < mItems.size())
        {
            return mItems.get(index);
        }

        return null;
    }

    /**
     * Adds the item to the top of the item list and removes any tail items while the item list size exceeds the
     * maximum history size for this model.
     * @param item to add
     */
    public void add(T item)
    {
        Platform.runLater(() -> {
            if(mItems.contains(item))
            {
                int itemRow = mItems.indexOf(item);
                mItems.set(itemRow, item); // Trigger an update for the item
            }
            else
            {
                mItems.add(0, item);

                while(mItems.size() > mHistorySize)
                {
                    mItems.remove(mItems.size() - 1);
                }
            }
        });
    }

    /**
     * Clears all messages from history
     */
    public void clear()
    {
        Platform.runLater(() -> {
            mItems.clear();
        });
    }

    /**
     * Clears the current messages and loads the messages argument
     */
    public void clearAndSet(List<T> items)
    {
        Platform.runLater(() -> {
            mItems.clear();
            for(T item: items)
            {
                mItems.add(item);
                if (mItems.size() > mHistorySize) {
                    break;
                }
            }
        });
    }

    /**
     * Current history size
     * @return history size
     */
    public int getHistorySize()
    {
        return mHistorySize;
    }

    /**
     * Sets the history size
     * @param historySize
     */
    public void setHistorySize(int historySize)
    {
        mHistorySize = historySize;
        Platform.runLater(() -> {
            while(mItems.size() > mHistorySize)
            {
                mItems.remove(mItems.size() - 1);
            }
        });
    }

    public int getRowCount()
    {
        return mItems.size();
    }
}

