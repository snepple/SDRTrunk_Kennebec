package io.github.dsheirer.spectrum;

import java.util.BitSet;

/**
 * Visual decimator for spectral data to reduce rendering load.
 * Implements the Ramer-Douglas-Peucker (RDP) algorithm.
 */
public class VisualDecimator {
    
    /**
     * Simplifies the array of magnitudes using RDP.
     * @param data array of float magnitudes
     * @param epsilon tolerance
     * @return array of indices that are kept
     */
    public static int[] decimate(float[] data, float epsilon) {
        if (data == null || data.length < 3) {
            if (data == null) return new int[0];
            int[] res = new int[data.length];
            for (int i = 0; i < data.length; i++) res[i] = i;
            return res;
        }

        BitSet bitSet = new BitSet(data.length);
        bitSet.set(0);
        bitSet.set(data.length - 1);
        
        rdp(data, 0, data.length - 1, epsilon, bitSet);
        
        int[] result = new int[bitSet.cardinality()];
        int idx = 0;
        for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i + 1)) {
            result[idx++] = i;
        }
        return result;
    }

    private static void rdp(float[] data, int startIdx, int endIdx, float epsilon, BitSet bitSet) {
        float dmax = 0f;
        int index = startIdx;

        float startX = startIdx;
        float startY = data[startIdx];
        float endX = endIdx;
        float endY = data[endIdx];

        float deltaX = endX - startX;
        float deltaY = endY - startY;
        float mag = deltaX * deltaX + deltaY * deltaY;

        for (int i = startIdx + 1; i < endIdx; i++) {
            float px = i;
            float py = data[i];

            float d;
            if (mag == 0) {
                float dx = px - startX;
                float dy = py - startY;
                d = dx * dx + dy * dy;
            } else {
                float u = ((px - startX) * deltaX + (py - startY) * deltaY) / mag;
                if (u < 0) {
                    float dx = px - startX;
                    float dy = py - startY;
                    d = dx * dx + dy * dy;
                } else if (u > 1) {
                    float dx = px - endX;
                    float dy = py - endY;
                    d = dx * dx + dy * dy;
                } else {
                    float ix = startX + u * deltaX;
                    float iy = startY + u * deltaY;
                    float dx = px - ix;
                    float dy = py - iy;
                    d = dx * dx + dy * dy;
                }
            }
            if (d > dmax) {
                index = i;
                dmax = d;
            }
        }

        if (dmax > epsilon * epsilon) {
            bitSet.set(index);
            rdp(data, startIdx, index, epsilon, bitSet);
            rdp(data, index, endIdx, epsilon, bitSet);
        }
    }
}
