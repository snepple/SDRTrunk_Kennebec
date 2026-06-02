package io.github.dsheirer.source.tuner.manager;

import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Orchestrator that dynamically ingests signal quality reports from multiple tuners
 * and selects the tuner with the best signal quality (highest SNR, lowest BER).
 * Thread-safe using ConcurrentHashMap for concurrent tuner report ingestion.
 */
public class SignalVotingOrchestrator implements Listener<CorrectedBinaryMessage> {
    private static final Logger mLog = LoggerFactory.getLogger(SignalVotingOrchestrator.class);

    /** Default BER threshold: messages with BER above this are discarded */
    private static final double DEFAULT_BER_THRESHOLD = 0.05;

    private final Broadcaster<CorrectedBinaryMessage> mBroadcaster = new Broadcaster<>();
    private final AtomicReference<String> mActiveSourceId = new AtomicReference<>();
    private final ConcurrentHashMap<String, SignalQualityReport> mReports = new ConcurrentHashMap<>();
    private volatile double mBerThreshold = DEFAULT_BER_THRESHOLD;

    /**
     * Signal quality report from a single tuner, containing SNR and BER metrics.
     */
    public static class SignalQualityReport {
        private final String mTunerId;
        private final long mFrequency;
        private final double mSnr;
        private final double mBer;
        private final long mTimestamp;

        public SignalQualityReport(String tunerId, long frequency, double snr, double ber) {
            mTunerId = tunerId;
            mFrequency = frequency;
            mSnr = snr;
            mBer = ber;
            mTimestamp = System.currentTimeMillis();
        }

        public String getTunerId() { return mTunerId; }
        public long getFrequency() { return mFrequency; }
        public double getSnr() { return mSnr; }
        public double getBer() { return mBer; }
        public long getTimestamp() { return mTimestamp; }

        /**
         * Computes a composite quality score: higher is better.
         * SNR contributes positively, BER contributes negatively (scaled by 100 for weighting).
         */
        public double getQualityScore() {
            return mSnr - (mBer * 100.0);
        }

        @Override
        public String toString() {
            return String.format("Tuner[%s] freq=%dHz SNR=%.1fdB BER=%.4f score=%.2f",
                mTunerId, mFrequency, mSnr, mBer, getQualityScore());
        }
    }

    /**
     * Submits a signal quality report from a tuner. Thread-safe.
     * @param report the quality report from a tuner
     */
    public void submitReport(SignalQualityReport report) {
        mReports.put(report.getTunerId(), report);
        mLog.debug("Signal quality report: {}", report);

        // Re-evaluate best tuner on each new report
        String previousBest = mActiveSourceId.get();
        String newBest = evaluateBestTuner();

        if (newBest != null && !newBest.equals(previousBest)) {
            mActiveSourceId.set(newBest);
            SignalQualityReport bestReport = mReports.get(newBest);
            mLog.info("Voting switch: active tuner changed from [{}] to [{}] (SNR={:.1f}dB, BER={:.4f})",
                previousBest, newBest,
                bestReport != null ? bestReport.getSnr() : 0.0,
                bestReport != null ? bestReport.getBer() : 0.0);
        }
    }

    /**
     * Evaluates all current reports and returns the tuner ID with the best quality score.
     * Best = highest SNR combined with lowest BER.
     * @return tuner ID of the best tuner, or null if no reports
     */
    private String evaluateBestTuner() {
        String bestId = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (Map.Entry<String, SignalQualityReport> entry : mReports.entrySet()) {
            SignalQualityReport report = entry.getValue();

            // Skip stale reports (older than 10 seconds)
            if (System.currentTimeMillis() - report.getTimestamp() > 10_000) {
                continue;
            }

            double score = report.getQualityScore();
            if (score > bestScore) {
                bestScore = score;
                bestId = entry.getKey();
            }
        }

        return bestId;
    }

    /**
     * Gets the currently active (best) tuner ID.
     * @return tuner ID or null if no tuner is selected
     */
    public String getBestTuner() {
        return mActiveSourceId.get();
    }

    /**
     * Gets the signal quality report for the currently active tuner.
     * @return the report, or null if no active tuner
     */
    public SignalQualityReport getBestTunerReport() {
        String bestId = mActiveSourceId.get();
        return bestId != null ? mReports.get(bestId) : null;
    }

    /**
     * Removes a tuner from the voting pool (e.g. when disconnected).
     * @param tunerId the tuner to remove
     */
    public void removeTuner(String tunerId) {
        mReports.remove(tunerId);
        mLog.info("Removed tuner [{}] from voting pool", tunerId);

        // If the removed tuner was the active one, re-evaluate
        if (tunerId.equals(mActiveSourceId.get())) {
            String newBest = evaluateBestTuner();
            mActiveSourceId.set(newBest);
            mLog.info("Active tuner removed; new best tuner: [{}]", newBest);
        }
    }

    /**
     * Purges stale reports older than the specified age in milliseconds.
     * @param maxAgeMs maximum age of reports to keep
     */
    public void purgeStaleReports(long maxAgeMs) {
        long cutoff = System.currentTimeMillis() - maxAgeMs;
        mReports.entrySet().removeIf(entry -> entry.getValue().getTimestamp() < cutoff);
    }

    /**
     * Sets the BER threshold. Messages with BER above this are discarded.
     * @param threshold BER threshold (0.0 to 1.0)
     */
    public void setBerThreshold(double threshold) {
        mBerThreshold = threshold;
    }

    /**
     * Returns the number of tuners currently in the voting pool.
     */
    public int getTunerCount() {
        return mReports.size();
    }

    public void addListener(Listener<CorrectedBinaryMessage> listener) {
        mBroadcaster.addListener(listener);
    }

    public void removeListener(Listener<CorrectedBinaryMessage> listener) {
        mBroadcaster.removeListener(listener);
    }

    @Override
    public void receive(CorrectedBinaryMessage message) {
        if (message != null) {
            double ber = (double) message.getCorrectedBitCount() / message.size();

            // Only forward if BER is below threshold (message is clean enough)
            if (ber <= mBerThreshold) {
                mBroadcaster.broadcast(message);
            }
        }
    }
}
