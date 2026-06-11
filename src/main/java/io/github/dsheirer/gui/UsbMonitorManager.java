
package io.github.dsheirer.gui;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;
import javafx.scene.control.Button;

import io.github.dsheirer.preference.UserPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.control.CheckBox;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.application.Platform;
import java.util.Optional;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

public class UsbMonitorManager {
    private static final Logger mLog = LoggerFactory.getLogger(UsbMonitorManager.class);
    private static final String SCRIPT_NAME = "usb_monitor.ps1";
    private static final String CONFIG_NAME = "usb_monitor_config.json";

    public static void manage(UserPreferences userPreferences) {
        String osName = System.getProperty("os.name");
        if (osName == null || (!osName.startsWith("Windows 10") && !osName.startsWith("Windows 11"))) {
            return;
        }

        boolean installed = userPreferences.getApplicationPreference().isUsbMonitorInstalled();

        if (installed) {
            startSilent(userPreferences);
        }
    }

    public static boolean install(UserPreferences userPreferences) {
        String osName = System.getProperty("os.name");
        if (osName == null || (!osName.startsWith("Windows 10") && !osName.startsWith("Windows 11"))) {
            return false;
        }

        try {
            Path appRoot = userPreferences.getDirectoryPreference().getDirectoryApplicationRoot();
            Path scriptDir = appRoot.resolve("scripts");
            if (!Files.exists(scriptDir)) {
                Files.createDirectories(scriptDir);
            }

            Path scriptPath = scriptDir.resolve(SCRIPT_NAME);
            Path configPath = scriptDir.resolve(CONFIG_NAME);

            try (InputStream is = UsbMonitorManager.class.getResourceAsStream("/scripts/" + SCRIPT_NAME)) {
                if (is == null) {
                    mLog.error("Could not find {} in resources", SCRIPT_NAME);
                    return false;
                }
                Files.copy(is, scriptPath, StandardCopyOption.REPLACE_EXISTING);
            }

            long currentPid = ProcessHandle.current().pid();
            Path logDir = userPreferences.getDirectoryPreference().getDirectoryApplicationLog();
            Path logFile = logDir.resolve("sdrtrunk_usb_monitor.log");

            String configJson = String.format("{\"ProcessId\": %d, \"LogFile\": \"%s\"}",
                    currentPid, logFile.toAbsolutePath().toString().replace("\\", "\\\\"));
            Files.writeString(configPath, configJson);

            String userName = System.getProperty("user.name");
            String taskName = "SDRTrunk_UsbMonitor_" + userName;
            String scriptPathStr = scriptPath.toAbsolutePath().toString();

            mLog.info("Creating scheduled task '{}'...", taskName);
            if (createScheduledTask(taskName, scriptPathStr)) {
                mLog.info("Successfully created scheduled task '{}'.", taskName);
                userPreferences.getApplicationPreference().setUsbMonitorInstalled(true);
                userPreferences.getApplicationPreference().setUsbMonitorPrompted(true);
                startScheduledTask(taskName);
                return true;
            } else {
                mLog.error("Failed to create scheduled task '{}'.", taskName);
                return false;
            }
        } catch (Exception e) {
            mLog.error("Failed to start USB Monitor script", e);
            return false;
        }
    }

    public static String prepareAndGetScheduledTaskScript(UserPreferences userPreferences) {
        String osName = System.getProperty("os.name");
        if (osName == null || (!osName.startsWith("Windows 10") && !osName.startsWith("Windows 11"))) {
            return "";
        }
        try {
            Path appRoot = userPreferences.getDirectoryPreference().getDirectoryApplicationRoot();
            Path scriptDir = appRoot.resolve("scripts");
            if (!Files.exists(scriptDir)) {
                Files.createDirectories(scriptDir);
            }

            Path scriptPath = scriptDir.resolve(SCRIPT_NAME);
            Path configPath = scriptDir.resolve(CONFIG_NAME);

            try (InputStream is = UsbMonitorManager.class.getResourceAsStream("/scripts/" + SCRIPT_NAME)) {
                if (is != null) {
                    Files.copy(is, scriptPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }

            long currentPid = ProcessHandle.current().pid();
            Path logDir = userPreferences.getDirectoryPreference().getDirectoryApplicationLog();
            Path logFile = logDir.resolve("sdrtrunk_usb_monitor.log");

            String configJson = String.format("{\"ProcessId\": %d, \"LogFile\": \"%s\"}",
                    currentPid, logFile.toAbsolutePath().toString().replace("\\", "\\\\"));
            Files.writeString(configPath, configJson);

            String userName = System.getProperty("user.name");
            String taskName = "SDRTrunk_UsbMonitor_" + userName;
            String scriptPathStr = scriptPath.toAbsolutePath().toString();

            return getScheduledTaskScriptContent(taskName, scriptPathStr);
        } catch (Exception e) {
            mLog.error("Error preparing USB Monitor script", e);
            return "";
        }
    }



    public static boolean uninstall(UserPreferences userPreferences) {
        String userName = System.getProperty("user.name");
        String taskName = "SDRTrunk_UsbMonitor_" + userName;
        try {
            String uninstallCmd = String.format("Unregister-ScheduledTask -TaskName '%s' -Confirm:$false -ErrorAction SilentlyContinue", taskName);
            String innerEncodedCmd = Base64.getEncoder().encodeToString(uninstallCmd.getBytes(StandardCharsets.UTF_16LE));
            String outerCmd = String.format("Start-Process powershell.exe -WindowStyle Hidden -Verb RunAs -ArgumentList @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-EncodedCommand', '%s') -Wait", innerEncodedCmd);
            String outerEncodedCmd = Base64.getEncoder().encodeToString(outerCmd.getBytes(StandardCharsets.UTF_16LE));
            ProcessBuilder pb = new ProcessBuilder(
                    "powershell.exe",
                    "-NoProfile",
                    "-ExecutionPolicy", "Bypass",
                    "-EncodedCommand",
                    outerEncodedCmd
            );
            Process process = pb.start();
            process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            userPreferences.getApplicationPreference().setUsbMonitorInstalled(false);
            return true;
        } catch (Exception e) {
            mLog.error("Error uninstalling task '{}'", taskName, e);
            return false;
        }
    }

    private static void startSilent(UserPreferences userPreferences) {
        try {
            Path appRoot = userPreferences.getDirectoryPreference().getDirectoryApplicationRoot();
            Path scriptDir = appRoot.resolve("scripts");
            Path scriptPath = scriptDir.resolve(SCRIPT_NAME);
            Path configPath = scriptDir.resolve(CONFIG_NAME);

            boolean needsUpdate = true;
            if (Files.exists(scriptPath)) {
                String content = Files.readString(scriptPath);
                if (content.contains("# Version: 1")) {
                    needsUpdate = false;
                }
            }
            if (needsUpdate) {
                try (InputStream is = UsbMonitorManager.class.getResourceAsStream("/scripts/" + SCRIPT_NAME)) {
                    if (is != null) {
                        Files.copy(is, scriptPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }

            long currentPid = ProcessHandle.current().pid();
            Path logDir = userPreferences.getDirectoryPreference().getDirectoryApplicationLog();
            Path logFile = logDir.resolve("sdrtrunk_usb_monitor.log");
            String configJson = String.format("{\"ProcessId\": %d, \"LogFile\": \"%s\"}",
                    currentPid, logFile.toAbsolutePath().toString().replace("\\", "\\\\"));
            Files.writeString(configPath, configJson);

            String userName = System.getProperty("user.name");
            String taskName = "SDRTrunk_UsbMonitor_" + userName;
            startScheduledTask(taskName);
        } catch (Exception e) {
            mLog.error("Error in startSilent", e);
        }
    }

    private static boolean checkScheduledTask(String taskName, String expectedArg) {
        try {
            String cmd = String.format("$task = Get-ScheduledTask -TaskName '%s' -ErrorAction SilentlyContinue; if ($task -and $task.Actions[0].Arguments -match [regex]::Escape('%s')) { exit 0 } else { exit 1 }", taskName, expectedArg);
            String encodedCmd = Base64.getEncoder().encodeToString(cmd.getBytes(StandardCharsets.UTF_16LE));
            ProcessBuilder pb = new ProcessBuilder(
                    "powershell.exe",
                    "-NoProfile",
                    "-ExecutionPolicy", "Bypass",
                    "-EncodedCommand",
                    encodedCmd
            );
            Process process = pb.start();
            process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS); int exitCode = process.isAlive() ? 1 : process.exitValue();
            return exitCode == 0;
        } catch (Exception e) {
            mLog.error("Error checking scheduled task '{}'", taskName, e);
            return false;
        }
    }

    public static String getScheduledTaskScriptContent(String taskName, String scriptPath) {
        String scriptCmd = String.format("& '%s'", scriptPath);
        String encodedScriptCmd = Base64.getEncoder().encodeToString(scriptCmd.getBytes(StandardCharsets.UTF_16LE));

        return String.format(
                "Unregister-ScheduledTask -TaskName '%s' -Confirm:$false -ErrorAction SilentlyContinue\r\n" +
                "$action = New-ScheduledTaskAction -Execute 'powershell.exe' -Argument '-NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -EncodedCommand %s'\r\n" +
                "$principal = New-ScheduledTaskPrincipal -UserId 'SYSTEM' -LogonType ServiceAccount -RunLevel Highest\r\n" +
                "$settings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -ExecutionTimeLimit (New-TimeSpan -Days 1000)\r\n" +
                "Register-ScheduledTask -TaskName '%s' -Action $action -Principal $principal -Settings $settings\r\n",
                taskName, encodedScriptCmd, taskName
        );
    }

    private static boolean createScheduledTask(String taskName, String scriptPath) {
        try {
            // Sanitize task name - dots and special chars can cause issues
            taskName = taskName.replaceAll("[^a-zA-Z0-9_]", "_");

            String psCommands = getScheduledTaskScriptContent(taskName, scriptPath);

            String encodedInnerCmd = Base64.getEncoder().encodeToString(psCommands.getBytes(StandardCharsets.UTF_16LE));

            String outerCmd = String.format(
                    "Start-Process powershell.exe -WindowStyle Hidden -Verb RunAs -ArgumentList @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-EncodedCommand', '%s') -Wait",
                    encodedInnerCmd
            );
            String outerEncodedCmd = Base64.getEncoder().encodeToString(outerCmd.getBytes(StandardCharsets.UTF_16LE));

            ProcessBuilder pb = new ProcessBuilder(
                    "powershell.exe",
                    "-NoProfile",
                    "-ExecutionPolicy", "Bypass",
                    "-EncodedCommand",
                    outerEncodedCmd
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS); int exitCode = process.isAlive() ? 1 : process.exitValue();
            mLog.info("Task creation process exited with code {}. Output: {}", exitCode, output.trim());

            Thread.sleep(1000);

            // Cannot reliably checkTaskExists as standard user for SYSTEM tasks
            return true;
        } catch (Exception e) {
            mLog.error("Error creating scheduled task '{}'", taskName, e);
            return false;
        }
    }

    private static boolean checkTaskExists(String taskName) {
        try {
            String cmd = String.format("Get-ScheduledTask -TaskName '%s' -ErrorAction Stop | Out-Null; exit 0", taskName);
            String encodedCmd = Base64.getEncoder().encodeToString(cmd.getBytes(StandardCharsets.UTF_16LE));
            ProcessBuilder pb = new ProcessBuilder(
                    "powershell.exe",
                    "-NoProfile",
                    "-ExecutionPolicy", "Bypass",
                    "-EncodedCommand",
                    encodedCmd
            );
            Process process = pb.start();
            process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS); int exitCode = process.isAlive() ? 1 : process.exitValue();
            return exitCode == 0;
        } catch (Exception e) {
            mLog.error("Error checking scheduled task '{}'", taskName, e);
            return false;
        }
    }

    private static void startScheduledTask(String taskName) {
        try {
            mLog.info("Starting USB Monitor scheduled task: {}", taskName);
            String cmd = String.format("Start-ScheduledTask -TaskName '%s'", taskName);
            String encodedCmd = Base64.getEncoder().encodeToString(cmd.getBytes(StandardCharsets.UTF_16LE));
            ProcessBuilder pb = new ProcessBuilder(
                    "powershell.exe",
                    "-NoProfile",
                    "-ExecutionPolicy", "Bypass",
                    "-WindowStyle", "Hidden",
                    "-EncodedCommand",
                    encodedCmd
            );
            Process process = pb.start();
            process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            mLog.error("Failed to start scheduled task '{}'", taskName, e);
        }
    }
}
