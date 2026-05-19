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

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

/**
 * Panel to display decoded messages/activity.
 */
public class MessageActivityPanel extends JFXPanel implements Listener<ProcessingChain>
{
    private static final long serialVersionUID = 1L;
    private static final Logger mLog = LoggerFactory.getLogger(MessageActivityPanel.class);
    private MessageActivityModel mMessageModel = new MessageActivityModel();
    private MessageHistory mCurrentMessageHistory;

    private UserPreferences mUserPreferences;
    private FilterSet<IMessage> mMessageFilterSet;
    private boolean mRestoringFilters = false;
    private HistoryManagementPanel<IMessage> mHistoryManagementPanel;
    private TableView<MessageItem> mTable;
    private FilteredList<MessageItem> mFilteredItems;

    /**
     * Constructs an instance
     * @param userPreferences
     */
    public MessageActivityPanel(UserPreferences userPreferences)
    {
        mUserPreferences = userPreferences;
        mHistoryManagementPanel = new HistoryManagementPanel<>(mMessageModel, "Message Filter Editor");

        Platform.runLater(() -> {
            mTable = new TableView<>();
            mTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            TableColumn<MessageItem, String> timeCol = new TableColumn<>("Time");
            timeCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getTimestamp(new java.text.SimpleDateFormat("yyyy:MM:dd HH:mm:ss"))
            ));

            TableColumn<MessageItem, String> protocolCol = new TableColumn<>("Protocol");
            protocolCol.setCellValueFactory(new PropertyValueFactory<>("protocol"));

            TableColumn<MessageItem, String> timeslotCol = new TableColumn<>("Timeslot");
            timeslotCol.setCellValueFactory(new PropertyValueFactory<>("timeslot"));

            TableColumn<MessageItem, String> messageCol = new TableColumn<>("Message");
            messageCol.setCellValueFactory(new PropertyValueFactory<>("text"));

            mTable.getColumns().addAll(timeCol, protocolCol, timeslotCol, messageCol);

            mFilteredItems = new FilteredList<>(mMessageModel.getItems(), this::evaluateFilter);
            mTable.setItems(mFilteredItems);

            VBox vbox = new VBox();
            VBox.setVgrow(mTable, Priority.ALWAYS);

            javafx.embed.swing.SwingNode historyNode = new javafx.embed.swing.SwingNode();
            javax.swing.SwingUtilities.invokeLater(() -> historyNode.setContent(mHistoryManagementPanel));

            vbox.getChildren().addAll(historyNode, mTable);

            Scene scene = new Scene(vbox);
            java.net.URL cssUrl = getClass().getResource("/sdrtrunk_style.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
            setScene(scene);
        });
    }

    private boolean evaluateFilter(MessageItem item) {
        if (mMessageFilterSet != null && item != null && item.getMessage() != null) {
            IMessage message = item.getMessage();
            return mMessageFilterSet.canProcess(message) && mMessageFilterSet.passes(message);
        }
        return false;
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
                Platform.runLater(() -> {
                    if (mFilteredItems != null) mFilteredItems.setPredicate(this::evaluateFilter);
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
            Platform.runLater(() -> {
                if (mFilteredItems != null) mFilteredItems.setPredicate(this::evaluateFilter);
            });
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
