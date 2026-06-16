/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.gui.preference.application;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.application.ApplicationPreference;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import io.github.dsheirer.gui.RestartManager;
import io.github.dsheirer.gui.RuntimeModelRegistry;
import javafx.scene.layout.Region;
import javafx.util.StringConverter;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import io.github.dsheirer.gui.UsbMonitorManager;
import io.github.dsheirer.gui.WindowsReliabilityManager;
import org.controlsfx.control.ToggleSwitch;
import javafx.scene.control.Tooltip;

/**
 * Preference settings for application
 */
public class ApplicationPreferenceEditor extends HBox
{
    private ApplicationPreference mApplicationPreference;
    private VBox mEditorPane;
    private Label mAutoStartTimeoutLabel;
    private Spinner<Integer> mTimeoutSpinner;
    private Label mMemoryLimitLabel;
    private ComboBox<MemoryOption> mMemoryComboBox;
    private Label mMemoryWarningLabel;
    private ToggleSwitch mAutomaticDiagnosticMonitoringToggle;
    private ToggleSwitch mUsbMonitorToggle;
    private boolean mUpdatingUsbMonitor = false;

    /**
     * Constructs an instance
     * @param userPreferences for obtaining reference to preference.
     */
    private UserPreferences mUserPreferences;
    public ApplicationPreferenceEditor(UserPreferences userPreferences)
    {
        mUserPreferences = userPreferences;
        mApplicationPreference = userPreferences.getApplicationPreference();
        setMaxWidth(Double.MAX_VALUE);

        VBox vbox = new VBox();
        vbox.setMaxHeight(Double.MAX_VALUE);
        vbox.setMaxWidth(Double.MAX_VALUE);
        vbox.getChildren().add(getEditorPane());
        HBox.setHgrow(vbox, Priority.ALWAYS);
        getChildren().add(vbox);
    }

    private VBox getEditorPane()
    {
        if(mEditorPane == null)
        {
            mEditorPane = new VBox(20);
            mEditorPane.setMaxWidth(Double.MAX_VALUE);
            mEditorPane.setPadding(new Insets(10, 10, 10, 10));

            // Card 1: Diagnostic Monitoring
            Label monitoringLabel = new Label("Application Health and Diagnostic Monitoring.");
            monitoringLabel.getStyleClass().add("hig-section-header");
            io.github.dsheirer.gui.preference.layout.SettingsCard diagCard = new io.github.dsheirer.gui.preference.layout.SettingsCard();
            diagCard.getChildren().add(new io.github.dsheirer.gui.preference.layout.SettingsRow("Enable Diagnostic Monitoring", getAutomaticDiagnosticMonitoringToggle()));

            // Card 1.5: Theme
            Label themeLabel = new Label("Application Theme");
            themeLabel.getStyleClass().add("hig-section-header");
            io.github.dsheirer.gui.preference.layout.SettingsCard themeCard = new io.github.dsheirer.gui.preference.layout.SettingsCard();
            ToggleSwitch themeToggle = new ToggleSwitch();
            themeToggle.setTooltip(new Tooltip("Enable Night Mode (Dark Theme)"));
            themeToggle.setSelected(io.github.dsheirer.gui.theme.ThemeManager.isNightModeEnabled());
            themeToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
                io.github.dsheirer.gui.theme.ThemeManager.toggleNightMode();
            });
            themeCard.getChildren().add(new io.github.dsheirer.gui.preference.layout.SettingsRow("Enable Dark Mode", themeToggle));

            // Card 1.75: Audio Toggles
            Label audioLabel = new Label("Audio Feature Toggles");
            audioLabel.getStyleClass().add("hig-section-header");
            io.github.dsheirer.gui.preference.layout.SettingsCard audioCard = new io.github.dsheirer.gui.preference.layout.SettingsCard();
            
            ToggleSwitch twoToneToggle = new ToggleSwitch();
            twoToneToggle.setTooltip(new Tooltip("Globally enable or disable Two-Tone audio detection. Disable to save CPU."));
            twoToneToggle.setSelected(mApplicationPreference.isAudioTwoToneDetectEnabled());
            twoToneToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
                mApplicationPreference.setAudioTwoToneDetectEnabled(newValue);
            });
            
            ToggleSwitch aliasToggle = new ToggleSwitch();
            aliasToggle.setTooltip(new Tooltip("Globally enable or disable identifying unit alias detection from audio/control streams. Disable to save CPU."));
            aliasToggle.setSelected(mApplicationPreference.isAudioAliasDetectEnabled());
            aliasToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
                mApplicationPreference.setAudioAliasDetectEnabled(newValue);
            });
            
            audioCard.getChildren().addAll(
                new io.github.dsheirer.gui.preference.layout.SettingsRow("Enable Two-Tone Detection", twoToneToggle),
                new io.github.dsheirer.gui.preference.layout.SettingsRow("Enable Talker Alias Detection (Identifying Units)", aliasToggle)
            );

            // Card 2: Auto Start
            Label autoStartLabel = new Label("Channel Auto-start Disable Timeout");
            autoStartLabel.getStyleClass().add("hig-section-header");
            HBox spinnerBox = new HBox(5);
            spinnerBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            spinnerBox.getChildren().addAll(getTimeoutSpinner(), new Label("seconds"));
            io.github.dsheirer.gui.preference.layout.SettingsCard autoStartCard = new io.github.dsheirer.gui.preference.layout.SettingsCard();
            autoStartCard.getChildren().add(new io.github.dsheirer.gui.preference.layout.SettingsRow("Timeout Duration", spinnerBox));

            // Card 3: Memory Limit
            Label memoryLabel = new Label("Memory Limit");
            memoryLabel.getStyleClass().add("hig-section-header");
            io.github.dsheirer.gui.preference.layout.SettingsCard memoryCard = new io.github.dsheirer.gui.preference.layout.SettingsCard();
            memoryCard.getChildren().add(new io.github.dsheirer.gui.preference.layout.SettingsRow("Allocated Memory", getMemoryComboBox()));
            getMemoryWarningLabel().getStyleClass().add("kennebec-secondary-text");
            getMemoryWarningLabel().setPadding(new Insets(5, 15, 5, 15));

            //Usage-based recommendation + one-click apply + restart, derived from the current tuners/channels/streams.
            Label recommendationLabel = new Label(buildRecommendationText());
            recommendationLabel.getStyleClass().add("kennebec-secondary-text");
            recommendationLabel.setWrapText(true);
            recommendationLabel.setPadding(new Insets(0, 15, 5, 15));

            Button applyRecommendedButton = new Button("Apply Recommended");
            applyRecommendedButton.setTooltip(new Tooltip("Set the allocated memory to the recommended value for your current tuners, channels, and streams."));
            applyRecommendedButton.setOnAction(e -> {
                int recommended = computeRecommendedMemoryGb();
                if(recommended > 0)
                {
                    mApplicationPreference.setAllocatedMemory(recommended);
                    selectMemoryOption(recommended);
                }
            });

            Button restartButton = new Button("Restart Now");
            restartButton.setTooltip(new Tooltip("Restart SDRTrunk now so memory and other changes take effect."));
            restartButton.setOnAction(e -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Restart SDRTrunk now to apply changes?",
                        ButtonType.OK, ButtonType.CANCEL);
                io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(confirm.getDialogPane());
                confirm.setHeaderText("Restart SDRTrunk");
                confirm.setTitle("Restart Required");
                confirm.showAndWait().ifPresent(buttonType -> {
                    if(buttonType == ButtonType.OK)
                    {
                        if(!RestartManager.restart())
                        {
                            Alert error = new Alert(Alert.AlertType.ERROR, "Could not locate the SDRTrunk launcher to " +
                                    "restart automatically. Please close and reopen SDRTrunk manually.", ButtonType.OK);
                            io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(error.getDialogPane());
                            error.showAndWait();
                        }
                    }
                });
            });

            HBox memoryButtonBox = new HBox(10, applyRecommendedButton, restartButton);
            memoryButtonBox.setPadding(new Insets(0, 15, 10, 15));

            // Card 4: USB Monitor (Only on Windows 10/11)
            Label usbLabel = null;
            io.github.dsheirer.gui.preference.layout.SettingsCard usbMonitorCard = null;
            Label usbDesc = null;
            String osName = System.getProperty("os.name");
            if (osName != null && (osName.startsWith("Windows 10") || osName.startsWith("Windows 11"))) {
                usbLabel = new Label("USB Monitor Script");
                usbLabel.getStyleClass().add("hig-section-header");
                usbMonitorCard = new io.github.dsheirer.gui.preference.layout.SettingsCard();
                usbMonitorCard.getChildren().add(new io.github.dsheirer.gui.preference.layout.SettingsRow("Install Tuner Monitoring Power Script", getUsbMonitorToggle()));

                usbDesc = new Label("Installs a background script to auto-reset failing SDR USB devices.");
                usbDesc.getStyleClass().add("kennebec-secondary-text");
                usbDesc.setPadding(new Insets(5, 15, 5, 15));
            }


            // Card 5: System Reliability (Only on Windows 10/11)
            Label srLabel = null;
            io.github.dsheirer.gui.preference.layout.SettingsCard systemReliabilityCard = null;
            if (WindowsReliabilityManager.isWindows10OrNewer()) {
                srLabel = new Label("System Reliability");
                srLabel.getStyleClass().add("hig-section-header");

                systemReliabilityCard = new io.github.dsheirer.gui.preference.layout.SettingsCard();

                ToggleSwitch autoStartToggle = new ToggleSwitch();
                autoStartToggle.setTooltip(new Tooltip("Automatically launch SDRTrunk when you log into Windows"));
                autoStartToggle.setSelected(mApplicationPreference.isAutoStartEnabled());
                autoStartToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
                    mApplicationPreference.setAutoStartEnabled(newValue);
                    WindowsReliabilityManager.setAutoStart(newValue);
                });

                ToggleSwitch watchdogToggle = new ToggleSwitch();
                watchdogToggle.setTooltip(new Tooltip("Monitors SDRTrunk and restarts it if it crashes or is closed unexpectedly"));
                watchdogToggle.setSelected(mApplicationPreference.isWatchdogEnabled());
                watchdogToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
                    mApplicationPreference.setWatchdogEnabled(newValue);
                });

                systemReliabilityCard.getChildren().addAll(
                        new io.github.dsheirer.gui.preference.layout.SettingsRow("Start SDRTrunk automatically on computer startup", autoStartToggle),
                        new io.github.dsheirer.gui.preference.layout.SettingsRow("Automatically restart SDRTrunk if it closes unexpectedly", watchdogToggle)
                );
            }

            mEditorPane.getChildren().addAll(monitoringLabel, diagCard, themeLabel, themeCard, audioLabel, audioCard, autoStartLabel, autoStartCard, memoryLabel, memoryCard, getMemoryWarningLabel(), recommendationLabel, memoryButtonBox);
            if (usbMonitorCard != null) {
                mEditorPane.getChildren().addAll(usbLabel, usbMonitorCard, usbDesc);
            }
            if (systemReliabilityCard != null) {
                mEditorPane.getChildren().addAll(srLabel, systemReliabilityCard);
            }

            // Card 6: Remote Access Optimization
            Label remoteLabel = new Label("Remote Access Optimization");
            remoteLabel.getStyleClass().add("hig-section-header");
            io.github.dsheirer.gui.preference.layout.SettingsCard remoteCard = new io.github.dsheirer.gui.preference.layout.SettingsCard();
            
            ToggleSwitch remoteToggle = new ToggleSwitch();
            remoteToggle.setTooltip(new Tooltip("Automatically throttles UI animations and FFT framerate when a remote desktop connection is detected (e.g. RustDesk)"));
            remoteToggle.setSelected(mApplicationPreference.isRemoteAccessOptimization());
            remoteToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
                mApplicationPreference.setRemoteAccessOptimization(newValue);
            });
            remoteCard.getChildren().add(new io.github.dsheirer.gui.preference.layout.SettingsRow("Enable Remote Desktop Optimizations", remoteToggle));

            mEditorPane.getChildren().addAll(remoteLabel, remoteCard);

            // Card 7: First-Time Wizard
            Label wizardLabel = new Label("Setup Wizard");
            wizardLabel.getStyleClass().add("hig-section-header");
            io.github.dsheirer.gui.preference.layout.SettingsCard wizardCard = new io.github.dsheirer.gui.preference.layout.SettingsCard();
            javafx.scene.control.Button wizardBtn = new javafx.scene.control.Button("Run Setup Wizard Again...");
            wizardBtn.getStyleClass().add("kennebec-primary-button");
            wizardBtn.setOnAction(e -> {
                java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(io.github.dsheirer.gui.SDRTrunk.class);
                prefs.putBoolean("sdrtrunk.first.time.wizard.completed", false);
                javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                a.setTitle("Setup Wizard");
                a.setHeaderText("Wizard Reset");
                a.setContentText("The first-time setup wizard will run the next time SDRTrunk starts. Please restart the application.");
                io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(a.getDialogPane());
                a.showAndWait();
            });
            wizardCard.getChildren().add(new io.github.dsheirer.gui.preference.layout.SettingsRow("Re-run initial configuration wizard", wizardBtn));
            mEditorPane.getChildren().addAll(wizardLabel, wizardCard);
        }

        return mEditorPane;

    }

    private Label getAutoStartTimeoutLabel()
    {
        if(mAutoStartTimeoutLabel == null)
        {
            mAutoStartTimeoutLabel = new Label("Channel Auto-start Disable Timeout");
        }

        return mAutoStartTimeoutLabel;
    }

    private Spinner<Integer> getTimeoutSpinner()
    {
        if(mTimeoutSpinner == null)
        {
            mTimeoutSpinner = new Spinner<>(0, 30, mApplicationPreference.getChannelAutoStartTimeout(), 1);
            mTimeoutSpinner.setTooltip(new Tooltip("Delay in seconds before auto-starting channels when the application loads. Use higher values if tuners take longer to initialize."));
            mTimeoutSpinner.valueProperty().addListener((observable, oldValue, newValue) -> mApplicationPreference.setChannelAutoStartTimeout(newValue));
        }

        return mTimeoutSpinner;
    }

    /**
     * Toggle switch to enable/disable automatic diagnostic monitoring.
     */
    private ToggleSwitch getAutomaticDiagnosticMonitoringToggle()
    {
        if(mAutomaticDiagnosticMonitoringToggle == null)
        {
            mAutomaticDiagnosticMonitoringToggle = new ToggleSwitch();
            mAutomaticDiagnosticMonitoringToggle.setTooltip(new Tooltip("Monitors system health (memory, CPU, tuners) and logs warnings if issues are detected"));
            mAutomaticDiagnosticMonitoringToggle.setSelected(mApplicationPreference.isAutomaticDiagnosticMonitoring());
            mAutomaticDiagnosticMonitoringToggle.selectedProperty().addListener((observable, oldValue, enabled) ->
                    mApplicationPreference.setAutomaticDiagnosticMonitoring(enabled));
        }

        return mAutomaticDiagnosticMonitoringToggle;
    }

    private ToggleSwitch getUsbMonitorToggle()
    {
        if(mUsbMonitorToggle == null)
        {
            mUsbMonitorToggle = new ToggleSwitch();
            mUsbMonitorToggle.setTooltip(new Tooltip("Installs a Windows service to automatically reset SDR USB devices if they become unresponsive"));
            mUsbMonitorToggle.setSelected(mApplicationPreference.isUsbMonitorInstalled());
            mUsbMonitorToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
                if (mUpdatingUsbMonitor) return;

                mUsbMonitorToggle.setDisable(true);
                
                java.util.concurrent.CompletableFuture.runAsync(() -> {
                    if(newValue) {
                        boolean success = UsbMonitorManager.install(mUserPreferences);
                        javafx.application.Platform.runLater(() -> {
                            if(!success) {
                                mUpdatingUsbMonitor = true;
                                mUsbMonitorToggle.setSelected(false);
                                mUpdatingUsbMonitor = false;
                            }
                            mUsbMonitorToggle.setDisable(false);
                        });
                    } else {
                        boolean success = UsbMonitorManager.uninstall(mUserPreferences);
                        javafx.application.Platform.runLater(() -> {
                            if(!success) {
                                mUpdatingUsbMonitor = true;
                                mUsbMonitorToggle.setSelected(true);
                                mUpdatingUsbMonitor = false;
                            }
                            mUsbMonitorToggle.setDisable(false);
                        });
                    }
                });
            });
        }
        return mUsbMonitorToggle;
    }

    private Label getMemoryLimitLabel()
    {
        if(mMemoryLimitLabel == null)
        {
            mMemoryLimitLabel = new Label("Allocated Memory");
        }

        return mMemoryLimitLabel;
    }


    public static class MemoryOption {
        private final int value;
        private final String label;

        public MemoryOption(int value, String label) {
            this.value = value;
            this.label = label;
        }

        public int getValue() {
            return value;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return label;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            MemoryOption that = (MemoryOption) obj;
            return value == that.value;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(value);
        }
    }

    private ComboBox<MemoryOption> getMemoryComboBox()
    {
        if(mMemoryComboBox == null)
        {
            mMemoryComboBox = new ComboBox<>();
            mMemoryComboBox.setMinWidth(Region.USE_PREF_SIZE);

            long maxMemoryBytes = ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getTotalMemorySize();
            long systemMaxMemoryGb = Math.round(maxMemoryBytes / (1024.0 * 1024.0 * 1024.0));
            long maxAllowed = Math.min(systemMaxMemoryGb, 32);

            List<MemoryOption> options = new ArrayList<>();
            int[] powersOfTwo = {2, 4, 8, 16, 32};

            for (int gb : powersOfTwo) {
                if (gb <= maxAllowed) {
                    String label = gb + " GB";
                    if (gb == 2) {
                        label += " - Recommended for 1 tuner";
                    } else if (gb == 4) {
                        label += " - Recommended for 2-3 tuners";
                    } else if (gb >= 8) {
                        label += " - Recommended for 4+ tuners";
                    }
                    options.add(new MemoryOption(gb, label));
                }
            }

            // In case maxAllowed is less than 2GB or not a power of 2, add it if missing and sensible
            if (options.isEmpty() || maxAllowed < 2) {
                 options.add(new MemoryOption((int) Math.max(1, maxAllowed), Math.max(1, maxAllowed) + " GB"));
            } else if (!options.stream().anyMatch(opt -> opt.getValue() == maxAllowed) && maxAllowed > options.get(options.size() - 1).getValue()) {
                 options.add(new MemoryOption((int) maxAllowed, maxAllowed + " GB"));
            }

            mMemoryComboBox.getItems().addAll(options);

            int currentMemory = mApplicationPreference.getAllocatedMemory();
            MemoryOption currentOption = new MemoryOption(currentMemory, currentMemory + " GB");

            if (!mMemoryComboBox.getItems().contains(currentOption)) {
                // Insert it in sorted order
                int index = 0;
                for (int i = 0; i < options.size(); i++) {
                    if (currentMemory < options.get(i).getValue()) {
                        break;
                    }
                    index++;
                }
                mMemoryComboBox.getItems().add(index, currentOption);
            }

            mMemoryComboBox.getSelectionModel().select(currentOption);

            mMemoryComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null && oldValue != null && !newValue.equals(oldValue)) {
                    mApplicationPreference.setAllocatedMemory(newValue.getValue());
                    javafx.application.Platform.runLater(() -> {
                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                        alert.setTitle("Restart Required");
                        alert.setHeaderText("Memory Allocation Changed");
                        alert.setContentText("A restart of SDRTrunk is required for the new memory allocation to take effect.");
                        alert.showAndWait();
                    });
                } else if (newValue != null && oldValue == null) {
                    mApplicationPreference.setAllocatedMemory(newValue.getValue());
                }
            });
        }

        return mMemoryComboBox;
    }

    private Label getMemoryWarningLabel()
    {
        if(mMemoryWarningLabel == null)
        {
            mMemoryWarningLabel = new Label("A restart of SDRTrunk is required for this setting to take effect.");
        }

        return mMemoryWarningLabel;
    }

    /**
     * Total physical system RAM in GB, or 0 if it cannot be determined.
     */
    private long systemRamGb()
    {
        try
        {
            long bytes = ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getTotalMemorySize();
            return Math.round(bytes / (1024.0 * 1024.0 * 1024.0));
        }
        catch(Throwable t)
        {
            return 0;
        }
    }

    /**
     * Computes a recommended heap allocation (GB) from the current workload: more tuners, channels, and streams
     * require more heap.  Capped to leave headroom for the OS and never exceeds 16 GB or available system RAM.
     */
    private int computeRecommendedMemoryGb()
    {
        int tuners = Math.max(0, RuntimeModelRegistry.getActiveTunerCount());
        int channels = Math.max(0, RuntimeModelRegistry.getTotalChannelCount());
        int streams = Math.max(0, RuntimeModelRegistry.getStreamCount());

        //Base 2 GB, +1 GB per additional tuner, +1 GB per 50 channels, +1 GB for heavy streaming.
        double gb = 2.0;
        if(tuners > 1)
        {
            gb += (tuners - 1);
        }
        gb += channels / 50.0;
        if(streams > 10)
        {
            gb += 1.0;
        }
        if(streams > 30)
        {
            gb += 1.0;
        }

        int recommended = Math.max(2, (int) Math.ceil(gb));

        long systemGb = systemRamGb();
        int cap = 16;
        if(systemGb > 0)
        {
            //Leave ~2 GB for the OS.
            cap = (int) Math.min(16, Math.max(2, systemGb - 2));
        }

        return Math.min(recommended, cap);
    }

    /**
     * Builds the human-readable recommendation, including a CPU-saturation caveat for multi-tuner setups on
     * low-core machines (a common cause of UI freezes that more RAM will not fix).
     */
    private String buildRecommendationText()
    {
        int tuners = RuntimeModelRegistry.getActiveTunerCount();
        int channels = RuntimeModelRegistry.getTotalChannelCount();
        int streams = RuntimeModelRegistry.getStreamCount();
        int cores = Runtime.getRuntime().availableProcessors();
        int recommended = computeRecommendedMemoryGb();

        StringBuilder sb = new StringBuilder();
        sb.append("Recommended: ").append(recommended).append(" GB  (based on ");
        sb.append(tuners < 0 ? "?" : tuners).append(" tuner(s), ");
        sb.append(channels < 0 ? "?" : channels).append(" channel(s), ");
        sb.append(streams < 0 ? "?" : streams).append(" stream(s), ");
        sb.append(cores).append(" CPU cores).");

        if(tuners >= 2 && cores <= 4)
        {
            sb.append("  Note: running multiple tuners on ").append(cores).append(" CPU cores can saturate the CPU " +
                    "and freeze the UI - this is a CPU limit that more memory will not fix. Consider lowering each " +
                    "tuner's sample rate (e.g. 2.5 MSPS) or reducing the number of active tuners/channels.");
        }

        return sb.toString();
    }

    /**
     * Selects the given memory value in the combo box, inserting it in sorted order if it is not already present.
     */
    private void selectMemoryOption(int gb)
    {
        ComboBox<MemoryOption> combo = getMemoryComboBox();
        MemoryOption target = new MemoryOption(gb, gb + " GB");

        if(!combo.getItems().contains(target))
        {
            int index = 0;
            for(MemoryOption option : combo.getItems())
            {
                if(gb < option.getValue())
                {
                    break;
                }
                index++;
            }
            combo.getItems().add(Math.min(index, combo.getItems().size()), target);
        }

        combo.getSelectionModel().select(target);
    }
}
