


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


import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import com.google.common.base.Joiner;
import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.IFilter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.filter.FilterSet;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.preference.NowPlayingPreference;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.controller.channel.ChannelProcessingManager;
import io.github.dsheirer.module.ProcessingChain;
import io.github.dsheirer.module.decode.event.filter.DecodeEventFilterSet;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.swing.TableViewColumnWidthMonitor;
import io.github.dsheirer.sample.Listener;

import javafx.scene.Node;
import javafx.application.Platform;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;











public class DecodeEventPanel extends VBox implements Listener<ProcessingChain>
{
    private static final long serialVersionUID = 1L;
    private final static Logger mLog = LoggerFactory.getLogger(DecodeEventPanel.class);
    private static final String TABLE_PREFERENCE_KEY = "decode.event.panel";

    private TableView mTable;
    private TableViewColumnWidthMonitor mTableColumnWidthMonitor;
    private DecodeEventModel mEventModel = new DecodeEventModel();
    private DecodeEventModel mGlobalEventModel = new DecodeEventModel();
    private ChannelProcessingManager mChannelProcessingManager;
    private Listener<IDecodeEvent> mGlobalEventListener;

    private class ActiveModelWrapper extends ClearableHistoryModel<IDecodeEvent> {
        @Override
        public void clear() {
            mEventModel.clear();
            mGlobalEventModel.clear();
        }

        @Override
        public void setHistorySize(int size) {
            super.setHistorySize(size);
            mEventModel.setHistorySize(size);
            mGlobalEventModel.setHistorySize(size);
        }

        // // @Override
        public int getColumnCount() {
            return 0;
        }

        // // @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return null;
        }
    }

    private ActiveModelWrapper mActiveModelWrapper = new ActiveModelWrapper();
    private DecodeEventHistory mCurrentEventHistory;
    private ScrollPane mEmptyScroller;
    private IconModel mIconModel;
    private AliasModel mAliasModel;
    private UserPreferences mUserPreferences;
    private FilterSet<IDecodeEvent> mFilterSet = new DecodeEventFilterSet();
    private HistoryManagementPanel<IDecodeEvent> mHistoryManagementPanel;


    /**
     * View for call event table
     * @param iconModel to display alias icons in table rows
     */
    public DecodeEventPanel(IconModel iconModel, UserPreferences userPreferences, AliasModel aliasModel, ChannelProcessingManager channelProcessingManager)
    {
        MyEventBus.getGlobalEventBus().register(this);

        // setLayout(new javafx.scene.layout.HBox(4));
        mIconModel = iconModel;
        mAliasModel = aliasModel;
        mUserPreferences = userPreferences;
        mChannelProcessingManager = channelProcessingManager;

        mGlobalEventListener = event -> mGlobalEventModel.receive(event);
        mChannelProcessingManager.addDecodeEventListener(mGlobalEventListener);
        mGlobalEventModel.setHistorySize(mUserPreferences.getNowPlayingPreference().getEventHistorySize());
        mTable = new TableView();
        mTable.getStyleClass().add("preferences-table");
        
        Label placeholderLabel = new Label("No decode events — events will appear when a channel is active");
        VBox placeholderContainer = new VBox(placeholderLabel);
        placeholderContainer.setAlignment(Pos.TOP_CENTER);
        placeholderContainer.setPadding(new Insets(10, 0, 0, 0));
        mTable.setPlaceholder(placeholderContainer);
        
        mTable.setTableMenuButtonVisible(true);
        mTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        mTable.getColumns().addAll(DecodeEventModel.createColumns(mAliasModel, mUserPreferences));
        //Bind the table to the global event model so events are visible before any channel is selected.
        //When a channel is selected, receive(ProcessingChain) rebinds to that channel's per-channel model.
        //(Previously the table was bound to mActiveModelWrapper, which is never populated with events - the
        //two real models below feed the table - so the table always showed the empty placeholder.)
        mTable.setItems(mGlobalEventModel.getItems());
        
        Runnable updateHeaderVisibility = () -> {
            boolean singleColumn = mTable.getColumns().size() <= 1;
            mTable.setTableMenuButtonVisible(!singleColumn);
            Pane header = (Pane) mTable.lookup("TableHeaderRow");
            if (header != null) {
                if (singleColumn) {
                    header.setMaxHeight(0);
                    header.setMinHeight(0);
                    header.setPrefHeight(0);
                    header.setVisible(false);
                } else {
                    header.setMaxHeight(Region.USE_COMPUTED_SIZE);
                    header.setMinHeight(Region.USE_COMPUTED_SIZE);
                    header.setPrefHeight(Region.USE_COMPUTED_SIZE);
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

        mTableColumnWidthMonitor = new TableViewColumnWidthMonitor(mUserPreferences, mTable, TABLE_PREFERENCE_KEY);
        updateCellRenderers();
        NowPlayingPreference nowPlayingPreference = mUserPreferences.getNowPlayingPreference();
        restoreFilterStates(nowPlayingPreference);

        mActiveModelWrapper.setHistorySize(nowPlayingPreference.getEventHistorySize());
        mHistoryManagementPanel = new HistoryManagementPanel<>(
                mActiveModelWrapper,
                "Event Filter Editor",
                nowPlayingPreference.getEventHistorySize(),
                nowPlayingPreference::setEventHistorySize);

        mHistoryManagementPanel.updateFilterSet(mFilterSet);
        getChildren().add(mHistoryManagementPanel);
        VBox.setVgrow(mTable, Priority.ALWAYS);
        getChildren().add(mTable);

        mFilterSet.register(() -> {
            saveFilterStates(nowPlayingPreference);
            mTable.refresh();
        });
    }


    private void restoreFilterStates(NowPlayingPreference prefs)
    {
        for(IFilter<IDecodeEvent> ifilter : mFilterSet.getFilters())
        {
            if(ifilter instanceof Filter<?,?> filter)
            {
                for(FilterElement<?> element : filter.getFilterElements())
                {
                    String key = filter.getName() + "." + element.getName();
                    element.setEnabled(prefs.isFilterEnabled(key));
                }
            }
        }
    }

    private void saveFilterStates(NowPlayingPreference prefs)
    {
        for(IFilter<IDecodeEvent> ifilter : mFilterSet.getFilters())
        {
            if(ifilter instanceof Filter<?,?> filter)
            {
                for(FilterElement<?> element : filter.getFilterElements())
                {
                    String key = filter.getName() + "." + element.getName();
                    prefs.setFilterEnabled(key, element.isEnabled());
                }
            }
        }
    }

    public void dispose()
    {
        MyEventBus.getGlobalEventBus().unregister(this);
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
            // TimestampCellRenderer removed during JavaFX migration
        }
    }

    /**
     * No-op placeholder for future JavaFX cell renderers.
     */
    private void updateCellRenderers()
    {
        // TODO: implement JavaFX cell renderers when needed
    }

    /**
     * Receives completed audio transcriptions and attaches each to its decode event so it shows in the
     * Transcription column.  Correlation is by TO talkgroup id (when known) plus closest start time, since
     * transcripts arrive asynchronously and are not directly linked to a decode event.
     */
    @Subscribe
    public void handleTranscription(io.github.dsheirer.transcription.TranscriptionEvent event)
    {
        if(event == null || event.getTranscript() == null || event.getTranscript().isBlank())
        {
            return;
        }

        final String transcript = event.getTranscript().trim();
        final long timestamp = event.getStartTimestamp();
        final Integer toId = event.getToId();
        final long frequency = event.getFrequency();

        Platform.runLater(() -> {
            IDecodeEvent best = findBestTranscriptionMatch(new java.util.ArrayList<IDecodeEvent>(mTable.getItems()),
                timestamp, toId, frequency);

            if(best != null)
            {
                //Append in audio-start order instead of overwriting, so a call split across multiple audio
                //segments shows its full text from the beginning rather than only the last-arriving fragment.
                best.addTranscriptionSegment(timestamp, transcript);
                mTable.refresh();
            }
        });
    }

    /**
     * Finds the decode event that best matches a completed transcription.  A call may be split into multiple audio
     * segments; later segments start well after the decode event start, so matching only against start time drops long
     * calls.  Treat the whole event interval as the match window, with a short grace period before/after it.
     */
    static IDecodeEvent findBestTranscriptionMatch(List<IDecodeEvent> decodeEvents, long timestamp, Integer toId,
            long frequency)
    {
        if(decodeEvents == null)
        {
            return null;
        }

        //Frequency uniquely identifies a conventional channel even when several channels share a default talkgroup,
        //so when it is known prefer matching on it. Only fall back to TO-talkgroup matching when no frequency-scoped
        //candidate is found (e.g. trunked protocols where the decode event carries no fixed channel frequency).
        IDecodeEvent best = bestByPredicate(decodeEvents, timestamp,
                decodeEvent -> frequency > 0 && frequencyMatches(decodeEvent, frequency));

        if(best != null)
        {
            return best;
        }

        return bestByPredicate(decodeEvents, timestamp, decodeEvent -> toMatches(decodeEvent, toId));
    }

    /**
     * Returns the decode event satisfying the predicate whose time interval is closest to the transcript timestamp,
     * within the 30-second match window.
     */
    private static IDecodeEvent bestByPredicate(List<IDecodeEvent> decodeEvents, long timestamp,
            java.util.function.Predicate<IDecodeEvent> predicate)
    {
        IDecodeEvent best = null;
        long bestDelta = Long.MAX_VALUE;

        for(IDecodeEvent decodeEvent : decodeEvents)
        {
            if(decodeEvent == null || !predicate.test(decodeEvent))
            {
                continue;
            }

            long delta = timestamp > 0 ? distanceFromEventInterval(decodeEvent, timestamp) : 0;

            if(delta < bestDelta && delta <= 30000)
            {
                bestDelta = delta;
                best = decodeEvent;
            }
        }

        return best;
    }

    /**
     * Indicates whether the decode event's channel downlink frequency matches the transcript's captured frequency.
     */
    private static boolean frequencyMatches(IDecodeEvent decodeEvent, long frequency)
    {
        return decodeEvent.getChannelDescriptor() != null
                && decodeEvent.getChannelDescriptor().getDownlinkFrequency() == frequency;
    }

    private static boolean toMatches(IDecodeEvent decodeEvent, Integer toId)
    {
        if(toId == null)
        {
            return true;
        }

        if(decodeEvent.getIdentifierCollection() != null)
        {
            for(io.github.dsheirer.identifier.Identifier id : decodeEvent.getIdentifierCollection()
                    .getIdentifiers(io.github.dsheirer.identifier.Role.TO))
            {
                if(id.getValue() instanceof Number && ((Number)id.getValue()).intValue() == toId)
                {
                    return true;
                }
            }
        }

        return false;
    }

    private static long distanceFromEventInterval(IDecodeEvent decodeEvent, long timestamp)
    {
        long start = decodeEvent.getTimeStart();
        long end = decodeEvent.getTimeEnd();

        if(end < start)
        {
            end = start;
        }

        if(timestamp >= start && timestamp <= end)
        {
            return 0;
        }

        return Math.min(Math.abs(timestamp - start), Math.abs(timestamp - end));
    }

    @Override
    public void receive(final ProcessingChain processingChain)
    {
        if(mCurrentEventHistory != null)
        {
            mCurrentEventHistory.removeListener(mEventModel);
        }

        Platform.runLater(() -> {
            if(processingChain != null)
            {
                mCurrentEventHistory = processingChain.getDecodeEventHistory();
                mEventModel.clearAndSet(mCurrentEventHistory.getItems());
                processingChain.getDecodeEventHistory().addListener(mEventModel);
                //Show the selected channel's events.
                mTable.setItems(mEventModel.getItems());
                mHistoryManagementPanel.setEnabled(true);
            }
            else
            {
                mCurrentEventHistory = null;
                //No channel selected - fall back to the global (all-channels) event stream.
                mTable.setItems(mGlobalEventModel.getItems());
                mHistoryManagementPanel.setEnabled(true);
            }

            updateCellRenderers();
            mTable.refresh();
        });
    }


}
