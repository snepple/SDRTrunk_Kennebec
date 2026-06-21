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
 * With AI: twice-daily Gemini consultation for deeper pattern recognition that distinguishes
 * propagation fades from incorrect hardware gain settings.  The user can also trigger an
 * on-demand consultation from the tuner editor at any time.
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

    // AI consultation rate limiting — twice a day to keep Gemini token usage modest
    private static final long AI_CONSULTATION_INTERVAL_MS = 43_200_000L; // 12 hours

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
     * @param frequencyHz channel center frequency, used to attribute the channel to a tuner (0 if unknown)
     * @param powerDbfs   complex I/Q signal power in dBFS (10·log₁₀(mean(I²+Q²)))
     */
    public void reportSignalLevel(String channelName, long frequencyHz, double powerDbfs)
    {
        ChannelStats stats = mChannelStats.computeIfAbsent(channelName, ChannelStats::new);
        if(frequencyHz > 0)
        {
            stats.setFrequencyHz(frequencyHz);
        }
        stats.update(powerDbfs);
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

            // Periodic (twice-daily) AI consultation when AI is available
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
        //#9 Only run the scheduled AI consultation when the advisor's auto-schedule is enabled (this also
        //requires the advisor feature itself to be enabled).  Manual runs bypass this via
        //requestManualConsultation().  The cadence is user-selectable rather than fixed at twice daily.
        if(!aiPref.isAIEnabled() || !aiPref.isGainAdvisorScheduleEnabled())
        {
            return false;
        }

        String apiKey = aiPref.getGeminiApiKey();
        if(apiKey == null || apiKey.isEmpty())
        {
            return false;
        }

        long intervalMs = aiPref.getGainAdvisorIntervalHours() * 60L * 60L * 1000L;
        long now = System.currentTimeMillis();
        return (now - mLastAiConsultationMs.get()) >= intervalMs;
    }

    private void consultGemini()
    {
        mLastAiConsultationMs.set(System.currentTimeMillis());

        java.util.function.Predicate<ChannelStats> all = s -> true;
        if(countChannels(MIN_SAMPLES_FOR_ADVICE, all) == 0)
        {
            return;
        }

        sendGeminiConsultation(buildStatsCsv(MIN_SAMPLES_FOR_ADVICE, all),
                recommendation -> mLog.info("AdaptiveGain AI recommendation: {}", recommendation),
                () -> { /* scheduled run: failures are non-fatal and already logged */ });
    }

    /**
     * Runs the gain advisor on demand for the channels falling within a tuner's passband (e.g. from
     * the tuner editor) and delivers a recommendation.  When AI is enabled and configured, consults
     * Gemini; otherwise returns the local heuristic summary so the button is useful without an API key.
     *
     * @param minFrequencyHz lower bound of the tuner's tuned passband (inclusive)
     * @param maxFrequencyHz upper bound of the tuner's tuned passband (inclusive)
     * @param onResult       receives the recommendation text (called off the FX thread)
     * @param onError        receives a user-facing message when no recommendation could be produced
     */
    public void requestManualConsultation(long minFrequencyHz, long maxFrequencyHz,
                                          java.util.function.Consumer<String> onResult,
                                          java.util.function.Consumer<String> onError)
    {
        //A channel belongs to this tuner when its (known) center frequency falls within the passband.
        java.util.function.Predicate<ChannelStats> onThisTuner = s ->
        {
            long f = s.getFrequencyHz();
            return f > 0 && f >= minFrequencyHz && f <= maxFrequencyHz;
        };

        try
        {
            //Relax the sample gate for manual runs so the button works shortly after channels start.
            if(countChannels(1, onThisTuner) == 0)
            {
                onError.accept("No active channels on this tuner are reporting I/Q signal levels yet. " +
                        "Start one or more NBFM channels on this tuner and let them receive traffic for a " +
                        "minute, then try again. (The advisor monitors NBFM channels.)");
                return;
            }

            String heuristic = buildHeuristicSummary(1, onThisTuner);

            var aiPref = mUserPreferences != null ? mUserPreferences.getAIPreference() : null;
            boolean aiAvailable = aiPref != null && aiPref.isAIEnabled() && aiPref.isGainAdvisorEnabled()
                    && aiPref.getGeminiApiKey() != null && !aiPref.getGeminiApiKey().isEmpty();

            if(!aiAvailable)
            {
                onResult.accept(heuristic +
                        "\n\nEnable AI and set a Gemini API key in Preferences for a deeper, " +
                        "propagation-aware recommendation.");
                return;
            }

            mLastAiConsultationMs.set(System.currentTimeMillis());
            sendGeminiConsultation(buildStatsCsv(1, onThisTuner),
                    onResult,
                    () -> onResult.accept(heuristic +
                            "\n\n(AI consultation was unavailable, so the local heuristic analysis is shown instead.)"));
        }
        catch(Exception e)
        {
            onError.accept("Gain advisor error: " + e.getMessage());
        }
    }

    /**
     * Number of channels with at least {@code minSamples} accumulated samples that match the filter.
     */
    private int countChannels(int minSamples, java.util.function.Predicate<ChannelStats> filter)
    {
        int count = 0;
        for(ChannelStats stats : mChannelStats.values())
        {
            if(stats.getSampleCount() >= minSamples && filter.test(stats))
            {
                count++;
            }
        }
        return count;
    }

    /**
     * Builds the per-channel CSV of power statistics for channels meeting the sample threshold and filter.
     */
    private String buildStatsCsv(int minSamples, java.util.function.Predicate<ChannelStats> filter)
    {
        StringJoiner csv = new StringJoiner("\n");
        csv.add("channel,freq_mhz,avg_dbfs,noise_floor_dbfs,peak_dbfs,snr_estimate_db,sample_count");
        for(ChannelStats stats : mChannelStats.values())
        {
            if(stats.getSampleCount() < minSamples || !filter.test(stats))
            {
                continue;
            }
            //Estimate SNR as the spread between peak (signal) and the minimum observed (≈ noise floor).
            double snrEstimate = stats.getMaxPowerDbfs() - stats.getMinPowerDbfs();
            csv.add(String.format("%s,%.4f,%.1f,%.1f,%.1f,%.1f,%d",
                    stats.getChannelName(),
                    stats.getFrequencyHz() / 1_000_000.0,
                    stats.getAveragePowerDbfs(),
                    stats.getMinPowerDbfs(),
                    stats.getMaxPowerDbfs(),
                    snrEstimate,
                    stats.getSampleCount()));
        }
        return csv.toString();
    }

    /**
     * Builds a plain-English, no-AI recommendation summary from the current statistics matching the filter.
     */
    private String buildHeuristicSummary(int minSamples, java.util.function.Predicate<ChannelStats> filter)
    {
        StringBuilder sb = new StringBuilder();
        int tooHigh = 0, tooLow = 0, optimal = 0;
        for(ChannelStats stats : mChannelStats.values())
        {
            if(stats.getSampleCount() < minSamples || !filter.test(stats))
            {
                continue;
            }
            double avg = stats.getAveragePowerDbfs();
            double snrEstimate = stats.getMaxPowerDbfs() - stats.getMinPowerDbfs();
            String verdict;
            if(avg > GAIN_TOO_HIGH_DBFS) { verdict = "too strong — reduce gain"; tooHigh++; }
            else if(avg < GAIN_TOO_LOW_DBFS) { verdict = "too weak — increase gain"; tooLow++; }
            else { verdict = "optimal"; optimal++; }
            sb.append(String.format("• %s: %.1f dBFS avg, peak %.1f, floor %.1f, ~%.0f dB SNR — %s%n",
                    stats.getChannelName(), avg, stats.getMaxPowerDbfs(), stats.getMinPowerDbfs(),
                    snrEstimate, verdict));
        }

        String headline;
        if(tooHigh > 0 && tooLow == 0)
        {
            headline = "Overall: signals are running hot. Lower the tuner gain to keep peaks below -3 dBFS.";
        }
        else if(tooLow > 0 && tooHigh == 0)
        {
            headline = "Overall: signals are weak. Raise the tuner gain to improve SNR (aim for -25 to -6 dBFS).";
        }
        else if(tooHigh > 0 && tooLow > 0)
        {
            headline = "Overall: mixed levels across channels on this tuner. Since all channels share one hardware " +
                    "gain, pick a setting that keeps the strongest channel below -3 dBFS without burying the weakest.";
        }
        else
        {
            headline = "Overall: all monitored channels are within the optimal -25 to -6 dBFS range. No change needed.";
        }

        return headline + "\n\nPer-channel detail:\n" + sb.toString().trim();
    }

    /**
     * Sends a Gemini consultation for the given stats CSV, invoking {@code onText} with the
     * recommendation on success or {@code onFailure} when no usable response is obtained.
     */
    private void sendGeminiConsultation(String csv, java.util.function.Consumer<String> onText, Runnable onFailure)
    {
        String prompt = "You are an RF engineer advising on SDR receiver gain settings.\n" +
                "The following table shows I/Q signal power statistics for active receive channels, sampled " +
                "over time. Each channel may share a physical SDR tuner with others. Columns:\n" +
                "  freq_mhz         - channel center frequency\n" +
                "  avg_dbfs         - average I/Q power (mix of signal and idle)\n" +
                "  noise_floor_dbfs - minimum observed power (approximate noise floor)\n" +
                "  peak_dbfs        - maximum observed power (approximate peak signal)\n" +
                "  snr_estimate_db  - peak minus noise floor (rough usable dynamic range / SNR)\n" +
                "  sample_count     - number of measurements\n\n" +
                csv + "\n\n" +
                "Guidance: optimal ADC utilization keeps peaks in the -25 to -6 dBFS window. peak_dbfs above " +
                "-3 dBFS risks clipping (reduce gain). noise_floor_dbfs below about -35 dBFS with low " +
                "snr_estimate_db suggests the signal is weak relative to the noise (increase gain). A healthy " +
                "snr_estimate_db is roughly 20 dB or more. Because all channels on one tuner share the same " +
                "hardware gain, recommend a single setting that keeps the strongest channel below clipping " +
                "while giving the weakest channel acceptable SNR.\n" +
                "Respond with a brief (2-4 sentence) plain-English recommendation: whether to increase, " +
                "decrease, or keep the tuner gain, by roughly how much, and why.";

        String apiKey = mUserPreferences.getAIPreference().getGeminiApiKey();
        String model = mUserPreferences.getAIPreference().getGeminiModel();
        if(model == null || model.isEmpty()) model = "models/gemini-1.5-flash";
        //Normalize so the path has exactly one "models/" segment regardless of how it is stored.
        if(!model.startsWith("models/")) model = "models/" + model;
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
                                    onText.accept(recommendation);
                                    return;
                                }
                            }
                            mLog.debug("AdaptiveGain AI consultation returned no text part");
                            onFailure.run();
                        }
                        else
                        {
                            mLog.debug("AdaptiveGain AI consultation returned HTTP {}", response.statusCode());
                            onFailure.run();
                        }
                    })
                    .exceptionally(ex -> {
                        mLog.debug("AdaptiveGain AI consultation failed: {}", ex.getMessage());
                        onFailure.run();
                        return null;
                    });
        }
        catch(Exception e)
        {
            mLog.debug("AdaptiveGain AI consultation error: {}", e.getMessage());
            onFailure.run();
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
        private volatile long mFrequencyHz = 0;

        ChannelStats(String channelName)
        {
            mChannelName = channelName;
        }

        void setFrequencyHz(long frequencyHz)
        {
            mFrequencyHz = frequencyHz;
        }

        long getFrequencyHz()
        {
            return mFrequencyHz;
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
