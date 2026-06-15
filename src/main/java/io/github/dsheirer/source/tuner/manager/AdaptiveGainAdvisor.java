package io.github.dsheirer.source.tuner.manager;

import io.github.dsheirer.preference.UserPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Adaptive Gain Advisor — monitors I/Q signal power levels reported by active NBFM channel
 * decoders and recommends gain adjustments to keep signals in the optimal ADC dynamic range.
 *
 * A single tuner typically serves multiple channels simultaneously; this advisor aggregates
 * statistics across all channels so that gain recommendations reflect the aggregate workload
 * rather than any single channel. It adapts continuously to environmental changes such as
 * antenna repositioning, propagation shifts, and weather-driven path loss.
 *
 * Without AI: pure heuristic based on average I/Q power vs threshold bands.
 * With AI: hourly Gemini consultation for deeper pattern recognition that distinguishes
 * propagation fades from incorrect hardware gain settings.
 *
 * Recommended I/Q power range: -25 to -6 dBFS (leaves headroom below clipping while
 * keeping signals well above the noise floor).
 */
public class AdaptiveGainAdvisor
{
    private static final Logger mLog = LoggerFactory.getLogger(AdaptiveGainAdvisor.class);

    private static volatile AdaptiveGainAdvisor INSTANCE;

    // Optimal ADC utilization band (dBFS)
    private static final double GAIN_TOO_HIGH_DBFS = -3.0;
    private static final double GAIN_TOO_LOW_DBFS  = -35.0;
    private static final double GAIN_OPTIMAL_MIN   = -25.0;
    private static final double GAIN_OPTIMAL_MAX   = -6.0;

    // Minimum sample count before issuing any recommendation (avoids false alarms on startup)
    private static final int MIN_SAMPLES_FOR_ADVICE = 50;

    // AI consultation rate limiting
    private static final long AI_CONSULTATION_INTERVAL_MS = 3_600_000L; // 1 hour

    private final ConcurrentHashMap<String, ChannelStats> mChannelStats = new ConcurrentHashMap<>();
    private final ScheduledExecutorService mScheduler;
    private volatile UserPreferences mUserPreferences;
    private final AtomicLong mLastAiConsultationMs = new AtomicLong(0);
    private final HttpClient mHttpClient;

    /**
     * Returns the singleton instance, creating it on first call.
     *
     * @param userPreferences for AI feature toggle and Gemini API key access
     */
    public static AdaptiveGainAdvisor getInstance(UserPreferences userPreferences)
    {
        if(INSTANCE == null)
        {
            synchronized(AdaptiveGainAdvisor.class)
            {
                if(INSTANCE == null)
                {
                    INSTANCE = new AdaptiveGainAdvisor(userPreferences);
                }
            }
        }
        return INSTANCE;
    }

    private AdaptiveGainAdvisor(UserPreferences userPreferences)
    {
        mUserPreferences = userPreferences;
        mHttpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .version(HttpClient.Version.HTTP_2)
                .build();

        mScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AdaptiveGainAdvisor");
            t.setDaemon(true);
            return t;
        });

        // Evaluate every 5 minutes; first check after 2 minutes to allow statistics to accumulate
        mScheduler.scheduleAtFixedRate(this::evaluate, 2, 5, TimeUnit.MINUTES);
        mLog.info("AdaptiveGainAdvisor started — monitoring I/Q power levels across active channels");
    }

    /**
     * Reports the current I/Q signal power level for a named channel.
     * Called from NBFMDecoder on a sampled basis (not every buffer).
     *
     * @param channelName unique channel identifier
     * @param powerDbfs   complex I/Q signal power in dBFS (10·log₁₀(mean(I²+Q²)))
     */
    public void reportSignalLevel(String channelName, double powerDbfs)
    {
        mChannelStats.computeIfAbsent(channelName, ChannelStats::new).update(powerDbfs);
    }

    /**
     * Removes a channel from monitoring when its decoder is stopped.
     *
     * @param channelName channel to remove
     */
    public void removeChannel(String channelName)
    {
        mChannelStats.remove(channelName);
    }

    /**
     * Returns a read-only snapshot of current per-channel average power values (dBFS).
     */
    public Map<String, Double> getChannelPowerSnapshot()
    {
        ConcurrentHashMap<String, Double> snapshot = new ConcurrentHashMap<>();
        for(Map.Entry<String, ChannelStats> e : mChannelStats.entrySet())
        {
            snapshot.put(e.getKey(), e.getValue().getAveragePowerDbfs());
        }
        return snapshot;
    }

    /**
     * Shuts down the advisor background thread.
     */
    public void stop()
    {
        mScheduler.shutdownNow();
        mLog.info("AdaptiveGainAdvisor stopped");
    }

    // -------------------------------------------------------------------------
    // Internal evaluation
    // -------------------------------------------------------------------------

    private void evaluate()
    {
        try
        {
            if(mChannelStats.isEmpty())
            {
                return;
            }

            int tooHigh = 0;
            int tooLow  = 0;
            int optimal = 0;

            for(ChannelStats stats : mChannelStats.values())
            {
                if(stats.getSampleCount() < MIN_SAMPLES_FOR_ADVICE)
                {
                    continue;
                }

                double avg = stats.getAveragePowerDbfs();

                if(avg > GAIN_TOO_HIGH_DBFS)
                {
                    tooHigh++;
                    mLog.warn("AdaptiveGain [{}]: signal too strong ({} dBFS avg) — consider reducing tuner gain to avoid ADC clipping",
                            stats.getChannelName(), String.format("%.1f", avg));
                }
                else if(avg < GAIN_TOO_LOW_DBFS)
                {
                    tooLow++;
                    mLog.warn("AdaptiveGain [{}]: signal too weak ({} dBFS avg) — consider increasing tuner gain for better SNR",
                            stats.getChannelName(), String.format("%.1f", avg));
                }
                else
                {
                    optimal++;
                    mLog.debug("AdaptiveGain [{}]: signal level optimal ({} dBFS avg)",
                            stats.getChannelName(), String.format("%.1f", avg));
                }
            }

            // Summary when there are issues across multiple channels
            int total = tooHigh + tooLow + optimal;
            if(total > 0 && (tooHigh > 0 || tooLow > 0))
            {
                mLog.info("AdaptiveGain summary: {} channels optimal, {} too strong, {} too weak (of {} monitored)",
                        optimal, tooHigh, tooLow, total);
            }

            // Hourly AI consultation when AI is available
            if(shouldConsultAI())
            {
                consultGemini();
            }
        }
        catch(Exception e)
        {
            mLog.error("AdaptiveGainAdvisor evaluation error", e);
        }
    }

    private boolean shouldConsultAI()
    {
        if(mUserPreferences == null)
        {
            return false;
        }

        var aiPref = mUserPreferences.getAIPreference();
        if(!aiPref.isAIEnabled() || !aiPref.isGainAdvisorEnabled())
        {
            return false;
        }

        String apiKey = aiPref.getGeminiApiKey();
        if(apiKey == null || apiKey.isEmpty())
        {
            return false;
        }

        long now = System.currentTimeMillis();
        return (now - mLastAiConsultationMs.get()) >= AI_CONSULTATION_INTERVAL_MS;
    }

    private void consultGemini()
    {
        mLastAiConsultationMs.set(System.currentTimeMillis());

        StringJoiner csv = new StringJoiner("\n");
        csv.add("channel,avg_dbfs,min_dbfs,max_dbfs,sample_count");
        for(ChannelStats stats : mChannelStats.values())
        {
            if(stats.getSampleCount() < MIN_SAMPLES_FOR_ADVICE)
            {
                continue;
            }
            csv.add(String.format("%s,%.1f,%.1f,%.1f,%d",
                    stats.getChannelName(),
                    stats.getAveragePowerDbfs(),
                    stats.getMinPowerDbfs(),
                    stats.getMaxPowerDbfs(),
                    stats.getSampleCount()));
        }

        String prompt = "You are an RF engineer advising on SDR receiver gain settings.\n" +
                "The following table shows I/Q signal power statistics (dBFS) for active receive channels " +
                "monitored over the last hour. Each channel may share a physical SDR tuner with others.\n\n" +
                csv + "\n\n" +
                "Optimal ADC utilization is -25 to -6 dBFS. Levels above -3 dBFS risk clipping. " +
                "Levels below -35 dBFS indicate poor SNR.\n" +
                "Respond with a brief (2-3 sentence) plain-English gain adjustment recommendation, " +
                "noting whether gain should increase, decrease, or stay the same, and why. " +
                "Consider that all channels on the same tuner share the same hardware gain.";

        String apiKey = mUserPreferences.getAIPreference().getGeminiApiKey();
        String model = mUserPreferences.getAIPreference().getGeminiModel();
        String url = "https://generativelanguage.googleapis.com/v1beta/" + model +
                ":generateContent?key=" + apiKey;

        String body = "{\"contents\":[{\"parts\":[{\"text\":\"" +
                prompt.replace("\"", "\\\"").replace("\n", "\\n") + "\"}]}]}";

        try
        {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            mHttpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if(response.statusCode() == 200)
                        {
                            // Extract first text part from Gemini response
                            String responseBody = response.body();
                            int textStart = responseBody.indexOf("\"text\":\"");
                            if(textStart >= 0)
                            {
                                int contentStart = textStart + 8;
                                int contentEnd = responseBody.indexOf("\"", contentStart);
                                if(contentEnd > contentStart)
                                {
                                    String recommendation = responseBody.substring(contentStart, contentEnd)
                                            .replace("\\n", " ").replace("\\\"", "\"");
                                    mLog.info("AdaptiveGain AI recommendation: {}", recommendation);
                                }
                            }
                        }
                        else
                        {
                            mLog.debug("AdaptiveGain AI consultation returned HTTP {}", response.statusCode());
                        }
                    })
                    .exceptionally(ex -> {
                        mLog.debug("AdaptiveGain AI consultation failed: {}", ex.getMessage());
                        return null;
                    });
        }
        catch(Exception e)
        {
            mLog.debug("AdaptiveGain AI consultation error: {}", e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Per-channel statistics accumulator
    // -------------------------------------------------------------------------

    private static class ChannelStats
    {
        private final String mChannelName;
        private double mSum = 0;
        private double mMin = Double.MAX_VALUE;
        private double mMax = Double.MIN_VALUE;
        private int mCount = 0;

        ChannelStats(String channelName)
        {
            mChannelName = channelName;
        }

        synchronized void update(double powerDbfs)
        {
            mSum += powerDbfs;
            mCount++;
            if(powerDbfs < mMin) mMin = powerDbfs;
            if(powerDbfs > mMax) mMax = powerDbfs;
        }

        String getChannelName()
        {
            return mChannelName;
        }

        synchronized double getAveragePowerDbfs()
        {
            return mCount > 0 ? mSum / mCount : -100.0;
        }

        synchronized double getMinPowerDbfs()
        {
            return mMin == Double.MAX_VALUE ? -100.0 : mMin;
        }

        synchronized double getMaxPowerDbfs()
        {
            return mMax == Double.MIN_VALUE ? -100.0 : mMax;
        }

        synchronized int getSampleCount()
        {
            return mCount;
        }
    }
}
