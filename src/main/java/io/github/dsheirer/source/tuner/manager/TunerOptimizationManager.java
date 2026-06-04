package io.github.dsheirer.source.tuner.manager;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TunerOptimizationManager {
    private static final Logger mLog = LoggerFactory.getLogger(TunerOptimizationManager.class);
    
    private final PlaylistManager mPlaylistManager;
    private final TunerManager mTunerManager;
    
    public TunerOptimizationManager(PlaylistManager playlistManager, TunerManager tunerManager) {
        this.mPlaylistManager = playlistManager;
        this.mTunerManager = tunerManager;
    }
    
    /**
     * Scans the PlaylistManager for active channels.
     * For each active tuner, find the minimum and maximum frequencies of assigned channels.
     * Calculates the optimal 'tightest' center frequency as (max_freq + min_freq) / 2
     * and applies it to the tuner dynamically, minimizing required bandwidth.
     */
    public void optimizeActiveTuners() {
        Set<String> activeTunerNames = new HashSet<>();
        
        // Scan the PlaylistManager for active channels
        for (Channel channel : mPlaylistManager.getChannelModel().getChannels()) {
            if (mPlaylistManager.getChannelProcessingManager().isProcessing(channel)) {
                String tunerName = channel.activeTunerNameProperty().get();
                if (tunerName != null && !tunerName.isEmpty()) {
                    activeTunerNames.add(tunerName);
                }
            }
        }
        
        for (DiscoveredTuner discoveredTuner : mTunerManager.getDiscoveredTunerModel().getAvailableTuners()) {
            if (discoveredTuner == null || !discoveredTuner.hasTuner()) {
                continue;
            }
            
            Tuner tuner = discoveredTuner.getTuner();
            
            // Only optimize if this tuner is running active channels
            if (!activeTunerNames.contains(tuner.getPreferredName())) {
                continue;
            }
            
            ChannelSourceManager csm = tuner.getChannelSourceManager();
            if (csm == null) {
                continue;
            }
            
            SortedSet<TunerChannel> channels = csm.getTunerChannels();
            if (channels == null || channels.isEmpty()) {
                continue;
            }
            
            long minFreq = Long.MAX_VALUE;
            long maxFreq = Long.MIN_VALUE;
            
            for (TunerChannel tc : channels) {
                minFreq = Math.min(minFreq, tc.getMinFrequency());
                maxFreq = Math.max(maxFreq, tc.getMaxFrequency());
            }
            
            long requiredBandwidth = maxFreq - minFreq;
            long optimalCenterFreq = minFreq + (requiredBandwidth / 2);
            
            TunerController controller = tuner.getTunerController();
            int optimalBandwidth = controller.getUsableBandwidth();
            
            if (requiredBandwidth > optimalBandwidth) {
                Platform.runLater(() -> promptUserForReassignment(tuner, requiredBandwidth, optimalBandwidth));
            } else {
                try {
                    controller.setFrequency(optimalCenterFreq);
                    mLog.info("Optimized center frequency for tuner " + tuner.getPreferredName() + " to " + optimalCenterFreq);
                } catch (SourceException e) {
                    mLog.error("Error setting optimal center frequency for tuner " + tuner.getPreferredName(), e);
                }
            }
        }
    }
    
    private void promptUserForReassignment(Tuner tuner, long requiredBandwidth, int optimalBandwidth) {
        Alert alert = new Alert(AlertType.WARNING); io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane());
        alert.setTitle("Tuner Bandwidth Exceeded");
        alert.setHeaderText("The required bandwidth for tuner " + tuner.getPreferredName() + " exceeds optimal bandwidth.");
        alert.setContentText(String.format("Required: %d Hz\nOptimal: %d Hz\n\nWould you like to Reassign outlier channels or move them to a Standby pool instead of forcing a CPU-heavy wide sample rate?", requiredBandwidth, optimalBandwidth));
        
        ButtonType reassignBtn = new ButtonType("Reassign");
        ButtonType standbyBtn = new ButtonType("Standby");
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        alert.getButtonTypes().setAll(reassignBtn, standbyBtn, cancelBtn);
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == reassignBtn) {
                mLog.info("User selected to Reassign outlier channels for tuner " + tuner.getPreferredName());
                // Logic to reassign channels would go here
            } else if (result.get() == standbyBtn) {
                mLog.info("User selected to move outlier channels to Standby for tuner " + tuner.getPreferredName());
                // Logic to move to standby pool would go here
            }
        }
    }
}
