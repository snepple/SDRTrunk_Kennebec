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
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.playlist.channel.ViewChannelRequest;
import io.github.dsheirer.module.ProcessingChain;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.source.config.SourceConfigTunerMultipleFrequency;
import io.github.dsheirer.source.config.SourceConfiguration;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * "Details" tab for the Now Playing view.  Presents the selected channel's key configuration as a clean
 * key/value card with a link to edit it in the playlist editor, and renders each decoder's live activity
 * summary in a styled, non-terminal report (section headings bolded, key/value rows aligned) rather than a
 * raw monospace text dump.  A Copy button preserves the ability to grab the raw report text.
 */
public class ChannelDetailPanel extends VBox implements Listener<ProcessingChain>
{
    private static final String EMPTY_DETAILS = "Please select a channel to view details";
    private static final DecimalFormat FREQUENCY_FORMAT = new DecimalFormat("#.00000");

    private Label mSystemLabel;
    private Label mSiteLabel;
    private Label mNameLabel;
    private GridPane mConfigGrid;
    private VBox mReportBox;
    private Button mEditButton;
    private Button mCopyButton;

    private final ChannelProcessingManager mChannelProcessingManager;
    private ProcessingChain mProcessingChain;
    private Channel mCurrentChannel;
    private String mRawReport = "";

    public ChannelDetailPanel(ChannelProcessingManager channelProcessingManager)
    {
        mChannelProcessingManager = channelProcessingManager;
        init();
    }

    private void init()
    {
        setSpacing(8);
        setPadding(new Insets(10));

        getChildren().add(buildHeader());
        getChildren().add(buildConfigCard());
        getChildren().add(buildReportCard());

        //Initial empty state.
        rebuildConfigGrid(new ArrayList<>());
        renderReport(EMPTY_DETAILS);
    }

    /**
     * Header card: System / Site / Channel identity plus the Edit, Copy and Refresh actions.
     */
    private HBox buildHeader()
    {
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(4, 10, 4, 10));
        header.getStyleClass().add("channel-detail-header");

        mSystemLabel = value(" ");
        mSiteLabel = value(" ");
        mNameLabel = value(" ");
        header.getChildren().addAll(key("System:"), mSystemLabel, key("Site:"), mSiteLabel,
                key("Channel:"), mNameLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().add(spacer);

        mEditButton = new Button("Edit Channel");
        mEditButton.getStyleClass().add("flat-button");
        mEditButton.setTooltip(new Tooltip("Open this channel's configuration in the Playlist editor"));
        mEditButton.setDisable(true);
        mEditButton.setOnAction(e -> openChannelEditor());

        mCopyButton = new Button("Copy");
        mCopyButton.getStyleClass().add("flat-button");
        mCopyButton.setTooltip(new Tooltip("Copy the activity report text to the clipboard"));
        mCopyButton.setDisable(true);
        mCopyButton.setOnAction(e -> copyReport());

        Button refreshButton = new Button("Refresh");
        refreshButton.getStyleClass().add("flat-button");
        refreshButton.setTooltip(new Tooltip("Re-read the current channel's details"));
        refreshButton.setOnAction(e -> receive(mProcessingChain));

        header.getChildren().addAll(mEditButton, mCopyButton, refreshButton);
        return header;
    }

    /**
     * Configuration card: a key/value grid of the channel's core settings.
     */
    private VBox buildConfigCard()
    {
        mConfigGrid = new GridPane();
        mConfigGrid.setHgap(10);
        mConfigGrid.setVgap(3);

        ColumnConstraints keyColumn = new ColumnConstraints();
        ColumnConstraints valueColumn = new ColumnConstraints();
        valueColumn.setHgrow(Priority.ALWAYS);
        valueColumn.setFillWidth(true);
        mConfigGrid.getColumnConstraints().addAll(keyColumn, valueColumn);

        VBox card = new VBox(mConfigGrid);
        card.getStyleClass().add("channel-detail-card");
        return card;
    }

    /**
     * Report card: scrollable, styled rendering of the decoder activity summaries.
     */
    private VBox buildReportCard()
    {
        mReportBox = new VBox(2);
        mReportBox.setPadding(new Insets(4));

        ScrollPane scroll = new ScrollPane(mReportBox);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("channel-detail-scroll");

        VBox card = new VBox(scroll);
        card.getStyleClass().add("channel-detail-card");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        VBox.setVgrow(card, Priority.ALWAYS);
        return card;
    }

    @Override
    public void receive(ProcessingChain processingChain)
    {
        mProcessingChain = processingChain;

        Channel channel = mChannelProcessingManager.getChannel(processingChain);

        final String system = channel != null ? channel.getSystem() : null;
        final String site = channel != null ? channel.getSite() : null;
        final String name = channel != null ?
            (channel.getChannelType() == Channel.ChannelType.TRAFFIC ? "Traffic Channel" : channel.getName()) : null;

        //Only standard (playlist) channels can be opened in the editor; traffic channels are ephemeral.
        final boolean editable = channel != null && channel.getChannelType() != Channel.ChannelType.TRAFFIC;
        final Channel editChannel = channel;

        final List<String[]> configRows = new ArrayList<>();
        final String report;

        if(processingChain != null)
        {
            if(channel != null)
            {
                if(channel.getDecodeConfiguration() != null)
                {
                    configRows.add(new String[]{"Decoder", String.valueOf(channel.getDecodeConfiguration().getDecoderType())});
                }

                SourceConfiguration sourceConfig = channel.getSourceConfiguration();
                if(sourceConfig instanceof SourceConfigTuner)
                {
                    long freq = ((SourceConfigTuner)sourceConfig).getFrequency();
                    if(freq > 0)
                    {
                        configRows.add(new String[]{"Frequency", FREQUENCY_FORMAT.format(freq / 1e6d) + " MHz"});
                    }
                }
                else if(sourceConfig instanceof SourceConfigTunerMultipleFrequency)
                {
                    List<Long> frequencies = ((SourceConfigTunerMultipleFrequency)sourceConfig).getFrequencies();
                    if(frequencies != null && !frequencies.isEmpty())
                    {
                        StringBuilder fsb = new StringBuilder();
                        for(int i = 0; i < frequencies.size(); i++)
                        {
                            if(i > 0) fsb.append(", ");
                            fsb.append(FREQUENCY_FORMAT.format(frequencies.get(i) / 1e6d));
                        }
                        fsb.append(" MHz");
                        configRows.add(new String[]{"Frequencies", fsb.toString()});
                    }
                }

                String aliasList = channel.getAliasListName();
                if(aliasList != null && !aliasList.isEmpty())
                {
                    configRows.add(new String[]{"Alias List", aliasList});
                }
            }

            StringBuilder sb = new StringBuilder();
            for(DecoderState decoderState : processingChain.getDecoderStates())
            {
                sb.append(decoderState.getActivitySummary());
            }
            report = sb.toString().trim();
        }
        else
        {
            report = EMPTY_DETAILS;
        }

        Platform.runLater(() -> {
            mSystemLabel.setText(system);
            mSiteLabel.setText(site);
            mNameLabel.setText(name);

            mCurrentChannel = editChannel;
            mEditButton.setDisable(!editable);

            rebuildConfigGrid(configRows);

            mRawReport = report;
            mCopyButton.setDisable(report == null || report.isEmpty() || report.equals(EMPTY_DETAILS));
            renderReport(report.isEmpty() ? EMPTY_DETAILS : report);
        });
    }

    /**
     * Rebuilds the configuration key/value grid from the supplied rows.
     */
    private void rebuildConfigGrid(List<String[]> rows)
    {
        mConfigGrid.getChildren().clear();

        if(rows.isEmpty())
        {
            mConfigGrid.add(value("No channel selected"), 0, 0, 2, 1);
            return;
        }

        int row = 0;
        for(String[] kv : rows)
        {
            Label valueLabel = value(kv[1]);
            valueLabel.setWrapText(true);
            mConfigGrid.add(key(kv[0] + ":"), 0, row);
            mConfigGrid.add(valueLabel, 1, row);
            row++;
        }
    }

    /**
     * Renders the raw activity-summary text into styled rows: section headings (no leading whitespace, no
     * tab) are bolded, tab-delimited "key: value" lines are split into aligned labels, and indented lines
     * become wrapped detail rows.  This modernizes the display without changing any decoder's summary text.
     */
    private void renderReport(String report)
    {
        mReportBox.getChildren().clear();

        if(report == null || report.isEmpty())
        {
            return;
        }

        for(String line : report.split("\n", -1))
        {
            if(line.isBlank())
            {
                Region gap = new Region();
                gap.setMinHeight(6);
                mReportBox.getChildren().add(gap);
                continue;
            }

            int tab = line.indexOf('\t');
            if(tab >= 0)
            {
                //Key/value row.
                Label keyLabel = key(line.substring(0, tab).trim());
                Label valueLabel = value(line.substring(tab + 1).trim());
                valueLabel.setWrapText(true);
                HBox.setHgrow(valueLabel, Priority.ALWAYS);
                HBox kvRow = new HBox(6, keyLabel, valueLabel);
                mReportBox.getChildren().add(kvRow);
            }
            else if(!Character.isWhitespace(line.charAt(0)))
            {
                //Section heading.
                Label heading = value(line.trim());
                heading.getStyleClass().add("channel-detail-section");
                heading.setWrapText(true);
                VBox.setMargin(heading, new Insets(4, 0, 2, 0));
                mReportBox.getChildren().add(heading);
            }
            else
            {
                //Indented detail line.
                Label detail = value(line.trim());
                detail.setWrapText(true);
                VBox.setMargin(detail, new Insets(0, 0, 0, 14));
                mReportBox.getChildren().add(detail);
            }
        }
    }

    /**
     * Opens the current channel's configuration in the playlist editor via the global event bus.
     */
    private void openChannelEditor()
    {
        Channel channel = mCurrentChannel;
        if(channel == null)
        {
            return;
        }
        //Posting a ViewChannelRequest brings the playlist editor forward and opens this channel (matching
        //the behavior used by the Radio Reference site/frequency editors).
        MyEventBus.getGlobalEventBus().post(new ViewChannelRequest(channel));
    }

    /**
     * Copies the raw activity report text to the system clipboard.
     */
    private void copyReport()
    {
        if(mRawReport == null || mRawReport.isEmpty())
        {
            return;
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(mRawReport);
        Clipboard.getSystemClipboard().setContent(content);
    }

    private static Label key(String text)
    {
        Label label = new Label(text);
        label.getStyleClass().add("channel-detail-key");
        return label;
    }

    private static Label value(String text)
    {
        Label label = new Label(text);
        label.getStyleClass().add("channel-detail-value");
        return label;
    }
}
