package io.github.dsheirer.dsp.filter.fir.real;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import org.apache.commons.lang3.ArrayUtils;

public class NativeRealFIRFilter implements IRealFilter {

    private final float[] mCoefficients;
    private float[] mBuffer;
    private final int mBufferOverlap;

    private ByteBuffer mSamplesBufferNative;
    private FloatBuffer mSamplesBuffer;

    private ByteBuffer mFilteredBufferNative;
    private FloatBuffer mFilteredBuffer;

    private ByteBuffer mInternalBufferNative;
    private FloatBuffer mInternalBuffer;

    private ByteBuffer mCoefficientsBufferNative;
    private FloatBuffer mCoefficientsBuffer;

    private int mLastSampleLength = -1;

    public NativeRealFIRFilter(float[] coefficients) {
        mCoefficients = coefficients;
        ArrayUtils.reverse(mCoefficients);
        mBufferOverlap = mCoefficients.length - 1;
        mBuffer = new float[mCoefficients.length];

        mCoefficientsBufferNative = ByteBuffer.allocateDirect(mCoefficients.length * 4);
        mCoefficientsBufferNative.order(ByteOrder.nativeOrder());
        mCoefficientsBuffer = mCoefficientsBufferNative.asFloatBuffer();
        mCoefficientsBuffer.put(mCoefficients);
        mCoefficientsBuffer.rewind();
    }

    @Override
    public float[] filter(float[] samples) {
        if(mLastSampleLength != samples.length) {
            mLastSampleLength = samples.length;
            int bufferLength = samples.length + mBufferOverlap;

            mSamplesBufferNative = ByteBuffer.allocateDirect(samples.length * 4);
            mSamplesBufferNative.order(ByteOrder.nativeOrder());
            mSamplesBuffer = mSamplesBufferNative.asFloatBuffer();

            mFilteredBufferNative = ByteBuffer.allocateDirect(samples.length * 4);
            mFilteredBufferNative.order(ByteOrder.nativeOrder());
            mFilteredBuffer = mFilteredBufferNative.asFloatBuffer();

            mInternalBufferNative = ByteBuffer.allocateDirect(bufferLength * 4);
            mInternalBufferNative.order(ByteOrder.nativeOrder());
            mInternalBuffer = mInternalBufferNative.asFloatBuffer();
        }

        int bufferLength = samples.length + mBufferOverlap;

        if(mBuffer.length != bufferLength)
        {
            float[] temp = new float[bufferLength];
            System.arraycopy(mBuffer, mBuffer.length - mBufferOverlap, temp, 0, mBufferOverlap);
            mBuffer = temp;
        }
        else
        {
            System.arraycopy(mBuffer, samples.length, mBuffer, 0, mBufferOverlap);
        }

        System.arraycopy(samples, 0, mBuffer, mBufferOverlap, samples.length);

        mInternalBuffer.clear();
        mInternalBuffer.put(mBuffer);
        mInternalBuffer.rewind();

        mFilteredBufferNative.clear();
        mFilteredBuffer.clear();

        nativeFilter(mInternalBufferNative, mCoefficientsBufferNative, mFilteredBufferNative, samples.length, mCoefficients.length);

        float[] filtered = new float[samples.length];
        mFilteredBuffer.get(filtered);

        return filtered;
    }

    private native void nativeFilter(ByteBuffer buffer, ByteBuffer coefficients, ByteBuffer filtered, int sampleLength, int coefficientLength);
}
