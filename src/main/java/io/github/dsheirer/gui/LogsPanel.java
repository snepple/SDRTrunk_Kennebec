package io.github.dsheirer.gui;

import io.github.dsheirer.monitor.DiagnosticMonitor;
import io.github.dsheirer.preference.UserPreferences;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.BorderPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class LogsPanel extends BorderPane {
    private static final Logger mLog = LoggerFactory.getLogger(LogsPanel.class);

    private LogsViewController controller;

    public LogsPanel(UserPreferences userPreferences, DiagnosticMonitor diagnosticMonitor) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LogsView.fxml"));
            loader.setRoot(this);
            loader.load();

            controller = loader.getController();
            controller.init(userPreferences, diagnosticMonitor);

            // Fix memory leak: hook up destruction
            sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene == null) {
                    if (controller != null) {
                        controller.destroy();
                    }
                }
            });

        } catch (IOException e) {
            mLog.error("Error loading LogsView.fxml", e);
        }
    }

    /**
     * Updates the diagnostic monitor on the underlying controller.  Used when the monitor becomes
     * available after this panel has already been constructed.
     */
    public void setDiagnosticMonitor(DiagnosticMonitor diagnosticMonitor) {
        if (controller != null) {
            controller.setDiagnosticMonitor(diagnosticMonitor);
        }
    }

    /**
     * Supplies the self-healing orchestrator to the underlying controller so AI log analysis can route into
     * auto-remediation and external notification.
     */
    public void setSelfHealingOrchestrator(io.github.dsheirer.preference.notification.SelfHealingOrchestrator orchestrator) {
        if (controller != null) {
            controller.setSelfHealingOrchestrator(orchestrator);
        }
    }
}
