package io.github.dsheirer.gui;

import javafx.scene.control.Alert;
import javafx.application.Platform;
import java.awt.TrayIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationManager {
    private static final Logger mLog = LoggerFactory.getLogger(NotificationManager.class);
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

    public void showNotification(String title, String message, TrayIcon.MessageType messageType) {
        if (mSystemTrayManager != null) {
            mSystemTrayManager.pushNotification(title, message, messageType);
        } else {
            // Fallback to JavaFX Alert
            Alert.AlertType alertType = Alert.AlertType.INFORMATION;
            if (messageType == TrayIcon.MessageType.ERROR) {
                alertType = Alert.AlertType.ERROR;
            } else if (messageType == TrayIcon.MessageType.WARNING) {
                alertType = Alert.AlertType.WARNING;
            }

            final Alert.AlertType finalAlertType = alertType;
            Platform.runLater(() -> {
                Alert alert = new Alert(finalAlertType); io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane());
                alert.setTitle(title);
                alert.setContentText(message);
                alert.showAndWait();
            });
        }
    }
}
