package io.github.dsheirer.channel.details;

import io.github.dsheirer.channel.state.DecoderState;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelProcessingManager;
import io.github.dsheirer.module.ProcessingChain;
import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.source.config.SourceConfigTunerMultipleFrequency;
import io.github.dsheirer.source.config.SourceConfiguration;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

import java.text.DecimalFormat;
import java.util.List;

public class ChannelDetailViewController {
    private static final String EMPTY_DETAILS = "Please select a channel to view details";
    private static final DecimalFormat FREQUENCY_FORMAT = new DecimalFormat("#.00000");

    @FXML private Label systemLabel;
    @FXML private Label siteLabel;
    @FXML private Label nameLabel;
    @FXML private TextArea detailTextPane;

    private ChannelProcessingManager mChannelProcessingManager;
    private ProcessingChain mProcessingChain;

    public void initialize() {
        detailTextPane.setText(EMPTY_DETAILS);
    }

    public void setChannelProcessingManager(ChannelProcessingManager manager) {
        mChannelProcessingManager = manager;
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        receive(mProcessingChain);
    }

    public void receive(ProcessingChain processingChain) {
        mProcessingChain = processingChain;

        Channel channel = mChannelProcessingManager != null ? mChannelProcessingManager.getChannel(processingChain) : null;

        final String system = channel != null ? channel.getSystem() : null;
        final String site = channel != null ? channel.getSite() : null;
        final String name = channel != null ?
            (channel.getChannelType() == Channel.ChannelType.TRAFFIC ? "Traffic Channel" : channel.getName()) : null;

        final String details;

        if(processingChain != null) {
            StringBuilder sb = new StringBuilder();

            //Add channel configuration header with useful context
            if(channel != null) {
                sb.append("Channel Configuration\n");

                if(channel.getDecodeConfiguration() != null) {
                    sb.append("  Decoder: ").append(channel.getDecodeConfiguration().getDecoderType()).append("\n");
                }

                SourceConfiguration sourceConfig = channel.getSourceConfiguration();
                if(sourceConfig instanceof SourceConfigTuner) {
                    long freq = ((SourceConfigTuner)sourceConfig).getFrequency();
                    if(freq > 0) {
                        sb.append("  Frequency: ").append(FREQUENCY_FORMAT.format(freq / 1e6d)).append(" MHz\n");
                    }
                } else if(sourceConfig instanceof SourceConfigTunerMultipleFrequency) {
                    List<Long> frequencies = ((SourceConfigTunerMultipleFrequency)sourceConfig).getFrequencies();
                    if(frequencies != null && !frequencies.isEmpty()) {
                        sb.append("  Frequencies: ");
                        for(int i = 0; i < frequencies.size(); i++) {
                            if(i > 0) sb.append(", ");
                            sb.append(FREQUENCY_FORMAT.format(frequencies.get(i) / 1e6d));
                        }
                        sb.append(" MHz\n");
                    }
                }

                String aliasList = channel.getAliasListName();
                if(aliasList != null && !aliasList.isEmpty()) {
                    sb.append("  Alias List: ").append(aliasList).append("\n");
                }

                sb.append("\n");
            }

            for(DecoderState decoderState : processingChain.getDecoderStates()) {
                sb.append(decoderState.getActivitySummary());
            }

            details = sb.toString();
        } else {
            details = EMPTY_DETAILS;
        }

        Platform.runLater(() -> {
            systemLabel.setText(system);
            siteLabel.setText(site);
            nameLabel.setText(name);
            detailTextPane.setText(details);
        });
    }
}
