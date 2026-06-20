package io.github.dsheirer.gui;

import io.github.dsheirer.gui.theme.ThemeManager;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Image;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.InputStream;
import javax.imageio.ImageIO;

/**
 * Manages the system tray icon that lets SDRTrunk keep running as a background service when the main window is
 * minimized.  Double-clicking the icon restores the window, and a right-click shows a themed JavaFX context menu
 * with quick actions.  A JavaFX context menu is used (rather than AWT's native PopupMenu) because the native menu
 * can't be styled to match the application's light/dark theme.
 */
public class SystemTrayManager {
    private static final Logger mLog = LoggerFactory.getLogger(SystemTrayManager.class);
    private static final String MAIN_CSS = "/sdrtrunk_style.css";
    private static final String NIGHT_CSS = "/css/night-mode.css";

    private TrayIcon mTrayIcon;
    private final Stage mPrimaryStage;
    private final SDRTrunk mApp;
    private boolean mAvailable = false;
    private Stage mAnchorStage;

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
            // Keep the JavaFX runtime alive when the main window is hidden to the tray.
            Platform.setImplicitExit(false);

            SystemTray tray = SystemTray.getSystemTray();

            // ImageIO.read preserves RGBA alpha; Toolkit.getImage() does not
            Image image;
            try (InputStream is = getClass().getResourceAsStream("/images/SDRTrunk_Application_Icon.png")) {
                if (is == null) {
                    mLog.warn("Could not find system tray icon.");
                    return;
                }
                image = ImageIO.read(is);
            }

            mTrayIcon = new TrayIcon(image, "SDRTrunk");
            mTrayIcon.setImageAutoSize(true);

            // Primary action (double-click on most platforms) restores the window.
            mTrayIcon.addActionListener(e -> mApp.showMainWindow());

            // Show the themed JavaFX context menu on the platform's popup trigger (right-click).
            mTrayIcon.addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) { maybeShowMenu(e); }
                @Override public void mouseReleased(MouseEvent e) { maybeShowMenu(e); }
            });

            tray.add(mTrayIcon);

            // Pre-create the invisible owner window the context menu needs so it's ready on first right-click.
            getAnchorStage();

            mAvailable = true;
            mLog.info("System tray icon installed");
        } catch (Exception e) {
            mLog.error("Error initializing system tray", e);
        }
    }

    private void maybeShowMenu(MouseEvent e) {
        if (e.isPopupTrigger()) {
            final int x = e.getXOnScreen();
            final int y = e.getYOnScreen();
            Platform.runLater(() -> showContextMenu(x, y));
        }
    }

    /**
     * Lazily creates (and keeps showing) a tiny, transparent, off-screen window to act as the owner for the tray
     * context menu.  A JavaFX popup requires a showing owner window, and the main window may be hidden in the tray.
     */
    private Stage getAnchorStage() {
        if (mAnchorStage == null) {
            mAnchorStage = new Stage();
            mAnchorStage.initStyle(StageStyle.TRANSPARENT);
            mAnchorStage.setScene(new Scene(new Pane(), 1, 1, Color.TRANSPARENT));
            mAnchorStage.setOpacity(0);
            mAnchorStage.setWidth(1);
            mAnchorStage.setHeight(1);
            mAnchorStage.setX(-30000);
            mAnchorStage.setY(-30000);
        }
        if (!mAnchorStage.isShowing()) {
            mAnchorStage.show();
        }
        return mAnchorStage;
    }

    private void showContextMenu(double screenX, double screenY) {
        try {
            Stage anchor = getAnchorStage();
            ContextMenu menu = buildContextMenu();
            menu.show(anchor, screenX, screenY);
            applyTheme(menu);
        } catch (Exception ex) {
            mLog.error("Error showing system tray context menu", ex);
        }
    }

    private ContextMenu buildContextMenu() {
        ContextMenu menu = new ContextMenu();

        MenuItem open = new MenuItem("Open SDRTrunk");
        open.setOnAction(e -> mApp.showMainWindow());

        MenuItem mute = new MenuItem(mApp.isAudioMuted() ? "Unmute Audio" : "Mute Audio");
        mute.setOnAction(e -> mApp.toggleAudioMute());

        MenuItem stopAll = new MenuItem("Stop All Channels");
        stopAll.setOnAction(e -> mApp.stopAllChannels());

        MenuItem restart = new MenuItem("Restart Autoplay Channels");
        restart.setOnAction(e -> mApp.restartAutoplayChannels());

        MenuItem exit = new MenuItem("Exit SDRTrunk");
        exit.setOnAction(e -> mApp.confirmAndExit());

        menu.getItems().addAll(open, new SeparatorMenuItem(), mute, stopAll, restart,
            new SeparatorMenuItem(), exit);

        return menu;
    }

    /**
     * Applies the application's light/dark stylesheets to the context menu so it matches the current theme.
     */
    private void applyTheme(ContextMenu menu) {
        if (menu.getScene() == null) {
            return;
        }

        menu.getScene().getStylesheets().clear();

        String mainCss = resource(MAIN_CSS);
        if (mainCss != null) {
            menu.getScene().getStylesheets().add(mainCss);
        }

        if (ThemeManager.isNightModeEnabled()) {
            String nightCss = resource(NIGHT_CSS);
            if (nightCss != null) {
                menu.getScene().getStylesheets().add(nightCss);
            }
        }
    }

    private String resource(String path) {
        return getClass().getResource(path) != null ? getClass().getResource(path).toExternalForm() : null;
    }

    /**
     * @return true if the system tray icon was installed successfully and is available for use.
     */
    public boolean isAvailable() {
        return mAvailable;
    }

    /**
     * Shows a balloon notification letting the user know the application is still running after being minimized to
     * the system tray.
     */
    public void notifyMinimizedToTray() {
        pushNotification("SDRTrunk is still running",
            "SDRTrunk minimized to the system tray. Double-click the tray icon to restore it.",
            TrayIcon.MessageType.INFO);
    }

    public void pushNotification(String title, String message, java.awt.TrayIcon.MessageType type) {
        if (mTrayIcon != null) {
            mTrayIcon.displayMessage(title, message, type);
        }
    }
}
