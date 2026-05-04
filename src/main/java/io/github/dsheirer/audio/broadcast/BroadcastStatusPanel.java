/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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
package io.github.dsheirer.audio.broadcast;

import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.playlist.streaming.ViewStreamRequest;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import io.github.dsheirer.icon.Icon;
import io.github.dsheirer.audio.broadcast.BroadcastEvent;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.swing.JTableColumnWidthMonitor;
import java.awt.Color;
import java.awt.Component;
import net.miginfocom.swing.MigLayout;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JPopupMenu;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import javax.swing.RowFilter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JPopupMenu;
import javax.swing.JCheckBoxMenuItem;


/**
 * Table of broadcast streams and statuses.
 */
public class BroadcastStatusPanel extends JPanel
{
    private JTable mTable;
    private JTableColumnWidthMonitor mColumnWidthMonitor;
    private JScrollPane mScrollPane;
    private BroadcastModel mBroadcastModel;
    private UserPreferences mUserPreferences;
    private String mPreferenceKey;

    /**
     * Constructs an instance
     * @param broadcastModel to access the streams
     * @param userPreferences for configuring the panel
     * @param preferenceKey to store column preferences for this panel.
     */
    public BroadcastStatusPanel(BroadcastModel broadcastModel, UserPreferences userPreferences, String preferenceKey)
    {
        mBroadcastModel = broadcastModel;
        mUserPreferences = userPreferences;
        mPreferenceKey = preferenceKey;

        init();
    }

    public JTable getTable()
    {
        return mTable;
    }

    private TableRowSorter<BroadcastModel> mRowSorter;

    private void init()
    {
        setLayout(new MigLayout("insets 0 0 0 0 ", "[grow,fill]", "[grow,fill]"));

        mTable = new JTable(mBroadcastModel);

        mRowSorter = new TableRowSorter<>(mBroadcastModel);
        mTable.setRowSorter(mRowSorter);

        DefaultTableCellRenderer renderer = (DefaultTableCellRenderer)mTable.getDefaultRenderer(String.class);
        renderer.setHorizontalAlignment(SwingConstants.CENTER);

        mTable.getColumnModel().getColumn(BroadcastModel.COLUMN_BROADCASTER_STATUS).setCellRenderer(new StatusCellRenderer());
        mTable.getColumnModel().getColumn(BroadcastModel.COLUMN_BROADCAST_SERVER_TYPE).setCellRenderer(new ServerTypeRenderer());
        mColumnWidthMonitor = new JTableColumnWidthMonitor(mUserPreferences, mTable, mPreferenceKey);

        mTable.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    JPopupMenu popup = new JPopupMenu();
                    JCheckBoxMenuItem hideDisabledItem = new JCheckBoxMenuItem("Hide Disabled Streams");

                    hideDisabledItem.addActionListener(evt -> {
                        if (hideDisabledItem.isSelected()) {
                            mRowSorter.setRowFilter(new RowFilter<BroadcastModel, Integer>() {
                                @Override
                                public boolean include(Entry<? extends BroadcastModel, ? extends Integer> entry) {
                                    BroadcastModel model = entry.getModel();
                                    BroadcastState state = (BroadcastState) model.getValueAt(entry.getIdentifier(), BroadcastModel.COLUMN_BROADCASTER_STATUS);
                                    return state != BroadcastState.DISABLED;
                                }
                            });
                        } else {
                            mRowSorter.setRowFilter(null);
                        }
                    });

                    popup.add(hideDisabledItem);
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        mTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int viewRowIndex = mTable.rowAtPoint(e.getPoint());
                if (viewRowIndex >= 0) {
                    if (e.getButton() == MouseEvent.BUTTON3) {
                        mTable.setRowSelectionInterval(viewRowIndex, viewRowIndex);
                        int modelRowIndex = mTable.convertRowIndexToModel(viewRowIndex);
                        if (modelRowIndex >= 0) {
                            String streamName = (String) mBroadcastModel.getValueAt(modelRowIndex, BroadcastModel.COLUMN_STREAM_NAME);
                            BroadcastConfiguration config = mBroadcastModel.getBroadcastConfiguration(streamName);
                            if (config != null) {
                                JPopupMenu popup = new JPopupMenu();
                                JCheckBoxMenuItem enableItem = new JCheckBoxMenuItem("Enable", config.isEnabled());
                                enableItem.addActionListener(evt -> {
                                    config.setEnabled(enableItem.isSelected());
                                    mBroadcastModel.process(new BroadcastEvent(config, BroadcastEvent.Event.CONFIGURATION_CHANGE));
                                });
                                popup.add(enableItem);
                                popup.show(e.getComponent(), e.getX(), e.getY());
                            }
                        }
                    } else if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                        int modelRowIndex = mTable.convertRowIndexToModel(viewRowIndex);
                        if (modelRowIndex >= 0) {
                            String streamName = (String) mBroadcastModel.getValueAt(modelRowIndex, BroadcastModel.COLUMN_STREAM_NAME);
                            BroadcastConfiguration config = mBroadcastModel.getBroadcastConfiguration(streamName);
                            if (config != null) {
                                MyEventBus.getGlobalEventBus().post(new ViewStreamRequest(config));
                            }
                        }
                    }
                }
            }
        });

        mTable.setFillsViewportHeight(true);
        mScrollPane = new JScrollPane(mTable);

        add(mScrollPane, "grow");
    }

    public class ServerTypeRenderer extends DefaultTableCellRenderer
    {
        public ServerTypeRenderer()
        {
            setOpaque(true);
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            JLabel component = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if(value instanceof BroadcastServerType broadcastServerType)
            {
                component.setText(broadcastServerType.toString());
                Icon icon = new Icon("empty", broadcastServerType.getIconPath());
                ImageIcon imageIcon = icon.getIcon();
                ImageIcon scaledIcon = IconModel.getScaledIcon(imageIcon, 13);
                component.setIcon(scaledIcon);
            }
            else
            {
                component.setText(null);
                component.setIcon(null);
            }

            return component;
        }
    }

    /**
     * Custom cell renderer for the broadcast state column.
     */
    public class StatusCellRenderer extends DefaultTableCellRenderer
    {
        public StatusCellRenderer()
        {
            setOpaque(true);
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column)
        {
            JLabel component = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if(isSelected)
            {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            }
            else
            {
                if(value instanceof BroadcastState)
                {
                    BroadcastState state = (BroadcastState)value;

                    if(state == BroadcastState.CONNECTED)
                    {
                        setBackground(Color.GREEN);
                        setForeground(table.getForeground());
                    }
                    else if(state == BroadcastState.DISABLED)
                    {
                        setBackground(table.getBackground());
                        setForeground(Color.LIGHT_GRAY);
                    }
                    else if(state == BroadcastState.INVALID_SETTINGS ||
                            state == BroadcastState.NETWORK_UNAVAILABLE)
                    {
                        setBackground(Color.YELLOW);
                        setForeground(table.getForeground());
                    }
                    else if(state.isErrorState())
                    {
                        setBackground(Color.RED);
                        setForeground(table.getForeground());
                    }
                    else
                    {
                        setBackground(table.getBackground());
                        setForeground(table.getForeground());
                    }
                }
                else
                {
                    setForeground(table.getForeground());
                    setBackground(table.getBackground());
                }
            }

            return this;
        }
    }
}
