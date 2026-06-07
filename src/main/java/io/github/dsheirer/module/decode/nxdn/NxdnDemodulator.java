package io.github.dsheirer.module.decode.nxdn;

import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.module.decode.FeedbackDecoder;

/**
 * NXDN Demodulator for C4FM.
 * Performs symbol slicing, PLL timing recovery, and Frame Sync Word (FSW) detection.
 */
public class NxdnDemodulator {
    private static final float SOFT_SYMBOL_QUADRANT_BOUNDARY = (float)(Math.PI / 2.0);
    private static final float TWO_PI = (float)(Math.PI * 2.0);

    private final NxdnMessageFramer mMessageFramer;
    private final FeedbackDecoder mFeedbackDecoder;

    private double mSamplesPerSymbol;
    private double mSamplePoint;
    private float[] mBuffer;
    private int mBufferPointer;
    private int mBufferReloadThreshold;

    private float mPll = 0.0f;
    private float mGain = 1.2f;

    public NxdnDemodulator(NxdnMessageFramer messageFramer, FeedbackDecoder feedbackDecoder) {
        mMessageFramer = messageFramer;
        mFeedbackDecoder = feedbackDecoder;
    }

    public void setSamplesPerSymbol(float samplesPerSymbol) {
        mSamplesPerSymbol = samplesPerSymbol;
        mSamplePoint = samplesPerSymbol;
        mBuffer = new float[2048];
        mBufferReloadThreshold = mBuffer.length - 100;
        mBufferPointer = mBufferReloadThreshold;
    }

    public void resetPLL() {
        mPll = 0.0f;
    }

    public void process(float[] samples) {
        int samplesPointer = 0;
        
        while(samplesPointer < samples.length) {
            if(mBufferPointer >= mBufferReloadThreshold) {
                int copyLength = Math.min(1024, samples.length - samplesPointer);
                System.arraycopy(mBuffer, copyLength, mBuffer, 0, mBuffer.length - copyLength);
                System.arraycopy(samples, samplesPointer, mBuffer, mBuffer.length - copyLength, copyLength);
                samplesPointer += copyLength;
                mBufferPointer -= copyLength;

                for(int x = mBuffer.length - copyLength; x < mBuffer.length; x++) {
                    if(mBuffer[x - 1] > 1.5f && mBuffer[x] < -1.5f) {
                        mBuffer[x] += TWO_PI;
                    } else if(mBuffer[x - 1] < -1.5f && mBuffer[x] > 1.5f) {
                        mBuffer[x] -= TWO_PI;
                    }
                }
            }

            while(mBufferPointer < mBufferReloadThreshold) {
                mBufferPointer++;
                mSamplePoint--;

                if(mSamplePoint < 1) {
                    float sample = mBuffer[mBufferPointer];
                    
                    // Equalize
                    float softSymbol = sample * mGain + mPll;

                    // Broadcast to GUI
                    mFeedbackDecoder.broadcast(softSymbol);

                    // Slice symbol
                    Dibit symbol = toSymbol(softSymbol);
                    
                    // Basic PLL timing adjustment (Mueller and Muller approach)
                    float error = softSymbol - symbol.getIdealPhase();
                    mSamplePoint += error * 0.05; // Loop filter gain

                    mMessageFramer.process(symbol);

                    mSamplePoint += mSamplesPerSymbol;
                }
            }
        }
    }

    public static Dibit toSymbol(float sample) {
        if(sample > 0) {
            return sample > SOFT_SYMBOL_QUADRANT_BOUNDARY ? Dibit.D01_PLUS_3 : Dibit.D00_PLUS_1;
        } else {
            return sample < -SOFT_SYMBOL_QUADRANT_BOUNDARY ? Dibit.D11_MINUS_3 : Dibit.D10_MINUS_1;
        }
    }
}
