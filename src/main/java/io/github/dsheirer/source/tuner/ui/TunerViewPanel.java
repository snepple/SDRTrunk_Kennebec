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

package io.github.dsheirer.source.tuner.ui;

import com.jidesoft.swing.JideSplitPane;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.swing.JTableColumnWidthMonitor;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationManager;
import io.github.dsheirer.source.tuner.manager.DiscoveredRecordingTuner;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.manager.TunerStatus;
import io.github.dsheirer.source.tuner.recording.AddRecordingTunerDialog;
import com.google.common.eventbus.Subscribe;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.application.Platform;
import java.util.Optional;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.List;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import io.github.dsheirer.gui.VisibilityListener;

import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.embed.swing.SwingNode;
import javafx.scene.Scene;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javax.swing.SwingUtilities;

/**
 * Panel containing a discovered tuners table and a tuner editor for a selected tuner.
 */
public class TunerViewPanel extends JFXPanel
{
    private static final long serialVersionUID = 1L;
    private final static Logger mLog = LoggerFactory.getLogger(TunerViewPanel.class);
    private static final String TABLE_PREFERENCE_KEY = "tuner.view.panel";

    private UserPreferences mUserPreferences;
    private DiscoveredTunerModel mDiscoveredTunerModel;
    private DiscoveredTunerEditor mDiscoveredTunerEditor;
    private TunerConfigurationManager mTunerConfigurationManager;
    private JTable mTunerTable;
    private JTableColumnWidthMonitor mColumnWidthMonitor;
    private TableRowSorter<DiscoveredTunerModel> mRowSorter;
    private SplitPane mSplitPane;
    private JButton mAddRecordingButton;
    private JButton mRemoveRecordingButton;

    /**
     * Constructs an instance
     * @param tunerManager for tuners
     * @param userPreferences for making recordings in the tuner editor
     */
    private VisibilityListener mVisibilityListener;

    public TunerViewPanel(TunerManager tunerManager, UserPreferences userPreferences, VisibilityListener visibilityListener)
    {
        mVisibilityListener = visibilityListener;
        mDiscoveredTunerModel = tunerManager.getDiscoveredTunerModel();
        mDiscoveredTunerEditor = new DiscoveredTunerEditor(userPreferences, tunerManager);
        mTunerConfigurationManager = tunerManager.getTunerConfigurationManager();
        mUserPreferences = userPreferences;
        init();
    }

    @Subscribe
    public void process(USBAlertEvent event)
    {
        EventQueue.invokeLater(() -> {
            Platform.runLater(() -> { Alert alert = new Alert(Alert.AlertType.WARNING); alert.setContentText(String.valueOf(event.getMessage())); alert.showAndWait(); });
        });
    }

    private void init()
    {
        io.github.dsheirer.eventbus.MyEventBus.getGlobalEventBus().register(this);

        Platform.runLater(() -> {
            VBox root = new VBox();

            ToolBar toolBar = new ToolBar();

            javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
            javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

            MenuButton manageBtn = new MenuButton();
            manageBtn.setGraphic(new javafx.scene.image.ImageView(new javafx.scene.image.Image(getClass().getResourceAsStream("/images/settings_16x16.png"))));
            manageBtn.setTooltip(new javafx.scene.control.Tooltip("Settings"));

            manageBtn.setOnShowing(event -> {
                manageBtn.getItems().clear();

                MenuItem specItem = new MenuItem();
                if (mVisibilityListener != null) {
                    specItem.setText(mVisibilityListener.isSpectrumVisible() ? "Hide Spectrum/Waterfall" : "Show Spectrum/Waterfall");
                } else {
                    specItem.setText("Toggle Spectrum/Waterfall");
                }
                specItem.setOnAction(evt -> {
                    if(mVisibilityListener != null) mVisibilityListener.onToggleSpectrum();
                });

                MenuItem resourceItem = new MenuItem();
                if (mVisibilityListener != null) {
                    resourceItem.setText(mVisibilityListener.isResourceVisible() ? "Hide Resource Status" : "Show Resource Status");
                } else {
                    resourceItem.setText("Toggle Resource Status");
                }
                resourceItem.setOnAction(evt -> {
                    if(mVisibilityListener != null) mVisibilityListener.onToggleResource();
                });

                manageBtn.getItems().addAll(specItem, resourceItem);
            });

            toolBar.getItems().addAll(spacer, manageBtn);

            root.getChildren().add(toolBar);

            Scene scene = new Scene(root);
            java.net.URL cssUrl = getClass().getResource("/sdrtrunk_style.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
            setScene(scene);
        });

        mRowSorter = new TableRowSorter<>(mDiscoveredTunerModel);
        List<RowSorter.SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(DiscoveredTunerModel.COLUMN_TUNER_TYPE, SortOrder.ASCENDING));
        mRowSorter.setSortKeys(sortKeys);

        mTunerTable = new JTable(mDiscoveredTunerModel);
        mTunerTable.setFillsViewportHeight(true);
        mTunerTable.setRowSorter(mRowSorter);
        mTunerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mTunerTable.getSelectionModel().addListSelectionListener(event ->
        {
            getRemoveRecordingButton().setEnabled(false);

            if(!event.getValueIsAdjusting())
            {
                int row = mTunerTable.getSelectedRow();

                if(row >= 0)
                {
                    int modelRow = mTunerTable.convertRowIndexToModel(row);

                    DiscoveredTuner selected = mDiscoveredTunerModel.getDiscoveredTuner(modelRow);
                    mDiscoveredTunerEditor.setItem(selected);
                    getRemoveRecordingButton().setEnabled(selected instanceof DiscoveredRecordingTuner);
                }
            }
        });

        //Add support for right-click context menu to the tuner table
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem logStateMenuItem = new JMenuItem("Log Tuner State");
        logStateMenuItem.addActionListener(e -> {
            int viewRow = mTunerTable.getSelectedRow();

            DiscoveredTuner selected = null;

            if(viewRow >= 0)
            {
                int modelRow = mTunerTable.convertRowIndexToModel(viewRow);
                selected = mDiscoveredTunerModel.getDiscoveredTuner(modelRow);
            }

            if(selected != null)
            {
                selected.logState();
            }
            else
            {
                mLog.error("Can't log state - tuner not selected");
            }
        });
        popupMenu.add(logStateMenuItem);
        mTunerTable.setComponentPopupMenu(popupMenu);

        //Monitor for tuner removal events so we can update the editor when our selected tuner is removed
        mDiscoveredTunerModel.addTableModelListener(e ->
        {
            //Detect when status is for the currently selected tuner
            if(e.getType() == TableModelEvent.DELETE &&
                mDiscoveredTunerEditor.hasItem() &&
                !mDiscoveredTunerModel.hasTuner(mDiscoveredTunerEditor.getItem()))
            {
                mDiscoveredTunerEditor.setItem(null);
            }
        });

        mDiscoveredTunerModel.addListener(tunerEvent ->
        {
            switch(tunerEvent.getEvent())
            {
                case UPDATE_LOCK_STATE:
                    if(tunerEvent.getTuner() != null)
                    {
                        int row = mTunerTable.getSelectedRow();

                        if(row >= 0)
                        {
                            int modelRow = mTunerTable.convertRowIndexToModel(row);
                            DiscoveredTuner selectedTuner = mDiscoveredTunerModel.getDiscoveredTuner(modelRow);

                            if(selectedTuner != null && selectedTuner.hasTuner() && tunerEvent.getTuner() == selectedTuner.getTuner())
                            {
                                mDiscoveredTunerEditor.setTunerLockState(selectedTuner.getTuner().getTunerController().isLockedSampleRate());
                            }
                        }
                    }
                    break;
            }
        });

        TableCellRenderer errorCellRenderer = new TunerStatusCellRenderer();
        mTunerTable.getColumnModel().getColumn(DiscoveredTunerModel.COLUMN_TUNER_STATUS).setCellRenderer(errorCellRenderer);

        mColumnWidthMonitor = new JTableColumnWidthMonitor(mUserPreferences, mTunerTable, TABLE_PREFERENCE_KEY);
        JScrollPane tunerTableScroller = new JScrollPane(mTunerTable);

        Platform.runLater(() -> {
            VBox root = (VBox) getScene().getRoot();

            mSplitPane = new SplitPane();
            VBox.setVgrow(mSplitPane, javafx.scene.layout.Priority.ALWAYS);

            VBox leftPane = new VBox();
            leftPane.setSpacing(5);
            leftPane.setStyle("-fx-padding: 5;");

            SwingNode tableSwingNode = new SwingNode();
            SwingUtilities.invokeLater(() -> {
                tableSwingNode.setContent(tunerTableScroller);
            });
            VBox.setVgrow(tableSwingNode, javafx.scene.layout.Priority.ALWAYS);

            SwingNode buttonSwingNode = new SwingNode();
            SwingUtilities.invokeLater(() -> {
                JPanel buttonPanel = new JPanel();
                buttonPanel.setLayout(new MigLayout("insets 0 1 3 0", "", ""));
                buttonPanel.add(getAddRecordingButton());
                buttonPanel.add(getRemoveRecordingButton());
                buttonSwingNode.setContent(buttonPanel);
            });

            leftPane.getChildren().addAll(tableSwingNode, buttonSwingNode);

            SwingNode editorSwingNode = new SwingNode();
            SwingUtilities.invokeLater(() -> {
                JScrollPane editorScroller = new JScrollPane(mDiscoveredTunerEditor);
                editorScroller.setPreferredSize(new Dimension(200, 200));
                editorSwingNode.setContent(editorScroller);
            });

            mSplitPane.getItems().addAll(leftPane, editorSwingNode);
            mSplitPane.setDividerPositions(0.5);

            root.getChildren().add(mSplitPane);
        });
    }

    private JButton getAddRecordingButton()
    {
        if(mAddRecordingButton == null)
        {
            mAddRecordingButton = new JButton("Add Recording Tuner");
            mAddRecordingButton.setMnemonic(java.awt.event.KeyEvent.VK_A);
            mAddRecordingButton.setToolTipText("Add a new recording tuner to the workspace");
            mAddRecordingButton.getAccessibleContext().setAccessibleName("Add Recording Tuner");
            mAddRecordingButton.getAccessibleContext().setAccessibleDescription("Opens a dialog to add a new recording tuner");
            mAddRecordingButton.addActionListener(e ->
            {
                AddRecordingTunerDialog dialog = new AddRecordingTunerDialog(mUserPreferences, mDiscoveredTunerModel,
                        mTunerConfigurationManager);

                EventQueue.invokeLater(() -> dialog.setVisible(true));
            });
        }

        return mAddRecordingButton;
    }

    private JButton getRemoveRecordingButton()
    {
        if(mRemoveRecordingButton == null)
        {
            mRemoveRecordingButton = new JButton("Remove Recording Tuner");
            mRemoveRecordingButton.setMnemonic(java.awt.event.KeyEvent.VK_R);
            mRemoveRecordingButton.setToolTipText("Permanently remove the selected recording tuner. This action cannot be undone.");
            mRemoveRecordingButton.getAccessibleContext().setAccessibleName("Remove Recording Tuner");
            mRemoveRecordingButton.getAccessibleContext().setAccessibleDescription("Removes the currently selected recording tuner");
            mRemoveRecordingButton.setEnabled(false);
            mRemoveRecordingButton.addActionListener(e -> {
                int[] indexes = mTunerTable.getSelectionModel().getSelectedIndices();

                //With single selection mode this should always be length one
                if(indexes.length == 1)
                {
                    int modelIndex = mTunerTable.convertRowIndexToModel(indexes[0]);
                    DiscoveredTuner selected = mDiscoveredTunerModel.getDiscoveredTuner(modelIndex);

                    if(selected instanceof DiscoveredRecordingTuner discoveredRecordingTuner)
                    {
                        mLog.info("Removing Tuner: " + discoveredRecordingTuner);
                        discoveredRecordingTuner.stop();
                        mTunerConfigurationManager.removeTunerConfiguration(discoveredRecordingTuner.getTunerConfiguration());
                        EventQueue.invokeLater(() -> mDiscoveredTunerModel.removeDiscoveredTuner(discoveredRecordingTuner));
                    }
                }
            });
        }

        return mRemoveRecordingButton;
    }

    /**
     * Custom cell renderer for the TunerStatus enumeration column
     */
    public class TunerStatusCellRenderer extends DefaultTableCellRenderer
    {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                       int row, int column)
        {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if(value instanceof TunerStatus tunerStatus)
            {
                if(tunerStatus == TunerStatus.ERROR)
                {
                    component.setForeground(Color.RED);
                }
                else if(tunerStatus == TunerStatus.DISABLED)
                {
                    component.setForeground(Color.DARK_GRAY);
                }
                else
                {
                    component.setForeground(table.getForeground());
                }
            }
            else
            {
                component.setForeground(table.getForeground());
            }

            return component;
        }
    }
}
