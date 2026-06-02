package io.github.dsheirer.dsp;

import net.openhft.affinity.AffinityLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages CPU affinity pinning for real-time DSP threads using OpenHFT Affinity.
 * Pinning threads to specific CPU cores reduces context-switch overhead and improves
 * cache locality for latency-sensitive DSP processing.
 */
public class ThreadPinningManager {
    private static final Logger mLog = LoggerFactory.getLogger(ThreadPinningManager.class);
    private static final ConcurrentHashMap<String, AffinityLock> mLocks = new ConcurrentHashMap<>();

    public static AffinityLock pinCurrentThread(String name) {
        try {
            AffinityLock lock = AffinityLock.acquireLock();
            mLocks.put(name, lock);
            mLog.info("Thread [{}] pinned to CPU {}", name, lock.cpuId());
            return lock;
        } catch (Exception e) {
            mLog.warn("Could not pin thread [{}]: {}", name, e.getMessage());
            return null;
        }
    }

    public static void releaseAll() {
        mLocks.forEach((name, lock) -> {
            try { lock.release(); mLog.info("Released thread pin for [{}]", name); }
            catch (Exception e) { mLog.warn("Error releasing pin for [{}]", name, e); }
        });
        mLocks.clear();
    }
}
