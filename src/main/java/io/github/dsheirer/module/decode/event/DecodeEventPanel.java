


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
        mTable.setTableMenuButtonVisible(true);
        // mTable.setFillsViewportHeight(true);
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
        mEmptyScroller = new ScrollPane(mTable);
        getChildren().add(mEmptyScroller);

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

    private void updateCellRenderers()
    {
        // mTable.getColumns().get...setCellRenderer...
        // mTable.getColumns().get...setCellRenderer...
        // mTable.getColumns().get...setCellRenderer...
        // mTable.getColumns().get...setCellRenderer...
        // mTable.getColumns().get...setCellRenderer...
        // mTable.getColumns().get...setCellRenderer...
        // mTable.getColumns().get...setCellRenderer...
        // mTable.getColumns().get...setCellRenderer...
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
                // mTable.setModel(mEventModel);
                // Row sorter removed during JavaFX migration
                mHistoryManagementPanel.setEnabled(true);
            }
            else
            {
                mCurrentEventHistory = null;
                // mTable.setModel(mGlobalEventModel);
                // Row sorter removed during JavaFX migration
                mHistoryManagementPanel.setEnabled(true);
            }

            updateCellRenderers();
            mTable.refresh();
            mTable.refresh();
        });
    }

    /**
     * Custom cell renderer for displaying identifiers from an identifier collection
     */
//     public class IdentifierCellRenderer extends DefaultTableCellRenderer
//     {
//         protected Role mRole;
// 
//         /**
//          * Constructs an instance of the cell renderer.
//          *
//          * @param role of the identifier
//          */
//         public IdentifierCellRenderer(Role role)
//         {
//             mRole = role;
//             setHorizontalAlignment(javafx.geometry.javafx.geometry.Pos.CENTER);
//         }
// 
        // // @Override
//         public Component getTableCellRendererComponent(TableView table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
//         {
//             Label label = (Label)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
// 
//             if(value instanceof IdentifierCollection)
//             {
//                 List<Identifier> identifiers = ((IdentifierCollection)value).getIdentifiers(mRole);
//                 label.setText(format(identifiers));
//             }
//             else
//             {
//                 label.setText(null);
//             }
// 
//             return label;
//         }
// 
//         /**
//          * Formats a list of identifiers as a comma separated list of values
//          * @param identifiers to format
//          * @return formatted list or null
//          */
//         protected String format(List<Identifier> identifiers)
//         {
//             if(identifiers == null || identifiers.isEmpty())
//             {
//                 return null;
//             }
// 
//             StringBuilder sb = new StringBuilder();
// 
//             for(Identifier identifier: identifiers)
//             {
//                 if(sb.length() > 0)
//                 {
//                     sb.append(",");
//                 }
// 
//                 if(identifier.getForm() == Form.TALKGROUP || identifier.getForm() == Form.RADIO || identifier.getForm() == Form.PATCH_GROUP)
//                 {
//                     sb.append(mUserPreferences.getTalkgroupFormatPreference().format(identifier));
//                 }
//                 else
//                 {
//                     sb.append(identifier);
//                 }
// 
//             }
// 
//             return sb.toString();
//         }
//     }
// 
//     /**
//      * Cell renderer for identifier aliases
//      */
//     public class AliasedIdentifierCellRenderer extends DefaultTableCellRenderer
//     {
//         private Role mRole;
// 
//         /**
//          * Constructs an instance of the cell renderer.
//          *
//          * @param role of the identifier
//          */
//         public AliasedIdentifierCellRenderer(Role role)
//         {
//             mRole = role;
//             setHorizontalAlignment(javafx.geometry.javafx.geometry.Pos.CENTER);
//         }
// 
        // // @Override
        // public Component getTableCellRendererComponent(TableView table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
//         {
//             Label label = (Label)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
// 
//             Color color = mTable.getForeground();
//             Image icon = null;
//             String text = null;
// 
//             if(value instanceof IdentifierCollection)
//             {
//                 IdentifierCollection identifierCollection = (IdentifierCollection)value;
//                 List<Identifier> identifiers = identifierCollection.getIdentifiers(mRole);
// 
//                 if(identifiers != null && !identifiers.isEmpty())
//                 {
//                     AliasList aliasList = mAliasModel.getAliasList(identifierCollection);
// 
//                     if(aliasList != null)
//                     {
//                         StringBuilder sb = new StringBuilder();
// 
//                         for(Identifier identifier: identifiers)
//                         {
//                             List<Alias> aliases = aliasList.getAliases(identifier);
// 
//                             if(!aliases.isEmpty())
//                             {
//                                 if(sb.length() > 0)
//                                 {
//                                     sb.append(",");
//                                 }
//                                 sb.append(Joiner.on(", ").skipNulls().join(aliases));
//                                 color = aliases.get(0).getDisplayColor();
//                                 icon = mIconModel.getIcon(aliases.get(0).getIconName(), IconModel.DEFAULT_ICON_SIZE);
//                             }
//                         }
// 
//                         text = sb.toString();
//                     }
//                 }
//             }
// 
//             label.setText(text);
//             label.setTextFill(color);
//             label.setIcon(icon);
// 
//             return label;
//         }
//     }
// 
//     public class TimestampCellRenderer extends DefaultTableCellRenderer
//     {
//         private SimpleDateFormat mTimestampFormatter;
// 
//         public TimestampCellRenderer()
//         {
//             setHorizontalAlignment(javafx.geometry.javafx.geometry.Pos.CENTER);
//             updatePreferences();
//         }
// 
//         public void updatePreferences()
//         {
//             mTimestampFormatter = mUserPreferences.getDecodeEventPreference().getTimestampFormat().getFormatter();
//         }
// 
        // // @Override
//         public Component getTableCellRendererComponent(TableView table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
//         {
//             Label label = (Label)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
// 
//             if(value instanceof Long)
//             {
//                 label.setText(mTimestampFormatter.format(new Date((long)value)));
//             }
//             else
//             {
//                 label.setText(null);
//             }
// 
//             return label;
//         }
//     }
// 
//     public class DurationCellRenderer extends DefaultTableCellRenderer
//     {
//         private DecimalFormat mDecimalFormat = new DecimalFormat("0.0");
// 
//         public DurationCellRenderer()
//         {
//             setHorizontalAlignment(javafx.geometry.javafx.geometry.Pos.CENTER);
//         }
// 
        // // @Override
//         public Component getTableCellRendererComponent(TableView table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
//         {
//             Label label = (Label)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
// 
//             String formatted = null;
// 
//             if(value instanceof Long)
//             {
//                 long duration = (long)value;
// 
//                 if(duration > 0)
//                 {
//                     formatted = mDecimalFormat.format((double)duration / 1e3d);
//                 }
//             }
// 
//             label.setText(formatted);
// 
//             return label;
//         }
//     }
// 
//     /**
//      * Frequency value cell renderer
//      */
//     public class FrequencyCellRenderer extends DefaultTableCellRenderer
//     {
//         private DecimalFormat mFrequencyFormatter = new DecimalFormat("0.00000");
// 
//         public FrequencyCellRenderer()
//         {
//             setHorizontalAlignment(javafx.geometry.javafx.geometry.Pos.CENTER);
//         }
// 
        // // @Override
//         public Component getTableCellRendererComponent(TableView table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
//         {
//             Label label = (Label)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
// 
//             String formatted = null;
// 
//             if(value instanceof IChannelDescriptor)
//             {
//                 IChannelDescriptor channelDescriptor = (IChannelDescriptor)value;
// 
//                 long frequency = channelDescriptor.getDownlinkFrequency();
// 
//                 if(frequency > 0)
//                 {
//                     formatted = mFrequencyFormatter.format(frequency / 1e6d);
//                 }
//             }
// 
//             label.setText(formatted);
// 
//             return label;
//         }
//     }
// 
//     /**
//      * Channel descriptor value cell renderer
//      */
//     public class ChannelDescriptorCellRenderer extends DefaultTableCellRenderer
//     {
//         public ChannelDescriptorCellRenderer()
//         {
//             setHorizontalAlignment(javafx.geometry.javafx.geometry.Pos.CENTER);
//         }
//     }
// 
//     /**
//      * Row filter for decode events
//      */
//     public class EventRowFilter extends RowFilter<TableModel, Integer>
//     {
        // // @Override
//         public boolean include(Entry<? extends TableModel, ? extends Integer> entry)
//         {
//             if(entry.getValueFactory() instanceof DecodeEventModel model)
//             {
//                 IDecodeEvent event = model.getItem(entry.getIdentifier());
// 
//                 if(event != null)
//                 {
//                     return mFilterSet.canProcess(event) && mFilterSet.passes(event);
//                 }
//             }
// 
//             return false;
//         }
//     }
// }
// 
// }

}
