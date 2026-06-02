package io.github.dsheirer.source.tuner.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rotates through a list of control channel frequencies on sync loss,
 * enabling automatic recovery when the primary control channel becomes unavailable.
 */
public class ControlChannelHunt {
    private static final Logger mLog = LoggerFactory.getLogger(ControlChannelHunt.class);
    private final List<Long> mFrequencies;
    private final AtomicInteger mCurrentIndex = new AtomicInteger(0);
    private volatile boolean mHunting = false;

    public ControlChannelHunt(List<Long> frequencies) {
        mFrequencies = frequencies;
        mLog.info("ControlChannelHunt initialized with {} frequencies", frequencies.size());
    }

    public long nextFrequency() {
        if (mFrequencies.isEmpty()) return 0;
        int idx = mCurrentIndex.getAndUpdate(i -> (i + 1) % mFrequencies.size());
        long freq = mFrequencies.get(idx);
        mLog.info("Control channel hunt -> {} Hz (index {})", freq, idx);
        return freq;
    }

    public void startHunt() { mHunting = true; mLog.info("Control channel hunt STARTED"); }
    public void stopHunt() { mHunting = false; mLog.info("Control channel hunt STOPPED"); }
    public boolean isHunting() { return mHunting; }
    public void reset() { mCurrentIndex.set(0); }
}
