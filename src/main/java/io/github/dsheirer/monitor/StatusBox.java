/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.monitor;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

/**
 * JavaFX status panel box.
 */
public class StatusBox extends HBox
{
    private ResourceMonitor mResourceMonitor;

    /**
     * Constructs an instance.
     * @param resourceMonitor for accessing resource usage statistics.
     */
    public StatusBox(ResourceMonitor resourceMonitor)
    {
        mResourceMonitor = resourceMonitor;
        setPadding(new Insets(1, 4, 1, 4));
        setSpacing(3);
        setAlignment(Pos.CENTER_LEFT);

        // --- CPU ---
        Label cpuLabel = createLabel("CPU:", "Java process CPU usage. Disabled if the CPU loading is not available from the OS");
        getChildren().add(cpuLabel);

        ProgressBar cpuIndicator = createBar(60);
        cpuIndicator.progressProperty().bind(mResourceMonitor.cpuPercentageProperty());
        cpuIndicator.disableProperty().bind(mResourceMonitor.cpuAvailableProperty().not());
        cpuIndicator.setTooltip(new Tooltip("Java process CPU usage"));
        getChildren().add(cpuIndicator);

        // --- Allocated Memory ---
        Label memoryLabel = createLabel("Alloc:", "Percentage of total system memory that Java has reserved from the Operating System");
        getChildren().add(memoryLabel);

        ProgressBar memoryBar = createBar(60);
        memoryBar.progressProperty().bind(mResourceMonitor.systemMemoryUsedPercentageProperty());
        memoryBar.setTooltip(new Tooltip("Allocated Memory - Percentage of total system memory reserved by Java"));
        getChildren().add(memoryBar);

        // --- Used Memory ---
        Label javaMemoryLabel = createLabel("Used:", "Percentage of allocated memory that Java/sdrtrunk is currently using");
        getChildren().add(javaMemoryLabel);

        ProgressBar javaMemoryBar = createBar(60);
        javaMemoryBar.progressProperty().bind(mResourceMonitor.javaMemoryUsedPercentageProperty());
        javaMemoryBar.setTooltip(new Tooltip("Used Memory - Percentage of allocated memory in use. Fluctuates with garbage collection"));
        getChildren().add(javaMemoryBar);

        // --- Event Logs ---
        Label eventLogsLabel = createLabel("Logs:", "Percentage of drive space used for event logs based on user-specified max threshold");
        getChildren().add(eventLogsLabel);

        ProgressBar eventLogsBar = createBar(50);
        eventLogsBar.progressProperty().bind(mResourceMonitor.directoryUsePercentEventLogsProperty());
        eventLogsBar.setTooltip(new Tooltip("Event Logs - Drive space usage vs. max threshold in user preferences"));
        getChildren().add(eventLogsBar);

        Label eventLogsSizeLabel = new Label();
        eventLogsSizeLabel.textProperty().bind(mResourceMonitor.fileSizeEventLogsProperty());
        eventLogsSizeLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        eventLogsSizeLabel.setStyle("-fx-font-size: 10px;");
        getChildren().add(eventLogsSizeLabel);

        // --- Recordings ---
        Label recordingsLabel = createLabel("Rec:", "Percentage of drive space used for recordings based on user-specified max threshold");
        getChildren().add(recordingsLabel);

        ProgressBar recordingsBar = createBar(50);
        recordingsBar.progressProperty().bind(mResourceMonitor.directoryUsePercentRecordingsProperty());
        recordingsBar.setTooltip(new Tooltip("Recordings - Drive space usage vs. max threshold in user preferences"));
        getChildren().add(recordingsBar);

        Label recordingsSizeLabel = new Label();
        recordingsSizeLabel.textProperty().bind(mResourceMonitor.fileSizeRecordingsProperty());
        recordingsSizeLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        recordingsSizeLabel.setStyle("-fx-font-size: 10px;");
        getChildren().add(recordingsSizeLabel);

        // --- Dropped Buffers ---
        Label droppedBuffersLabel = createLabel("Drops:", "Total number of native buffers dropped across all tuners due to processor overload");
        getChildren().add(droppedBuffersLabel);

        Label droppedBuffersValue = new Label();
        droppedBuffersValue.textProperty().bind(mResourceMonitor.droppedBuffersProperty().asString());
        droppedBuffersValue.setTooltip(new Tooltip("Dropped Buffers - Native buffers dropped due to processor overload"));
        droppedBuffersValue.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        droppedBuffersValue.setStyle("-fx-font-size: 10px;");
        getChildren().add(droppedBuffersValue);

        // --- Remote Desktop Indicator ---
        jiconfont.javafx.IconNode remoteIcon = new jiconfont.javafx.IconNode(jiconfont.icons.font_awesome.FontAwesome.DESKTOP);
        remoteIcon.setIconSize(14);
        Label remoteLabel = new Label("", remoteIcon);
        remoteLabel.setTooltip(new Tooltip("Remote Desktop Mode Active (Animations/FFT throttled)"));
        remoteLabel.setVisible(false);
        remoteLabel.setManaged(false);
        getChildren().add(remoteLabel);

        io.github.dsheirer.eventbus.MyEventBus.getGlobalEventBus().register(this);

        this.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                io.github.dsheirer.eventbus.MyEventBus.getGlobalEventBus().unregister(this);
            }
        });
    }

    /**
     * Creates a compact, non-truncating label with a tooltip.
     */
    private Label createLabel(String text, String tooltipText)
    {
        Label label = new Label(text);
        label.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        label.setTooltip(new Tooltip(tooltipText));
        label.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;");
        return label;
    }

    /**
     * Creates a narrow progress bar with a fixed preferred width.
     */
    private ProgressBar createBar(double width)
    {
        ProgressBar bar = new ProgressBar();
        bar.setPrefWidth(width);
        bar.setMaxWidth(width);
        bar.setPrefHeight(14);
        bar.setMaxHeight(14);
        return bar;
    }

    @com.google.common.eventbus.Subscribe
    public void onRemoteDesktopMode(io.github.dsheirer.eventbus.RemoteDesktopModeEvent event) {
        javafx.application.Platform.runLater(() -> {
            boolean active = event.isActive();
            getChildren().stream()
                .filter(node -> node instanceof Label && ((Label) node).getTooltip() != null && ((Label) node).getTooltip().getText().contains("Remote Desktop"))
                .forEach(node -> {
                    node.setVisible(active);
                    node.setManaged(active);
                });
        });
    }
}
