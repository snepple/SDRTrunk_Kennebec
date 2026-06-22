package io.github.dsheirer.module.decode.nbfm.ai;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link WavUtil}. In particular this guards the WAV header sample-rate field: NBFM audio is captured at
 * 8 kHz, and the header must declare the true rate so a downstream listener (the Gemini optimizer) interprets
 * frequencies correctly rather than ~6x too high.
 */
public class WavUtilTest
{
    private static int readLittleEndianInt(byte[] data, int offset)
    {
        return ByteBuffer.wrap(data, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    @Test
    public void writesTheGivenSampleRateIntoTheHeader() throws Exception
    {
        byte[] wav = WavUtil.floatsToWav(List.of(new float[]{0f, 0.25f, -0.25f, 0.5f}), 8000);

        //RIFF/WAVE markers
        assertEquals("RIFF", new String(wav, 0, 4));
        assertEquals("WAVE", new String(wav, 8, 4));
        //Sample rate is a little-endian int at byte offset 24, byte rate (rate * 2 bytes/sample mono 16-bit) at 28.
        assertEquals(8000, readLittleEndianInt(wav, 24), "header must declare the true 8 kHz capture rate");
        assertEquals(8000 * 2, readLittleEndianInt(wav, 28));
        //Mono, 16-bit PCM
        assertEquals(1, ByteBuffer.wrap(wav, 22, 2).order(ByteOrder.LITTLE_ENDIAN).getShort());
        assertEquals(16, ByteBuffer.wrap(wav, 34, 2).order(ByteOrder.LITTLE_ENDIAN).getShort());
    }

    @Test
    public void honorsAnArbitrarySampleRate() throws Exception
    {
        byte[] wav = WavUtil.floatsToWav(List.of(new float[]{0f}), 16000);
        assertEquals(16000, readLittleEndianInt(wav, 24));
    }

    @Test
    public void convertsAndClampsFloatsToPcm16() throws Exception
    {
        byte[] wav = WavUtil.floatsToWav(List.of(new float[]{0f, 1.0f, -1.0f, 2.0f}), 8000);
        int dataStart = 44; //standard 44-byte header

        short s0 = ByteBuffer.wrap(wav, dataStart, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
        short s1 = ByteBuffer.wrap(wav, dataStart + 2, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
        short s2 = ByteBuffer.wrap(wav, dataStart + 4, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
        short s3 = ByteBuffer.wrap(wav, dataStart + 6, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();

        assertEquals(0, s0);
        assertEquals(32767, s1);          //+1.0 -> full scale
        assertEquals(-32767, s2);         //-1.0 -> negative full scale
        assertEquals(32767, s3);          //+2.0 clamps to full scale
    }
}
