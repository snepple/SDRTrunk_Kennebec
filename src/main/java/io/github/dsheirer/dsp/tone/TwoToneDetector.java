package io.github.dsheirer.dsp.tone;

import io.github.dsheirer.dsp.filter.GoertzelFilter;
import io.github.dsheirer.dsp.window.WindowType;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.playlist.PlaylistV2;
import io.github.dsheirer.playlist.TwoToneConfiguration;
import io.github.dsheirer.playlist.TwoToneDiscoveryLog;
import io.github.dsheirer.audio.broadcast.zello.ZelloBroadcaster;
import io.github.dsheirer.audio.broadcast.BroadcastConfiguration;
import io.github.dsheirer.audio.broadcast.zello.ZelloConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicBoolean;

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

    private static final int POWER_THRESHOLD_DB = 10; // Simple threshold, tune as needed

    private final ExecutorService mExecutorService = Executors.newSingleThreadExecutor();
    private final LinkedTransferQueue<float[]> mAudioQueue = new LinkedTransferQueue<>();
    private final AtomicBoolean mRunning = new AtomicBoolean(true);

    private final PlaylistManager mPlaylistManager;
    private final List<ZelloBroadcaster> mZelloBroadcasters;

    // Discovery tracking
    public static final List<TwoToneDiscoveryLog> DISCOVERY_LOG = new ArrayList<>();

    // State machine for Tone A -> Tone B
    private double mCurrentToneA = 0.0;
    private int mCurrentToneABlocks = 0;
    private double mCurrentToneB = 0.0;
    private int mCurrentToneBBlocks = 0;

    public TwoToneDetector(PlaylistManager playlistManager, List<ZelloBroadcaster> zelloBroadcasters)
    {
        mPlaylistManager = playlistManager;
        mZelloBroadcasters = zelloBroadcasters;

        mExecutorService.submit(() -> {
            while (mRunning.get())
            {
                try
                {
                    float[] buffer = mAudioQueue.take();
                    processBuffer(buffer);
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

    public void processAudio(float[] buffer)
    {
        if(buffer != null && buffer.length == BLOCK_SIZE)
        {
            mAudioQueue.offer(buffer.clone());
        }
    }

    private void processBuffer(float[] buffer)
    {
        PlaylistV2 playlist = mPlaylistManager.getCurrentPlaylist();
        if(playlist == null) return;

        List<TwoToneConfiguration> configs = playlist.getTwoToneConfigurations();
        boolean discoveryEnabled = playlist.isToneDiscoveryEnabled();

        if (configs.isEmpty() && !discoveryEnabled)
        {
            return; // Nothing to do
        }

        // To make this fully optimized, we would typically run an FFT or a bank of Goertzel filters
        // For simplicity and since we want a "magical" experience, we'll scan known configurations

        boolean matchedToneAThisBlock = false;
        boolean matchedToneBThisBlock = false;

        for(TwoToneConfiguration config : configs)
        {
            long freqA = Math.round(config.getToneA());
            long freqB = Math.round(config.getToneB());

            if (freqA <= 0 || freqB <= 0) continue;

            GoertzelFilter filterA = new GoertzelFilter(SAMPLE_RATE, freqA, BLOCK_SIZE, WindowType.BLACKMAN);
            GoertzelFilter filterB = new GoertzelFilter(SAMPLE_RATE, freqB, BLOCK_SIZE, WindowType.BLACKMAN);

            int powerA = filterA.getPower(buffer.clone());
            int powerB = filterB.getPower(buffer.clone());

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
            else if (powerB > POWER_THRESHOLD_DB && mCurrentToneABlocks >= MIN_TONE_BLOCKS && mCurrentToneA == config.getToneA())
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
                if(mCurrentToneBBlocks >= MIN_TONE_BLOCKS)
                {
                    mLog.info("Two Tone Detected: {} (A:{} B:{})", config.getAlias(), config.getToneA(), config.getToneB());
                    triggerAlert(config);

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
            // Placeholder for FFT unknown peak detection logic
            // E.g. finding a 600Hz tone that isn't mapped
            // logDiscovery(600.0, 700.0);
        }
    }

    private void triggerAlert(TwoToneConfiguration config)
    {
        String template = config.getTemplate() != null ? config.getTemplate() : "Dispatch Received: %ALIAS%";
        String text = template.replace("%ALIAS%", config.getAlias() != null ? config.getAlias() : "Unknown");

        for(ZelloBroadcaster broadcaster : mZelloBroadcasters)
        {
            BroadcastConfiguration bc = broadcaster.getBroadcastConfiguration();
            if(bc instanceof ZelloConfiguration)
            {
                ZelloConfiguration zc = (ZelloConfiguration) bc;
                if(config.getZelloChannel() != null && config.getZelloChannel().equals(zc.getChannel()))
                {
                    mLog.info("Sending Zello Alert to {}: {}", zc.getChannel(), text);
                    broadcaster.sendTextMessage(text);
                }
            }
        }
    }

    private void logDiscovery(double toneA, double toneB)
    {
        DISCOVERY_LOG.add(new TwoToneDiscoveryLog(toneA, toneB));
    }

    public void dispose()
    {
        mRunning.set(false);
        mExecutorService.shutdownNow();
    }
}
