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

package io.github.dsheirer.source.tuner.sdrplay.rsp1a;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.sdrplay.DiscoveredRspTuner;
import io.github.dsheirer.source.tuner.sdrplay.RspSampleRate;
import io.github.dsheirer.source.tuner.sdrplay.RspTunerEditor;
import io.github.dsheirer.source.tuner.sdrplay.api.SDRPlayException;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.control.AgcMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.control.CheckBox;
import io.github.dsheirer.gui.control.ToggleSwitch;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;


/**
 * RSP1A Tuner Editor
 */
public class Rsp1aTunerEditor extends RspTunerEditor<Rsp1aTunerConfiguration>
{
    private static final Logger mLog = LoggerFactory.getLogger(Rsp1aTunerEditor.class);

    private ComboBox<RspSampleRate> mSampleRateCombo;
    private CheckBox mBiasTCheckBox;
    private CheckBox mRfDabNotchCheckBox;
    private CheckBox mRfNotchCheckBox;

    /**
     * Constructs an instance
     * @param userPreferences for settings
     * @param tunerManager for state updates
     * @param discoveredTuner to edit or control.
     */
    public Rsp1aTunerEditor(UserPreferences userPreferences, TunerManager tunerManager, DiscoveredRspTuner discoveredTuner)
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

        getChildren().add(new Label("Frequency (MHz):"));
        getChildren().add(getFrequencyPanel());

        getChildren().add(new Label("Sample Rate:"));
        getChildren().add(getSampleRateCombo());

        getChildren().add(new Label("Gain:"));
        getChildren().add(getGainPanel());
        getChildren().add(new Label("LNA:"));
        Button lnaHelp = createHelpIcon("?");
        lnaHelp.setTooltip(new javafx.scene.control.Tooltip("<html><b>LNA Gain:</b> The power of the signal amplifier.<br>Increase this for distant signals, but lower it if you see a lot of static/noise.</html>"));
        getChildren().add(lnaHelp);
        getChildren().add(getLNASlider());
        getChildren().add(new Label("IF:"));
        getChildren().add(getIfGainSlider());

        getChildren().add(new Separator());

        getChildren().add(new Label());
        getChildren().add(getBiasTCheckBox());
        getChildren().add(new Label());
        getChildren().add(getRfNotchCheckBox());
        getChildren().add(new Label());
        getChildren().add(getRfDabNotchCheckBox());
    }

    /**
     * Access tuner controller
     */
    private Rsp1aTunerController getTunerController()
    {
        if(hasTuner())
        {
            return (Rsp1aTunerController)getTuner().getTunerController();
        }

        return null;
    }

    @Override
    public void setTunerLockState(boolean locked)
    {
        getFrequencyPanel().updateControls();
        getSampleRateCombo().setDisable(!(!locked));
        updateSampleRateToolTip();
    }

    @Override
    protected void tunerStatusUpdated()
    {
        setLoading(true);

        getTunerIdLabel().setText(getDiscoveredTuner().getName());

        String status = getDiscoveredTuner().getTunerStatus().toString();
        if(getDiscoveredTuner().hasErrorMessage())
        {
            status += " - " + getDiscoveredTuner().getErrorMessage();
        }
        getTunerStatusLabel().setText(status);
        getButtonPanel().updateControls();
        getFrequencyPanel().updateControls();

        getSampleRateCombo().setDisable(!(hasTuner()) && !getTuner().getTunerController().isLockedSampleRate());
        getSampleRateCombo().setValue(hasTuner() ? getTunerController().getControlRsp().getSampleRateEnumeration() : null);
        updateSampleRateToolTip();

        getAgcButton().setDisable(!(hasTuner()));
        if(hasTuner())
        {
            AgcMode current = getTunerController().getControlRsp().getAgcMode();
            getAgcButton().setSelected(current == null || current.equals(AgcMode.ENABLE));
            getAgcButton().setText((current == null || current.equals(AgcMode.ENABLE)) ? AUTOMATIC : MANUAL);
            getLNASlider().setLNA(getTunerController().getControlRsp().getLNA());
            getIfGainSlider().setGR(getTunerController().getControlRsp().getBasebandGainReduction());

            //Register to receive gain overload notifications
            getTunerController().getControlRsp().setGainOverloadListener(this);
            updateGainLabel();
        }

        getLNASlider().setDisable(!(hasTuner()));
        getIfGainSlider().setDisable(!(hasTuner()) && getTunerController().getControlRsp().getAgcMode() != AgcMode.ENABLE);
        getGainValueLabel().setDisable(!(hasTuner()));
        getBiasTCheckBox().setDisable(!(hasTuner()));

        try
        {
            getBiasTCheckBox().setSelected(hasTuner() && getTunerController().getControlRsp().isBiasT());
        }
        catch(SDRPlayException se)
        {
            mLog.error("Error setting Bias-T enabled state in editor");
        }

        getRfDabNotchCheckBox().setDisable(!(hasTuner()));
        try
        {
            getRfDabNotchCheckBox().setSelected(hasTuner() && getTunerController().getControlRsp().isRfDabNotch());
        }
        catch(SDRPlayException se)
        {
            mLog.error("Error setting RF DAB Notch enabled state in editor");
        }

        getRfNotchCheckBox().setDisable(!(hasTuner()));
        try
        {
            getRfNotchCheckBox().setSelected(hasTuner() ? getTunerController().getControlRsp().isRfNotch() : false);
        }
        catch(SDRPlayException se)
        {
            mLog.error("Error setting RF Notch enabled state in editor");
        }

        setLoading(false);
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
            getConfiguration().setSampleRate((RspSampleRate)getSampleRateCombo().getValue());
            getConfiguration().setBiasT(getBiasTCheckBox().isSelected());
            getConfiguration().setRfNotch(getRfNotchCheckBox().isSelected());
            getConfiguration().setRfDabNotch(getRfDabNotchCheckBox().isSelected());
            getConfiguration().setLNA(getLNASlider().getLNA());
            getConfiguration().setBasebandGainReduction(getIfGainSlider().getGR());
            getConfiguration().setAgcMode(getAgcButton().isSelected() ? AgcMode.ENABLE : AgcMode.DISABLE);

            saveConfiguration();
        }
    }

    /**
     * Sample rate selection combobox control
     */
    private ComboBox<RspSampleRate> getSampleRateCombo()
    {
        if(mSampleRateCombo == null)
        {
            RspSampleRate[] rspSampleRates = RspSampleRate.SINGLE_TUNER_SAMPLE_RATES.toArray(new RspSampleRate[RspSampleRate.SINGLE_TUNER_SAMPLE_RATES.size()]);
            mSampleRateCombo = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(rspSampleRates)));
            mSampleRateCombo.setDisable(!(false));
            mSampleRateCombo.setOnAction(e -> {
                if(hasTuner() && !isLoading())
                {
                    RspSampleRate selected = (RspSampleRate)mSampleRateCombo.getValue();

                    try
                    {
                        getTunerController().setSampleRate(selected);
                        //Adjust the min/max values for the sample rate.
                        adjustForSampleRate((int)selected.getSampleRate());
                        save();
                    }
                    catch(SDRPlayException se)
                    {
                        mLog.error("Error setting sample rate for RSP1A tuner", se);
                    }
                }
            });
        }

        return mSampleRateCombo;
    }

    /**
     * Checkbox control for Bias-T
     */
    private CheckBox getBiasTCheckBox()
    {
        if(mBiasTCheckBox == null)
        {
            mBiasTCheckBox = new CheckBox("Bias-T Power");
            mBiasTCheckBox.setDisable(!(false));
            mBiasTCheckBox.setOnAction(e -> {
                if(hasTuner() && !isLoading())
                {
                    try
                    {
                        getTunerController().getControlRsp().setBiasT(mBiasTCheckBox.isSelected());
                        save();
                    }
                    catch(SDRPlayException se)
                    {
                        mLog.error("Unable to set RSP1A Bias-T enabled to " + mBiasTCheckBox.isSelected(), se);
                    }
                }
            });
        }

        return mBiasTCheckBox;
    }

    /**
     * Checkbox control for RF DAB notch
     */
    private CheckBox getRfDabNotchCheckBox()
    {
        if(mRfDabNotchCheckBox == null)
        {
            mRfDabNotchCheckBox = new CheckBox("DAB Broadcast Band Filter (160-235 MHz)");
            mRfDabNotchCheckBox.setDisable(!(false));
            mRfDabNotchCheckBox.setOnAction(e -> {
                if(hasTuner() && !isLoading())
                {
                    try
                    {
                        getTunerController().getControlRsp().setRfDabNotch(mRfDabNotchCheckBox.isSelected());
                        save();
                    }
                    catch(SDRPlayException se)
                    {
                        mLog.error("Unable to set RSP1A RF DAB notch enabled to " + mRfDabNotchCheckBox.isSelected(), se);
                    }
                }
            });
        }

        return mRfDabNotchCheckBox;
    }

    /**
     * Checkbox control for RF notch
     */
    private CheckBox getRfNotchCheckBox()
    {
        if(mRfNotchCheckBox == null)
        {
            mRfNotchCheckBox = new CheckBox("FM Broadcast Band Filter (77-115 MHz)");
            mRfNotchCheckBox.setDisable(!(false));
            mRfNotchCheckBox.setOnAction(e -> {
                if(hasTuner() && !isLoading())
                {
                    try
                    {
                        getTunerController().getControlRsp().setRfNotch(mRfNotchCheckBox.isSelected());
                        save();
                    }
                    catch(SDRPlayException se)
                    {
                        mLog.error("Unable to set RSP1A RF notch enabled to " + mRfNotchCheckBox.isSelected(), se);
                    }
                }
            });
        }

        return mRfNotchCheckBox;
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



    protected Button createHelpIcon(String text) {
        Button button = new Button(text);
        button.setPadding(new javafx.geometry.Insets(0, 2, 0, 2));
        button.setFocusTraversable(false);
        return button;
    }
}
