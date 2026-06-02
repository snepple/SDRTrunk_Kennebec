package io.github.dsheirer.preference.notification;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AntiFloodFilter {
    private static final Logger mLog = LoggerFactory.getLogger(AntiFloodFilter.class);

    private final Map<String, Long> mLastAlertTime = new ConcurrentHashMap<>();
    private final Map<String, Integer> mReminderCount = new ConcurrentHashMap<>();
    private final Map<String, SystemAlert> mActiveFaults = new ConcurrentHashMap<>();
    private final ScheduledExecutorService mScheduler = Executors.newScheduledThreadPool(1);
    private final NotificationRouter mRouter;

    // Cooldown base
    private static final int MAX_REMINDERS = 3;

    private long getCooldown(int reminderCount) {
        if (reminderCount == 0) return 15 * 60 * 1000L;
        if (reminderCount == 1) return 30 * 60 * 1000L;
        return 60 * 60 * 1000L;
    }

    public AntiFloodFilter(NotificationRouter router) {
        this.mRouter = router;
        mScheduler.scheduleAtFixedRate(this::checkReminders, 1, 1, TimeUnit.MINUTES);
    }

    public boolean checkAndRecord(String signature) {
        long now = System.currentTimeMillis();
        Long lastTime = mLastAlertTime.get(signature);

        if (lastTime == null || (now - lastTime) >= getCooldown(mReminderCount.getOrDefault(signature, 0))) {
            mLastAlertTime.put(signature, now);
            mReminderCount.put(signature, 0);
            return true;
        } else {
            mLog.info("Suppressed duplicate alert for signature: {}", signature);
            return false;
        }
    }

    public void processAlert(SystemAlert alert) {
        long now = System.currentTimeMillis();
        String signature = alert.getFaultSignature();

        if (signature == null || signature.isEmpty()) {
            mRouter.dispatch(alert);
            return;
        }

        Long lastTime = mLastAlertTime.get(signature);

        if (lastTime == null || (now - lastTime) >= getCooldown(mReminderCount.getOrDefault(signature, 0))) {
            mLastAlertTime.put(signature, now);
            mReminderCount.put(signature, 0);
            mActiveFaults.put(signature, alert);
            mRouter.dispatch(alert);
        } else {
            mLog.info("Suppressed duplicate alert for signature: {}", signature);
        }
    }

    public void clearFault(String signature) {
        if (mActiveFaults.containsKey(signature)) {
            SystemAlert originalAlert = mActiveFaults.remove(signature);
            mLastAlertTime.remove(signature);
            mReminderCount.remove(signature);

            SystemAlert recoveryAlert = new SystemAlert(
                    originalAlert.getCategory(),
                    "System Recovered: " + originalAlert.getMessage(),
                    signature
            );
            mRouter.dispatch(recoveryAlert);
        }
    }

    private void checkReminders() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, SystemAlert> entry : mActiveFaults.entrySet()) {
            String signature = entry.getKey();
            Long lastTime = mLastAlertTime.get(signature);
            Integer count = mReminderCount.getOrDefault(signature, 0);

            if (lastTime != null && (now - lastTime) >= getCooldown(count) && count < MAX_REMINDERS) {
                SystemAlert originalAlert = entry.getValue();
                SystemAlert reminderAlert = new SystemAlert(
                        originalAlert.getCategory(),
                        "Reminder (" + (count + 1) + "/" + MAX_REMINDERS + "): " + originalAlert.getMessage(),
                        signature
                );
                mRouter.dispatch(reminderAlert);

                mLastAlertTime.put(signature, now);
                mReminderCount.put(signature, count + 1);
            }
        }
    }

    public void shutdown() {
        mScheduler.shutdown();
    }
}
