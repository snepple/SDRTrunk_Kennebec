package io.github.dsheirer.module.decode.nbfm.ai;

import io.github.dsheirer.preference.UserPreferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AudioBufferManagerTest
{
    @TempDir
    Path mTempDir;
    private UserPreferences mPreferences;

    @AfterEach
    public void tearDown()
    {
        if(mPreferences != null)
        {
            mPreferences.getDirectoryPreference().resetDirectoryConfiguration();
        }
    }

    @Test
    public void loadsOnlyNewestFiveEvents() throws Exception
    {
        mPreferences = new UserPreferences();
        mPreferences.getDirectoryPreference().setDirectoryConfiguration(mTempDir);
        AudioBufferManager manager = new AudioBufferManager(mPreferences, "Test Channel");
        Path bufferDir = mTempDir.resolve("ai_buffers").resolve("Test_Channel");

        for(int i = 0; i < 7; i++)
        {
            writeRawFloats(bufferDir.resolve(String.format("%013d.raw", i)), i);
        }

        List<List<float[]>> events = manager.getBufferedEvents();

        assertEquals(5, events.size());
        assertEquals(2.0f, sampleAt(events.get(0), 0), 0.0f);
        assertEquals(6.0f, sampleAt(events.get(4), 0), 0.0f);
    }

    @Test
    public void capsLargeEventsToHeadAndTailSamples() throws Exception
    {
        mPreferences = new UserPreferences();
        mPreferences.getDirectoryPreference().setDirectoryConfiguration(mTempDir);
        AudioBufferManager manager = new AudioBufferManager(mPreferences, "Large Channel");
        Path bufferDir = mTempDir.resolve("ai_buffers").resolve("Large_Channel");

        int maxSamples = AudioBufferManager.MAX_EVENT_BYTES / Float.BYTES;
        int totalSamples = maxSamples + 64;
        writeSequentialRawFloats(bufferDir.resolve("0000000000001.raw"), totalSamples);

        List<List<float[]>> events = manager.getBufferedEvents();

        assertEquals(1, events.size());
        assertEquals(maxSamples, sampleCount(events.get(0)));
        assertEquals(0.0f, sampleAt(events.get(0), 0), 0.0f);
        assertEquals(totalSamples - (maxSamples / 2), sampleAt(events.get(0), maxSamples / 2), 0.0f);
        assertEquals(totalSamples - 1.0f, sampleAt(events.get(0), maxSamples - 1), 0.0f);
    }

    private static void writeRawFloats(Path path, float... samples) throws Exception
    {
        ByteBuffer buffer = ByteBuffer.allocate(samples.length * Float.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        for(float sample: samples)
        {
            buffer.putFloat(sample);
        }
        Files.write(path, buffer.array());
    }

    private static void writeSequentialRawFloats(Path path, int sampleCount) throws Exception
    {
        ByteBuffer buffer = ByteBuffer.allocate(sampleCount * Float.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        for(int i = 0; i < sampleCount; i++)
        {
            buffer.putFloat(i);
        }
        Files.write(path, buffer.array());
    }

    private static int sampleCount(List<float[]> event)
    {
        return event.stream().mapToInt(chunk -> chunk.length).sum();
    }

    private static float sampleAt(List<float[]> event, int index)
    {
        int offset = index;
        for(float[] chunk: event)
        {
            if(offset < chunk.length)
            {
                return chunk[offset];
            }
            offset -= chunk.length;
        }

        throw new IndexOutOfBoundsException(index);
    }
}
