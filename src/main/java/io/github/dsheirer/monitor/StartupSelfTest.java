/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.health.SystemHealthAlertEvent;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates the critical receive-and-hear-audio prerequisites shortly after startup and reports
 * failures in plain language, so a broken installation (missing JMBE after an update, no audio
 * device, unwritable recording directory, no tuner) is surfaced immediately instead of being
 * discovered as silence hours later.
 */
public class StartupSelfTest
{
    private static final Logger mLog = LoggerFactory.getLogger(StartupSelfTest.class);

    private StartupSelfTest()
    {
    }

    /**
     * Runs all checks.  Intended to be invoked once, a short delay after startup so tuner
     * discovery has completed.
     * @return list of failure descriptions (empty when all checks pass)
     */
    public static List<String> run(UserPreferences userPreferences, TunerManager tunerManager)
    {
        List<String> failures = new ArrayList<>();

        //JMBE library: required for P25/DMR voice audio
        try
        {
            Path jmbe = userPreferences.getJmbeLibraryPreference().getPathJmbeLibrary();

            if(jmbe == null || !Files.exists(jmbe))
            {
                failures.add("JMBE audio library is not configured or the file is missing" +
                    (jmbe != null ? " [" + jmbe + "]" : "") +
                    " - digital voice (P25/DMR) will be silent.  Re-run the JMBE setup in the first-time wizard " +
                    "or User Preferences.");
            }
        }
        catch(Exception e)
        {
            failures.add("Unable to verify the JMBE audio library: " + e.getMessage());
        }

        //Audio output device
        try
        {
            boolean hasOutput = false;

            for(Mixer.Info mixerInfo : AudioSystem.getMixerInfo())
            {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);

                if(mixer.getSourceLineInfo() != null && mixer.getSourceLineInfo().length > 0)
                {
                    for(javax.sound.sampled.Line.Info lineInfo : mixer.getSourceLineInfo())
                    {
                        if(SourceDataLine.class.isAssignableFrom(lineInfo.getLineClass()))
                        {
                            hasOutput = true;
                            break;
                        }
                    }
                }

                if(hasOutput)
                {
                    break;
                }
            }

            if(!hasOutput)
            {
                failures.add("No audio output device detected - audio playback will be silent.  " +
                    "Recording and streaming are unaffected.");
            }
        }
        catch(Exception e)
        {
            failures.add("Unable to verify audio output devices: " + e.getMessage());
        }

        //Recording directory writable (recording and streaming both depend on it)
        try
        {
            Path recordingDirectory = userPreferences.getDirectoryPreference().getDirectoryRecording();
            Files.createDirectories(recordingDirectory);
            Path probe = recordingDirectory.resolve(".sdrtrunk_write_test");
            Files.writeString(probe, "test");
            Files.deleteIfExists(probe);
        }
        catch(Exception e)
        {
            failures.add("Recording directory is not writable [" + e.getMessage() +
                "] - recording and streaming will fail.");
        }

        //At least one tuner
        try
        {
            if(tunerManager.getAvailableTuners().isEmpty())
            {
                failures.add("No SDR tuner detected - check that the device is plugged in and recognized " +
                    "by the operating system.");
            }
        }
        catch(Exception e)
        {
            failures.add("Unable to verify tuner availability: " + e.getMessage());
        }

        if(failures.isEmpty())
        {
            mLog.info("Startup self-test passed: JMBE, audio output, recording directory, and tuner all OK");
        }
        else
        {
            for(String failure : failures)
            {
                mLog.warn("Startup self-test: " + failure);
            }

            MyEventBus.getGlobalEventBus().post(new SystemHealthAlertEvent(
                SystemHealthAlertEvent.AlertType.SYSTEM,
                "Startup Self-Test Failed",
                failures.size() + " issue(s) detected: " + String.join(" | ", failures)));
        }

        return failures;
    }
}
