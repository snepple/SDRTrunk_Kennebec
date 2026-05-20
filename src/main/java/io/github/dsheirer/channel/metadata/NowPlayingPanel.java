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
import javafx.application.Platform;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import io.github.dsheirer.gui.VisibilityListener;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.module.ProcessingChain;

import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.JCheckBoxMenuItem;
import java.awt.Cursor;
import java.awt.Color;
import java.awt.Insets;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;

public class NowPlayingPanel extends VBox implements Listener<ProcessingChain>
{
    private final ChannelMetadataPanel mChannelMetadataPanel;
    private final ChannelDetailPanel mChannelDetailPanel;
    private final DecodeEventPanel mDecodeEventPanel;
    private final MessageActivityPanel mMessageActivityPanel;
    private final ChannelSpectrumPanel mChannelSpectrumSquelchPanel;
    private TabPane mTabbedPane;
    private Object mBroadcastStatusPanel;
    private Object mResourceStatusPanel;
    private Object mSpectralPanel;

    private VisibilityListener mVisibilityListener;
    private WidgetContainer mWidgetContainer;
    private ScrollPane mScrollPane;
    private NowPlayingPreference mNowPlayingPreference;
    private JButton mManageWidgetsBtn;

    public NowPlayingPanel(PlaylistManager playlistManager, IconModel iconModel, UserPreferences userPreferences,
                           SettingsManager settingsManager, TunerManager tunerManager, boolean detailTabsVisible, VisibilityListener visibilityListener)
    {
        mVisibilityListener = visibilityListener;
        mChannelDetailPanel = new ChannelDetailPanel(playlistManager.getChannelProcessingManager());
        mDecodeEventPanel = new DecodeEventPanel(iconModel, userPreferences, playlistManager.getAliasModel(), playlistManager.getChannelProcessingManager());
        mMessageActivityPanel = new MessageActivityPanel(userPreferences);
        mChannelMetadataPanel = new ChannelMetadataPanel(playlistManager, iconModel, userPreferences, tunerManager);
        mChannelSpectrumSquelchPanel = new ChannelSpectrumPanel(playlistManager, settingsManager);
        mNowPlayingPreference = new NowPlayingPreference();

        init();
    }

    @Override
    public void receive(ProcessingChain processingChain) {
        Platform.runLater(() -> {
            TabPane pane = getTabbedPane();

            if (processingChain == null) {
                pane.getTabs().removeIf(tab -> {
                    String text = tab.getText();
                    return "Details".equals(text) || "Messages".equals(text) || "Channel".equals(text);
                });
                mChannelSpectrumSquelchPanel.setPanelVisible(false);
            } else {
                pane.getTabs().clear();
                pane.getTabs().add(createTab("Details", mChannelDetailPanel));
                pane.getTabs().add(createTab("Events", mDecodeEventPanel));
                pane.getTabs().add(createTab("Messages", mMessageActivityPanel));
                pane.getTabs().add(createTab("Channel", mChannelSpectrumSquelchPanel));

                for(Tab tab : pane.getTabs()) {
                    tab.setClosable(false);
                }

                Tab selectedTab = pane.getSelectionModel().getSelectedItem();
                mChannelSpectrumSquelchPanel.setPanelVisible(selectedTab != null && "Channel".equals(selectedTab.getText()));
            }
        });
    }

    public void dispose()
    {
    }

    public void setComponents(Object spectralPanel, Object broadcastStatusPanel, Object resourceStatusPanel) {
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
            mWidgetContainer.setWidgetVisible("details", visible);
        }
    }

    public void setBroadcastStatusPanel(Object panel) {
        mBroadcastStatusPanel = panel;
    }

    private Tab createTab(String title, Object content) {
        Tab tab = new Tab(title);
        if (content instanceof javafx.scene.Node) {
            tab.setContent((javafx.scene.Node) content);
        } else if (content instanceof javax.swing.JComponent) {
            javafx.embed.swing.SwingNode swingNode = new javafx.embed.swing.SwingNode();
            javax.swing.SwingUtilities.invokeLater(() -> swingNode.setContent((javax.swing.JComponent) content));
            tab.setContent(swingNode);
        }
        return tab;
    }

    private TabPane getTabbedPane()
    {
        if(mTabbedPane == null)
        {
            mTabbedPane = new TabPane();

            Tab detailsTab = createTab("Details", mChannelDetailPanel);
            Tab eventsTab = createTab("Events", mDecodeEventPanel);
            Tab messagesTab = createTab("Messages", mMessageActivityPanel);
            Tab channelTab = createTab("Channel", mChannelSpectrumSquelchPanel);

            detailsTab.setClosable(false);
            eventsTab.setClosable(false);
            messagesTab.setClosable(false);
            channelTab.setClosable(false);

            mTabbedPane.getTabs().addAll(detailsTab, eventsTab, messagesTab, channelTab);

            mTabbedPane.getSelectionModel().selectedItemProperty().addListener((observable, oldTab, newTab) -> {
                mChannelSpectrumSquelchPanel.setPanelVisible(newTab != null && "Channel".equals(newTab.getText()));
            });
        }

        return mTabbedPane;
    }

    public JButton getManageWidgetsButton() {
        if (mManageWidgetsBtn == null) {
            mManageWidgetsBtn = new JButton(IconFontSwing.buildIcon(FontAwesome.COG, 14, Color.BLACK));
            mManageWidgetsBtn.setToolTipText("Manage Widgets");
            mManageWidgetsBtn.getAccessibleContext().setAccessibleName("Manage Widgets");
            mManageWidgetsBtn.getAccessibleContext().setAccessibleDescription("Opens a menu to manage visible widgets");
            mManageWidgetsBtn.addActionListener(e -> showManageWidgetsPopup(mManageWidgetsBtn));
            mManageWidgetsBtn.setFocusPainted(false);
            mManageWidgetsBtn.setContentAreaFilled(false);
            mManageWidgetsBtn.setBorderPainted(false);
            mManageWidgetsBtn.setMargin(new Insets(0, 4, 0, 4));
            mManageWidgetsBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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

        Platform.runLater(() -> {
            ScrollPane scrollPane = new ScrollPane(mWidgetContainer);
            scrollPane.setFitToWidth(true);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
            VBox.setVgrow(scrollPane, Priority.ALWAYS);
            getChildren().add(scrollPane);
        });
    }

    private void setupWidgets() {
        Platform.runLater(() -> {
            mWidgetContainer.removeAll();

            if (mSpectralPanel != null) {
                Widget spectrumWidget = new Widget("spectrum", "Spectrum/Waterfall", mSpectralPanel, mWidgetContainer, 150);
                mWidgetContainer.addWidget(spectrumWidget, false);
            }

            if (mChannelMetadataPanel != null) {
                Widget channelTableWidget = new Widget("channel_table", "Channel Table", mChannelMetadataPanel, mWidgetContainer, 150);
                mWidgetContainer.addWidget(channelTableWidget, false);
            }

            if (getTabbedPane() != null) {
                Widget detailsWidget = new Widget("details", "Channel Details", getTabbedPane(), mWidgetContainer, 200);
                mWidgetContainer.addWidget(detailsWidget, false);
            }

            if (mBroadcastStatusPanel != null) {
                Widget streamingWidget = new Widget("streaming", "Streaming Status", mBroadcastStatusPanel, mWidgetContainer, 70);
                mWidgetContainer.addWidget(streamingWidget, false);
            }

            if (mResourceStatusPanel != null) {
                Widget resourceWidget = new Widget("resource", "Resource Status", mResourceStatusPanel, mWidgetContainer, 30);
                resourceWidget.setMinimizeButtonVisible(false);
                mWidgetContainer.addWidget(resourceWidget, true); // Pinned to bottom
            }

            mWidgetContainer.layoutWidgets("resource");
        });
    }

    private void showManageWidgetsPopup(JButton source) {
        JPopupMenu popup = new JPopupMenu();

        if (mSpectralPanel != null) addPopupItem(popup, "Spectrum/Waterfall", "spectrum");
        if (mChannelMetadataPanel != null) addPopupItem(popup, "Channel Table", "channel_table");
        if (getTabbedPane() != null) addPopupItem(popup, "Channel Details", "details");
        if (mBroadcastStatusPanel != null) addPopupItem(popup, "Streaming Status", "streaming");
        if (mResourceStatusPanel != null) addPopupItem(popup, "Resource Status", "resource");

        popup.show(source, 0, source.getHeight());
    }

    private void addPopupItem(JPopupMenu popup, String label, String widgetId) {
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(label);
        item.setSelected(mNowPlayingPreference.isWidgetVisible(widgetId, true));
        item.addActionListener(e -> mWidgetContainer.setWidgetVisible(widgetId, item.isSelected()));
        popup.add(item);
    }
}
