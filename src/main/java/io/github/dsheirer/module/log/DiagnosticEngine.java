package io.github.dsheirer.module.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiagnosticEngine {
    private static final Logger mLog = LoggerFactory.getLogger(DiagnosticEngine.class);

    public static class InsightCard {
        public String title;
        public String description;
        public String remediation;

        public InsightCard(String title, String description, String remediation) {
            this.title = title;
            this.description = description;
            this.remediation = remediation;
        }
    }

    /**
     * Map a raw stack trace or error message to a semantic InsightCard.
     */
    public static InsightCard mapError(Throwable throwable, String contextMessage) {
        String message = throwable != null ? throwable.getMessage() : "";
        String fullError = contextMessage + " " + message;

        if (fullError.contains("JsonProcessingException") || fullError.contains("Unexpected character")) {
            return new InsightCard(
                "Configuration Corruption Detected",
                "The primary XML playlist file was unreadable.",
                "The Auto-Rollback service has been triggered. Please check playlist_corrupt.xml for details."
            );
        } else if (fullError.contains("TunerTimeoutException") || fullError.contains("USB transfer timeout")) {
            return new InsightCard(
                "SDR Hardware Timeout",
                "The tuner stopped sending IQ data. This is typically caused by USB bandwidth limitations or power saving modes.",
                "Try plugging the SDR directly into a USB 3.0 motherboard port and disabling USB Selective Suspend."
            );
        } else if (fullError.contains("SocketTimeoutException") || fullError.contains("Zello kicked")) {
            return new InsightCard(
                "Streaming Connection Lost",
                "The outbound WebSocket connection to the streaming server failed or timed out.",
                "Ensure your network allows outbound traffic on WSS. The system is entering exponential backoff."
            );
        } else {
            return new InsightCard(
                "Unknown System Error",
                "An unexpected runtime error occurred: " + message,
                "Please generate a Support Bundle and submit an issue on the issue tracker."
            );
        }
    }

    /**
     * Optional automated remediation hook (e.g. flush tuners if USB faults are detected).
     */
    public static void executeRemediation(InsightCard card, Runnable restartHook) {
        mLog.warn("Executing Remediation for: " + card.title);
        if ("SDR Hardware Timeout".equals(card.title)) {
            mLog.info("Remediation: Rebooting tuners...");
            if(restartHook != null) restartHook.run();
        }
    }
}
