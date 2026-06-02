package io.github.dsheirer.module.decode.nbfm.ai;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

public class WavUtil {
    public static byte[] floatsToWav(List<float[]> chunks, int sampleRate) throws IOException {
        int totalFloats = 0;
        for (float[] chunk : chunks) {
            totalFloats += chunk.length;
        }

        short[] pcm = new short[totalFloats];
        int idx = 0;
        for (float[] chunk : chunks) {
            for (float f : chunk) {
                // Clip and convert
                float clamped = Math.max(-1.0f, Math.min(1.0f, f));
                pcm[idx++] = (short)(clamped * 32767.0f);
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int byteRate = sampleRate * 2;
        int dataSize = pcm.length * 2;
        int fileSize = 36 + dataSize;

        // RIFF header
        baos.write("RIFF".getBytes());
        baos.write(intToBytes(fileSize));
        baos.write("WAVE".getBytes());

        // fmt chunk
        baos.write("fmt ".getBytes());
        baos.write(intToBytes(16)); // Subchunk1Size
        baos.write(shortToBytes((short)1)); // AudioFormat (PCM)
        baos.write(shortToBytes((short)1)); // NumChannels (Mono)
        baos.write(intToBytes(sampleRate)); // SampleRate
        baos.write(intToBytes(byteRate)); // ByteRate
        baos.write(shortToBytes((short)2)); // BlockAlign
        baos.write(shortToBytes((short)16)); // BitsPerSample

        // data chunk
        baos.write("data".getBytes());
        baos.write(intToBytes(dataSize));

        for (short s : pcm) {
            baos.write(shortToBytes(s));
        }

        return baos.toByteArray();
    }

    private static byte[] intToBytes(int value) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
    }

    private static byte[] shortToBytes(short value) {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array();
    }
}
