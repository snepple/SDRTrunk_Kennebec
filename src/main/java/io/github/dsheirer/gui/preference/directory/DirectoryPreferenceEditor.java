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

package io.github.dsheirer.gui.preference.directory;

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.directory.DirectoryPreference;
import java.io.File;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import io.github.dsheirer.gui.preference.layout.SettingsCard;
import io.github.dsheirer.gui.preference.layout.SettingsRow;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Preference settings for channel event view
 */
public class DirectoryPreferenceEditor extends VBox
{
    private final static Logger mLog = LoggerFactory.getLogger(DirectoryPreferenceEditor.class);

    private DirectoryPreference mDirectoryPreference;
    private VBox mEditorPane;

    private Label mApplicationRootLabel;
    private Button mChangeApplicationRootButton;
    private Button mResetApplicationRootButton;
    private Label mApplicationRootPathLabel;

    private Label mApplicationLogsLabel;
    private Button mChangeApplicationLogsButton;
    private Button mResetApplicationLogsButton;
    private Label mApplicationLogsPathLabel;

    private Label mConfigurationLabel;
    private Button mChangeConfigurationButton;
    private Button mResetConfigurationButton;
    private Label mConfigurationPathLabel;

    private Label mEventLogsLabel;
    private Button mChangeEventLogsButton;
    private Button mResetEventLogsButton;
    private Label mEventLogsPathLabel;

    private Label mJmbeLabel;
    private Button mChangeJmbeButton;
    private Button mResetJmbeButton;
    private Label mJmbePathLabel;

    private Label mPlaylistLabel;
    private Button mChangePlaylistButton;
    private Button mResetPlaylistButton;
    private Label mPlaylistPathLabel;

    private Label mRecordingLabel;
    private Button mChangeRecordingButton;
    private Button mResetRecordingButton;
    private Label mRecordingPathLabel;

    private Label mScreenCaptureLabel;
    private Button mChangeScreenCaptureButton;
    private Button mResetScreenCaptureButton;
    private Label mScreenCapturePathLabel;

    private Label mStreamingLabel;
    private Button mChangeStreamingButton;
    private Button mResetStreamingButton;
    private Label mStreamingPathLabel;

    private Spinner<Integer> mRecordingSpinner;
    private Spinner<Integer> mEventLogSpinner;

    public DirectoryPreferenceEditor(UserPreferences userPreferences)
    {
        mDirectoryPreference = userPreferences.getDirectoryPreference();

        //Register to receive directory preference update notifications so we can update the path labels
        MyEventBus.getGlobalEventBus().register(this);

        setPadding(new Insets(10, 10, 10, 10));
        setSpacing(20);

        Label headerLabel = new Label("Directory Preferences");
        headerLabel.getStyleClass().add("hig-section-header");
        getChildren().add(headerLabel);

        VBox.setVgrow(getEditorPane(), Priority.ALWAYS);
        getChildren().add(getEditorPane());
    }

    public void dispose()
    {
        MyEventBus.getGlobalEventBus().unregister(this);
    }

    private VBox getEditorPane()
    {
        if(mEditorPane == null)
        {
            mEditorPane = new VBox();

            SettingsCard mainCard = new SettingsCard();

            // Application Root
            HBox rootControls = new HBox(10);
            rootControls.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            getApplicationRootPathLabel().getStyleClass().add("kennebec-secondary-text");
            rootControls.getChildren().addAll(getApplicationRootPathLabel(), getChangeApplicationRootButton(), getResetApplicationRootButton());
            mainCard.getChildren().add(new SettingsRow("Application Root", rootControls));

            // Application Logs
            HBox appLogsControls = new HBox(10);
            appLogsControls.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            getApplicationLogsPathLabel().getStyleClass().add("kennebec-secondary-text");
            appLogsControls.getChildren().addAll(getApplicationLogsPathLabel(), getChangeApplicationLogsButton(), getResetApplicationLogsButton());
            mainCard.getChildren().add(new SettingsRow("Application Logs", appLogsControls));

            // Configuration
            HBox configControls = new HBox(10);
            configControls.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            getConfigurationPathLabel().getStyleClass().add("kennebec-secondary-text");
            configControls.getChildren().addAll(getConfigurationPathLabel(), getChangeConfigurationButton(), getResetConfigurationButton());
            mainCard.getChildren().add(new SettingsRow("Configuration", configControls));

            // Event Logs
            HBox eventLogsControls = new HBox(10);
            eventLogsControls.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            getEventLogsPathLabel().getStyleClass().add("kennebec-secondary-text");
            eventLogsControls.getChildren().addAll(getEventLogsPathLabel(), getChangeEventLogsButton(), getResetEventLogsButton());
            mainCard.getChildren().add(new SettingsRow("Event Logs", eventLogsControls));

            // JMBE Audio Library
            HBox jmbeControls = new HBox(10);
            jmbeControls.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            getJmbePathLabel().getStyleClass().add("kennebec-secondary-text");
            jmbeControls.getChildren().addAll(getJmbePathLabel(), getChangeJmbeButton(), getResetJmbeButton());
            mainCard.getChildren().add(new SettingsRow("JMBE Audio Library", jmbeControls));

            // Playlists
            HBox playlistControls = new HBox(10);
            playlistControls.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            getPlaylistPathLabel().getStyleClass().add("kennebec-secondary-text");
            playlistControls.getChildren().addAll(getPlaylistPathLabel(), getChangePlaylistButton(), getResetPlaylistButton());
            mainCard.getChildren().add(new SettingsRow("Playlists", playlistControls));

            // Recordings
            HBox recordingControls = new HBox(10);
            recordingControls.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            getRecordingPathLabel().getStyleClass().add("kennebec-secondary-text");
            recordingControls.getChildren().addAll(getRecordingPathLabel(), getChangeRecordingButton(), getResetRecordingButton());
            mainCard.getChildren().add(new SettingsRow("Recordings", recordingControls));

            // Screen Captures
            HBox captureControls = new HBox(10);
            captureControls.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            getScreenCapturePathLabel().getStyleClass().add("kennebec-secondary-text");
            captureControls.getChildren().addAll(getScreenCapturePathLabel(), getChangeScreenCaptureButton(), getResetScreenCaptureButton());
            mainCard.getChildren().add(new SettingsRow("Screen Captures", captureControls));

            // Streaming
            HBox streamingControls = new HBox(10);
            streamingControls.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            getStreamingPathLabel().getStyleClass().add("kennebec-secondary-text");
            streamingControls.getChildren().addAll(getStreamingPathLabel(), getChangeStreamingButton(), getResetStreamingButton());
            mainCard.getChildren().add(new SettingsRow("Streaming", streamingControls));

            mEditorPane.getChildren().add(mainCard);

            Label monitorLabel = new Label("File storage usage monitoring - maximum size thresholds (MB)");
            monitorLabel.getStyleClass().add("hig-section-header");
            mEditorPane.getChildren().add(monitorLabel);

            SettingsCard monitorCard = new SettingsCard();
            monitorCard.getChildren().add(new SettingsRow("Event Logs", getEventLogSpinner()));
            monitorCard.getChildren().add(new SettingsRow("Recordings", getRecordingSpinner()));
            mEditorPane.getChildren().add(monitorCard);
        }

        return mEditorPane;
    }

    /**
     * Recording directory maximum size threshold spinner
     * @return spinner
     */
    private Spinner<Integer> getRecordingSpinner()
    {
        if(mRecordingSpinner == null)
        {
            mRecordingSpinner = new Spinner<>(100, Integer.MAX_VALUE, mDirectoryPreference.getDirectoryMaxUsageRecordings(), 100);
            mRecordingSpinner.setTooltip(new Tooltip("Maximum size limit in megabytes (MB) for the recordings directory. Older recordings will be automatically deleted when this limit is reached."));
            mRecordingSpinner.valueProperty().addListener((observable, oldValue, newValue) -> mDirectoryPreference
                    .setDirectoryMaxUsageRecordings(newValue));
        }

        return mRecordingSpinner;
    }

    /**
     * Event log directory maximum size threshold spinner
     * @return spinner
     */
    private Spinner<Integer> getEventLogSpinner()
    {
        if(mEventLogSpinner == null)
        {
            mEventLogSpinner = new Spinner<>(100, Integer.MAX_VALUE, mDirectoryPreference.getDirectoryMaxUsageEventLogs(), 100);
            mEventLogSpinner.setTooltip(new Tooltip("Maximum size limit in megabytes (MB) for the event logs directory. Older logs will be automatically deleted when this limit is reached."));
            mEventLogSpinner.setEditable(true);
            mEventLogSpinner.valueProperty().addListener((observable, oldValue, newValue) -> mDirectoryPreference
                    .setDirectoryMaxUsageEventLogs(newValue));
        }

        return mEventLogSpinner;
    }

    private Label getApplicationRootLabel()
    {
        if(mApplicationRootLabel == null)
        {
            mApplicationRootLabel = new Label("Application Root");
        }

        return mApplicationRootLabel;
    }

    private Button getChangeApplicationRootButton()
    {
        if(mChangeApplicationRootButton == null)
        {
            mChangeApplicationRootButton = new Button("Change...");
            mChangeApplicationRootButton.setTooltip(new Tooltip("Select a different folder location."));
            mChangeApplicationRootButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    DirectoryChooser directoryChooser = new DirectoryChooser();
                    directoryChooser.setTitle("Select Application Root Folder");
                    directoryChooser.setInitialDirectory(mDirectoryPreference.getDirectoryApplicationRoot().toFile());
                    Stage stage = (Stage)getChangeApplicationRootButton().getScene().getWindow();
                    File selected = directoryChooser.showDialog(stage);

                    if(selected != null)
                    {
                        mDirectoryPreference.setDirectoryApplicationRoot(selected.toPath());
                    }
                }
            });
        }

        return mChangeApplicationRootButton;
    }

    private Button getResetApplicationRootButton()
    {
        if(mResetApplicationRootButton == null)
        {
            mResetApplicationRootButton = new Button("Reset");
            mResetApplicationRootButton.setTooltip(new Tooltip("Restore the default folder location."));
            mResetApplicationRootButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    mDirectoryPreference.resetDirectoryApplicationRoot();
                }
            });
        }

        return mResetApplicationRootButton;
    }

    private Label getApplicationRootPathLabel()
    {
        if(mApplicationRootPathLabel == null)
        {
            mApplicationRootPathLabel = new Label(mDirectoryPreference.getDirectoryApplicationRoot().toString());
        }

        return mApplicationRootPathLabel;
    }

    private Label getApplicationLogsLabel()
    {
        if(mApplicationLogsLabel == null)
        {
            mApplicationLogsLabel = new Label("Application Logs");
        }

        return mApplicationLogsLabel;
    }

    private Button getChangeApplicationLogsButton()
    {
        if(mChangeApplicationLogsButton == null)
        {
            mChangeApplicationLogsButton = new Button("Change...");
            mChangeApplicationLogsButton.setTooltip(new Tooltip("Select a different folder location."));
            mChangeApplicationLogsButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    DirectoryChooser directoryChooser = new DirectoryChooser();
                    directoryChooser.setTitle("Select Application Logs Folder");
                    directoryChooser.setInitialDirectory(mDirectoryPreference.getDirectoryApplicationLog().toFile());
                    Stage stage = (Stage)getChangeApplicationLogsButton().getScene().getWindow();
                    File selected = directoryChooser.showDialog(stage);

                    if(selected != null)
                    {
                        mDirectoryPreference.setDirectoryApplicationLogs(selected.toPath());
                    }
                }
            });
        }

        return mChangeApplicationLogsButton;
    }

    private Button getResetApplicationLogsButton()
    {
        if(mResetApplicationLogsButton == null)
        {
            mResetApplicationLogsButton = new Button("Reset");
            mResetApplicationLogsButton.setTooltip(new Tooltip("Restore the default folder location."));
            mResetApplicationLogsButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    mDirectoryPreference.resetDirectoryApplicationLogs();
                }
            });
        }

        return mResetApplicationLogsButton;
    }

    private Label getApplicationLogsPathLabel()
    {
        if(mApplicationLogsPathLabel == null)
        {
            mApplicationLogsPathLabel = new Label(mDirectoryPreference.getDirectoryApplicationLog().toString());
        }

        return mApplicationLogsPathLabel;
    }

    private Label getConfigurationLabel()
    {
        if(mConfigurationLabel == null)
        {
            mConfigurationLabel = new Label("Configuration");
        }

        return mConfigurationLabel;
    }

    private Button getChangeConfigurationButton()
    {
        if(mChangeConfigurationButton == null)
        {
            mChangeConfigurationButton = new Button("Change...");
            mChangeConfigurationButton.setTooltip(new Tooltip("Select a different folder location."));
            mChangeConfigurationButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    DirectoryChooser directoryChooser = new DirectoryChooser();
                    directoryChooser.setTitle("Select Configuration Folder");
                    directoryChooser.setInitialDirectory(mDirectoryPreference.getDirectoryConfiguration().toFile());
                    Stage stage = (Stage)getChangeConfigurationButton().getScene().getWindow();
                    File selected = directoryChooser.showDialog(stage);

                    if(selected != null)
                    {
                        mDirectoryPreference.setDirectoryConfiguration(selected.toPath());
                    }
                }
            });
        }

        return mChangeConfigurationButton;
    }

    private Button getResetConfigurationButton()
    {
        if(mResetConfigurationButton == null)
        {
            mResetConfigurationButton = new Button("Reset");
            mResetConfigurationButton.setTooltip(new Tooltip("Restore the default folder location."));
            mResetConfigurationButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    mDirectoryPreference.resetDirectoryConfiguration();
                }
            });
        }

        return mResetConfigurationButton;
    }

    private Label getConfigurationPathLabel()
    {
        if(mConfigurationPathLabel == null)
        {
            mConfigurationPathLabel = new Label(mDirectoryPreference.getDirectoryConfiguration().toString());
        }

        return mConfigurationPathLabel;
    }

    private Label getEventLogsLabel()
    {
        if(mEventLogsLabel == null)
        {
            mEventLogsLabel = new Label("Event Logs");
        }

        return mEventLogsLabel;
    }

    private Button getChangeEventLogsButton()
    {
        if(mChangeEventLogsButton == null)
        {
            mChangeEventLogsButton = new Button("Change...");
            mChangeEventLogsButton.setTooltip(new Tooltip("Select a different folder location."));
            mChangeEventLogsButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    DirectoryChooser directoryChooser = new DirectoryChooser();
                    directoryChooser.setTitle("Select Event Logs Folder");
                    directoryChooser.setInitialDirectory(mDirectoryPreference.getDirectoryEventLog().toFile());
                    Stage stage = (Stage)getChangeEventLogsButton().getScene().getWindow();
                    File selected = directoryChooser.showDialog(stage);

                    if(selected != null)
                    {
                        mDirectoryPreference.setDirectoryEventLogs(selected.toPath());
                    }
                }
            });
        }

        return mChangeEventLogsButton;
    }

    private Button getResetEventLogsButton()
    {
        if(mResetEventLogsButton == null)
        {
            mResetEventLogsButton = new Button("Reset");
            mResetEventLogsButton.setTooltip(new Tooltip("Restore the default folder location."));
            mResetEventLogsButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    mDirectoryPreference.resetDirectoryEventLogs();
                }
            });
        }

        return mResetEventLogsButton;
    }

    private Label getEventLogsPathLabel()
    {
        if(mEventLogsPathLabel == null)
        {
            mEventLogsPathLabel = new Label(mDirectoryPreference.getDirectoryEventLog().toString());
        }

        return mEventLogsPathLabel;
    }

    private Label getJmbeLabel()
    {
        if(mJmbeLabel == null)
        {
            mJmbeLabel = new Label("JMBE Libraries");
        }

        return mJmbeLabel;
    }

    private Button getChangeJmbeButton()
    {
        if(mChangeJmbeButton == null)
        {
            mChangeJmbeButton = new Button("Change...");
            mChangeJmbeButton.setTooltip(new Tooltip("Select a different folder location."));
            mChangeJmbeButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    DirectoryChooser directoryChooser = new DirectoryChooser();
                    directoryChooser.setTitle("Select JMBE Folder");
                    directoryChooser.setInitialDirectory(mDirectoryPreference.getDirectoryJmbe().toFile());
                    Stage stage = (Stage)getChangeJmbeButton().getScene().getWindow();
                    File selected = directoryChooser.showDialog(stage);

                    if(selected != null)
                    {
                        mDirectoryPreference.setDirectoryJmbe(selected.toPath());
                    }
                }
            });
        }

        return mChangeJmbeButton;
    }

    private Button getResetJmbeButton()
    {
        if(mResetJmbeButton == null)
        {
            mResetJmbeButton = new Button("Reset");
            mResetJmbeButton.setTooltip(new Tooltip("Restore the default folder location."));
            mResetJmbeButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    mDirectoryPreference.resetDirectoryJmbe();
                }
            });
        }

        return mResetJmbeButton;
    }

    private Label getJmbePathLabel()
    {
        if(mJmbePathLabel == null)
        {
            mJmbePathLabel = new Label(mDirectoryPreference.getDirectoryJmbe().toString());
        }

        return mJmbePathLabel;
    }

    private Label getPlaylistLabel()
    {
        if(mPlaylistLabel == null)
        {
            mPlaylistLabel = new Label("Playlists");
        }

        return mPlaylistLabel;
    }

    private Button getChangePlaylistButton()
    {
        if(mChangePlaylistButton == null)
        {
            mChangePlaylistButton = new Button("Change...");
            mChangePlaylistButton.setTooltip(new Tooltip("Select a different folder location."));
            mChangePlaylistButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    DirectoryChooser directoryChooser = new DirectoryChooser();
                    directoryChooser.setTitle("Select Playlist Folder");
                    directoryChooser.setInitialDirectory(mDirectoryPreference.getDirectoryPlaylist().toFile());
                    Stage stage = (Stage)getChangePlaylistButton().getScene().getWindow();
                    File selected = directoryChooser.showDialog(stage);

                    if(selected != null)
                    {
                        mDirectoryPreference.setDirectoryPlaylist(selected.toPath());
                    }
                }
            });
        }

        return mChangePlaylistButton;
    }

    private Button getResetPlaylistButton()
    {
        if(mResetPlaylistButton == null)
        {
            mResetPlaylistButton = new Button("Reset");
            mResetPlaylistButton.setTooltip(new Tooltip("Restore the default folder location."));
            mResetPlaylistButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    mDirectoryPreference.resetDirectoryPlaylist();
                }
            });
        }

        return mResetPlaylistButton;
    }

    private Label getPlaylistPathLabel()
    {
        if(mPlaylistPathLabel == null)
        {
            mPlaylistPathLabel = new Label(mDirectoryPreference.getDirectoryPlaylist().toString());
        }

        return mPlaylistPathLabel;
    }

    private Label getRecordingLabel()
    {
        if(mRecordingLabel == null)
        {
            mRecordingLabel = new Label("Recordings");
        }

        return mRecordingLabel;
    }

    private Button getChangeRecordingButton()
    {
        if(mChangeRecordingButton == null)
        {
            mChangeRecordingButton = new Button("Change...");
            mChangeRecordingButton.setTooltip(new Tooltip("Select a different folder location."));
            mChangeRecordingButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    DirectoryChooser directoryChooser = new DirectoryChooser();
                    directoryChooser.setTitle("Select Recording Folder");
                    directoryChooser.setInitialDirectory(mDirectoryPreference.getDirectoryRecording().toFile());
                    Stage stage = (Stage)getChangeRecordingButton().getScene().getWindow();
                    File selected = directoryChooser.showDialog(stage);

                    if(selected != null)
                    {
                        mDirectoryPreference.setDirectoryRecording(selected.toPath());
                    }
                }
            });
        }

        return mChangeRecordingButton;
    }

    private Button getResetRecordingButton()
    {
        if(mResetRecordingButton == null)
        {
            mResetRecordingButton = new Button("Reset");
            mResetRecordingButton.setTooltip(new Tooltip("Restore the default folder location."));
            mResetRecordingButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    mDirectoryPreference.resetDirectoryRecording();
                }
            });
        }

        return mResetRecordingButton;
    }

    private Label getRecordingPathLabel()
    {
        if(mRecordingPathLabel == null)
        {
            mRecordingPathLabel = new Label(mDirectoryPreference.getDirectoryRecording().toString());
        }

        return mRecordingPathLabel;
    }

    private Label getScreenCaptureLabel()
    {
        if(mScreenCaptureLabel == null)
        {
            mScreenCaptureLabel = new Label("Screen Captures");
        }

        return mScreenCaptureLabel;
    }

    private Button getChangeScreenCaptureButton()
    {
        if(mChangeScreenCaptureButton == null)
        {
            mChangeScreenCaptureButton = new Button("Change...");
            mChangeScreenCaptureButton.setTooltip(new Tooltip("Select a different folder location."));
            mChangeScreenCaptureButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    DirectoryChooser directoryChooser = new DirectoryChooser();
                    directoryChooser.setTitle("Select Screen Capture Folder");
                    directoryChooser.setInitialDirectory(mDirectoryPreference.getDirectoryScreenCapture().toFile());
                    Stage stage = (Stage)getChangeScreenCaptureButton().getScene().getWindow();
                    File selected = directoryChooser.showDialog(stage);

                    if(selected != null)
                    {
                        mDirectoryPreference.setDirectoryScreenCapture(selected.toPath());
                    }
                }
            });
        }

        return mChangeScreenCaptureButton;
    }

    private Button getResetScreenCaptureButton()
    {
        if(mResetScreenCaptureButton == null)
        {
            mResetScreenCaptureButton = new Button("Reset");
            mResetScreenCaptureButton.setTooltip(new Tooltip("Restore the default folder location."));
            mResetScreenCaptureButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    mDirectoryPreference.resetDirectoryScreenCapture();
                }
            });
        }

        return mResetScreenCaptureButton;
    }

    private Label getScreenCapturePathLabel()
    {
        if(mScreenCapturePathLabel == null)
        {
            mScreenCapturePathLabel = new Label(mDirectoryPreference.getDirectoryScreenCapture().toString());
        }

        return mScreenCapturePathLabel;
    }

    private Label getStreamingLabel()
    {
        if(mStreamingLabel == null)
        {
            mStreamingLabel = new Label("Streaming");
        }

        return mStreamingLabel;
    }

    private Button getChangeStreamingButton()
    {
        if(mChangeStreamingButton == null)
        {
            mChangeStreamingButton = new Button("Change...");
            mChangeStreamingButton.setTooltip(new Tooltip("Select a different folder location."));
            mChangeStreamingButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    DirectoryChooser directoryChooser = new DirectoryChooser();
                    directoryChooser.setTitle("Select Streaming Folder");
                    directoryChooser.setInitialDirectory(mDirectoryPreference.getDirectoryStreaming().toFile());
                    Stage stage = (Stage)getChangeStreamingButton().getScene().getWindow();
                    File selected = directoryChooser.showDialog(stage);

                    if(selected != null)
                    {
                        mDirectoryPreference.setDirectoryStreaming(selected.toPath());
                    }
                }
            });
        }

        return mChangeStreamingButton;
    }

    private Button getResetStreamingButton()
    {
        if(mResetStreamingButton == null)
        {
            mResetStreamingButton = new Button("Reset");
            mResetStreamingButton.setTooltip(new Tooltip("Restore the default folder location."));
            mResetStreamingButton.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent event)
                {
                    mDirectoryPreference.resetDirectoryStreaming();
                }
            });
        }

        return mResetStreamingButton;
    }

    private Label getStreamingPathLabel()
    {
        if(mStreamingPathLabel == null)
        {
            mStreamingPathLabel = new Label(mDirectoryPreference.getDirectoryStreaming().toString());
        }

        return mStreamingPathLabel;
    }

    @Subscribe
    public void preferenceUpdated(PreferenceType preferenceType)
    {
        if(preferenceType != null && preferenceType == PreferenceType.DIRECTORY)
        {
            getApplicationRootPathLabel().setText(mDirectoryPreference.getDirectoryApplicationRoot().toString());
            getApplicationLogsPathLabel().setText(mDirectoryPreference.getDirectoryApplicationLog().toString());
            getEventLogsPathLabel().setText(mDirectoryPreference.getDirectoryEventLog().toString());
            getPlaylistPathLabel().setText(mDirectoryPreference.getDirectoryPlaylist().toString());
            getRecordingPathLabel().setText(mDirectoryPreference.getDirectoryRecording().toString());
            getScreenCapturePathLabel().setText(mDirectoryPreference.getDirectoryScreenCapture().toString());
            getStreamingPathLabel().setText(mDirectoryPreference.getDirectoryStreaming().toString());
        }
    }
}
