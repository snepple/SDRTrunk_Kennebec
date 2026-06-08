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
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
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
    private NowPlayingPreference mNowPlayingPreference;
    private Button mManageWidgetsBtn;
    private SplitPane mChannelSplitPane;

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
        }
    }

    public void setResourceStatusPanelVisible(boolean visible) {
        if (mWidgetContainer != null) {
            mWidgetContainer.setWidgetVisible("resource", visible);
        }
    }

    public void setBroadcastStatusPanelVisible(boolean visible) {
        if (mWidgetContainer != null) {
            mWidgetContainer.setWidgetVisible("streaming", visible);
        }
    }

    public void setDetailTabsVisible(boolean visible) {
        if (mWidgetContainer != null) {
            mWidgetContainer.setWidgetVisible("channel", visible);
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

        // Place WidgetContainer directly in VBox — no ScrollPane, no GridPane.
        // The WidgetContainer (extends VBox) handles drag-to-reorder natively.
        // Each widget gets VBox.setVgrow(Priority.ALWAYS) to share available space.
        VBox.setVgrow(mWidgetContainer, Priority.ALWAYS);
        getChildren().add(mWidgetContainer);
    }

    private void setupWidgets() {
        mWidgetContainer.removeAll();

        if (mSpectralPanel != null) {
            Widget spectrumWidget = new Widget("spectrum", "Spectrum/Waterfall", (Region) mSpectralPanel, mWidgetContainer, 100);
            VBox.setVgrow(spectrumWidget, Priority.ALWAYS);
            mWidgetContainer.addWidget(spectrumWidget, false);
        }

        // Combined Channel Table + Channel Details widget using a horizontal SplitPane
        if (mChannelMetadataPanel != null && getTabbedPane() != null) {
            mChannelSplitPane = new SplitPane();
            mChannelSplitPane.setOrientation(javafx.geometry.Orientation.HORIZONTAL);
            mChannelSplitPane.getItems().addAll(mChannelMetadataPanel, getTabbedPane());
            mChannelSplitPane.setDividerPositions(0.40);
            mChannelSplitPane.setMinHeight(0);

            Widget channelWidget = new Widget("channel", "Channel Table & Details", mChannelSplitPane, mWidgetContainer, 100);
            VBox.setVgrow(channelWidget, Priority.ALWAYS);
            mWidgetContainer.addWidget(channelWidget, false);
        }

        if (mBroadcastStatusPanel != null) {
            Widget streamingWidget = new Widget("streaming", "Streaming Status", (Region) mBroadcastStatusPanel, mWidgetContainer, 40);
            VBox.setVgrow(streamingWidget, Priority.SOMETIMES);
            mWidgetContainer.addWidget(streamingWidget, false);
        }

        if (mResourceStatusPanel != null) {
            Widget resourceWidget = new Widget("resource", "Resource Status", (Region) mResourceStatusPanel, mWidgetContainer, 30);
            resourceWidget.setMinimizeButtonVisible(false);
            VBox.setVgrow(resourceWidget, Priority.SOMETIMES);
            mWidgetContainer.addWidget(resourceWidget, true);
        }

        mWidgetContainer.layoutWidgets("resource");
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
        item.setOnAction(e -> mWidgetContainer.setWidgetVisible(widgetId, item.isSelected()));
        popup.getItems().add(item);
    }
}
