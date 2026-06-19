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
import javafx.geometry.Pos;
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
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;

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
    private Widget mSpectrumWidget;

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
                // Remove TabPane from split pane to collapse it
                if (mChannelSplitPane != null && mChannelSplitPane.getItems().contains(pane)) {
                    mChannelSplitPane.getItems().remove(pane);
                }
                mChannelSpectrumSquelchPanel.setPanelVisible(false);
            } else {
                // Restore TabPane to split pane to expand it
                if (mChannelSplitPane != null && !mChannelSplitPane.getItems().contains(pane)) {
                    mChannelSplitPane.getItems().add(pane);
                    mChannelSplitPane.setDividerPositions(0.40);
                }
                // Restore all tabs in correct order: Details, Events, Messages, Channel Spectrum, Advanced
                pane.getTabs().clear();
                pane.getTabs().add(new javafx.scene.control.Tab("Details", mChannelDetailPanel));
                pane.getTabs().add(new javafx.scene.control.Tab("Events", mDecodeEventPanel));
                pane.getTabs().add(new javafx.scene.control.Tab("Messages", mMessageActivityPanel));
                pane.getTabs().add(new javafx.scene.control.Tab("Channel Spectrum", mChannelSpectrumSquelchPanel));
                
                javafx.scene.control.Tab advancedTab = new javafx.scene.control.Tab("Advanced", getAdvancedTabContent());
                pane.getTabs().add(advancedTab);
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
            mWidgetContainer.ensureComponentInWidget("spectrum_v2");
            mWidgetContainer.ensureComponentInWidget("streaming");
            mWidgetContainer.ensureComponentInWidget("resource");
        }
    }

    public void toggleDetailsPane() {
        Platform.runLater(() -> {
            if (mChannelSplitPane != null) {
                TabPane pane = getTabbedPane();
                if (mChannelSplitPane.getItems().contains(pane)) {
                    javafx.animation.Timeline timeline = new javafx.animation.Timeline();
                    javafx.animation.KeyValue kv = new javafx.animation.KeyValue(mChannelSplitPane.getDividers().get(0).positionProperty(), 1.0, javafx.animation.Interpolator.EASE_BOTH);
                    javafx.animation.KeyFrame kf = new javafx.animation.KeyFrame(javafx.util.Duration.millis(250), kv);
                    timeline.getKeyFrames().add(kf);
                    timeline.setOnFinished(e -> mChannelSplitPane.getItems().remove(pane));
                    timeline.play();
                } else {
                    mChannelSplitPane.getItems().add(pane);
                    mChannelSplitPane.setDividerPositions(1.0);
                    javafx.animation.Timeline timeline = new javafx.animation.Timeline();
                    javafx.animation.KeyValue kv = new javafx.animation.KeyValue(mChannelSplitPane.getDividers().get(0).positionProperty(), 0.40, javafx.animation.Interpolator.EASE_BOTH);
                    javafx.animation.KeyFrame kf = new javafx.animation.KeyFrame(javafx.util.Duration.millis(250), kv);
                    timeline.getKeyFrames().add(kf);
                    timeline.play();
                }
            }
        });
    }

    public boolean isDetailsPaneVisible() {
        if (mChannelSplitPane != null) {
            return mChannelSplitPane.getItems().contains(getTabbedPane());
        }
        return false;
    }




    public void setSpectralPanelVisible(boolean visible) {
        if (mWidgetContainer != null) {
            mWidgetContainer.setWidgetVisible("spectrum_v2", visible);
        }
    }

    /**
     * Starts or stops the spectral display's DFT processor based on whether the Spectrum/Waterfall widget is
     * currently visible and expanded.  Stopping the processor pauses both the spectrum and the waterfall
     * computations, saving CPU while the widget is minimized or hidden.  Safe to call repeatedly; start() is a
     * no-op when no tuner is selected.
     */
    public void updateSpectrumProcessing() {
        if (mSpectrumWidget != null && mSpectralPanel instanceof io.github.dsheirer.spectrum.SpectralDisplayPanel sdp) {
            if (mSpectrumWidget.isVisible() && !mSpectrumWidget.isMinimized()) {
                sdp.start();
            } else {
                sdp.stop();
            }
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
        
        Tab detailsTab = new Tab("Details", mChannelDetailPanel);
        Tab eventsTab = new Tab("Events", mDecodeEventPanel);
        Tab messagesTab = new Tab("Messages", mMessageActivityPanel);
        Tab spectrumTab = new Tab("Channel Spectrum", mChannelSpectrumSquelchPanel);
        Tab advancedTab = new Tab("Advanced", getAdvancedTabContent());
        
        makeTabTearable(detailsTab);
        makeTabTearable(eventsTab);
        makeTabTearable(messagesTab);
        makeTabTearable(spectrumTab);
        makeTabTearable(advancedTab);
        
        // Add tear-off hints and icons to all tabs
        for (Tab tab : new Tab[]{detailsTab, eventsTab, messagesTab, spectrumTab, advancedTab}) {
            tab.setTooltip(new javafx.scene.control.Tooltip(tab.getText() + " — Right-click to pop out"));
            IconNode popIcon = new IconNode(FontAwesome.EXTERNAL_LINK);
            popIcon.setIconSize(10);
            popIcon.setFill(javafx.scene.paint.Color.web("#C7C7CC"));
            
            javafx.scene.control.Label tabLabel = new javafx.scene.control.Label(tab.getText(), popIcon);
            tabLabel.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
            tabLabel.setGraphicTextGap(6);
            tabLabel.getStyleClass().add("tab-label");
            
            tab.setText(""); // clear text, use custom label
            tab.setGraphic(tabLabel);
        }

        mTabbedPane.getTabs().addAll(detailsTab, eventsTab, messagesTab, spectrumTab, advancedTab);
    }
    return mTabbedPane;
}

    private javafx.scene.Node getAdvancedTabContent() {
        javafx.scene.Node squelchView = mChannelSpectrumSquelchPanel.getNoiseSquelchView();
        javafx.scene.Node powerView = mChannelSpectrumSquelchPanel.getSignalPowerView();
        javafx.scene.Node symbolView = mChannelSpectrumSquelchPanel.getSymbolView();
        javafx.scene.Node logSettingsView = mChannelSpectrumSquelchPanel.getLogSettingsNode();

        javafx.scene.control.TitledPane squelchPane = new javafx.scene.control.TitledPane("Squelch", squelchView);
        javafx.scene.control.TitledPane powerPane = new javafx.scene.control.TitledPane("Signal Power", powerView);
        javafx.scene.control.TitledPane symbolsPane = new javafx.scene.control.TitledPane("Symbols", symbolView);
        javafx.scene.control.TitledPane logPane = new javafx.scene.control.TitledPane("Log Settings", logSettingsView);

        javafx.scene.control.Accordion accordion = new javafx.scene.control.Accordion();
        accordion.getPanes().addAll(squelchPane, powerPane, symbolsPane, logPane);
        accordion.setExpandedPane(squelchPane);

        javafx.scene.control.ScrollPane scrollWrapper = new javafx.scene.control.ScrollPane(accordion);
        scrollWrapper.setFitToWidth(true);
        scrollWrapper.setStyle("-fx-background-color: transparent;");

        return scrollWrapper;
    }

    private void makeTabTearable(Tab tab) {
        ContextMenu contextMenu = new ContextMenu();
        javafx.scene.control.MenuItem popOutItem = new javafx.scene.control.MenuItem("Pop Out / Tear Off");
        popOutItem.setOnAction(e -> {
            TabPane parentPane = tab.getTabPane();
            if (parentPane != null) {
                parentPane.getTabs().remove(tab);
            }
            
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle(tab.getText());
            
            TabPane standalonePane = new TabPane();
            standalonePane.getTabs().add(tab);
            
            javafx.scene.Scene scene = new javafx.scene.Scene(standalonePane, 800, 600);
            stage.setScene(scene);
            
            stage.setOnCloseRequest(event -> {
                standalonePane.getTabs().remove(tab);
                if (mTabbedPane != null) {
                    mTabbedPane.getTabs().add(tab);
                }
            });
            
            stage.show();
        });
        
        tab.setContextMenu(contextMenu);
    }

    public Button getManageWidgetsButton() {
        if (mManageWidgetsBtn == null) {
            mManageWidgetsBtn = new Button("\u2699");
            mManageWidgetsBtn.setTooltip(new javafx.scene.control.Tooltip("Show or hide panels"));
            mManageWidgetsBtn.getStyleClass().add("kennebec-toolbar-button");
            mManageWidgetsBtn.setStyle("-fx-font-size: 14px;");
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

        // Float the "manage widgets" gear in the bottom-right corner, overlaid on the content, instead
        // of consuming an entire toolbar row for a single button.
        javafx.scene.layout.StackPane contentStack = new javafx.scene.layout.StackPane(mWidgetContainer);
        VBox.setVgrow(contentStack, Priority.ALWAYS);

        Button manageWidgets = getManageWidgetsButton();
        javafx.scene.layout.StackPane.setAlignment(manageWidgets, Pos.BOTTOM_RIGHT);
        javafx.scene.layout.StackPane.setMargin(manageWidgets, new javafx.geometry.Insets(0, 8, 8, 0));
        //Let clicks pass through the empty area of the overlay to the content beneath it.
        contentStack.setPickOnBounds(false);
        contentStack.getChildren().add(manageWidgets);

        getChildren().add(contentStack);
    }

    private void setupWidgets() {
        mWidgetContainer.removeAll();

        if (mSpectralPanel != null) {
            Widget spectrumWidget = new Widget("spectrum_v2", "Spectrum/Waterfall", (Region) mSpectralPanel, mWidgetContainer, 100);
            int prefHeight = mNowPlayingPreference.getWidgetHeight("spectrum_v2", 120);
            ((Region) mSpectralPanel).setPrefHeight(prefHeight);
            VBox.setVgrow(spectrumWidget, Priority.NEVER);
            mWidgetContainer.addWidget(spectrumWidget, false);

            mSpectrumWidget = spectrumWidget;
            //Pause the DFT (which feeds BOTH the spectrum and the waterfall) whenever the widget is minimized or
            //hidden so we don't burn CPU computing graphs the user can't see.  Listeners are attached after
            //addWidget() so the initial state restore doesn't fire them prematurely.
            spectrumWidget.visibleProperty().addListener((o, was, now) -> updateSpectrumProcessing());
            spectrumWidget.minimizedProperty().addListener((o, was, now) -> updateSpectrumProcessing());
            updateSpectrumProcessing();
        }

        // Combined Channel Table + Channel Details widget using a horizontal SplitPane
        if (mChannelMetadataPanel != null && getTabbedPane() != null) {
            mChannelSplitPane = new SplitPane();
            mChannelSplitPane.setOrientation(javafx.geometry.Orientation.HORIZONTAL);
            mChannelSplitPane.getItems().addAll(mChannelMetadataPanel);
            // DO NOT add getTabbedPane() initially; it will be added when a channel is selected or toggled.
            mChannelSplitPane.setDividerPositions(1.0);
            mChannelSplitPane.setMinHeight(0);

            Widget channelWidget = new Widget("channel", "Channel Table & Details", mChannelSplitPane, mWidgetContainer, 100);
            VBox.setVgrow(channelWidget, Priority.ALWAYS);
            mWidgetContainer.addWidget(channelWidget, false);
        }

        if (mBroadcastStatusPanel != null) {
            Widget streamingWidget = new Widget("streaming", "Streaming Status", (Region) mBroadcastStatusPanel, mWidgetContainer, 40);
            int prefHeight = mNowPlayingPreference.getWidgetHeight("streaming", 130);
            ((Region) mBroadcastStatusPanel).setPrefHeight(prefHeight);
            VBox.setVgrow(streamingWidget, Priority.NEVER);
            mWidgetContainer.addWidget(streamingWidget, false);
        }

        if (mResourceStatusPanel != null) {
            Widget resourceWidget = new Widget("resource", "Resource Status", (Region) mResourceStatusPanel, mWidgetContainer, 30);
            resourceWidget.setMinimizeButtonVisible(false);
            resourceWidget.setHeaderVisible(false); // Hide the title bar to save vertical space
            VBox.setVgrow(resourceWidget, Priority.SOMETIMES);
            mWidgetContainer.addWidget(resourceWidget, true);
        }

        mWidgetContainer.layoutWidgets("resource");
    }

    private void showManageWidgetsPopup() {
        javafx.scene.control.ContextMenu popup = new javafx.scene.control.ContextMenu();

        if (mSpectralPanel != null) addPopupItem(popup, "Spectrum/Waterfall", "spectrum_v2");
        if (mChannelSplitPane != null) addPopupItem(popup, "Channel Table & Details", "channel");
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
