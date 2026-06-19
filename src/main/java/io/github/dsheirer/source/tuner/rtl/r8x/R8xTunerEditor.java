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
package io.github.dsheirer.source.tuner.rtl.r8x;
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
import io.github.dsheirer.source.tuner.ui.TunerEditor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.LibUsbException;

import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

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
 * R8xxx Tuner Editor
 */
public class R8xTunerEditor extends TunerEditor<RTL2832Tuner, R8xTunerConfiguration>
{
    private final static Logger mLog = LoggerFactory.getLogger(R8xTunerEditor.class);
    private static final long serialVersionUID = 1L;
    private static final R8xEmbeddedTuner.MasterGain DEFAULT_GAIN = R8xEmbeddedTuner.MasterGain.GAIN_279;
    private ToggleButton mBiasTButton;
    private ComboBox<SampleRate> mSampleRateCombo;
    private ComboBox<R8xEmbeddedTuner.MasterGain> mMasterGainCombo;
    private ComboBox<R8xEmbeddedTuner.MixerGain> mMixerGainCombo;
    private ComboBox<R8xEmbeddedTuner.LNAGain> mLNAGainCombo;
    private ComboBox<R8xEmbeddedTuner.VGAGain> mVGAGainCombo;

    /**
     * Constructs an instance
     * @param userPreferences for wide-band recordings
     * @param tunerManager for saving configurations
     * @param discoveredTuner to edit
     */
    public R8xTunerEditor(UserPreferences userPreferences, TunerManager tunerManager, DiscoveredTuner discoveredTuner)
    {
        super(userPreferences, tunerManager, discoveredTuner);
        init();
        tunerStatusUpdated();
    }

    @Override
    public long getMinimumTunableFrequency()
    {
        return R8xEmbeddedTuner.MINIMUM_TUNABLE_FREQUENCY_HZ;
    }

    @Override
    public long getMaximumTunableFrequency()
    {
        return R8xEmbeddedTuner.MAXIMUM_TUNABLE_FREQUENCY_HZ;
    }

    /**
     * Access the R8xxx embedded tuner
     * @return R8xxx tuner if there is a tuner, or null otherwise
     */
    private R8xEmbeddedTuner getEmbeddedTuner()
    {
        if(hasTuner())
        {
            return (R8xEmbeddedTuner) getTuner().getController().getEmbeddedTuner();
        }

        return null;
    }

    private String getLogPrefix()
    {
        return getEmbeddedTuner().getTunerType().getLabel() + " Tuner Controller - ";
    }

    private void init()
    {
        setSpacing(8);
        setPadding(new javafx.geometry.Insets(10));

        // Tuner info grid
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(10);
        infoGrid.setVgap(4);
        infoGrid.add(new Label("Tuner:"), 0, 0);
        infoGrid.add(getTunerIdLabel(), 1, 0);
        infoGrid.add(new Label("Status:"), 0, 1);
        infoGrid.add(getTunerStatusLabel(), 1, 1);
        infoGrid.add(new Label("Bias-T:"), 0, 2);
        infoGrid.add(getBiasTButton(), 1, 2);
        getChildren().add(infoGrid);

        getChildren().add(getButtonPanel());
        getChildren().add(new Separator());

        // Frequency section
        HBox freqLabelRow = new HBox(new Label("Frequency (MHz):"));
        freqLabelRow.setAlignment(Pos.CENTER_LEFT);
        getChildren().add(freqLabelRow);
        getChildren().add(getFrequencyPanel());

        // Sample rate row
        HBox sampleRateRow = new HBox(10);
        sampleRateRow.setAlignment(Pos.CENTER_LEFT);
        sampleRateRow.getChildren().addAll(new Label("Sample Rate:"), getSampleRateCombo());
        getChildren().add(sampleRateRow);

        getChildren().add(new Separator());

        // Gain section as a grid
        Label gainLabel = new Label("Gain:");
        GridPane gainGrid = new GridPane();
        gainGrid.setHgap(10);
        gainGrid.setVgap(4);
        gainGrid.add(new Label("Master:"), 0, 0);
        gainGrid.add(getMasterGainCombo(), 1, 0);
        gainGrid.add(new Label("Mixer:"), 0, 1);
        gainGrid.add(getMixerGainCombo(), 1, 1);
        gainGrid.add(new Label("LNA:"), 0, 2);
        gainGrid.add(getLNAGainCombo(), 1, 2);
        gainGrid.add(new Label("VGA:"), 0, 3);
        gainGrid.add(getVGAGainCombo(), 1, 3);
        getChildren().addAll(gainLabel, gainGrid);
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
            getMasterGainCombo().setDisable(!(true));
            R8xEmbeddedTuner.MasterGain gain = getConfiguration().getMasterGain();
            getMasterGainCombo().setDisable(!(true));
            getMasterGainCombo().setValue(gain);

            if(gain == R8xEmbeddedTuner.MasterGain.MANUAL)
            {
                getMixerGainCombo().setValue(getConfiguration().getMixerGain());
                getMixerGainCombo().setDisable(!(true));

                getLNAGainCombo().setValue(getConfiguration().getLNAGain());
                getLNAGainCombo().setDisable(!(true));

                getVGAGainCombo().setValue(getConfiguration().getVGAGain());
                getVGAGainCombo().setDisable(!(true));
            }
            else
            {
                getMixerGainCombo().setDisable(!(false));
                getMixerGainCombo().setValue(gain.getMixerGain());

                getLNAGainCombo().setDisable(!(false));
                getLNAGainCombo().setValue(gain.getLNAGain());

                getVGAGainCombo().setDisable(!(false));
                getVGAGainCombo().setValue(gain.getVGAGain());
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
            getVGAGainCombo().setDisable(!(false));
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
                    applyDeviceControl("r8x-bias-t", () -> getTuner().getController().setBiasT(biasT),
                            getLogPrefix() + "couldn't set Bias-T");
               }
            });
        }

        return mBiasTButton;
    }

private ComboBox getVGAGainCombo()
    {
        if(mVGAGainCombo == null)
        {
            mVGAGainCombo = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(R8xEmbeddedTuner.VGAGain.values())))));
            mVGAGainCombo.setDisable(!(false));
            mVGAGainCombo.setOnAction(arg0 ->
            {
                if(hasTuner() && !isLoading())
                {
                    R8xEmbeddedTuner.VGAGain vgaGain = (R8xEmbeddedTuner.VGAGain) mVGAGainCombo.getValue();

                    if(vgaGain == null)
                    {
                        vgaGain = DEFAULT_GAIN.getVGAGain();
                    }

                    final R8xEmbeddedTuner.VGAGain finalVgaGain = vgaGain;
                    final boolean apply = !mVGAGainCombo.isDisabled();

                    save();

                    if(apply)
                    {
                        applyDeviceControl("r8x-vga-gain", () -> {
                            try { getEmbeddedTuner().setVGAGain(finalVgaGain, true); }
                            catch(Exception ex) { throw new RuntimeException(ex); }
                        }, getLogPrefix() + "couldn't apply VGA gain setting");
                    }
                }
            });
            mVGAGainCombo.setTooltip(new javafx.scene.control.Tooltip("<html>VGA Gain.  Set master gain to <b>MANUAL</b> to enable adjustment</html>"));
        }

        return mVGAGainCombo;
    }

    private ComboBox getLNAGainCombo()
    {
        if(mLNAGainCombo == null)
        {
            mLNAGainCombo = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(R8xEmbeddedTuner.LNAGain.values())))));
            mLNAGainCombo.setDisable(!(false));
            mLNAGainCombo.setOnAction(arg0 ->
            {
                if(hasTuner() && !isLoading())
                {
                    R8xEmbeddedTuner.LNAGain lnaGain = (R8xEmbeddedTuner.LNAGain) mLNAGainCombo.getValue();

                    if(lnaGain == null)
                    {
                        lnaGain = DEFAULT_GAIN.getLNAGain();
                    }

                    final R8xEmbeddedTuner.LNAGain finalLnaGain = lnaGain;
                    final boolean apply = !mLNAGainCombo.isDisabled();

                    save();

                    if(apply)
                    {
                        applyDeviceControl("r8x-lna-gain", () -> {
                            try { getEmbeddedTuner().setLNAGain(finalLnaGain, true); }
                            catch(Exception ex) { throw new RuntimeException(ex); }
                        }, getLogPrefix() + "couldn't apply LNA gain setting");
                    }
                }
            });
            mLNAGainCombo.setTooltip(new javafx.scene.control.Tooltip("The power of the signal amplifier. Increase this for distant signals, but lower it if you see a lot of static/noise. Set master gain to MANUAL to enable adjustment."));
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
                if(hasTuner() && !isLoading())
                {
                    SampleRate sampleRate = (SampleRate) mSampleRateCombo.getValue();
                    //Adjust the min/max values for the sample rate.
                    adjustForSampleRate(sampleRate.getRate());
                    save();
                    applyDeviceControl("r8x-sample-rate", () -> {
                        try { getTuner().getController().setSampleRate(sampleRate); }
                        catch(Exception ex) { throw new RuntimeException(ex); }
                    }, getLogPrefix() + "couldn't apply sample rate setting [" + sampleRate.getLabel() + "]");
                }
            });
        }

        return mSampleRateCombo;
    }

    private ComboBox getMixerGainCombo()
    {
        if(mMixerGainCombo == null)
        {
            mMixerGainCombo = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(R8xEmbeddedTuner.MixerGain.values())))));
            mMixerGainCombo.setDisable(!(false));
            mMixerGainCombo.setOnAction(arg0 ->
            {
                if(hasTuner() && !isLoading())
                {
                    R8xEmbeddedTuner.MixerGain mixerGain = (R8xEmbeddedTuner.MixerGain) mMixerGainCombo.getValue();

                    if(mixerGain == null)
                    {
                        mixerGain = DEFAULT_GAIN.getMixerGain();
                    }

                    final R8xEmbeddedTuner.MixerGain finalMixerGain = mixerGain;
                    final boolean apply = !mMixerGainCombo.isDisabled();

                    save();

                    if(apply)
                    {
                        applyDeviceControl("r8x-mixer-gain", () -> {
                            try { getEmbeddedTuner().setMixerGain(finalMixerGain, true); }
                            catch(Exception ex) { throw new RuntimeException(ex); }
                        }, getLogPrefix() + "couldn't apply mixer gain setting");
                    }
                }
            });
            mMixerGainCombo.setTooltip(new javafx.scene.control.Tooltip("<html>Mixer Gain.  Set master gain to <b>MANUAL</b> to enable adjustment</html>"));
        }

        return mMixerGainCombo;
    }

    private ComboBox getMasterGainCombo()
    {
        if(mMasterGainCombo == null)
        {
            mMasterGainCombo = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(R8xEmbeddedTuner.MasterGain.values())))));
            mMasterGainCombo.setDisable(!(false));
            mMasterGainCombo.setOnAction(arg0 ->
            {
                if(hasTuner() && !isLoading())
                {
                    R8xEmbeddedTuner.MasterGain gain = (R8xEmbeddedTuner.MasterGain)getMasterGainCombo().getValue();

                    if(gain == R8xEmbeddedTuner.MasterGain.MANUAL)
                    {
                        getMixerGainCombo().setValue(gain.getMixerGain());
                        getMixerGainCombo().setDisable(!(true));

                        getLNAGainCombo().setValue(gain.getLNAGain());
                        getLNAGainCombo().setDisable(!(true));

                        getVGAGainCombo().setValue(gain.getVGAGain());
                        getVGAGainCombo().setDisable(!(true));
                    }
                    else
                    {
                        getMixerGainCombo().setDisable(!(false));
                        getMixerGainCombo().setValue(gain.getMixerGain());

                        getLNAGainCombo().setDisable(!(false));
                        getLNAGainCombo().setValue(gain.getLNAGain());

                        getVGAGainCombo().setDisable(!(false));
                        getVGAGainCombo().setValue(gain.getVGAGain());
                    }

                    save();
                    applyDeviceControl("r8x-master-gain", () -> {
                        try { getEmbeddedTuner().setGain(gain, true); }
                        catch(Exception ex) { throw new RuntimeException(ex); }
                    }, getLogPrefix() + "couldn't apply gain setting");
                }
            });
            mMasterGainCombo.setTooltip(new javafx.scene.control.Tooltip("<html>Select <b>AUTOMATIC</b> for auto gain, <b>MANUAL</b> to enable<br> " +
                    "independent control of <i>Mixer</i>, <i>LNA</i> and <i>Enhance</i> gain<br>settings, or one of the " +
                    "individual gain settings for<br>semi-manual gain control</html>"));
        }

        return mMasterGainCombo;
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
            R8xTunerConfiguration config = getConfiguration();
            config.setBiasT(getTuner().getController().isBiasT());
            config.setFrequency(getFrequencyControl().getFrequency());
            getConfiguration().setMinimumFrequency(getMinimumFrequencyTextField().getFrequency());
            getConfiguration().setMaximumFrequency(getMaximumFrequencyTextField().getFrequency());
            double value = (Double)getFrequencyCorrectionSpinner().getValue();
            config.setFrequencyCorrection(value);
            config.setAutoPPMCorrectionEnabled(getAutoPPMCheckBox().isSelected());

            config.setSampleRate((SampleRate)getSampleRateCombo().getValue());
            R8xEmbeddedTuner.MasterGain gain = (R8xEmbeddedTuner.MasterGain)getMasterGainCombo().getValue();
            config.setMasterGain(gain);
            R8xEmbeddedTuner.MixerGain mixerGain = (R8xEmbeddedTuner.MixerGain)getMixerGainCombo().getValue();
            config.setMixerGain(mixerGain);
            R8xEmbeddedTuner.LNAGain lnaGain = (R8xEmbeddedTuner.LNAGain)getLNAGainCombo().getValue();
            config.setLNAGain(lnaGain);
            R8xEmbeddedTuner.VGAGain vgaGain = (R8xEmbeddedTuner.VGAGain)getVGAGainCombo().getValue();
            config.setVGAGain(vgaGain);
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