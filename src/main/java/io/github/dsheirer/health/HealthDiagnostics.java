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

package io.github.dsheirer.health;

/**
 * Deterministic System Health diagnostics: maps recognized SDRTrunk log signatures (stack traces, hardware error
 * codes, RF telemetry) to a plain-language explanation and an actionable remediation pathway.
 *
 * <p>This is the rules engine behind the Log Detail "health card" overlay: it turns cryptic, developer-level log
 * lines into something an average radio enthusiast or public-safety administrator can act on, without calling any
 * external AI service.  It is a pure function (no I/O, no UI) so it can be unit tested and reused anywhere a log
 * line needs interpretation.</p>
 *
 * <p>The signature catalog covers the dominant SDRTrunk failure modes: USB/LibUSB hardware abstraction failures,
 * JVM heap and native-thread exhaustion, CPU/USB bus saturation and dropped samples, audio-device/sample-rate
 * mismatches, RF synchronization loss / oscillator drift, JMBE vocoder dependency failures, playlist/alias
 * configuration faults, SIMD/vector calibration, and non-fatal parsing drops.</p>
 */
public final class HealthDiagnostics
{
    /**
     * Severity of a diagnosis, used to prioritize and color the health card.
     */
    public enum Severity
    {
        CRITICAL,   //Causes (or will imminently cause) an application crash or total loss of function.
        WARNING,    //Degrades performance or reliability; should be addressed.
        INFO        //Informational / non-fatal; no user action required.
    }

    /**
     * A structured diagnosis: a short title, a plain-language analysis of the cause, and the concrete action the
     * user should take.
     *
     * @param severity how serious the condition is
     * @param title short headline (e.g. "Critical Memory Exhaustion")
     * @param analysis plain-language explanation of the likely root cause
     * @param action concrete remediation step(s) the user should take
     */
    public record Diagnosis(Severity severity, String title, String analysis, String action)
    {
        /**
         * Renders the diagnosis as the plain text shown in the Log Detail inspector.
         */
        public String toInspectorText()
        {
            StringBuilder sb = new StringBuilder();
            sb.append(title).append("\n\n").append(analysis);

            if(action != null && !action.isBlank())
            {
                sb.append("\n\nFix: ").append(action);
            }

            return sb.toString();
        }
    }

    private HealthDiagnostics()
    {
    }

    /**
     * Returns a structured {@link Diagnosis} for a recognized log message, or null when there is no specific
     * guidance for it.
     *
     * @param message the log message text (any case)
     * @return a diagnosis or null
     */
    public static Diagnosis diagnose(String message)
    {
        if(message == null)
        {
            return null;
        }

        String m = message.toLowerCase();

        // ---------------------------------------------------------------------
        // JVM resource exhaustion (check the native-thread case before generic heap).
        // ---------------------------------------------------------------------
        if(m.contains("outofmemoryerror") && m.contains("unable to create native thread"))
        {
            return new Diagnosis(Severity.CRITICAL, "Out of Native Threads",
                "The operating system refused to create more threads for the per-channel decoders. The system " +
                    "thread limit is saturated, often from monitoring too many channels at once.",
                "Reduce the number of active/Max Traffic Channels, close other heavy apps (e.g. Chrome), and on " +
                    "Linux raise the limit (kernel.threads-max / ulimit -u). Then restart SDRTrunk.");
        }
        if(m.contains("unable to create native thread"))
        {
            return new Diagnosis(Severity.CRITICAL, "Out of Native Threads",
                "The operating system refused SDRTrunk's request to start another decoding thread - the " +
                    "system-wide thread limit is saturated.",
                "Reduce the number of active decoding channels (Max Traffic Channels), close non-essential " +
                    "applications, and on Linux raise kernel.threads-max / ulimit -u.");
        }
        if(m.contains("outofmemoryerror") || m.contains("java heap space"))
        {
            return new Diagnosis(Severity.CRITICAL, "Critical Memory Exhaustion",
                "The application ran out of allocated Java heap memory, usually from monitoring wide spectrum, " +
                    "too many channels, or the waterfall/spectrum display at once.",
                "Increase the memory allocation in User Preferences > Memory (or set -Xmx4096m / -Xmx6G in the " +
                    "startup script), then restart SDRTrunk. Disabling the spectrum/waterfall also helps.");
        }

        // ---------------------------------------------------------------------
        // USB / LibUSB hardware abstraction.
        // ---------------------------------------------------------------------
        if(m.contains("libusb_error_no_device") || m.contains("-4/libusb") || m.contains("[-4]"))
        {
            return new Diagnosis(Severity.CRITICAL, "Tuner Driver Not Installed",
                "LibUSB could not open the tuner (error -4 / NO_DEVICE). On Windows this almost always means the " +
                    "WinUSB driver isn't bound to the device, or the dongle is unplugged / lacks permissions.",
                "Launch Zadig, choose Options > List All Devices, select the tuner's bulk interface, and install " +
                    "the WinUSB driver. Then reconnect the tuner and restart SDRTrunk.");
        }
        if(m.contains("[-99]") || (m.contains("-99") && m.contains("usb")))
        {
            return new Diagnosis(Severity.CRITICAL, "USB Device List Failed to Initialize",
                "LibUSB failed to initialize the USB device-list pointer (error -99), commonly caused by a driver " +
                    "conflict or corruption on the host.",
                "In Zadig choose 'List All Devices' and reinstall WinUSB for the tuner, or move the tuner to a " +
                    "different physical USB controller, then restart SDRTrunk.");
        }
        if(m.contains("failing to set the usb config") || m.contains("could not claim") || m.contains("libusb_error_busy"))
        {
            return new Diagnosis(Severity.CRITICAL, "Tuner Already In Use",
                "The tuner has already been claimed by another program, so SDRTrunk cannot configure it.",
                "Close any other SDR software (SDR++, SDRSharp, Unitrunker, rtl_tcp, etc.) that may be holding the " +
                    "device, then restart SDRTrunk.");
        }
        if(m.contains("data over run") || m.contains("transfer buffers exhausted") ||
           (m.contains("dropped") && m.contains("sample")))
        {
            return new Diagnosis(Severity.WARNING, "USB Bus Saturation - Dropped Samples",
                "The USB controller or CPU can't move the wideband sample stream fast enough, so samples are being " +
                    "thrown away. Dropped samples break the bitstream and cause garbled audio and CRC/false decodes.",
                "Lower the tuner sample rate, run fewer tuners on one root hub, or move the dongle to a dedicated " +
                    "USB controller. Disabling the spectrum/waterfall display also frees bandwidth.");
        }
        if(m.contains("processor overloaded") || m.contains("cannot keep up"))
        {
            return new Diagnosis(Severity.WARNING, "Processing Overload - Dropped Samples",
                "The CPU can't process the incoming sample rate fast enough, so samples are being dropped.",
                "Lower the tuner sample rate, reduce the number of simultaneously decoding channels, prefer the " +
                    "Polyphase channelizer, disable the waterfall, or close other CPU-heavy programs.");
        }

        // ---------------------------------------------------------------------
        // Audio subsystem.
        // ---------------------------------------------------------------------
        if(m.contains("audio format not supported") || (m.contains("mixer") && m.contains("not supported")))
        {
            return new Diagnosis(Severity.WARNING, "Audio Output Device Incompatible",
                "The selected audio device (often a default HDMI output) doesn't support the format SDRTrunk needs, " +
                    "so playback is silent.",
                "Open User Preferences and select a compatible analog output or a virtual audio cable instead of " +
                    "the default device.");
        }

        // ---------------------------------------------------------------------
        // RF synchronization / oscillator drift.
        // ---------------------------------------------------------------------
        if(m.contains("sync loss"))
        {
            return new Diagnosis(Severity.WARNING, "Control Channel Sync Loss",
                "The tuner is losing physical lock on the control channel. Common causes are oscillator (PPM) " +
                    "drift, front-end overload from Automatic Gain Control in a noisy environment, or dropped " +
                    "samples from USB/CPU saturation.",
                "Set the tuner gain to a fixed mid value (disable Automatic Gain Control), calibrate the PPM offset, " +
                    "and lower the sample rate if drops persist.");
        }

        // ---------------------------------------------------------------------
        // JMBE vocoder dependency (P25 / DMR).
        // ---------------------------------------------------------------------
        if(m.contains("getaudiocodec") || (m.contains("jmbe") && (m.contains("not configured") ||
            m.contains("missing") || m.contains("silent") || m.contains("null"))))
        {
            return new Diagnosis(Severity.CRITICAL, "JMBE Vocoder Not Available",
                "Digital voice (P25 / DMR) needs the optional JMBE library, which isn't installed or its path is " +
                    "invalid - so digital channels decode but produce no audio (or throw a codec error).",
                "Open User Preferences > JMBE Audio Library and run the compiler wizard to build and select the " +
                    "JMBE jar. A matching Java Development Kit (consistent JAVA_HOME / Path) is required to compile it.");
        }
        if(m.contains("javac") && (m.contains("not recognized") || m.contains("not found") || m.contains("command")))
        {
            return new Diagnosis(Severity.WARNING, "Java Compiler Not On Path",
                "The JMBE library couldn't be compiled because the javac compiler wasn't found, usually from " +
                    "conflicting or duplicated JAVA_HOME / Path entries pointing at different Java versions.",
                "Ensure a single, valid JDK is on the Path and JAVA_HOME points to it (remove duplicate/recursive " +
                    "entries), then re-run the JMBE compiler wizard.");
        }

        // ---------------------------------------------------------------------
        // Playlist / alias configuration faults.
        // ---------------------------------------------------------------------
        if(m.contains("getaliaslist") || m.contains("aliasdirectory"))
        {
            return new Diagnosis(Severity.CRITICAL, "Auto-Start Channel Missing Alias List",
                "An auto-start channel was enabled without an alias list assigned, which throws a " +
                    "NullPointerException during startup and halts the application.",
                "Assign an alias list to the channel (or disable auto-start). The Playlist linter can disable the " +
                    "offending channel automatically.");
        }

        // ---------------------------------------------------------------------
        // Audio buffer management leak.
        // ---------------------------------------------------------------------
        if(m.contains("decrementusercount") || m.contains("reusablebuffer"))
        {
            return new Diagnosis(Severity.WARNING, "Audio Buffer Management Fault",
                "An audio buffer was released more times than it was used, indicating the JMBE codec lost sync with " +
                    "the bitstream and the audio pipeline may be leaking buffers.",
                "If this repeats frequently, restart SDRTrunk to flush the corrupted buffer state and restore audio.");
        }

        // ---------------------------------------------------------------------
        // SIMD / vector calibration.
        // ---------------------------------------------------------------------
        if(m.contains("vector") && (m.contains("uncalibrated") || m.contains("not calibrated") ||
            m.contains("disabled") || m.contains("calibration")))
        {
            return new Diagnosis(Severity.INFO, "SIMD Vector Operations Not Calibrated",
                "The CPU's SIMD vector math (which accelerates DSP) hasn't been calibrated, so SDRTrunk may be using " +
                    "the slower scalar path.",
                "Run the vector calibration in User Preferences so the optimal computational path is selected.");
        }

        // ---------------------------------------------------------------------
        // USB power management / unexpected shutdown.
        // ---------------------------------------------------------------------
        if(m.contains("selective suspend") || (m.contains("usb") && m.contains("power")))
        {
            return new Diagnosis(Severity.WARNING, "USB Power Management Risk",
                "Windows USB selective suspend can power down the tuner port to save energy, abruptly stopping " +
                    "decoding.",
                "In Device Manager, open each USB Root Hub and the tuner, and uncheck 'Allow the computer to turn " +
                    "off this device to save power'.");
        }

        // ---------------------------------------------------------------------
        // Non-fatal parsing drops (suppress noise; reassure the user).
        // ---------------------------------------------------------------------
        if(m.contains("ambtc") || (m.contains("getmessage") && m.contains("null")))
        {
            return new Diagnosis(Severity.INFO, "Unreadable Data Packet Dropped",
                "An over-the-air data packet was empty or malformed and was safely discarded. This is a transient " +
                    "RF/parsing event, not a configuration problem.",
                null);
        }

        // ---------------------------------------------------------------------
        // Streaming / network integration.
        // ---------------------------------------------------------------------
        if(m.contains("websockethandshakeexception") || (m.contains("zello") && m.contains("failed")))
        {
            return new Diagnosis(Severity.WARNING, "Streaming Connection Failed",
                "A streaming connection (e.g. Zello/Broadcastify) failed to connect.",
                "Verify the stream's network name, username and password, and confirm internet access. SDRTrunk " +
                    "will keep retrying automatically.");
        }
        if(m.contains("address already in use"))
        {
            return new Diagnosis(Severity.WARNING, "Another Instance Is Running",
                "Another copy of SDRTrunk is already running and holding the local network port.",
                "Close the other instance - only one copy should run at a time.");
        }
        if(m.contains("unable to source channel"))
        {
            return new Diagnosis(Severity.WARNING, "No Tuner Available For Channel",
                "No tuner could provide this channel's frequency - either no enabled tuner covers it, or the " +
                    "tuners are at capacity / out of instantaneous bandwidth.",
                "Enable a tuner that covers the frequency, reduce competing channels, or add another tuner.");
        }
        if(m.contains("no sdr tuner detected") || m.contains("no tuner available"))
        {
            return new Diagnosis(Severity.CRITICAL, "No SDR Tuner Found",
                "No SDR tuner was detected. The device may be unplugged, missing its driver, or in use by another " +
                    "program.",
                "Check the connection, install the correct driver (WinUSB via Zadig on Windows), and close other " +
                    "SDR software.");
        }
        if(m.contains("gc pressure") || m.contains("memory at"))
        {
            return new Diagnosis(Severity.WARNING, "Low Memory / GC Pressure",
                "The application is running low on memory and spending time garbage-collecting.",
                "Increase the allocated memory in User Preferences > Memory, then restart SDRTrunk.");
        }

        // ---------------------------------------------------------------------
        // Informational events (reassure; no action).
        // ---------------------------------------------------------------------
        if(m.contains("auto-repairing stream"))
        {
            return new Diagnosis(Severity.INFO, "Stream Auto-Reconnecting",
                "A stream dropped into an error state and is reconnecting automatically.",
                "No action needed unless it recurs, which usually points to bad credentials or a network issue.");
        }
        if(m.contains("configuration backup created"))
        {
            return new Diagnosis(Severity.INFO, "Configuration Backed Up",
                "A routine automatic backup of your configuration was saved.", null);
        }
        if(m.contains("auto-optimized sample rate"))
        {
            return new Diagnosis(Severity.INFO, "Sample Rate Auto-Optimized",
                "SDRTrunk chose a tuner sample rate that covers the active channels while using less CPU.", null);
        }

        return null;
    }

    /**
     * Returns the plain-language inspector text for a recognized log message, or null when there is no specific
     * guidance.  Convenience wrapper around {@link #diagnose(String)}.
     */
    public static String explain(String message)
    {
        Diagnosis diagnosis = diagnose(message);
        return diagnosis != null ? diagnosis.toInspectorText() : null;
    }
}
