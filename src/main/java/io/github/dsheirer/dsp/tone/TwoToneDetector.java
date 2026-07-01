package io.github.dsheirer.dsp.tone;

import io.github.dsheirer.dsp.filter.GoertzelFilter;
import io.github.dsheirer.dsp.window.WindowType;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.playlist.PlaylistV2;
import io.github.dsheirer.alias.Alias;
import java.util.Collection;
import io.github.dsheirer.playlist.TwoToneConfiguration;
import io.github.dsheirer.playlist.TwoToneDiscoveryLog;
import io.github.dsheirer.audio.broadcast.zello.ZelloBroadcaster;
import io.github.dsheirer.audio.broadcast.AbstractAudioBroadcaster;
import io.github.dsheirer.audio.broadcast.BroadcastModel;
import io.github.dsheirer.preference.notification.AntiFloodFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import io.github.dsheirer.audio.AudioSegment;
import io.github.dsheirer.identifier.configuration.FrequencyConfigurationIdentifier;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.audio.broadcast.mqtt.MqttService;
import io.github.dsheirer.eventbus.MyEventBus;
import org.jtransforms.fft.FloatFFT_1D;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.audio.broadcast.mqtt.MqttService;
import io.github.dsheirer.eventbus.MyEventBus;

/**
 * Detects A/B two-tone sequences in a background thread to prevent audio playback stuttering.
 */
public class TwoToneDetector
{
    private static final Logger mLog = LoggerFactory.getLogger(TwoToneDetector.class);

    private static final int SAMPLE_RATE = 8000;
    // 160 samples @ 8kHz is 20ms block size
    private static final int BLOCK_SIZE = 160;

    // A tone has to be present for a minimum duration to be recognized
    private static final int MIN_TONE_DURATION_MS = 300;
    private static final int MIN_TONE_BLOCKS = MIN_TONE_DURATION_MS / 20;
    private static final int LONG_A_MIN_TONE_DURATION_MS = 2000;
    private static final int LONG_A_MIN_TONE_BLOCKS = LONG_A_MIN_TONE_DURATION_MS / 20;

    private static final int POWER_THRESHOLD_DB = 10; // Simple threshold, tune as needed
    //Dominance check: a 20ms Goertzel block only resolves ~100Hz, so a strong off-target tone (or broadband voice)
    //can raise the in-band power above POWER_THRESHOLD_DB even though the real energy is elsewhere (e.g. a 450Hz tone
    //leaks ~19dB into a 349Hz detector and falsely triggered it).  A tone only counts if no off-target probe exceeds
    //the target power by more than DOMINANCE_MARGIN_DB.  Validated to reject the 450->349 case while keeping real
    //on-frequency tones and correctly separating close A/B page tones (e.g. 1669/1530).
    private static final int DOMINANCE_MARGIN_DB = 6;
    private static final long[] DOMINANCE_PROBE_OFFSETS_HZ = {70, 140};
    //Discovery quality floors: real paging tones concentrate energy in a narrow peak (very high peak-to-average);
    //voice/noise spreads it.  The ratio floor was raised from 8 to reject voice formants that produced spurious
    //candidates, and discovered A/B must differ by at least this many Hz so a single drifting formant is not split
    //into a bogus A-then-B pair.
    private static final double DISCOVERY_MIN_PEAK_TO_AVG = 12.0;
    private static final double DISCOVERY_MIN_AB_SEPARATION_HZ = 20.0;

    private final ExecutorService mExecutorService = Executors.newSingleThreadExecutor();
    private final LinkedTransferQueue<AudioBufferWrapper> mAudioQueue = new LinkedTransferQueue<>();
    private final AtomicBoolean mRunning = new AtomicBoolean(true);

    private final PlaylistManager mPlaylistManager;
    private final BroadcastModel mBroadcastModel;

    // Per-segment cache for alias-based audio routing.  Alias membership is constant for the lifetime of an audio
    // segment, so we resolve the applicable detectors once per segment instead of for every 20 ms audio block.
    private AudioSegment mLastRoutedSegment;
    private java.util.Set<String> mLastApplicableDetectors = java.util.Collections.emptySet();

    // Bounded routing diagnostics.  Logs the first ROUTING_LOG_LIMIT alias-routing decisions so a user can see, per
    // analog call, which aliases resolved on the segment and therefore which configured detectors were included vs.
    // silently excluded.  This is the definitive way to confirm whether a detector that "isn't working" is simply
    // never being routed the audio (its assigned alias did not resolve on the call's talkgroup).
    private static final int ROUTING_LOG_LIMIT = 60;
    private int mRoutingLogCount = 0;

    // Rate-limited Goertzel power diagnostics.  Logs the first GOERTZEL_DIAG_LIMIT power readings per detector
    // whenever a tone is above threshold, so we can confirm the power normalization is producing correct values.
    private static final int GOERTZEL_DIAG_LIMIT = 10;
    private final java.util.HashMap<String, Integer> mGoertzelDiagCount = new java.util.HashMap<>();

    // Cached Goertzel filters (keyed by target frequency) and a reused windowing scratch buffer, so tone detection
    // does not allocate a filter (with its window coefficients) and a buffer clone on every 20 ms block.  Accessed
    // only from the single detector processing thread.
    private final java.util.HashMap<Long, GoertzelFilter> mGoertzelFilters = new java.util.HashMap<>();
    private final float[] mGoertzelScratch = new float[BLOCK_SIZE];

    // Longer analysis window (~100ms) for tone frequency discrimination.  A 20ms block only resolves ~100Hz; 100ms
    // resolves ~10Hz, approaching the 2% tolerance so close tones (e.g. 349 vs 387) can be told apart and off-target
    // tones/voice no longer leak across.  Separate cached Goertzel filters and scratch buffer at this window size.
    static final int ANALYSIS_WINDOW_SAMPLES = 800; // 100ms @ 8kHz
    private static final int MAX_ANALYSIS_SEGMENTS = 64;
    private final java.util.HashMap<Long, GoertzelFilter> mAnalysisGoertzelFilters = new java.util.HashMap<>();
    private final float[] mAnalysisScratch = new float[ANALYSIS_WINDOW_SAMPLES];

    // Per-segment rolling analysis window (newest samples at the end).  Per-segment so interleaved audio from
    // concurrent channels is never mixed into one measurement.  Bounded LRU to avoid leaking AudioSegment references.
    private static final class AnalysisWindow
    {
        final float[] buffer = new float[ANALYSIS_WINDOW_SAMPLES];
        int filled = 0;

        void append(float[] block)
        {
            int n = block.length;
            if(n >= ANALYSIS_WINDOW_SAMPLES)
            {
                System.arraycopy(block, n - ANALYSIS_WINDOW_SAMPLES, buffer, 0, ANALYSIS_WINDOW_SAMPLES);
                filled = ANALYSIS_WINDOW_SAMPLES;
            }
            else
            {
                System.arraycopy(buffer, n, buffer, 0, ANALYSIS_WINDOW_SAMPLES - n); // shift older samples left
                System.arraycopy(block, 0, buffer, ANALYSIS_WINDOW_SAMPLES - n, n);   // append newest at the end
                filled = Math.min(ANALYSIS_WINDOW_SAMPLES, filled + n);
            }
        }

        boolean isFull()
        {
            return filled >= ANALYSIS_WINDOW_SAMPLES;
        }
    }
    private final java.util.Map<AudioSegment, AnalysisWindow> mAnalysisWindowBySegment =
        new java.util.LinkedHashMap<>(16, 0.75f, true)
        {
            @Override
            protected boolean removeEldestEntry(java.util.Map.Entry<AudioSegment, AnalysisWindow> eldest)
            {
                return size() > MAX_ANALYSIS_SEGMENTS;
            }
        };

    // Per-segment audio re-blocking accumulator.  Decoder audio arrives in 512-sample blocks (NBFM/AM RealResampler
    // output) but the detector analyses fixed BLOCK_SIZE (160-sample) blocks, so processAudio() carries each segment's
    // sub-block remainder here until enough samples accumulate for the next analysis block.  Bounded LRU (eldest idle
    // call's tiny remainder is evicted) so a long-running session cannot leak AudioSegment references.  Accessed only
    // from the single audio-manager thread that calls processAudio().
    private static final int MAX_ACCUMULATOR_SEGMENTS = 64;
    private final java.util.Map<AudioSegment, float[]> mAccumulatorBySegment =
        new java.util.LinkedHashMap<>(16, 0.75f, true)
        {
            @Override
            protected boolean removeEldestEntry(java.util.Map.Entry<AudioSegment, float[]> eldest)
            {
                return size() > MAX_ACCUMULATOR_SEGMENTS;
            }
        };

    // Discovery tracking
    public static final List<TwoToneDiscoveryLog> DISCOVERY_LOG = new ArrayList<>();

    // State machine for Tone A -> Tone B.  This MUST be tracked per-detector: a single shared state machine is
    // overwritten when more than one configured detector sees its Tone A in the same 20 ms block.  That happens
    // routinely because two detectors can have nearly-identical Tone A frequencies (e.g. 798.8 Hz and 799.0 Hz),
    // both of which fall within tolerance of the same transmitted tone.  With shared state each detector reset the
    // other's block counter every block, so neither ever reached the minimum hold time and NO detector fired.
    // Keyed by detector name (config.getAlias()); accessed only from the single detector processing thread.
    private static final class ToneState
    {
        double currentToneA = 0.0;
        int currentToneABlocks = 0;
        double currentToneB = 0.0;
        int currentToneBBlocks = 0;
        int gapBlocks = 0;
    }
    private final java.util.Map<String, ToneState> mToneStateByDetector = new java.util.HashMap<>();
    // Per-detector duplicate suppression: maps detector alias → last trigger timestamp (millis)
    private final java.util.Map<String, Long> mLastTriggerTimeByDetector = new java.util.concurrent.ConcurrentHashMap<>();

    // FFT Auto-Discovery.  Like the configured-detector state above, this MUST be tracked per-call: the ring buffer
    // and tone-progress fields were previously shared instance fields, so when several analog channels were active
    // at once their audio interleaved into one ring buffer and the FFT never saw a clean isolated tone.  On a busy
    // multi-channel system that silences discovery entirely (no UNRECOGNIZED/matches lines despite real tone-outs).
    // Each call (AudioSegment) now gets its own ring buffer and tone state.  The FFT instance itself is stateless
    // across calls (it transforms the array passed to realForward) so a single shared instance is fine.  Accessed
    // only from the single detector processing thread.
    private static final int FFT_SIZE = 4096;
    private final FloatFFT_1D mFft = new FloatFFT_1D(FFT_SIZE);

    private static final class DiscoveryState
    {
        final float[] ringBuffer = new float[FFT_SIZE];
        int ringIndex = 0;
        int samplesReceived = 0;
        int fftCounter = 0;
        double currentToneA = 0.0;
        int toneABlocks = 0;
        double currentToneB = 0.0;
        int toneBBlocks = 0;
        int silenceCount = 0;
    }

    // Bounded LRU map of per-call discovery state.  Capacity comfortably exceeds the number of simultaneously active
    // analog channels; the eldest idle entry is evicted when exceeded (a re-activated call just restarts discovery).
    private static final int MAX_DISCOVERY_SEGMENTS = 64;
    private final java.util.Map<AudioSegment, DiscoveryState> mDiscoveryStateBySegment =
        new java.util.LinkedHashMap<>(16, 0.75f, true)
        {
            @Override
            protected boolean removeEldestEntry(java.util.Map.Entry<AudioSegment, DiscoveryState> eldest)
            {
                return size() > MAX_DISCOVERY_SEGMENTS;
            }
        };

    // Bounded diagnostic: logs the first DISCOVERY_FFT_LOG_LIMIT FFT windows the discovery path evaluates, with the
    // observed peak frequency, peak magnitude, and peak-to-average ratio, plus whether it cleared the detection floors
    // (magnitude > 5000 AND ratio > 8).  This confirms the detector is running on real audio and reveals whether the
    // absolute magnitude floor is too high for weak signals (the AdaptiveGain advisor reports most channels well below
    // optimal level), in which case the floor can be lowered or made relative.
    private static final int DISCOVERY_FFT_LOG_LIMIT = 40;
    private int mDiscoveryFftLogCount = 0;

    public TwoToneDetector(PlaylistManager playlistManager)
    {
        mPlaylistManager = playlistManager;
        mBroadcastModel = (playlistManager != null) ? playlistManager.getBroadcastModel() : null;

        mExecutorService.submit(() -> {
            while (mRunning.get())
            {
                try
                {
                    AudioBufferWrapper wrapper = mAudioQueue.take();
                    processBuffer(wrapper.buffer, wrapper.segment);
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
                catch (Exception e)
                {
                    mLog.error("Error in TwoToneDetector", e);
                }
            }
        });
    }

    public void processAudio(float[] buffer, AudioSegment segment)
    {
        if(buffer == null || buffer.length == 0)
        {
            return;
        }

        //Re-block the incoming audio into the detector's fixed BLOCK_SIZE (160-sample / 20 ms @ 8 kHz) analysis
        //blocks.  The NBFM/AM decoders emit audio in 512-sample blocks (RealResampler output length), so requiring an
        //exact 160-sample buffer here previously dropped EVERY buffer and no detector ever ran.  Any remainder
        //(< BLOCK_SIZE) is carried per-segment so audio from different concurrent calls is never mixed into one
        //analysis block.  Called only from the single audio-manager thread, so the accumulator map needs no locking.
        float[] leftover = mAccumulatorBySegment.get(segment);
        int leftoverLen = (leftover != null) ? leftover.length : 0;
        int total = leftoverLen + buffer.length;

        if(total < BLOCK_SIZE)
        {
            //Not enough for a full analysis block yet - grow the per-segment remainder and wait for more audio.
            float[] grown = new float[total];
            if(leftoverLen > 0) System.arraycopy(leftover, 0, grown, 0, leftoverLen);
            System.arraycopy(buffer, 0, grown, leftoverLen, buffer.length);
            mAccumulatorBySegment.put(segment, grown);
            return;
        }

        float[] combined = new float[total];
        if(leftoverLen > 0) System.arraycopy(leftover, 0, combined, 0, leftoverLen);
        System.arraycopy(buffer, 0, combined, leftoverLen, buffer.length);

        int fullBlocks = total / BLOCK_SIZE;
        for(int b = 0; b < fullBlocks; b++)
        {
            float[] block = new float[BLOCK_SIZE];
            System.arraycopy(combined, b * BLOCK_SIZE, block, 0, BLOCK_SIZE);
            mAudioQueue.offer(new AudioBufferWrapper(block, segment));
        }

        int remainder = total % BLOCK_SIZE;
        if(remainder > 0)
        {
            float[] rem = new float[remainder];
            System.arraycopy(combined, fullBlocks * BLOCK_SIZE, rem, 0, remainder);
            mAccumulatorBySegment.put(segment, rem);
        }
        else
        {
            mAccumulatorBySegment.remove(segment);
        }
    }

    private void processBuffer(float[] buffer, AudioSegment segment)
    {
        PlaylistV2 playlist = mPlaylistManager.getCurrentPlaylist();
        if(playlist == null) return;

        List<TwoToneConfiguration> configs = playlist.getTwoToneConfigurations();
        boolean discoveryEnabled = isDiscoveryEnabled(playlist, mPlaylistManager);

        if (configs.isEmpty() && !discoveryEnabled)
        {
            return; // Nothing to do
        }

        //Per-call discovery state (a null segment cannot be tracked, so discovery is skipped for it).
        DiscoveryState discovery = (discoveryEnabled && segment != null)
                ? mDiscoveryStateBySegment.computeIfAbsent(segment, s -> new DiscoveryState())
                : null;

        if (discovery != null) {
            for (int i = 0; i < buffer.length; i++) {
                discovery.ringBuffer[discovery.ringIndex] = buffer[i];
                discovery.ringIndex = (discovery.ringIndex + 1) % FFT_SIZE;
                discovery.samplesReceived++;
                discovery.fftCounter++;
            }
        }

        // To make this fully optimized, we would typically run an FFT or a bank of Goertzel filters
        // For simplicity and since we want a "magical" experience, we'll scan known configurations

        //Alias-based routing: a detector only receives this segment's audio when one of its selected aliases
        //resolves on the segment (or when it has no aliases selected, in which case it runs globally for backward
        //compatibility).  resolveSegmentDetectorNames() now searches ALL alias lists — not just the channel's
        //configured list — so a detector in alias list "Somerset" is visible even when the channel uses "Kennebec".
        java.util.Set<String> applicableDetectors = getApplicableDetectorNames(segment, configs);

        for(TwoToneConfiguration config : configs)
        {
            if (!config.isEnabled()) continue;
            if (!applicableDetectors.contains(config.getAlias())) continue;

            //Per-detector tone state so concurrently-tracked detectors cannot clobber each other's progress.
            String detectorKey = config.getAlias() != null ? config.getAlias() : ("@" + System.identityHashCode(config));
            ToneState state = mToneStateByDetector.computeIfAbsent(detectorKey, k -> new ToneState());

            long freqA = Math.round(config.getToneA());
            long freqB = Math.round(config.getToneB());
            // Use percentage-based tolerance (IAmResponding style) with fallback to absolute Hz
            double tolA = config.getEffectiveToleranceHz(config.getToneA());
            double tolB = config.getEffectiveToleranceHz(config.getToneB());
            // Per-tone durations (IAmResponding style) with fallback to legacy single duration.  Enforce an absolute
            // minimum hold of MINIMUM_TONE_DURATION_FLOOR_MS so a tone is never gated below 500ms - this mitigates
            // voice triggering / false positives even if a detector is configured with a very short length.
            double minToneAMs = Math.max(TwoToneConfiguration.MINIMUM_TONE_DURATION_FLOOR_MS, config.getEffectiveToneADurationMs());
            double minToneBMs = Math.max(TwoToneConfiguration.MINIMUM_TONE_DURATION_FLOOR_MS, config.getEffectiveToneBDurationMs());
            int minToneABlocks = Math.max(1, (int)(minToneAMs / 20));
            int minToneBBlocks = Math.max(1, (int)(minToneBMs / 20));
            // Gap tolerance: number of silent blocks allowed between tone A and tone B
            int maxGapBlocks = Math.max(0, (int)(config.getToneGapLengthSec() * 1000 / 20));

            if (freqA <= 0) continue;
            if (!config.isLongATone() && freqB <= 0) continue;

            int powerA = getTolerancePower(buffer, freqA, tolA);

            if (config.isLongATone())
            {
                if (powerA > POWER_THRESHOLD_DB)
                {
                    if(state.currentToneA == config.getToneA())
                    {
                        state.currentToneABlocks++;
                    }
                    else
                    {
                        state.currentToneA = config.getToneA();
                        state.currentToneABlocks = 1;
                    }

                    if(state.currentToneABlocks == LONG_A_MIN_TONE_BLOCKS)
                    {
                        triggerAlertIfMatched(config, segment);
                        // Do not reset state.currentToneABlocks to 0 here.
                        // Allow it to keep incrementing as long as the tone is present
                        // to prevent multiple triggers for the same continuous long tone.
                    }
                }
                else if (state.currentToneA == config.getToneA())
                {
                    state.currentToneA = 0;
                    state.currentToneABlocks = 0;
                }
                continue;
            }

            int powerB = getTolerancePower(buffer, freqB, tolB);

            // Diagnostic: log Goertzel power levels for this detector (rate-limited per detector)
            Integer diagCount = mGoertzelDiagCount.getOrDefault(detectorKey, 0);
            if (diagCount < GOERTZEL_DIAG_LIMIT && (powerA > POWER_THRESHOLD_DB || powerB > POWER_THRESHOLD_DB))
            {
                mGoertzelDiagCount.put(detectorKey, diagCount + 1);
                mLog.info("Goertzel [{}] A:{} Hz pwr={} dB, B:{} Hz pwr={} dB (threshold={}) state: toneABlocks={}/{} toneBBlocks={}/{} gap={}",
                    config.getAlias(), freqA, powerA, freqB, powerB, POWER_THRESHOLD_DB,
                    state.currentToneABlocks, minToneABlocks, state.currentToneBBlocks, minToneBBlocks, state.gapBlocks);
            }

            // --- Tone detection with dominance arbitration ---
            // The old mutually-exclusive if/else structure prevented Tone B from ever being detected when Tone A's
            // Goertzel bin still showed power above threshold.  This is common when Tone B is a harmonic of Tone A
            // (e.g. Sidney Fire: A=799Hz, B=1598Hz = 2×A), because spectral leakage from B bleeds into the A bin.
            //
            // Fix: when BOTH tones show power above threshold, the stronger one wins.  When only one shows power,
            // that one is used.  When neither shows power, count gap blocks if we're between A and B.
            boolean aAboveThreshold = powerA > POWER_THRESHOLD_DB;
            boolean bAboveThreshold = powerB > POWER_THRESHOLD_DB;
            boolean toneADominant = false;
            boolean toneBDominant = false;

            if (aAboveThreshold && bAboveThreshold)
            {
                // Both show power — the stronger one wins.
                if (powerA >= powerB)
                {
                    toneADominant = true;
                }
                else
                {
                    toneBDominant = true;
                }
            }
            else if (aAboveThreshold)
            {
                toneADominant = true;
            }
            else if (bAboveThreshold)
            {
                toneBDominant = true;
            }

            // Tone A counting
            if (toneADominant && state.currentToneBBlocks == 0)
            {
                // Only count Tone A blocks if we haven't started counting Tone B yet
                // (prevents re-detecting A after B has started)
                if(state.currentToneA == config.getToneA())
                {
                    state.currentToneABlocks++;
                }
                else
                {
                    state.currentToneA = config.getToneA();
                    state.currentToneABlocks = 1;
                    state.currentToneB = 0;
                    state.currentToneBBlocks = 0;
                    state.gapBlocks = 0;
                }
            }
            // Tone B counting (only valid if Tone A was previously detected and held long enough)
            else if (toneBDominant && state.currentToneABlocks >= minToneABlocks && state.currentToneA == config.getToneA())
            {
                if(state.currentToneB == config.getToneB())
                {
                    state.currentToneBBlocks++;
                }
                else
                {
                    state.currentToneB = config.getToneB();
                    state.currentToneBBlocks = 1;
                }

                // If B is held long enough, it's a confirmed sequence
                if(state.currentToneBBlocks >= minToneBBlocks)
                {
                    triggerAlertIfMatched(config, segment);

                    // Reset to avoid multiple triggers for the same continuous tone
                    state.currentToneA = 0;
                    state.currentToneABlocks = 0;
                    state.currentToneB = 0;
                    state.currentToneBBlocks = 0;
                    state.gapBlocks = 0;
                }
            }
            // Neither tone dominant — count gap blocks between tones
            else if (state.currentToneABlocks >= minToneABlocks && state.currentToneA == config.getToneA()
                    && state.currentToneBBlocks == 0)
            {
                state.gapBlocks++;
                // If the gap exceeds the configured tolerance, reset
                if (state.gapBlocks > maxGapBlocks + 5) // +5 blocks (100ms) of grace
                {
                    state.currentToneA = 0;
                    state.currentToneABlocks = 0;
                    state.gapBlocks = 0;
                }
            }
        }

        // If in discovery mode, look for strong tones with the FFT path even when a configured detector matched.
        // A true discovery mode would require an FFT to find the strongest peak frequency
        if (discovery != null)
        {
            if (discovery.fftCounter >= 800 && discovery.samplesReceived >= FFT_SIZE) {
                discovery.fftCounter = 0;
                float[] fftBuffer = new float[FFT_SIZE];
                for (int i = 0; i < FFT_SIZE; i++) {
                    fftBuffer[i] = discovery.ringBuffer[(discovery.ringIndex + i) % FFT_SIZE];
                }

                mFft.realForward(fftBuffer);
                
                double maxMagnitude = 0;
                int maxIndex = -1;
                double magnitudeSum = 0;
                int binCount = FFT_SIZE / 2 - 1;
                for (int i = 1; i < FFT_SIZE / 2; i++) {
                    float re = fftBuffer[2 * i];
                    float im = fftBuffer[2 * i + 1];
                    double mag = re * re + im * im;
                    magnitudeSum += mag;
                    if (mag > maxMagnitude) {
                        maxMagnitude = mag;
                        maxIndex = i;
                    }
                }

                // Require the peak to be above a magnitude floor AND significantly above the average.
                // A pure two-tone paging tone concentrates energy in a narrow peak; broadband noise or
                // voice spreads energy evenly, producing a low peak-to-average ratio.
                double avgMagnitude = magnitudeSum / binCount;
                double peakToAvgRatio = (avgMagnitude > 0) ? (maxMagnitude / avgMagnitude) : 0;

                if (mDiscoveryFftLogCount < DISCOVERY_FFT_LOG_LIMIT) {
                    mDiscoveryFftLogCount++;
                    double peakFreq = (maxIndex > 0) ? Math.round((double) maxIndex * SAMPLE_RATE / FFT_SIZE) : 0;
                    mLog.debug("TwoTone discovery FFT: peakFreq={}Hz peakMag={} ratio={} passesFloors={} (need mag>5000 AND ratio>{})",
                            peakFreq, String.format("%.0f", maxMagnitude), String.format("%.1f", peakToAvgRatio),
                            (maxMagnitude > 5000.0 && peakToAvgRatio > DISCOVERY_MIN_PEAK_TO_AVG),
                            String.format("%.0f", DISCOVERY_MIN_PEAK_TO_AVG));
                }

                if (maxMagnitude > 5000.0 && peakToAvgRatio > DISCOVERY_MIN_PEAK_TO_AVG) { // strong peak AND concentrated energy
                    double frequency = (double) maxIndex * SAMPLE_RATE / FFT_SIZE;
                    frequency = Math.round(frequency);
                    
                    //Reset the silence counter whenever a valid tone window is seen.
                    discovery.silenceCount = 0;

                    if (frequency >= 300 && frequency <= 3000) {
                        if (discovery.currentToneB > 0) {
                            if (Math.abs(discovery.currentToneB - frequency) < 5) {
                                discovery.toneBBlocks++;
                                if (discovery.toneBBlocks >= 3) { // 3 * 100ms = 300ms — matches MIN_TONE_DURATION_MS
                                    logDiscovery(discovery.currentToneA, discovery.currentToneB, segment);
                                    discovery.currentToneA = 0;
                                    discovery.toneABlocks = 0;
                                    discovery.currentToneB = 0;
                                    discovery.toneBBlocks = 0;
                                }
                            } else {
                                discovery.currentToneB = 0;
                                discovery.toneBBlocks = 0;
                            }
                        } else if (discovery.currentToneA > 0) {
                            if (Math.abs(discovery.currentToneA - frequency) < 5) {
                                discovery.toneABlocks++;
                            } else if (discovery.toneABlocks >= 3 &&
                                    Math.abs(discovery.currentToneA - frequency) >= DISCOVERY_MIN_AB_SEPARATION_HZ) {
                                // Tone A was held long enough and this is a genuinely different frequency: treat it as
                                // Tone B.  Requiring a minimum A/B separation prevents a single drifting formant from
                                // being split into a bogus A-then-B candidate.
                                discovery.currentToneB = frequency;
                                discovery.toneBBlocks = 1;
                            } else {
                                discovery.currentToneA = frequency;
                                discovery.toneABlocks = 1;
                            }
                        } else {
                            discovery.currentToneA = frequency;
                            discovery.toneABlocks = 1;
                        }
                    }
                } else {
                    // No strong tone in this 100ms window.  Two-tone paging has a brief gap between
                    // tone A and tone B, so a single quiet window must NOT reset tone A state.
                    // Only reset after sustained silence (3+ consecutive non-tone windows = 300ms).
                    discovery.silenceCount++;
                    if (discovery.silenceCount >= 3) {
                        discovery.currentToneA = 0;
                        discovery.toneABlocks = 0;
                        discovery.currentToneB = 0;
                        discovery.toneBBlocks = 0;
                    }
                }
            }
        }
    }

    static boolean isDiscoveryEnabled(PlaylistV2 playlist, PlaylistManager playlistManager)
    {
        if(playlist != null && playlist.isToneDiscoveryEnabled())
        {
            return true;
        }

        try
        {
            return playlistManager != null &&
                playlistManager.getUserPreferences() != null &&
                playlistManager.getUserPreferences().getAIPreference() != null &&
                playlistManager.getUserPreferences().getAIPreference().isAIEnabled() &&
                playlistManager.getUserPreferences().getAIPreference().isAIToneDiscoveryEnabled();
        }
        catch(Exception e)
        {
            return false;
        }
    }

    /**
     * Determines which detector configurations should receive this audio segment, based on the alias(es) selected for
     * each detector.  A detector that has one or more aliases selected only receives audio from segments whose resolved
     * alias selected it (alias-routed live audio).  A detector with no aliases selected runs globally against all audio,
     * preserving the legacy behavior.  Results are cached per audio segment because alias membership is constant for the
     * lifetime of a segment and this is evaluated for every 20 ms audio block.
     */
    java.util.Set<String> getApplicableDetectorNames(AudioSegment segment, List<TwoToneConfiguration> configs)
    {
        if(segment == mLastRoutedSegment)
        {
            return mLastApplicableDetectors;
        }

        java.util.Set<String> segmentDetectors = resolveSegmentDetectorNamesWithFallback(segment);
        java.util.Set<String> applicable = new java.util.HashSet<>();

        for(TwoToneConfiguration config : configs)
        {
            String name = config.getAlias();

            if(name == null)
            {
                continue;
            }

            if(segmentDetectors.contains(name))
            {
                applicable.add(name); //An alias selected for this detector resolved on the segment
            }
            //Detectors without alias mappings are NOT added — alias restriction is required.
        }

        mLastRoutedSegment = segment;
        mLastApplicableDetectors = applicable;

        //Diagnostic: surface the routing decision for the first several calls so a user can confirm whether a detector
        //that "isn't working" is actually being fed.  Logs the call's channel, the aliases that resolved on it, the
        //detectors that WILL run, and any configured detectors that were EXCLUDED because their assigned alias did not
        //resolve on this call (the common cause: the channel's talkgroup does not match the alias's talkgroup).
        if(mRoutingLogCount < ROUTING_LOG_LIMIT && !configs.isEmpty())
        {
            mRoutingLogCount++;

            java.util.List<String> excludedNoMatch = new java.util.ArrayList<>();
            java.util.List<String> excludedNoAlias = new java.util.ArrayList<>();
            for(TwoToneConfiguration config : configs)
            {
                if(config.getAlias() != null && config.isEnabled() && !applicable.contains(config.getAlias()))
                {
                    if(!detectorHasAnyAliasMapping(config.getAlias()))
                    {
                        excludedNoAlias.add(config.getAlias());
                    }
                    else
                    {
                        excludedNoMatch.add(config.getAlias());
                    }
                }
            }

            String channel = getSegmentChannelName(segment);

            mLog.info("TwoTone routing [channel={}] resolvedAliasDetectors={} willRun={} excludedNeedAliasMatch={} excludedNoAlias={}",
                    channel != null ? channel : "?", segmentDetectors, applicable, excludedNoMatch, excludedNoAlias);
        }

        return applicable;
    }

    /**
     * Resolves detector names from identifiers first.  If conventional analog audio has no TO/talkgroup or tone
     * identifier, fall back to matching the configured channel name to an alias/detector name in the segment's alias
     * list.  This keeps alias-scoped detectors from being starved on conventional channels while preserving normal
     * identifier-based routing whenever it succeeds.
     */
    private java.util.Set<String> resolveSegmentDetectorNamesWithFallback(AudioSegment segment)
    {
        java.util.Set<String> names = resolveSegmentDetectorNames(segment);

        if(names.isEmpty())
        {
            names.addAll(resolveChannelAliasDetectorNames(segment));
        }

        return names;
    }

    /**
     * Resolves detector names from identifiers by searching ALL aliases globally, not just the channel's
     * configured alias list.  A detector's alias can live in any alias list (e.g. "Somerset") while the
     * channel may use a different list (e.g. "Kennebec").  The old code called
     * {@code getAliasList(segment.getIdentifierCollection())} which only returned aliases from the channel's
     * list, causing cross-list detector aliases to be invisible and never trigger.  This now matches the
     * scope of {@link #detectorHasAnyAliasMapping(String)} which also searches all aliases globally.
     */
    private java.util.Set<String> resolveSegmentDetectorNames(AudioSegment segment)
    {
        java.util.Set<String> names = new java.util.HashSet<>();

        if(segment == null)
        {
            return names;
        }

        try
        {
            //Collect all distinct alias list names so we can search across all of them.
            java.util.Set<String> aliasListNames = new java.util.HashSet<>();
            for(Alias alias : mPlaylistManager.getAliasModel().aliasList())
            {
                if(alias.hasList())
                {
                    aliasListNames.add(alias.getAliasListName());
                }
            }

            //Search each alias list for aliases matching this segment's identifiers.
            for(String listName : aliasListNames)
            {
                io.github.dsheirer.alias.AliasList aliasList =
                    mPlaylistManager.getAliasModel().getAliasList(listName);

                if(aliasList != null)
                {
                    for(Identifier identifier : segment.getIdentifierCollection().getIdentifiers())
                    {
                        List<Alias> aliases = aliasList.getAliases(identifier);

                        if(aliases != null)
                        {
                            for(Alias alias : aliases)
                            {
                                for(io.github.dsheirer.alias.id.twotone.TwoToneDetectorID detectorId : alias.getTwoToneDetectors())
                                {
                                    if(detectorId.getDetectorName() != null)
                                    {
                                        names.add(detectorId.getDetectorName());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        catch(Exception e)
        {
            mLog.error("Error resolving aliases for two tone detector audio routing", e);
        }

        return names;
    }

    /**
     * Resolves detector names by matching the segment's configured channel name to aliases in the segment's alias list.
     * This is specifically for conventional analog channels where the call may not contain a TO/talkgroup identifier.
     */
    private java.util.Set<String> resolveChannelAliasDetectorNames(AudioSegment segment)
    {
        java.util.Set<String> names = new java.util.HashSet<>();

        if(segment == null || segment.getIdentifierCollection() == null)
        {
            return names;
        }

        String channelName = getSegmentChannelName(segment);

        if(channelName == null || channelName.trim().isEmpty())
        {
            return names;
        }

        try
        {
            io.github.dsheirer.alias.AliasList aliasList =
                mPlaylistManager.getAliasModel().getAliasList(segment.getIdentifierCollection());

            if(aliasList != null)
            {
                for(Alias alias : aliasList.aliases())
                {
                    boolean aliasMatchesChannel = textMatches(alias.getName(), channelName);

                    for(io.github.dsheirer.alias.id.twotone.TwoToneDetectorID detectorId : alias.getTwoToneDetectors())
                    {
                        String detectorName = detectorId.getDetectorName();

                        if(detectorName != null && (aliasMatchesChannel || textMatches(detectorName, channelName)))
                        {
                            names.add(detectorName);
                        }
                    }
                }
            }
        }
        catch(Exception e)
        {
            mLog.error("Error resolving channel alias for two tone detector audio routing", e);
        }

        return names;
    }

    private String getSegmentChannelName(AudioSegment segment)
    {
        if(segment != null && segment.getIdentifierCollection() != null)
        {
            Identifier chId = segment.getIdentifierCollection()
                    .getIdentifier(IdentifierClass.CONFIGURATION, Form.CHANNEL, Role.ANY);

            if(chId instanceof io.github.dsheirer.identifier.configuration.ChannelNameConfigurationIdentifier cnci)
            {
                return cnci.getValue();
            }
        }

        return null;
    }

    private boolean textMatches(String first, String second)
    {
        return first != null && second != null && first.trim().equalsIgnoreCase(second.trim());
    }

    /**
     * Indicates if any alias in the alias model has selected the named detector.
     */
    private boolean detectorHasAnyAliasMapping(String detectorName)
    {
        for(Alias alias : mPlaylistManager.getAliasModel().aliasList())
        {
            if(alias.hasTwoToneDetector(detectorName))
            {
                return true;
            }
        }

        return false;
    }

    private int getTolerancePower(float[] buffer, long freq, double tol) {
        if (tol <= 0) {
            return powerAt(buffer, freq);
        }
        int p1 = powerAt(buffer, freq);
        int p2 = powerAt(buffer, (long)(freq - tol));
        int p3 = powerAt(buffer, (long)(freq + tol));
        return Math.max(p1, Math.max(p2, p3));
    }

    /**
     * Returns the Goertzel power (dB) of the target frequency within the buffer, reusing a cached filter instance and
     * a scratch buffer.  GoertzelFilter.getPower() windows its input in place, so the samples are copied into the
     * reused scratch buffer rather than cloning the caller's buffer on every call.
     */
    private int powerAt(float[] buffer, long freq) {
        //Select the cached filter + scratch matching the buffer length (20ms block or the ~100ms analysis window).
        final java.util.HashMap<Long, GoertzelFilter> cache;
        final float[] scratch;
        if (buffer.length == BLOCK_SIZE) {
            cache = mGoertzelFilters; scratch = mGoertzelScratch;
        } else if (buffer.length == ANALYSIS_WINDOW_SAMPLES) {
            cache = mAnalysisGoertzelFilters; scratch = mAnalysisScratch;
        } else {
            //Fall back to a one-off filter for an unexpected buffer size rather than corrupting a scratch buffer.
            return new GoertzelFilter(SAMPLE_RATE, freq, buffer.length, WindowType.BLACKMAN).getPower(buffer.clone());
        }

        GoertzelFilter filter = cache.get(freq);
        if (filter == null) {
            filter = new GoertzelFilter(SAMPLE_RATE, freq, buffer.length, WindowType.BLACKMAN);
            cache.put(freq, filter);
        }

        System.arraycopy(buffer, 0, scratch, 0, buffer.length);
        return filter.getPower(scratch);
    }

    /**
     * Returns true if the configured tone is genuinely present AND dominant, i.e. not merely spectral leakage from a
     * stronger nearby tone or broadband voice.  Because the 20ms Goertzel block only resolves ~100Hz, a strong tone
     * up to ~100Hz away (or voice energy) can push the in-band power above POWER_THRESHOLD_DB; this rejects that by
     * requiring no off-target probe frequency to exceed the target power by more than DOMINANCE_MARGIN_DB.
     */
    private boolean isDominantTone(float[] buffer, long freq, double tol)
    {
        int target = getTolerancePower(buffer, freq, tol);

        if(target <= POWER_THRESHOLD_DB)
        {
            return false;
        }

        for(long offset : DOMINANCE_PROBE_OFFSETS_HZ)
        {
            if(powerAt(buffer, freq - offset) > target + DOMINANCE_MARGIN_DB) return false;
            if(powerAt(buffer, freq + offset) > target + DOMINANCE_MARGIN_DB) return false;
        }

        return true;
    }


    private void triggerAlertIfMatched(TwoToneConfiguration config, AudioSegment segment)
    {
        boolean shouldTrigger = true;
        //Alias routing check: detectors MUST have an alias mapping to trigger.  If the detector has
        //selected aliases, it can only trigger when the segment resolved to one of them.
        if (segment != null) {
            if (!detectorHasAnyAliasMapping(config.getAlias()) ||
                    !resolveSegmentDetectorNamesWithFallback(segment).contains(config.getAlias())) {
                shouldTrigger = false;
            }
        }
        else
        {
            //No segment context — require alias mapping to exist (can't verify match without segment)
            if (!detectorHasAnyAliasMapping(config.getAlias())) {
                shouldTrigger = false;
            }
        }

        if (shouldTrigger) {
            // Per-detector duplicate suppression (IAmResponding "Ignore Duplicate Time")
            double ignoreSec = config.getIgnoreDuplicateSec();
            if (ignoreSec > 0) {
                long now = System.currentTimeMillis();
                Long lastFired = mLastTriggerTimeByDetector.get(config.getAlias());
                if (lastFired != null && (now - lastFired) < (long)(ignoreSec * 1000)) {
                    mLog.info("Suppressed duplicate Two-Tone for [{}] — within {}s ignore window",
                            config.getAlias(), (int)ignoreSec);
                    return;
                }
                mLastTriggerTimeByDetector.put(config.getAlias(), now);
            }

            mLog.info("Two Tone Detected: {} (A:{} B:{})", config.getAlias(), config.getToneA(), config.getToneB());

            //Record this detection in the detector's persisted, per-detector history (most-recent first) so the user
            //can review the date/time and channel of every two-tone hit for this detector in the editor's Detection
            //History tab.  schedulePlaylistSave() is debounced (coalesces to one write every 2s) so it survives restarts.
            config.recordDetection(System.currentTimeMillis(), getSegmentChannelName(segment));

            if(mPlaylistManager != null)
            {
                mPlaylistManager.schedulePlaylistSave();
            }

            triggerAlert(config, segment);

            String channel = "Unknown";
            if(segment != null) {
                io.github.dsheirer.identifier.Identifier id = segment.getIdentifierCollection().getIdentifier(io.github.dsheirer.identifier.IdentifierClass.CONFIGURATION, io.github.dsheirer.identifier.Form.CHANNEL, io.github.dsheirer.identifier.Role.ANY);
                if (id instanceof io.github.dsheirer.identifier.configuration.ChannelNameConfigurationIdentifier) {
                    channel = ((io.github.dsheirer.identifier.configuration.ChannelNameConfigurationIdentifier)id).getValue();
                }
            }
            org.slf4j.LoggerFactory.getLogger(io.github.dsheirer.log.TwoToneLog.LOGGER_NAME).info("[Channel: {}] [Alias: {}] - [{}]", channel, config.getAlias(), config.getAlias());
        }
    }

    private AntiFloodFilter mAntiFloodFilter;

    public void setAntiFloodFilter(AntiFloodFilter filter) {
        this.mAntiFloodFilter = filter;
    }

    private void triggerAlert(TwoToneConfiguration config, AudioSegment segment)
    {
        String template = (config.getTemplate() != null && !config.getTemplate().isEmpty()) ? config.getTemplate() : "Dispatch Received: {Alias}";

        String alias = config.getAlias() != null ? config.getAlias() : "Unknown";
        String channel = "Unknown";
        String frequency = "Unknown";

        if (segment != null) {
            Identifier chId = segment.getIdentifierCollection().getIdentifier(IdentifierClass.CONFIGURATION, Form.CHANNEL, Role.ANY);
            if (chId instanceof io.github.dsheirer.identifier.configuration.ChannelNameConfigurationIdentifier) {
                channel = ((io.github.dsheirer.identifier.configuration.ChannelNameConfigurationIdentifier)chId).getValue();
            }
            Identifier freqId = segment.getIdentifierCollection().getIdentifier(IdentifierClass.CONFIGURATION, Form.CHANNEL_FREQUENCY, Role.ANY);
            if (freqId instanceof FrequencyConfigurationIdentifier) {
                frequency = String.valueOf(((FrequencyConfigurationIdentifier)freqId).getValue());
            }
        }

        String timestamp = String.valueOf(System.currentTimeMillis());

        String text = template.replace("%ALIAS%", alias)
                              .replace("{Alias}", alias)
                              .replace("{Channel Name}", channel)
                              .replace("{Frequency}", frequency)
                              .replace("{Timestamp}", timestamp);


        if (config.isEnableMqttPublish()) {
            String payload = config.getMqttPayload() != null ? config.getMqttPayload() : "";
            payload = payload.replace("[DetectorName]", config.getAlias() != null ? config.getAlias() : "Unknown");
            payload = payload.replace("[Timestamp]", String.valueOf(System.currentTimeMillis()));
            payload = payload.replace("[Frequency]", frequency);

            MqttService.getInstance().publish(config.getMqttTopic(), payload);
        }

        MyEventBus.getGlobalEventBus().post(new TwoToneDetectedEvent(channel, text, config.isShowNotification()));

        //AntiFloodFilter removed from two-tone path: the per-detector ignoreDuplicateSec (configured in each
        //detector's settings) already provides precise duplicate suppression.  The global AntiFloodFilter had
        //15–60 minute escalating cooldowns that are far too aggressive for fire/EMS paging, where multiple
        //dispatches per hour are normal and every alert must be sent.

        boolean sendText = config.isEnableZelloTextMessage();
        boolean sendTone = config.isEnableZelloAlert() && config.getZelloAlertFile() != null &&
                !config.getZelloAlertFile().isEmpty();
        boolean sendChannelAlert = config.isEnableZelloChannelAlert();

        if(sendText || sendTone || sendChannelAlert)
        {
            //Build the channel alert text from the detector's template using the same shortcodes
            String channelAlertText = null;
            if(sendChannelAlert)
            {
                channelAlertText = config.getZelloChannelAlertText() != null
                        ? config.getZelloChannelAlertText() : "Dispatch Received: {Alias}";
                channelAlertText = channelAlertText
                        .replace("%ALIAS%", config.getAlias() != null ? config.getAlias() : "Unknown")
                        .replace("{Alias}", config.getAlias() != null ? config.getAlias() : "Unknown")
                        .replace("{Channel Name}", channel != null ? channel : "Unknown")
                        .replace("{Frequency}", frequency)
                        .replace("{Timestamp}", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                                .format(new java.util.Date()));
            }

            for(String streamName : config.getEffectiveZelloChannels())
            {
                ZelloBroadcaster broadcaster = getZelloBroadcaster(streamName);

                if(broadcaster == null)
                {
                    mLog.warn("Two Tone Zello alert: no active Zello broadcaster for stream [{}] - is the stream " +
                            "enabled and connected?", streamName);
                    continue;
                }

                mLog.info("Sending Two Tone Zello alert to stream [{}]: {}", streamName, text);

                if(sendText)
                {
                    broadcaster.sendTextMessage(text);
                }
                if(sendTone)
                {
                    broadcaster.injectPreDispatchAudio(config.getZelloAlertFile());
                }
                if(sendChannelAlert)
                {
                    broadcaster.sendChannelAlert(channelAlertText);
                }
            }
        }

        if (config.getAlertFilePath() != null && !config.getAlertFilePath().isEmpty()) {
            try {
                String path = config.getAlertFilePath();
                java.net.URL resource = null;
                if (!path.contains("\\") && !path.contains("/") && !path.contains(":")) {
                    resource = TwoToneDetector.class.getResource("/audio/thinline/" + path);
                    if (resource == null) {
                        resource = TwoToneDetector.class.getResource("/audio/" + path);
                    }
                }
                
                boolean isMp3 = path.toLowerCase().endsWith(".mp3");

                if (resource != null) {
                    if (isMp3) {
                        playAlertMp3(resource.toURI().toString());
                    } else {
                        playAlertClip(javax.sound.sampled.AudioSystem.getAudioInputStream(resource));
                    }
                } else {
                    java.io.File file = new java.io.File(path);
                    if (file.exists()) {
                        if (isMp3) {
                            playAlertMp3(file.toURI().toString());
                        } else {
                            playAlertClip(javax.sound.sampled.AudioSystem.getAudioInputStream(file));
                        }
                    } else {
                        mLog.error("Could not find alert audio file or resource: " + path);
                    }
                }
            } catch (Exception ex) {
                mLog.error("Error playing local alert audio: " + config.getAlertFilePath(), ex);
            }
        }
    }

    //Strong references to in-flight local alert players/clips so the JavaFX MediaPlayer and javax Clip are not
    //garbage-collected mid-playback.  A local variable becomes GC-eligible as soon as the method/lambda returns, which
    //caused local audio alerts to be cut off or not play at all (unreliable local audio alert).  Each entry removes
    //itself when playback ends.
    private final java.util.Set<Object> mActiveAlertPlayers = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** Plays an mp3 alert via JavaFX MediaPlayer on the FX thread, retaining a strong reference until it finishes. */
    private void playAlertMp3(String mediaUri) {
        javafx.application.Platform.runLater(() -> {
            try {
                javafx.scene.media.Media media = new javafx.scene.media.Media(mediaUri);
                javafx.scene.media.MediaPlayer player = new javafx.scene.media.MediaPlayer(media);
                mActiveAlertPlayers.add(player);
                Runnable release = () -> {
                    mActiveAlertPlayers.remove(player);
                    try { player.dispose(); } catch (Exception ignored) {}
                };
                player.setOnEndOfMedia(release);
                player.setOnError(release);
                player.play();
            } catch (Exception ex) {
                mLog.error("Error playing mp3 alert", ex);
            }
        });
    }

    /** Plays a sampled (e.g. WAV) alert clip, retaining a strong reference until playback stops. */
    private void playAlertClip(javax.sound.sampled.AudioInputStream ais) {
        try {
            javax.sound.sampled.Clip clip = javax.sound.sampled.AudioSystem.getClip();
            clip.open(ais);
            mActiveAlertPlayers.add(clip);
            clip.addLineListener(ev -> {
                if (ev.getType() == javax.sound.sampled.LineEvent.Type.STOP) {
                    mActiveAlertPlayers.remove(clip);
                    try { clip.close(); } catch (Exception ignored) {}
                }
            });
            clip.start();
        } catch (Exception ex) {
            mLog.error("Error playing local alert clip", ex);
        }
    }

    /**
     * Resolves the live Zello broadcaster for the given broadcast stream name, or null if there is no active Zello
     * stream with that name.  The two tone editor stores broadcast stream names (BroadcastConfiguration.getName()),
     * so the same name is used here to look up the running broadcaster.
     */
    private ZelloBroadcaster getZelloBroadcaster(String streamName)
    {
        if(streamName == null || mBroadcastModel == null)
        {
            return null;
        }

        AbstractAudioBroadcaster broadcaster = mBroadcastModel.getBroadcaster(streamName);

        if(broadcaster instanceof ZelloBroadcaster)
        {
            return (ZelloBroadcaster) broadcaster;
        }

        return null;
    }

    private void logDiscovery(double toneA, double toneB, AudioSegment segment)
    {
        mLog.info(String.format("Discovery: Detected unknown two-tone sequence: Tone A: %.1f Hz, Tone B: %.1f Hz", toneA, toneB));
        
        boolean exists = false;
        for (TwoToneConfiguration config : mPlaylistManager.getTwoToneConfigurations()) {
            //Use the standard analog tolerance (~1.5%) so slightly drifted measurements still match an existing
            //detector instead of being reported as a new discovery.
            if (ToneStandards.toneSequencesMatch(config.getToneA(), config.getToneB(), toneA, toneB)) {
                exists = true;
                break;
            }
        }

        //RF channel frequency the tone was heard on (part of the tombstone key / human-review package).
        double channelFrequency = 0.0;
        if(segment != null) {
            Identifier freqId = segment.getIdentifierCollection().getIdentifier(IdentifierClass.CONFIGURATION, Form.CHANNEL_FREQUENCY, Role.ANY);
            if (freqId instanceof FrequencyConfigurationIdentifier) {
                Object value = ((FrequencyConfigurationIdentifier) freqId).getValue();
                if (value instanceof Number) {
                    channelFrequency = ((Number) value).doubleValue();
                }
            }
        }

        if (!exists) {
            // Emit event for AI Tone Discovery Manager, including the channel name so an AI-created detector can be
            // named for / record where the tones were heard.
            io.github.dsheirer.eventbus.MyEventBus.getGlobalEventBus().post(
                    new ToneDiscoveredEvent(toneA, toneB, segment, channelFrequency, getSegmentChannelName(segment)));
        }
        
        String channel = "Unknown";
        if(segment != null) {
            io.github.dsheirer.identifier.Identifier id = segment.getIdentifierCollection().getIdentifier(io.github.dsheirer.identifier.IdentifierClass.CONFIGURATION, io.github.dsheirer.identifier.Form.CHANNEL, io.github.dsheirer.identifier.Role.ANY);
            if (id instanceof io.github.dsheirer.identifier.configuration.ChannelNameConfigurationIdentifier) {
                channel = ((io.github.dsheirer.identifier.configuration.ChannelNameConfigurationIdentifier)id).getValue();
            }
        }
        //Log every detected two-tone sequence - recognized or not - with its tones and channel, so the user can see
        //discovery is working even for tones that won't be auto-added (auto-add requires repeated occurrences).
        org.slf4j.LoggerFactory.getLogger(io.github.dsheirer.log.TwoToneLog.LOGGER_NAME).info(
                "[Channel: {}] [{} MHz] Tone A: {} Hz, Tone B: {} Hz - {}",
                channel,
                channelFrequency > 0 ? String.format("%.4f", channelFrequency / 1.0E6) : "?",
                String.format("%.1f", toneA),
                String.format("%.1f", toneB),
                exists ? "matches existing detector" : "UNRECOGNIZED (candidate for new detector)");
    }

    public void dispose()
    {
        mRunning.set(false);
        mExecutorService.shutdownNow();
    }
    private static class AudioBufferWrapper {
        float[] buffer;
        AudioSegment segment;
        AudioBufferWrapper(float[] buffer, AudioSegment segment) {
            this.buffer = buffer;
            this.segment = segment;
        }
    }
}
