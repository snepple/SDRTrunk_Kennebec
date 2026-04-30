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

package io.github.dsheirer.gui.playlist;

import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.icon.ViewIconManagerRequest;
import io.github.dsheirer.gui.playlist.alias.AliasEditor;
import io.github.dsheirer.gui.playlist.alias.AliasTabRequest;
import io.github.dsheirer.gui.playlist.channel.ChannelEditor;
import io.github.dsheirer.gui.playlist.channel.ChannelTabRequest;
import io.github.dsheirer.gui.playlist.manager.PlaylistManagerEditor;
import io.github.dsheirer.gui.playlist.radioreference.RadioReferenceEditor;
import io.github.dsheirer.gui.playlist.streaming.StreamTabRequest;
import io.github.dsheirer.gui.playlist.streaming.StreamingEditor;

import io.github.dsheirer.gui.playlist.twotone.TwoToneEditor;
import io.github.dsheirer.gui.playlist.twotone.TwoToneTabRequest;

import io.github.dsheirer.gui.preference.PreferenceEditorType;
import io.github.dsheirer.gui.preference.ViewUserPreferenceEditorRequest;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.util.ThreadPool;
import io.github.dsheirer.util.TimeStamp;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;


import javafx.scene.control.SplitPane;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/**
 * JavaFX playlist, channels, aliases, streaming and radioreference.com import editor
 */
public class PlaylistEditor extends BorderPane
{
    private static final Logger mLog = LoggerFactory.getLogger(PlaylistEditor.class);

    private PlaylistManager mPlaylistManager;
    private TunerManager mTunerManager;
    private UserPreferences mUserPreferences;
    private MenuBar mMenuBar;
    private SplitPane mSplitPane;
    private ListView<String> mSidebar;
    private ScrollPane mPlaylistsScrollPane;
    private ScrollPane mChannelsScrollPane;
    private ScrollPane mAliasesScrollPane;
    private ScrollPane mRadioReferenceScrollPane;
    private ScrollPane mStreamingScrollPane;
    private ScrollPane mTwoToneScrollPane;
    private TwoToneEditor mTwoToneEditor;

    private AliasEditor mAliasEditor;
    private ChannelEditor mChannelEditor;
    private StreamingEditor mStreamingEditor;

    /**
     * Constructs an instance
     * @param playlistManager for alias and channel models
     * @param tunerManager for tuners
     * @param userPreferences for settings
     */
    public PlaylistEditor(PlaylistManager playlistManager, TunerManager tunerManager, UserPreferences userPreferences)
    {
        mPlaylistManager = playlistManager;
        mTunerManager = tunerManager;
        mUserPreferences = userPreferences;

        //Throw a new runnable back onto the FX thread to lazy load the editor content after the editor has been
        //constructed and shown.
        Platform.runLater(() -> {
            setCenter(getSplitPane());
        });
    }

    /**
     * Process requests for sub-editor actions like view an alias or view a channel.
     *
     * Note: this method must be invoked on the JavaFX platform thread
     * @param request to process
     */
    public void process(PlaylistEditorRequest request)
    {
        // Ensure UI initialization
        getSplitPane();
        switch(request.getTabName())
        {
            case ALIAS:
                if(request instanceof AliasTabRequest)
                {
                    mSidebar.getSelectionModel().select("Aliases");
                    getAliasEditor().process((AliasTabRequest)request);
                }
                break;

            case TWO_TONE:
                if(request instanceof TwoToneTabRequest)
                {
                    mSidebar.getSelectionModel().select("Two Tones");
                    getTwoToneEditor().process((TwoToneTabRequest)request);
                }
                break;

            case CHANNEL:
                if(request instanceof ChannelTabRequest)
                {
                    mSidebar.getSelectionModel().select("Channels");
                    getChannelEditor().process((ChannelTabRequest)request);
                }
                break;
            case STREAM:
                if(request instanceof StreamTabRequest)
                {
                    mSidebar.getSelectionModel().select("Streaming");
                    getStreamingEditor().process((StreamTabRequest)request);
                }
                break;
            case PLAYLIST:
                mSidebar.getSelectionModel().select("Playlists");
                break;
            default:
                mLog.warn("Unrecognized playlist editor request: " + request.getClass());
                break;
        }
    }

    private MenuBar getMenuBar()
    {
        if(mMenuBar == null)
        {
            mMenuBar = new MenuBar();

            //File Menu
            Menu fileMenu = new Menu("_File");
            fileMenu.setAccelerator(new KeyCodeCombination(KeyCode.F, KeyCombination.ALT_ANY));

            MenuItem closeItem = new MenuItem("_Close");
            closeItem.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.ALT_ANY));
            closeItem.setOnAction(event -> getMenuBar().getParent().getScene().getWindow().hide());
            fileMenu.getItems().add(closeItem);
            mMenuBar.getMenus().add(fileMenu);

            Menu viewMenu = new Menu("_View");
            viewMenu.setAccelerator(new KeyCodeCombination(KeyCode.V, KeyCombination.ALT_ANY));

            MenuItem iconManagerItem = new MenuItem("_Icon Manager");
            iconManagerItem.setAccelerator(new KeyCodeCombination(KeyCode.I, KeyCombination.ALT_ANY));
            iconManagerItem.setOnAction(event -> MyEventBus.getGlobalEventBus().post(new ViewIconManagerRequest()));
            viewMenu.getItems().add(iconManagerItem);

            MenuItem userPreferenceItem = new MenuItem("_User Preferences");
            userPreferenceItem.setAccelerator(new KeyCodeCombination(KeyCode.U, KeyCombination.ALT_ANY));
            userPreferenceItem.setOnAction(event -> MyEventBus.getGlobalEventBus()
                .post(new ViewUserPreferenceEditorRequest(PreferenceEditorType.TALKGROUP_FORMAT)));
            viewMenu.getItems().add(userPreferenceItem);

            mMenuBar.getMenus().add(viewMenu);

            Menu screenShot = new Menu("_Screenshot");
            IconNode cameraNode = new IconNode(FontAwesome.CAMERA);
            cameraNode.setFill(Color.DARKGRAY);
            screenShot.setGraphic(cameraNode);
            MenuItem menuItem = new MenuItem();
            screenShot.getItems().add(menuItem);
            screenShot.setOnShowing(event -> screenShot.hide());
            screenShot.setOnShown(event -> menuItem.fire());
            menuItem.setOnAction(event -> {
                WritableImage image = getMenuBar().getScene().snapshot(null);
                final BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
                String filename = TimeStamp.getTimeStamp("_") + "_screen_capture.png";
                final Path captureFile = mUserPreferences.getDirectoryPreference().getDirectoryScreenCapture().resolve(filename);

                ThreadPool.CACHED.submit(() -> {
                    try
                    {
                        ImageIO.write(bufferedImage, "png", captureFile.toFile());
                    }
                    catch(IOException e)
                    {
                        mLog.error("Couldn't write screen capture to file [" + captureFile.toString() + "]", e);
                    }
                });
            });
            mMenuBar.getMenus().add(screenShot);

        }

        return mMenuBar;
    }

    private SplitPane getSplitPane()
    {
        if(mSplitPane == null)
        {
            mSplitPane = new SplitPane();
            mSplitPane.setDividerPositions(0.2);

            VBox sidebarContainer = new VBox();
            sidebarContainer.setStyle("-fx-background-color: #F2F2F7;");

            Label header = new Label("Library");
            header.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #8E8E93; -fx-padding: 16 8 8 16;");

            mSidebar = new ListView<>();
            mSidebar.getItems().addAll("Playlists", "Channels", "Aliases", "Streaming", "Radio Reference", "Two Tones");
            mSidebar.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-control-inner-background: transparent;");

            VBox.setVgrow(mSidebar, Priority.ALWAYS);
            sidebarContainer.getChildren().addAll(header, mSidebar);

            mSplitPane.getItems().addAll(sidebarContainer, getPlaylistsScrollPane());

            mSidebar.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    showEditorForSidebarItem(newVal);
                }
            });
            mSidebar.getSelectionModel().select("Playlists");
        }

        return mSplitPane;
    }

    private void showEditorForSidebarItem(String item) {
        if (mSplitPane.getItems().size() > 1) {
            mSplitPane.getItems().remove(1);
        }
        switch (item) {
            case "Playlists":
                mSplitPane.getItems().add(getPlaylistsScrollPane());
                break;
            case "Channels":
                mSplitPane.getItems().add(getChannelsScrollPane());
                break;
            case "Aliases":
                mSplitPane.getItems().add(getAliasesScrollPane());
                break;
            case "Streaming":
                mSplitPane.getItems().add(getStreamingScrollPane());
                break;
            case "Radio Reference":
                mSplitPane.getItems().add(getRadioReferenceScrollPane());
                break;
            case "Two Tones":
                mSplitPane.getItems().add(getTwoToneScrollPane());
                break;
        }
    }

    private ScrollPane getAliasesScrollPane()
    {
        if(mAliasesScrollPane == null)
        {
            mAliasesScrollPane = new ScrollPane(getAliasEditor());
            mAliasesScrollPane.setFitToWidth(true);
            mAliasesScrollPane.setFitToHeight(true);
        }

        return mAliasesScrollPane;
    }

    private AliasEditor getAliasEditor()
    {
        if(mAliasEditor == null)
        {
            mAliasEditor = new AliasEditor(mPlaylistManager, mUserPreferences);
        }

        return mAliasEditor;
    }

    private ScrollPane getChannelsScrollPane()
    {
        if(mChannelsScrollPane == null)
        {
            mChannelsScrollPane = new ScrollPane(getChannelEditor());
            mChannelsScrollPane.setFitToWidth(true);
            mChannelsScrollPane.setFitToHeight(true);
        }

        return mChannelsScrollPane;
    }

    private ChannelEditor getChannelEditor()
    {
        if(mChannelEditor == null)
        {
            mChannelEditor = new ChannelEditor(mPlaylistManager, mTunerManager, mUserPreferences);
        }

        return mChannelEditor;
    }

    private ScrollPane getTwoToneScrollPane()
    {
        if(mTwoToneScrollPane == null)
        {
            mTwoToneScrollPane = new ScrollPane(getTwoToneEditor());
            mTwoToneScrollPane.setFitToWidth(true);
            mTwoToneScrollPane.setFitToHeight(true);
        }

        return mTwoToneScrollPane;
    }

    private TwoToneEditor getTwoToneEditor()
    {
        if(mTwoToneEditor == null)
        {
            mTwoToneEditor = new TwoToneEditor(mPlaylistManager);
        }

        return mTwoToneEditor;
    }

    private ScrollPane getPlaylistsScrollPane()
    {
        if(mPlaylistsScrollPane == null)
        {
            mPlaylistsScrollPane = new ScrollPane(new PlaylistManagerEditor(mPlaylistManager, mUserPreferences));
            mPlaylistsScrollPane.setFitToWidth(true);
            mPlaylistsScrollPane.setFitToHeight(true);
        }

        return mPlaylistsScrollPane;
    }

    private ScrollPane getRadioReferenceScrollPane()
    {
        if(mRadioReferenceScrollPane == null)
        {
            mRadioReferenceScrollPane = new ScrollPane(new RadioReferenceEditor(mUserPreferences, mPlaylistManager));
            mRadioReferenceScrollPane.setFitToWidth(true);
            mRadioReferenceScrollPane.setFitToHeight(true);
        }

        return mRadioReferenceScrollPane;
    }

    private ScrollPane getStreamingScrollPane()
    {
        if(mStreamingScrollPane == null)
        {
            mStreamingScrollPane = new ScrollPane(getStreamingEditor());
            mStreamingScrollPane.setFitToWidth(true);
            mStreamingScrollPane.setFitToHeight(true);
        }

        return mStreamingScrollPane;
    }

    private StreamingEditor getStreamingEditor()
    {
        if(mStreamingEditor == null)
        {
            mStreamingEditor = new StreamingEditor(mPlaylistManager);
        }

        return mStreamingEditor;
    }
}
