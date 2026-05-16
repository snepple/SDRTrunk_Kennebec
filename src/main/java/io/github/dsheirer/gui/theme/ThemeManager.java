package io.github.dsheirer.gui.theme;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import io.github.dsheirer.util.ThreadPool;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.Locale;

public class ThemeManager {
    private static final Logger mLog = LoggerFactory.getLogger(ThemeManager.class);
    private static final String REGISTRY_PATH = "Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize";
    private static final String REGISTRY_KEY = "AppsUseLightTheme";
    
    private boolean mIsWindows11;
    private ScheduledFuture<?> mRegistryMonitorFuture;

    public ThemeManager() {
        String osName = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        // Basic check for Windows 11 - usually reported as Windows 11 or Windows 10 with build >= 22000
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

            // Set system properties for FlatLaf BEFORE initializing LookAndFeel
            if (mIsWindows11) {
                System.setProperty("flatlaf.window.backdrop", "mica");
                System.setProperty("flatlaf.useWindowDecorations", "true");
            }

            if (useLightTheme) {
                UIManager.setLookAndFeel(new FlatLightLaf());
            } else {
                UIManager.setLookAndFeel(new FlatDarkLaf());
            }
            
            // Apply Mica if Windows 11
            if (mIsWindows11) {
                UIManager.put("TitlePane.useWindowDecorations", true);
                UIManager.put("TitlePane.menuBarEmbedded", false);
                JFrame.setDefaultLookAndFeelDecorated(true);
                JDialog.setDefaultLookAndFeelDecorated(true);
            }


            if (isWindows()) {
                UIManager.put("ScrollBar.showButtons", true);
                UIManager.put("ScrollBar.width", 16);
                UIManager.put("ScrollBar.thumbArc", 0);
                UIManager.put("ScrollBar.thumbInsets", new java.awt.Insets(0, 0, 0, 0));
            }

            // Update UI tree if running
            for (java.awt.Window window : java.awt.Window.getWindows()) {
                SwingUtilities.updateComponentTreeUI(window);
            }
        } catch (Exception e) {
            mLog.error("Failed to initialize FlatLaf theme", e);
        }
    }

    private void startRegistryMonitor() {
        mRegistryMonitorFuture = ThreadPool.SCHEDULED.scheduleWithFixedDelay(new Runnable() {
            private int lastValue = -1;

            @Override
            public void run() {
                try {
                    int currentValue = Advapi32Util.registryGetIntValue(WinReg.HKEY_CURRENT_USER, REGISTRY_PATH, REGISTRY_KEY);
                    if (lastValue == -1) {
                        lastValue = currentValue;
                    } else if (currentValue != lastValue) {
                        lastValue = currentValue;
                        SwingUtilities.invokeLater(ThemeManager.this::applyTheme);
                    }
                } catch (Exception e) {
                    // Key might not exist temporarily
                }
            }
        }, 0, 2, TimeUnit.SECONDS);
    }
}
