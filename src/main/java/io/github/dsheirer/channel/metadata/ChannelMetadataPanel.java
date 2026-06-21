package io.github.dsheirer.channel.metadata;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Circle;
import javafx.geometry.*;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.source.tuner.ui.DiscoveredTunerModel;
import io.github.dsheirer.settings.SettingsManager;
import io.github.dsheirer.preference.NowPlayingPreference;
import io.github.dsheirer.module.ProcessingChain;
import io.github.dsheirer.controller.channel.ChannelProcessingManager;
import io.github.dsheirer.channel.state.State;


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
    private static final org.slf4j.Logger mLog = org.slf4j.LoggerFactory.getLogger(ChannelMetadataPanel.class);
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
        //Render a colored status indicator next to the state text (green = active/call, amber = fade,
        //red = encrypted, grey = idle) so activity is recognizable at a glance.
        stateCol.setCellFactory(col -> new TableCell<ChannelMetadata, String>() {
            private final Circle indicator = new Circle(5);
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if(empty || item == null || item.isEmpty()) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    indicator.setFill(stateColor(item));
                    setGraphic(indicator);
                    setGraphicTextGap(6);
                }
            }
        });
        
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

        //Tuner the channel is playing through (friendly name when set).  Resolved via the channel's
        //activeTunerName; kept current by the 500ms refresh in setupActivityPolling().
        TableColumn<ChannelMetadata, String> tunerCol = new TableColumn<>("Tuner");
        tunerCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(resolveTunerName(cellData.getValue())));
        tunerCol.setPrefWidth(150);

        //Activity column removed (low-value sparkline). The idle->active polling that powers the Received
        //count is retained in setupActivityPolling().

        mTable.getColumns().addAll(stateCol, channelCol, freqCol, receivedCol, toCol, fromCol, tunerCol);
        mTable.setTableMenuButtonVisible(true);

        setupRowContextMenu();

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
                    boolean currentlyVisible = np.isDetailsPaneVisible();
                    np.toggleDetailsPane();
                    toggleDetailsBtn.setText(currentlyVisible ? "\u25B6 Show Details" : "\u25C0 Hide Details");
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

    /**
     * Resolves the friendly tuner name a channel is currently playing through, or "" if not running.
     */
    private String resolveTunerName(ChannelMetadata metadata)
    {
        try
        {
            if(mChannelProcessingManager != null && mChannelProcessingManager.getChannelMetadataModel() != null)
            {
                io.github.dsheirer.controller.channel.Channel channel =
                    mChannelProcessingManager.getChannelMetadataModel().getChannelFromMetadata(metadata);

                if(channel != null && channel.activeTunerNameProperty().get() != null)
                {
                    return channel.activeTunerNameProperty().get();
                }
            }
        }
        catch(Throwable t)
        {
            //fall through to empty
        }

        return "";
    }

    /**
     * Indicator color for a channel state display value.  Active / call-producing states are green,
     * fade is amber, encrypted is red, and idle (or anything else) is grey.
     */
    private static Color stateColor(String state)
    {
        if(state == null)
        {
            return Color.web("#9e9e9e");
        }

        switch(state.trim().toUpperCase())
        {
            case "CALL":
            case "ACTIVE":
            case "CONTROL":
            case "DATA":
                return Color.web("#2ecc40"); //green
            case "ENCRYPTED":
                return Color.web("#ff4136"); //red
            case "FADE":
                return Color.web("#ff851b"); //amber
            default:
                return Color.web("#9e9e9e"); //grey (IDLE, TEARDOWN, RESET, unknown)
        }
    }

    /**
     * Adds a right-click context menu to the channels table for managing the playing channel.
     */
    private void setupRowContextMenu()
    {
        mTable.setRowFactory(tv -> {
            javafx.scene.control.TableRow<ChannelMetadata> row = new javafx.scene.control.TableRow<>();
            javafx.scene.control.ContextMenu menu = new javafx.scene.control.ContextMenu();

            javafx.scene.control.MenuItem stopItem = new javafx.scene.control.MenuItem("Stop Channel");
            stopItem.setOnAction(e -> stopChannelAsync(channelForRow(row)));

            javafx.scene.control.MenuItem restartItem = new javafx.scene.control.MenuItem("Restart Channel");
            restartItem.setOnAction(e -> restartChannelAsync(channelForRow(row)));

            menu.getItems().addAll(stopItem, restartItem);

            //Only show the menu on non-empty rows.
            row.contextMenuProperty().bind(javafx.beans.binding.Bindings.when(row.emptyProperty())
                    .then((javafx.scene.control.ContextMenu) null).otherwise(menu));

            return row;
        });
    }

    private io.github.dsheirer.controller.channel.Channel channelForRow(javafx.scene.control.TableRow<ChannelMetadata> row)
    {
        if(row == null || row.getItem() == null || mChannelProcessingManager == null ||
                mChannelProcessingManager.getChannelMetadataModel() == null)
        {
            return null;
        }

        return mChannelProcessingManager.getChannelMetadataModel().getChannelFromMetadata(row.getItem());
    }

    private void stopChannelAsync(io.github.dsheirer.controller.channel.Channel channel)
    {
        if(channel == null)
        {
            return;
        }

        io.github.dsheirer.util.ThreadPool.CACHED.submit(() -> {
            try { mChannelProcessingManager.stop(channel); }
            catch(Exception ex) { mLog.error("Error stopping channel [" + channel.getName() + "]", ex); }
        });
    }

    private void restartChannelAsync(io.github.dsheirer.controller.channel.Channel channel)
    {
        if(channel == null)
        {
            return;
        }

        io.github.dsheirer.util.ThreadPool.CACHED.submit(() -> {
            try { mChannelProcessingManager.stop(channel); mChannelProcessingManager.start(channel); }
            catch(Exception ex) { mLog.error("Error restarting channel [" + channel.getName() + "]", ex); }
        });
    }

    private void setupActivityPolling() {
        mPollingTimeline = new Timeline(new KeyFrame(Duration.millis(500), e -> {
            boolean updated = false;
            for (ChannelMetadata meta : mTable.getItems()) {
                List<Boolean> history = mActivityHistory.computeIfAbsent(meta, k -> new ArrayList<>());
                //A channel that is producing audio reports state CALL (not ACTIVE) - notably NBFM/analog
                //channels only ever use CALL/IDLE/FADE.  Count any of the active-producing states so the
                //Received tally reflects real traffic on conventional (NBFM) channels, not just trunked ACTIVE.
                State state = meta.hasDecoderStateIdentifier() ? meta.getChannelStateIdentifier().getValue() : null;
                boolean isActive = state != null && State.SINGLE_CHANNEL_ACTIVE_STATES.contains(state);
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
