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
package io.github.dsheirer.source.tuner.hackrf;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.hackrf.HackRFTunerController.HackRFLNAGain;
import io.github.dsheirer.source.tuner.hackrf.HackRFTunerController.HackRFSampleRate;
import io.github.dsheirer.source.tuner.hackrf.HackRFTunerController.HackRFVGAGain;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.ui.TunerEditor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * HackRF Tuner Editor
 */
public class HackRFTunerEditor extends TunerEditor<HackRFTuner,HackRFTunerConfiguration>
{
    private static final long serialVersionUID = 1L;
    private final static Logger mLog = LoggerFactory.getLogger(HackRFTunerEditor.class);
    private ComboBox<HackRFSampleRate> mSampleRateCombo;
    private ToggleButton mAmplifier;
    private ComboBox<HackRFLNAGain> mLnaGainCombo;
    private ComboBox<HackRFVGAGain> mVgaGainCombo;

    /**
     * Constructs an instance
     * @param userPreferences for wide-band recordings
     * @param tunerManager to save configuration
     * @param discoveredTuner to control
     */
    public HackRFTunerEditor(UserPreferences userPreferences, TunerManager tunerManager, DiscoveredTuner discoveredTuner)
    {
        super(userPreferences, tunerManager, discoveredTuner);
        init();
        tunerStatusUpdated();
    }

    @Override
    public long getMinimumTunableFrequency()
    {
        return HackRFTunerController.MINIMUM_TUNABLE_FREQUENCY_HZ;
    }

    @Override
    public long getMaximumTunableFrequency()
    {
        return HackRFTunerController.MAXIMUM_TUNABLE_FREQUENCY_HZ;
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

        getChildren().add(new Label("Frequency (MHz):"));
        getChildren().add(getFrequencyPanel());

        getChildren().add(new Label("Sample Rate:"));
        getChildren().add(getSampleRateCombo());

        getChildren().add(new Separator());

        getChildren().add(new Label("Gain Control"));
        getChildren().add(getAmplifierToggle());

        getChildren().add(new Label("LNA:"));
        getChildren().add(getLnaGainCombo());

        getChildren().add(new Label("VGA:"));
        getChildren().add(getVgaGainCombo());
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
        getSampleRateCombo().setDisable(!(hasTuner()) && !getTuner().getTunerController().isLockedSampleRate());
        updateSampleRateToolTip();

        getAmplifierToggle().setDisable(!(hasTuner()));
        getLnaGainCombo().setDisable(!(hasTuner()));
        getVgaGainCombo().setDisable(!(hasTuner()));

        if(hasConfiguration())
        {
            getSampleRateCombo().setValue(getConfiguration().getSampleRate());
            getAmplifierToggle().setSelected(getConfiguration().getAmplifierEnabled());
            getLnaGainCombo().setValue(getConfiguration().getLNAGain());
            getVgaGainCombo().setValue(getConfiguration().getVGAGain());
        }

        setLoading(false);
    }

    private ComboBox getVgaGainCombo()
    {
        if(mVgaGainCombo == null)
        {
            mVgaGainCombo = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(HackRFVGAGain.values()));
            mVgaGainCombo.setTooltip(new javafx.scene.control.Tooltip("<html>VGA Gain.  Adjust to set the baseband gain</html>"));
            mVgaGainCombo.setDisable(!(false));
            mVgaGainCombo.setOnAction(arg0 ->
            {
                if(!isLoading())
                {
                    try
                    {
                        HackRFVGAGain vgaGain = (HackRFVGAGain) mVgaGainCombo.getValue();

                        if(vgaGain == null)
                        {
                            vgaGain = HackRFVGAGain.GAIN_16;
                        }

                        getTuner().getController().setVGAGain(vgaGain);
                        save();
                    }
                    catch(UsbException e)
                    {
                        Platform.runLater(() -> { Alert alert = new Alert(Alert.AlertType.INFORMATION); io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane()); alert.setContentText(String.valueOf("HackRF Tuner Controller"
                                + " - couldn't apply the VGA gain setting - " + e.getLocalizedMessage())); alert.showAndWait(); });

                        mLog.error("HackRF Tuner Controller - couldn't apply VGA gain setting", e);
                    }
                }
            });
        }

        return mVgaGainCombo;
    }

    private ComboBox getLnaGainCombo()
    {
        if(mLnaGainCombo == null)
        {
            mLnaGainCombo = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(HackRFLNAGain.values()));
            mLnaGainCombo.setTooltip(new javafx.scene.control.Tooltip("<html>LNA Gain.  Adjust to set the IF gain</html>"));
            mLnaGainCombo.setDisable(!(false));
            mLnaGainCombo.setOnAction(arg0 ->
            {
                if(!isLoading())
                {
                    try
                    {
                        HackRFLNAGain lnaGain = (HackRFLNAGain) mLnaGainCombo.getValue();

                        if(lnaGain == null)
                        {
                            lnaGain = HackRFLNAGain.GAIN_16;
                        }

                        getTuner().getController().setLNAGain(lnaGain);
                        save();
                    }
                    catch(UsbException e)
                    {
                        Platform.runLater(() -> { Alert alert = new Alert(Alert.AlertType.INFORMATION); io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane()); alert.setContentText(String.valueOf("HackRF Tuner Controller"
                                + " - couldn't apply the LNA gain setting - " + e.getLocalizedMessage())); alert.showAndWait(); });
                        mLog.error("HackRF Tuner Controller - couldn't apply LNA gain setting - ", e);
                    }
                }
            });
        }

        return mLnaGainCombo;
    }

    private ToggleButton getAmplifierToggle()
    {
        if(mAmplifier == null)
        {
            mAmplifier = new ToggleButton("Amplifier");
            mAmplifier.setTooltip(new javafx.scene.control.Tooltip("Enable or disable the gain amplifier"));
            mAmplifier.setDisable(!(false));
            mAmplifier.setOnAction(arg0 ->
            {
                if(!isLoading())
                {
                    try
                    {
                        getTuner().getController().setAmplifierEnabled(mAmplifier.isSelected());
                        save();
                    }
                    catch(UsbException e)
                    {
                        mLog.error("couldn't enable/disable amplifier", e);

                        Platform.runLater(() -> { Alert alert = new Alert(Alert.AlertType.ERROR); io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane()); alert.setContentText(String.valueOf("Couldn't change amplifier setting")); alert.showAndWait(); });
                    }
                }
            });
        }

        return mAmplifier;
    }

    private ComboBox getSampleRateCombo()
    {
        if(mSampleRateCombo == null)
        {
            HackRFSampleRate[] validRates = HackRFSampleRate.VALID_SAMPLE_RATES.toArray(new HackRFSampleRate[0]);
            mSampleRateCombo = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(validRates)));
            mSampleRateCombo.setDisable(!(false));
            mSampleRateCombo.setOnAction(e ->
            {
                if(!isLoading())
                {
                    HackRFSampleRate sampleRate = (HackRFSampleRate)getSampleRateCombo().getValue();

                    try
                    {
                        getTuner().getController().setSampleRate(sampleRate);
                        //Adjust the min/max values for the sample rate.
                        adjustForSampleRate(sampleRate.getRate());
                        save();
                    }
                    catch(SourceException | UsbException e2)
                    {
                        Platform.runLater(() -> { Alert alert = new Alert(Alert.AlertType.INFORMATION); io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane()); alert.setContentText(String.valueOf("HackRF Tuner Controller"
                                + " - couldn't apply the sample rate setting [" + sampleRate.getLabel() +
                                "] " + e2.getLocalizedMessage())); alert.showAndWait(); });

                        mLog.error("HackRF Tuner Controller - couldn't apply sample rate setting [" +
                                sampleRate.getLabel() + "]", e);
                    }
                }
            });
        }

        return mSampleRateCombo;
    }

/**
     * Updates the sample rate tooltip according to the tuner controller's lock state.
     */
    private void updateSampleRateToolTip()
    {
        if(hasTuner() && getTuner().getController().isLockedSampleRate())
        {
            mSampleRateCombo.setTooltip(new javafx.scene.control.Tooltip("Sample Rate is locked.  Disable decoding channels to unlock."));
        }
        else
        {
            mSampleRateCombo.setTooltip(new javafx.scene.control.Tooltip("Select a sample rate for the tuner"));
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
        HackRFTunerController.BoardID board = HackRFTunerController.BoardID.INVALID;

        try
        {
            if(hasTuner())
            {
                board = getTuner().getController().getBoardID();
            }
        }
        catch(UsbException e)
        {
            mLog.error("couldn't read HackRF board identifier", e);
        }

        StringBuilder sb = new StringBuilder();

        sb.append("<html><h3>HackRF Tuner</h3>");
        sb.append("<b>Board: </b>");
        sb.append(board.getLabel());
        sb.append("<br>");

        HackRFTunerController.Serial serial = null;

        try
        {
            if(hasTuner())
            {
                serial = getTuner().getController().getSerial();
            }
        }
        catch(Exception e)
        {
            mLog.error("couldn't read HackRF serial number", e);
        }

        if(serial != null)
        {
            sb.append("<b>Serial: </b>");
            sb.append(serial.getSerialNumber());
            sb.append("<br>");

            sb.append("<b>Part: </b>");
            sb.append(serial.getPartID());
            sb.append("<br>");
        }
        else
        {
            sb.append("<b>Serial: Unknown</b><br>");
            sb.append("<b>Part: Unknown</b><br>");
        }

        String firmware = null;

        try
        {
            if(hasTuner())
            {
                firmware = getTuner().getController().getFirmwareVersion();
            }
        }
        catch(Exception e)
        {
            mLog.error("couldn't read HackRF firmware version", e);
        }

        if(firmware != null)
        {
            sb.append("<b>Firmware: </b>");
            sb.append(firmware);
            sb.append("<br>");
        }
        else
        {
            sb.append("<b>Firmware: Unknown</b><br>");
        }

        return sb.toString();
    }

    @Override
    public void save()
    {
        if(hasConfiguration() && !isLoading())
        {
            getConfiguration().setFrequency(getFrequencyControl().getFrequency());
            getConfiguration().setMinimumFrequency(getMinimumFrequencyTextField().getFrequency());
            getConfiguration().setMaximumFrequency(getMaximumFrequencyTextField().getFrequency());
            double value = (Double) getFrequencyCorrectionSpinner().getValue();
            getConfiguration().setFrequencyCorrection(value);
            getConfiguration().setAutoPPMCorrectionEnabled(getAutoPPMCheckBox().isSelected());
            getConfiguration().setSampleRate((HackRFSampleRate)getSampleRateCombo().getValue());
            getConfiguration().setAmplifierEnabled(getAmplifierToggle().isSelected());
            getConfiguration().setLNAGain((HackRFLNAGain)getLnaGainCombo().getValue());
            getConfiguration().setVGAGain((HackRFVGAGain)getVgaGainCombo().getValue());
            saveConfiguration();
        }
    }
}