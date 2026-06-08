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
package io.github.dsheirer.controller.channel;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.util.ThreadPool;






import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import org.slf4j.Logger;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.preference.UserPreferences;
import org.slf4j.LoggerFactory;








public class ChannelAutoStartFrame
{
    private final static Logger mLog = LoggerFactory.getLogger(ChannelAutoStartFrame.class);

    private Listener<ChannelEvent> mChannelEventListener;
    private List<Channel> mChannels;





    private AtomicBoolean mChannelsStarted = new AtomicBoolean();
    private int mAutoStartTimeoutSeconds;


    /**
     * Creates and displays a channel auto-start gui for presenting the user with a list of channels that
     * will be automatically started once the countdown timer reaches zero, or the user chooses to start
     * now or cancel.
     *
     * @param listener to receive channel start/enable request(s)
     * @param channels to auto-start
     */

    private Stage mStage;
    private Label mCountdownLabel;
    private Button mStartButton;
    private Button mCancelButton;
    private TableView<Channel> mChannelTable;
    private Timeline mCountdownTimeline;

    public ChannelAutoStartFrame(Listener<ChannelEvent> listener, List<Channel> channels, UserPreferences preferences)
    {
        mChannelEventListener = listener;
        mChannels = channels;

        Platform.runLater(() -> {
            mStage = new Stage();
            mStage.setTitle("Channel Auto-Start Manager");

            VBox root = new VBox(10);
            root.setPadding(new Insets(10));

            mCountdownLabel = new Label("Starting channels in ...");
            root.getChildren().add(mCountdownLabel);

            mChannelTable = new TableView<>();
            mChannelTable.setPlaceholder(new Label("No channels configured for auto-start"));
            mChannelTable.setItems(FXCollections.observableArrayList(mChannels));
            VBox.setVgrow(mChannelTable, Priority.ALWAYS);

            TableColumn<Channel, String> col = new TableColumn<>("Channel Name");
            col.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
            col.setPrefWidth(280);
            mChannelTable.getColumns().add(col);
            mChannelTable.setTableMenuButtonVisible(true);

            root.getChildren().add(mChannelTable);

            HBox buttonBox = new HBox(10);
            buttonBox.setAlignment(Pos.CENTER_RIGHT);

            mStartButton = new Button("Start");
            mStartButton.setOnAction(e -> {
                if (mCountdownTimeline != null) mCountdownTimeline.stop();
                startChannels();
                mStage.close();
            });

            mCancelButton = new Button("Cancel");
            mCancelButton.setOnAction(e -> {
                if (mCountdownTimeline != null) mCountdownTimeline.stop();
                mStage.close();
            });

            buttonBox.getChildren().addAll(mStartButton, mCancelButton);
            root.getChildren().add(buttonBox);

            Scene scene = new Scene(root, 400, 300);
            mStage.setScene(scene);

            mStage.setOnCloseRequest(e -> {
                mLog.info("Channel auto-start canceled by user - window closed");
                if (mCountdownTimeline != null) mCountdownTimeline.stop();
            });

            mStage.show();
            mStage.toFront();

            mAutoStartTimeoutSeconds = preferences.getApplicationPreference().getChannelAutoStartTimeout();

            mCountdownTimeline = new Timeline(new KeyFrame(Duration.millis(1000), e -> {
                mAutoStartTimeoutSeconds--;
                mCountdownLabel.setText("Starting channels in " + mAutoStartTimeoutSeconds + " seconds.");

                if (mAutoStartTimeoutSeconds <= 0) {
                    mCountdownTimeline.stop();
                    startChannels();
                    mStage.close();
                }
            }));
            mCountdownTimeline.setCycleCount(Timeline.INDEFINITE);
            mCountdownTimeline.play();
        });
    }

    public void setVisible(boolean visible) {
        if (mStage != null) {
            Platform.runLater(() -> {
                if (visible) {
                    mStage.show();
                    mStage.toFront();
                } else {
                    mStage.hide();
                }
            });
        } else {
            Platform.runLater(() -> {
                if (mStage != null && visible) {
                    mStage.show();
                    mStage.toFront();
                }
            });
        }
    }

    private void startChannels()
    {
        if(mChannelsStarted.compareAndSet(false, true))
        {
            if(mChannelEventListener != null)
            {
                for(Channel channel : mChannels)
                {
                    mChannelEventListener.receive(new ChannelEvent(channel, ChannelEvent.Event.REQUEST_ENABLE));
                }
            }
        }
    }
}
