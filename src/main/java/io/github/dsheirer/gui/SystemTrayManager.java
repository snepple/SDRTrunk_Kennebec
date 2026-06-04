package io.github.dsheirer.gui;

import javafx.application.Platform;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.net.URL;

public class SystemTrayManager {
    private static final Logger mLog = LoggerFactory.getLogger(SystemTrayManager.class);
    private TrayIcon mTrayIcon;
    private final Stage mPrimaryStage;
    private final SDRTrunk mApp;

    public SystemTrayManager(Stage primaryStage, SDRTrunk app) {
        this.mPrimaryStage = primaryStage;
        this.mApp = app;
        initSystemTray();
    }

    private void initSystemTray() {
        if (!SystemTray.isSupported()) {
            mLog.warn("SystemTray is not supported on this platform.");
            return;
        }

        try {
            // Ensure JavaFX doesn't close completely when main window is hidden
            Platform.setImplicitExit(false);

            SystemTray tray = SystemTray.getSystemTray();
            
            URL iconURL = getClass().getResource("/images/SDRTrunk_Application_Icon.png");
            if (iconURL == null) {
                mLog.warn("Could not find system tray icon.");
                return;
            }
            
            Image image = Toolkit.getDefaultToolkit().getImage(iconURL);
            
            PopupMenu popup = new PopupMenu();
            MenuItem showItem = new MenuItem("Show SDRTrunk");
            showItem.addActionListener(e -> Platform.runLater(() -> {
                if (mPrimaryStage != null) {
                    mPrimaryStage.show();
                    mPrimaryStage.toFront();
                }
            }));
            
            MenuItem exitItem = new MenuItem("Exit");
            exitItem.addActionListener(e -> {
                Platform.runLater(() -> {
                    mApp.onItemSelected("exit");
                });
            });
            
            popup.add(showItem);
            popup.addSeparator();
            popup.add(exitItem);

            mTrayIcon = new TrayIcon(image, "SDRTrunk", popup);
            mTrayIcon.setImageAutoSize(true);
            
            mTrayIcon.addActionListener(e -> Platform.runLater(() -> {
                if (mPrimaryStage != null) {
                    mPrimaryStage.show();
                    mPrimaryStage.toFront();
                }
            }));

            tray.add(mTrayIcon);
        } catch (Exception e) {
            mLog.error("Error initializing system tray", e);
        }
    }

    public void pushNotification(String title, String message, java.awt.TrayIcon.MessageType type) {
        if (mTrayIcon != null) {
            mTrayIcon.displayMessage(title, message, type);
        }
    }
}
