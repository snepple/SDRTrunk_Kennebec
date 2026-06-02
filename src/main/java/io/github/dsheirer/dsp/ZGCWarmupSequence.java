package io.github.dsheirer.dsp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ZGC warm-up sequence that allocates and releases temporary buffers during startup
 * to trigger ZGC concurrent cycles before real-time DSP begins.
 */
public class ZGCWarmupSequence {
    private static final Logger mLog = LoggerFactory.getLogger(ZGCWarmupSequence.class);

    public static void execute() {
        mLog.info("ZGC warm-up: pre-allocating GC heap regions...");
        long start = System.nanoTime();
        byte[][] warmup = new byte[64][];
        for (int i = 0; i < 64; i++) { warmup[i] = new byte[1024 * 1024]; } // 64MB total
        for (int i = 0; i < 64; i++) { warmup[i] = null; }
        System.gc();
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        mLog.info("ZGC warm-up complete in {}ms", (System.nanoTime() - start) / 1_000_000);
    }
}
