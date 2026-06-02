package io.github.dsheirer.dsp.filter;

import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Dynamic Adjacent Channel Interference (ACI) Rejector.
 * Monitors adjacent FFT bins (or signal power) and dynamically tightens bandpass filters
 * in real-time to mitigate interference from adjacent signals.
 */
public class AdjacentChannelInterferenceRejector implements Listener<INativeBuffer> {
    private static final Logger mLog = LoggerFactory.getLogger(AdjacentChannelInterferenceRejector.class);
    
    private Broadcaster<INativeBuffer> mBroadcaster = new Broadcaster<>();
    private AtomicInteger mBandpassWidth = new AtomicInteger(12500); // default 12.5 kHz
    private final int mMinimumBandwidth;
    private final int mMaximumBandwidth;

    /**
     * Constructs an ACI Rejector.
     * @param maxBandwidth normal operating bandwidth (e.g. 12500)
     * @param minBandwidth tightened bandwidth under interference (e.g. 8000)
     */
    public AdjacentChannelInterferenceRejector(int maxBandwidth, int minBandwidth) {
        mMaximumBandwidth = maxBandwidth;
        mMinimumBandwidth = minBandwidth;
        mBandpassWidth.set(maxBandwidth);
    }
    
    public void addListener(Listener<INativeBuffer> listener) {
        mBroadcaster.addListener(listener);
    }
    
    public void removeListener(Listener<INativeBuffer> listener) {
        mBroadcaster.removeListener(listener);
    }

    /**
     * Periodically called by a background task or spectral monitor to supply adjacent bin power levels.
     * @param adjacentPower current adjacent channel power metric
     * @param threshold power threshold above which interference is detected
     */
    public void updateAdjacentPower(double adjacentPower, double threshold) {
        if (adjacentPower > threshold) {
            // High interference, shrink bandpass
            int current = mBandpassWidth.get();
            if (current > mMinimumBandwidth) {
                int newWidth = Math.max(mMinimumBandwidth, current - 500); // shrink by 500 Hz
                mBandpassWidth.set(newWidth);
                mLog.debug("ACI detected. Shrinking bandpass filter to {} Hz", newWidth);
                reconfigureFilters(newWidth);
            }
        } else {
            // Low interference, relax bandpass
            int current = mBandpassWidth.get();
            if (current < mMaximumBandwidth) {
                int newWidth = Math.min(mMaximumBandwidth, current + 500); // relax by 500 Hz
                mBandpassWidth.set(newWidth);
                mLog.debug("ACI resolved. Relaxing bandpass filter to {} Hz", newWidth);
                reconfigureFilters(newWidth);
            }
        }
    }
    
    /**
     * Dynamically rebuilds or reconfigures the underlying FIR filters.
     * @param newWidth new bandwidth in Hertz
     */
    private void reconfigureFilters(int newWidth) {
        // In a full implementation, this calculates new filter taps using the new cutoff frequency
        // and swaps them in using a lock-free reference update.
    }

    @Override
    public void receive(INativeBuffer buffer) {
        // Apply dynamic filtering here.
        // For now, this is a pass-through shell.
        mBroadcaster.broadcast(buffer);
    }
}
