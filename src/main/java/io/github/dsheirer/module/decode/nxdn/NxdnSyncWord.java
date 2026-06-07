package io.github.dsheirer.module.decode.nxdn;

public enum NxdnSyncWord {
    FSW_4800(0x1D55, 4800),
    FSW_9600(0x3566, 9600); // Note: Hypothetical placeholders for stub

    private final int mSyncPattern;
    private final int mBaudRate;

    NxdnSyncWord(int syncPattern, int baudRate) {
        mSyncPattern = syncPattern;
        mBaudRate = baudRate;
    }

    public int getBaudRate() {
        return mBaudRate;
    }
}
