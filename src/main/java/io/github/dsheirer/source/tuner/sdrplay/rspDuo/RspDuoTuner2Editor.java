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

package io.github.dsheirer.source.tuner.sdrplay.rspDuo;

import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.preference.PreferenceEditorType;
import io.github.dsheirer.gui.preference.ViewUserPreferenceEditorRequest;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.sdrplay.DiscoveredRspTuner;
import io.github.dsheirer.source.tuner.sdrplay.RspSampleRate;
import io.github.dsheirer.source.tuner.sdrplay.RspTunerEditor;
import io.github.dsheirer.source.tuner.sdrplay.api.SDRPlayException;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.control.AgcMode;
import java.util.EnumSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import io.github.dsheirer.gui.control.ToggleSwitch;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.control.Separator;


/**
 * RSPduo Tuner 2 Editor
 */
public class RspDuoTuner2Editor extends RspTunerEditor<RspDuoTuner2Configuration>
{
    private static final Logger mLog = LoggerFactory.getLogger(RspDuoTuner2Editor.class);

    private ComboBox<RspSampleRate> mSampleRateCombo;
    private CheckBox mBiasTCheckBox;
    private CheckBox mExternalReferenceOutputCheckBox;
    private CheckBox mRfDabNotchCheckBox;
    private CheckBox mRfNotchCheckBox;
    private Button mTunerPreferencesButton;

    /**
     * Constructs an instance
     * @param userPreferences for settings
     * @param tunerManager for state updates
     * @param discoveredTuner to edit or control.
     */
    public RspDuoTuner2Editor(UserPreferences userPreferences, TunerManager tunerManager, DiscoveredRspTuner discoveredTuner)
    {
        super(userPreferences, tunerManager, discoveredTuner);
        init();
        tunerStatusUpdated();
    }

    private void init()
    {
        // setLayout(new javafx.scene.layout.HBox(4));

        getChildren().add(new Label("Tuner:"));
        VBox labelAndButtonPanel = new VBox();
        labelAndButtonPanel.getChildren().add(getTunerIdLabel());
        labelAndButtonPanel.getChildren().add(getTunerPreferencesButton());
        getChildren().add(labelAndButtonPanel);
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
        getChildren().add(getLNASlider());
        getChildren().add(new Label("IF:"));
        getChildren().add(getIfGainSlider());

        getChildren().add(new Separator());

        getChildren().add(new Label());
        getChildren().add(getExternalReferenceOutputCheckBox());
        getChildren().add(new Label());
        getChildren().add(getBiasTCheckBox());
        getChildren().add(new Label());
        getChildren().add(getRfDabNotchCheckBox());
        getChildren().add(new Label());
        getChildren().add(getRfNotchCheckBox());
    }

    private DiscoveredRspTuner getDiscoveredRspTuner()
    {
        return (DiscoveredRspTuner)getDiscoveredTuner();
    }

    /**
     * Access tuner controller
     */
    private RspDuoTuner2Controller getTunerController()
    {
        if(hasTuner())
        {
            return (RspDuoTuner2Controller) getTuner().getTunerController();
        }

        return null;
    }

    /**
     * Indicates if this editor has a tuner and the tuner is configured for slave mode.
     */
    private boolean isSlaveMode()
    {
        return getDiscoveredRspTuner().getDeviceInfo().getDeviceSelectionMode().isSlaveMode();
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

        //If master/slave configuration, disable the slave's enable/disable button - user can disable
        //the tuner via the master device.
        if(isSlaveMode())
        {
            getEnabledButton().setDisable(!(false));
        }

        getFrequencyPanel().updateControls();

        clearSampleRates();
        if(hasTuner())
        {
            setSampleRates(getTunerController().getControlRsp().getSupportedSampleRates());
        }
        getSampleRateCombo().setDisable(!(hasTuner()) && !isSlaveMode() && !getTuner().getTunerController().isLockedSampleRate());
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

        getRfDabNotchCheckBox().setDisable(!(hasTuner()));
        try
        {
            getRfDabNotchCheckBox().setSelected(hasTuner() && getTunerController().getControlRsp().isRfDabNotch());
        }
        catch(SDRPlayException se)
        {
            mLog.error("Error setting RSPduo tuner 2 RF DAB Notch enabled state in editor");
        }

        getRfNotchCheckBox().setDisable(!(hasTuner()));
        try
        {
            getRfNotchCheckBox().setSelected(hasTuner() && getTunerController().getControlRsp().isRfNotch());
        }
        catch(SDRPlayException se)
        {
            mLog.error("Error setting RSPduo tuner 2 RF Notch enabled state in editor");
        }

        getBiasTCheckBox().setDisable(!(hasTuner()));
        try
        {
            getBiasTCheckBox().setSelected(hasTuner() && getTunerController().getControlRsp().isBiasT());
        }
        catch(SDRPlayException se)
        {
            mLog.error("Error setting RSPduo tuner 2 Bias-T enabled state in editor");
        }

        getExternalReferenceOutputCheckBox().setDisable(!(hasTuner()) && !isSlaveMode());
        try
        {
            getExternalReferenceOutputCheckBox()
                    .setSelected(hasTuner() && !isSlaveMode() && getTunerController().getControlRsp().isExternalReferenceOutput());
        }
        catch(SDRPlayException se)
        {
            mLog.error("Error setting RSPduo tuner 2 external reference output enabled state in editor");
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
            if(getSampleRateCombo().getValue() != null)
            {
                getConfiguration().setSampleRate((RspSampleRate)getSampleRateCombo().getValue());
            }
            getConfiguration().setBiasT(getBiasTCheckBox().isSelected());
            if(hasTuner() && !isSlaveMode())
            {
                getConfiguration().setExternalReferenceOutput(getExternalReferenceOutputCheckBox().isSelected());
            }
            getConfiguration().setRfDabNotch(getRfDabNotchCheckBox().isSelected());
            getConfiguration().setRfNotch(getRfNotchCheckBox().isSelected());
            getConfiguration().setLNA(getLNASlider().getLNA());
            getConfiguration().setBasebandGainReduction(getIfGainSlider().getGR());
            getConfiguration().setAgcMode(getAgcButton().isSelected() ? AgcMode.ENABLE : AgcMode.DISABLE);

            saveConfiguration();
        }
    }

    /**
     * Updates the sample rates listed in the combobox.
     * @param sampleRates to use.
     */
    private void setSampleRates(EnumSet<RspSampleRate> sampleRates)
    {
        if(!sampleRates.isEmpty())
        {
            for(RspSampleRate sampleRate: sampleRates)
            {
                getSampleRateCombo().getItems().add(sampleRate);
            }
        }
    }

    /**
     * Removes all sample rate options from the combo box.
     */
    private void clearSampleRates()
    {
        getSampleRateCombo().getItems().clear();
    }

    /**
     * Sample rate selection combobox control
     */
    private ComboBox<RspSampleRate> getSampleRateCombo()
    {
        if(mSampleRateCombo == null)
        {
            mSampleRateCombo = new ComboBox<>();
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
                        mLog.error("Error setting sample rate for RSPduo tuner 2", se);
                    }
                }
            });
        }

        return mSampleRateCombo;
    }

    /**
     * Checkbox control for RF DAB notch
     */
    private CheckBox getRfDabNotchCheckBox()
    {
        if(mRfDabNotchCheckBox == null)
        {
            mRfDabNotchCheckBox = new CheckBox("DAB Broadcast Band Filter (157-235 MHz)");
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
                        mLog.error("Unable to set RSPduo tuner 2 RF DAB notch enabled to " + mRfDabNotchCheckBox.isSelected(), se);
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
            mRfNotchCheckBox = new CheckBox("FM Broadcast Band Filter (78-114 MHz)");
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
                        mLog.error("Unable to set RSPduo tuner 2 RF notch enabled to " + mRfNotchCheckBox.isSelected(), se);
                    }
                }
            });
        }

        return mRfNotchCheckBox;
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
                        mLog.error("Unable to set RSPduo tuner 2 AM notch enabled to " + mBiasTCheckBox.isSelected(), se);
                    }
                }
            });
        }

        return mBiasTCheckBox;
    }

    /**
     * Checkbox control for External Reference Output
     */
    private CheckBox getExternalReferenceOutputCheckBox()
    {
        if(mExternalReferenceOutputCheckBox == null)
        {
            mExternalReferenceOutputCheckBox = new CheckBox("External Reference Output");
            mExternalReferenceOutputCheckBox.setDisable(!(false));
            mExternalReferenceOutputCheckBox.setOnAction(e -> {
                if(hasTuner() && !isLoading())
                {
                    try
                    {
                        getTunerController().getControlRsp().setExternalReferenceOutput(mExternalReferenceOutputCheckBox.isSelected());
                        save();
                    }
                    catch(SDRPlayException se)
                    {
                        mLog.error("Unable to set RSPduo tuner 2 external reference output notch enabled to " +
                                mExternalReferenceOutputCheckBox.isSelected(), se);
                    }
                }
            });
        }

        return mExternalReferenceOutputCheckBox;
    }

    /**
     * Button to launch the User Preferences editor to show the RSPduo tuner preference selection.
     * @return constructed button.
     */
    private Button getTunerPreferencesButton()
    {
        if(mTunerPreferencesButton == null)
        {
            mTunerPreferencesButton = new Button("RSPduo Preferences");
            mTunerPreferencesButton.setOnAction(e -> MyEventBus.getGlobalEventBus()
                    .post(new ViewUserPreferenceEditorRequest(PreferenceEditorType.SOURCE_TUNERS)));
        }

        return mTunerPreferencesButton;
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
}
