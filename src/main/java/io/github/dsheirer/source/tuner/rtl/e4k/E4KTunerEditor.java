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
package io.github.dsheirer.source.tuner.rtl.e4k;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;
import javafx.scene.control.ProgressBar;




import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.application.Platform;
import java.util.Optional;


import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.rtl.RTL2832Tuner;
import io.github.dsheirer.source.tuner.rtl.RTL2832TunerController;
import io.github.dsheirer.source.tuner.rtl.RTL2832TunerController.SampleRate;
import io.github.dsheirer.source.tuner.rtl.e4k.E4KEmbeddedTuner.E4KGain;
import io.github.dsheirer.source.tuner.rtl.e4k.E4KEmbeddedTuner.E4KLNAGain;
import io.github.dsheirer.source.tuner.rtl.e4k.E4KEmbeddedTuner.E4KMixerGain;
import io.github.dsheirer.source.tuner.ui.TunerEditor;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;

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
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;

import javax.usb.UsbException;

/**
 * E4000 tuner editor
 */
public class E4KTunerEditor extends TunerEditor<RTL2832Tuner, E4KTunerConfiguration>
{
    private final static Logger mLog = LoggerFactory.getLogger(E4KTunerEditor.class);
    private static final long serialVersionUID = 1L;
    private ToggleButton mBiasTButton;
    private ComboBox<SampleRate> mSampleRateCombo;
    private ComboBox<E4KGain> mMasterGainCombo;
    private ComboBox<E4KMixerGain> mMixerGainCombo;
    private ComboBox<E4KLNAGain> mLNAGainCombo;
    private ComboBox<E4KEmbeddedTuner.IFGain> mIfGainCombo;

    /**
     * Constructs an instance
     * @param userPreferences for wide-band recordings
     * @param tunerManager to save configuration
     * @param discoveredTuner to control
     */
    public E4KTunerEditor(UserPreferences userPreferences, TunerManager tunerManager, DiscoveredTuner discoveredTuner)
    {
        super(userPreferences, tunerManager, discoveredTuner);
        init();
        tunerStatusUpdated();
    }

    @Override
    public long getMinimumTunableFrequency()
    {
        return E4KEmbeddedTuner.MINIMUM_TUNABLE_FREQUENCY_HZ;
    }

    @Override
    public long getMaximumTunableFrequency()
    {
        return E4KEmbeddedTuner.MAXIMUM_TUNABLE_FREQUENCY_HZ;
    }

    /**
     * Access to the E4000 embedded tuner
     * @return E4000 tuner if there is a tuner, or null otherwise
     */
    public E4KEmbeddedTuner getEmbeddedTuner()
    {
        if(hasTuner())
        {
            return (E4KEmbeddedTuner)getTuner().getController().getEmbeddedTuner();
        }

        return null;
    }

    private void init()
    {
        setSpacing(8);
        setPadding(new Insets(10));

        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(10);
        infoGrid.setVgap(4);
        infoGrid.add(new Label("Tuner:"), 0, 0);
        infoGrid.add(getTunerIdLabel(), 1, 0);
        infoGrid.add(new Label("Status:"), 0, 1);
        infoGrid.add(getTunerStatusLabel(), 1, 1);
        getChildren().add(infoGrid);

        getChildren().add(getBiasTButton());

        getChildren().add(getButtonPanel());

        getChildren().add(new Separator());

        GridPane freqGrid = new GridPane();
        freqGrid.setHgap(10);
        freqGrid.setVgap(4);
        freqGrid.add(new Label("Frequency (MHz):"), 0, 0);
        freqGrid.add(getFrequencyPanel(), 1, 0);
        freqGrid.add(new Label("Sample Rate:"), 0, 1);
        freqGrid.add(getSampleRateCombo(), 1, 1);
        getChildren().add(freqGrid);

        getChildren().add(new Separator());
        getChildren().add(new Label("Mixer/LNA Gain Control"));

        GridPane gainGrid = new GridPane();
        gainGrid.setHgap(10);
        gainGrid.setVgap(4);
        gainGrid.add(new Label("Master:"), 0, 0);
        gainGrid.add(getMasterGainCombo(), 1, 0);
        gainGrid.add(new Label("Mixer:"), 0, 1);
        gainGrid.add(getMixerGainCombo(), 1, 1);
        gainGrid.add(new Label("LNA:"), 0, 2);
        gainGrid.add(getLNAGainCombo(), 1, 2);
        getChildren().add(gainGrid);

        getChildren().add(new Separator());

        GridPane ifGrid = new GridPane();
        ifGrid.setHgap(10);
        ifGrid.setVgap(4);
        ifGrid.add(new Label("IF Gain:"), 0, 0);
        ifGrid.add(getIfGainCombo(), 1, 0);
        getChildren().add(ifGrid);
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
        getTunerStatusLabel().setText(getDiscoveredTuner().getTunerStatus().toString());
        getButtonPanel().updateControls();
        getFrequencyPanel().updateControls();

        if(hasTuner())
        {
            getBiasTButton().setDisable(!(true));
            getBiasTButton().setSelected(getConfiguration().isBiasT());
            getSampleRateCombo().setDisable(!(true));
            getSampleRateCombo().setValue(getConfiguration().getSampleRate());
            getMasterGainCombo().setDisable(!(true));
            getIfGainCombo().setDisable(!(true));
            getIfGainCombo().setValue(getConfiguration().getIFGain());

            E4KGain gain = getConfiguration().getMasterGain();
            getMasterGainCombo().setDisable(!(true));
            getMasterGainCombo().setValue(gain);

            if(gain == E4KGain.MANUAL)
            {
                getMixerGainCombo().setValue(getConfiguration().getMixerGain());
                getMixerGainCombo().setDisable(!(true));

                getLNAGainCombo().setValue(getConfiguration().getLNAGain());
                getLNAGainCombo().setDisable(!(true));
            }
            else
            {
                getMixerGainCombo().setDisable(!(false));
                getMixerGainCombo().setValue(gain.getMixerGain());

                getLNAGainCombo().setDisable(!(false));
                getLNAGainCombo().setValue(gain.getLNAGain());
            }
        }
        else
        {
            getBiasTButton().setDisable(!(false));
            getBiasTButton().setSelected(false);
            getSampleRateCombo().setDisable(!(false));
            getMasterGainCombo().setDisable(!(false));
            getMixerGainCombo().setDisable(!(false));
            getLNAGainCombo().setDisable(!(false));
            getIfGainCombo().setDisable(!(false));
        }

        updateSampleRateToolTip();

        setLoading(false);
    }

    /**
     * Bias-T toggle button
     * @return
     */
    private ToggleButton getBiasTButton()
    {
        if(mBiasTButton == null)
        {
            mBiasTButton = new ToggleButton("Bias-T");
            mBiasTButton.setDisable(!(false));
            mBiasTButton.setOnAction(e -> {
                if(hasTuner() && !isLoading())
                {
                    boolean biasT = mBiasTButton.isSelected();
                    save();
                    applyDeviceControl("e4k-bias-t", () -> getTuner().getController().setBiasT(biasT),
                            "E4000 Tuner Controller - couldn't set Bias-T");
                }
            });
        }

        return mBiasTButton;
    }

    private ComboBox getIfGainCombo()
    {
        if(mIfGainCombo == null)
        {
            mIfGainCombo = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(E4KEmbeddedTuner.IFGain.values())))));
            mIfGainCombo.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent e)
                {
                    if(hasTuner() && !isLoading())
                    {
                        E4KEmbeddedTuner.IFGain selected = (E4KEmbeddedTuner.IFGain) getIfGainCombo().getValue();
                        save();
                        applyDeviceControl("e4k-if-gain", () -> getEmbeddedTuner().setIFGain(selected, true),
                                "E4000 Tuner Controller - couldn't apply IF gain setting");
                    }
                }
            });
            mIfGainCombo.setTooltip(new javafx.scene.control.Tooltip("Linear IF Gain"));
            mIfGainCombo.setDisable(!(true));
        }

        return mIfGainCombo;
    }

    private ComboBox getLNAGainCombo()
    {
        if(mLNAGainCombo == null)
        {
            mLNAGainCombo = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(E4KLNAGain.values())))));
            mLNAGainCombo.setOnAction(arg0 ->
            {
                if(hasTuner() && !isLoading())
                {
                    E4KLNAGain lnaGain = (E4KLNAGain) mLNAGainCombo.getValue();
                    save();
                    applyDeviceControl("e4k-lna-gain", () -> {
                        try { getEmbeddedTuner().setLNAGain(lnaGain, true); }
                        catch(Exception ex) { throw new RuntimeException(ex); }
                    }, "E4000 Tuner Controller - couldn't apply LNA gain setting");
                }
            });
            mLNAGainCombo.setTooltip(new javafx.scene.control.Tooltip("<html>LNA Gain.  Set master gain to <b>MANUAL</b> to enable adjustment</html>"));
            mLNAGainCombo.setDisable(!(false));
        }

        return mLNAGainCombo;
    }

    private ComboBox getMixerGainCombo()
    {
        if(mMixerGainCombo == null)
        {
            mMixerGainCombo = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(E4KMixerGain.values())))));
            mMixerGainCombo.setOnAction(arg0 ->
            {
                if(hasTuner() && !isLoading())
                {
                    E4KMixerGain mixerGain = (E4KMixerGain) mMixerGainCombo.getValue();
                    save();
                    applyDeviceControl("e4k-mixer-gain", () -> {
                        try { getEmbeddedTuner().setMixerGain(mixerGain, true); }
                        catch(Exception ex) { throw new RuntimeException(ex); }
                    }, "E4000 Tuner Controller - couldn't apply mixer gain setting");
                }
            });
            mMixerGainCombo.setTooltip(new javafx.scene.control.Tooltip("<html>Mixer Gain.  Set master gain to <b>MASTER</b> to enable adjustment</html>"));
            mMixerGainCombo.setDisable(!(false));
        }

        return mMixerGainCombo;
    }

    private ComboBox getMasterGainCombo()
    {
        if(mMasterGainCombo == null)
        {
            mMasterGainCombo = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(E4KGain.values())))));
            mMasterGainCombo.setDisable(!(false));
            mMasterGainCombo.setOnAction(arg0 ->
            {
                if(hasTuner() && !isLoading())
                {
                    E4KGain gain = (E4KGain) mMasterGainCombo.getValue();

                    if(gain == E4KGain.MANUAL)
                    {
                        try {
                            getMixerGainCombo().setValue(getEmbeddedTuner().getMixerGain(true));
                            getMixerGainCombo().setDisable(!(true));
                            getLNAGainCombo().setValue(getEmbeddedTuner().getLNAGain(true));
                            getLNAGainCombo().setDisable(!(true));
                        } catch (javax.usb.UsbException e) {
                            mLog.error("Error getting gain from tuner", e);
                        }
                    }
                    else
                    {
                        getMixerGainCombo().setDisable(!(false));
                        getMixerGainCombo().setValue(gain.getMixerGain());
                        getLNAGainCombo().setDisable(!(false));
                        getLNAGainCombo().setValue(gain.getLNAGain());
                    }

                    save();
                    applyDeviceControl("e4k-master-gain", () -> {
                        try { getEmbeddedTuner().setGain(gain, true); }
                        catch(Exception ex) { throw new RuntimeException(ex); }
                    }, "E4000 Tuner Controller - couldn't apply gain setting");
                }
            });

            mMasterGainCombo.setTooltip(new javafx.scene.control.Tooltip("<html>Select <b>AUTOMATIC</b> for auto gain, <b>MANUAL</b> to enable<br> " +
                    "independent control of <i>Mixer</i>, <i>LNA</i> and <i>Enhance</i> gain<br>settings, or one of the " +
                    "individual gain settings for<br>semi-manual gain control</html>"));
        }

        return mMasterGainCombo;
    }

    private ComboBox getSampleRateCombo()
    {
        if(mSampleRateCombo == null)
        {
            mSampleRateCombo = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(SampleRate.values())))));
            mSampleRateCombo.setDisable(!(false));
            mSampleRateCombo.setOnAction(e ->
            {
                if(hasTuner() && !isLoading())
                {
                    SampleRate sampleRate = (SampleRate) mSampleRateCombo.getValue();
                    //Adjust the min/max values for the sample rate.
                    adjustForSampleRate(sampleRate.getRate());
                    save();
                    applyDeviceControl("e4k-sample-rate", () -> {
                        try { getTuner().getController().setSampleRate(sampleRate); }
                        catch(Exception ex) { throw new RuntimeException(ex); }
                    }, "E4000 Tuner Controller - couldn't apply sample rate setting [" + sampleRate.getLabel() + "]");
                }
            });
        }

        return mSampleRateCombo;
    }

/**
     * Updates the sample rate tooltip according to the tuner controller's lock state.
     */
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

        sb.append("<html><h3>RTL-2832 with E4000 Tuner</h3>");

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
            E4KTunerConfiguration config = getConfiguration();
            config.setFrequency(getFrequencyControl().getFrequency());
            getConfiguration().setMinimumFrequency(getMinimumFrequencyTextField().getFrequency());
            getConfiguration().setMaximumFrequency(getMaximumFrequencyTextField().getFrequency());
            double value = (Double)getFrequencyCorrectionSpinner().getValue();
            config.setFrequencyCorrection(value);
            config.setAutoPPMCorrectionEnabled(getAutoPPMCheckBox().isSelected());
            config.setBiasT(getTuner().getController().isBiasT());
            config.setSampleRate((SampleRate)getSampleRateCombo().getValue());
            config.setMasterGain((E4KGain)getMasterGainCombo().getValue());
            config.setMixerGain((E4KMixerGain)getMixerGainCombo().getValue());
            config.setLNAGain((E4KLNAGain)getLNAGainCombo().getValue());
            config.setIFGain((E4KEmbeddedTuner.IFGain)getIfGainCombo().getValue());
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