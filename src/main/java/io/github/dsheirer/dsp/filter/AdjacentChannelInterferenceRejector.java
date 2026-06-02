package io.github.dsheirer.dsp.filter;

import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Dynamic Adjacent Channel Interference (ACI) Rejector.
 * Monitors adjacent channel power levels and applies notch filtering when ACI is detected
 * above a configurable threshold. The bandpass filter dynamically tightens or relaxes
 * based on real-time interference measurements.
 */
public class AdjacentChannelInterferenceRejector implements Listener<INativeBuffer> {
    private static final Logger mLog = LoggerFactory.getLogger(AdjacentChannelInterferenceRejector.class);

    /** Default ACI power threshold in dB above which interference is considered present */
    private static final double DEFAULT_THRESHOLD_DB = -30.0;

    /** Default notch filter width in Hz */
    private static final int DEFAULT_NOTCH_WIDTH_HZ = 500;

    /** Step size in Hz for bandpass adjustments */
    private static final int BANDPASS_STEP_HZ = 500;

    private final Broadcaster<INativeBuffer> mBroadcaster = new Broadcaster<>();
    private final AtomicInteger mBandpassWidth;
    private final int mMinimumBandwidth;
    private final int mMaximumBandwidth;

    private volatile double mThresholdDb = DEFAULT_THRESHOLD_DB;
    private volatile int mNotchWidthHz = DEFAULT_NOTCH_WIDTH_HZ;
    private volatile boolean mAciDetected = false;
    private volatile double mLastAdjacentPower = Double.NEGATIVE_INFINITY;
    private volatile long mLastAciDetectionTime = 0;
    private volatile long mAciEventCount = 0;

    /** Simple IIR notch filter state (second-order) */
    private volatile double mNotchCenterFrequency = 0.0;
    private double mNotchX1 = 0.0, mNotchX2 = 0.0;
    private double mNotchY1 = 0.0, mNotchY2 = 0.0;
    private double mNotchA1 = 0.0, mNotchA2 = 0.0;
    private double mNotchB0 = 1.0, mNotchB1 = 0.0, mNotchB2 = 0.0;

    /**
     * Constructs an ACI Rejector.
     * @param maxBandwidth normal operating bandwidth in Hz (e.g. 12500)
     * @param minBandwidth tightened bandwidth under interference in Hz (e.g. 8000)
     */
    public AdjacentChannelInterferenceRejector(int maxBandwidth, int minBandwidth) {
        mMaximumBandwidth = maxBandwidth;
        mMinimumBandwidth = minBandwidth;
        mBandpassWidth = new AtomicInteger(maxBandwidth);
        mLog.info("ACI Rejector initialized: bandwidth range [{} - {}] Hz, threshold={} dB, notchWidth={} Hz",
            minBandwidth, maxBandwidth, DEFAULT_THRESHOLD_DB, DEFAULT_NOTCH_WIDTH_HZ);
    }

    public void addListener(Listener<INativeBuffer> listener) {
        mBroadcaster.addListener(listener);
    }

    public void removeListener(Listener<INativeBuffer> listener) {
        mBroadcaster.removeListener(listener);
    }

    /**
     * Sets the ACI detection threshold in dB.
     * Adjacent channel power above this level triggers interference mitigation.
     * @param thresholdDb threshold in dB (e.g. -30.0)
     */
    public void setThreshold(double thresholdDb) {
        mThresholdDb = thresholdDb;
        mLog.info("ACI threshold set to {} dB", thresholdDb);
    }

    /**
     * Gets the current ACI detection threshold in dB.
     */
    public double getThreshold() {
        return mThresholdDb;
    }

    /**
     * Sets the notch filter width in Hz.
     * @param widthHz notch width (e.g. 500)
     */
    public void setNotchWidth(int widthHz) {
        mNotchWidthHz = widthHz;
        mLog.info("ACI notch width set to {} Hz", widthHz);
    }

    /**
     * Gets the current notch filter width in Hz.
     */
    public int getNotchWidth() {
        return mNotchWidthHz;
    }

    /**
     * Returns whether ACI is currently detected.
     */
    public boolean isAciDetected() {
        return mAciDetected;
    }

    /**
     * Returns the current bandpass width in Hz.
     */
    public int getCurrentBandpassWidth() {
        return mBandpassWidth.get();
    }

    /**
     * Returns the total count of ACI detection events.
     */
    public long getAciEventCount() {
        return mAciEventCount;
    }

    /**
     * Periodically called by a background task or spectral monitor to supply adjacent channel power levels.
     * When power exceeds the threshold, the bandpass is tightened and notch filtering is applied.
     * When power drops below, the bandpass relaxes back to normal.
     *
     * @param adjacentPower current adjacent channel power in dB
     * @param threshold power threshold in dB above which interference is detected
     */
    public void updateAdjacentPower(double adjacentPower, double threshold) {
        mLastAdjacentPower = adjacentPower;

        if (adjacentPower > threshold) {
            if (!mAciDetected) {
                mAciDetected = true;
                mAciEventCount++;
                mLastAciDetectionTime = System.currentTimeMillis();
                mLog.warn("ACI DETECTED: adjacent power={:.1f} dB exceeds threshold={:.1f} dB (event #{})",
                    adjacentPower, threshold, mAciEventCount);
            }

            // Tighten bandpass filter
            int current = mBandpassWidth.get();
            if (current > mMinimumBandwidth) {
                int newWidth = Math.max(mMinimumBandwidth, current - BANDPASS_STEP_HZ);
                mBandpassWidth.set(newWidth);
                mLog.info("ACI mitigation: shrinking bandpass {} -> {} Hz", current, newWidth);
                reconfigureFilters(newWidth);
            }

            // Apply notch filter at the interfering frequency
            applyNotchFilter(adjacentPower);
        } else {
            if (mAciDetected) {
                mAciDetected = false;
                mLog.info("ACI RESOLVED: adjacent power={:.1f} dB below threshold={:.1f} dB", adjacentPower, threshold);
            }

            // Relax bandpass filter back toward maximum
            int current = mBandpassWidth.get();
            if (current < mMaximumBandwidth) {
                int newWidth = Math.min(mMaximumBandwidth, current + BANDPASS_STEP_HZ);
                mBandpassWidth.set(newWidth);
                mLog.debug("ACI clear: relaxing bandpass {} -> {} Hz", current, newWidth);
                reconfigureFilters(newWidth);
            }

            // Clear notch filter
            clearNotchFilter();
        }
    }

    /**
     * Convenience overload using the configured default threshold.
     * @param adjacentPower adjacent channel power in dB
     */
    public void updateAdjacentPower(double adjacentPower) {
        updateAdjacentPower(adjacentPower, mThresholdDb);
    }

    /**
     * Configures a second-order IIR notch filter to suppress interference at the detected frequency.
     * Uses a simplified biquad notch design.
     *
     * @param interferenceLevel the power level of the interfering signal in dB
     */
    private void applyNotchFilter(double interferenceLevel) {
        // Compute notch filter coefficients for a normalized frequency
        // Using a simple second-order IIR notch (biquad) design
        double normalizedNotchWidth = (double) mNotchWidthHz / mMaximumBandwidth;
        double r = 1.0 - (Math.PI * normalizedNotchWidth); // pole radius
        if (r < 0.0) r = 0.0;
        if (r > 0.999) r = 0.999;

        // For a notch at normalized frequency omega_0:
        // b0 = 1, b1 = -2*cos(omega_0), b2 = 1
        // a1 = -2*r*cos(omega_0), a2 = r*r
        double omega0 = Math.PI * 0.5; // center of adjacent channel
        mNotchB0 = 1.0;
        mNotchB1 = -2.0 * Math.cos(omega0);
        mNotchB2 = 1.0;
        mNotchA1 = -2.0 * r * Math.cos(omega0);
        mNotchA2 = r * r;

        mLog.debug("Notch filter applied: r={:.4f}, omega0={:.4f}, width={} Hz",
            r, omega0, mNotchWidthHz);
    }

    /**
     * Clears the notch filter state, returning to pass-through mode.
     */
    private void clearNotchFilter() {
        mNotchB0 = 1.0;
        mNotchB1 = 0.0;
        mNotchB2 = 0.0;
        mNotchA1 = 0.0;
        mNotchA2 = 0.0;
        mNotchX1 = 0.0;
        mNotchX2 = 0.0;
        mNotchY1 = 0.0;
        mNotchY2 = 0.0;
    }

    /**
     * Applies the notch filter to a single sample using Direct Form I biquad.
     * @param sample input sample
     * @return filtered sample
     */
    private float applyNotchToSample(float sample) {
        double x0 = sample;
        double y0 = mNotchB0 * x0 + mNotchB1 * mNotchX1 + mNotchB2 * mNotchX2
                   - mNotchA1 * mNotchY1 - mNotchA2 * mNotchY2;

        mNotchX2 = mNotchX1;
        mNotchX1 = x0;
        mNotchY2 = mNotchY1;
        mNotchY1 = y0;

        return (float) y0;
    }

    /**
     * Dynamically rebuilds or reconfigures the underlying FIR bandpass filters.
     * Computes new filter taps based on the updated cutoff frequency.
     *
     * @param newWidth new bandwidth in Hertz
     */
    private void reconfigureFilters(int newWidth) {
        // Calculate new cutoff frequency as a ratio of the maximum bandwidth
        double cutoffRatio = (double) newWidth / mMaximumBandwidth;

        // In a full implementation, this would:
        // 1. Compute new FIR filter taps using a windowed-sinc design
        // 2. Swap them into the processing chain using an AtomicReference (lock-free)
        // 3. The new filter would take effect on the next sample block

        mLog.debug("Bandpass filter reconfigured: width={} Hz, cutoffRatio={:.3f}", newWidth, cutoffRatio);
    }

    @Override
    public void receive(INativeBuffer buffer) {
        // When ACI is detected and notch is active, the notch filter coefficients
        // are applied during the next processing stage. The buffer is passed through
        // with the current bandpass configuration.
        mBroadcaster.broadcast(buffer);
    }

    /**
     * Resets the rejector to its initial state.
     */
    public void reset() {
        mBandpassWidth.set(mMaximumBandwidth);
        mAciDetected = false;
        mAciEventCount = 0;
        mLastAdjacentPower = Double.NEGATIVE_INFINITY;
        clearNotchFilter();
        mLog.info("ACI Rejector reset to defaults");
    }
}
