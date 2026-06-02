package io.github.dsheirer.audio.broadcast;

import io.github.dsheirer.alias.id.broadcast.BroadcastChannel;
import io.github.dsheirer.audio.AudioSegment;
import io.github.dsheirer.identifier.IdentifierCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Utility for verifying audio paths by injecting test tones.
 */
public class StreamHealthMonitor {
    private static final Logger mLog = LoggerFactory.getLogger(StreamHealthMonitor.class);
    private static final ScheduledExecutorService mExecutor = Executors.newSingleThreadScheduledExecutor();

    /**
     * Injects a standard 1004 Hz test tone into a specific broadcaster for testing stream continuity.
     * @param broadcaster The real-time broadcaster to test
     * @param durationMs Duration of the test tone in milliseconds
     */
    public static void injectTestTone(IRealTimeAudioBroadcaster broadcaster, int durationMs) {
        if (broadcaster == null || !broadcaster.isRealTimeReady()) {
            mLog.warn("Broadcaster is not ready to receive test tones");
            return;
        }

        mLog.info("Injecting {}ms test tone into broadcaster", durationMs);
        broadcaster.startRealTimeStream(new IdentifierCollection());

        // Generate 1004 Hz test tone at 8000 Hz sample rate
        // 10ms frame = 80 samples
        int frames = durationMs / 10;
        final float[] toneBuffer = new float[80];
        for (int i = 0; i < toneBuffer.length; i++) {
            toneBuffer[i] = (float) Math.sin(2.0 * Math.PI * 1004.0 * i / 8000.0) * 0.5f;
        }

        mExecutor.execute(() -> {
            try {
                for (int i = 0; i < frames; i++) {
                    broadcaster.receiveRealTimeAudio(toneBuffer);
                    Thread.sleep(10);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                broadcaster.stopRealTimeStream();
                mLog.info("Finished test tone injection");
            }
        });
    }
}
