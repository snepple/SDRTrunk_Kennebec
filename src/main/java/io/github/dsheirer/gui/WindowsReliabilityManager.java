package io.github.dsheirer.gui;

import io.github.dsheirer.preference.UserPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class WindowsReliabilityManager {
    private static final Logger mLog = LoggerFactory.getLogger(WindowsReliabilityManager.class);
    private static final String GRACEFUL_EXIT_FILE = "sdrtrunk_graceful_exit";

    public static boolean isWindows10OrNewer() {
        String osName = System.getProperty("os.name");
        return osName != null && (osName.startsWith("Windows 10") || osName.startsWith("Windows 11"));
    }

    public static void manage(UserPreferences prefs) {
        if (!isWindows10OrNewer()) {
            return;
        }

        try {
            Path exitFile = Paths.get(System.getProperty("user.dir"), GRACEFUL_EXIT_FILE);
            Files.deleteIfExists(exitFile);
        } catch (IOException e) {
            mLog.warn("Failed to clean up graceful exit file", e);
        }

        if (prefs.getApplicationPreference().isWatchdogEnabled()) {
            startWatchdog();
        }
    }

    private static void startWatchdog() {
        try {
            long currentPid = ProcessHandle.current().pid();
            String userDir = System.getProperty("user.dir");
            String command = String.format(
                "$pidToMonitor = %d; Wait-Process -Id $pidToMonitor; if (-not (Test-Path -Path '%s\\%s')) { Start-Process 'bin\\sdrtrunk.bat' }",
                currentPid, userDir, GRACEFUL_EXIT_FILE
            );

            new ProcessBuilder("powershell.exe", "-WindowStyle", "Hidden", "-Command", command).start();
            mLog.info("Windows Watchdog started monitoring PID {}", currentPid);
        } catch (Exception e) {
            mLog.error("Failed to start Windows Watchdog", e);
        }
    }

    public static void setAutoStart(boolean enable) {
        if (!isWindows10OrNewer()) {
            return;
        }

        try {
            String scriptPath = System.getProperty("user.dir") + "\\bin\\sdrtrunk.bat";
            ProcessBuilder pb;
            if (enable) {
                pb = new ProcessBuilder("reg", "add", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run", "/v", "SDRTrunk", "/t", "REG_SZ", "/d", "\"" + scriptPath + "\"", "/f");
            } else {
                pb = new ProcessBuilder("reg", "delete", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run", "/v", "SDRTrunk", "/f");
            }
            pb.start();
            mLog.info("Windows AutoStart configured to {}", enable);
        } catch (Exception e) {
            mLog.error("Failed to configure Windows AutoStart", e);
        }
    }

    public static void stopWatchdog() {
        if (!isWindows10OrNewer()) {
            return;
        }
        try {
            Path exitFile = Paths.get(System.getProperty("user.dir"), GRACEFUL_EXIT_FILE);
            Files.writeString(exitFile, "exit");
        } catch (IOException e) {
            mLog.warn("Failed to write graceful exit file", e);
        }
    }
}
