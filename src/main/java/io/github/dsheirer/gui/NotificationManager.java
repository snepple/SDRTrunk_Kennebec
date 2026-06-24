package io.github.dsheirer.gui;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.util.Duration;
import java.awt.TrayIcon;
import org.controlsfx.control.Notifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Surfaces system/health notifications to the user.  These are shown as non-intrusive, auto-dismissing toast
 * notifications in the corner of the screen (ControlsFX {@link Notifications}) rather than modal dialogs, so a
 * recurring condition can't stack up blocking dialogs that the user must dismiss one by one.
 */
public class NotificationManager {
    private static final Logger mLog = LoggerFactory.getLogger(NotificationManager.class);

    /** How long a notification toast stays on screen before it fades out on its own. */
    private static final Duration TOAST_DURATION = Duration.seconds(8);

    private static NotificationManager instance;

    private SystemTrayManager mSystemTrayManager;

    private NotificationManager() {
    }

    public static synchronized NotificationManager getInstance() {
        if (instance == null) {
            instance = new NotificationManager();
        }
        return instance;
    }

    public void setSystemTrayManager(SystemTrayManager manager) {
        this.mSystemTrayManager = manager;
    }

    /**
     * Shows a non-intrusive, self-dismissing toast notification.  Runs on the JavaFX application thread and never
     * blocks; multiple notifications stack briefly and fade out rather than requiring the user to acknowledge each.
     *
     * @param title short headline.
     * @param message body text.
     * @param messageType severity, mapped to the toast's icon/styling.
     */
    public void showNotification(String title, String message, TrayIcon.MessageType messageType) {
        Platform.runLater(() -> {
            try {
                Notifications toast = Notifications.create()
                        .title(title)
                        .text(message)
                        .hideAfter(TOAST_DURATION)
                        .position(Pos.BOTTOM_RIGHT);

                switch (messageType) {
                    case ERROR -> toast.showError();
                    case WARNING -> toast.showWarning();
                    default -> toast.showInformation();
                }
            } catch (Exception e) {
                //Fall back to logging if the toast can't be shown (e.g. no JavaFX screen available yet).
                mLog.warn("Unable to display notification toast [{}]: {} - {}", messageType, title, message);
            }
        });
    }
}
