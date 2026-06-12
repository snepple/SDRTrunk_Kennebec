/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.monitor;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;

import io.github.dsheirer.controller.channel.ChannelProcessingManager;
import io.github.dsheirer.log.LoggingSuppressor;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.util.ThreadPool;
import io.github.dsheirer.util.TimeStamp;
import java.io.IOException;
import java.lang.ProcessBuilder;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.application.Platform;
import java.util.Optional;

/**
 * Utility class for monitoring system components and producing logging reports.
 */
public class DiagnosticMonitor
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticMonitor.class);
    private final LoggingSuppressor LOG_SUPPRESSOR = new LoggingSuppressor(LOGGER);
    private static final String DIVIDER = "\n\n=========================================================================\n\n";
    private UserPreferences mUserPreferences;
    private ChannelProcessingManager mChannelProcessingManager;
    private TunerManager mTunerManager;
    private ScheduledFuture<?> mBlockedThreadMonitorHandle;
    private BlockedThreadMonitor mMonitor = new BlockedThreadMonitor();
    private boolean mUserAlertedToBlockedThreadCondition = false;
    private Map<Integer,Integer> mBlockedThreadDetectionCountMap = new HashMap<>();
    private boolean mHeadless;

    /**
     * Constructs an instance
     * @param userPreferences for application logging directory lookup.
     * @param channelProcessingManager for accessing running channel information
     * @param tunerManager for accessing allocated tuner channel information
     * @param headless to indicate if the thread deadlock monitor should show a user notification.
     */
    public DiagnosticMonitor(UserPreferences userPreferences, ChannelProcessingManager channelProcessingManager,
                             TunerManager tunerManager, boolean headless)
    {
        mUserPreferences = userPreferences;
        mChannelProcessingManager = channelProcessingManager;
        mTunerManager = tunerManager;
        mHeadless = headless;
    }

    /**
     * Starts monitoring for blocked threads
     */
    public void start()
    {
        if(mBlockedThreadMonitorHandle != null)
        {
            mBlockedThreadMonitorHandle.cancel(true);
        }

        if(mUserPreferences.getApplicationPreference().isAutomaticDiagnosticMonitoring())
        {
            LOGGER.info("Diagnostic monitoring enabled running every 30 seconds");
            mBlockedThreadMonitorHandle = ThreadPool.SCHEDULED.scheduleAtFixedRate(mMonitor, 30, 30, TimeUnit.SECONDS);
        }
        else
        {
            LOGGER.info("Diagnostic monitoring disabled per user preference (application).");
        }
        
        checkForPowerThrottling();
    }

    /**
     * Stops monitoring for blocked threads.
     */
    public void stop()
    {
        if(mBlockedThreadMonitorHandle != null)
        {
            mBlockedThreadMonitorHandle.cancel(true);
        }

        mBlockedThreadMonitorHandle = null;
    }

    private boolean mUserAlertedToPowerThrottling = false;

    private void checkForPowerThrottling() {
        if (!mUserAlertedToPowerThrottling && System.getProperty("os.name").toLowerCase().contains("win")) {
            boolean prompted = false;
            try {
                java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(io.github.dsheirer.gui.SDRTrunk.class);
                prompted = prefs.getBoolean("sdrtrunk.diagnostics.powerthrottling.prompted", false);
            } catch (Exception ex) {}

            if (prompted) {
                mUserAlertedToPowerThrottling = true;
                return;
            }

            boolean isSdrTrunkExeDisabled = isEcoQoSDisabled("SDRTrunk.exe");
            boolean isJavaExeDisabled = isEcoQoSDisabled("java.exe");
            
            if (!isSdrTrunkExeDisabled && !isJavaExeDisabled) {
                mUserAlertedToPowerThrottling = true;
                
                // Show the dialog first instead of blindly throwing a UAC prompt at the user
                if (!mHeadless) {
                    String processPath = ProcessHandle.current().info().command().orElse("java.exe");
                    if (!processPath.toLowerCase().endsWith(".exe")) {
                        processPath = "java.exe";
                    }
                    
                    String title = "SDRTrunk Needs Your Permission";
                    String message = "Windows is slowing down SDRTrunk to save power. " +
                                     "This can cause audio to cut out or sound choppy.\n\n" +
                                     "To fix this, click \"Fix It Now\" below. Windows will ask " +
                                     "for your permission (UAC) — just click \"Yes\" on the popup that appears.\n\n" +
                                     "If you'd rather do this later, click \"Remind Me Later\". " +
                                     "You can also fix this manually:\n\n" +
                                     "Step 1: Right-click the Start button and choose\n" +
                                     "             \"Terminal (Admin)\" or \"PowerShell (Admin)\"\n" +
                                     "Step 2: Type this command and press Enter:\n" +
                                     "             powercfg /powerthrottling disable /path \"" + processPath + "\"";
                    
                    final String finalProcessPath = processPath;
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.WARNING); io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane());
                        alert.setTitle(title);
                        alert.setHeaderText(title);
                        alert.setContentText(message);
                        
                        javafx.scene.control.ButtonType fixBtn = new javafx.scene.control.ButtonType("Fix It Now", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
                        javafx.scene.control.ButtonType laterBtn = new javafx.scene.control.ButtonType("Remind Me Later", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
                        javafx.scene.control.ButtonType dismissBtn = new javafx.scene.control.ButtonType("Don't Ask Again", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
                        
                        alert.getButtonTypes().setAll(fixBtn, laterBtn, dismissBtn);
                        
                        Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
                        if (result.isPresent() && result.get() == fixBtn) {
                            try {
                                String retryCmd = "Start-Process powercfg -ArgumentList '/powerthrottling disable /path `\"" + finalProcessPath + "`\"' -Verb RunAs -WindowStyle Hidden";
                                new ProcessBuilder("powershell.exe", "-Command", retryCmd).start();
                                // Save preference so we don't prompt again after they fix it
                                java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(io.github.dsheirer.gui.SDRTrunk.class);
                                prefs.putBoolean("sdrtrunk.diagnostics.powerthrottling.prompted", true);
                            } catch (Exception ex) {
                                LOGGER.error("Failed to disable power throttling", ex);
                            }
                        } else if (result.isPresent() && result.get() == dismissBtn) {
                            // Save preference so we NEVER prompt again
                            try {
                                java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(io.github.dsheirer.gui.SDRTrunk.class);
                                prefs.putBoolean("sdrtrunk.diagnostics.powerthrottling.prompted", true);
                            } catch (Exception ex) {
                                LOGGER.error("Failed to save preferences", ex);
                            }
                        }
                    });
                }
            }
        }
    }

    private boolean isEcoQoSDisabled(String processName) {
        try {
            Process process = new ProcessBuilder("reg", "query",
                    "HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Image File Execution Options\\" + processName + "\\PerfOptions",
                    "/v", "CpuPriorityClass").start();
            java.util.Scanner s = new java.util.Scanner(process.getInputStream()).useDelimiter("\\A");
            String output = s.hasNext() ? s.next() : "";
            process.waitFor();
            if (output.contains("0x3")) {
                return true;
            }
            
            process = new ProcessBuilder("reg", "query",
                    "HKCU\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Image File Execution Options\\" + processName + "\\PerfOptions",
                    "/v", "CpuPriorityClass").start();
            s = new java.util.Scanner(process.getInputStream()).useDelimiter("\\A");
            output = s.hasNext() ? s.next() : "";
            process.waitFor();
            
            if (output.contains("0x3")) {
                return true;
            }
        } catch (Exception e) {
            // Ignore
        }
        return false;
    }

    /**
     * Checks for blocked threads and on discovery, generates a diagnostic report and notifies the user (once).
     */
    private void checkForBlockedThreads()
    {
        if(!mUserAlertedToBlockedThreadCondition)
        {
            try
            {
                ThreadMXBean bean = ManagementFactory.getThreadMXBean();

                long ids[] = bean.findDeadlockedThreads();

                if(ids != null)
                {
                    mUserAlertedToBlockedThreadCondition = true;

                    ThreadInfo threadInfo[] = bean.getThreadInfo(ids);

                    StringBuilder sb = new StringBuilder();
                    sb.append("sdrtrunk detected a critical application error with a threading deadlock, described as follows:\n");

                    for (ThreadInfo threadInfo1 : threadInfo)
                    {
                        sb.append("Thread ID[").append(threadInfo1.getThreadId());
                        sb.append("] Name [").append(threadInfo1.getThreadName());
                        sb.append("] Lock [").append(threadInfo1.getLockName());
                        sb.append("] Owned By [ID:").append(threadInfo1.getLockOwnerId());
                        sb.append(" | NAME:").append(threadInfo1.getLockName());
                        sb.append("]\n");
                    }

                    LOGGER.error(sb.toString());
                    Path reportPath = generateProcessingDiagnosticReport(sb + DIVIDER);
                    LOGGER.error("Thread deadlock report generated: " + reportPath);

                    if(!mHeadless)
                    {
                        String title = "sdrtrunk: Critical Error Detected";
                        String message = "The sdrtrunk application has detected a thread deadlock situation.\n" +
                                         "The application may degrade over time and eventually run out of memory.\n" +
                                         "A diagnostic report was generated.  Please open an issue on the GitHub\n" +
                                         "website and attach this diagnostic report:\n\n" + reportPath.toString();
                        Platform.runLater(() -> { Alert alert = new Alert(Alert.AlertType.ERROR); io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane()); alert.setContentText(String.valueOf(title)); alert.showAndWait(); });
                    }
                }
            }
            catch(Exception e)
            {
                LOG_SUPPRESSOR.error("run error", 1, "Error while monitoring for deadlocked " +
                        "threads: " + e.getLocalizedMessage());
                //Set the flag so that we don't try to run again.
                mUserAlertedToBlockedThreadCondition = true;
            }
        }
    }

    /**
     * Creates a diagnostic report containing state information for channels that are in a processing state.
     * @param message to prepend to the report
     * @return path for the log file that was created.
     */
    public Path generateProcessingDiagnosticReport(String message) throws IOException
    {
        StringBuilder sb = new StringBuilder();
        sb.append(message);
        sb.append("\n\nsdrtrunk Processing Diagnostic Report\n");
        sb.append(DIVIDER);
        sb.append(getEnvironmentReport());
        sb.append(DIVIDER);
        sb.append(mTunerManager.getDiscoveredTunerModel().getDiagnosticReport());
        sb.append(DIVIDER);
        sb.append(mChannelProcessingManager.getDiagnosticInformation());
        sb.append(DIVIDER);
        // sb.append... getDiagnosticInformation()
        sb.append(DIVIDER);
        sb.append(getThreadDumpReport());
        sb.append(DIVIDER);

        Path logDirectory = mUserPreferences.getDirectoryPreference().getDirectoryApplicationLog();
        String file = TimeStamp.getFileFormattedDateTime() + "_sdrtrunk_processing_diagnostic_report.log";
        Path output = logDirectory.resolve(file);
        Files.write(output, sb.toString().getBytes());
        return output;
    }

    /**
     * Dumps the current threads to a log file with current date and time to the application log directory.
     * @return path to the thread dump log file that was created.
     * @throws IOException if there is an issue writing the contents to the log file.
     */
    public Path generateThreadDumpReport() throws IOException
    {
        String report = getThreadDumpReport();
        Path logDirectory = mUserPreferences.getDirectoryPreference().getDirectoryApplicationLog();
        String file = TimeStamp.getFileFormattedDateTime() + "_sdrtrunk_thread_dump.log";
        Path output = logDirectory.resolve(file);
        Files.write(output, report.getBytes());
        return output;
    }

    /**
     * Creates a thread dump report.
     * @return report text.
     */
    public String getThreadDumpReport()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Thread Dump Report\n\n");

        for(Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet())
        {
            sb.append(entry.getKey() + " " + entry.getKey().getState()).append("\n");

            for(StackTraceElement ste : entry.getValue())
            {
                sb.append("\tat " + ste).append("\n");
            }

            sb.append("\n\n");
        }

        return sb.toString();
    }

    /**
     * Generates a JVM and application environment report
     */
    public String getEnvironmentReport()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("JVM and Application Environment Report\n");
        Attributes atts = findManifestAttributes();
        if (atts != null) {
            sb.append("\nVersion  : " + atts.getValue("Implementation-Version"));
            sb.append("\nGradle Version    : " + atts.getValue("Created-By"));
            sb.append("\nBuild Timestamp   : " + atts.getValue("Build-Timestamp"));
            sb.append("\nBuild-JDK         : " + atts.getValue("Build-JDK"));
            sb.append("\nBuild OS          : " + atts.getValue("Build-OS"));
        }
        else
        {
            sb.append("\nApplication:       no build information available");
        }

        sb.append("\nHost OS Name:          " + System.getProperty("os.name"));
        sb.append("\nHost OS Arch:          " + System.getProperty("os.arch"));
        sb.append("\nHost OS Version:       " + System.getProperty("os.version"));
        sb.append("\nHost CPU Cores:        " + Runtime.getRuntime().availableProcessors());
        sb.append("\nHost Max Java Memory:  " + FileUtils.byteCountToDisplaySize(Runtime.getRuntime().maxMemory()));
        sb.append("\nHost Allocated Memory: " + FileUtils.byteCountToDisplaySize(Runtime.getRuntime().totalMemory()));
        sb.append("\nHost Free Memory:      " + FileUtils.byteCountToDisplaySize(Runtime.getRuntime().freeMemory()));
        sb.append("\nHost Used Memory:      " + FileUtils.byteCountToDisplaySize(Runtime.getRuntime().totalMemory() -
                Runtime.getRuntime().freeMemory()));
        sb.append("\nStorage Directories:");
        sb.append("\n Application Root: " + mUserPreferences.getDirectoryPreference().getDirectoryApplicationRoot());
        sb.append("\n Application Log:  " + mUserPreferences.getDirectoryPreference().getDirectoryApplicationLog());
        sb.append("\n Event Log:        " + mUserPreferences.getDirectoryPreference().getDirectoryEventLog());
        sb.append("\n Playlist:         " + mUserPreferences.getDirectoryPreference().getDirectoryPlaylist());
        sb.append("\n Recordings:       " + mUserPreferences.getDirectoryPreference().getDirectoryRecording());
        sb.append("\n");
        return sb.toString();
    }

    /**
     * Finds the jar manifest attributes
     * @return attributes or null.
     */
    public Attributes findManifestAttributes() {
        try {
            Enumeration<URL> resources = getClass().getClassLoader().getResources("META-INF/MANIFEST.MF");
            while (resources.hasMoreElements()) {
                try {
                    Manifest manifest = new Manifest(resources.nextElement().openStream());
                    Attributes atts = manifest.getMainAttributes();
                    Boolean hasTitle = atts.containsValue("sdrtrunk project");
                    if (hasTitle) {
                        return atts;
                    }
                } catch (IOException E) {
                    return null;
                }
            }
        }
        catch (Exception ex)
        {
            return null;
        }

        return null;
    }

    /**
     * Runnable to periodically check for blocked threads
     */
    public class BlockedThreadMonitor implements Runnable
    {
        @Override
        public void run()
        {
            try
            {
                checkForBlockedThreads();
            }
            catch(Throwable t)
            {
                LOG_SUPPRESSOR.error("Error", 3, "Error while checking for blocked threads", t);
            }

            try
            {
                checkForMemoryPressure();
            }
            catch(Throwable t)
            {
                LOG_SUPPRESSOR.error("Memory Error", 3, "Error while checking memory pressure", t);
            }
        }
    }

    //Memory pressure detection: sustained near-full heap precedes OOM death-by-GC-thrash.
    //Threshold checks run on the 30-second diagnostic cadence; ten consecutive over-threshold
    //samples (~5 minutes) trigger remediation.
    private static final double MEMORY_PRESSURE_THRESHOLD = 0.92;
    private static final int MEMORY_PRESSURE_TRIGGER_COUNT = 10;
    private int mMemoryPressureCount = 0;
    private boolean mMemoryPressureAlerted = false;

    /**
     * Detects sustained heap exhaustion.  In headless mode the application performs a pre-emptive
     * exit (non-zero code) so the watchdog/systemd restarts a fresh instance before the JVM
     * degrades into a GC-thrashing zombie.  In GUI mode the user is alerted instead.
     * Opt out with -Dsdrtrunk.memory.autoRestart=false or SDRTRUNK_MEMORY_AUTORESTART=false.
     */
    private void checkForMemoryPressure()
    {
        Runtime runtime = Runtime.getRuntime();
        long max = runtime.maxMemory();
        long used = runtime.totalMemory() - runtime.freeMemory();
        double usage = (double)used / (double)max;

        if(usage < MEMORY_PRESSURE_THRESHOLD)
        {
            mMemoryPressureCount = 0;
            return;
        }

        //Confirm pressure is real by requesting a GC on the first over-threshold sample
        if(mMemoryPressureCount == 0)
        {
            System.gc();
        }

        mMemoryPressureCount++;

        if(mMemoryPressureCount < MEMORY_PRESSURE_TRIGGER_COUNT)
        {
            return;
        }

        mMemoryPressureCount = 0;

        String autoRestart = System.getProperty("sdrtrunk.memory.autoRestart",
            System.getenv("SDRTRUNK_MEMORY_AUTORESTART"));
        boolean autoRestartEnabled = autoRestart == null || !autoRestart.equalsIgnoreCase("false");

        LOGGER.error("Sustained memory pressure detected - heap usage [" + (int)(usage * 100) +
            "%] of [" + (max / (1024 * 1024)) + "MB] for ~5 minutes");

        if(mHeadless && autoRestartEnabled)
        {
            LOGGER.error("Performing pre-emptive restart before memory exhaustion - " +
                "the watchdog/service manager will start a fresh instance");
            io.github.dsheirer.eventbus.MyEventBus.getGlobalEventBus().post(
                new io.github.dsheirer.health.SystemHealthAlertEvent(
                    io.github.dsheirer.health.SystemHealthAlertEvent.AlertType.SYSTEM,
                    "Pre-emptive Memory Restart",
                    "Heap usage exceeded " + (int)(MEMORY_PRESSURE_THRESHOLD * 100) +
                        "% for 5 minutes - restarting before memory exhaustion."));

            new Thread(() -> {
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                System.exit(2);
            }).start();
        }
        else if(!mMemoryPressureAlerted)
        {
            mMemoryPressureAlerted = true;
            io.github.dsheirer.eventbus.MyEventBus.getGlobalEventBus().post(
                new io.github.dsheirer.health.SystemHealthAlertEvent(
                    io.github.dsheirer.health.SystemHealthAlertEvent.AlertType.SYSTEM,
                    "Memory Pressure",
                    "Heap usage has exceeded " + (int)(MEMORY_PRESSURE_THRESHOLD * 100) +
                        "% for 5 minutes - consider increasing the memory allocation in " +
                        "preferences or restarting the application."));
        }
    }
}
