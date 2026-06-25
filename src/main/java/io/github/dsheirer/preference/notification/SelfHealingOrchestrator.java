package io.github.dsheirer.preference.notification;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SelfHealingOrchestrator {
    private static final Logger mLog = LoggerFactory.getLogger(SelfHealingOrchestrator.class);

    private final AntiFloodFilter mFilter;
    private final Map<String, Integer> mRetryCounts = new ConcurrentHashMap<>();
    private static final int MAX_RETRIES = 3;

    public SelfHealingOrchestrator(AntiFloodFilter filter) {
        this.mFilter = filter;
    }

    public void intercept(SystemAlert alert) {
        String signature = alert.getFaultSignature();
        if (signature == null || signature.isEmpty()) {
            mFilter.processAlert(alert);
            return;
        }

        int retries = mRetryCounts.getOrDefault(signature, 0);

        if (alert.getCategory() == AlertCategory.SYSTEM && alert.getMessage().contains("Audio Pipeline Fault")) {
            if (retries < MAX_RETRIES) {
                mLog.info("Attempting automated JMBE flush for fault: {} (Attempt {}/{})", signature, retries + 1, MAX_RETRIES);
                performJMBEFlush();
                mRetryCounts.put(signature, retries + 1);
                return; // Suppress alert, we are healing
            }
        } else if (alert.getCategory() == AlertCategory.HARDWARE) {
            //Do NOT attempt a soft reset for antenna disconnection events — a reset cannot fix a
            //physical disconnection and may disrupt other channels sharing the same tuner.
            if (alert.getMessage() != null && alert.getMessage().contains("antenna")) {
                mLog.info("Antenna disconnection detected for fault: {} — skipping soft-reset (not recoverable by reset)", signature);
                mFilter.processAlert(alert);
                return;
            }

            if (retries < MAX_RETRIES) {
                mLog.info("Attempting automated tuner soft-reset for fault: {} (Attempt {}/{})", signature, retries + 1, MAX_RETRIES);
                performTunerSoftReset();
                mRetryCounts.put(signature, retries + 1);
                return; // Suppress alert, we are healing
            }
        }

        // If not healed or max retries reached, pass to filter
        if (retries >= MAX_RETRIES) {
            mLog.warn("Max retries reached for fault: {}, escalating.", signature);
        }
        mFilter.processAlert(alert);
    }

    public void notifyRecovered(String signature) {
        mRetryCounts.remove(signature);
        mFilter.clearFault(signature);
    }

    private void performJMBEFlush() {
        mLog.info("Executing JMBE flush...");
        // Integration point for JMBE flush logic
    }

    private void performTunerSoftReset() {
        mLog.info("Executing Tuner soft-reset...");
        // Integration point for Tuner soft reset logic
    }
}
