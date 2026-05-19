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

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javax.swing.SpinnerNumberModel;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * FC0013 Tuner Editor
 */
public class FC0013TunerEditor extends TunerEditor<RTL2832Tuner,FC0013TunerConfiguration>
{
    private static final long serialVersionUID = 1L;
    private final static Logger mLog = LoggerFactory.getLogger(FC0013TunerEditor.class);

    private FC0013TunerEditorController mController;

    private static final FC0013EmbeddedTuner.LNAGain DEFAULT_LNA_GAIN = FC0013EmbeddedTuner.LNAGain.G00;

    /**
     * Constructs an instance
     * @param userPreferences for wide-band recordings
     * @param tunerManager to save configuration
     * @param discoveredTuner to control
     */
    public FC0013TunerEditor(UserPreferences userPreferences, TunerManager tunerManager, DiscoveredTuner discoveredTuner)
    {
        super(userPreferences, tunerManager, discoveredTuner);
        init();
        tunerStatusUpdated();
    }

    private FC0013EmbeddedTuner getEmbeddedTuner()
    {
        if(hasTuner())
        {
            return (FC0013EmbeddedTuner)getTuner().getController().getEmbeddedTuner();
        }

        return null;
    }

    private String getLogPrefix()
    {
        if (hasTuner() && getEmbeddedTuner() != null) {
            return getEmbeddedTuner().getTunerType().getLabel() + " Tuner Controller - ";
        }
        return "FC0013 Tuner Controller - ";
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

    @Override
    protected void tunerStatusUpdated()
    {
        setLoading(true);

        if(mController != null) {
            Platform.runLater(() -> {
                if(hasTuner())
                {
                    mController.getTunerIdLabel().setText(getTuner().getPreferredName() + getUsbInfo());
                }
                else
                {
                    mController.getTunerIdLabel().setText(getDiscoveredTuner().getName());
                }

                String status = getDiscoveredTuner().getTunerStatus().toString();
                if(getDiscoveredTuner().hasErrorMessage())
                {
                    status += " - " + getDiscoveredTuner().getErrorMessage();
                }
                mController.getTunerStatusLabel().setText(status);

                if(hasTuner())
                {
                    mController.getSampleRateCombo().getSelectionModel().select(getConfiguration().getSampleRate());
                }
                else if(hasConfiguration())
                {
                    mController.getSampleRateCombo().getSelectionModel().select(getConfiguration().getSampleRate());
                }

                mController.getBiasTToggleButton().setDisable(!hasTuner());
                mController.getAgcToggleButton().setDisable(!hasTuner());

                if(hasConfiguration())
                {
                    mController.getBiasTToggleButton().setSelected(getConfiguration().isBiasT());
                    mController.getAgcToggleButton().setSelected(getConfiguration().getAGC());
                    mController.getLnaGainCombo().getSelectionModel().select(getConfiguration().getLnaGain());
                    mController.getLnaGainCombo().setDisable(getConfiguration().getAGC());
                }

                updateSampleRateToolTip();
            });
        }

        getButtonPanel().updateControls();
        getFrequencyPanel().updateControls();

        setLoading(false);
    }

    private void init()
    {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/source/tuner/rtl/fc0013/FC0013TunerEditor.fxml"));
                Parent root = loader.load();
                mController = loader.getController();
                mController.setEditor(this);

                mController.getButtonPanelContainer().getChildren().add(getButtonPanel());
                mController.getFrequencyPanelContainer().getChildren().add(getFrequencyPanel());

                mController.getSampleRateCombo().getItems().addAll(SampleRate.values());
                mController.getSampleRateCombo().setDisable(true);
                mController.getSampleRateCombo().setOnAction(e -> {
                    if(!isLoading()) {
                        SampleRate sampleRate = mController.getSampleRateCombo().getSelectionModel().getSelectedItem();
                        try {
                            getTuner().getController().setSampleRate(sampleRate);
                            adjustForSampleRate(sampleRate.getRate());
                            save();
                        } catch(SourceException | LibUsbException eSampleRate) {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setContentText(String.valueOf(getLogPrefix() + "couldn't apply the sample rate setting [" + sampleRate.getLabel() + "] " + eSampleRate.getLocalizedMessage()));
                            alert.showAndWait();
                            mLog.error(getLogPrefix() + "couldn't apply sample rate setting [" + sampleRate.getLabel() + "]", eSampleRate);
                        }
                    }
                });

                mController.getLnaGainCombo().getItems().addAll(FC0013EmbeddedTuner.LNAGain.values());
                mController.getLnaGainCombo().setDisable(true);
                mController.getLnaGainCombo().setOnAction(e -> {
                    if(!isLoading()) {
                        try {
                            FC0013EmbeddedTuner.LNAGain lnaGain = mController.getLnaGainCombo().getSelectionModel().getSelectedItem();
                            if(lnaGain == null) {
                                lnaGain = DEFAULT_LNA_GAIN;
                            }
                            if(!mController.getLnaGainCombo().isDisabled() && getEmbeddedTuner() != null) {
                                getEmbeddedTuner().setGain(mController.getAgcToggleButton().isSelected(), lnaGain);
                            }
                            save();
                        } catch(Exception ex) {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setContentText(String.valueOf(getLogPrefix() + "couldn't apply the LNA gain setting - " + ex.getLocalizedMessage()));
                            alert.showAndWait();
                            mLog.error(getLogPrefix() + "couldn't apply LNA gain setting - ", ex);
                        }
                    }
                });

                mController.getBiasTToggleButton().setDisable(true);
                mController.getBiasTToggleButton().setOnAction(e -> {
                    if(!isLoading()) {
                        try {
                            getTuner().getController().setBiasT(mController.getBiasTToggleButton().isSelected());
                            save();
                        } catch (Exception ex) {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setContentText(String.valueOf(getLogPrefix() + "couldn't set bias-t" + ex.getLocalizedMessage()));
                            alert.showAndWait();
                            mLog.error(getLogPrefix() + "couldn't set bias-t", ex);
                        }
                    }
                });

                mController.getAgcToggleButton().setDisable(true);
                mController.getAgcToggleButton().setOnAction(e -> {
                    if(!isLoading()) {
                        try {
                            boolean agc = mController.getAgcToggleButton().isSelected();
                            FC0013EmbeddedTuner.LNAGain lnaGain = mController.getLnaGainCombo().getSelectionModel().getSelectedItem();
                            if (getEmbeddedTuner() != null) {
                                getEmbeddedTuner().setGain(agc, lnaGain);
                            }
                            mController.getLnaGainCombo().setDisable(agc);
                            save();
                        } catch(Exception ex) {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setContentText(String.valueOf(getLogPrefix() + "couldn't set AGC" + ex.getLocalizedMessage()));
                            alert.showAndWait();
                            mLog.error(getLogPrefix() + "couldn't set AGC", ex);
                        }
                    }
                });

                mController.getChangeSerialButton().setOnAction(e -> {
                    if (!hasTuner()) return;
                    TextInputDialog dialog = new TextInputDialog();
                    dialog.setTitle("Change RTL-SDR Serial Number");
                    dialog.setHeaderText("Enter new Serial Number (Alphanumeric only, max 16 chars):\n\nWARNING: Writing to hardware memory is inherently risky.\nDo not disconnect the device during the write process.");
                    Optional<String> result = dialog.showAndWait();
                    String newSerial = result.orElse(null);

                    if (newSerial != null) {
                        newSerial = newSerial.trim();
                        if (!newSerial.matches("[A-Za-z0-9]*") || newSerial.length() > 16) {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setContentText("Invalid serial number. Must be alphanumeric and max 16 characters.");
                            alert.showAndWait();
                            return;
                        }

                        final String serialToSet = newSerial;
                        ExecutorService executor = Executors.newSingleThreadExecutor();
                        executor.submit(() -> {
                            try {
                                ((io.github.dsheirer.source.tuner.rtl.RTL2832TunerController)getTuner().getTunerController()).setSerialNumber(serialToSet);
                                Platform.runLater(() -> {
                                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                    alert.setContentText("Serial number updated successfully.\nPlease disconnect and reconnect the tuner.");
                                    alert.showAndWait();
                                });
                            } catch (Exception ex) {
                                Platform.runLater(() -> {
                                    Alert alert = new Alert(Alert.AlertType.ERROR);
                                    alert.setContentText("Failed to update serial number: " + ex.getMessage());
                                    alert.showAndWait();
                                });
                            }
                        });
                    }
                });

                this.getChildren().add(root);
            } catch (Exception e) {
                mLog.error("Error loading FC0013TunerEditor FXML", e);
            }
        });
    }

    private void updateSampleRateToolTip()
    {
        if(mController != null) {
            if(hasTuner() && getTuner().getTunerController().isLockedSampleRate())
            {
                mController.getSampleRateCombo().setTooltip(new javafx.scene.control.Tooltip("Sample Rate is locked.  Disable decoding channels to unlock."));
            }
            else if(hasTuner())
            {
                mController.getSampleRateCombo().setTooltip(new javafx.scene.control.Tooltip("Select a sample rate for the tuner"));
            }
            else
            {
                mController.getSampleRateCombo().setTooltip(new javafx.scene.control.Tooltip("No tuner available"));
            }
        }
    }

    @Override
    public void setTunerLockState(boolean locked)
    {
        getFrequencyPanel().updateControls();
        if(mController != null) {
            Platform.runLater(() -> {
                mController.getSampleRateCombo().setDisable(locked);
                updateSampleRateToolTip();
            });
        }
    }

    protected String getTunerInfo()
    {
        StringBuilder sb = new StringBuilder();
        RTL2832TunerController.Descriptor descriptor = getTuner().getController().getDescriptor();

        if (getEmbeddedTuner() != null) {
            sb.append("<html><h3>RTL-2832 with ").append(getEmbeddedTuner().getTunerType().getLabel()).append(" Tuner</h3>");
        } else {
            sb.append("<html><h3>RTL-2832 Tuner</h3>");
        }

        if(descriptor == null)
        {
            sb.append("No EEPROM Descriptor Available");
        }
        else
        {
            sb.append("<b>USB ID: </b>").append(descriptor.getVendorID()).append(":").append(descriptor.getProductID()).append("<br>");
            sb.append("<b>Vendor: </b>").append(descriptor.getVendorLabel()).append("<br>");
            sb.append("<b>Product: </b>").append(descriptor.getProductLabel()).append("<br>");
            sb.append("<b>Serial: </b>").append(descriptor.getSerial()).append("<br>");
            sb.append("<b>IR Enabled: </b>").append(descriptor.irEnabled()).append("<br>");
            sb.append("<b>Remote Wake: </b>").append(descriptor.remoteWakeupEnabled()).append("<br>");
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
            double value = ((SpinnerNumberModel)getFrequencyCorrectionSpinner().getModel()).getNumber().doubleValue();
            config.setFrequencyCorrection(value);
            config.setAutoPPMCorrectionEnabled(getAutoPPMCheckBox().isSelected());

            if(mController != null) {
                Platform.runLater(() -> {
                    config.setSampleRate(mController.getSampleRateCombo().getSelectionModel().getSelectedItem());
                    config.setBiasT(mController.getBiasTToggleButton().isSelected());
                    config.setAGC(mController.getAgcToggleButton().isSelected());
                    FC0013EmbeddedTuner.LNAGain lnaGain = mController.getLnaGainCombo().getSelectionModel().getSelectedItem();
                    config.setLnaGain(lnaGain);
                    saveConfiguration();
                });
            } else {
                saveConfiguration();
            }
        }
    }
}
