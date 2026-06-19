package io.github.dsheirer.channel.metadata;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.source.tuner.ui.DiscoveredTunerModel;
import io.github.dsheirer.settings.SettingsManager;
import io.github.dsheirer.preference.NowPlayingPreference;
import io.github.dsheirer.module.ProcessingChain;
import io.github.dsheirer.controller.channel.ChannelProcessingManager;


import io.github.dsheirer.sample.Listener;

import javafx.scene.layout.VBox;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableCell;
import javafx.scene.layout.Priority;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class ChannelMetadataPanel extends VBox
{
    //Displays channel frequency in MHz with up to 4 decimal places (e.g. 155.1252).
    private static final DecimalFormat FREQUENCY_MHZ_FORMAT = new DecimalFormat("0.0###");

    private ChannelProcessingManager mChannelProcessingManager;
    private PlaylistManager mPlaylistManager;
    private SettingsManager mSettingsManager;
    private DiscoveredTunerModel mDiscoveredTunerModel;
    private NowPlayingPreference mNowPlayingPreference;
    
    private TableView<ChannelMetadata> mTable;
    private List<Listener<ProcessingChain>> mListeners = new ArrayList<>();
    
    private Map<ChannelMetadata, List<Boolean>> mActivityHistory = new HashMap<>();
    //Running tally of receive events (idle->active transitions) per channel for the "Received" column.
    private Map<ChannelMetadata, Integer> mReceivedCount = new HashMap<>();
    private Timeline mPollingTimeline;
    private HBox mToolbar;

    public ChannelMetadataPanel(ChannelProcessingManager channelProcessingManager, 
                                PlaylistManager playlistManager, 
                                SettingsManager systemSettingsManager, 
                                DiscoveredTunerModel discoveredTunerModel, 
                                NowPlayingPreference nowPlayingPreference)
    {
        mChannelProcessingManager = channelProcessingManager;
        mPlaylistManager = playlistManager;
        mSettingsManager = systemSettingsManager;
        mDiscoveredTunerModel = discoveredTunerModel;
        mNowPlayingPreference = nowPlayingPreference;
        
        mTable = new TableView<>();
        mTable.getStyleClass().add("preferences-table");
        
        TableColumn<ChannelMetadata, String> stateCol = new TableColumn<>("State");
        stateCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
            cellData.getValue().hasDecoderStateIdentifier() ? cellData.getValue().getChannelStateIdentifier().toString() : ""));
        
        TableColumn<ChannelMetadata, String> channelCol = new TableColumn<>("Channel");
        channelCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
            cellData.getValue().hasChannelConfigurationIdentifier() ? cellData.getValue().getChannelNameConfigurationIdentifier().toString() : ""));
        //Default the Channel column wide enough to show full channel names (e.g. "Oakland/Belgrade/Rome").
        channelCol.setPrefWidth(200);
            
        TableColumn<ChannelMetadata, String> freqCol = new TableColumn<>("Frequency");
        freqCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
            formatFrequencyMhz(cellData.getValue())));

        TableColumn<ChannelMetadata, String> receivedCol = new TableColumn<>("Received");
        receivedCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
            String.valueOf(mReceivedCount.getOrDefault(cellData.getValue(), 0))));

        TableColumn<ChannelMetadata, String> toCol = new TableColumn<>("To");
        toCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
            cellData.getValue().hasToIdentifier() ? cellData.getValue().getToIdentifier().toString() : ""));

        TableColumn<ChannelMetadata, String> fromCol = new TableColumn<>("From");
        fromCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
            cellData.getValue().hasFromIdentifier() ? cellData.getValue().getFromIdentifier().toString() : ""));
            
        //Activity column removed (low-value sparkline). The idle->active polling that powers the Received
        //count is retained in setupActivityPolling().

        mTable.getColumns().addAll(stateCol, channelCol, freqCol, receivedCol, toCol, fromCol);
        mTable.setTableMenuButtonVisible(true);
        
        setupActivityPolling();
        
        if (mChannelProcessingManager != null && mChannelProcessingManager.getChannelMetadataModel() != null) {
            mTable.setItems(mChannelProcessingManager.getChannelMetadataModel().getObservableList());
        }
        
        VBox.setVgrow(mTable, Priority.ALWAYS);
        mTable.setPlaceholder(new Label("No active channels — start a channel to see activity here"));
        
        mToolbar = new HBox(8);
        mToolbar.setPadding(new Insets(4, 10, 4, 10));
        mToolbar.setAlignment(Pos.CENTER_RIGHT);
        mToolbar.getStyleClass().add("kennebec-filter-toolbar");
        Button toggleDetailsBtn = new Button("\u25B6 Show Details");
        toggleDetailsBtn.getStyleClass().add("kennebec-toolbar-button");
        toggleDetailsBtn.setTooltip(new javafx.scene.control.Tooltip("Show or hide the channel details panel"));
        toggleDetailsBtn.setOnAction(e -> {
            for(Listener<ProcessingChain> listener : mListeners) {
                if (listener instanceof io.github.dsheirer.channel.metadata.NowPlayingPanel) {
                    io.github.dsheirer.channel.metadata.NowPlayingPanel np = (io.github.dsheirer.channel.metadata.NowPlayingPanel) listener;
                    np.toggleDetailsPane();
                    // Update button text based on new state
                    boolean nowVisible = np.isDetailsPaneVisible();
                    toggleDetailsBtn.setText(nowVisible ? "\u25C0 Hide Details" : "\u25B6 Show Details");
                }
            }
        });
        mToolbar.getChildren().add(toggleDetailsBtn);

        getChildren().addAll(mToolbar, mTable);
        mTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            ProcessingChain pc = null;
            if (newSelection != null) {
                pc = mChannelProcessingManager.getProcessingChain(mChannelProcessingManager.getChannelMetadataModel().getChannelFromMetadata(newSelection));
            }
            for(Listener<ProcessingChain> listener : mListeners) {
                listener.receive(pc);
            }
        });
    }
    
    /**
     * Formats the channel's frequency (stored in Hz) as MHz with up to 4 decimal places, e.g. 155.1252.
     */
    private static String formatFrequencyMhz(ChannelMetadata metadata)
    {
        try
        {
            if(metadata != null && metadata.hasFrequencyConfigurationIdentifier())
            {
                Object value = metadata.getFrequencyConfigurationIdentifier().getValue();

                if(value instanceof Number)
                {
                    return FREQUENCY_MHZ_FORMAT.format(((Number) value).doubleValue() / 1_000_000.0d);
                }
            }
        }
        catch(Throwable t)
        {
            //Fall through to empty string
        }

        return "";
    }

    private void setupActivityPolling() {
        mPollingTimeline = new Timeline(new KeyFrame(Duration.millis(500), e -> {
            boolean updated = false;
            for (ChannelMetadata meta : mTable.getItems()) {
                List<Boolean> history = mActivityHistory.computeIfAbsent(meta, k -> new ArrayList<>());
                boolean isActive = meta.hasDecoderStateIdentifier() && meta.getChannelStateIdentifier().toString().equalsIgnoreCase("ACTIVE");
                //Count each transition from idle to active as one received event.
                boolean wasActive = !history.isEmpty() && history.get(history.size() - 1);
                if(isActive && !wasActive)
                {
                    mReceivedCount.merge(meta, 1, Integer::sum);
                }
                history.add(isActive);
                if (history.size() > 20) {
                    history.remove(0);
                }
                updated = true;
            }
            if (updated) {
                mTable.refresh();
            }
        }));
        mPollingTimeline.setCycleCount(Timeline.INDEFINITE);
        mPollingTimeline.play();
    }

    public void addProcessingChainSelectionListener(Listener<ProcessingChain> listener)
    {
        mListeners.add(listener);
    }

    public void removeProcessingChainSelectionListener(Listener<ProcessingChain> listener)
    {
        mListeners.remove(listener);
    }
}
