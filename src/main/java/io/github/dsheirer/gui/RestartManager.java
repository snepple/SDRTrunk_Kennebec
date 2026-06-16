/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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
package io.github.dsheirer.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Relaunches SDRTrunk by spawning a small helper process that waits for the current process to exit and then starts
 * the application launcher, after which it triggers a clean JavaFX shutdown.  Using a wait-for-exit helper avoids two
 * instances briefly contending for the same tuners/USB devices and single-instance locks.
 */
public final class RestartManager
{
    private final static Logger mLog = LoggerFactory.getLogger(RestartManager.class);

    private RestartManager()
    {
    }

    /**
     * Attempts to restart the application.  Spawns a detached relaunch helper and then requests a clean shutdown via
     * {@link Platform#exit()} (which runs the JavaFX Application stop() / processShutdown() path).
     * @return true if the relaunch helper was started; false if no launcher could be located.
     */
    public static boolean restart()
    {
        try
        {
            String userDir = System.getProperty("user.dir");
            boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");

            File launcher = findLauncher(userDir, windows);

            if(launcher == null)
            {
                mLog.warn("Restart requested but no SDRTrunk launcher could be located under [{}] - manual restart required", userDir);
                return false;
            }

            long pid = ProcessHandle.current().pid();
            List<String> command = buildRelaunchCommand(launcher, userDir, windows, pid);

            mLog.info("Restart requested - spawning relaunch helper for launcher [{}] after PID [{}] exits", launcher, pid);
            new ProcessBuilder(command).directory(new File(userDir)).start();

            //Request a clean shutdown; JavaFX will run Application.stop() -> processShutdown().
            Platform.runLater(Platform::exit);
            return true;
        }
        catch(Exception e)
        {
            mLog.error("Error attempting to restart the application", e);
            return false;
        }
    }

    /**
     * Locates the most appropriate launcher to relaunch, checking the common install layouts relative to the working
     * directory.
     */
    private static File findLauncher(String userDir, boolean windows)
    {
        List<File> candidates = new ArrayList<>();
        File base = new File(userDir);

        if(windows)
        {
            candidates.add(new File(base, "bin/sdrtrunk.bat"));
            candidates.add(new File(base, "bin/sdr-trunk.bat"));
            candidates.add(new File(base, "SDRTrunk.exe"));
            candidates.add(new File(base.getParentFile(), "SDRTrunk.exe"));
        }
        else
        {
            candidates.add(new File(base, "bin/sdrtrunk"));
            candidates.add(new File(base, "bin/sdr-trunk"));
        }

        for(File candidate : candidates)
        {
            if(candidate != null && candidate.exists())
            {
                return candidate;
            }
        }

        return null;
    }

    /**
     * Builds an OS-appropriate command that waits for the current process to exit and then starts the launcher.
     */
    private static List<String> buildRelaunchCommand(File launcher, String userDir, boolean windows, long pid)
    {
        List<String> command = new ArrayList<>();

        if(windows)
        {
            String script = "Wait-Process -Id " + pid + " -ErrorAction SilentlyContinue; " +
                    "Start-Process -FilePath '" + launcher.getAbsolutePath() + "' -WorkingDirectory '" + userDir + "'";
            command.add("powershell.exe");
            command.add("-WindowStyle");
            command.add("Hidden");
            command.add("-Command");
            command.add(script);
        }
        else
        {
            String script = "while kill -0 " + pid + " 2>/dev/null; do sleep 0.5; done; " +
                    "\"" + launcher.getAbsolutePath() + "\"";
            command.add("sh");
            command.add("-c");
            command.add(script);
        }

        return command;
    }
}
