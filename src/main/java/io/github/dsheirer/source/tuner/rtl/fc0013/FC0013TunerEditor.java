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
package io.github.dsheirer.source.tuner.rtl.fc0013;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;
import javafx.scene.control.ProgressBar;


import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.rtl.RTL2832Tuner;
import io.github.dsheirer.source.tuner.rtl.RTL2832TunerController;
import io.github.dsheirer.source.tuner.rtl.RTL2832TunerController.SampleRate;
import io.github.dsheirer.source.tuner.ui.TunerEditor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.LibUsbException;

import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.application.Platform;
import java.util.Optional;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;


/**
 * FC0013 Tuner Editor
 */
public class FC0013TunerEditor extends TunerEditor<RTL2832Tuner, FC0013TunerConfiguration>
{
    private final static Logger mLog = LoggerFactory.getLogger(FC0013TunerEditor.class);
    private static final long serialVersionUID = 1L;
    private static final FC0013EmbeddedTuner.LNAGain DEFAULT_LNA_GAIN = FC0013EmbeddedTuner.LNAGain.G14;
    private ToggleButton mBiasTButton;
    private ComboBox<SampleRate> mSampleRateCombo;

    private ToggleButton mAgcToggleButton;
    private ComboBox<FC0013EmbeddedTuner.LNAGain> mLNAGainCombo;

    /**
     * Constructs an instance
     * @param userPreferences for wide-band recordings
     * @param tunerManager for saving configurations
     * @param discoveredTuner to edit
     */
    public FC0013TunerEditor(UserPreferences userPreferences, TunerManager tunerManager, DiscoveredTuner discoveredTuner)
    {
        super(userPreferences, tunerManager, discoveredTuner);
        init();
        tunerStatusUpdated();
    }

    @Override
    public long getMinimumTunableFrequency()
    {
        return FC0013EmbeddedTuner.MINIMUM_TUNABLE_FREQUENCY_HZ;
    }

    @Override
    public long getMaximumTunableFrequency()
    {
        return FC0013EmbeddedTuner.MAXIMUM_TUNABLE_FREQUENCY_HZ;
    }

    /**
     * Access the FC0013 embedded tuner
     * @return tuner if there is a tuner, or null otherwise
     */
    private FC0013EmbeddedTuner getEmbeddedTuner()
    {
        if(hasTuner())
        {
            return (FC0013EmbeddedTuner) getTuner().getController().getEmbeddedTuner();
        }

        return null;
    }

    private String getLogPrefix()
    {
        return getEmbeddedTuner().getTunerType().getLabel() + " Tuner Controller - ";
    }

    private void init()
    {
        // setLayout(new javafx.scene.layout.HBox(4));

        getChildren().add(new Label("Tuner:"));
        getChildren().add(getTunerIdLabel());


        getChildren().add(new Label("Status:"));
        getChildren().add(getTunerStatusLabel());
        getChildren().add(getBiasTButton());

        getChildren().add(getButtonPanel());

        getChildren().add(new Separator());

        getChildren().add(new Label("Frequency (MHz):"));
        getChildren().add(getFrequencyPanel());

        getChildren().add(new Label("Sample Rate:"));
        getChildren().add(getSampleRateCombo());

        getChildren().add(new Separator());

        VBox gainPanel = new VBox();
        gainPanel.getChildren().add(new Label("Gain"));
        gainPanel.getChildren().add(getAgcToggleButton());
        gainPanel.getChildren().add(new Label("LNA:"));
        getChildren().add(gainPanel);
        getChildren().add(getLNAGainCombo());
    }

    @Override
    protected void tunerStatusUpdated()
    {
        setLoading(true);

        if(hasTuner())
        {
            getTunerIdLabel().setText(getTuner().getPreferredName() + getUsbInfo());
        }
        else
        {
            getTunerIdLabel().setText(getDiscoveredTuner().getName());
        }

        String status = getDiscoveredTuner().getTunerStatus().toString();
        if(getDiscoveredTuner().hasErrorMessage())
        {
            status += " - " + getDiscoveredTuner().getErrorMessage();
        }
        getTunerStatusLabel().setText(status);
        getButtonPanel().updateControls();
        getFrequencyPanel().updateControls();

        if(hasTuner())
        {
            getBiasTButton().setDisable(!(true));
            getBiasTButton().setSelected(getConfiguration().isBiasT());
            getSampleRateCombo().setDisable(!(true));
            getSampleRateCombo().setValue(getConfiguration().getSampleRate());
            getAgcToggleButton().setDisable(!(true));
            getAgcToggleButton().setSelected(getConfiguration().getAGC());
            getLNAGainCombo().setDisable(!getConfiguration().getAGC());
            getLNAGainCombo().setValue(getConfiguration().getLnaGain());
        }
        else
        {
            getBiasTButton().setDisable(!(false));
            getBiasTButton().setSelected(false);
            getSampleRateCombo().setDisable(!(false));
            getAgcToggleButton().setDisable(!(false));
            getLNAGainCombo().setDisable(!(false));
        }

        updateSampleRateToolTip();

        setLoading(false);
    }

    /**
     * Bias-T toggle button
     * @return bias-t button
     */
    private ToggleButton getBiasTButton()
    {
        if(mBiasTButton == null)
        {
            mBiasTButton = new ToggleButton("Bias-T");
            mBiasTButton.setDisable(!(false));
            mBiasTButton.setOnAction(e -> {
                if(!isLoading())
                {
                    getTuner().getController().setBiasT(mBiasTButton.isSelected());
                    save();
               }
            });
        }

        return mBiasTButton;
    }

    /**
     * Hyperlink button that provides tuner information
     */
private ComboBox getLNAGainCombo()
    {
        if(mLNAGainCombo == null)
        {
            mLNAGainCombo = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(FC0013EmbeddedTuner.LNAGain.values())))));
            mLNAGainCombo.setDisable(!(false));
            mLNAGainCombo.setOnAction(arg0 ->
            {
                if(!isLoading())
                {
                    try
                    {
                        FC0013EmbeddedTuner.LNAGain lnaGain = (FC0013EmbeddedTuner.LNAGain) mLNAGainCombo.getValue();

                        if(lnaGain == null)
                        {
                            lnaGain = DEFAULT_LNA_GAIN;
                        }

                        if(!mLNAGainCombo.isDisabled())
                        {
                            getEmbeddedTuner().setGain(getAgcToggleButton().isSelected(), lnaGain);
                        }

                        save();
                    }
                    catch(Exception e)
                    {
                        Platform.runLater(() -> { Alert alert = new Alert(Alert.AlertType.INFORMATION); io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane()); alert.setContentText(String.valueOf(getLogPrefix() +
                                "couldn't apply the LNA gain setting - " + e.getLocalizedMessage())); alert.showAndWait(); });
                        mLog.error(getLogPrefix() + "couldn't apply LNA gain setting - ", e);
                    }
                }
            });
            mLNAGainCombo.setTooltip(new javafx.scene.control.Tooltip("<html>LNA Gain.  Set master gain to <b>MANUAL</b> to enable adjustment</html>"));
        }

        return mLNAGainCombo;
    }

    private ComboBox getSampleRateCombo()
    {
        if(mSampleRateCombo == null)
        {
            mSampleRateCombo = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(SampleRate.values())))));
            mSampleRateCombo.setDisable(!(false));
            mSampleRateCombo.setOnAction(e ->
            {
                if(!isLoading())
                {
                    SampleRate sampleRate = (SampleRate) mSampleRateCombo.getValue();

                    try
                    {
                        getTuner().getController().setSampleRate(sampleRate);
                        //Adjust the min/max values for the sample rate.
                        adjustForSampleRate(sampleRate.getRate());
                        save();
                    }
                    catch(SourceException | LibUsbException eSampleRate)
                    {
                        Platform.runLater(() -> { Alert alert = new Alert(Alert.AlertType.INFORMATION); io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane()); alert.setContentText(String.valueOf(getLogPrefix() + "couldn't apply the sample rate setting [" +
                                        sampleRate.getLabel() + "] " + eSampleRate.getLocalizedMessage())); alert.showAndWait(); });

                        mLog.error(getLogPrefix() + "couldn't apply sample rate setting [" + sampleRate.getLabel() +
                                "]", eSampleRate);
                    }
                }
            });
        }

        return mSampleRateCombo;
    }

    private ToggleButton getAgcToggleButton()
    {
        if(mAgcToggleButton == null)
        {
            mAgcToggleButton = new ToggleButton("AGC");
            mAgcToggleButton.setDisable(!(false));
            mAgcToggleButton.setOnAction(arg0 ->
            {
                if(!isLoading())
                {
                    try
                    {
                        boolean agc = getAgcToggleButton().isSelected();
                        FC0013EmbeddedTuner.LNAGain lnaGain = (FC0013EmbeddedTuner.LNAGain)getLNAGainCombo().getValue();
                        getEmbeddedTuner().setGain(agc, lnaGain);
                        getLNAGainCombo().setDisable(!(!agc));
                        save();
                    }
                    catch(Exception e)
                    {
                        Platform.runLater(() -> { Alert alert = new Alert(Alert.AlertType.INFORMATION); io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane()); alert.setContentText(String.valueOf(getLogPrefix() +
                                "couldn't set AGC" + e.getLocalizedMessage())); alert.showAndWait(); });
                        mLog.error(getLogPrefix() + "couldn't set AGC", e);
                    }
                }
            });
            mAgcToggleButton.setTooltip(new javafx.scene.control.Tooltip("<html>Automatic Gain Control (AGC). </html>"));
        }

        return mAgcToggleButton;
    }

    /**
     * Updates the sample rate tooltip according to the tuner controller's lock state.
     */
    private void updateSampleRateToolTip()
    {
        if(hasTuner() && getTuner().getTunerController().isLockedSampleRate())
        {
            getSampleRateCombo().setTooltip(new javafx.scene.control.Tooltip("Sample Rate is locked.  Disable decoding channels to unlock."));
        }
        else if(hasTuner())
        {
            getSampleRateCombo().setTooltip(new javafx.scene.control.Tooltip("Select a sample rate for the tuner"));
        }
        else
        {
            getSampleRateCombo().setTooltip(new javafx.scene.control.Tooltip("No tuner available"));
        }
    }

    @Override
    public void setTunerLockState(boolean locked)
    {
        getFrequencyPanel().updateControls();
        getSampleRateCombo().setDisable(!(!locked));
        updateSampleRateToolTip();
    }

    protected String getTunerInfo()
    {
        StringBuilder sb = new StringBuilder();
        RTL2832TunerController.Descriptor descriptor = getTuner().getController().getDescriptor();
        sb.append("<html><h3>RTL-2832 with " + getEmbeddedTuner().getTunerType().getLabel() + " Tuner</h3>");

        if(descriptor == null)
        {
            sb.append("No EEPROM Descriptor Available");
        }
        else
        {
            sb.append("<b>USB ID: </b>");
            sb.append(descriptor.getVendorID());
            sb.append(":");
            sb.append(descriptor.getProductID());
            sb.append("<br>");

            sb.append("<b>Vendor: </b>");
            sb.append(descriptor.getVendorLabel());
            sb.append("<br>");

            sb.append("<b>Product: </b>");
            sb.append(descriptor.getProductLabel());
            sb.append("<br>");

            sb.append("<b>Serial: </b>");
            sb.append(descriptor.getSerial());
            sb.append("<br>");

            sb.append("<b>IR Enabled: </b>");
            sb.append(descriptor.irEnabled());
            sb.append("<br>");

            sb.append("<b>Remote Wake: </b>");
            sb.append(descriptor.remoteWakeupEnabled());
            sb.append("<br>");
        }

        return sb.toString();
    }

    @Override
    public void save()
    {
        if(hasConfiguration() && !isLoading())
        {
            FC0013TunerConfiguration config = getConfiguration();
            config.setBiasT(getTuner().getController().isBiasT());
            config.setFrequency(getFrequencyControl().getFrequency());
            getConfiguration().setMinimumFrequency(getMinimumFrequencyTextField().getFrequency());
            getConfiguration().setMaximumFrequency(getMaximumFrequencyTextField().getFrequency());
            double value = (Double)getFrequencyCorrectionSpinner().getValue();
            config.setFrequencyCorrection(value);
            config.setAutoPPMCorrectionEnabled(getAutoPPMCheckBox().isSelected());

            config.setSampleRate((SampleRate)getSampleRateCombo().getValue());
            config.setAGC(getAgcToggleButton().isSelected());
            FC0013EmbeddedTuner.LNAGain lnaGain = (FC0013EmbeddedTuner.LNAGain)getLNAGainCombo().getValue();
            config.setLnaGain(lnaGain);
            saveConfiguration();
        }
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