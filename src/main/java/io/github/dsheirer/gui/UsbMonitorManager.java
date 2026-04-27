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
            Path logFile = logDir.resolve("sdrtrunk_app.log");

            List<String> command = new ArrayList<>();
            command.add("powershell.exe");
            command.add("-NoProfile");
            command.add("-ExecutionPolicy");
            command.add("Bypass");
            command.add("-Command");

            String startCommand = String.format(
                    "Start-Process powershell.exe -Verb RunAs -WindowStyle Hidden -ArgumentList '-NoProfile -ExecutionPolicy Bypass -File \"%s\" -ProcessId %d -LogFile \"%s\"'",
                    scriptPath.toAbsolutePath().toString(),
                    currentPid,
                    logFile.toAbsolutePath().toString()
            );

            command.add(startCommand);

            mLog.info("Starting USB Monitor script from: {}", scriptPath);
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.start();
        } catch (Exception e) {
            mLog.error("Failed to start USB Monitor script", e);
        }
    }
}
