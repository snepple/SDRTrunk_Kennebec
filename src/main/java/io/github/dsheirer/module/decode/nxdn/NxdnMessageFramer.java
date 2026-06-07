package io.github.dsheirer.module.decode.nxdn;

import io.github.dsheirer.dsp.symbol.Dibit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NXDN Message Framer.
 * Receives stream of Dibits, detects Sync Words, and extracts LICH and payload frames.
 */
public class NxdnMessageFramer {
    private static final Logger mLog = LoggerFactory.getLogger(NxdnMessageFramer.class);
    
    private boolean mSyncDetected = false;
    private int mBitCount = 0;

    public void process(Dibit symbol) {
        // Basic framer logic (placeholder)
        if(mSyncDetected) {
            mBitCount += 2;
            if(mBitCount >= 384) { // NXDN typical frame size placeholder
                mLog.debug("NXDN Frame Received");
                mSyncDetected = false;
                mBitCount = 0;
            }
        }
    }

    public void syncDetected() {
        mSyncDetected = true;
        mBitCount = 0;
    }
    
    public boolean isAssembling() {
        return mSyncDetected;
    }
}
