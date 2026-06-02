package io.github.dsheirer.gui;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Singleton that records key application events (channel start/stop, tuner connect/disconnect, errors)
 * with timestamps to a JSON file at [SDRTrunk home]/state_journal.json.
 *
 * Features:
 * - Thread-safe event recording
 * - Batched writes to disk every 30 seconds
 * - Limits entries to the last 1000
 * - Loads previous state on startup
 */
public class StateJournal
{
    private static final Logger mLog = LoggerFactory.getLogger(StateJournal.class);
    private static final int MAX_ENTRIES = 1000;
    private static final int FLUSH_INTERVAL_SECONDS = 30;
    private static final String JOURNAL_FILENAME = "state_journal.json";

    private static volatile StateJournal sInstance;

    private final Path mJournalPath;
    private final CopyOnWriteArrayList<Map<String, Object>> mEntries;
    private final ObjectMapper mObjectMapper;
    private final ScheduledExecutorService mScheduler;
    private ScheduledFuture<?> mFlushFuture;
    private volatile boolean mDirty = false;

    /**
     * Private constructor for singleton.
     * @param applicationRoot the SDRTrunk application root directory
     */
    private StateJournal(Path applicationRoot)
    {
        mJournalPath = applicationRoot.resolve(JOURNAL_FILENAME);
        mEntries = new CopyOnWriteArrayList<>();
        mObjectMapper = new ObjectMapper();
        mObjectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        mScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "StateJournal-Flush");
            t.setDaemon(true);
            return t;
        });

        // Load existing entries from disk
        loadFromDisk();

        // Schedule periodic flush
        mFlushFuture = mScheduler.scheduleAtFixedRate(this::flushToDisk,
            FLUSH_INTERVAL_SECONDS, FLUSH_INTERVAL_SECONDS, TimeUnit.SECONDS);

        mLog.info("StateJournal initialized with {} existing entries at [{}]", mEntries.size(), mJournalPath);
    }

    /**
     * Initializes the singleton instance. Should be called once during application startup.
     * @param applicationRoot the SDRTrunk application root directory
     * @return the singleton instance
     */
    public static StateJournal init(Path applicationRoot)
    {
        if (sInstance == null)
        {
            synchronized (StateJournal.class)
            {
                if (sInstance == null)
                {
                    sInstance = new StateJournal(applicationRoot);
                }
            }
        }
        return sInstance;
    }

    /**
     * Gets the singleton instance. Returns null if not yet initialized.
     */
    public static StateJournal getInstance()
    {
        return sInstance;
    }

    /**
     * Records a key event with details.
     * @param event the event name (e.g., "channel_start", "tuner_connect", "error")
     * @param details optional key-value details about the event
     */
    public void record(String event, Map<String, Object> details)
    {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("timestamp", Instant.now().toString());
        entry.put("event", event);
        if (details != null && !details.isEmpty())
        {
            entry.put("details", details);
        }

        mEntries.add(entry);

        // Trim to MAX_ENTRIES if necessary
        while (mEntries.size() > MAX_ENTRIES)
        {
            mEntries.remove(0);
        }

        mDirty = true;
    }

    /**
     * Convenience method to record an event without details.
     */
    public void record(String event)
    {
        record(event, null);
    }

    /**
     * Returns an unmodifiable view of the current journal entries.
     */
    public List<Map<String, Object>> getEntries()
    {
        return Collections.unmodifiableList(new ArrayList<>(mEntries));
    }

    /**
     * Loads journal entries from disk.
     */
    private void loadFromDisk()
    {
        if (Files.exists(mJournalPath))
        {
            try
            {
                List<Map<String, Object>> loaded = mObjectMapper.readValue(
                    mJournalPath.toFile(),
                    new TypeReference<List<Map<String, Object>>>() {}
                );

                if (loaded != null)
                {
                    // Only keep the last MAX_ENTRIES
                    int startIndex = Math.max(0, loaded.size() - MAX_ENTRIES);
                    mEntries.addAll(loaded.subList(startIndex, loaded.size()));
                }

                mLog.info("StateJournal loaded {} entries from disk", mEntries.size());
            }
            catch (IOException e)
            {
                mLog.error("Error loading state journal from [{}]", mJournalPath, e);
            }
        }
    }

    /**
     * Flushes current entries to disk. Called periodically by the scheduler.
     */
    private void flushToDisk()
    {
        if (!mDirty)
        {
            return;
        }

        try
        {
            List<Map<String, Object>> snapshot = new ArrayList<>(mEntries);
            mObjectMapper.writeValue(mJournalPath.toFile(), snapshot);
            mDirty = false;
        }
        catch (IOException e)
        {
            mLog.error("Error writing state journal to [{}]", mJournalPath, e);
        }
    }

    /**
     * Shuts down the journal, performing a final flush to disk.
     */
    public void stop()
    {
        mLog.info("StateJournal shutting down...");

        if (mFlushFuture != null)
        {
            mFlushFuture.cancel(false);
        }

        // Final flush
        flushToDisk();

        mScheduler.shutdown();
        try
        {
            if (!mScheduler.awaitTermination(5, TimeUnit.SECONDS))
            {
                mScheduler.shutdownNow();
            }
        }
        catch (InterruptedException e)
        {
            mScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        mLog.info("StateJournal stopped with {} entries", mEntries.size());
    }
}
