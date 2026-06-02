package io.github.dsheirer.dsp;

public class FastTrig {
    private static final int BITS = 16;
    private static final int SIZE = 1 << BITS;
    private static final int MASK = SIZE - 1;
    private static final double RAD_TO_INDEX = SIZE / (Math.PI * 2.0);

    private static final float[] SIN_TABLE = new float[SIZE];

    static {
        for (int i = 0; i < SIZE; i++) {
            SIN_TABLE[i] = (float) Math.sin((i * Math.PI * 2.0) / SIZE);
        }
    }

    /**
     * Fast sine approximation using a 65536-entry look-up table.
     * @param radians angle in radians
     * @return approximate sine
     */
    public static float sin(double radians) {
        int index = (int) (radians * RAD_TO_INDEX) & MASK;
        return SIN_TABLE[index];
    }

    /**
     * Fast cosine approximation using a 65536-entry look-up table.
     * @param radians angle in radians
     * @return approximate cosine
     */
    public static float cos(double radians) {
        int index = (int) ((radians + Math.PI / 2.0) * RAD_TO_INDEX) & MASK;
        return SIN_TABLE[index];
    }
}
