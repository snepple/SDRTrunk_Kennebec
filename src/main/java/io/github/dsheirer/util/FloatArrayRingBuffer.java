package io.github.dsheirer.util;

/**
 * Ring buffer for primitive arrays to eliminate continuous object allocation
 * in high-speed DSP and audio decoding paths. By cycling through a fixed set of pre-allocated
 * arrays, GC pressure is reduced without requiring complex object lifecycle management or release() calls.
 */
public class FloatArrayRingBuffer {
    private final float[][] mBuffers;
    private int mPointer = 0;

    /**
     * Constructs a ring buffer of float arrays.
     * @param bufferCount number of arrays in the ring
     * @param arraySize size of each float array
     */
    public FloatArrayRingBuffer(int bufferCount, int arraySize) {
        mBuffers = new float[bufferCount][arraySize];
    }

    /**
     * Gets the next float array in the ring.
     * @return clean float array
     */
    public float[] next() {
        float[] buffer = mBuffers[mPointer];
        mPointer = (mPointer + 1) % mBuffers.length;
        // Clean array before returning
        java.util.Arrays.fill(buffer, 0.0f);
        return buffer;
    }
}
