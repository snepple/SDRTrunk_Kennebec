package io.github.dsheirer.gui.theme;

import javafx.application.Platform;
import javafx.scene.Scene;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Locale;

public class ThemeManager {
    private static final Logger mLog = LoggerFactory.getLogger(ThemeManager.class);
    private static final String REGISTRY_PATH = "Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize";
    private static final String REGISTRY_KEY = "AppsUseLightTheme";
    private static final String NIGHT_MODE_CSS = "/css/night-mode.css";
    
    private static boolean mNightModeEnabled = false;
    private static final java.util.Set<Scene> mActiveScenes = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());
    private boolean mIsWindows11;
    private Thread mRegistryMonitorThread;

    public ThemeManager() {
        String osName = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        mIsWindows11 = osName.contains("windows 11") || (osName.contains("windows 10") && getWindowsBuildNumber() >= 22000);
        
        applyTheme();
        
        if (isWindows()) {
            startRegistryMonitor();
        }
    }

    private int getWindowsBuildNumber() {
        try {
            if (isWindows()) {
                String build = Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, 
                    "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion", "CurrentBuild");
                return Integer.parseInt(build);
            }
        } catch (Exception e) {
            mLog.error("Error getting Windows build number", e);
        }
        return 0;
    }

    public boolean isWindows11() {
        return mIsWindows11;
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("windows");
    }

    private void applyTheme() {
        try {
            boolean useLightTheme = true;
            if (isWindows()) {
                try {
                    int value = Advapi32Util.registryGetIntValue(WinReg.HKEY_CURRENT_USER, REGISTRY_PATH, REGISTRY_KEY);
                    useLightTheme = (value == 1);
                } catch (Exception e) {
                    mLog.debug("Could not read registry for theme, defaulting to light", e);
                }
            }

            if (useLightTheme) {
                // Application.setUserAgentStylesheet(new Modena().getUserAgentStylesheet());
            } else {
                // Application.setUserAgentStylesheet(new Caspian().getUserAgentStylesheet());
            }
            
        } catch (Exception e) {
            mLog.error("Failed to initialize JavaFX theme", e);
        }
    }

    private void startRegistryMonitor() {
        mRegistryMonitorThread = new Thread(() -> {
            try {
                int lastValue = Advapi32Util.registryGetIntValue(WinReg.HKEY_CURRENT_USER, REGISTRY_PATH, REGISTRY_KEY);
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        int currentValue = Advapi32Util.registryGetIntValue(WinReg.HKEY_CURRENT_USER, REGISTRY_PATH, REGISTRY_KEY);
                        if (currentValue != lastValue) {
                            lastValue = currentValue;
                            Platform.runLater(this::applyTheme);
                        }
                    } catch (Exception ex) {
                    }
                    Thread.sleep(2000); // Poll every 2 seconds
                }
            } catch (Exception e) {
                mLog.error("Registry monitor thread failed", e);
            }
        });
        mRegistryMonitorThread.setDaemon(true);
        mRegistryMonitorThread.start();
    }

    public static void registerScene(Scene scene) {
        if (scene != null) {
            mActiveScenes.add(scene);
            String cssUrl = ThemeManager.class.getResource(NIGHT_MODE_CSS) != null
                ? ThemeManager.class.getResource(NIGHT_MODE_CSS).toExternalForm()
                : NIGHT_MODE_CSS;
            if (mNightModeEnabled && !scene.getStylesheets().contains(cssUrl)) {
                scene.getStylesheets().add(cssUrl);
            } else if (!mNightModeEnabled && scene.getStylesheets().contains(cssUrl)) {
                scene.getStylesheets().remove(cssUrl);
            }
        }
    }

    /**
     * Toggles night mode on/off for all registered scenes by adding or removing the night-mode.css stylesheet.
     */
    public static void toggleNightMode() {
        String cssUrl = ThemeManager.class.getResource(NIGHT_MODE_CSS) != null
            ? ThemeManager.class.getResource(NIGHT_MODE_CSS).toExternalForm()
            : NIGHT_MODE_CSS;

        mNightModeEnabled = !mNightModeEnabled;
        
        for (Scene scene : mActiveScenes) {
            if (scene != null) {
                if (mNightModeEnabled) {
                    if (!scene.getStylesheets().contains(cssUrl)) {
                        scene.getStylesheets().add(cssUrl);
                    }
                } else {
                    scene.getStylesheets().remove(cssUrl);
                }
            }
        }
        mLog.info("Night mode " + (mNightModeEnabled ? "enabled" : "disabled"));
    }

    /**
     * @return true if night mode is currently enabled
     */
    public static boolean isNightModeEnabled() {
        return mNightModeEnabled;
    }

    /**
     * Applies the current theme (including night mode if enabled) to a DialogPane.
     * Call this after creating an Alert or Dialog to ensure it matches the app theme.
     */
    public static void applyCurrentTheme(javafx.scene.control.DialogPane dialogPane) {
        if (dialogPane == null) return;
        String mainCss = ThemeManager.class.getResource("/sdrtrunk_style.css") != null
            ? ThemeManager.class.getResource("/sdrtrunk_style.css").toExternalForm() : null;
        if (mainCss != null && !dialogPane.getStylesheets().contains(mainCss)) {
            dialogPane.getStylesheets().add(mainCss);
        }
        if (mNightModeEnabled) {
            String nightCss = ThemeManager.class.getResource(NIGHT_MODE_CSS) != null
                ? ThemeManager.class.getResource(NIGHT_MODE_CSS).toExternalForm() : null;
            if (nightCss != null && !dialogPane.getStylesheets().contains(nightCss)) {
                dialogPane.getStylesheets().add(nightCss);
            }
        }
    }
}
