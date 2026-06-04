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

package io.github.dsheirer.source.tuner.rtl;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;



import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.application.Platform;
import java.util.Optional;


import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.rtl.r8x.R8xEmbeddedTuner;
import io.github.dsheirer.source.tuner.ui.TunerEditor;


import javafx.scene.control.Label;
import javafx.scene.control.Separator;

/**
 * Tuner editor for RTL2832 tuner that has not been started, or for an unknown tuner type
 */
public class RTL2832UnknownTunerEditor extends TunerEditor<RTL2832Tuner, RTL2832TunerConfiguration>
{
    /**
     * Constructs an instance
     *
     * @param userPreferences for starting wide-band recorders
     * @param tunerManager for requesting configuration saves.
     * @param discoveredTuner that is not started, or that doesn't have a recognized tuner type
     */
    public RTL2832UnknownTunerEditor(UserPreferences userPreferences, TunerManager tunerManager,
                                     DiscoveredTuner discoveredTuner)
    {
        super(userPreferences, tunerManager, discoveredTuner);
        init();
        tunerStatusUpdated();
    }

    private void init()
    {
        // setLayout(new javafx.scene.layout.HBox(4));

        getChildren().add(new Label("Tuner:"));
        getChildren().add(getTunerIdLabel());

        getChildren().add(new Label("Status:"));
        getChildren().add(getTunerStatusLabel());

        getChildren().add(getButtonPanel());

        getChildren().add(new Separator());

    }

    @Override
    public long getMinimumTunableFrequency()
    {
        //Bogus value.
        return R8xEmbeddedTuner.MINIMUM_TUNABLE_FREQUENCY_HZ;
    }

    @Override
    public long getMaximumTunableFrequency()
    {
        //Bogus value.
        return R8xEmbeddedTuner.MAXIMUM_TUNABLE_FREQUENCY_HZ;
    }

    @Override
    protected void save()
    {
        //No-op
    }

    @Override
    protected void tunerStatusUpdated()
    {
        setLoading(true);
        getTunerIdLabel().setText(getDiscoveredTuner().getName() + (hasTuner() ? " ID:" + getTuner().getUniqueID() : "") + getUsbInfo());

        String status = getDiscoveredTuner().getTunerStatus().toString();
        if(getDiscoveredTuner().hasErrorMessage())
        {
            status += " - " + getDiscoveredTuner().getErrorMessage();
        }
        getTunerStatusLabel().setText(status);
        getButtonPanel().updateControls();
        setLoading(false);
    }

    @Override
    public void setTunerLockState(boolean locked)
    {
        getFrequencyPanel().updateControls();
    }

    private Button getChangeSerialButton() {
        Button btn = new Button("Change Serial Number");
        btn.setOnAction(e -> {
            if (!hasTuner()) return;
TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Change RTL-SDR Serial Number");
            dialog.setHeaderText("Enter new Serial Number (Alphanumeric only, max 16 chars):\n\nWARNING: Writing to hardware memory is inherently risky.\nDo not disconnect the device during the write process.");
            Optional<String> result = dialog.showAndWait();
            String newSerial = result.orElse(null);

            if (newSerial != null) {
                newSerial = newSerial.trim();
                if (!newSerial.matches("[A-Za-z0-9]*") || newSerial.length() > 16) {
                    Platform.runLater(() -> { Alert alert = new Alert(Alert.AlertType.ERROR); io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane()); alert.setContentText(String.valueOf("Invalid serial number. Must be alphanumeric and max 16 characters.")); alert.showAndWait(); });
                    return;
                }

                final String serialToSet = newSerial;
                javafx.scene.control.ProgressBar progressMonitor = new javafx.scene.control.ProgressBar();
                // progressMonitor.setMillisToDecideToPopup...
                // progressMonitor.setMillisToPopup...
                progressMonitor.setProgress(10);

                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(() -> {
                    try {
                        ((io.github.dsheirer.source.tuner.rtl.RTL2832TunerController)getTuner().getTunerController()).setSerialNumber(serialToSet);
                        javafx.application.Platform.runLater(() -> {
                            progressMonitor.setProgress(100);
                            Platform.runLater(() -> { Alert alert = new Alert(Alert.AlertType.INFORMATION); io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane()); alert.setContentText(String.valueOf("Serial number updated successfully.\nPlease disconnect and reconnect the tuner.")); alert.showAndWait(); });
                        });
                    } catch (Exception ex) {
                        javafx.application.Platform.runLater(() -> {
                            // progressMonitor.close();
                            Platform.runLater(() -> { Alert alert = new Alert(Alert.AlertType.ERROR); io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane()); alert.setContentText(String.valueOf("Failed to update serial number: " + ex.getMessage())); alert.showAndWait(); });
                        });
                    }
                });
            }
        });
        return btn;
    }

}