package io.github.dsheirer.module.ai;

import io.github.dsheirer.monitor.ResourceMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.*;

/**
 * Predictive maintenance engine that monitors system health metrics and generates
 * warnings before failures occur. Analyzes CPU load, memory pressure, tuner error
 * rates, and disk I/O to predict potential issues.
 */
public class PredictiveMaintenanceEngine
{
    private static final Logger mLog = LoggerFactory.getLogger(PredictiveMaintenanceEngine.class);

    private final ScheduledExecutorService mExecutor;
    private final ConcurrentHashMap<String, Double> mMetrics = new ConcurrentHashMap<>();
    private final ResourceMonitor mResourceMonitor;

    private static final double CPU_WARN = 80.0;
    private static final double MEM_WARN = 85.0;
    private static final double TUNER_ERROR_RATE_WARN = 5.0;
    private static final double DISK_USAGE_WARN = 90.0;

    public PredictiveMaintenanceEngine(ResourceMonitor resourceMonitor)
    {
        mResourceMonitor = resourceMonitor;
        mExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "PredictiveMaintenance");
            t.setDaemon(true);
            return t;
        });
        mExecutor.scheduleAtFixedRate(this::analyze, 60, 60, TimeUnit.SECONDS);
        mLog.info("PredictiveMaintenanceEngine started (60s analysis interval)");
    }

    /**
     * Report a metric value for analysis.
     * @param key metric name (e.g. "cpu_percent", "memory_percent", "tuner_error_rate")
     * @param value current metric value
     */
    public void reportMetric(String key, double value)
    {
        mMetrics.put(key, value);
    }

    private void analyze()
    {
        try
        {
            // Pull live values from ResourceMonitor when available (reads are benign cross-thread
            // for SimpleDoubleProperty — worst case we get a slightly stale value, acceptable for
            // a 60-second warning system).
            if (mResourceMonitor != null)
            {
                double cpuFraction = mResourceMonitor.cpuPercentageProperty().get();
                if (cpuFraction > 0)
                {
                    mMetrics.put("cpu_percent", cpuFraction * 100.0);
                }

                double memFraction = mResourceMonitor.javaMemoryUsedPercentageProperty().get();
                mMetrics.put("memory_percent", memFraction * 100.0);

                double diskEventLogs = mResourceMonitor.directoryUsePercentEventLogsProperty().get();
                double diskRecordings = mResourceMonitor.directoryUsePercentRecordingsProperty().get();
                mMetrics.put("disk_usage_percent", Math.max(diskEventLogs, diskRecordings) * 100.0);
            }

            Double cpu = mMetrics.get("cpu_percent");
            if(cpu != null && cpu > CPU_WARN)
            {
                mLog.warn("Predictive: CPU at {}% - consider reducing active channels",
                    String.format("%.1f", cpu));
            }

            Double mem = mMetrics.get("memory_percent");
            if(mem != null && mem > MEM_WARN)
            {
                mLog.warn("Predictive: Memory at {}% - GC pressure detected, consider increasing heap",
                    String.format("%.1f", mem));
            }

            Double tunerErrors = mMetrics.get("tuner_error_rate");
            if(tunerErrors != null && tunerErrors > TUNER_ERROR_RATE_WARN)
            {
                mLog.warn("Predictive: Tuner error rate {}% - hardware may need attention",
                    String.format("%.1f", tunerErrors));
            }

            Double diskUsage = mMetrics.get("disk_usage_percent");
            if(diskUsage != null && diskUsage > DISK_USAGE_WARN)
            {
                mLog.warn("Predictive: Disk usage at {}% - recordings may fail soon",
                    String.format("%.1f", diskUsage));
            }
        }
        catch(Exception e)
        {
            mLog.error("PredictiveMaintenanceEngine analysis error", e);
        }
    }

    public Map<String, Double> getMetrics()
    {
        return new ConcurrentHashMap<>(mMetrics);
    }

    public void stop()
    {
        mExecutor.shutdownNow();
        mLog.info("PredictiveMaintenanceEngine stopped");
    }
}
