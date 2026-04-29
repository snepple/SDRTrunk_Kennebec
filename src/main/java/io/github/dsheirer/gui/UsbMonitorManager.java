package io.github.dsheirer.gui;

import io.github.dsheirer.preference.UserPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class UsbMonitorManager {
    private static final Logger mLog = LoggerFactory.getLogger(UsbMonitorManager.class);
    private static final String SCRIPT_NAME = "usb_monitor.ps1";
    private static final String CONFIG_NAME = "usb_monitor_config.json";

    public static void start(UserPreferences userPreferences) {
        String osName = System.getProperty("os.name");
        if (osName == null || (!osName.startsWith("Windows 10") && !osName.startsWith("Windows 11"))) {
            mLog.info("USB Monitor is only supported on Windows 10 or newer. Current OS: {}", osName);
            return;
        }

        try {
            Path appRoot = userPreferences.getDirectoryPreference().getDirectoryApplicationRoot();
            Path scriptDir = appRoot.resolve("scripts");
            if (!Files.exists(scriptDir)) {
                Files.createDirectories(scriptDir);
            }

            Path scriptPath = scriptDir.resolve(SCRIPT_NAME);
            Path configPath = scriptDir.resolve(CONFIG_NAME);

            // Extract script from resources
            try (InputStream is = UsbMonitorManager.class.getResourceAsStream("/scripts/" + SCRIPT_NAME)) {
                if (is == null) {
                    mLog.error("Could not find {} in resources", SCRIPT_NAME);
                    return;
                }
                Files.copy(is, scriptPath, StandardCopyOption.REPLACE_EXISTING);
            }

            long currentPid = ProcessHandle.current().pid();
            Path logDir = userPreferences.getDirectoryPreference().getDirectoryApplicationLog();
            Path logFile = logDir.resolve("sdrtrunk_usb_monitor.log");

            // Write JSON configuration for the PowerShell script to read
            String configJson = String.format("{\"ProcessId\": %d, \"LogFile\": \"%s\"}",
                    currentPid, logFile.toAbsolutePath().toString().replace("\\", "\\\\"));
            Files.writeString(configPath, configJson);

            String userName = System.getProperty("user.name");
            String taskName = "SDRTrunk_UsbMonitor_" + userName;
            String scriptPathStr = scriptPath.toAbsolutePath().toString();

            // Check if the scheduled task already exists
            boolean taskExists = checkScheduledTask(taskName, scriptPathStr);

            if (!taskExists) {
                mLog.info("Scheduled task '{}' does not exist or requires updating. Prompting UAC to create it...", taskName);
                if (createScheduledTask(taskName, scriptPathStr)) {
                    mLog.info("Successfully created scheduled task '{}'.", taskName);
                } else {
                    mLog.error("Failed to create scheduled task '{}'. USB Monitor may not run.", taskName);
                    return;
                }
            }

            // Start the scheduled task silently
            startScheduledTask(taskName);

        } catch (Exception e) {
            mLog.error("Failed to start USB Monitor script", e);
        }
    }

    private static boolean checkScheduledTask(String taskName, String scriptPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "powershell.exe",
                    "-NoProfile",
                    "-ExecutionPolicy", "Bypass",
                    "-Command",
                    String.format("$task = Get-ScheduledTask -TaskName '%s' -ErrorAction SilentlyContinue; if ($task -and $task.Actions[0].Arguments -match [regex]::Escape('%s')) { exit 0 } else { exit 1 }",
                            taskName, scriptPath)
            );
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            mLog.error("Error checking scheduled task '{}'", taskName, e);
            return false;
        }
    }

    private static boolean createScheduledTask(String taskName, String scriptPath) {
        try {
            // Unregister old task if it exists (using RunAs to avoid access denied if it's elevated)
            // Then register the new task
            String psCommand = String.format(
                    "Unregister-ScheduledTask -TaskName '%s' -Confirm:$false -ErrorAction SilentlyContinue; " +
                            "$action = New-ScheduledTaskAction -Execute 'powershell.exe' -Argument '-NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File \"%s\"'; " +
                            "$principal = New-ScheduledTaskPrincipal -UserId 'NT AUTHORITY\\SYSTEM' -LogonType ServiceAccount -RunLevel Highest; " +
                            "$settings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -ExecutionTimeLimit (New-TimeSpan -Days 1000); " +
                            "Register-ScheduledTask -TaskName '%s' -Action $action -Principal $principal -Settings $settings",
                    taskName, scriptPath, taskName
            );

            // Execute the powershell script using Start-Process -Verb RunAs to prompt for UAC once
            ProcessBuilder pb = new ProcessBuilder(
                    "powershell.exe",
                    "-NoProfile",
                    "-ExecutionPolicy", "Bypass",
                    "-Command",
                    String.format("Start-Process powershell.exe -WindowStyle Hidden -Verb RunAs -ArgumentList '-NoProfile -ExecutionPolicy Bypass -Command \"%s\"' -Wait", psCommand)
            );

            Process process = pb.start();
            int exitCode = process.waitFor();

            // Allow a moment for task registration to complete in the background process
            Thread.sleep(1000);

            // Re-verify it was created successfully
            return checkScheduledTask(taskName, scriptPath);
        } catch (Exception e) {
            mLog.error("Error creating scheduled task '{}'", taskName, e);
            return false;
        }
    }

    private static void startScheduledTask(String taskName) {
        try {
            mLog.info("Starting USB Monitor scheduled task: {}", taskName);
            ProcessBuilder pb = new ProcessBuilder(
                    "powershell.exe",
                    "-NoProfile",
                    "-ExecutionPolicy", "Bypass",
                    "-WindowStyle", "Hidden",
                    "-Command",
                    String.format("Start-ScheduledTask -TaskName '%s'", taskName)
            );
            Process process = pb.start();
            process.waitFor();
        } catch (Exception e) {
            mLog.error("Failed to start scheduled task '{}'", taskName, e);
        }
    }
}
