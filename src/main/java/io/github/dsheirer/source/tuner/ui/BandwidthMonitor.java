package io.github.dsheirer.source.tuner.ui;

import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.DiscoveredUSBTuner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class BandwidthMonitor {
    private static final Logger mLog = LoggerFactory.getLogger(BandwidthMonitor.class);
    public static final long SOFT_CEILING_THRESHOLD = 30 * 1024 * 1024; // 30 MB/s

    public static long calculateBusBandwidth(List<DiscoveredTuner> tuners, int busNumber) {
        long totalBandwidth = 0;
        for (DiscoveredTuner tuner : tuners) {
            if (tuner instanceof DiscoveredUSBTuner usbTuner && usbTuner.getBus() == busNumber) {
                if (usbTuner.hasTuner() && usbTuner.getTunerStatus() == io.github.dsheirer.source.tuner.manager.TunerStatus.ENABLED) {
                    long currentSampleRate = 0;
                    try {
                        currentSampleRate = (long) usbTuner.getTuner().getTunerController().getCurrentSampleRate();
                    } catch (Exception e) {
                        mLog.error("Error getting sample rate", e);
                    }
                    double sampleSizeBits = usbTuner.getTuner().getSampleSize();
                    long bytesPerSecond = (long) ((currentSampleRate * sampleSizeBits) / 8.0);
                    totalBandwidth += bytesPerSecond;
                }
            }
        }
        return totalBandwidth;
    }

    public static boolean willExceedThreshold(List<DiscoveredTuner> tuners, DiscoveredUSBTuner tunerToStart) {
        int targetBus = tunerToStart.getBus();
        long currentBandwidth = calculateBusBandwidth(tuners, targetBus);

        long requestedBandwidth = 0;
        if (tunerToStart.hasTuner()) {
            requestedBandwidth = tunerToStart.getTuner().getMaximumUSBBitsPerSecond() / 8;
        } else {
            // estimate max
            requestedBandwidth = 320000000 / 8; // generic
        }

        return (currentBandwidth + requestedBandwidth) > SOFT_CEILING_THRESHOLD;
    }
}
