package io.github.dsheirer.channel.metadata;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelProcessingManager;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.module.ProcessingChain;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ChannelMetadataPanel extends VBox
{
    private final static Logger mLog = LoggerFactory.getLogger(ChannelMetadataPanel.class);

    private ChannelProcessingManager mChannelProcessingManager;
    private IconModel mIconModel;
    private UserPreferences mUserPreferences;
    private Broadcaster<ProcessingChain> mSelectedProcessingChainBroadcaster = new Broadcaster<>();
    private Channel mUserSelectedChannel;
    private TunerManager mTunerManager;
    private PlaylistManager mPlaylistManager;
    private ChannelMetadataTableController mTableController;

    public ChannelMetadataPanel(PlaylistManager playlistManager, IconModel iconModel, UserPreferences userPreferences,
                                TunerManager tunerManager)
    {
        mPlaylistManager = playlistManager;
        mChannelProcessingManager = playlistManager.getChannelProcessingManager();
        mIconModel = iconModel;
        mUserPreferences = userPreferences;
        mTunerManager = tunerManager;
        init();
    }

    private void init()
    {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/channel/metadata/ChannelMetadataTable.fxml"));
                Parent root = loader.load();
                mTableController = loader.getController();
                mTableController.initialize(mUserPreferences, mIconModel);
                mTableController.setItems(mChannelProcessingManager.getChannelMetadataModel().getItems());

                mTableController.getTableView().getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                    handleSelection(newSelection);
                });

                VBox.setVgrow(root, Priority.ALWAYS);
                getChildren().add(root);
            } catch (IOException e) {
                mLog.error("Error loading ChannelMetadataTable.fxml", e);
            }
        });
    }

    private void handleSelection(ChannelMetadata selectedMetadata) {
        ProcessingChain processingChain = null;
        if(selectedMetadata != null)
        {
            mUserSelectedChannel = mChannelProcessingManager.getChannelMetadataModel()
                .getChannelFromMetadata(selectedMetadata);

            processingChain = mChannelProcessingManager.getProcessingChain(mUserSelectedChannel);
        }

        mSelectedProcessingChainBroadcaster.broadcast(processingChain);
    }

    public void addProcessingChainSelectionListener(Listener<ProcessingChain> listener)
    {
        mSelectedProcessingChainBroadcaster.addListener(listener);
    }

    public void removeProcessingChainSelectionListener(Listener<ProcessingChain> listener)
    {
        mSelectedProcessingChainBroadcaster.removeListener(listener);
    }
}
