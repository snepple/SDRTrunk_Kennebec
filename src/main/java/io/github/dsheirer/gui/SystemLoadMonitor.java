package io.github.dsheirer.gui;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import io.github.dsheirer.controller.NamingThreadFactory;

/**
 * Monitors system CPU load dynamically.
 * Can be used by UI components to reduce refresh rates during CPU spikes.
 */
public class SystemLoadMonitor {
    private static final Logger mLog = LoggerFactory.getLogger(SystemLoadMonitor.class);
    
    private static SystemLoadMonitor mInstance;
    private final DoubleProperty mCpuLoad = new SimpleDoubleProperty(0.0);
    private final ScheduledExecutorService mExecutorService;
    private com.sun.management.OperatingSystemMXBean mOsBean;

    private SystemLoadMonitor() {
        try {
            java.lang.management.OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
            if (bean instanceof com.sun.management.OperatingSystemMXBean sunBean) {
                mOsBean = sunBean;
            } else {
                mLog.warn("OperatingSystemMXBean is not com.sun.management.OperatingSystemMXBean. CPU monitoring disabled.");
            }
        } catch (Exception e) {
            mLog.error("Failed to initialize OperatingSystemMXBean", e);
        }

        mExecutorService = Executors.newSingleThreadScheduledExecutor(new NamingThreadFactory("System-Load-Monitor"));
        if (mOsBean != null) {
            mExecutorService.scheduleAtFixedRate(this::updateCpuLoad, 0, 1, TimeUnit.SECONDS);
        }
    }

    public static synchronized SystemLoadMonitor getInstance() {
        if (mInstance == null) {
            mInstance = new SystemLoadMonitor();
        }
        return mInstance;
    }

    private void updateCpuLoad() {
        if (mOsBean != null) {
            double load = mOsBean.getCpuLoad();
            if (load >= 0) {
                Platform.runLater(() -> mCpuLoad.set(load));
            }
        }
    }

    /**
     * @return Observable property for the current CPU load (0.0 to 1.0)
     */
    public DoubleProperty cpuLoadProperty() {
        return mCpuLoad;
    }

    /**
     * @return Current CPU load (0.0 to 1.0)
     */
    public double getCpuLoad() {
        return mCpuLoad.get();
    }
    
    /**
     * Stop the executor.
     */
    public void dispose() {
        mExecutorService.shutdownNow();
    }
}
