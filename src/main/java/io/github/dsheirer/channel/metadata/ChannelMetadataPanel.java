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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class ChannelMetadataPanel extends VBox
{
    private ChannelProcessingManager mChannelProcessingManager;
    private PlaylistManager mPlaylistManager;
    private SettingsManager mSettingsManager;
    private DiscoveredTunerModel mDiscoveredTunerModel;
    private NowPlayingPreference mNowPlayingPreference;
    
    private TableView<ChannelMetadata> mTable;
    private List<Listener<ProcessingChain>> mListeners = new ArrayList<>();
    
    private Map<ChannelMetadata, List<Boolean>> mActivityHistory = new HashMap<>();
    private Timeline mPollingTimeline;

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
        
        TableColumn<ChannelMetadata, String> stateCol = new TableColumn<>("State");
        stateCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
            cellData.getValue().hasDecoderStateIdentifier() ? cellData.getValue().getChannelStateIdentifier().toString() : ""));
        
        TableColumn<ChannelMetadata, String> channelCol = new TableColumn<>("Channel");
        channelCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
            cellData.getValue().hasChannelConfigurationIdentifier() ? cellData.getValue().getChannelNameConfigurationIdentifier().toString() : ""));
            
        TableColumn<ChannelMetadata, String> freqCol = new TableColumn<>("Frequency");
        freqCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
            cellData.getValue().hasFrequencyConfigurationIdentifier() ? cellData.getValue().getFrequencyConfigurationIdentifier().toString() : ""));

        TableColumn<ChannelMetadata, String> toCol = new TableColumn<>("To");
        toCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
            cellData.getValue().hasToIdentifier() ? cellData.getValue().getToIdentifier().toString() : ""));

        TableColumn<ChannelMetadata, String> fromCol = new TableColumn<>("From");
        fromCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
            cellData.getValue().hasFromIdentifier() ? cellData.getValue().getFromIdentifier().toString() : ""));
            
        TableColumn<ChannelMetadata, ChannelMetadata> activityCol = new TableColumn<>("Activity");
        activityCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue()));
        activityCol.setCellFactory(column -> new TableCell<ChannelMetadata, ChannelMetadata>() {
            private Canvas canvas = new Canvas(60, 16);
            {
                setGraphic(canvas);
            }
            @Override
            protected void updateItem(ChannelMetadata item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    setGraphic(canvas);
                    GraphicsContext gc = canvas.getGraphicsContext2D();
                    gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                    List<Boolean> history = mActivityHistory.get(item);
                    if (history != null) {
                        double barWidth = 3;
                        double spacing = 1;
                        double x = canvas.getWidth() - barWidth;
                        for (int i = history.size() - 1; i >= 0 && x >= 0; i--) {
                            if (history.get(i)) {
                                gc.setFill(Color.web("#00ffcc")); // active cyan
                                gc.fillRect(x, 2, barWidth, canvas.getHeight() - 4);
                            } else {
                                gc.setFill(Color.web("#333333")); // idle dark gray
                                gc.fillRect(x, canvas.getHeight() / 2 - 1, barWidth, 2);
                            }
                            x -= (barWidth + spacing);
                        }
                    }
                }
            }
        });

        mTable.getColumns().addAll(stateCol, activityCol, channelCol, freqCol, toCol, fromCol);
        mTable.setTableMenuButtonVisible(true);
        
        setupActivityPolling();
        
        if (mChannelProcessingManager != null && mChannelProcessingManager.getChannelMetadataModel() != null) {
            mTable.setItems(mChannelProcessingManager.getChannelMetadataModel().getObservableList());
        }
        
        VBox.setVgrow(mTable, Priority.ALWAYS);
        mTable.setPlaceholder(new Label("No active channels — start a channel to see activity here"));
        getChildren().add(mTable);
        
        mTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                ProcessingChain pc = mChannelProcessingManager.getProcessingChain(mChannelProcessingManager.getChannelMetadataModel().getChannelFromMetadata(newSelection));
                for(Listener<ProcessingChain> listener : mListeners) {
                    listener.receive(pc);
                }
            }
        });
    }
    
    private void setupActivityPolling() {
        mPollingTimeline = new Timeline(new KeyFrame(Duration.millis(500), e -> {
            boolean updated = false;
            for (ChannelMetadata meta : mTable.getItems()) {
                List<Boolean> history = mActivityHistory.computeIfAbsent(meta, k -> new ArrayList<>());
                boolean isActive = meta.hasDecoderStateIdentifier() && meta.getChannelStateIdentifier().toString().equalsIgnoreCase("ACTIVE");
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
