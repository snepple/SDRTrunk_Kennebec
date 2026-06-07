package io.github.dsheirer.module.decode.nxdn;

/**
 * Frame Sync and Message extraction for NXDN
 */
public class NxdnFrame {
    private byte[] payload;
    
    public NxdnFrame(byte[] rawBits) {
        this.payload = rawBits;
    }
    
    /**
     * Finds the Frame Sync Word (FSW) within a stream of bits
     */
    public static int findSyncWord(byte[] stream) {
        // TODO: Implement correlation against NXDN FSW (0x0020 or similar based on mode)
        return -1;
    }
    
    public byte[] getPayload() {
        return payload;
    }
}
