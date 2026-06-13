package io.github.dsheirer.gui;

import io.github.dsheirer.preference.UserPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

public class WindowsReliabilityManager {
    private static final Logger mLog = LoggerFactory.getLogger(WindowsReliabilityManager.class);
    private static final String GRACEFUL_EXIT_FILE = "sdrtrunk_graceful_exit";
    private static final String RESTART_MARKER_FILE = "sdrtrunk_watchdog_restart";

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
            int healthPort = RestApiWatchdog.getConfiguredPort();

            //Use absolute paths and an explicit working directory: the watchdog-restarted instance
            //(and instances launched from the registry Run key) may not inherit the install
            //directory as the current directory, which would break a relative bin\sdrtrunk.bat.
            //A restart marker file provides crash-loop protection: if the application died within
            //120 seconds of the previous watchdog restart, do not restart again - a tight
            //crash/restart loop cannot be fixed by restarting.
            //
            //Health integration: while the process is alive, the watchdog polls the local REST
            ///health endpoint each minute.  A 503 response means degraded-but-alive (e.g. a tuner
            //in recovery) and is treated as healthy for liveness purposes.  Only after the API has
            //responded at least once (so configurations with the API disabled are never affected)
            //do five consecutive no-response polls indicate a wedged process, which is then killed
            //so the existing restart logic recovers it.
            String command = String.format(
                "$appPid = %d; $healthPort = %d; $apiSeen = $false; $fails = 0; " +
                "while (Get-Process -Id $appPid -ErrorAction SilentlyContinue) { " +
                  "Start-Sleep -Seconds 60; " +
                  "$alive = $false; " +
                  "try { Invoke-WebRequest -Uri ('http://127.0.0.1:' + $healthPort + '/health') -UseBasicParsing -TimeoutSec 10 | Out-Null; $alive = $true } " +
                  "catch { if ($_.Exception.Response) { $alive = $true } }; " +
                  "if ($alive) { $apiSeen = $true; $fails = 0 } " +
                  "elseif ($apiSeen) { $fails++; if ($fails -ge 5) { Stop-Process -Id $appPid -Force -ErrorAction SilentlyContinue; break } } " +
                "}; " +
                "Wait-Process -Id $appPid -ErrorAction SilentlyContinue; " +
                "if (-not (Test-Path -Path '%s\\%s')) { " +
                  "$marker = '%s\\%s'; " +
                  "if ((Test-Path $marker) -and (((Get-Date) - (Get-Item $marker).LastWriteTime).TotalSeconds -lt 120)) { exit }; " +
                  "New-Item -ItemType File -Path $marker -Force | Out-Null; " +
                  "Start-Process -FilePath '%s\\bin\\sdrtrunk.bat' -WorkingDirectory '%s' " +
                "}",
                currentPid, healthPort, userDir, GRACEFUL_EXIT_FILE, userDir, RESTART_MARKER_FILE, userDir, userDir
            );

            String encodedCmd = Base64.getEncoder().encodeToString(command.getBytes(StandardCharsets.UTF_16LE));

            new ProcessBuilder("powershell.exe", "-WindowStyle", "Hidden", "-EncodedCommand", encodedCmd).start();
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
