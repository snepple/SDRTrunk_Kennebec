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
import javafx.scene.layout.Priority;

import java.util.ArrayList;
import java.util.List;

public class ChannelMetadataPanel extends VBox
{
    private ChannelProcessingManager mChannelProcessingManager;
    private PlaylistManager mPlaylistManager;
    private SettingsManager mSettingsManager;
    private DiscoveredTunerModel mDiscoveredTunerModel;
    private NowPlayingPreference mNowPlayingPreference;
    
    private TableView<ChannelMetadata> mTable;
    private List<Listener<ProcessingChain>> mListeners = new ArrayList<>();

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
            
        mTable.getColumns().addAll(stateCol, channelCol, freqCol, toCol, fromCol);
        mTable.setTableMenuButtonVisible(true);
        
        if (mChannelProcessingManager != null && mChannelProcessingManager.getChannelMetadataModel() != null) {
            mTable.setItems(mChannelProcessingManager.getChannelMetadataModel().getObservableList());
        }
        
        VBox.setVgrow(mTable, Priority.ALWAYS);
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

    public void addProcessingChainSelectionListener(Listener<ProcessingChain> listener)
    {
        mListeners.add(listener);
    }

    public void removeProcessingChainSelectionListener(Listener<ProcessingChain> listener)
    {
        mListeners.remove(listener);
    }
}
