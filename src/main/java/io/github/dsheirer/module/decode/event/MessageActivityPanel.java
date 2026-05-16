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
import io.github.dsheirer.sample.Listener;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel to display decoded messages/activity.
 */
public class MessageActivityPanel extends JFXPanel implements Listener<ProcessingChain>
{
    private static final Logger mLog = LoggerFactory.getLogger(MessageActivityPanel.class);
    private final String TABLE_PREFERENCE_KEY = "message.activity.panel";
    private MessageActivityModel mMessageModel = new MessageActivityModel();
    private MessageHistory mCurrentMessageHistory;

    private UserPreferences mUserPreferences;
    private FilterSet<IMessage> mMessageFilterSet;
    private boolean mRestoringFilters = false;
    private HistoryManagementPanel<IMessage> mHistoryManagementPanel;
    private MessageActivityPanelController mController;

    /**
     * Constructs an instance
     * @param userPreferences
     */
    public MessageActivityPanel(UserPreferences userPreferences)
    {
        mUserPreferences = userPreferences;
        mHistoryManagementPanel = new HistoryManagementPanel<>(mMessageModel, "Message Filter Editor");

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/module/decode/event/MessageActivityPanel.fxml"));
                Parent root = loader.load();
                mController = loader.getController();
                mController.init(mMessageModel, mHistoryManagementPanel);

                Scene scene = new Scene(root);
                java.net.URL cssUrl = getClass().getResource("/sdrtrunk_style.css");
                if (cssUrl != null) {
                    scene.getStylesheets().add(cssUrl.toExternalForm());
                }
                setScene(scene);
            } catch (IOException e) {
                mLog.error("Error loading MessageActivityPanel.fxml", e);
            }
        });
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

            Platform.runLater(() -> {
                if (mController != null) {
                    mController.setMessageFilterSet(mMessageFilterSet);
                }
            });

            //Register filter change listener to refresh the table and persist filter states.
            mMessageFilterSet.register(() -> {
                if(!mRestoringFilters)
                {
                    saveFilterStates(mMessageFilterSet);
                }
                mMessageModel.fireTableDataChanged();
                Platform.runLater(() -> {
                    if (mController != null) mController.refreshFilter();
                });
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
            Platform.runLater(() -> {
                if (mController != null) {
                    mController.setMessageFilterSet(null);
                }
            });
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
