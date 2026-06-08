/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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
package io.github.dsheirer.channel.metadata;


import io.github.dsheirer.channel.details.ChannelDetailPanel;
import io.github.dsheirer.gui.channel.ChannelSpectrumPanel;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.module.decode.event.DecodeEventPanel;
import io.github.dsheirer.module.decode.event.MessageActivityPanel;
import io.github.dsheirer.gui.widget.Widget;
import io.github.dsheirer.gui.widget.WidgetContainer;
import io.github.dsheirer.preference.NowPlayingPreference;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.settings.SettingsManager;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import javafx.scene.paint.Color;

import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tab;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.Node;
import javafx.beans.value.ChangeListener;
import io.github.dsheirer.gui.VisibilityListener;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.module.ProcessingChain;
import javafx.application.Platform;

/**
 * Swing panel for Now Playing channels table and channel details tab set.
 */
public class NowPlayingPanel extends VBox implements Listener<ProcessingChain>
{
    private final ChannelMetadataPanel mChannelMetadataPanel;
    private final ChannelDetailPanel mChannelDetailPanel;
    private final DecodeEventPanel mDecodeEventPanel;
    private final MessageActivityPanel mMessageActivityPanel;
    private final ChannelSpectrumPanel mChannelSpectrumSquelchPanel;
    private TabPane mTabbedPane;
    private javafx.scene.Node mBroadcastStatusPanel;
    private javafx.scene.Node mResourceStatusPanel;
    private javafx.scene.Node mSpectralPanel;

    private VisibilityListener mVisibilityListener;
    private ChangeListener<Tab> mTabbedPaneChangeListener;
    private WidgetContainer mWidgetContainer;
    private GridPane mGridPane;
    private NowPlayingPreference mNowPlayingPreference;
    private Button mManageWidgetsBtn;

    // Widget references for grid management
    private Widget mSpectrumWidget;
    private Widget mChannelWidget;  // Combined Channel Table + Details
    private SplitPane mChannelSplitPane;
    private Widget mStreamingWidget;
    private Widget mResourceWidget;

    /**
     * GUI panel that combines the currently decoding channels metadata table and viewers for channel details,
     * messages, events, and spectral view.
     */
    public NowPlayingPanel(PlaylistManager playlistManager, IconModel iconModel, UserPreferences userPreferences,
                           SettingsManager settingsManager, TunerManager tunerManager, boolean detailTabsVisible, VisibilityListener visibilityListener)
    {
        mVisibilityListener = visibilityListener;
        mChannelDetailPanel = new ChannelDetailPanel(playlistManager.getChannelProcessingManager());
        mDecodeEventPanel = new DecodeEventPanel(iconModel, userPreferences, playlistManager.getAliasModel(), playlistManager.getChannelProcessingManager());
        mMessageActivityPanel = new MessageActivityPanel(userPreferences);
        mChannelMetadataPanel = new ChannelMetadataPanel(playlistManager.getChannelProcessingManager(), playlistManager, settingsManager, tunerManager.getDiscoveredTunerModel(), mNowPlayingPreference);
        mChannelSpectrumSquelchPanel = new ChannelSpectrumPanel(playlistManager, settingsManager);
        mNowPlayingPreference = new NowPlayingPreference();

        init();
    }

    /**
     * Dispose method to clean up listeners
     */
        @Override
    public void receive(ProcessingChain processingChain) {
        Platform.runLater(() -> {
            TabPane pane = getTabbedPane();

            if (processingChain == null) {
                // Remove all tabs except Events
                pane.getTabs().removeIf(tab -> tab.getContent() == mChannelDetailPanel);
                pane.getTabs().removeIf(tab -> tab.getContent() == mMessageActivityPanel);
                pane.getTabs().removeIf(tab -> tab.getContent() == mChannelSpectrumSquelchPanel);
                mChannelSpectrumSquelchPanel.setPanelVisible(false);
            } else {
                // Restore all tabs in correct order: Details, Events, Messages, Channel
                pane.getTabs().clear();
                pane.getTabs().add(new javafx.scene.control.Tab("Details", mChannelDetailPanel));
                pane.getTabs().add(new javafx.scene.control.Tab("Events", mDecodeEventPanel));
                pane.getTabs().add(new javafx.scene.control.Tab("Messages", mMessageActivityPanel));
                pane.getTabs().add(new javafx.scene.control.Tab("Channel", mChannelSpectrumSquelchPanel));
                // visibility managed elsewhere
            }
        });
    }

    public void dispose()
    {
        if(mTabbedPane != null && mTabbedPaneChangeListener != null)
        {
            mTabbedPane.getSelectionModel().selectedItemProperty().removeListener(mTabbedPaneChangeListener);
        }
    }

    public void setNodes(javafx.scene.Node spectralPanel, javafx.scene.Node broadcastStatusPanel, javafx.scene.Node resourceStatusPanel) {
        boolean initialized = (mSpectralPanel != null);
        mSpectralPanel = spectralPanel;
        mBroadcastStatusPanel = broadcastStatusPanel;
        mResourceStatusPanel = resourceStatusPanel;

        if (!initialized) {
            setupWidgets();
        } else {
            mWidgetContainer.ensureComponentInWidget("spectrum");
            mWidgetContainer.ensureComponentInWidget("streaming");
            mWidgetContainer.ensureComponentInWidget("resource");
        }
    }




    public void setSpectralPanelVisible(boolean visible) {
        if (mWidgetContainer != null) {
            mWidgetContainer.setWidgetVisible("spectrum", visible);
            rebuildGrid();
        }
    }

    public void setResourceStatusPanelVisible(boolean visible) {
        if (mWidgetContainer != null) {
            mWidgetContainer.setWidgetVisible("resource", visible);
            rebuildGrid();
        }
    }

    public void setBroadcastStatusPanelVisible(boolean visible) {
        if (mWidgetContainer != null) {
            mWidgetContainer.setWidgetVisible("streaming", visible);
            rebuildGrid();
        }
    }

    public void setDetailTabsVisible(boolean visible) {
        if (mWidgetContainer != null) {
            mWidgetContainer.setWidgetVisible("channel", visible);
            rebuildGrid();
        }
    }

    public void setBroadcastStatusPanel(javafx.scene.Node panel) {
        // Kept for backward compatibility if called elsewhere, actual injection happens in setNodes
        mBroadcastStatusPanel = panel;
    }

    private TabPane getTabbedPane() {
    if (mTabbedPane == null) {
        mTabbedPane = new TabPane();
        mTabbedPane.getTabs().add(new Tab("Details", mChannelDetailPanel));
        mTabbedPane.getTabs().add(new Tab("Events", mDecodeEventPanel));
        mTabbedPane.getTabs().add(new Tab("Messages", mMessageActivityPanel));
        mTabbedPane.getTabs().add(new Tab("Channel", mChannelSpectrumSquelchPanel));
    }
    return mTabbedPane;
}

    public Button getManageWidgetsButton() {
        if (mManageWidgetsBtn == null) {
            mManageWidgetsBtn = new Button("\u2699");
            mManageWidgetsBtn.setTooltip(new javafx.scene.control.Tooltip("Show or hide panels"));
            mManageWidgetsBtn.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-background-radius: 4; " +
                                       "-fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2 6 2 6;");
            mManageWidgetsBtn.setOnMouseEntered(e -> mManageWidgetsBtn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.18); -fx-background-radius: 4; " +
                "-fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2 6 2 6;"));
            mManageWidgetsBtn.setOnMouseExited(e -> mManageWidgetsBtn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.08); -fx-background-radius: 4; " +
                "-fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2 6 2 6;"));
            mManageWidgetsBtn.setOnAction(e -> showManageWidgetsPopup());
            mManageWidgetsBtn.setPadding(new javafx.geometry.Insets(2, 6, 2, 6));
            mManageWidgetsBtn.setCursor(javafx.scene.Cursor.HAND);
        }
        return mManageWidgetsBtn;
    }

    private void init()
    {

        mChannelMetadataPanel.addProcessingChainSelectionListener(mChannelDetailPanel);
        mChannelMetadataPanel.addProcessingChainSelectionListener(mDecodeEventPanel);
        mChannelMetadataPanel.addProcessingChainSelectionListener(mMessageActivityPanel);
        mChannelMetadataPanel.addProcessingChainSelectionListener(mChannelSpectrumSquelchPanel);
        mChannelMetadataPanel.addProcessingChainSelectionListener(this);

        mWidgetContainer = new WidgetContainer(mNowPlayingPreference);

        // Use GridPane instead of ScrollPane for no-scroll layout
        mGridPane = new GridPane();
        mGridPane.setVgap(2);
        mGridPane.setHgap(4);

        // Two equal-width columns (used for streaming + resource row)
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        col1.setHgrow(Priority.ALWAYS);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        col2.setHgrow(Priority.ALWAYS);
        mGridPane.getColumnConstraints().addAll(col1, col2);

        VBox.setVgrow(mGridPane, Priority.ALWAYS);

        getChildren().add(mGridPane);
    }

    private void setupWidgets() {
        mWidgetContainer.removeAll();

        if (mSpectralPanel != null) {
            mSpectrumWidget = new Widget("spectrum", "Spectrum/Waterfall", (Region) mSpectralPanel, mWidgetContainer, 150);
            mWidgetContainer.addWidget(mSpectrumWidget, false);
        }

        // Combined Channel Table + Channel Details widget using a horizontal SplitPane
        if (mChannelMetadataPanel != null && getTabbedPane() != null) {
            mChannelSplitPane = new SplitPane();
            mChannelSplitPane.setOrientation(javafx.geometry.Orientation.HORIZONTAL);
            mChannelSplitPane.getItems().addAll(mChannelMetadataPanel, getTabbedPane());
            mChannelSplitPane.setDividerPositions(0.40);
            mChannelSplitPane.setMinHeight(0);

            mChannelWidget = new Widget("channel", "Channel Table & Details", mChannelSplitPane, mWidgetContainer, 120);
            mWidgetContainer.addWidget(mChannelWidget, false);
        }

        if (mBroadcastStatusPanel != null) {
            mStreamingWidget = new Widget("streaming", "Streaming Status", (Region) mBroadcastStatusPanel, mWidgetContainer, 50);
            mWidgetContainer.addWidget(mStreamingWidget, false);
        }

        if (mResourceStatusPanel != null) {
            mResourceWidget = new Widget("resource", "Resource Status", (Region) mResourceStatusPanel, mWidgetContainer, 30);
            mResourceWidget.setMinimizeButtonVisible(false);
            mWidgetContainer.addWidget(mResourceWidget, true);
        }

        mWidgetContainer.layoutWidgets("resource");

        rebuildGrid();
    }

    /**
     * Rebuilds the grid layout based on current widget visibility.
     * Layout:
     *   Row 0: Spectrum (spans 2 columns)                         — 35% height
     *   Row 1: Channel Table & Details SplitPane (spans 2 cols)   — 45% height
     *   Row 2: Streaming | Resource (side-by-side)                — 20% height
     *
     * If a widget is hidden, remaining widgets share the space proportionally.
     */
    private void rebuildGrid() {
        mGridPane.getChildren().clear();
        mGridPane.getRowConstraints().clear();

        int row = 0;

        // Row 0: Spectrum (full width)
        if (mSpectrumWidget != null && mSpectrumWidget.isVisible()) {
            GridPane.setColumnSpan(mSpectrumWidget, 2);
            GridPane.setRowIndex(mSpectrumWidget, row);
            GridPane.setColumnIndex(mSpectrumWidget, 0);
            GridPane.setVgrow(mSpectrumWidget, Priority.ALWAYS);
            GridPane.setHgrow(mSpectrumWidget, Priority.ALWAYS);
            GridPane.setFillWidth(mSpectrumWidget, true);
            GridPane.setFillHeight(mSpectrumWidget, true);
            mGridPane.getChildren().add(mSpectrumWidget);

            RowConstraints spectrumRow = new RowConstraints();
            spectrumRow.setPercentHeight(35);
            spectrumRow.setVgrow(Priority.ALWAYS);
            mGridPane.getRowConstraints().add(spectrumRow);
            row++;
        }

        // Row 1: Combined Channel Table & Details (full width SplitPane)
        if (mChannelWidget != null && mChannelWidget.isVisible()) {
            GridPane.setColumnSpan(mChannelWidget, 2);
            GridPane.setRowIndex(mChannelWidget, row);
            GridPane.setColumnIndex(mChannelWidget, 0);
            GridPane.setVgrow(mChannelWidget, Priority.ALWAYS);
            GridPane.setHgrow(mChannelWidget, Priority.ALWAYS);
            GridPane.setFillWidth(mChannelWidget, true);
            GridPane.setFillHeight(mChannelWidget, true);
            mGridPane.getChildren().add(mChannelWidget);

            RowConstraints channelRow = new RowConstraints();
            channelRow.setPercentHeight(45);
            channelRow.setVgrow(Priority.ALWAYS);
            mGridPane.getRowConstraints().add(channelRow);
            row++;
        }

        // Row 2: Streaming + Resource (side-by-side, compact)
        boolean streamingVisible = mStreamingWidget != null && mStreamingWidget.isVisible();
        boolean resourceVisible = mResourceWidget != null && mResourceWidget.isVisible();

        if (streamingVisible || resourceVisible) {
            if (streamingVisible && resourceVisible) {
                GridPane.setColumnSpan(mStreamingWidget, 1);
                GridPane.setRowIndex(mStreamingWidget, row);
                GridPane.setColumnIndex(mStreamingWidget, 0);
                GridPane.setVgrow(mStreamingWidget, Priority.SOMETIMES);
                GridPane.setHgrow(mStreamingWidget, Priority.ALWAYS);
                GridPane.setFillWidth(mStreamingWidget, true);
                GridPane.setFillHeight(mStreamingWidget, true);
                mGridPane.getChildren().add(mStreamingWidget);

                GridPane.setColumnSpan(mResourceWidget, 1);
                GridPane.setRowIndex(mResourceWidget, row);
                GridPane.setColumnIndex(mResourceWidget, 1);
                GridPane.setVgrow(mResourceWidget, Priority.SOMETIMES);
                GridPane.setHgrow(mResourceWidget, Priority.ALWAYS);
                GridPane.setFillWidth(mResourceWidget, true);
                GridPane.setFillHeight(mResourceWidget, true);
                mGridPane.getChildren().add(mResourceWidget);
            } else if (streamingVisible) {
                GridPane.setColumnSpan(mStreamingWidget, 2);
                GridPane.setRowIndex(mStreamingWidget, row);
                GridPane.setColumnIndex(mStreamingWidget, 0);
                GridPane.setVgrow(mStreamingWidget, Priority.SOMETIMES);
                GridPane.setHgrow(mStreamingWidget, Priority.ALWAYS);
                GridPane.setFillWidth(mStreamingWidget, true);
                GridPane.setFillHeight(mStreamingWidget, true);
                mGridPane.getChildren().add(mStreamingWidget);
            } else {
                GridPane.setColumnSpan(mResourceWidget, 2);
                GridPane.setRowIndex(mResourceWidget, row);
                GridPane.setColumnIndex(mResourceWidget, 0);
                GridPane.setVgrow(mResourceWidget, Priority.SOMETIMES);
                GridPane.setHgrow(mResourceWidget, Priority.ALWAYS);
                GridPane.setFillWidth(mResourceWidget, true);
                GridPane.setFillHeight(mResourceWidget, true);
                mGridPane.getChildren().add(mResourceWidget);
            }

            RowConstraints bottomRow = new RowConstraints();
            bottomRow.setPercentHeight(20);
            bottomRow.setVgrow(Priority.SOMETIMES);
            mGridPane.getRowConstraints().add(bottomRow);
        }

        mGridPane.requestLayout();
    }

    private void showManageWidgetsPopup() {
        javafx.scene.control.ContextMenu popup = new javafx.scene.control.ContextMenu();

        if (mSpectralPanel != null) addPopupItem(popup, "Spectrum/Waterfall", "spectrum");
        if (mChannelWidget != null) addPopupItem(popup, "Channel Table & Details", "channel");
        if (mBroadcastStatusPanel != null) addPopupItem(popup, "Streaming Status", "streaming");
        if (mResourceStatusPanel != null) addPopupItem(popup, "Resource Status", "resource");

        popup.show(mManageWidgetsBtn, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    private void addPopupItem(ContextMenu popup, String label, String widgetId) {
        CheckMenuItem item = new CheckMenuItem(label);
        item.setSelected(mNowPlayingPreference.isWidgetVisible(widgetId, true));
        item.setOnAction(e -> {
            mWidgetContainer.setWidgetVisible(widgetId, item.isSelected());
            rebuildGrid();
        });
        popup.getItems().add(item);
    }
}
