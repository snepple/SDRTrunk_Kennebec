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

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.filter.FilterSet;
import io.github.dsheirer.filter.IFilter;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.MessageHistory;
import io.github.dsheirer.module.ProcessingChain;
import io.github.dsheirer.module.decode.DecoderFactory;
import io.github.dsheirer.preference.UserPreferences;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.dsheirer.sample.Listener;
import javafx.application.Platform;

/**
 * Panel to display decoded messages/activity.
 */
public class MessageActivityPanel extends javafx.scene.layout.VBox implements Listener<ProcessingChain>
{
    private static final long serialVersionUID = 1L;
    private static final Logger mLog = LoggerFactory.getLogger(MessageActivityPanel.class);
    private final String TABLE_PREFERENCE_KEY = "message.activity.panel";
    private MessageActivityModel mMessageModel = new MessageActivityModel();
    private MessageHistory mCurrentMessageHistory;
    private javafx.scene.control.TableView<MessageItem> mTable = new javafx.scene.control.TableView<>();

    private UserPreferences mUserPreferences;
    private FilterSet<IMessage> mMessageFilterSet;
    private boolean mRestoringFilters = false;
    private HistoryManagementPanel<IMessage> mHistoryManagementPanel;

    /**
     * Constructs an instance
     * @param userPreferences
     */
    public MessageActivityPanel(UserPreferences userPreferences)
    {
        mUserPreferences = userPreferences;
        
        javafx.scene.control.Label placeholderLabel = new javafx.scene.control.Label("No messages — messages will appear when a channel is active");
        javafx.scene.layout.VBox placeholderContainer = new javafx.scene.layout.VBox(placeholderLabel);
        placeholderContainer.setAlignment(javafx.geometry.Pos.TOP_CENTER);
        placeholderContainer.setPadding(new javafx.geometry.Insets(10, 0, 0, 0));
        mTable.setPlaceholder(placeholderContainer);
        
        mTable.setTableMenuButtonVisible(true);
        mTable.setColumnResizePolicy(javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY);
        mTable.getColumns().addAll(MessageActivityModel.createColumns());
        mTable.setItems(mMessageModel.getItems());
        
        Runnable updateHeaderVisibility = () -> {
            boolean singleColumn = mTable.getColumns().size() <= 1;
            mTable.setTableMenuButtonVisible(!singleColumn);
            javafx.scene.layout.Pane header = (javafx.scene.layout.Pane) mTable.lookup("TableHeaderRow");
            if (header != null) {
                if (singleColumn) {
                    header.setMaxHeight(0);
                    header.setMinHeight(0);
                    header.setPrefHeight(0);
                    header.setVisible(false);
                } else {
                    header.setMaxHeight(javafx.scene.layout.Region.USE_COMPUTED_SIZE);
                    header.setMinHeight(javafx.scene.layout.Region.USE_COMPUTED_SIZE);
                    header.setPrefHeight(javafx.scene.layout.Region.USE_COMPUTED_SIZE);
                    header.setVisible(true);
                }
            }
        };

        mTable.getColumns().addListener((javafx.beans.InvalidationListener) obs -> {
            updateHeaderVisibility.run();
        });

        mTable.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            Platform.runLater(updateHeaderVisibility);
        });

        mHistoryManagementPanel = new HistoryManagementPanel<>(mMessageModel, "Message Filter Editor");
        getChildren().add(mHistoryManagementPanel);
        javafx.scene.layout.VBox.setVgrow(mTable, javafx.scene.layout.Priority.ALWAYS);
        getChildren().add(mTable);
    }

    /**
     * Updates the message activity model with message history from the specified processing chain
     */
    @Override
    public void receive(ProcessingChain processingChain)
    {
        if(mCurrentMessageHistory != null)
        {
            mCurrentMessageHistory.removeListener(mMessageModel);
        }

        //Unregister from changes made to the filter set
        if(mMessageFilterSet != null)
        {
            mMessageFilterSet.register(null);
        }

        if(processingChain != null)
        {
            mCurrentMessageHistory = processingChain.getMessageHistory();
            mMessageFilterSet = DecoderFactory.getMessageFilters(processingChain.getModules());
            mRestoringFilters = true;
            restoreFilterStates(mMessageFilterSet);
            mRestoringFilters = false;
            //Register filter change listener to refresh the table and persist filter states.
            mMessageFilterSet.register(() -> {
                if(!mRestoringFilters)
                {
                    saveFilterStates(mMessageFilterSet);
                }
                // mMessageModel.fireTableDataChanged();
            });
            if(mHistoryManagementPanel != null)
            {
                mHistoryManagementPanel.updateFilterSet(mMessageFilterSet);
            }

            List<MessageItem> currentHistory = new ArrayList<>();
            for(IMessage message: mCurrentMessageHistory.getItems())
            {
                currentHistory.add(new MessageItem(message));
            }

            mMessageModel.clearAndSet(currentHistory);
            mCurrentMessageHistory.addListener(mMessageModel);
            mHistoryManagementPanel.setEnabled(true);
        }
        else
        {
            mCurrentMessageHistory = null;
            mMessageFilterSet = null;
            mMessageModel.clear();
            mHistoryManagementPanel.setEnabled(false);
        }
    }

    private void restoreFilterStates(FilterSet<IMessage> filterSet)
    {
        for(IFilter<IMessage> ifilter : filterSet.getFilters())
        {
            if(ifilter instanceof Filter<?,?> filter)
            {
                for(FilterElement<?> element : filter.getFilterElements())
                {
                    String key = filter.getName() + "." + element.getName();
                    element.setEnabled(mUserPreferences.getNowPlayingPreference().isFilterEnabled(key));
                }
            }
        }
    }

    private void saveFilterStates(FilterSet<IMessage> filterSet)
    {
        for(IFilter<IMessage> ifilter : filterSet.getFilters())
        {
            if(ifilter instanceof Filter<?,?> filter)
            {
                for(FilterElement<?> element : filter.getFilterElements())
                {
                    String key = filter.getName() + "." + element.getName();
                    mUserPreferences.getNowPlayingPreference().setFilterEnabled(key, element.isEnabled());
                }
            }
        }
    }
}
