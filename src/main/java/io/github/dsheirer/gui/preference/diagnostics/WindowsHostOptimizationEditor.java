/*
 * *****************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.gui.preference.diagnostics;

import io.github.dsheirer.gui.preference.layout.SettingsCard;
import io.github.dsheirer.gui.preference.layout.SettingsRow;
import io.github.dsheirer.preference.UserPreferences;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Standalone editor for Windows Host Optimization settings.
 * Disables USB Selective Suspend, adds Windows Defender exclusions,
 * and disables DPTF throttling to prevent dropped packets.
 */
public class WindowsHostOptimizationEditor extends VBox
{
    public WindowsHostOptimizationEditor(UserPreferences userPreferences)
    {
        setPadding(new Insets(10, 10, 10, 10));
        setSpacing(20);
        setMaxWidth(Double.MAX_VALUE);

        // Section header
        Label header = new Label("Windows Host Optimization");
        header.getStyleClass().add("hig-section-header");
        getChildren().add(header);

        // Description
        Label description = new Label(
            "Disables USB Selective Suspend, adds Windows Defender exclusions, "
          + "and disables DPTF throttling to prevent dropped packets. "
          + "These optimizations help ensure stable SDR tuner operation.");
        description.setWrapText(true);
        description.getStyleClass().add("kennebec-secondary-text");
        description.setPadding(new Insets(0, 10, 0, 10));
        getChildren().add(description);

        // Action buttons
        Button checkBtn = new Button("Check Host Settings");
        Button applyBtn = new Button("Apply Optimizations (Requires Admin)");

        TextArea outputArea = new TextArea();
        outputArea.setPrefRowCount(12);
        outputArea.setEditable(false);
        outputArea.setPromptText("Click 'Check Host Settings' to view the current status of Windows "
            + "power plans and Defender exclusions...");
        outputArea.getStyleClass().add("log-inspector-text");
        outputArea.setWrapText(true);

        checkBtn.setOnAction(e -> {
            checkBtn.setDisable(true);
            applyBtn.setDisable(true);
            outputArea.setText("Checking host settings, please wait...");
            WindowsHostOptimizer.checkDiagnostics().thenAccept(result -> {
                javafx.application.Platform.runLater(() -> {
                    outputArea.setText(result);
                    checkBtn.setDisable(false);
                    applyBtn.setDisable(false);
                });
            });
        });

        applyBtn.setOnAction(e -> {
            checkBtn.setDisable(true);
            applyBtn.setDisable(true);
            String originalApplyText = applyBtn.getText();
            applyBtn.setText("Applying... Please approve UAC");

            WindowsHostOptimizer.runOptimizationScript().thenAccept(success -> {
                javafx.application.Platform.runLater(() -> {
                    checkBtn.setDisable(false);
                    applyBtn.setDisable(false);
                    applyBtn.setText(originalApplyText);

                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        success ? javafx.scene.control.Alert.AlertType.INFORMATION
                                : javafx.scene.control.Alert.AlertType.ERROR
                    );
                    alert.setTitle("Windows Host Optimization");
                    alert.setHeaderText(null);
                    alert.setContentText(success
                        ? "Optimizations applied successfully!"
                        : "Failed to apply optimizations. Please ensure you clicked 'Yes' on the Admin prompt.");
                    alert.showAndWait();

                    if (success) {
                        checkBtn.fire(); // Refresh the output
                    }
                });
            });
        });

        SettingsCard actionsCard = new SettingsCard();
        actionsCard.getChildren().add(new SettingsRow("Actions", new HBox(10, checkBtn, applyBtn)));
        getChildren().add(actionsCard);

        SettingsCard outputCard = new SettingsCard();
        outputCard.getChildren().add(outputArea);
        getChildren().add(outputCard);
    }
}
