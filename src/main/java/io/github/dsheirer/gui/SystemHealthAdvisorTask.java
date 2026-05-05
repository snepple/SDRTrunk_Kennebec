package io.github.dsheirer.gui;

import javafx.application.Platform;
import javafx.scene.control.Label;
import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;
import java.util.TimerTask;
import io.github.dsheirer.health.SystemHealthAlertEvent;
import io.github.dsheirer.eventbus.MyEventBus;

public class SystemHealthAdvisorTask extends TimerTask {
    private final Label performanceLabel;
    private final OperatingSystemMXBean osBean;

    public SystemHealthAdvisorTask(Label performanceLabel) {
        this.performanceLabel = performanceLabel;
        this.osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    }

    @Override
    public void run() {
        double cpuLoad = osBean.getCpuLoad();
        long totalMemory = osBean.getTotalMemorySize();
        long freeMemory = osBean.getFreeMemorySize();
        long usedMemory = totalMemory - freeMemory;
        double memoryUsage = (double) usedMemory / totalMemory;

        String cpuStatus = cpuLoad < 0.0 ? "Unknown" : String.format("%.1f%%", cpuLoad * 100);
        String memoryStatus = String.format("%.1f%%", memoryUsage * 100);

        String suggestions = "No suggestions at this time.";
        boolean warning = false;

        if (cpuLoad > 0.8) {
            suggestions = "High CPU usage detected. Consider reducing sample rates or disabling waterfall displays to prevent audio dropouts.";
            warning = true;
        } else if (memoryUsage > 0.8) {
             suggestions = "High Memory usage detected. Consider closing unused features or tuners.";
             warning = true;
        }

        final String statusText = "Status: Active\n"
                + "CPU Usage: " + cpuStatus + "\n"
                + "Memory Usage: " + memoryStatus + "\n\n"
                + "Optimization Suggestions:\n"
                + suggestions;


        if (warning) {
            SystemHealthAlertEvent event = new SystemHealthAlertEvent(
                SystemHealthAlertEvent.AlertType.SYSTEM,
                "Application and System Error",
                suggestions
            );
            MyEventBus.getGlobalEventBus().post(event);
        }

        final boolean isWarning = warning;

        Platform.runLater(() -> {
            performanceLabel.setText(statusText);
            if (isWarning) {
                performanceLabel.setStyle("-fx-text-fill: #ff3b30;"); // HIG destructive red
            } else {
                performanceLabel.setStyle("-fx-text-fill: -fx-text-base-color;"); // default
            }
        });
    }
}
