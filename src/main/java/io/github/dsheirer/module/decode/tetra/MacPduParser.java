package io.github.dsheirer.module.decode.tetra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses TETRA Medium Access Control (MAC) Protocol Data Units (PDU).
 */
public class MacPduParser {
    private static final Logger mLog = LoggerFactory.getLogger(MacPduParser.class);

    public void parse(byte[] pduPayload) {
        // TODO: Extract Logical Channels (TCH, SCH, AACH)
        mLog.debug("Received {} bytes for TETRA MAC PDU parsing", pduPayload.length);
    }
}
