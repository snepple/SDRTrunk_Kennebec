package io.github.dsheirer.gui;

import javax.swing.JLabel;
import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;
import java.util.TimerTask;

public class SystemHealthAdvisorTask extends TimerTask {
    private final JLabel performanceLabel;
    private final OperatingSystemMXBean osBean;

    public SystemHealthAdvisorTask(JLabel performanceLabel) {
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

        String suggestions = "<p>No suggestions at this time.</p>";
        if (cpuLoad > 0.8) {
            suggestions = "<p style='color:red;'>High CPU usage detected. Consider reducing sample rates or disabling waterfall displays to prevent audio dropouts.</p>";
        } else if (memoryUsage > 0.8) {
             suggestions = "<p style='color:red;'>High Memory usage detected. Consider closing unused features or tuners.</p>";
        }

        final String finalHtml = "<html><div style='text-align: center; padding: 20px;'>"
                + "<h1>System Health & Performance Advisor</h1>"
                + "<p>Status: Active</p>"
                + "<p>CPU Usage: " + cpuStatus + "</p>"
                + "<p>Memory Usage: " + memoryStatus + "</p>"
                + "<br>"
                + "<p><i>Optimization Suggestions:</i></p>"
                + suggestions
                + "</div></html>";

        javax.swing.SwingUtilities.invokeLater(() -> {
            performanceLabel.setText(finalHtml);
        });
    }
}
