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
package io.github.dsheirer.module.decode.nbfm;

import io.github.dsheirer.audio.squelch.SquelchState;
import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.channel.state.IDecoderStateEventProvider;
import io.github.dsheirer.channel.state.State;
import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.decimate.DecimationFilterFactory;
import io.github.dsheirer.dsp.filter.decimate.IRealDecimationFilter;
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.dsp.filter.fir.FIRFilterSpecification;
import io.github.dsheirer.dsp.filter.fir.real.IRealFilter;
import io.github.dsheirer.dsp.filter.nbfm.NBFMAudioFilters;
import io.github.dsheirer.dsp.filter.resample.RealResampler;
import io.github.dsheirer.dsp.fm.FmDemodulatorFactory;
import io.github.dsheirer.dsp.fm.IDemodulator;
import io.github.dsheirer.dsp.squelch.INoiseSquelchController;
import io.github.dsheirer.dsp.squelch.NoiseSquelch;
import io.github.dsheirer.dsp.squelch.NoiseSquelchState;
import io.github.dsheirer.dsp.squelch.SquelchTailRemover;
import io.github.dsheirer.dsp.window.WindowType;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.SquelchControlDecoder;
import io.github.dsheirer.module.decode.config.ChannelToneFilter;
import io.github.dsheirer.module.decode.ctcss.CTCSSCode;
import io.github.dsheirer.module.decode.dcs.DCSCode;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.sample.complex.IComplexSamplesListener;
import io.github.dsheirer.sample.real.IRealBufferProvider;
import io.github.dsheirer.source.ISourceEventListener;
import io.github.dsheirer.source.ISourceEventProvider;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.module.decode.nbfm.ai.AIAnalysisResult;
import io.github.dsheirer.module.decode.nbfm.ai.AIAudioOptimizer;
import io.github.dsheirer.module.decode.nbfm.ai.AudioBufferManager;
import io.github.dsheirer.module.decode.nbfm.ai.AudioWatchdogService;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.manager.AdaptiveGainAdvisor;
import io.github.dsheirer.util.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * NBFM decoder with integrated noise squelch, CTCSS tone filtering, and squelch tail removal.
 *
 * Demodulates complex sample buffers and feeds unfiltered, demodulated audio to Noise Squelch.
 * Squelch operates on the noise level with open and close thresholds to pass low-noise audio
 * and block high-noise audio.  Audio is filtered and resampled to 8 kHz for downstream consumers.
 *
 * When CTCSS tone filtering is enabled, the resampled audio is analyzed by a CTCSSDetector.
 * When DCS code filtering is enabled, the resampled audio is analyzed by a DCSDetector.
 * Audio is only passed downstream when the configured tone/code is confirmed present.
 * This prevents hearing distant/interfering signals on the same frequency that use a
 * different CTCSS tone or DCS code. The channel goes fully idle when the wrong tone/code
 * is detected — just like a real radio with tone squelch.
 *
 * When squelch tail removal is enabled, a SquelchTailRemover buffers audio and discards
 * the trailing noise burst that occurs when a transmitter drops carrier.
 */
public class NBFMDecoder extends SquelchControlDecoder implements ISourceEventListener, ISourceEventProvider,
        IComplexSamplesListener, Listener<ComplexSamples>, IRealBufferProvider, IDecoderStateEventProvider,
        INoiseSquelchController
{
    private final static Logger mLog = LoggerFactory.getLogger(NBFMDecoder.class);
    private NBFMDecoderState mDecoderState;
    private static final double DEMODULATED_AUDIO_SAMPLE_RATE = 8000.0;
    private final IDemodulator mDemodulator = FmDemodulatorFactory.getFmDemodulator();
    private final SourceEventProcessor mSourceEventProcessor = new SourceEventProcessor();
    //Outbound source-event broadcaster (wired by ProcessingChain). Used to publish channel power level
    //so the Signal Power meter (SignalPowerView) updates for NBFM channels, just like the AM decoder.
    private Listener<SourceEvent> mSourceEventListener;
    private final NoiseSquelch mNoiseSquelch;
    private IRealFilter mIBasebandFilter;
    private IRealFilter mQBasebandFilter;
    private IRealDecimationFilter mIDecimationFilter;
    private IRealDecimationFilter mQDecimationFilter;
    private Listener<float[]> mResampledBufferListener;
    private Listener<DecoderStateEvent> mDecoderStateEventListener;
    private RealResampler mResampler;
    private final double mChannelBandwidth;

    // CTCSS/DCS tone filtering
    private final boolean mToneFilterEnabled;
    private final ChannelToneFilter.ToneType mToneFilterType;
    private final Set<CTCSSCode> mTargetCTCSSCodes;
    private final Set<DCSCode> mTargetDCSCodes;
    private CTCSSDetector mCTCSSDetector;
    private DCSDetector mDCSDetector;
    private volatile boolean mToneMatch = false;

    /**
     * Holdover period (ms) for tone match across brief squelch closures.
     * When noise squelch flutters closed and re-opens within this window,
     * the previously confirmed tone match is preserved — avoiding a full
     * re-detection delay (225ms+ for CTCSS, 340ms+ for DCS) on each flutter.
     * This mimics real radio behavior where tone squelch has a short holdover.
     */
    private static final long TONE_HOLDOVER_MS = 500;
    private volatile long mLastToneMatchTime = 0;

    // Tone-squelch false-trigger suppression (per channel, from config; both default off).
    //  - minimum call duration: a tone match must sustain (passing the gate) this long before it becomes a real
    //    call, so brief static bursts that momentarily carry the tone are dropped (lead-in audio is buffered so a
    //    qualifying call's start isn't clipped).
    //  - require noise squelch: additionally gate audio on the noise squelch being open (tone AND carrier), so
    //    noisy static can't open the channel even when it carries the tone.
    private final int mToneMinCallDurationMs;
    private final boolean mToneRequireNoiseSquelch;
    private boolean mCallQualified = false;
    private long mToneSustainStartMs = -1;
    private final java.util.ArrayDeque<float[]> mPendingCallAudio = new java.util.ArrayDeque<>();
    private int mPendingCallSamples = 0;

    // Squelch tail/head removal
    private final boolean mSquelchTailRemovalEnabled;
    private final int mSquelchTailRemovalMs;
    private final int mSquelchHeadRemovalMs;
    private SquelchTailRemover mSquelchTailRemover;

    // Automatic squelch calibration (auto-calibrate on start + continuous drift + tail optimization)
    private final io.github.dsheirer.dsp.squelch.SquelchAutoCalibrator mSquelchAutoCalibrator;
    private Listener<NoiseSquelchState> mExternalNoiseSquelchStateListener;

    // VoxSend audio filter chain (low-pass, de-emphasis, bass boost, voice enhancement, noise gate)
    private NBFMAudioFilters mAudioFilters;
    private AudioBufferManager mAudioBufferManager;
    private AIAudioOptimizer mAIAudioOptimizer;
    private AudioWatchdogService mAudioWatchdog;
    private final DecodeConfigNBFM mNBFMConfig;
    private final UserPreferences mUserPreferences;
    private final String mChannelName;
    private int mCallEventCount = 0;
    private final AtomicBoolean mOptimizationRunning = new AtomicBoolean(false);

    // Signal level sampling for AdaptiveGainAdvisor (sample every 50th buffer for efficiency)
    private int mIQSampleCounter = 0;
    private static final int IQ_SAMPLE_INTERVAL = 50;
    //Current channel center frequency (Hz), tracked from source events so the gain advisor can
    //attribute this channel to the tuner whose passband contains it.
    private long mChannelFrequency = 0;

    //AI auto-optimize readiness: the optimizer should run as soon as possible after startup, but only
    //once the channel has accumulated enough good audio (at least MIN_QUALIFYING_CALLS calls that are
    //each at least 5 seconds long) for the AI to make sound filter choices.  After that first
    //"startup" run, subsequent runs follow the twice-daily cadence in AIPreference.
    private int mCurrentCallSamples = 0;
    private int mQualifyingCallCount = 0;
    private boolean mStartupOptimizeDone = false;
    private static final int MIN_QUALIFYING_CALLS = 5;
    private static final int MIN_QUALIFYING_CALL_SAMPLES = (int)(5 * DEMODULATED_AUDIO_SAMPLE_RATE);

    /**
     * Constructs an instance
     *
     * @param config to setup the NBFM decoder and noise squelch control.
     */
    public NBFMDecoder(String channelName, DecodeConfigNBFM config, UserPreferences userPreferences)
    {
        super(config);

        //Save config reference for audio filter initialization (deferred until sample rate is known)
        mNBFMConfig = config;
        mUserPreferences = userPreferences;
        mChannelName = channelName;
        mAIAudioOptimizer = new AIAudioOptimizer(userPreferences);
        mAudioBufferManager = new AudioBufferManager(userPreferences, channelName);

        // Wire audio watchdog (silence detection + Gemini triage when enabled)
        if(userPreferences.getAIPreference().isSystemHealthAdvisorEnabled())
        {
            mAudioWatchdog = new AudioWatchdogService(userPreferences);
        }

        //Save channel bandwidth to setup channel baseband filter.
        mChannelBandwidth = config.getBandwidth().getValue();
        mNoiseSquelch = new NoiseSquelch(config.getSquelchNoiseOpenThreshold(), config.getSquelchNoiseCloseThreshold(),
                config.getSquelchHysteresisOpenThreshold(), config.getSquelchHysteresisCloseThreshold());
        mNoiseSquelch.setAdaptive(config.isSquelchNoiseAdaptive());

        //Scheduled squelch calibration.  Calibration is manual-only (the per-channel Calibrate button) unless
        //the user enables a schedule in preferences; this calibrator runs only when that schedule is enabled,
        //at the configured interval (minimum 12 hours), and is locked out when the user has manually adjusted
        //this channel's squelch.  Applies thresholds (and a tail-removal duration) to the live squelch and
        //persisted config without marking the change as a manual adjustment.
        mSquelchAutoCalibrator = new io.github.dsheirer.dsp.squelch.SquelchAutoCalibrator(channelName,
                () -> mUserPreferences.getAIPreference().isSquelchAdvisorScheduleEnabled(),
                () -> getDecodeConfiguration().isSquelchManuallyAdjusted(),
                () -> mUserPreferences.getAIPreference().getSquelchAdvisorIntervalHours() * 3_600_000L,
                (open, close) -> {
                    mNoiseSquelch.setNoiseThreshold(open, close);
                    getDecodeConfiguration().setSquelchNoiseOpenThreshold(open);
                    getDecodeConfiguration().setSquelchNoiseCloseThreshold(close);
                },
                tailMs -> {
                    if(mSquelchTailRemover != null)
                    {
                        mSquelchTailRemover.setTailRemovalMs(tailMs);
                    }
                    getDecodeConfiguration().setSquelchTailRemovalMs(tailMs);
                });

        //Register an internal squelch-state listener that feeds the auto-calibrator and forwards to the UI.
        mNoiseSquelch.setNoiseSquelchStateListener(this::onNoiseSquelchState);

// Extract CTCSS/DCS tone filter configuration
        mToneFilterEnabled = config.hasToneFiltering();
        mToneMinCallDurationMs = config.getToneMinCallDurationMs();
        mToneRequireNoiseSquelch = config.isToneRequireNoiseSquelch();
        if(mToneFilterEnabled)
        {
            mTargetCTCSSCodes = extractCTCSSCodes(config.getToneFilters());
            mTargetDCSCodes = extractDCSCodes(config.getToneFilters());

            // Determine which type of filter is configured
            if(!mTargetCTCSSCodes.isEmpty())
            {
                mToneFilterType = ChannelToneFilter.ToneType.CTCSS;
                mLog.info("CTCSS tone filtering enabled with {} target code(s)", mTargetCTCSSCodes.size());
            }
            else if(!mTargetDCSCodes.isEmpty())
            {
                mToneFilterType = ChannelToneFilter.ToneType.DCS;
                mLog.info("DCS code filtering enabled with {} target code(s)", mTargetDCSCodes.size());
            }
            else
            {
                mToneFilterType = null;
                mLog.warn("Tone filtering enabled but no valid CTCSS or DCS codes configured");
            }
        }
        else
        {
            mTargetCTCSSCodes = null;
            mTargetDCSCodes = null;
            mToneFilterType = null;
        }

        //Send squelch controlled audio to the resampler and notify the decoder state that the call continues.
        mNoiseSquelch.setAudioListener(audio -> {
            if(isToneGated())
            {
                //Tone-gated channels resample the full demodulated stream in receive() so the tone detector is
                //never starved by the noise squelch; their output is gated on the tone match, not this squelch.
                return;
            }

            // if squelch is closing (it hasn't propagated yet to mute the audio)
            //  call the resampler with lastBatch set to true. This will zero pad the input buffer and ensure
            //  the output buffer gets emptied.
            if(mNoiseSquelch.isSquelched())
            {
                mResampler.resample(audio, true);
            }
            else
            {
                mResampler.resample(audio);     // this method will set lastBatch to false
                // Only signal call activity if tone matches (or no tone filter configured)
                if(!mToneFilterEnabled || mToneMatch)
                {
                    notifyCallContinuation();
                }
            }
        });

        // Extract squelch tail removal configuration
        mSquelchTailRemovalEnabled = config.isSquelchTailRemovalEnabled();
        mSquelchTailRemovalMs = config.getSquelchTailRemovalMs();
        mSquelchHeadRemovalMs = config.getSquelchHeadRemovalMs();

        //Notify the decoder state of call starts and ends, and manage tail remover + tone reset.
        //When tone filtering is enabled, we defer the call start until the correct tone is confirmed.
        //The channel stays idle until the CTCSS detector confirms the right tone — just like a real radio.
        mNoiseSquelch.setSquelchStateListener(squelchState -> {
            if(isToneGated())
            {
                //Tone-gated channels derive call start/continuation/end (and tail-remover open/close) from the
                //tone detector, which is fed continuously, so a mistuned noise squelch can neither start spurious
                //calls nor end real ones.  The noise squelch does not participate in their gating.
                return;
            }

            if(squelchState == SquelchState.SQUELCH)
            {
                // Squelch closed (end of transmission)
                if(mSquelchTailRemover != null)
                {
                    mSquelchTailRemover.squelchClose();
                }

                // DON'T immediately reset tone match or detectors here.
                // Brief squelch closures (noise flutter) shouldn't force a full
                // re-detection cycle. Instead, we preserve the tone match for a
                // holdover period. The tone match will be cleared either:
                //   (a) when squelch re-opens and holdover has expired, or
                //   (b) when the detector reports tone lost or rejected.

                // Only send call end if a call was actually active (tone was matched or no filter)
                if(!mToneFilterEnabled || mToneMatch)
                {
                    notifyCallEnd();
                }
            }
            else
            {
                // Squelch opened (start of transmission)
                if(mSquelchTailRemover != null)
                {
                    mSquelchTailRemover.squelchOpen();
                }

                if(!mToneFilterEnabled)
                {
                    // No tone filter — start call immediately (original behavior)
                    notifyCallStart();
                }
                else
                {
                    // Tone filter is active. Check if we have a recent tone match
                    // within the holdover window — if so, preserve it and resume
                    // audio immediately without waiting for re-detection.
                    long elapsed = System.currentTimeMillis() - mLastToneMatchTime;

                    if(mToneMatch && elapsed < TONE_HOLDOVER_MS)
                    {
                        // Holdover active — continue as if tone is still confirmed.
                        // The detector keeps running and will reject/lost if tone changes.
                        notifyCallStart();
                    }
                    else
                    {
                        // Holdover expired or no previous match — full reset.
                        // Channel stays idle until detector confirms the correct tone.
                        mToneMatch = false;

                        if(mCTCSSDetector != null)
                        {
                            mCTCSSDetector.reset();
                        }
                        if(mDCSDetector != null)
                        {
                            mDCSDetector.reset();
                        }
                    }
                }
            }
        });
    }

    /**
     * Extracts CTCSSCode targets from the channel tone filter configuration.
     * @param filters list of configured tone filters
     * @return set of CTCSS codes to accept, or empty set if none configured
     */
    private Set<CTCSSCode> extractCTCSSCodes(List<ChannelToneFilter> filters)
    {
        EnumSet<CTCSSCode> codes = EnumSet.noneOf(CTCSSCode.class);

        if(filters != null)
        {
            for(ChannelToneFilter filter : filters)
            {
                if(filter.getToneType() == ChannelToneFilter.ToneType.CTCSS)
                {
                    CTCSSCode code = filter.getCTCSSCode();
                    if(code != null && code != CTCSSCode.UNKNOWN)
                    {
                        codes.add(code);
                        mLog.info("CTCSS target: {} ({})", code.getDisplayString(), code.name());
                    }
                }
            }
        }

        return codes;
    }

    /**
     * Extracts DCSCode targets from the channel tone filter configuration.
     * @param filters list of configured tone filters
     * @return set of DCS codes to accept, or empty set if none configured
     */
    private Set<DCSCode> extractDCSCodes(List<ChannelToneFilter> filters)
    {
        EnumSet<DCSCode> codes = EnumSet.noneOf(DCSCode.class);

        if(filters != null)
        {
            for(ChannelToneFilter filter : filters)
            {
                if(filter.getToneType() == ChannelToneFilter.ToneType.DCS)
                {
                    DCSCode code = filter.getDCSCode();
                    if(code != null && code != DCSCode.UNKNOWN)
                    {
                        codes.add(code);
                        mLog.info("DCS target: {}", code.toString());
                    }
                }
            }
        }

        return codes;
    }

    /**
     * Sets the decoder state reference for CTCSS/DCS detection integration.
     * @param decoderState the NBFM decoder state to receive tone notifications
     */
    public void setDecoderState(NBFMDecoderState decoderState)
    {
        mDecoderState = decoderState;
    }

    /**
     * Decoder type.
     * @return type
     */
    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.NBFM;
    }

    /**
     * Decode configuration for this decoder.
     * @return configuration
     */
    @Override
    public DecodeConfigNBFM getDecodeConfiguration()
    {
        return (DecodeConfigNBFM)super.getDecodeConfiguration();
    }

    /**
     * Register the noise squelch state listener.  This will normally be a GUI noise squelch state view/controller.
     * @param listener to receive states or pass null to de-register a listener.
     */
    @Override
    public void setNoiseSquelchStateListener(Listener<NoiseSquelchState> listener)
    {
        //Store the external (UI) listener.  The decoder keeps its own internal listener registered with the
        //squelch so the auto-calibrator always receives state, even when no UI is attached.
        mExternalNoiseSquelchStateListener = listener;
    }

    /**
     * Internal squelch-state handler: feeds the automatic calibrator and forwards to the external (UI)
     * listener when one is registered.
     * @param state latest noise squelch state.
     */
    private void onNoiseSquelchState(NoiseSquelchState state)
    {
        mSquelchAutoCalibrator.process(state);

        Listener<NoiseSquelchState> external = mExternalNoiseSquelchStateListener;
        if(external != null)
        {
            external.receive(state);
        }
    }

    @Override
    public void markSquelchManuallyAdjusted()
    {
        getDecodeConfiguration().setSquelchManuallyAdjusted(true);
    }

    @Override
    public void clearSquelchManualAdjustment()
    {
        getDecodeConfiguration().setSquelchManuallyAdjusted(false);
    }

    @Override
    public boolean isSquelchManuallyAdjusted()
    {
        return getDecodeConfiguration().isSquelchManuallyAdjusted();
    }

    /**
     * Applies new open and close noise threshold values for the noise squelch.
     * @param open for the open noise variance calculation in range 0.1 - 0.5 where open <= close value.
     * @param close for the close noise variance calculation. in range 0.1 - 0.5 where close >= open.
     */
    @Override
    public void setNoiseThreshold(float open, float close)
    {
        mNoiseSquelch.setNoiseThreshold(open, close);

        //Update the channel configuration and schedule a playlist save.
        getDecodeConfiguration().setSquelchNoiseOpenThreshold(open);
        getDecodeConfiguration().setSquelchNoiseCloseThreshold(close);
    }

    /**
     * Enables or disables adaptive noise-floor squelch tracking for this channel and persists the setting.
     * @param enabled true to enable adaptive tracking.
     */
    @Override
    public void setAdaptiveSquelch(boolean enabled)
    {
        mNoiseSquelch.setAdaptive(enabled);
        getDecodeConfiguration().setSquelchNoiseAdaptive(enabled);
    }

    /**
     * Indicates whether adaptive noise-floor squelch tracking is enabled for this channel.
     */
    @Override
    public boolean isAdaptiveSquelch()
    {
        return mNoiseSquelch.isAdaptive();
    }

    /**
     * Sets the open and close hysteresis thresholds in units of 10 milliseconds.
     * @param open in range 1-10, recommend: 4 where open <= close
     * @param close in range 1-10, recommend: 6 where close >= open.
     */
    @Override
    public void setHysteresisThreshold(int open, int close)
    {
        mNoiseSquelch.setHysteresisThreshold(open, close);
        getDecodeConfiguration().setSquelchHysteresisOpenThreshold(open);
        getDecodeConfiguration().setSquelchHysteresisCloseThreshold(close);
    }

    /**
     * Sets the squelch override state to temporarily bypass/override squelch control and pass all audio.
     * @param override (true) or (false) to turn off squelch override.
     */
    @Override
    public void setSquelchOverride(boolean override)
    {
        mNoiseSquelch.setSquelchOverride(override);
    }

    /**
     * Implements the ISourceEventListener interface
     */
    @Override
    public Listener<SourceEvent> getSourceEventListener()
    {
        return mSourceEventProcessor;
    }

    /**
     * Implements the ISourceEventProvider interface.  The ProcessingChain registers its source-event
     * broadcaster here so the decoder can publish channel power level (NOTIFICATION_CHANNEL_POWER) to the
     * Signal Power meter, just like the AM decoder.
     */
    @Override
    public void setSourceEventListener(Listener<SourceEvent> listener)
    {
        mSourceEventListener = listener;
    }

    /**
     * Implements the ISourceEventProvider interface to de-register the source-event broadcaster.
     */
    @Override
    public void removeSourceEventListener()
    {
        mSourceEventListener = null;
    }

    /**
     * Module interface methods - unused.
     */
    @Override
    public String getChannelName()
    {
        return mChannelName;
    }

    @Override
    public void reset() {}

    @Override
    public void start() {}

    @Override
    public void stop()
    {
        // Remove channel from gain advisor so its stale stats don't skew future recommendations
        if(mUserPreferences.getAIPreference().isGainAdvisorEnabled())
        {
            AdaptiveGainAdvisor.getInstance(mUserPreferences).removeChannel(mChannelName);
        }

        // Cancel this channel's watchdog check on the shared scheduler so stopped channels aren't polled.
        if(mAudioWatchdog != null)
        {
            mAudioWatchdog.shutdown();
        }
    }

    /**
     * Broadcasts the demodulated, resampled to 8 kHz audio samples to the registered listener.
     *
     * @param demodulatedSamples to broadcast
     */
    protected void broadcast(float[] demodulatedSamples)
    {
        if(mResampledBufferListener != null)
        {
            mResampledBufferListener.receive(demodulatedSamples);
        }
    }

    /**
     * Indicates whether this channel is tone-gated, i.e. it has a valid CTCSS or DCS tone filter configured.
     * For tone-gated channels the tone/code detector - fed continuously from the demodulated stream - is the
     * squelch, so the noise squelch does not gate audio or drive the call lifecycle.
     * @return true when a valid CTCSS/DCS tone filter is active.
     */
    private boolean isToneGated()
    {
        return mToneFilterEnabled && mToneFilterType != null;
    }

    /**
     * Processes resampled 8 kHz audio through the CTCSS/DCS tone filter and squelch tail remover
     * before broadcasting to downstream consumers.
     *
     * Audio flow: resampler → this method → detector analysis → tone gate → tail remover → broadcast
     *
     * @param resampledAudio 8 kHz audio from the resampler
     */
    private void processResampledAudio(float[] resampledAudio)
    {
        // Step 1: Feed audio to the active tone/code detector for analysis
        if(mCTCSSDetector != null)
        {
            mCTCSSDetector.process(resampledAudio);
        }
        if(mDCSDetector != null)
        {
            mDCSDetector.process(resampledAudio);
        }

        // Step 2: Gate audio.  Tone-gated channels run the tone-squelch state machine (minimum-call-duration
        // qualification + optional require-noise-squelch AND-gate); everything else passes straight through.
        if(isToneGated())
        {
            processToneGatedAudio(resampledAudio);
            return;
        }

        if(mToneFilterEnabled && !mToneMatch)
        {
            // Tone filtering enabled but no valid code configured (misconfiguration) - block audio.
            return;
        }

        emitAudio(resampledAudio);
    }

    /**
     * Applies the tone-squelch call gating for tone-filtered channels: the minimum-call-duration qualification
     * and the optional require-noise-squelch (tone AND carrier) gate.  Audio for a tone match that never
     * qualifies (too brief, or never passes the gate) is dropped, suppressing false triggers; a qualifying call
     * flushes its buffered lead-in so its start is not clipped.
     */
    private void processToneGatedAudio(float[] resampledAudio)
    {
        if(!mToneMatch)
        {
            return; //No matching tone - nothing passes (idle is driven from receive()).
        }

        //Optional AND-gate: also require the noise squelch open so noisy static carrying the tone can't open it.
        boolean passGate = !mToneRequireNoiseSquelch || !mNoiseSquelch.isSquelched();

        if(mCallQualified)
        {
            //Established call: pass audio while the gate is open; briefly mute on a carrier gap without ending.
            if(passGate)
            {
                notifyCallContinuation();
                emitAudio(resampledAudio);
            }
            return;
        }

        //Candidate call: the tone has matched but has not yet sustained long enough to be a real call.
        if(!passGate)
        {
            //Gate closed (noisy static under the AND-gate, or carrier not open) - restart the qualification
            //window and drop any buffered lead-in so a brief, gated burst can't qualify.
            mToneSustainStartMs = -1;
            clearPendingCallAudio();
            return;
        }

        long now = System.currentTimeMillis();
        if(mToneSustainStartMs < 0)
        {
            mToneSustainStartMs = now;
        }

        bufferPendingCallAudio(resampledAudio);

        if((now - mToneSustainStartMs) >= mToneMinCallDurationMs)
        {
            qualifyToneCall();
        }
    }

    /**
     * Resets candidate-call state at the start of a new tone match.  The call is not started until it qualifies
     * in {@link #processToneGatedAudio(float[])}.
     */
    private void onToneCandidateStart()
    {
        mCallQualified = false;
        mToneSustainStartMs = -1;
        clearPendingCallAudio();
    }

    /**
     * Promotes the current candidate to a real call: starts the call, opens the tail remover, and flushes the
     * buffered lead-in audio so the call's start is not clipped.
     */
    private void qualifyToneCall()
    {
        mCallQualified = true;

        if(mSquelchTailRemover != null)
        {
            mSquelchTailRemover.squelchOpen();
        }

        notifyCallStart();

        while(!mPendingCallAudio.isEmpty())
        {
            float[] buffered = mPendingCallAudio.pollFirst();
            notifyCallContinuation();
            emitAudio(buffered);
        }
        mPendingCallSamples = 0;
    }

    /**
     * Ends the current tone match.  A qualified call is ended normally (tail remover + call end); an unqualified
     * candidate (too brief / never passed the gate) is discarded so no call event or audio is produced.
     */
    private void onToneCandidateEnd()
    {
        if(mCallQualified)
        {
            if(mSquelchTailRemover != null)
            {
                mSquelchTailRemover.squelchClose();
            }
            notifyCallEnd();
        }
        else
        {
            clearPendingCallAudio();
        }

        mCallQualified = false;
        mToneSustainStartMs = -1;
        notifyIdle();
    }

    /**
     * Buffers candidate-call lead-in audio, bounded to roughly the minimum-call-duration window so memory can't
     * grow without bound if a candidate lingers without qualifying.
     */
    private void bufferPendingCallAudio(float[] audio)
    {
        mPendingCallAudio.addLast(audio);
        mPendingCallSamples += audio.length;

        int maxSamples = (int)(((mToneMinCallDurationMs + 250) / 1000.0) * DEMODULATED_AUDIO_SAMPLE_RATE) + 1024;
        while(mPendingCallSamples > maxSamples && !mPendingCallAudio.isEmpty())
        {
            float[] dropped = mPendingCallAudio.pollFirst();
            mPendingCallSamples -= dropped.length;
        }
    }

    /**
     * Discards any buffered candidate-call lead-in audio.
     */
    private void clearPendingCallAudio()
    {
        mPendingCallAudio.clear();
        mPendingCallSamples = 0;
    }

    /**
     * Applies the audio filter chain, AI buffering, and tail remover / broadcast to a buffer that has passed the
     * squelch/tone gating.  Shared by the tone-gated and non-tone-gated paths.
     */
    private void emitAudio(float[] resampledAudio)
    {
        // Apply VoxSend audio filter chain (low-pass, de-emphasis, bass boost, voice enhancement, noise gate).
        if(mAudioWatchdog != null)
        {
            mAudioWatchdog.feedAudioData(resampledAudio);
        }
        if(isAudioBufferingEnabled())
        {
            mAudioBufferManager.addAudioSamples(resampledAudio);
        }
        //Accumulate this call's audio length (8 kHz) to decide whether it is a "qualifying" call.
        mCurrentCallSamples += resampledAudio.length;
        if(mAudioFilters != null)
        {
            mAudioFilters.process(resampledAudio);
        }

        // Pass through squelch tail remover if enabled, otherwise broadcast directly.
        if(mSquelchTailRemover != null)
        {
            mSquelchTailRemover.process(resampledAudio);
        }
        else
        {
            broadcast(resampledAudio);
        }
    }

    /**
     * Implements the IRealBufferProvider interface to register a listener for demodulated audio samples.
     *
     * @param listener to receive demodulated, resampled audio sample buffers.
     */
    public AudioBufferManager getAudioBufferManager() {
        return mAudioBufferManager;
    }

    public AIAudioOptimizer getAIAudioOptimizer() {
        return mAIAudioOptimizer;
    }

    public void setBufferListener(Listener<float[]> listener)
    {
        mResampledBufferListener = listener;
    }

    /**
     * Implements the IRealBufferProvider interface to deregister a listener from receiving demodulated audio samples.
     */
    @Override
    public void removeBufferListener()
    {
        mResampledBufferListener = null;
    }

    /**
     * Implements the IComplexSampleListener interface to receive a stream of complex sample buffers.
     */
    @Override
    public Listener<ComplexSamples> getComplexSamplesListener()
    {
        return this;
    }

    /**
     * Implements the Listener<ComplexSample> interface to receive a stream of complex I/Q sample buffers
     */
    @Override
    public void receive(ComplexSamples samples)
    {
        if(mIDecimationFilter == null || mQDecimationFilter == null)
        {
            throw new IllegalStateException("NBFM demodulator module must receive a sample rate change source event " +
                    "before it can process complex sample buffers");
        }

        // Feed I/Q data to watchdog for silence detection (checks every buffer, lightweight)
        if(mAudioWatchdog != null)
        {
            mAudioWatchdog.feedIQData(samples.i());
        }

        // Sample I/Q power every IQ_SAMPLE_INTERVAL-th buffer.  The AdaptiveGainAdvisor uses the WIDEBAND channel
        // power (the whole channelizer slice, approximating what the ADC/front-end sees); the Signal Power meter is
        // driven separately from the IN-BAND power measured after the baseband filter below, so it reflects the actual
        // narrowband signal instead of the near-constant wideband noise floor (which made the meter appear frozen).
        boolean samplePowerThisBuffer = (++mIQSampleCounter >= IQ_SAMPLE_INTERVAL);
        if(samplePowerThisBuffer)
        {
            mIQSampleCounter = 0;

            if(mUserPreferences.getAIPreference().isGainAdvisorEnabled())
            {
                float[] rawI = samples.i();
                float[] rawQ = samples.q();
                double sumSquared = 0.0;
                for(int idx = 0; idx < rawI.length; idx++)
                {
                    sumSquared += rawI[idx] * (double)rawI[idx] + rawQ[idx] * (double)rawQ[idx];
                }
                double powerDbfs = 10.0 * Math.log10(Math.max(sumSquared / rawI.length, 1e-20));
                AdaptiveGainAdvisor.getInstance(mUserPreferences).reportSignalLevel(mChannelName, mChannelFrequency, powerDbfs);
            }
        }

        float[] decimatedI = mIDecimationFilter.decimateReal(samples.i());
        float[] decimatedQ = mQDecimationFilter.decimateReal(samples.q());

        float[] filteredI = mIBasebandFilter.filter(decimatedI);
        float[] filteredQ = mQBasebandFilter.filter(decimatedQ);

        //Publish the in-band channel power (post-baseband-filter) so the Signal Power meter tracks the actual
        //narrowband signal: it sits near the in-band noise floor when idle and rises clearly during a transmission,
        //instead of showing the static wideband noise floor.
        if(samplePowerThisBuffer)
        {
            Listener<SourceEvent> sourceEventListener = mSourceEventListener;
            if(sourceEventListener != null && filteredI.length > 0)
            {
                double sumSquared = 0.0;
                for(int idx = 0; idx < filteredI.length; idx++)
                {
                    sumSquared += filteredI[idx] * (double)filteredI[idx] + filteredQ[idx] * (double)filteredQ[idx];
                }
                double inBandDbfs = 10.0 * Math.log10(Math.max(sumSquared / filteredI.length, 1e-20));
                sourceEventListener.receive(SourceEvent.channelPowerLevel(null, inBandDbfs));
            }
        }

        float[] demodulated = mDemodulator.demodulate(filteredI, filteredQ);

        mNoiseSquelch.process(demodulated);

        if(isToneGated())
        {
            //Tone-gated channel: the tone/code detector is the squelch.  Feed the FULL demodulated stream to the
            //resampler so the detector is never starved by a mistuned or closed noise squelch (the previous design
            //only forwarded audio while the noise squelch was open, which silently killed tone-filtered channels
            //whose noise squelch never opened).  processResampledAudio() gates output on the tone match, and the
            //call lifecycle is driven by the detector.  The noise squelch still runs above purely to render the
            //squelch activity chart.
            if(mResampler != null)
            {
                mResampler.resample(demodulated);
            }

            if(!mToneMatch)
            {
                notifyIdle();
            }
        }
        //Once we process the sample buffer, if the ending state is squelch closed, update the decoder state that we
        // are idle.
        else if(mNoiseSquelch.isSquelched())
        {
            notifyIdle();
        }
    }

    /**
     * Indicates whether AI audio buffering should capture this call's audio to disk.  The buffered
     * .raw files are only ever consumed by the NBFM audio auto-optimizer, so when that feature is
     * disabled (or the user has opted this channel out) we skip writing them entirely - avoiding
     * needless disk I/O, unbounded disk growth and per-call buffer allocation churn.
     */
    private boolean isAudioBufferingEnabled()
    {
        return mUserPreferences.getAIPreference().isNBFMAudioAutoOptimizeEnabled()
                && !mNBFMConfig.isAiAutoOptimizeOptedOut();
    }

    /**
     * Broadcasts a call start state event
     */
    private void notifyCallStart()
    {
        mCurrentCallSamples = 0;
        if(isAudioBufferingEnabled())
        {
            mAudioBufferManager.startEvent();
        }
        broadcast(new DecoderStateEvent(this, DecoderStateEvent.Event.START, State.CALL, 0));
    }

    /**
     * Broadcasts a call continuation state event
     */
    private void notifyCallContinuation()
    {
        broadcast(new DecoderStateEvent(this, DecoderStateEvent.Event.CONTINUATION, State.CALL, 0));
    }

    /**
     * Broadcasts a call end state event.  If the user has enabled AI audio auto-optimization, the
     * optimizer runs in the background once enough good audio has accumulated.  It runs as soon as
     * possible after startup (after MIN_QUALIFYING_CALLS calls of at least 5 seconds each) to get good
     * filters in place quickly, then follows the twice-daily cadence in AIPreference to limit token use.
     */
    private void notifyCallEnd()
    {
        mAudioBufferManager.endEvent();
        broadcast(new DecoderStateEvent(this, DecoderStateEvent.Event.END, State.CALL, 0));

        mCallEventCount++;

        //Count this as a "qualifying" call only if it contained at least 5 seconds of audio.
        if(mCurrentCallSamples >= MIN_QUALIFYING_CALL_SAMPLES)
        {
            mQualifyingCallCount++;
        }
        mCurrentCallSamples = 0;

        if(shouldAutoOptimizeNow() && mOptimizationRunning.compareAndSet(false, true))
        {
            //Mark the startup priming run done and record the time up-front so a failure or empty
            //buffer still respects the twice-daily cadence and we never hammer the API.
            mStartupOptimizeDone = true;
            mUserPreferences.getAIPreference().setNBFMLastOptimizeMs(mChannelName, System.currentTimeMillis());
            final DecodeConfigNBFM configSnapshot = mNBFMConfig;
            ThreadPool.CACHED.submit(() -> {
                try
                {
                    List<List<float[]>> events = mAudioBufferManager.getBufferedEvents();
                    if(!events.isEmpty())
                    {
                        String priorSummary = mUserPreferences.getAIPreference().getNBFMLastOptimizeSummary(mChannelName);
                        AIAnalysisResult result = mAIAudioOptimizer.analyzeRawAudio(configSnapshot, events, priorSummary);
                        applyAIOptimizationResult(result, "AUTO");
                        mLog.info("AI auto-optimization applied to NBFM channel: {}", result.getImprovements());
                    }
                }
                catch(Exception e)
                {
                    mLog.warn("NBFM auto-optimization failed: {}", e.getMessage());
                }
                finally
                {
                    mOptimizationRunning.set(false);
                }
            });
        }
    }

    /**
     * Decides whether an automatic NBFM filter optimization should run now.  Requires the feature to
     * be enabled and at least {@link #MIN_QUALIFYING_CALLS} qualifying calls (>= 5 seconds each) of
     * buffered audio.  The first ("startup") run fires as soon as that audio is available so good
     * filters are applied quickly; afterward it falls back to the twice-daily cadence in AIPreference.
     */
    private boolean shouldAutoOptimizeNow()
    {
        //#9 Auto runs (startup priming + ongoing cadence) require the auto-schedule toggle, which itself
        //requires the feature to be enabled.  When the feature is on but auto-schedule is off, only manual
        //runs occur (a separate path), so this method - which governs automatic runs only - returns false.
        if(!mUserPreferences.getAIPreference().isNBFMAutoScheduleEnabled())
        {
            return false;
        }

        //Respect a manual opt-out: if the user has edited any filter, leave all filters alone.
        if(mNBFMConfig.isAiAutoOptimizeOptedOut())
        {
            return false;
        }

        if(mQualifyingCallCount < MIN_QUALIFYING_CALLS)
        {
            return false;
        }

        if(!mStartupOptimizeDone)
        {
            //Startup priming: optimize as soon as enough good audio has accumulated this session.
            return true;
        }

        //Ongoing: at most twice a day per channel.
        return mUserPreferences.getAIPreference().isNBFMAutoOptimizeDue(mChannelName);
    }

    /**
     * Applies an AI analysis result to both the live audio filter chain and the persisted
     * channel configuration so the improved settings survive a restart.
     */
    private void applyAIOptimizationResult(AIAnalysisResult result, String trigger)
    {
        // Persist to config so settings survive channel restart / playlist save
        mNBFMConfig.setHissReductionEnabled(result.isHissReductionEnabled());
        mNBFMConfig.setHissReductionDb(result.getHissReductionDb());
        mNBFMConfig.setHissReductionCornerHz(result.getHissReductionCorner());
        mNBFMConfig.setLowPassEnabled(result.isLowPassEnabled());
        mNBFMConfig.setLowPassCutoff(result.getLowPassCutoff());
        mNBFMConfig.setDeemphasisEnabled(result.isDeemphasisEnabled());
        mNBFMConfig.setBassBoostEnabled(result.isBassBoostEnabled());
        mNBFMConfig.setBassBoostDb(result.getBassBoostDb());
        mNBFMConfig.setAgcEnabled(result.isAgcEnabled());
        mNBFMConfig.setAgcTargetLevel(result.getAgcTargetLevel());
        mNBFMConfig.setNoiseGateEnabled(result.isNoiseGateEnabled());
        mNBFMConfig.setNoiseGateThreshold(result.getNoiseGateThreshold());
        mNBFMConfig.setNoiseGateReduction(result.getNoiseGateReduction());
        mNBFMConfig.setNoiseGateHoldTime(result.getNoiseGateHoldTime());
        mNBFMConfig.setAgcMaxGain(result.getAgcMaxGain());
        mNBFMConfig.setSquelchTailRemovalEnabled(result.isSquelchTailRemovalEnabled());
        mNBFMConfig.setSquelchTailRemovalMs(result.getSquelchTailRemovalMs());
        mNBFMConfig.setSquelchHeadRemovalMs(result.getSquelchHeadRemovalMs());

        // Apply to live filter chain immediately (no restart needed)
        if(mAudioFilters != null)
        {
            mAudioFilters.setHissReductionEnabled(result.isHissReductionEnabled());
            mAudioFilters.setHissReductionDb(result.getHissReductionDb());
            mAudioFilters.setHissReductionCornerHz(result.getHissReductionCorner());
            mAudioFilters.setLowPassEnabled(result.isLowPassEnabled());
            mAudioFilters.setLowPassCutoff(result.getLowPassCutoff());
            mAudioFilters.setDeemphasisEnabled(result.isDeemphasisEnabled());
            mAudioFilters.setBassBoostEnabled(result.isBassBoostEnabled());
            mAudioFilters.setBassBoost(result.getBassBoostDb());
            mAudioFilters.setVoiceEnhanceEnabled(result.isAgcEnabled());
            mAudioFilters.setVoiceEnhancement(mapAgcTargetToVoiceEnhancement(result.getAgcTargetLevel()));
            float inputGainLinear = (float)Math.pow(10.0, result.getAgcMaxGain() / 40.0);
            mAudioFilters.setInputGain(inputGainLinear);
            mAudioFilters.setNoiseGateEnabled(result.isNoiseGateEnabled());
            mAudioFilters.setSquelchThreshold(result.getNoiseGateThreshold());
            mAudioFilters.setSquelchReduction(result.getNoiseGateReduction());
            mAudioFilters.setHoldTime(result.getNoiseGateHoldTime());
        }

        //Apply the squelch tail/head trim to the live remover too, so an auto-optimization takes effect immediately
        //instead of staying stale until the channel restarts.
        if(mSquelchTailRemover != null)
        {
            mSquelchTailRemover.setTailRemovalMs(result.getSquelchTailRemovalMs());
            mSquelchTailRemover.setHeadRemovalMs(result.getSquelchHeadRemovalMs());
        }

        recordOptimizationSummary(result, trigger);
    }

    /**
     * Records a human-readable summary (trigger, time, what changed and why) of the optimization just
     * applied, so the channel UI can show the last run and the optimizer can learn from its prior change.
     */
    private void recordOptimizationSummary(AIAnalysisResult result, String trigger)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(trigger).append(" ")
          .append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date()))
          .append("] ");

        String improvements = result.getImprovements();
        String issues = result.getIssuesFound();

        if(improvements != null && !improvements.isEmpty())
        {
            sb.append("Changes: ").append(improvements);
        }
        if(issues != null && !issues.isEmpty())
        {
            if(improvements != null && !improvements.isEmpty())
            {
                sb.append("  ");
            }
            sb.append("Why: ").append(issues);
        }

        mUserPreferences.getAIPreference().setNBFMLastOptimizeSummary(mChannelName, sb.toString());
    }

    /**
     * Broadcasts an idle notification
     */
    private void notifyIdle()
    {
        broadcast(new DecoderStateEvent(this, DecoderStateEvent.Event.CONTINUATION, State.IDLE, 0));
    }

    /**
     * Broadcasts the decoder state event to an optional registered listener
     */
    private void broadcast(DecoderStateEvent event)
    {
        if(mDecoderStateEventListener != null)
        {
            mDecoderStateEventListener.receive(event);
        }
    }

    /**
     * Sets the decoder state listener
     */
    @Override
    public void setDecoderStateListener(Listener<DecoderStateEvent> listener)
    {
        mDecoderStateEventListener = listener;
    }

    /**
     * Removes the decoder state event listener
     */
    @Override
    public void removeDecoderStateListener()
    {
        mDecoderStateEventListener = null;
    }

    /**
     * Updates the decoder to process complex sample buffers at the specified sample rate.
     * @param sampleRate of the incoming complex sample buffer stream.
     */
    private void setSampleRate(double sampleRate)
    {
        int decimationRate = 0;
        double decimatedSampleRate = sampleRate;

        if(sampleRate / 2 >= (mChannelBandwidth * 2))
        {
            decimationRate = 2;

            while(sampleRate / decimationRate / 2 >= (mChannelBandwidth * 2))
            {
                decimationRate *= 2;
            }
        }

        if(decimationRate > 0)
        {
            decimatedSampleRate /= decimationRate;
        }

        mIDecimationFilter = DecimationFilterFactory.getRealDecimationFilter(decimationRate);
        mQDecimationFilter = DecimationFilterFactory.getRealDecimationFilter(decimationRate);

        if((decimatedSampleRate < (2.0 * mChannelBandwidth)))
        {
            throw new IllegalStateException(getDecoderType().name() + " demodulator with channel bandwidth [" + mChannelBandwidth + "] requires a channel sample rate of [" + (2.0 * mChannelBandwidth + "] - sample rate of [" + decimatedSampleRate + "] is not supported"));
        }

        mNoiseSquelch.setSampleRate(decimatedSampleRate);

        int passBandStop = (int) (mChannelBandwidth * .8);
        int stopBandStart = (int) mChannelBandwidth;

        float[] coefficients = null;

        FIRFilterSpecification specification = FIRFilterSpecification.lowPassBuilder().sampleRate(decimatedSampleRate * 2).gridDensity(16).oddLength(true).passBandCutoff(passBandStop).passBandAmplitude(1.0).passBandRipple(0.01).stopBandStart(stopBandStart).stopBandAmplitude(0.0).stopBandRipple(0.005) //Approximately 90 dB attenuation
                .build();

        try
        {
            coefficients = FilterFactory.getTaps(specification);
        }
        catch(FilterDesignException fde)
        {
            mLog.error("Couldn't design demodulator remez filter for sample rate [" + sampleRate + "] pass frequency [" + passBandStop + "] and stop frequency [" + stopBandStart + "] - will proceed using sinc (low-pass) filter");
        }

        if(coefficients == null)
        {
            mLog.info("Unable to use remez filter designer for sample rate [" + decimatedSampleRate + "] pass band stop [" + passBandStop + "] and stop band start [" + stopBandStart + "] - will proceed using simple low pass filter design");
            coefficients = FilterFactory.getLowPass(decimatedSampleRate, passBandStop, stopBandStart, 60, WindowType.HAMMING, true);
        }

        mIBasebandFilter = FilterFactory.getRealFilter(coefficients);
        mQBasebandFilter = FilterFactory.getRealFilter(coefficients);

        // Create resampler — output goes through our processing chain instead of directly to broadcast
        mResampler = new RealResampler(decimatedSampleRate, DEMODULATED_AUDIO_SAMPLE_RATE, 4192, 512);
        mResampler.setListener(NBFMDecoder.this::processResampledAudio);

        // Initialize CTCSS detector at 8 kHz (resampled audio rate) if tone filtering is enabled
        if(mToneFilterEnabled && mTargetCTCSSCodes != null && !mTargetCTCSSCodes.isEmpty())
        {
            mCTCSSDetector = new CTCSSDetector(mTargetCTCSSCodes, (float) DEMODULATED_AUDIO_SAMPLE_RATE);
            mCTCSSDetector.setListener(new CTCSSDetector.CTCSSDetectorListener()
            {
                @Override
                public void ctcssDetected(CTCSSCode code)
                {
                    // Matching tone confirmed — wake up the channel like a real radio unsquelching
                    boolean wasBlocked = !mToneMatch;
                    mToneMatch = true;
                    mLastToneMatchTime = System.currentTimeMillis();

                    // A new tone match begins a candidate call; it becomes a real call (call start + audio) only
                    // once it sustains the configured minimum duration, handled in processToneGatedAudio().
                    if(wasBlocked)
                    {
                        onToneCandidateStart();
                    }

                    if(mDecoderState != null)
                    {
                        mDecoderState.setDetectedCTCSS(code);
                    }
                }

                @Override
                public void ctcssRejected(CTCSSCode code)
                {
                    // Wrong tone confirmed — end any qualified call (or discard an unqualified candidate) and idle.
                    mToneMatch = false;
                    onToneCandidateEnd();

                    if(mDecoderState != null)
                    {
                        mDecoderState.setRejectedCTCSS(code);
                    }
                }

                @Override
                public void ctcssLost()
                {
                    // Tone lost — end any qualified call (or discard an unqualified candidate) and idle.
                    mToneMatch = false;
                    onToneCandidateEnd();

                    if(mDecoderState != null)
                    {
                        mDecoderState.setToneLost();
                    }
                }
            });

            mLog.info("CTCSSDetector initialized at {} Hz sample rate", DEMODULATED_AUDIO_SAMPLE_RATE);
        }

        // Initialize DCS detector at 8 kHz if DCS filtering is enabled
        if(mToneFilterEnabled && mToneFilterType == ChannelToneFilter.ToneType.DCS
                && mTargetDCSCodes != null && !mTargetDCSCodes.isEmpty())
        {
            mDCSDetector = new DCSDetector(mTargetDCSCodes);
            mDCSDetector.setListener(new DCSDetector.DCSDetectorListener()
            {
                @Override
                public void dcsDetected(DCSCode code)
                {
                    // Matching code confirmed — wake up the channel
                    boolean wasBlocked = !mToneMatch;
                    mToneMatch = true;
                    mLastToneMatchTime = System.currentTimeMillis();

                    if(wasBlocked)
                    {
                        onToneCandidateStart();
                    }

                    if(mDecoderState != null)
                    {
                        mDecoderState.setDetectedDCS(code);
                    }
                }

                @Override
                public void dcsRejected(DCSCode code)
                {
                    // Wrong code confirmed — end any qualified call (or discard an unqualified candidate) and idle.
                    mToneMatch = false;
                    onToneCandidateEnd();

                    if(mDecoderState != null)
                    {
                        mDecoderState.setRejectedDCS(code);
                    }
                }

                @Override
                public void dcsLost()
                {
                    // Code lost — end any qualified call (or discard an unqualified candidate) and idle.
                    mToneMatch = false;
                    onToneCandidateEnd();

                    if(mDecoderState != null)
                    {
                        mDecoderState.setToneLost();
                    }
                }
            });

            mLog.info("DCSDetector initialized for channel-level DCS filtering");
        }

        // Initialize squelch tail remover if enabled
        if(mSquelchTailRemovalEnabled)
        {
            mSquelchTailRemover = new SquelchTailRemover(mSquelchTailRemovalMs, mSquelchHeadRemovalMs);
            mSquelchTailRemover.setOutputListener(NBFMDecoder.this::broadcast);
            mLog.info("SquelchTailRemover initialized: tail={}ms, head={}ms", mSquelchTailRemovalMs, mSquelchHeadRemovalMs);
        }

        // Initialize VoxSend audio filter chain at the resampled audio rate (8 kHz)
        initializeAudioFilters(DEMODULATED_AUDIO_SAMPLE_RATE);
    }

    /**
     * Initializes the VoxSend audio filter chain from the channel configuration.
     * Applies settings for low-pass, de-emphasis, bass boost, voice enhancement, and noise gate.
     *
     * @param sampleRate the audio sample rate (typically 8000 Hz after resampling)
     */
    private void initializeAudioFilters(double sampleRate)
    {
        mAudioFilters = new NBFMAudioFilters(sampleRate);

        // Low-pass filter
        mAudioFilters.setLowPassEnabled(mNBFMConfig.isLowPassEnabled());
        mAudioFilters.setLowPassCutoff(mNBFMConfig.getLowPassCutoff());

        // FM de-emphasis
        mAudioFilters.setDeemphasisEnabled(mNBFMConfig.isDeemphasisEnabled());
        mAudioFilters.setDeemphasisTimeConstant(mNBFMConfig.getDeemphasisTimeConstant());

        // Bass boost
        mAudioFilters.setBassBoostEnabled(mNBFMConfig.isBassBoostEnabled());
        mAudioFilters.setBassBoost(mNBFMConfig.getBassBoostDb());

        // Voice enhancement (stored as AGC target level, mapped from -30...-6 dB to 0...1.0)
        mAudioFilters.setVoiceEnhanceEnabled(mNBFMConfig.isAgcEnabled());
        float voiceEnhanceAmount = mapAgcTargetToVoiceEnhancement(mNBFMConfig.getAgcTargetLevel());
        mAudioFilters.setVoiceEnhancement(voiceEnhanceAmount);

        // Input gain (stored as AGC max gain in dB, map to linear)
        float inputGainDb = mNBFMConfig.getAgcMaxGain();
        float inputGainLinear = (float)Math.pow(10.0, inputGainDb / 40.0); // half the dB for reasonable mapping
        mAudioFilters.setInputGain(inputGainLinear);

        // Noise gate
        mAudioFilters.setNoiseGateEnabled(mNBFMConfig.isNoiseGateEnabled());
        mAudioFilters.setSquelchThreshold(mNBFMConfig.getNoiseGateThreshold());
        mAudioFilters.setSquelchReduction(mNBFMConfig.getNoiseGateReduction());
        mAudioFilters.setHoldTime(mNBFMConfig.getNoiseGateHoldTime());

        // Hiss reduction (high-shelf cut above corner frequency)
        mAudioFilters.setHissReductionCornerHz(mNBFMConfig.getHissReductionCornerHz());
        mAudioFilters.setHissReductionDb(mNBFMConfig.getHissReductionDb());
        mAudioFilters.setHissReductionEnabled(mNBFMConfig.isHissReductionEnabled());

        mLog.info("VoxSend audio filters initialized: lowPass={} ({}Hz), deemphasis={} ({}μs), " +
                "hissReduction={} ({}dB@{}Hz), bassBoost={} ({}dB), voiceEnhance={} ({}), noiseGate={} ({}%)",
                mNBFMConfig.isLowPassEnabled(), mNBFMConfig.getLowPassCutoff(),
                mNBFMConfig.isDeemphasisEnabled(), mNBFMConfig.getDeemphasisTimeConstant(),
                mNBFMConfig.isHissReductionEnabled(), mNBFMConfig.getHissReductionDb(), mNBFMConfig.getHissReductionCornerHz(),
                mNBFMConfig.isBassBoostEnabled(), mNBFMConfig.getBassBoostDb(),
                mNBFMConfig.isAgcEnabled(), voiceEnhanceAmount,
                mNBFMConfig.isNoiseGateEnabled(), mNBFMConfig.getNoiseGateThreshold());
    }

    /**
     * Maps the AGC target level (stored as -30 to -6 dB) to voice enhancement amount (0.0 to 1.0).
     * The AGC target level field is repurposed to store voice enhancement strength.
     */
    private float mapAgcTargetToVoiceEnhancement(float agcTargetLevel)
    {
        // agcTargetLevel range is -30 to -6 dB, map to 0.0 to 1.0
        // -30 dB = 0.0 (no enhancement), -6 dB = 1.0 (max enhancement)
        float normalized = (agcTargetLevel - (-30.0f)) / ((-6.0f) - (-30.0f));
        return Math.max(0.0f, Math.min(1.0f, normalized));
    }

    /**
     * Monitors sample rate change source event(s) to set up the filters, decimation, and demodulator.
     */
    public class SourceEventProcessor implements Listener<SourceEvent>
    {
        @Override
        public void receive(SourceEvent sourceEvent)
        {
            if(sourceEvent.getEvent() == SourceEvent.Event.NOTIFICATION_SAMPLE_RATE_CHANGE)
            {
                setSampleRate(sourceEvent.getValue().doubleValue());
            }
            else if(sourceEvent.getEvent() == SourceEvent.Event.NOTIFICATION_FREQUENCY_CHANGE)
            {
                //Track the channel's center frequency so the gain advisor can attribute it to a tuner.
                mChannelFrequency = sourceEvent.getValue().longValue();
            }
        }
    }
}
