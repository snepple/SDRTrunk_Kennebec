/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package io.github.dsheirer.channel.details;

import io.github.dsheirer.channel.state.DecoderState;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelProcessingManager;
import io.github.dsheirer.module.ProcessingChain;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.source.config.SourceConfigTunerMultipleFrequency;
import io.github.dsheirer.source.config.SourceConfiguration;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.text.DecimalFormat;
import java.util.List;

public class ChannelDetailPanel extends VBox implements Listener<ProcessingChain>
{
    private static final String EMPTY_DETAILS = "Please select a channel to view details";

    private Label mSystemLabel;
    private Label mSiteLabel;
    private Label mNameLabel;
    private TextArea mDetailTextPane;

    private ChannelProcessingManager mChannelProcessingManager;
    private ProcessingChain mProcessingChain;

    public ChannelDetailPanel(ChannelProcessingManager channelProcessingManager)
    {
        mChannelProcessingManager = channelProcessingManager;
        init();
    }

    private void init()
    {
        setSpacing(8);
        setPadding(new Insets(10));

        HBox buttonPanel = new HBox(10);
        buttonPanel.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        buttonPanel.setPadding(new Insets(4, 10, 4, 10));
        buttonPanel.setStyle("-fx-background-color: #F9F9FB; -fx-background-radius: 8; -fx-border-color: #E5E5EA; -fx-border-radius: 8; -fx-border-width: 1;");

        Label systemTitle = new Label("System:");
        systemTitle.setStyle("-fx-text-fill: #8E8E93; -fx-font-weight: bold; -fx-font-size: 12px;");
        buttonPanel.getChildren().add(systemTitle);
        mSystemLabel = new Label(" ");
        mSystemLabel.setStyle("-fx-text-fill: #1C1C1E; -fx-font-size: 13px;");
        buttonPanel.getChildren().add(mSystemLabel);

        Label siteTitle = new Label("Site:");
        siteTitle.setStyle("-fx-text-fill: #8E8E93; -fx-font-weight: bold; -fx-font-size: 12px;");
        buttonPanel.getChildren().add(siteTitle);
        mSiteLabel = new Label(" ");
        mSiteLabel.setStyle("-fx-text-fill: #1C1C1E; -fx-font-size: 13px;");
        buttonPanel.getChildren().add(mSiteLabel);

        Label nameTitle = new Label("Channel:");
        nameTitle.setStyle("-fx-text-fill: #8E8E93; -fx-font-weight: bold; -fx-font-size: 12px;");
        buttonPanel.getChildren().add(nameTitle);
        mNameLabel = new Label(" ");
        mNameLabel.setStyle("-fx-text-fill: #1C1C1E; -fx-font-size: 13px;");
        buttonPanel.getChildren().add(mNameLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        buttonPanel.getChildren().add(spacer);

        Button refreshButton = new Button("Refresh");
        refreshButton.getStyleClass().add("flat-button");
        refreshButton.setOnAction(e -> receive(mProcessingChain));
        buttonPanel.getChildren().add(refreshButton);

        getChildren().add(buttonPanel);

        mDetailTextPane = new TextArea(EMPTY_DETAILS);
        mDetailTextPane.setEditable(false);
        mDetailTextPane.setStyle("-fx-font-family: monospace; -fx-font-size: 12px; -fx-background-radius: 8; -fx-border-radius: 8;");
        VBox.setVgrow(mDetailTextPane, Priority.ALWAYS);

        getChildren().add(mDetailTextPane);
    }

    private static final DecimalFormat FREQUENCY_FORMAT = new DecimalFormat("#.00000");

    @Override
    public void receive(ProcessingChain processingChain)
    {
        mProcessingChain = processingChain;

        Channel channel = mChannelProcessingManager.getChannel(processingChain);

        final String system = channel != null ? channel.getSystem() : null;
        final String site = channel != null ? channel.getSite() : null;
        final String name = channel != null ?
            (channel.getChannelType() == Channel.ChannelType.TRAFFIC ? "Traffic Channel" : channel.getName()) : null;

        final String details;

        if(processingChain != null)
        {
            StringBuilder sb = new StringBuilder();

            //Add channel configuration header with useful context
            if(channel != null)
            {
                sb.append("Channel Configuration\n");

                if(channel.getDecodeConfiguration() != null)
                {
                    sb.append("  Decoder: ").append(channel.getDecodeConfiguration().getDecoderType()).append("\n");
                }

                SourceConfiguration sourceConfig = channel.getSourceConfiguration();
                if(sourceConfig instanceof SourceConfigTuner)
                {
                    long freq = ((SourceConfigTuner)sourceConfig).getFrequency();
                    if(freq > 0)
                    {
                        sb.append("  Frequency: ").append(FREQUENCY_FORMAT.format(freq / 1e6d)).append(" MHz\n");
                    }
                }
                else if(sourceConfig instanceof SourceConfigTunerMultipleFrequency)
                {
                    List<Long> frequencies = ((SourceConfigTunerMultipleFrequency)sourceConfig).getFrequencies();
                    if(frequencies != null && !frequencies.isEmpty())
                    {
                        sb.append("  Frequencies: ");
                        for(int i = 0; i < frequencies.size(); i++)
                        {
                            if(i > 0) sb.append(", ");
                            sb.append(FREQUENCY_FORMAT.format(frequencies.get(i) / 1e6d));
                        }
                        sb.append(" MHz\n");
                    }
                }

                String aliasList = channel.getAliasListName();
                if(aliasList != null && !aliasList.isEmpty())
                {
                    sb.append("  Alias List: ").append(aliasList).append("\n");
                }

                sb.append("\n");
            }

            for(DecoderState decoderState : processingChain.getDecoderStates())
            {
                sb.append(decoderState.getActivitySummary());
            }

            details = sb.toString();
        }
        else
        {
            details = EMPTY_DETAILS;
        }

        Platform.runLater(() -> {
            mSystemLabel.setText(system);
            mSiteLabel.setText(site);
            mNameLabel.setText(name);
            mDetailTextPane.setText(details);
        });
    }
}
