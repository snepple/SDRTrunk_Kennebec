package io.github.dsheirer.gui.theme;

import javafx.application.Platform;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Locale;

public class ThemeManager {
    private static final Logger mLog = LoggerFactory.getLogger(ThemeManager.class);
    private static final String REGISTRY_PATH = "Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize";
    private static final String REGISTRY_KEY = "AppsUseLightTheme";
    
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
}
