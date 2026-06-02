package io.github.dsheirer.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * High-performance object pool for primitive arrays to reduce allocation pressure 
 * in DSP and decoding loops.
 */
public class BufferPool {
    private static final BufferPool INSTANCE = new BufferPool();
    
    private final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<float[]>> floatPools = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<byte[]>> bytePools = new ConcurrentHashMap<>();
    
    private BufferPool() {}
    
    public static BufferPool getInstance() {
        return INSTANCE;
    }
    
    /**
     * Obtains a float array of the specified size.
     */
    public float[] getFloatArray(int size) {
        ConcurrentLinkedQueue<float[]> pool = floatPools.computeIfAbsent(size, k -> new ConcurrentLinkedQueue<>());
        float[] arr = pool.poll();
        if (arr == null) {
            return new float[size];
        }
        return arr;
    }
    
    /**
     * Returns a float array to the pool.
     */
    public void returnFloatArray(float[] array) {
        if (array == null) return;
        ConcurrentLinkedQueue<float[]> pool = floatPools.get(array.length);
        if (pool != null) {
            pool.offer(array);
        }
    }
    
    /**
     * Obtains a byte array of the specified size.
     */
    public byte[] getByteArray(int size) {
        ConcurrentLinkedQueue<byte[]> pool = bytePools.computeIfAbsent(size, k -> new ConcurrentLinkedQueue<>());
        byte[] arr = pool.poll();
        if (arr == null) {
            return new byte[size];
        }
        return arr;
    }
    
    /**
     * Returns a byte array to the pool.
     */
    public void returnByteArray(byte[] array) {
        if (array == null) return;
        ConcurrentLinkedQueue<byte[]> pool = bytePools.get(array.length);
        if (pool != null) {
            pool.offer(array);
        }
    }
}
