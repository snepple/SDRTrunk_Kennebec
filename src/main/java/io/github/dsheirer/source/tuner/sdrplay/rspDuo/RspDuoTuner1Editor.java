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
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner.RspDuoAmPort;
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
 * RSPduo Tuner 1 Editor
 */
public class RspDuoTuner1Editor extends RspTunerEditor<RspDuoTuner1Configuration>
{
    private static final Logger mLog = LoggerFactory.getLogger(RspDuoTuner1Editor.class);

    private ComboBox<RspSampleRate> mSampleRateCombo;
    private CheckBox mRfDabNotchCheckBox;
    private CheckBox mRfNotchCheckBox;
    private CheckBox mAmNotchCheckBox;
    private CheckBox mExternalReferenceOutputCheckBox;
    private ComboBox<RspDuoAmPort> mAmPortCombo;
    private Button mTunerPreferencesButton;

    /**
     * Constructs an instance
     * @param userPreferences for settings
     * @param tunerManager for state updates
     * @param discoveredTuner to edit or control.
     */
    public RspDuoTuner1Editor(UserPreferences userPreferences, TunerManager tunerManager, DiscoveredRspTuner discoveredTuner)
    {
        super(userPreferences, tunerManager, discoveredTuner);
        init();
        tunerStatusUpdated();
    }

    private void init()
    {
        setSpacing(8);
        setPadding(new javafx.geometry.Insets(10));

        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(10);
        infoGrid.setVgap(4);
        infoGrid.add(new Label("Tuner:"), 0, 0);
        infoGrid.add(getTunerIdLabel(), 1, 0);
        infoGrid.add(getTunerPreferencesButton(), 1, 1);
        infoGrid.add(new Label("Status:"), 0, 2);
        infoGrid.add(getTunerStatusLabel(), 1, 2);
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

        GridPane gainGrid = new GridPane();
        gainGrid.setHgap(10);
        gainGrid.setVgap(4);
        gainGrid.add(new Label("Gain:"), 0, 0);
        gainGrid.add(getGainPanel(), 1, 0);
        gainGrid.add(new Label("LNA:"), 0, 1);
        gainGrid.add(getLNASlider(), 1, 1);
        gainGrid.add(new Label("IF:"), 0, 2);
        gainGrid.add(getIfGainSlider(), 1, 2);
        getChildren().add(gainGrid);

        getChildren().add(new Separator());

        getChildren().add(getExternalReferenceOutputCheckBox());
        getChildren().add(getAmNotchCheckBox());
        getChildren().add(getRfDabNotchCheckBox());
        getChildren().add(getRfNotchCheckBox());

        GridPane amPortGrid = new GridPane();
        amPortGrid.setHgap(10);
        amPortGrid.setVgap(4);
        amPortGrid.add(new Label("AM Port:"), 0, 0);
        amPortGrid.add(getAmPortCombo(), 1, 0);
        getChildren().add(amPortGrid);
    }

    /**
     * Access tuner controller
     */
    private RspDuoTuner1Controller getTunerController()
    {
        if(hasTuner())
        {
            return (RspDuoTuner1Controller) getTuner().getTunerController();
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

        clearSampleRates();
        if(hasTuner())
        {
            setSampleRates(getTunerController().getControlRsp().getSupportedSampleRates());
        }
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

        getAmPortCombo().setDisable(!(hasTuner()) && !getTuner().getTunerController().isLockedSampleRate());
        try
        {
            getAmPortCombo().setValue(hasTuner() ? getTunerController().getControlRsp().getAmPort() : null);
        }
        catch(SDRPlayException se)
        {
            mLog.error("Error setting RSPduo tuner 1 AM Port in editor");
        }

        getRfDabNotchCheckBox().setDisable(!(hasTuner()));
        try
        {
            getRfDabNotchCheckBox().setSelected(hasTuner() && getTunerController().getControlRsp().isRfDabNotch());
        }
        catch(SDRPlayException se)
        {
            mLog.error("Error setting RSPduo tuner 1 RF DAB Notch enabled state in editor");
        }

        getRfNotchCheckBox().setDisable(!(hasTuner()));
        try
        {
            getRfNotchCheckBox().setSelected(hasTuner() && getTunerController().getControlRsp().isRfNotch());
        }
        catch(SDRPlayException se)
        {
            mLog.error("Error setting RSPduo tuner 1 RF Notch enabled state in editor");
        }

        getAmNotchCheckBox().setDisable(!(hasTuner()));
        try
        {
            getAmNotchCheckBox().setSelected(hasTuner() && getTunerController().getControlRsp().isAmNotch());
        }
        catch(SDRPlayException se)
        {
            mLog.error("Error setting RSPduo tuner 1 AM Notch enabled state in editor");
        }

        getExternalReferenceOutputCheckBox().setDisable(!(hasTuner()));
        try
        {
            getExternalReferenceOutputCheckBox()
                    .setSelected(hasTuner() && getTunerController().getControlRsp().isExternalReferenceOutput());
        }
        catch(SDRPlayException se)
        {
            mLog.error("Error setting RSPduo tuner 1 external reference output enabled state in editor");
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
            getConfiguration().setAmNotch(getAmNotchCheckBox().isSelected());
            getConfiguration().setAmPort(getAmPortCombo().getValue() != null ? (RspDuoAmPort)getAmPortCombo().getValue() : null);
            getConfiguration().setExternalReferenceOutput(getExternalReferenceOutputCheckBox().isSelected());
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
                        mLog.error("Error setting sample rate for RSPduo tuner 1", se);
                    }
                }
            });
        }

        return mSampleRateCombo;
    }

    /**
     * AM port selection combobox control
     */
    private ComboBox<RspDuoAmPort> getAmPortCombo()
    {
        if(mAmPortCombo == null)
        {
            mAmPortCombo = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(RspDuoAmPort.values())))));
            mAmPortCombo.setDisable(!(false));
            mAmPortCombo.setOnAction(e -> {
                if(hasTuner() && !isLoading())
                {
                    RspDuoAmPort selected = (RspDuoAmPort)mAmPortCombo.getValue();

                    try
                    {
                        getTunerController().getControlRsp().setAmPort(selected);
                        save();
                    }
                    catch(SDRPlayException se)
                    {
                        mLog.error("Error setting AM port for RSPduo tuner 1", se);
                    }
                }
            });
        }

        return mAmPortCombo;
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
                        mLog.error("Unable to set RSPduo tuner 1 RF DAB notch enabled to " + mRfDabNotchCheckBox.isSelected(), se);
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
                        mLog.error("Unable to set RSPduo tuner 1 RF notch enabled to " + mRfNotchCheckBox.isSelected(), se);
                    }
                }
            });
        }

        return mRfNotchCheckBox;
    }

    /**
     * Checkbox control for AM notch
     */
    private CheckBox getAmNotchCheckBox()
    {
        if(mAmNotchCheckBox == null)
        {
            mAmNotchCheckBox = new CheckBox("AM Broadcast Band Filter (415-1640 kHz)");
            mAmNotchCheckBox.setDisable(!(false));
            mAmNotchCheckBox.setOnAction(e -> {
                if(hasTuner() && !isLoading())
                {
                    try
                    {
                        getTunerController().getControlRsp().setAmNotch(mAmNotchCheckBox.isSelected());
                        save();
                    }
                    catch(SDRPlayException se)
                    {
                        mLog.error("Unable to set RSPduo tuner 1 AM notch enabled to " + mAmNotchCheckBox.isSelected(), se);
                    }
                }
            });
        }

        return mAmNotchCheckBox;
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
                        mLog.error("Unable to set RSPduo tuner 1 external reference output notch enabled to " +
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
