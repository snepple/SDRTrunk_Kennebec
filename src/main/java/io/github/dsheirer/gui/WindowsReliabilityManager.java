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

    //Guards against starting more than one watchdog process per run (e.g. started at launch and then
    //toggled on again at runtime).
    private static volatile boolean mWatchdogStarted = false;

    //Directory for the graceful-exit and watchdog-restart marker files.  These MUST live in a user-writable
    //location: when SDRTrunk is installed under C:\Program Files (the installer default), user.dir is not
    //writable without elevation, so marker writes failed with AccessDeniedException - silently breaking
    //graceful-exit detection and watchdog crash-loop protection.  Resolved from the user preferences
    //(application root) at startup; falls back to user.dir if not yet initialized.
    private static volatile String mMarkerDir;

    /**
     * Resolves the user-writable directory for the marker files, falling back to the working directory if
     * the preference-based application root has not yet been initialized.
     */
    private static String resolveMarkerDir() {
        String dir = mMarkerDir;
        return (dir != null) ? dir : System.getProperty("user.dir");
    }

    /**
     * Resolves the launcher used to (re)start the application: the native jpackage executable
     * (SDRTrunk.exe) when present, otherwise the runtime-image launcher (bin\sdrtrunk.bat). This makes
     * auto-start and watchdog-restart work for both the installer and the portable/runtime-zip layouts.
     */
    private static String resolveLauncher() {
        String dir = System.getProperty("user.dir");
        Path exe = Paths.get(dir, "SDRTrunk.exe");
        if (Files.exists(exe)) {
            return exe.toString();
        }
        return Paths.get(dir, "bin", "sdrtrunk.bat").toString();
    }

    /**
     * Starts the watchdog immediately if it is enabled and not already running. Lets the runtime toggle
     * take effect without requiring a restart.
     */
    public static void startWatchdogIfEnabled(UserPreferences prefs) {
        if (isWindows10OrNewer() && prefs != null && prefs.getApplicationPreference().isWatchdogEnabled()) {
            mMarkerDir = prefs.getDirectoryPreference().getDirectoryApplicationRoot().toString();
            startWatchdog();
        }
    }

    public static boolean isWindows10OrNewer() {
        String osName = System.getProperty("os.name");
        return osName != null && (osName.startsWith("Windows 10") || osName.startsWith("Windows 11"));
    }

    public static void manage(UserPreferences prefs) {
        if (!isWindows10OrNewer()) {
            return;
        }

        mMarkerDir = prefs.getDirectoryPreference().getDirectoryApplicationRoot().toString();

        try {
            Path exitFile = Paths.get(resolveMarkerDir(), GRACEFUL_EXIT_FILE);
            Files.deleteIfExists(exitFile);
        } catch (IOException e) {
            mLog.warn("Failed to clean up graceful exit file", e);
        }

        if (prefs.getApplicationPreference().isWatchdogEnabled()) {
            startWatchdog();
        }
    }

    private static synchronized void startWatchdog() {
        if (mWatchdogStarted) {
            return;
        }
        try {
            long currentPid = ProcessHandle.current().pid();
            String userDir = System.getProperty("user.dir");
            String markerDir = resolveMarkerDir();
            String launcher = resolveLauncher();
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
                  "Start-Process -FilePath '%s' -WorkingDirectory '%s' " +
                "}",
                currentPid, healthPort, markerDir, GRACEFUL_EXIT_FILE, markerDir, RESTART_MARKER_FILE, launcher, userDir
            );

            String encodedCmd = Base64.getEncoder().encodeToString(command.getBytes(StandardCharsets.UTF_16LE));

            new ProcessBuilder("powershell.exe", "-WindowStyle", "Hidden", "-EncodedCommand", encodedCmd).start();
            mWatchdogStarted = true;
            mLog.info("Windows Watchdog started monitoring PID {} (restart launcher: {})", currentPid, launcher);
        } catch (Exception e) {
            mLog.error("Failed to start Windows Watchdog", e);
        }
    }

    public static void setAutoStart(boolean enable) {
        if (!isWindows10OrNewer()) {
            return;
        }

        try {
            String scriptPath = resolveLauncher();
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
            Path exitFile = Paths.get(resolveMarkerDir(), GRACEFUL_EXIT_FILE);
            Files.writeString(exitFile, "exit");
        } catch (IOException e) {
            mLog.warn("Failed to write graceful exit file", e);
        }
    }
}
