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
