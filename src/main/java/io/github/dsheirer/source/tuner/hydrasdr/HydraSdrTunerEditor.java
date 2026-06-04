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
package io.github.dsheirer.source.tuner.hydrasdr;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;
import javafx.geometry.Orientation;


import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.hydrasdr.HydraSdrTunerController.Gain;
import io.github.dsheirer.source.tuner.hydrasdr.HydraSdrTunerController.GainMode;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.manager.TunerStatus;
import io.github.dsheirer.source.tuner.ui.TunerEditor;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import io.github.dsheirer.gui.control.ToggleSwitch;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.application.Platform;
import java.util.Optional;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;




/**
 * HydraSDR tuner editor/controller
 */
public class HydraSdrTunerEditor extends TunerEditor<HydraSdrTuner, HydraSdrTunerConfiguration>
{
    private static final long serialVersionUID = 1L;
    private final static Logger mLog = LoggerFactory.getLogger(HydraSdrTunerEditor.class);
    private ComboBox<HydraSdrSampleRate> mSampleRateCombo;
    private ComboBox<GainMode> mGainModeCombo;
    private Slider mMasterGainSlider;
    private Label mMasterGainLabel;
    private Label mMasterGainValueLabel;
    private Slider mIFGainSlider;
    private Label mIFGainLabel;
    private Label mIFGainValueLabel;
    private Slider mLNAGainSlider;
    private Label mLNAGainValueLabel;
    private Slider mMixerGainSlider;
    private Label mMixerGainValueLabel;
    private CheckBox mLNAAGCCheckBox;
    private CheckBox mMixerAGCCheckBox;
    private CheckBox mBiasTCheckBox;

    /**
     * Constructs an instance
     * @param userPreferences for wide-band recordings
     * @param tunerManager for saving tuner configuration
     * @param discoveredTuner for the optionally usable HydraSDR tuner and controller
     */
    public HydraSdrTunerEditor(UserPreferences userPreferences, TunerManager tunerManager, DiscoveredTuner discoveredTuner)
    {
        super(userPreferences, tunerManager, discoveredTuner);
        init();
        tunerStatusUpdated();
    }

    @Override
    public long getMinimumTunableFrequency()
    {
        return HydraSdrTunerController.MINIMUM_TUNABLE_FREQUENCY_HZ;
    }

    @Override
    public long getMaximumTunableFrequency()
    {
        return HydraSdrTunerController.MAXIMUM_TUNABLE_FREQUENCY_HZ;
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
        getBiasTCheckBox().setDisable(!(hasTuner()));
        if(hasTuner() && hasConfiguration())
        {
            getBiasTCheckBox().setSelected(getConfiguration().isBiasT());
        }
        updateGainComponents((hasTuner() && hasConfiguration()) ? getConfiguration().getGain() : null);

        if(hasTuner())
        {
            List<HydraSdrSampleRate> rates = getTuner().getController().getSampleRates();
            getSampleRateCombo().setItems(FXCollections.observableArrayList(rates.toArray(new HydraSdrSampleRate[rates.size()])));

            if(hasConfiguration())
            {
                HydraSdrSampleRate sampleRate = getSampleRate(getConfiguration().getSampleRate());
                getSampleRateCombo().setValue(sampleRate);
            }
        }
        else
        {
            getSampleRateCombo().setItems(FXCollections.observableArrayList());
        }

        setLoading(false);
    }

    private void init()
    {
        

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

        getChildren().add(new Label());
        getChildren().add(getBiasTCheckBox());

        getChildren().add(new Separator());
        getChildren().add(new Label("Gain Control"));

        getChildren().add(new Label("Mode:"));
        getChildren().add(getGainModeCombo());

        getChildren().add(getMasterGainLabel());
        getChildren().add(getMasterGainSlider());
        getChildren().add(getMasterGainValueLabel());

        getChildren().add(getIFGainLabel());
        getChildren().add(getIFGainSlider());
        getChildren().add(getIFGainValueLabel());

        getChildren().add(getMixerAGCCheckBox());
        getChildren().add(getMixerGainSlider());
        getChildren().add(getMixerGainValueLabel());

        getChildren().add(getLNAAGCCheckBox());
        getChildren().add(getLNAGainSlider());
        getChildren().add(getLNAGainValueLabel());
    }

    private CheckBox getLNAAGCCheckBox()
    {
        if(mLNAAGCCheckBox == null)
        {
            mLNAAGCCheckBox = new CheckBox("AGC LNA:");
            mLNAAGCCheckBox.setDisable(!(false));
            mLNAAGCCheckBox.setOnAction(e ->
            {
                if(hasTuner() && !isLoading())
                {
                    try
                    {
                        getTuner().getController().setLNAAGC(getLNAAGCCheckBox().isSelected());
                        getLNAGainSlider().setDisable(!getLNAAGCCheckBox().isSelected());
                        save();
                    }
                    catch(Exception e1)
                    {
                        mLog.error("Error setting LNA AGC Enabled");
                    }
                }
            });
        }

        return mLNAAGCCheckBox;
    }

    private CheckBox getMixerAGCCheckBox()
    {
        if(mMixerAGCCheckBox == null)
        {
            mMixerAGCCheckBox = new CheckBox("AGC Mixer:");
            mMixerAGCCheckBox.setDisable(!(false));
            mMixerAGCCheckBox.setOnAction(e ->
            {
                if(hasTuner() && !isLoading())
                {
                    try
                    {
                        getTuner().getController().setMixerAGC(getMixerAGCCheckBox().isSelected());
                        getMixerGainSlider().setDisable(!getMixerAGCCheckBox().isSelected());
                        save();
                    }
                    catch(Exception e1)
                    {
                        mLog.error("Error setting Mixer AGC Enabled");
                    }
                }
            });
        }

        return mMixerAGCCheckBox;
    }

    /**
     * Checkbox to enable/disable Bias-T power for active antennas
     */
    private CheckBox getBiasTCheckBox()
    {
        if(mBiasTCheckBox == null)
        {
            mBiasTCheckBox = new CheckBox("Bias-T");
            mBiasTCheckBox.setDisable(!(false));
            mBiasTCheckBox.setTooltip(new javafx.scene.control.Tooltip("Enable Bias-T power output for active antennas"));
            mBiasTCheckBox.setOnAction(e ->
            {
                if(hasTuner() && !isLoading())
                {
                    try
                    {
                        getTuner().getController().setBiasT(mBiasTCheckBox.isSelected());
                        save();
                    }
                    catch(Exception e1)
                    {
                        mLog.error("Error setting Bias-T", e1);
                        Platform.runLater(() -> { Alert alert = new Alert(Alert.AlertType.INFORMATION); io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane()); alert.setContentText(String.valueOf("Couldn't set Bias-T: " + e1.getMessage())); alert.showAndWait(); });
                    }
                }
            });
        }

        return mBiasTCheckBox;
    }

    private Label getLNAGainValueLabel()
    {
        if(mLNAGainValueLabel == null)
        {
            mLNAGainValueLabel = new Label("0");
            mLNAGainValueLabel.setDisable(!(false));
        }

        return mLNAGainValueLabel;
    }

    private Slider getLNAGainSlider()
    {
        if(mLNAGainSlider == null)
        {
            mLNAGainSlider = new Slider(HydraSdrTunerController.LNA_GAIN_MIN,
                    HydraSdrTunerController.LNA_GAIN_MAX, HydraSdrTunerController.LNA_GAIN_MIN);
            mLNAGainSlider.setDisable(!(false));
            mLNAGainSlider.setMajorTickUnit(1);
            mLNAGainSlider.setShowTickMarks(true);
            mLNAGainSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                int gain = (int) (int) mLNAGainSlider.getValue();

                if(hasTuner() && !isLoading())
                {
                    try
                    {
                        getTuner().getController().setLNAGain(gain);
                        save();
                    }
                    catch(Exception e)
                    {
                        mLog.error("Couldn't set HydraSDR LNA gain to:" + gain, e);
                        Platform.runLater(() -> { Alert alert = new Alert(Alert.AlertType.INFORMATION); io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane()); alert.setContentText(String.valueOf("Couldn't set LNA gain value to " + gain)); alert.showAndWait(); });
                    }
                }

                getLNAGainValueLabel().setText(String.valueOf(gain));
            });
        }

        return mLNAGainSlider;
    }

    private Label getMixerGainValueLabel()
    {
        if(mMixerGainValueLabel == null)
        {
            mMixerGainValueLabel = new Label("0");
            mMixerGainValueLabel.setDisable(!(false));
        }

        return mMixerGainValueLabel;
    }

    private Slider getMixerGainSlider()
    {
        if(mMixerGainSlider == null)
        {
            mMixerGainSlider = new Slider(HydraSdrTunerController.MIXER_GAIN_MIN,
                    HydraSdrTunerController.MIXER_GAIN_MAX, HydraSdrTunerController.MIXER_GAIN_MIN);
            mMixerGainSlider.setDisable(!(false));
            mMixerGainSlider.setMajorTickUnit(1);
            mMixerGainSlider.setShowTickMarks(true);
            mMixerGainSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                    int gain = (int) (int) mMixerGainSlider.getValue();

                    if(hasTuner() && !isLoading())
                    {
                        try
                        {
                            getTuner().getController().setMixerGain(gain);
                            save();
                        }
                        catch(Exception e)
                        {
                            mLog.error("Couldn't set HydraSDR Mixer gain to:" + gain, e);
                            Platform.runLater(() -> { Alert alert = new Alert(Alert.AlertType.INFORMATION); io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane()); alert.setContentText(String.valueOf("Couldn't set Mixer gain value to " + gain)); alert.showAndWait(); });
                        }
                    }

                    getMixerGainValueLabel().setText(String.valueOf(gain));
            });
        }

        return mMixerGainSlider;
    }

    private Label getIFGainLabel()
    {
        if(mIFGainLabel == null)
        {
            mIFGainLabel = new Label("IF:");
        }

        return mIFGainLabel;
    }

    private Label getIFGainValueLabel()
    {
        if(mIFGainValueLabel == null)
        {
            mIFGainValueLabel = new Label("0");
            mIFGainValueLabel.setDisable(!(false));
        }

        return mIFGainValueLabel;
    }

    private Slider getIFGainSlider()
    {
        if(mIFGainSlider == null)
        {
            mIFGainSlider = new Slider(HydraSdrTunerController.IF_GAIN_MIN,
                    HydraSdrTunerController.IF_GAIN_MAX, HydraSdrTunerController.IF_GAIN_MIN);
            mIFGainSlider.setDisable(!(false));
            mIFGainSlider.setMajorTickUnit(1);
            mIFGainSlider.setShowTickMarks(true);
            mIFGainSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                int gain = (int) (int) mIFGainSlider.getValue();

                if(hasTuner() && !isLoading())
                {
                    try
                    {
                        getTuner().getController().setIFGain(gain);
                        save();
                    }
                    catch(Exception e)
                    {
                        mLog.error("Couldn't set HydraSDR IF gain to:" + gain, e);
                        Platform.runLater(() -> { Alert alert = new Alert(Alert.AlertType.INFORMATION); io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane()); alert.setContentText(String.valueOf("Couldn't set IF gain value to " + gain)); alert.showAndWait(); });
                    }
                }

                getIFGainValueLabel().setText(String.valueOf(gain));
            });
        }

        return mIFGainSlider;
    }

    private Label getMasterGainLabel()
    {
        if(mMasterGainLabel == null)
        {
            mMasterGainLabel = new Label("Master:");
        }

        return mMasterGainLabel;
    }

    private Label getMasterGainValueLabel()
    {
        if(mMasterGainValueLabel == null)
        {
            mMasterGainValueLabel = new Label("0");
            mMasterGainValueLabel.setDisable(!(false));
        }

        return mMasterGainValueLabel;
    }

    private Slider getMasterGainSlider()
    {
        if(mMasterGainSlider == null)
        {
            mMasterGainSlider = new Slider(HydraSdrTunerController.GAIN_MIN,
                    HydraSdrTunerController.GAIN_MAX, HydraSdrTunerController.GAIN_MIN);
            mMasterGainSlider.setDisable(!(false));
            mMasterGainSlider.setMajorTickUnit(1);
            mMasterGainSlider.setShowTickMarks(true);

            mMasterGainSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                GainMode mode = (GainMode)mGainModeCombo.getValue();
                int value = (int) (int) mMasterGainSlider.getValue();
                Gain gain = Gain.getGain(mode, value);

                if(hasTuner() && !isLoading())
                {
                    try
                    {
                        getTuner().getController().setGain(gain);
                        save();
                    }
                    catch(Exception e)
                    {
                        mLog.error("Couldn't set HydraSDR gain to:" + gain.name(), e);
                        Platform.runLater(() -> { Alert alert = new Alert(Alert.AlertType.INFORMATION); io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane()); alert.setContentText(String.valueOf("Couldn't set gain value to " +
                                gain.getValue())); alert.showAndWait(); });
                    }
                }

                getMasterGainValueLabel().setText(String.valueOf(value));
            });
        }

        return mMasterGainSlider;
    }

    private ComboBox<GainMode> getGainModeCombo()
    {
        if(mGainModeCombo == null)
        {
            mGainModeCombo = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(GainMode.values())))));
            mGainModeCombo.setDisable(!(false));
            mGainModeCombo.setOnAction(e ->
            {
                if(hasTuner() && !isLoading())
                {
                    GainMode mode = (GainMode)mGainModeCombo.getValue();
                    int value = (int)getMasterGainSlider().getValue();
                    Gain gain = Gain.getGain(mode, value);
                    updateGainComponents(gain);
                    save();
                }
            });
        }

        return mGainModeCombo;
    }

    private ComboBox<HydraSdrSampleRate> getSampleRateCombo()
    {
        if(mSampleRateCombo == null)
        {
            mSampleRateCombo = new ComboBox<>();
            mSampleRateCombo.setDisable(!(false));
            mSampleRateCombo.setOnAction(new EventHandler<ActionEvent>()
            {
                @Override
                public void handle(ActionEvent e)
                {
                    if(hasTuner() && !isLoading())
                    {
                        HydraSdrSampleRate rate = (HydraSdrSampleRate)mSampleRateCombo.getValue();

                        try
                        {
                            getTuner().getController().setSampleRate(rate);

                            //Adjust the min/max values for the sample rate.
                            adjustForSampleRate(rate.getRate());

                            save();
                        }
                        catch(Exception e1)
                        {
                            Platform.runLater(() -> { Alert alert = new Alert(Alert.AlertType.INFORMATION); io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane()); alert.setContentText(String.valueOf("Couldn't set sample rate to " + rate.getLabel())); alert.showAndWait(); });
                            mLog.error("Error setting HydraSDR sample rate", e1);
                        }
                    }
                }
            });
        }

        return mSampleRateCombo;
    }

    /**
     * Hyperlink button that provides tuner information
     */
/**
     * Updates the enabled state of each of the gain controls according to the
     * specified gain mode.  The master gain control is enabled for linearity
     * and sensitivity and the individual gain controls are disabled, and
     * vice-versa for custom mode.
     */
    private void updateGainComponents(Gain gain)
    {
        if(hasTuner() && gain != null)
        {
            boolean isCustom = gain.equals(Gain.CUSTOM);

            getGainModeCombo().setDisable(!(true));
            getGainModeCombo().setValue(gain.getGainMode());
            getMasterGainLabel().setDisable(!(!isCustom));
            getMasterGainSlider().setDisable(!(!isCustom));
            getMasterGainSlider().setValue(gain.getValue());
            getMasterGainValueLabel().setDisable(!(!isCustom));
            getIFGainLabel().setDisable(!(isCustom));
            getIFGainSlider().setDisable(!(isCustom));
            getIFGainValueLabel().setDisable(!(isCustom));
            getLNAAGCCheckBox().setDisable(!(isCustom));
            getLNAGainSlider().setDisable(!(isCustom && !getConfiguration().isLNAAGC()));
            getLNAGainValueLabel().setDisable(!(isCustom));
            getMixerAGCCheckBox().setDisable(!(isCustom));
            getMixerGainSlider().setDisable(!(isCustom && !getConfiguration().isMixerAGC()));
            getMixerGainValueLabel().setDisable(!(isCustom));
            if(isCustom)
            {
                getIFGainSlider().setValue(getConfiguration().getIFGain());
                getLNAGainSlider().setValue(getConfiguration().getLNAGain());
                getMixerGainSlider().setValue(getConfiguration().getMixerGain());
                getMixerAGCCheckBox().setSelected(getConfiguration().isMixerAGC());
                getLNAAGCCheckBox().setSelected(getConfiguration().isLNAAGC());
            }
            else
            {
                getIFGainSlider().setValue(0);
                getLNAGainSlider().setValue(0);
                getMixerGainSlider().setValue(0);
                getMixerAGCCheckBox().setSelected(false);
                getLNAAGCCheckBox().setSelected(false);
            }
        }
        else
        {
            getGainModeCombo().setDisable(!(false));
            getMasterGainLabel().setDisable(!(false));
            getMasterGainSlider().setDisable(!(false));
            getMasterGainSlider().setValue(0);
            getMasterGainValueLabel().setDisable(!(false));
            getIFGainLabel().setDisable(!(false));
            getIFGainSlider().setDisable(!(false));
            getIFGainSlider().setValue(0);
            getIFGainValueLabel().setDisable(!(false));
            getLNAAGCCheckBox().setDisable(!(false));
            getLNAAGCCheckBox().setSelected(false);
            getLNAGainSlider().setDisable(!(false));
            getLNAGainSlider().setValue(0);
            getLNAGainValueLabel().setDisable(!(false));
            getMixerAGCCheckBox().setDisable(!(false));
            getMixerAGCCheckBox().setSelected(false);
            getMixerGainSlider().setDisable(!(false));
            getMixerGainSlider().setValue(0);
            getMixerGainValueLabel().setDisable(!(false));
        }
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
            getConfiguration().setSampleRate(((HydraSdrSampleRate)getSampleRateCombo().getValue()).getRate());
            Gain gain = Gain.getGain((GainMode)mGainModeCombo.getValue(), (int)getMasterGainSlider().getValue());
            getConfiguration().setGain(gain);
            getConfiguration().setIFGain((int)getIFGainSlider().getValue());
            getConfiguration().setMixerGain((int)getMixerGainSlider().getValue());
            getConfiguration().setLNAGain((int)getLNAGainSlider().getValue());
            getConfiguration().setMixerAGC(getMixerAGCCheckBox().isSelected());
            getConfiguration().setLNAAGC(getLNAAGCCheckBox().isSelected());
            getConfiguration().setBiasT(getBiasTCheckBox().isSelected());
            saveConfiguration();
        }
    }

    /**
     * Finds the HydraSDR sample rate entry that matches the value.
     * @param value in Hertz
     * @return the matching rate entry or null.
     */
    private HydraSdrSampleRate getSampleRate(int value)
    {
        if(hasTuner())
        {
            List<HydraSdrSampleRate> rates = getTuner().getController().getSampleRates();

            for(HydraSdrSampleRate rate : rates)
            {
                if(rate.getRate() == value)
                {
                    return rate;
                }
            }

            if(rates.size() > 0)
            {
                return rates.get(0);
            }
        }

        return null;
    }

    /**
     * Updates the sample rate tooltip according to the tuner controller's lock state.
     */
    private void updateSampleRateToolTip()
    {
        if(hasTuner() && getTuner().getController().isLockedSampleRate())
        {
            getSampleRateCombo().setTooltip(new javafx.scene.control.Tooltip("Sample Rate is locked.  Disable decoding channels to unlock."));
        }
        else
        {
            getSampleRateCombo().setTooltip(new javafx.scene.control.Tooltip("Select a sample rate for the tuner"));
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
        if(getDiscoveredTuner().getTunerStatus() == TunerStatus.ERROR)
        {
            return getDiscoveredTuner().getErrorMessage();
        }

        if(hasTuner())
        {
            StringBuilder sb = new StringBuilder();

            HydraSdrDeviceInformation info = getTuner().getController().getDeviceInfo();

            sb.append("<html><h3>HydraSDR Tuner</h3>");
            sb.append("<b>Serial: </b>");
            sb.append(info.getSerialNumber());
            sb.append("<br>");

            sb.append("<b>Firmware: </b>");
            String[] firmware = info.getVersion().split(" ");
            sb.append(firmware.length > 1 ? firmware[0] : info.getVersion());
            sb.append("<br>");

            sb.append("<b>Part: </b>");
            sb.append(info.getPartNumber());
            sb.append("<br>");

            sb.append("<b>Board ID: </b>");
            sb.append(info.getBoardID().getLabel());
            sb.append("<br>");

            return sb.toString();
        }

        return null;
    }
}
