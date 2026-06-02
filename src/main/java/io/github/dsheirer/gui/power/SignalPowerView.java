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

package io.github.dsheirer.gui.power;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.dsp.squelch.ISquelchConfiguration;
import io.github.dsheirer.gui.control.DbPowerMeterJFX;

import io.github.dsheirer.module.ProcessingChain;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.source.SourceEvent;
import javafx.application.Platform;
import javafx.scene.layout.Pane;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;




import java.text.DecimalFormat;

/**
 * Swing view for displaying signal power measurements with integrated squelch control.
 */
public class SignalPowerView extends VBox
{
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.0");
    private static final String NOT_AVAILABLE = "Not Available";

    // We keep mPowerMeter as DbPowerMeterJFX
    private final DbPowerMeterJFX mPowerMeter = new DbPowerMeterJFX();
    private final PeakMonitor mPeakMonitor = new PeakMonitor(DbPowerMeterJFX.DEFAULT_MINIMUM_POWER);
    private Label mPowerLabel;
    private Label mPeakLabel;
    private Label mSquelchLabel;
    private Label mSquelchValueLabel;
    private Button mSquelchUpButton;
    private Button mSquelchDownButton;
    private CheckBox mSquelchAutoTrackCheckBox;
    private double mSquelchThreshold;
    private final PlaylistManager mPlaylistManager;
    private ProcessingChain mProcessingChain;

    public SignalPowerView(PlaylistManager playlistManager)
    {
        mPlaylistManager = playlistManager;

        
        
        

        
            HBox root = new HBox(8);
            root.setPadding(new Insets(16));

            mPowerMeter.setPeakVisible(true);
            mPowerMeter.setSquelchThresholdVisible(true);

            VBox meterContainer = new VBox();
            meterContainer.setPadding(new Insets(5));
            meterContainer.setStyle("-fx-border-color: lightgray; -fx-border-width: 1; -fx-border-radius: 3;");
            Label meterTitle = new Label("Power (dB)");
            meterTitle.setStyle("-fx-padding: 0 0 5 0; -fx-text-fill: black;");
            meterContainer.getChildren().addAll(meterTitle, mPowerMeter);

            root.getChildren().add(meterContainer);

            GridPane valuePanel = new GridPane();
            valuePanel.setHgap(8);
            valuePanel.setVgap(12);
            valuePanel.setPadding(new Insets(16));

            mPeakLabel = new Label("0");
            mPeakLabel.setTooltip(new Tooltip("Current peak power level in decibels."));
            valuePanel.add(new Label("Peak:"), 0, 0);
            valuePanel.add(mPeakLabel, 1, 0);

            mPowerLabel = new Label("0");
            mPowerLabel.setTooltip(new Tooltip("Current Power level in decibels"));
            valuePanel.add(new Label("Power:"), 0, 1);
            valuePanel.add(mPowerLabel, 1, 1);

            mSquelchLabel = new Label("Squelch:");
            mSquelchLabel.setDisable(true);
            valuePanel.add(mSquelchLabel, 0, 2);

            mSquelchValueLabel = new Label(NOT_AVAILABLE);
            mSquelchValueLabel.setTooltip(new Tooltip("Squelch threshold value in decibels"));
            mSquelchValueLabel.setDisable(true);
            valuePanel.add(mSquelchValueLabel, 1, 2);

            mSquelchUpButton = new Button("▲");
            mSquelchUpButton.setTooltip(new Tooltip("Increases the squelch threshold value"));
            mSquelchUpButton.setDisable(true);
            mSquelchUpButton.setOnAction(e -> broadcast(SourceEvent.requestSquelchThreshold(null, mSquelchThreshold + 1)));
            valuePanel.add(mSquelchUpButton, 2, 2);

            mSquelchDownButton = new Button("▼");
            mSquelchDownButton.setTooltip(new Tooltip("Decreases the squelch threshold value."));
            mSquelchDownButton.setDisable(true);
            mSquelchDownButton.setOnAction(e -> broadcast(SourceEvent.requestSquelchThreshold(null, mSquelchThreshold - 1)));
            valuePanel.add(mSquelchDownButton, 3, 2);

            mSquelchAutoTrackCheckBox = new CheckBox("Auto Track");
            mSquelchAutoTrackCheckBox.setTooltip(new Tooltip("Enable or disable monitoring of the noise floor to auto-adjust the squelch threshold value maintaining a consistent level/buffer above the noise floor"));
            mSquelchAutoTrackCheckBox.setDisable(true);
            mSquelchAutoTrackCheckBox.setOnAction(e -> {
                broadcast(SourceEvent.requestSquelchAutoTrack(mSquelchAutoTrackCheckBox.isSelected()));
            });
    
            valuePanel.add(mSquelchAutoTrackCheckBox, 0, 3, 4, 1);

            HBox.setHgrow(valuePanel, Priority.ALWAYS);
            root.getChildren().add(valuePanel);
        this.getChildren().add(root);

            


    }

    /**
     * Updates the channel's decode configuration with a new squelch threshold value
     */
    private void setConfigSquelchThreshold(int threshold)
    {
        if(mProcessingChain != null)
        {
            Channel channel = mPlaylistManager.getChannelProcessingManager().getChannel(mProcessingChain);

            if(channel != null && channel.getDecodeConfiguration() instanceof ISquelchConfiguration configuration)
            {
                configuration.setSquelchThreshold(threshold);
                mPlaylistManager.schedulePlaylistSave();
            }
        }
    }

    /**
     * Updates the channel configuration squelch auto-track feature setting.
     * @param autoTrack true to enable.
     */
    private void setConfigSquelchAutoTrack(boolean autoTrack)
    {
        Channel channel = mPlaylistManager.getChannelProcessingManager().getChannel(mProcessingChain);

        if(channel != null && channel.getDecodeConfiguration() instanceof ISquelchConfiguration configuration)
        {
            configuration.setSquelchAutoTrack(autoTrack);
            mPlaylistManager.schedulePlaylistSave();
        }
    }

    private void broadcast(SourceEvent sourceEvent)
    {
        if(mProcessingChain != null)
        {
            mProcessingChain.broadcast(sourceEvent);
        }
    }

    /**
     * Resets controls when changing processing chain source.
     */
    private void reset()
    {
        mPeakMonitor.reset();
        
            mPowerMeter.reset();
            mPeakLabel.setText("0");
            mPowerLabel.setText("0");
            mSquelchLabel.setDisable(true);
            mSquelchValueLabel.setText("Not Available");
            mSquelchValueLabel.setDisable(true);
            mSquelchUpButton.setDisable(true);
            mSquelchDownButton.setDisable(true);
            mSquelchAutoTrackCheckBox.setDisable(true);
            mSquelchAutoTrackCheckBox.setSelected(false);

    }

    public void receive(SourceEvent sourceEvent)
    {
        switch(sourceEvent.getEvent())
        {
            case NOTIFICATION_CHANNEL_POWER ->
            {
                final double power = sourceEvent.getValue().doubleValue();
                final double peak = mPeakMonitor.process(power);

                
                    mPowerMeter.setPower(power);
                    mPowerLabel.setText(DECIMAL_FORMAT.format(power));

                    mPowerMeter.setPeak(peak);
                    mPeakLabel.setText(DECIMAL_FORMAT.format(peak));
        
            }
            case NOTIFICATION_SQUELCH_THRESHOLD ->
            {
                final double threshold = sourceEvent.getValue().doubleValue();
                mSquelchThreshold = threshold;
                setConfigSquelchThreshold((int)threshold);

                
                    mPowerMeter.setSquelchThreshold(threshold);
                    mSquelchLabel.setDisable(false);
                    mSquelchValueLabel.setDisable(false);
                    mSquelchValueLabel.setText(DECIMAL_FORMAT.format(threshold));
                    mSquelchDownButton.setDisable(false);
                    mSquelchUpButton.setDisable(false);
        
            }
            case NOTIFICATION_SQUELCH_AUTO_TRACK ->
            {
                boolean autoTrack = sourceEvent.getValue().intValue() == 1;
                setConfigSquelchAutoTrack(autoTrack);
                
                    mSquelchAutoTrackCheckBox.setSelected(autoTrack);
                    mSquelchAutoTrackCheckBox.setDisable(false);
        
            }
        }
    }

    /**
     * Sets the processing chain for this view
     */
    public void setProcessingChain(ProcessingChain processingChain)
    {
        mProcessingChain = processingChain;
        reset();
    }
}
