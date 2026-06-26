package io.github.dsheirer.module.decode.nbfm.ai;

import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.ByteBuffer;
import java.io.IOException;
import java.io.FileOutputStream;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.dsheirer.preference.UserPreferences;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

public class AudioBufferManager {
    private static final Logger mLog = LoggerFactory.getLogger(AudioBufferManager.class);
    private static final Pattern FILENAME_CLEANUP_PATTERN = Pattern.compile("[^a-zA-Z0-9.-]");
    private static final int MAX_EVENTS = 5;
    private static final int AUDIO_SAMPLE_RATE = 8000;
    private static final int BYTES_PER_FLOAT = Float.BYTES;
    static final int MAX_EVENT_AUDIO_SECONDS = 30;
    static final int MAX_EVENT_BYTES = AUDIO_SAMPLE_RATE * BYTES_PER_FLOAT * MAX_EVENT_AUDIO_SECONDS;
    private final LinkedList<List<float[]>> mAudioEvents = new LinkedList<>();
    private List<float[]> mCurrentEvent = null;
    private Path mBufferDir;
    private String mChannelName;
    private FileOutputStream mCurrentOutputStream;
    private Path mCurrentFilePath;

    public AudioBufferManager(UserPreferences preferences, String channelName) {
        mChannelName = channelName != null ? FILENAME_CLEANUP_PATTERN.matcher(channelName).replaceAll("_") : "unknown";
        mBufferDir = preferences.getDirectoryPreference().getDirectoryConfiguration().resolve("ai_buffers").resolve(mChannelName);
        try {
            Files.createDirectories(mBufferDir);
        } catch (IOException e) {
            mLog.error("Error creating AI buffer directory: " + mBufferDir, e);
        }
    }

    public void startEvent() {
        synchronized (mAudioEvents) {
            mCurrentEvent = new ArrayList<>();
            mCurrentFilePath = mBufferDir.resolve(System.currentTimeMillis() + ".raw");
            try {
                mCurrentOutputStream = new FileOutputStream(mCurrentFilePath.toFile());
            } catch (IOException e) {
                mLog.error("Error opening output stream for AI buffer: " + mCurrentFilePath, e);
                mCurrentOutputStream = null;
            }
        }
    }

    public void addAudioSamples(float[] audioSamples) {
        synchronized (mAudioEvents) {
            if (mCurrentOutputStream != null) {
                try {
                    ByteBuffer buffer = ByteBuffer.allocate(audioSamples.length * 4).order(ByteOrder.LITTLE_ENDIAN);
                    for(float f : audioSamples) buffer.putFloat(f);
                    mCurrentOutputStream.write(buffer.array());
                } catch (IOException e) {
                    mLog.error("Error writing audio samples to AI buffer", e);
                }
            }
        }
    }

    public void endEvent() {
        synchronized (mAudioEvents) {
            if (mCurrentOutputStream != null) {
                try {
                    mCurrentOutputStream.close();
                } catch (IOException e) {
                    mLog.error("Error closing AI buffer output stream", e);
                }
                mCurrentOutputStream = null;
            }
            cleanUpOldEvents();
        }
    }

    private void cleanUpOldEvents() {
        try (Stream<Path> stream = Files.list(mBufferDir)) {
            List<Path> files = stream.filter(path -> path.toString().endsWith(".raw"))
                                     .sorted()
                                     .collect(Collectors.toList());
            int removable = files.size() - MAX_EVENTS;
            for (int i = 0; i < removable; i++) {
                Path file = files.get(i);
                try {
                    Files.deleteIfExists(file);
                } catch (IOException e) {
                    //The file may be transiently locked by another process (antivirus, cloud sync) on
                    //Windows. Skip it this round rather than aborting the entire cleanup so newer files
                    //can still be pruned; it will be retried on the next cleanup cycle. Logged at debug
                    //to avoid flooding the log with a stack trace on every call end.
                    mLog.debug("Could not delete old AI buffer file (will retry next cleanup): {} - {}",
                            file, e.getMessage());
                }
            }
        } catch (IOException e) {
            mLog.error("Error cleaning up old AI buffer events", e);
        }
    }

    public List<List<float[]>> getBufferedEvents() {
        List<List<float[]>> events = new ArrayList<>();
        try (Stream<Path> stream = Files.list(mBufferDir)) {
            List<Path> files = stream.filter(path -> path.toString().endsWith(".raw"))
                                     .sorted()
                                     .collect(Collectors.toList());
            if(files.size() > MAX_EVENTS) {
                files = files.subList(files.size() - MAX_EVENTS, files.size());
            }
            for (Path file : files) {
                byte[] bytes = readBoundedRawAudio(file);
                if(bytes.length == 0) {
                    continue;
                }
                ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
                List<float[]> event = new ArrayList<>();
                // Read in chunks of 512 floats (same as Resampler)
                int chunkSize = 512;
                float[] floats = new float[bytes.length / 4];
                buffer.asFloatBuffer().get(floats);
                for(int i = 0; i < floats.length; i += chunkSize) {
                    int length = Math.min(chunkSize, floats.length - i);
                    float[] chunk = new float[length];
                    System.arraycopy(floats, i, chunk, 0, length);
                    event.add(chunk);
                }
                events.add(event);
            }
        } catch (IOException e) {
            mLog.error("Error reading AI buffered events", e);
        }
        return events;
    }

    private byte[] readBoundedRawAudio(Path file) throws IOException {
        long byteCount = Files.size(file);
        long alignedByteCount = byteCount - (byteCount % BYTES_PER_FLOAT);
        if(alignedByteCount <= 0) {
            return new byte[0];
        }

        if(alignedByteCount <= MAX_EVENT_BYTES) {
            byte[] bytes = Files.readAllBytes(file);
            if(bytes.length == alignedByteCount) {
                return bytes;
            }

            byte[] alignedBytes = new byte[(int)alignedByteCount];
            System.arraycopy(bytes, 0, alignedBytes, 0, alignedBytes.length);
            return alignedBytes;
        }

        int bytesToRead = alignToFloatBoundary(MAX_EVENT_BYTES);
        int headBytes = alignToFloatBoundary(bytesToRead / 2);
        int tailBytes = bytesToRead - headBytes;
        byte[] bytes = new byte[bytesToRead];

        try(FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
            readFully(channel, ByteBuffer.wrap(bytes, 0, headBytes), 0);
            readFully(channel, ByteBuffer.wrap(bytes, headBytes, tailBytes), alignedByteCount - tailBytes);
        }

        mLog.debug("AI audio buffer event {} is {} bytes; using first and last {} seconds for analysis",
                file.getFileName(), byteCount, MAX_EVENT_AUDIO_SECONDS);
        return bytes;
    }

    private static int alignToFloatBoundary(int bytes) {
        return bytes - (bytes % BYTES_PER_FLOAT);
    }

    private static void readFully(FileChannel channel, ByteBuffer buffer, long position) throws IOException {
        while(buffer.hasRemaining()) {
            int read = channel.read(buffer, position);
            if(read < 0) {
                break;
            }
            position += read;
        }
    }

    public static int getBufferedEventCount(UserPreferences preferences, String channelName) {
        if (preferences == null || channelName == null) return 0;
        String safeName = FILENAME_CLEANUP_PATTERN.matcher(channelName).replaceAll("_");
        Path dir = preferences.getDirectoryPreference().getDirectoryConfiguration().resolve("ai_buffers").resolve(safeName);
        if (!Files.exists(dir)) return 0;
        try (Stream<Path> stream = Files.list(dir)) {
            return (int) stream.filter(path -> path.toString().endsWith(".raw")).count();
        } catch (IOException e) {
            return 0;
        }
    }
}
