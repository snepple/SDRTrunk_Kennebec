package io.github.dsheirer.audio.broadcast;

import io.github.dsheirer.util.ThreadPool;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitors configured broadcast streams to ensure they are actively streaming.
 * If a stream stalls (e.g. queue fills up without successfully streaming) or disconnects,
 * the watchdog will attempt to restart it.
 */
public class StreamingWatchdog implements Runnable {
    private final static Logger mLog = LoggerFactory.getLogger(StreamingWatchdog.class);
    private BroadcastModel mBroadcastModel;
    private ScheduledFuture<?> mWatchdogFuture;
    private Map<ConfiguredBroadcast, Integer> mLastAgedOffCounts = new HashMap<>();

    public StreamingWatchdog(BroadcastModel broadcastModel) {
        mBroadcastModel = broadcastModel;
    }

    public void start() {
        if (mWatchdogFuture == null || mWatchdogFuture.isCancelled()) {
            mLog.info("Starting Streaming Watchdog (checks every 60 seconds)");
            mWatchdogFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(this, 60L, 60L, TimeUnit.SECONDS);
        }
    }

    public void stop() {
        if (mWatchdogFuture != null) {
            mLog.info("Stopping Streaming Watchdog");
            mWatchdogFuture.cancel(false);
            mWatchdogFuture = null;
        }
    }

    @Override
    public void run() {
        try {
            for (ConfiguredBroadcast configuredBroadcast : mBroadcastModel.getConfiguredBroadcasts()) {
                if (configuredBroadcast.getBroadcastConfiguration().isEnabled()) {
                    AbstractAudioBroadcaster<?> broadcaster = configuredBroadcast.getAudioBroadcaster();
                    if (broadcaster != null) {
                        checkStreamHealth(configuredBroadcast, broadcaster);
                    }
                } else {
                    // Remove disabled broadcasts from tracking map
                    mLastAgedOffCounts.remove(configuredBroadcast);
                }
            }
        } catch (Exception e) {
            mLog.error("Error in Streaming Watchdog execution", e);
        }
    }

    private void checkStreamHealth(ConfiguredBroadcast configuredBroadcast, AbstractAudioBroadcaster<?> broadcaster) {
        BroadcastState state = broadcaster.getBroadcastState();
        boolean needsRestart = false;
        String reason = "";

        // 1. Check for error or disconnected states
        if (state == BroadcastState.ERROR || state == BroadcastState.DISCONNECTED ||
            state == BroadcastState.NETWORK_UNAVAILABLE || state == BroadcastState.NO_SERVER ||
            state == BroadcastState.TEMPORARY_BROADCAST_ERROR) {
            
            needsRestart = true;
            reason = "Stream is in state: " + state.toString();
        }
        
        // 2. Check for stalled queue or dropped packets (only for connected streams)
        if (state == BroadcastState.CONNECTED) {
            int queueSize = 0;
            if (broadcaster instanceof AudioStreamingBroadcaster) {
                queueSize = ((AudioStreamingBroadcaster<?>) broadcaster).getAudioQueueSize();
            }
            
            int currentAgedOffCount = broadcaster.getAgedOffAudioCount();
            int lastAgedOffCount = mLastAgedOffCounts.getOrDefault(configuredBroadcast, currentAgedOffCount);
            
            if (queueSize >= 50) {
                needsRestart = true;
                reason = "Stream queue stalled (Queue size: " + queueSize + " >= 50)";
            } else if (currentAgedOffCount > lastAgedOffCount) {
                needsRestart = true;
                reason = "Stream is dropping packets (Aged off count increased by " + (currentAgedOffCount - lastAgedOffCount) + ")";
            }
            
            mLastAgedOffCounts.put(configuredBroadcast, currentAgedOffCount);
        }

        if (needsRestart) {
            String streamName = configuredBroadcast.getBroadcastConfiguration().getName();
            mLog.warn("Streaming Watchdog: Auto-repairing stream '{}'. Reason: {}", streamName, reason);
            restartStream(configuredBroadcast);
        }
    }

    private void restartStream(ConfiguredBroadcast configuredBroadcast) {
        // We use BroadcastModel CONFIGURATION_CHANGE event to cleanly delete and recreate the stream
        mBroadcastModel.process(new BroadcastEvent(configuredBroadcast.getBroadcastConfiguration(), BroadcastEvent.Event.CONFIGURATION_CHANGE));
    }
}
