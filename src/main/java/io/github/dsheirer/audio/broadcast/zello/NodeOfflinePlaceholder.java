package io.github.dsheirer.audio.broadcast.zello;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Injects placeholder silence frames when a Zello node goes offline.
 * Prevents stream disconnection by maintaining a minimal audio heartbeat
 * until the node reconnects.
 */
public class NodeOfflinePlaceholder
{
    private static final Logger mLog = LoggerFactory.getLogger(NodeOfflinePlaceholder.class);
    private static final int SILENCE_FRAME_SIZE = 960; // 20ms at 48kHz mono
    private boolean mInjecting = false;

    /**
     * Returns a silence frame (zeroed PCM samples) suitable for Opus encoding.
     */
    public byte[] getSilenceFrame()
    {
        return new byte[SILENCE_FRAME_SIZE * 2]; // 16-bit PCM silence
    }

    public void startInjection()
    {
        mInjecting = true;
        mLog.info("Node offline - injecting placeholder silence");
    }

    public void stopInjection()
    {
        mInjecting = false;
        mLog.info("Node back online - stopping placeholder");
    }

    public boolean isInjecting()
    {
        return mInjecting;
    }
}
