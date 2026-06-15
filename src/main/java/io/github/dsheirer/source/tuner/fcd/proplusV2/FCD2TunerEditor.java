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
package io.github.dsheirer.source.tuner.fcd.proplusV2;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.fcd.FCDTuner;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.ui.TunerEditor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.geometry.Insets;
import javafx.scene.layout.GridPane;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import io.github.dsheirer.gui.control.ToggleSwitch;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.application.Platform;
import java.util.Optional;
import javafx.scene.control.Separator;


/**
 * Funcube Dongle Pro Plus tuner editor
 */
public class FCD2TunerEditor extends TunerEditor<FCDTuner, FCD2TunerConfiguration>
{
    private final static Logger mLog = LoggerFactory.getLogger(FCD2TunerEditor.class);
    private static final long serialVersionUID = 1L;
    private CheckBox mLnaGainCheckBox;
    private CheckBox mMixerGainCheckBox;

    /**
     * Constructs an instance
     * @param userPreferences for wide-band recordings
     * @param tunerManager to save configuration
     * @param discoveredTuner to control
     */
    public FCD2TunerEditor(UserPreferences userPreferences, TunerManager tunerManager, DiscoveredTuner discoveredTuner)
    {
        super(userPreferences, tunerManager, discoveredTuner);
        init();
        tunerStatusUpdated();
    }

    private FCD2TunerController getController()
    {
        if(hasTuner())
        {
            return (FCD2TunerController)getTuner().getController();
        }

        return null;
    }

    @Override
    public long getMinimumTunableFrequency()
    {
        return FCD2TunerController.MINIMUM_TUNABLE_FREQUENCY_HZ;
    }

    @Override
    public long getMaximumTunableFrequency()
    {
        return FCD2TunerController.MAXIMUM_TUNABLE_FREQUENCY_HZ;
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
        getLnaGainCheckBox().setDisable(!(hasTuner()));
        getMixerGainCheckBox().setDisable(!(hasTuner()));

        if(hasTuner() && hasConfiguration())
        {
            getLnaGainCheckBox().setSelected(getConfiguration().getGainLNA());
            getMixerGainCheckBox().setSelected(getConfiguration().getGainMixer());
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
        getChildren().add(freqGrid);

        getChildren().add(new Separator());
        getChildren().add(getLnaGainCheckBox());
        getChildren().add(getMixerGainCheckBox());
    }

public CheckBox getLnaGainCheckBox()
    {
        if(mLnaGainCheckBox == null)
        {
            mLnaGainCheckBox = new CheckBox("LNA Gain");
            mLnaGainCheckBox.setDisable(!(false));
            mLnaGainCheckBox.setOnAction(event ->
            {
                if(!isLoading())
                {
                    try
                    {
                        getController().setLNAGain(getLnaGainCheckBox().isSelected());
                        save();
                    }
                    catch(SourceException e)
                    {
                        mLog.error("Couldn't set LNA gain for FCD2", e);
                    }
                }
            });
        }

        return mLnaGainCheckBox;
    }

    public CheckBox getMixerGainCheckBox()
    {
        if(mMixerGainCheckBox == null)
        {
            mMixerGainCheckBox = new CheckBox("Mixer Gain");
            mMixerGainCheckBox.setDisable(!(false));
            mMixerGainCheckBox.setOnAction(event ->
            {
                if(!isLoading())
                {
                    try
                    {
                        getController().setMixerGain(getMixerGainCheckBox().isSelected());
                        save();
                    }
                    catch(SourceException e)
                    {
                        mLog.error("Couldn't set mixer gain for FCD2", e);
                    }
                }
            });
        }

        return mMixerGainCheckBox;
    }

    @Override
    public void setTunerLockState(boolean locked)
    {
        getFrequencyPanel().updateControls();
    }

    protected String getTunerInfo()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("<html><h3>Funcube Dongle Pro Plus Tuner</h3>");

        if(hasTuner())
        {
            sb.append("<b>USB ID: </b>");
            sb.append(getController().getUSBID());
            sb.append("<br>");

            sb.append("<b>USB Address: </b>");
            sb.append(getController().getUSBAddress());
            sb.append("<br>");

            sb.append("<b>USB Speed: </b>");
            sb.append(getController().getUSBSpeed());
            sb.append("<br>");

            sb.append("<b>Cellular Band: </b>");
            sb.append(getController().getConfiguration().getBandBlocking());
            sb.append("<br>");

            sb.append("<b>Firmware: </b>");
            sb.append(getController().getConfiguration().getFirmware());
            sb.append("<br>");
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
            getConfiguration().setGainLNA(getLnaGainCheckBox().isSelected());
            getConfiguration().setGainMixer(getMixerGainCheckBox().isSelected());
            saveConfiguration();
        }
    }
}