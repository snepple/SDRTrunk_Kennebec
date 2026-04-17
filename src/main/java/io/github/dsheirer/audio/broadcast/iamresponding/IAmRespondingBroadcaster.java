/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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
package io.github.dsheirer.audio.broadcast.iamresponding;

import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.broadcast.AbstractAudioBroadcaster;
import io.github.dsheirer.audio.broadcast.AudioRecording;
import io.github.dsheirer.audio.broadcast.BroadcastState;
import io.github.dsheirer.audio.broadcast.IRealTimeAudioBroadcaster;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.sample.ConversionUtils;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IAmRespondingBroadcaster extends AbstractAudioBroadcaster<IAmRespondingConfiguration> implements IRealTimeAudioBroadcaster
{
    private static final Logger mLog = LoggerFactory.getLogger(IAmRespondingBroadcaster.class);

    private static final int TARGET_SAMPLE_RATE = 8000;
    private static final int SAMPLES_PER_CHUNK = 400; // 50ms at 8000 Hz

    private DatagramSocket mSocket;
    private AtomicBoolean mActive = new AtomicBoolean(false);
    private float[] mAccumulator = new float[SAMPLES_PER_CHUNK];
    private int mAccumulatorIndex = 0;

    public IAmRespondingBroadcaster(IAmRespondingConfiguration configuration, AliasModel aliasModel)
    {
        super(configuration);
        setBroadcastState(BroadcastState.DISCONNECTED);
    }

    @Override
    public int getAudioQueueSize()
    {
        return 0; // Not using the file-based queue
    }

    @Override
    public void receive(AudioRecording recording)
    {
        // Ignored, using real-time API
        recording.removePendingReplay();
    }

    @Override
    public void dispose()
    {
    }

    @Override
    public void start()
    {
        if (getBroadcastState() != BroadcastState.CONNECTED)
        {
            setBroadcastState(BroadcastState.CONNECTED);

            try
            {
                mSocket = new DatagramSocket();
            }
            catch(SocketException e)
            {
                mLog.error("Error creating UDP socket for IAmResponding", e);
                setBroadcastState(BroadcastState.ERROR);
            }
        }
    }

    @Override
    public void stop()
    {
        if (getBroadcastState() == BroadcastState.CONNECTED)
        {
            setBroadcastState(BroadcastState.DISCONNECTED);

            if(mSocket != null && !mSocket.isClosed())
            {
                mSocket.close();
                mSocket = null;
            }
        }
    }


    @Override
    public void startRealTimeStream(IdentifierCollection identifiers)
    {
        if(getBroadcastState() != BroadcastState.CONNECTED)
        {
            return;
        }

        // Try to acquire the First-Active lock
        if(mActive.compareAndSet(false, true))
        {
            mAccumulatorIndex = 0;
        }
    }

    @Override
    public void receiveRealTimeAudio(float[] audioBuffer)
    {
        if(mActive.get() && mSocket != null && !mSocket.isClosed() && getBroadcastConfiguration().isValid())
        {
            for(float sample : audioBuffer)
            {
                mAccumulator[mAccumulatorIndex++] = sample;

                if(mAccumulatorIndex == SAMPLES_PER_CHUNK)
                {
                    transmitChunk();
                    mAccumulatorIndex = 0;
                }
            }
        }
    }

    private void transmitChunk()
    {
        try
        {
            ByteBuffer pcmBuffer = ConversionUtils.convertToSigned16BitSamples(mAccumulator);
            byte[] pcmBytes = new byte[pcmBuffer.remaining()];
            pcmBuffer.get(pcmBytes);

            InetAddress address = InetAddress.getByName(getBroadcastConfiguration().getHost());
            DatagramPacket packet = new DatagramPacket(pcmBytes, pcmBytes.length, address, getBroadcastConfiguration().getPort());
            mSocket.send(packet);
        }
        catch(IOException e)
        {
            mLog.error("Error sending UDP packet for IAmResponding", e);
        }
    }

    @Override
    public void stopRealTimeStream()
    {
        // Release the lock
        mActive.set(false);
        mAccumulatorIndex = 0;
    }

    @Override
    public boolean isRealTimeReady()
    {
        return getBroadcastState() == BroadcastState.CONNECTED;
    }
}
