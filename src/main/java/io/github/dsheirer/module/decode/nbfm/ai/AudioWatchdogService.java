package io.github.dsheirer.module.decode.nbfm.ai;

import io.github.dsheirer.preference.UserPreferences;

/**
 * Inert no-op stub (retained for API compatibility).
 *
 * <p>This watchdog previously ran a per-channel 1-second timer that treated a channel being quiet as a
 * hardware fault: after a few seconds of "I/Q silence" or "zero-amplitude audio" it logged a "Soft USB
 * Reset" / "JMBE flush" and fired a blocking Gemini HTTP call to ask whether the silence was "terminal".
 * For scanner channels - which are silent the great majority of the time, and whose audio is legitimately
 * zero whenever squelch is closed - this produced thousands of false WARN/ERROR log lines per day (over
 * 8,000 in a single 24-hour capture) and a stream of needless API calls, while the advertised remediation
 * ({@code triggerSoftUsbReset()} / {@code triggerJmbeFlush()}) was never actually implemented - both were
 * empty methods that only logged.</p>
 *
 * <p>Genuine tuner stalls and USB faults are already detected and recovered by the tuner/USB layer
 * ({@code DiscoveredTuner} recovery), so this audio-level watchdog added cost and log noise with no
 * benefit.  It is intentionally neutralized here: no scheduler thread is started and the feed methods do
 * nothing.  The public API is kept so callers (e.g. {@code NBFMDecoder}) compile and run unchanged.</p>
 */
public class AudioWatchdogService
{
    /**
     * Constructs an inert watchdog.  No background thread or timer is started.
     * @param prefs user preferences (unused; retained for API compatibility).
     */
    public AudioWatchdogService(UserPreferences prefs)
    {
        //Intentionally does nothing - see class documentation.
    }

    /**
     * No-op.  Retained so the decoder can feed I/Q without a null check change.
     * @param iqSamples ignored.
     */
    public void feedIQData(float[] iqSamples)
    {
        //Intentionally does nothing.
    }

    /**
     * No-op.  Retained so the decoder can feed audio without a null check change.
     * @param audioSamples ignored.
     */
    public void feedAudioData(float[] audioSamples)
    {
        //Intentionally does nothing.
    }

    /**
     * No-op.  There is no background task to cancel.
     */
    public void shutdown()
    {
        //Intentionally does nothing.
    }
}
