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

    private final ExecutorService mExecutorService = Executors.newSingleThreadExecutor();
    private final LinkedTransferQueue<AudioBufferWrapper> mAudioQueue = new LinkedTransferQueue<>();
    private final AtomicBoolean mRunning = new AtomicBoolean(true);

    private final PlaylistManager mPlaylistManager;
    private final BroadcastModel mBroadcastModel;

    // Per-segment cache for alias-based audio routing.  Alias membership is constant for the lifetime of an audio
    // segment, so we resolve the applicable detectors once per segment instead of for every 20 ms audio block.
    private AudioSegment mLastRoutedSegment;
    private java.util.Set<String> mLastApplicableDetectors = java.util.Collections.emptySet();

    // Discovery tracking
    public static final List<TwoToneDiscoveryLog> DISCOVERY_LOG = new ArrayList<>();

    // State machine for Tone A -> Tone B
    private double mCurrentToneA = 0.0;
    private int mCurrentToneABlocks = 0;
    private double mCurrentToneB = 0.0;
    private int mCurrentToneBBlocks = 0;

    // FFT Auto-Discovery variables
    private static final int FFT_SIZE = 4096;
    private final float[] mRingBuffer = new float[FFT_SIZE];
    private int mRingIndex = 0;
    private int mSamplesReceived = 0;
    private int mFftCounter = 0;
    private FloatFFT_1D mFft = new FloatFFT_1D(FFT_SIZE);

    private double mDiscoveryCurrentToneA = 0.0;
    private int mDiscoveryToneABlocks = 0;
    private double mDiscoveryCurrentToneB = 0.0;
    private int mDiscoveryToneBBlocks = 0;

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
        if(buffer != null && buffer.length == BLOCK_SIZE)
        {
            mAudioQueue.offer(new AudioBufferWrapper(buffer.clone(), segment));
        }
    }

    private void processBuffer(float[] buffer, AudioSegment segment)
    {
        PlaylistV2 playlist = mPlaylistManager.getCurrentPlaylist();
        if(playlist == null) return;

        List<TwoToneConfiguration> configs = playlist.getTwoToneConfigurations();
        boolean discoveryEnabled = playlist.isToneDiscoveryEnabled();

        if (configs.isEmpty() && !discoveryEnabled)
        {
            return; // Nothing to do
        }

        if (discoveryEnabled) {
            for (int i = 0; i < buffer.length; i++) {
                mRingBuffer[mRingIndex] = buffer[i];
                mRingIndex = (mRingIndex + 1) % FFT_SIZE;
                mSamplesReceived++;
                mFftCounter++;
            }
        }

        // To make this fully optimized, we would typically run an FFT or a bank of Goertzel filters
        // For simplicity and since we want a "magical" experience, we'll scan known configurations

        boolean matchedToneAThisBlock = false;
        boolean matchedToneBThisBlock = false;

        //Alias-based routing: a detector only receives this segment's audio when one of its selected aliases resolves
        //on the segment (or when it has no aliases selected, in which case it runs globally for backward compatibility).
        java.util.Set<String> applicableDetectors = getApplicableDetectorNames(segment, configs);

        for(TwoToneConfiguration config : configs)
        {
            if (!config.isEnabled()) continue;
            if (!applicableDetectors.contains(config.getAlias())) continue;

            long freqA = Math.round(config.getToneA());
            long freqB = Math.round(config.getToneB());
            double tol = config.getFrequencyTolerance();
            int minToneBlocks = Math.max(1, (int)(config.getToneDurationMs() / 20));

            if (freqA <= 0) continue;
            if (!config.isLongATone() && freqB <= 0) continue;

            int powerA = getTolerancePower(buffer, freqA, tol);

            if (config.isLongATone())
            {
                if (powerA > POWER_THRESHOLD_DB)
                {
                    matchedToneAThisBlock = true;
                    if(mCurrentToneA == config.getToneA())
                    {
                        mCurrentToneABlocks++;
                    }
                    else
                    {
                        mCurrentToneA = config.getToneA();
                        mCurrentToneABlocks = 1;
                    }

                    if(mCurrentToneABlocks == LONG_A_MIN_TONE_BLOCKS)
                    {
                        triggerAlertIfMatched(config, segment);
                        // Do not reset mCurrentToneABlocks to 0 here.
                        // Allow it to keep incrementing as long as the tone is present
                        // to prevent multiple triggers for the same continuous long tone.
                    }
                }
                else if (mCurrentToneA == config.getToneA())
                {
                    mCurrentToneA = 0;
                    mCurrentToneABlocks = 0;
                }
                continue;
            }

            int powerB = getTolerancePower(buffer, freqB, tol);

            // Tone A detection
            if (powerA > POWER_THRESHOLD_DB)
            {
                matchedToneAThisBlock = true;
                if(mCurrentToneA == config.getToneA())
                {
                    mCurrentToneABlocks++;
                }
                else
                {
                    mCurrentToneA = config.getToneA();
                    mCurrentToneABlocks = 1;
                    mCurrentToneB = 0;
                    mCurrentToneBBlocks = 0;
                }
            }
            // Tone B detection (only valid if Tone A was previously detected and held)
            else if (powerB > POWER_THRESHOLD_DB && mCurrentToneABlocks >= minToneBlocks && mCurrentToneA == config.getToneA())
            {
                matchedToneBThisBlock = true;
                if(mCurrentToneB == config.getToneB())
                {
                    mCurrentToneBBlocks++;
                }
                else
                {
                    mCurrentToneB = config.getToneB();
                    mCurrentToneBBlocks = 1;
                }


                // If B is held long enough, it's a confirmed sequence
                if(mCurrentToneBBlocks >= minToneBlocks)
                {
                    triggerAlertIfMatched(config, segment);

                    // Reset to avoid multiple triggers for the same continuous tone
                    mCurrentToneA = 0;
                    mCurrentToneABlocks = 0;
                    mCurrentToneB = 0;
                    mCurrentToneBBlocks = 0;
                }

            }
        }

        // If in discovery mode and we found strong unknown tones (simulated logic for unknown frequencies)
        // A true discovery mode would require an FFT to find the strongest peak frequency
        if (discoveryEnabled && !matchedToneAThisBlock && !matchedToneBThisBlock)
        {
            if (mFftCounter >= 800 && mSamplesReceived >= FFT_SIZE) {
                mFftCounter = 0;
                float[] fftBuffer = new float[FFT_SIZE];
                for (int i = 0; i < FFT_SIZE; i++) {
                    fftBuffer[i] = mRingBuffer[(mRingIndex + i) % FFT_SIZE];
                }
                
                mFft.realForward(fftBuffer);
                
                double maxMagnitude = 0;
                int maxIndex = -1;
                for (int i = 1; i < FFT_SIZE / 2; i++) {
                    float re = fftBuffer[2 * i];
                    float im = fftBuffer[2 * i + 1];
                    double mag = re * re + im * im;
                    if (mag > maxMagnitude) {
                        maxMagnitude = mag;
                        maxIndex = i;
                    }
                }
                
                if (maxMagnitude > 1000.0) { // relative threshold
                    double frequency = (double) maxIndex * SAMPLE_RATE / FFT_SIZE;
                    frequency = Math.round(frequency);
                    
                    if (frequency >= 300 && frequency <= 3000) {
                        if (mDiscoveryCurrentToneB > 0) {
                            if (Math.abs(mDiscoveryCurrentToneB - frequency) < 5) {
                                mDiscoveryToneBBlocks++;
                                if (mDiscoveryToneBBlocks >= 3) { // 3 * 100ms = 300ms
                                    logDiscovery(mDiscoveryCurrentToneA, mDiscoveryCurrentToneB, segment);
                                    mDiscoveryCurrentToneA = 0;
                                    mDiscoveryToneABlocks = 0;
                                    mDiscoveryCurrentToneB = 0;
                                    mDiscoveryToneBBlocks = 0;
                                }
                            } else {
                                mDiscoveryCurrentToneB = 0;
                                mDiscoveryToneBBlocks = 0;
                            }
                        } else if (mDiscoveryCurrentToneA > 0) {
                            if (Math.abs(mDiscoveryCurrentToneA - frequency) < 5) {
                                mDiscoveryToneABlocks++;
                            } else {
                                if (mDiscoveryToneABlocks >= 3) {
                                    mDiscoveryCurrentToneB = frequency;
                                    mDiscoveryToneBBlocks = 1;
                                } else {
                                    mDiscoveryCurrentToneA = frequency;
                                    mDiscoveryToneABlocks = 1;
                                }
                            }
                        } else {
                            mDiscoveryCurrentToneA = frequency;
                            mDiscoveryToneABlocks = 1;
                        }
                    }
                } else {
                    mDiscoveryCurrentToneA = 0;
                    mDiscoveryToneABlocks = 0;
                    mDiscoveryCurrentToneB = 0;
                    mDiscoveryToneBBlocks = 0;
                }
            }
        }
    }

    /**
     * Determines which detector configurations should receive this audio segment, based on the alias(es) selected for
     * each detector.  A detector that has one or more aliases selected only receives audio from segments whose resolved
     * alias selected it (alias-routed live audio).  A detector with no aliases selected runs globally against all audio,
     * preserving the legacy behavior.  Results are cached per audio segment because alias membership is constant for the
     * lifetime of a segment and this is evaluated for every 20 ms audio block.
     */
    private java.util.Set<String> getApplicableDetectorNames(AudioSegment segment, List<TwoToneConfiguration> configs)
    {
        if(segment == mLastRoutedSegment)
        {
            return mLastApplicableDetectors;
        }

        java.util.Set<String> segmentDetectors = resolveSegmentDetectorNames(segment);
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
            else if(!detectorHasAnyAliasMapping(name))
            {
                applicable.add(name); //No aliases selected for this detector -> global (legacy) behavior
            }
        }

        mLastRoutedSegment = segment;
        mLastApplicableDetectors = applicable;
        return applicable;
    }

    /**
     * Resolves the set of two-tone detector names referenced by the alias(es) that match the segment's identifiers.
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
            io.github.dsheirer.alias.AliasList aliasList =
                mPlaylistManager.getAliasModel().getAliasList(segment.getIdentifierCollection());

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
        catch(Exception e)
        {
            mLog.error("Error resolving aliases for two tone detector audio routing", e);
        }

        return names;
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
            return new GoertzelFilter(SAMPLE_RATE, freq, BLOCK_SIZE, WindowType.BLACKMAN).getPower(buffer.clone());
        }
        int p1 = new GoertzelFilter(SAMPLE_RATE, freq, BLOCK_SIZE, WindowType.BLACKMAN).getPower(buffer.clone());
        int p2 = new GoertzelFilter(SAMPLE_RATE, (long)(freq - tol), BLOCK_SIZE, WindowType.BLACKMAN).getPower(buffer.clone());
        int p3 = new GoertzelFilter(SAMPLE_RATE, (long)(freq + tol), BLOCK_SIZE, WindowType.BLACKMAN).getPower(buffer.clone());
        return Math.max(p1, Math.max(p2, p3));
    }


    private void triggerAlertIfMatched(TwoToneConfiguration config, AudioSegment segment)
    {
        boolean shouldTrigger = true;
        // Check alias associations
        if (segment != null) {
            io.github.dsheirer.alias.AliasList aliasList = mPlaylistManager.getAliasModel().getAliasList(segment.getIdentifierCollection());
            boolean foundMapping = false;
            if (aliasList != null) {
                for (io.github.dsheirer.identifier.Identifier identifier : segment.getIdentifierCollection().getIdentifiers()) {
                    java.util.List<io.github.dsheirer.alias.Alias> aliases = aliasList.getAliases(identifier);
                    if (aliases != null) {
                        for (io.github.dsheirer.alias.Alias alias : aliases) {
                            if (alias.hasTwoToneDetector(config.getAlias())) {
                                foundMapping = true;
                                break;
                            }
                        }
                    }
                    if (foundMapping) break;
                }
            }

            // If we didn't find a mapping on the segment's aliases, we need to see if the config has ANY aliases at all.
            // If it has aliases configured but they didn't match, we skip.
            // If it has NO aliases configured, we trigger (global mode).
            if (!foundMapping) {
                boolean hasAnyMapping = false;
                for (io.github.dsheirer.alias.Alias alias : mPlaylistManager.getAliasModel().aliasList()) {
                    if (alias.hasTwoToneDetector(config.getAlias())) {
                        hasAnyMapping = true;
                        break;
                    }
                }
                if (hasAnyMapping) {
                    shouldTrigger = false;
                }
            }
        }

        if (shouldTrigger) {
            mLog.info("Two Tone Detected: {} (A:{} B:{})", config.getAlias(), config.getToneA(), config.getToneB());
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

        if (mAntiFloodFilter != null && !mAntiFloodFilter.checkAndRecord("TWOTONE:" + alias)) {
            mLog.info("Suppressed duplicate Two-Tone Zello alert for alias: {}", alias);
            return;
        }

        boolean sendText = config.isEnableZelloTextMessage();
        boolean sendTone = config.isEnableZelloAlert() && config.getZelloAlertFile() != null &&
                !config.getZelloAlertFile().isEmpty();

        if(sendText || sendTone)
        {
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
                
                if (resource != null) {
                    if (path.toLowerCase().endsWith(".mp3")) {
                        final java.net.URL finalResource = resource;
                        javafx.application.Platform.runLater(() -> {
                            try {
                                javafx.scene.media.Media media = new javafx.scene.media.Media(finalResource.toURI().toString());
                                javafx.scene.media.MediaPlayer mediaPlayer = new javafx.scene.media.MediaPlayer(media);
                                mediaPlayer.play();
                            } catch (Exception ex) {
                                mLog.error("Error playing mp3 alert", ex);
                            }
                        });
                    } else {
                        javax.sound.sampled.AudioInputStream ais = javax.sound.sampled.AudioSystem.getAudioInputStream(resource);
                        javax.sound.sampled.Clip clip = javax.sound.sampled.AudioSystem.getClip();
                        clip.open(ais);
                        clip.start();
                    }
                } else {
                    java.io.File file = new java.io.File(path);
                    if (file.exists()) {
                        if (path.toLowerCase().endsWith(".mp3")) {
                            javafx.application.Platform.runLater(() -> {
                                try {
                                    javafx.scene.media.Media media = new javafx.scene.media.Media(file.toURI().toString());
                                    javafx.scene.media.MediaPlayer mediaPlayer = new javafx.scene.media.MediaPlayer(media);
                                    mediaPlayer.play();
                                } catch (Exception ex) {
                                    mLog.error("Error playing mp3 alert", ex);
                                }
                            });
                        } else {
                            javax.sound.sampled.AudioInputStream ais = javax.sound.sampled.AudioSystem.getAudioInputStream(file);
                            javax.sound.sampled.Clip clip = javax.sound.sampled.AudioSystem.getClip();
                            clip.open(ais);
                            clip.start();
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
            if (Math.abs(config.getToneA() - toneA) < config.getFrequencyTolerance() && 
                Math.abs(config.getToneB() - toneB) < config.getFrequencyTolerance()) {
                exists = true;
                break;
            }
        }
        
        if (!exists) {
            // Emit event for AI Tone Discovery Manager
            io.github.dsheirer.eventbus.MyEventBus.getGlobalEventBus().post(new ToneDiscoveredEvent(toneA, toneB, segment));
        }
        
        String channel = "Unknown";
        if(segment != null) {
            io.github.dsheirer.identifier.Identifier id = segment.getIdentifierCollection().getIdentifier(io.github.dsheirer.identifier.IdentifierClass.CONFIGURATION, io.github.dsheirer.identifier.Form.CHANNEL, io.github.dsheirer.identifier.Role.ANY);
            if (id instanceof io.github.dsheirer.identifier.configuration.ChannelNameConfigurationIdentifier) {
                channel = ((io.github.dsheirer.identifier.configuration.ChannelNameConfigurationIdentifier)id).getValue();
            }
        }
        org.slf4j.LoggerFactory.getLogger(io.github.dsheirer.log.TwoToneLog.LOGGER_NAME).info("[Channel: {}] - [Unknown]", channel);
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
