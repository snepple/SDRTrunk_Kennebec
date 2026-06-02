

package io.github.dsheirer.gui;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;

import javafx.scene.control.Button;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.application.Platform;
import java.util.Optional;
import java.awt.*;

public class NotificationManager {
    private static final Logger mLog = LoggerFactory.getLogger(NotificationManager.class);
    private static NotificationManager instance;

    private boolean useSystemTray = false;
    private TrayIcon trayIcon;

    private NotificationManager() {
        if (SystemTray.isSupported()) {
            try {
                SystemTray tray = SystemTray.getSystemTray();
                // We need a dummy image for the tray icon
                java.awt.Image image = java.awt.Toolkit.getDefaultToolkit().createImage(new byte[0]); 
                trayIcon = new TrayIcon(image, "SDRTrunk");
                trayIcon.setImageAutoSize(true);
                tray.add(trayIcon);
                useSystemTray = true;
                mLog.info("SystemTray initialized for native notifications.");
            } catch (AWTException e) {
                mLog.error("SystemTray is supported, but could not be added.", e);
            }
        } else {
            mLog.info("SystemTray is not supported on this platform. Falling back to Swing popups.");
        }
    }

    public static synchronized NotificationManager getInstance() {
        if (instance == null) {
            instance = new NotificationManager();
        }
        return instance;
    }

    public void showNotification(String title, String message, TrayIcon.MessageType messageType) {
        if (useSystemTray && trayIcon != null) {
            trayIcon.displayMessage(title, message, messageType);
        } else {
            // Fallback to legacy Swing popup
            Alert.AlertType alertType = Alert.AlertType.INFORMATION;
            if (messageType == TrayIcon.MessageType.ERROR) {
                alertType = Alert.AlertType.ERROR;
            } else if (messageType == TrayIcon.MessageType.WARNING) {
                alertType = Alert.AlertType.WARNING;
            }

            final Alert.AlertType finalAlertType = alertType;
            Platform.runLater(() -> {
                Alert alert = new Alert(finalAlertType);
                alert.setTitle(title);
                alert.setContentText(message);
                alert.showAndWait();
            });
        }
    }
}
