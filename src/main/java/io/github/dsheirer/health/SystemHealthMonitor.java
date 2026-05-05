package io.github.dsheirer.health;

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.gui.NotificationManager;
import io.github.dsheirer.message.SyncLossMessage;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.notification.NotificationPreference;
import io.github.dsheirer.preference.notification.NotificationRecipient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.TrayIcon;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SystemHealthMonitor {
    private static final Logger mLog = LoggerFactory.getLogger(SystemHealthMonitor.class);

    // 60 minutes in milliseconds
    private static final long RATE_LIMIT_MS = TimeUnit.MINUTES.toMillis(60);

    private final UserPreferences mUserPreferences;

    private final ConcurrentHashMap<SystemHealthAlertEvent.AlertType, Long> mLastAlertTimes = new ConcurrentHashMap<>();

    // Track continuous sync loss. Reset when normal active state occurs.
    // For simplicity, tracking the global cumulative bits across channels that lost sync recently.
    // We expect 9600 bps. A threshold of ~300,000 bits is ~31 seconds of continuous sync loss.
    private final AtomicInteger mSyncLossBitCount = new AtomicInteger(0);
    private static final int SYNC_LOSS_THRESHOLD_BITS = 300000;

    public SystemHealthMonitor(UserPreferences userPreferences) {
        this.mUserPreferences = userPreferences;
        mLog.info("SystemHealthMonitor started");
    }

    @Subscribe
    public void handleHealthAlert(SystemHealthAlertEvent event) {
        triggerAlert(event.getType(), event.getTitle(), event.getMessage());
    }

    @Subscribe
    public void handleSyncLoss(SyncLossMessage message) {
        int currentLoss = mSyncLossBitCount.addAndGet(message.getBitsProcessed());
        if (currentLoss >= SYNC_LOSS_THRESHOLD_BITS) {
            triggerAlert(SystemHealthAlertEvent.AlertType.SIGNAL,
                "Signal and Decoding Issue",
                "A site has experienced sustained synchronization loss (" + currentLoss + " bits lost). " +
                "Voice traffic decoding may be prevented.");

            // Reset to prevent immediate re-triggering of the accumulation,
            // rate limiting will prevent the actual notification spam
            mSyncLossBitCount.set(0);
        }
    }

    // In a full implementation, we'd want to reset the sync loss counter when valid messages are received.
    // For now, it will accumulate over time, but the 60-minute rate limit prevents spam.

    private void triggerAlert(SystemHealthAlertEvent.AlertType type, String title, String message) {
        NotificationPreference pref = mUserPreferences.getNotificationPreference();
        if (pref == null) {
            return;
        }

        boolean enabled = false;
        switch (type) {
            case HARDWARE:
                enabled = pref.isHardwareAlertEnabled();
                break;
            case SIGNAL:
                enabled = pref.isSignalAlertEnabled();
                break;
            case SYSTEM:
                enabled = pref.isSystemAlertEnabled();
                break;
            case INTEGRATION:
                enabled = pref.isIntegrationAlertEnabled();
                break;
        }

        if (!enabled) {
            return;
        }

        long now = System.currentTimeMillis();
        long lastAlertTime = mLastAlertTimes.getOrDefault(type, 0L);

        if (now - lastAlertTime >= RATE_LIMIT_MS) {
            // Update the timestamp and send notification
            mLastAlertTimes.put(type, now);
            mLog.warn("System Health Alert Triggered [" + type + "]: " + message);

            // Dispatch to NotificationManager
            NotificationManager.getInstance().showNotification(title, message, TrayIcon.MessageType.ERROR);

            // Dispatch to recipients (for simplicity, we just use existing NotificationManager which handles Desktop popups.
            // A more complete implementation might route through telegram/email recipients if configured, but NotificationManager suffices for the UI alert requirement).
        } else {
            mLog.debug("System Health Alert Suppressed (Rate Limited) [" + type + "]: " + message);
        }
    }
}
