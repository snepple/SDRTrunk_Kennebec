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
import io.github.dsheirer.preference.diagnostics.DiagnosticsCategory;
import io.github.dsheirer.preference.diagnostics.DiagnosticsPreference;
import io.github.dsheirer.preference.diagnostics.LogLevelController;
import java.util.EnumMap;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Runtime diagnostics preference panel. Lets the user toggle DEBUG logging for selected
 * subsystems without editing logback.xml and restarting.
 *
 * Introduced in ap-14.6.
 */
public class DiagnosticsPreferenceEditor extends VBox
{
    private final DiagnosticsPreference mDiagnosticsPreference;
    private final Map<DiagnosticsCategory, CheckBox> mCategoryCheckBoxes = new EnumMap<>(DiagnosticsCategory.class);
    private CheckBox mMasterAllToggle;

    public DiagnosticsPreferenceEditor(UserPreferences userPreferences)
    {
        mDiagnosticsPreference = userPreferences.getDiagnosticsPreference();
        setPadding(new Insets(10, 10, 10, 10));
        setSpacing(20);
        setMaxWidth(Double.MAX_VALUE);

        // Section header
        Label header = new Label("Runtime Diagnostics");
        header.getStyleClass().add("hig-section-header");
        getChildren().add(header);

        // Description
        Label description = new Label(
            "Enable DEBUG logging for individual subsystems without editing logback.xml. "
          + "Changes take effect immediately.");
        description.setWrapText(true);
        description.getStyleClass().add("kennebec-secondary-text");
        description.setPadding(new Insets(0, 10, 0, 10));
        getChildren().add(description);

        // Warning
        Label warning = new Label(
            "Warning: enabling everything at once generates extremely large log files "
          + "(hundreds of megabytes per day with P25 traffic). Only turn on the categories "
          + "you are actively debugging.");
        warning.setWrapText(true);
        warning.setStyle("-fx-text-fill: darkred;");
        warning.setPadding(new Insets(0, 10, 0, 10));
        getChildren().add(warning);

        // Master toggle card
        mMasterAllToggle = new CheckBox("Enable ALL diagnostics categories");
        mMasterAllToggle.setOnAction(event -> {
            boolean enable = mMasterAllToggle.isSelected();
            for(DiagnosticsCategory category : DiagnosticsCategory.values())
            {
                CheckBox box = mCategoryCheckBoxes.get(category);
                if(box != null)
                {
                    box.setSelected(enable);
                }
                mDiagnosticsPreference.setEnabled(category, enable);
                LogLevelController.apply(category, enable);
            }
        });

        SettingsCard masterCard = new SettingsCard();
        masterCard.getChildren().add(new SettingsRow("Master Toggle", mMasterAllToggle));
        getChildren().add(masterCard);

        // Category checkboxes card
        SettingsCard categoriesCard = new SettingsCard();
        for(DiagnosticsCategory category : DiagnosticsCategory.values())
        {
            CheckBox box = new CheckBox();
            box.setSelected(mDiagnosticsPreference.isEnabled(category));
            box.selectedProperty().addListener((obs, oldVal, newVal) -> {
                mDiagnosticsPreference.setEnabled(category, newVal);
                LogLevelController.apply(category, newVal);
                syncMasterToggle();
            });
            mCategoryCheckBoxes.put(category, box);

            categoriesCard.getChildren().add(new SettingsRow(category.getDisplayName(), box));
        }
        getChildren().add(categoriesCard);

        // Windows Host Optimization section
        Label optimizeHeader = new Label("Windows Host Optimization");
        optimizeHeader.getStyleClass().add("hig-section-header");
        getChildren().add(optimizeHeader);

        Label optimizeDesc = new Label("Disables USB Selective Suspend, adds Windows Defender exclusions, and disables DPTF throttling to prevent dropped packets.");
        optimizeDesc.setWrapText(true);
        optimizeDesc.getStyleClass().add("kennebec-secondary-text");
        optimizeDesc.setPadding(new Insets(0, 10, 0, 10));
        getChildren().add(optimizeDesc);

        Button checkBtn = new Button("Check Host Settings");
        checkBtn.setTooltip(new Tooltip("Checks the current status of Windows power plans and Defender exclusions."));
        Button applyBtn = new Button("Apply Optimizations (Requires Admin)");
        applyBtn.setTooltip(new Tooltip("Applies optimizations by disabling USB Selective Suspend, adding Defender exclusions, and disabling DPTF throttling. Requires Administrator privileges."));
        
        TextArea outputArea = new TextArea();
        outputArea.setPrefRowCount(8);
        outputArea.setEditable(false);
        outputArea.setPromptText("Click 'Check Host Settings' to view the current status of Windows power plans and Defender exclusions...");
        outputArea.setStyle("-fx-font-family: monospace;");
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
                        success ? javafx.scene.control.Alert.AlertType.INFORMATION : javafx.scene.control.Alert.AlertType.ERROR
                    );
                    alert.setTitle("Windows Host Optimization");
                    alert.setHeaderText(null);
                    alert.setContentText(success ? "Optimizations applied successfully!" : "Failed to apply optimizations. Please ensure you clicked 'Yes' on the Admin prompt.");
                    alert.showAndWait();
                    
                    if (success) {
                        checkBtn.fire(); // Refresh the output
                    }
                });
            });
        });

        SettingsCard optimizeCard = new SettingsCard();
        optimizeCard.getChildren().add(new SettingsRow("Actions", new HBox(10, checkBtn, applyBtn)));
        getChildren().add(optimizeCard);

        SettingsCard outputCard = new SettingsCard();
        outputCard.getChildren().add(outputArea);
        getChildren().add(outputCard);

        syncMasterToggle();
    }

    private void syncMasterToggle()
    {
        if(mMasterAllToggle == null)
        {
            return;
        }
        boolean allOn = true;
        for(DiagnosticsCategory category : DiagnosticsCategory.values())
        {
            if(!mDiagnosticsPreference.isEnabled(category))
            {
                allOn = false;
                break;
            }
        }
        mMasterAllToggle.setSelected(allOn);
    }
}
