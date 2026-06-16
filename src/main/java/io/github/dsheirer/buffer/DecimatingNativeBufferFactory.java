/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */
package io.github.dsheirer.buffer;

import io.github.dsheirer.dsp.filter.decimate.DecimationFilterFactory;
import io.github.dsheirer.dsp.filter.decimate.IRealDecimationFilter;
import io.github.dsheirer.sample.complex.ComplexSamples;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Native buffer factory decorator that decimates the complex sample stream produced by a wrapped factory by a power
 * of two before delivering it downstream.
 *
 * This lets a tuner run its hardware at a higher native sample rate while presenting a lower effective sample rate to
 * the rest of the application.  Its primary use is offering a software "middle" sample rate (e.g. 5 MSPS from an
 * Airspy R2 that only supports 2.5 and 10 MSPS in firmware): the hardware streams at 10 MSPS over USB and this
 * decorator decimates by two so the polyphase channelizer only has to process half the bandwidth/channels.
 *
 * Note: decimation happens after the USB transfer, so it reduces channelizer/CPU load but not USB throughput.
 *
 * Complex decimation is performed as two independent real half-band decimations (one for I, one for Q), mirroring
 * {@code HalfBandTunerChannelSource}.  The half-band filters are stateful and retain history across buffers, so a
 * single instance is kept per stream; call {@link #reset()} whenever the stream restarts (e.g. on a rate change).
 */
public class DecimatingNativeBufferFactory implements INativeBufferFactory
{
    private final INativeBufferFactory mDelegate;
    private final int mDecimation;
    private IRealDecimationFilter mIDecimationFilter;
    private IRealDecimationFilter mQDecimationFilter;
    private float mSamplesPerMillisecond = 1.0f;

    /**
     * Constructs an instance.
     * @param delegate factory that produces the full-rate native buffers to be decimated.
     * @param decimation power-of-two decimation factor (e.g. 2 for half rate).
     */
    public DecimatingNativeBufferFactory(INativeBufferFactory delegate, int decimation)
    {
        mDelegate = delegate;
        mDecimation = decimation;
        reset();
    }

    /**
     * Recreates the half-band decimation filters, discarding any retained filter history.  Call when the stream
     * (re)starts so a previous session's tail samples do not bleed into the new stream.
     */
    public final void reset()
    {
        mIDecimationFilter = DecimationFilterFactory.getRealDecimationFilter(mDecimation);
        mQDecimationFilter = DecimationFilterFactory.getRealDecimationFilter(mDecimation);
    }

    @Override
    public void setSamplesPerMillisecond(float samplesPerMillisecond)
    {
        //This value reflects the post-decimation (advertised) rate used for our output buffers.
        mSamplesPerMillisecond = samplesPerMillisecond;
        //The wrapped factory operates on the pre-decimation (hardware) rate.
        mDelegate.setSamplesPerMillisecond(samplesPerMillisecond * mDecimation);
    }

    @Override
    public INativeBuffer getBuffer(ByteBuffer samples, long timestamp)
    {
        INativeBuffer raw = mDelegate.getBuffer(samples, timestamp);

        //Accumulate the wrapped buffer's complex samples into contiguous I/Q arrays so the stateful half-band
        //filters receive one continuous block per transfer (preserving inter-buffer continuity).
        float[] i = new float[Math.max(raw.sampleCount(), 0)];
        float[] q = new float[i.length];
        int offset = 0;

        Iterator<ComplexSamples> iterator = raw.iterator();

        while(iterator.hasNext())
        {
            ComplexSamples cs = iterator.next();
            int length = cs.i().length;

            if(offset + length > i.length)
            {
                i = Arrays.copyOf(i, offset + length);
                q = Arrays.copyOf(q, offset + length);
            }

            System.arraycopy(cs.i(), 0, i, offset, length);
            System.arraycopy(cs.q(), 0, q, offset, length);
            offset += length;
        }

        //Trim to an exact multiple of the decimation factor (defensive - the half-band decimator requires it).
        offset -= (offset % mDecimation);

        if(offset != i.length)
        {
            i = Arrays.copyOf(i, offset);
            q = Arrays.copyOf(q, offset);
        }

        float[] decimatedI = mIDecimationFilter.decimateReal(i);
        float[] decimatedQ = mQDecimationFilter.decimateReal(q);

        ComplexSamples decimated = new ComplexSamples(decimatedI, decimatedQ, timestamp);
        return new FloatNativeBuffer(decimated.toInterleaved().samples(), timestamp, mSamplesPerMillisecond);
    }
}
