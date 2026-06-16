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

        getChildren().add(getBiasTCheckBox());

        getChildren().add(new Separator());
        getChildren().add(new Label("Gain Control"));

        GridPane gainGrid = new GridPane();
        gainGrid.setHgap(10);
        gainGrid.setVgap(4);
        gainGrid.add(new Label("Mode:"), 0, 0);
        gainGrid.add(getGainModeCombo(), 1, 0);
        gainGrid.add(getMasterGainLabel(), 0, 1);
        gainGrid.add(getMasterGainSlider(), 1, 1);
        gainGrid.add(getMasterGainValueLabel(), 2, 1);
        gainGrid.add(getIFGainLabel(), 0, 2);
        gainGrid.add(getIFGainSlider(), 1, 2);
        gainGrid.add(getIFGainValueLabel(), 2, 2);
        gainGrid.add(getMixerAGCCheckBox(), 0, 3);
        gainGrid.add(getMixerGainSlider(), 1, 3);
        gainGrid.add(getMixerGainValueLabel(), 2, 3);
        gainGrid.add(getLNAAGCCheckBox(), 0, 4);
        gainGrid.add(getLNAGainSlider(), 1, 4);
        gainGrid.add(getLNAGainValueLabel(), 2, 4);
        getChildren().add(gainGrid);
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
                    final boolean lnaAGC = getLNAAGCCheckBox().isSelected();
                    getLNAGainSlider().setDisable(!lnaAGC);
                    save();
                    applyDeviceControl("hydra-lna-agc", () -> getTuner().getController().setLNAAGC(lnaAGC), "Error setting LNA AGC Enabled");
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
                    final boolean mixerAGC = getMixerAGCCheckBox().isSelected();
                    getMixerGainSlider().setDisable(!mixerAGC);
                    save();
                    applyDeviceControl("hydra-mixer-agc", () -> getTuner().getController().setMixerAGC(mixerAGC), "Error setting Mixer AGC Enabled");
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
                    final boolean biasT = mBiasTCheckBox.isSelected();
                    save();
                    applyDeviceControl("hydra-bias-t", () -> getTuner().getController().setBiasT(biasT), "Error setting Bias-T");
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
                    save();
                    applyDeviceControl("hydra-lna-gain", () -> getTuner().getController().setLNAGain(gain), "Couldn't set HydraSDR LNA gain to " + gain);
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
                        save();
                        applyDeviceControl("hydra-mixer-gain", () -> getTuner().getController().setMixerGain(gain), "Couldn't set HydraSDR Mixer gain to " + gain);
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
                    save();
                    applyDeviceControl("hydra-if-gain", () -> getTuner().getController().setIFGain(gain), "Couldn't set HydraSDR IF gain to " + gain);
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
                    save();
                    applyDeviceControl("hydra-master-gain", () -> getTuner().getController().setGain(gain), "Couldn't set HydraSDR gain to " + gain.name());
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
                        final HydraSdrSampleRate rate = (HydraSdrSampleRate)mSampleRateCombo.getValue();

                        //Adjust the min/max values for the sample rate.
                        adjustForSampleRate(rate.getRate());

                        save();
                        applyDeviceControl("hydra-sample-rate", () -> getTuner().getController().setSampleRate(rate), "Error setting HydraSDR sample rate");
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
