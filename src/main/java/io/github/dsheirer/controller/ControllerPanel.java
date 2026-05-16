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
package io.github.dsheirer.controller;

import io.github.dsheirer.gui.SidebarPanel;
import io.github.dsheirer.gui.VisibilityListener;



import com.jidesoft.swing.JideTabbedPane;
import io.github.dsheirer.audio.playback.AudioPanel;
import io.github.dsheirer.audio.playback.AudioPlaybackManager;
import io.github.dsheirer.channel.metadata.NowPlayingPanel;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.playlist.ViewPlaylistRequest;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.map.MapPanel;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.application.Platform;
import io.github.dsheirer.map.MapService;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.settings.SettingsManager;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.ui.TunerViewPanel;
import io.github.dsheirer.gui.recordings.AudioRecordingsPanel;
import java.awt.Color;
import java.awt.Dimension;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.embed.swing.SwingNode;
import javafx.scene.Node;
import io.github.dsheirer.gui.help.HelpViewer;
import javax.swing.JSplitPane;
import javax.swing.JPanel;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.embed.swing.SwingNode;
import javafx.scene.Node;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.DefaultListModel;
import java.awt.CardLayout;
import java.awt.BorderLayout;


public class ControllerPanel extends javafx.scene.layout.BorderPane
{
    private final static Logger mLog = LoggerFactory.getLogger(ControllerPanel.class);
    private static final long serialVersionUID = 1L;
    private int mSettingsTabIndex = -1;

    private AudioPanel mAudioPanel;
    private NowPlayingPanel mNowPlayingPanel;
    private MapPanel mMapPanel;
    private TunerViewPanel mTunerManagerPanel;
    private AudioRecordingsPanel mAudioRecordingsPanel;


    private StackPane mCardPanel;

    private JList<String> mSidebarList;
    private Node mResourcePanel;

    public ControllerPanel(PlaylistManager playlistManager, AudioPlaybackManager audioPlaybackManager,
                           IconModel iconModel, MapService mapService, SettingsManager settingsManager,
                           TunerManager tunerManager, UserPreferences userPreferences, boolean detailTabsVisible, VisibilityListener visibilityListener)
    {
        mAudioPanel = new AudioPanel(iconModel, userPreferences, settingsManager, audioPlaybackManager,
            playlistManager.getAliasModel(), playlistManager.getBroadcastModel());
        mNowPlayingPanel = new NowPlayingPanel(playlistManager, iconModel, userPreferences, settingsManager, tunerManager, detailTabsVisible, visibilityListener);
        mMapPanel = new MapPanel(mapService, playlistManager.getAliasModel(), iconModel, settingsManager);
        mTunerManagerPanel = new TunerViewPanel(tunerManager, userPreferences, visibilityListener);
        mAudioRecordingsPanel = new AudioRecordingsPanel(userPreferences, playlistManager);

        mAudioPanel.setManageWidgetsButton(mNowPlayingPanel.getManageWidgetsButton());

        init();
    }

    /**
     * Now playing panel.
     */

    /**
     * Audio panel.
     */
    public AudioPanel getAudioPanel()
    {
        return mAudioPanel;
    }

    public NowPlayingPanel getNowPlayingPanel()
    {
        return mNowPlayingPanel;
    }


    private void init()
    {
        mCardPanel = new StackPane();

        // Wrap Swing components in SwingNode
        SwingNode nowPlayingNode = new SwingNode();
        nowPlayingNode.setContent(mNowPlayingPanel);
        mCardPanel.getChildren().add(nowPlayingNode);
        nowPlayingNode.setId("now_playing");
        nowPlayingNode.setVisible(false);

        mCardPanel.getChildren().add(mMapPanel);
        mMapPanel.setId("map");
        mMapPanel.setVisible(false);
        
        SwingNode tunerManagerNode = new SwingNode();
        tunerManagerNode.setContent(mTunerManagerPanel);
        mCardPanel.getChildren().add(tunerManagerNode);
        tunerManagerNode.setId("tuners");
        tunerManagerNode.setVisible(false);

        mCardPanel.getChildren().add(mAudioRecordingsPanel);
        mAudioRecordingsPanel.setId("audio_recordings");
        mAudioRecordingsPanel.setVisible(false);

        HelpViewer helpViewer = new HelpViewer();
        mCardPanel.getChildren().add(helpViewer);
        helpViewer.setId("help_viewer");
        helpViewer.setVisible(false);

        this.setCenter(mCardPanel);
        // AudioPanel moved to SDRTrunk.java
    }

    public void addView(String id, Node view) {
        mCardPanel.getChildren().add(view);
        view.setId(id);
        view.setVisible(false);
    }

    public void showView(String id) {
        mNowPlayingPanel.getManageWidgetsButton().setVisible("now_playing".equals(id));
        for (Node child : mCardPanel.getChildren()) {
            child.setVisible(id.equals(child.getId()));
        }
    }

    public void setResourcePanel(Node resourcePanel) {
        mResourcePanel = resourcePanel;
        this.setBottom(mResourcePanel);
        mResourcePanel.setVisible(false);
    }

    public void setResourcePanelVisible(boolean visible) {
        if (mResourcePanel != null) {
            mResourcePanel.setVisible(visible);
        }
    }

}
