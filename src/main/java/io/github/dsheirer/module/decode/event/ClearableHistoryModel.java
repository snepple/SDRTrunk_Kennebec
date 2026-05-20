package io.github.dsheirer.module.decode.event;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

public abstract class ClearableHistoryModel<T>
{
    public static final int DEFAULT_HISTORY_SIZE = 200;
    private ObservableList<T> mItems = FXCollections.observableArrayList();
    private int mHistorySize = DEFAULT_HISTORY_SIZE;

    public ObservableList<T> getItems() {
        return mItems;
    }

    public T getItem(int index)
    {
        if(index >= 0 && index < mItems.size())
        {
            return mItems.get(index);
        }

        return null;
    }

    public void add(T item)
    {
        Platform.runLater(() -> {
            if(!mItems.contains(item))
            {
                mItems.add(0, item);

                while(mItems.size() > mHistorySize)
                {
                    mItems.remove(mItems.size() - 1);
                }
            }
            else
            {
                int itemRow = mItems.indexOf(item);
                if (itemRow >= 0) {
                    mItems.set(itemRow, item);
                }
            }
        });
    }

    public void clear()
    {
        Platform.runLater(() -> mItems.clear());
    }

    public void clearAndSet(List<T> items)
    {
        Platform.runLater(() -> {
            mItems.clear();
            for(T item: items)
            {
                if(!mItems.contains(item))
                {
                    mItems.add(item);
                }
            }
            while(mItems.size() > mHistorySize)
            {
                mItems.remove(mItems.size() - 1);
            }
        });
    }

    public int getHistorySize()
    {
        return mHistorySize;
    }

    public void setHistorySize(int historySize)
    {
        mHistorySize = historySize;
    }

    public int getRowCount()
    {
        return mItems.size();
    }
}
