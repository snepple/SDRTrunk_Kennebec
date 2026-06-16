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

package io.github.dsheirer.source.tuner.airspy.hf;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.ui.TunerEditor;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.application.Platform;
import java.util.Optional;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;


/**
 * Tuner editor for Airspy HF+/Discovery tuners
 */
public class AirspyHfTunerEditor extends TunerEditor<AirspyHfTuner,AirspyHfTunerConfiguration>
{
    private static final Logger mLog = LoggerFactory.getLogger(AirspyHfTunerEditor.class);
    private ComboBox<AirspyHfSampleRate> mSampleRateCombo;
    private ComboBox<Attenuation> mAttenuationCombo;
    private ToggleButton mAgcToggleButton;
    private ToggleButton mLnaToggleButton;

    /**
     * Constructs an instance
     *
     * @param userPreferences
     * @param tunerManager for requesting configuration saves.
     * @param discoveredTuner
     */
    public AirspyHfTunerEditor(UserPreferences userPreferences, TunerManager tunerManager, DiscoveredTuner discoveredTuner)
    {
        super(userPreferences, tunerManager, discoveredTuner);
        init();
        tunerStatusUpdated();
    }

    @Override
    public long getMinimumTunableFrequency()
    {
        return AirspyHfTunerController.MINIMUM_TUNABLE_FREQUENCY_HZ;
    }

    @Override
    public long getMaximumTunableFrequency()
    {
        return AirspyHfTunerController.MAXIMUM_TUNABLE_FREQUENCY_HZ;
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

        getChildren().add(new Separator());

        getChildren().add(getAgcToggleButton());
        getChildren().add(getLnaToggleButton());

        GridPane attGrid = new GridPane();
        attGrid.setHgap(10);
        attGrid.setVgap(4);
        attGrid.add(new Label("Attenuation:"), 0, 0);
        attGrid.add(getAttenuationCombo(), 1, 0);
        getChildren().add(attGrid);
    }

    @Override
    protected void save()
    {
        if(hasConfiguration() && !isLoading())
        {
            getConfiguration().setFrequency(getFrequencyControl().getFrequency());
            getConfiguration().setMinimumFrequency(getMinimumFrequencyTextField().getFrequency());
            getConfiguration().setMaximumFrequency(getMaximumFrequencyTextField().getFrequency());
            double value = (Double) getFrequencyCorrectionSpinner().getValue();
            getConfiguration().setFrequencyCorrection(value);
            getConfiguration().setAutoPPMCorrectionEnabled(getAutoPPMCheckBox().isSelected());
            getConfiguration().setSampleRate((int)getTuner().getController().getSampleRate());

            Attenuation attenuation = (Attenuation)getAttenuationCombo().getValue();
            getConfiguration().setAttenuation(attenuation);
            getConfiguration().setAgc(getAgcToggleButton().isSelected());
            getConfiguration().setLna(getLnaToggleButton().isSelected());

            saveConfiguration();
        }
    }

    @Override
    protected void tunerStatusUpdated()
    {
        setLoading(true);

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
            getTunerIdLabel().setText("Airspy " + getTuner().getController().getBoardId() + " SER# " + getTuner().getController().getSerialNumber() + getUsbInfo()); //


            //Permanently disable the sample rates combo and force 912kHz usage - during dev testing, attempts to use
            //the other sample rates produced inconsistent results.
            getSampleRateCombo().setDisable(!(false));
            getSampleRateCombo().getItems().clear();
            for(AirspyHfSampleRate sampleRate: getTuner().getController().getAvailableSampleRates())
            {
                getSampleRateCombo().getItems().add(sampleRate);
            }
            getAgcToggleButton().setDisable(!(true));
            getAgcToggleButton().setSelected(getTuner().getController().getAgc());
            getLnaToggleButton().setDisable(!(true));
            getLnaToggleButton().setSelected(getTuner().getController().getLna());
            getSampleRateCombo().setValue(getTuner().getController().getCurrentAirspySampleRate());
            getAttenuationCombo().setDisable(!(true));
            getAttenuationCombo().setValue(getTuner().getController().getAttenuation());
        }
        else
        {
            getTunerIdLabel().setText("Airspy HF+" + getUsbInfo());
            getAgcToggleButton().setDisable(!(false));
            getAgcToggleButton().setSelected(false);
            getLnaToggleButton().setDisable(!(false));
            getLnaToggleButton().setSelected(false);
            getSampleRateCombo().setDisable(!(false));
            getAttenuationCombo().setDisable(!(false));
            getAttenuationCombo().setValue(Attenuation.A0);
        }

        updateSampleRateToolTip();

        setLoading(false);
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

    /**
     * Sample rate combo for selecting among available sample rates.
     * @return sample rate combo
     */
    private ComboBox<AirspyHfSampleRate> getSampleRateCombo()
    {
        if(mSampleRateCombo == null)
        {
            mSampleRateCombo = new ComboBox<>();
            mSampleRateCombo.setDisable(!(false));
            mSampleRateCombo.setOnAction(e ->
            {
                if(!isLoading())
                {
                    final AirspyHfSampleRate sampleRate = (AirspyHfSampleRate)mSampleRateCombo.getValue();

                    if(sampleRate != null)
                    {
                        //Adjust the min/max values for the sample rate.
                        adjustForSampleRate(sampleRate.getSampleRate());

                        save();
                        applyDeviceControl("airspyhf-sample-rate", () -> getTuner().getController().setSampleRate(sampleRate), "Error setting Airspy Hf Sample Rate [" + sampleRate + "]");
                    }
                }
            });
        }

        return mSampleRateCombo;
    }

    /**
     * Combobox with available attenuation items
     * @return combo box
     */
    private ComboBox<Attenuation> getAttenuationCombo()
    {
        if(mAttenuationCombo == null)
        {
            mAttenuationCombo = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(Attenuation.values())))));
            mAttenuationCombo.setDisable(!(false));
            mAttenuationCombo.setOnAction(e -> {
                if(!isLoading())
                {
                    final Attenuation selected = (Attenuation)mAttenuationCombo.getValue();
                    save();
                    applyDeviceControl("airspyhf-attenuation", () -> getTuner().getController().setAttenuation(selected), "Error setting Airspy Hf attenuation [" + selected + "]");
                }
            });
        }

        return mAttenuationCombo;
    }

    /**
     * Automatic Gain Control (AGC) toggle button
     */
    private ToggleButton getAgcToggleButton()
    {
        if(mAgcToggleButton == null)
        {
            mAgcToggleButton = new ToggleButton("AGC");
            mAgcToggleButton.setTooltip(new javafx.scene.control.Tooltip("Automatic Gain Control"));
            mAgcToggleButton.setDisable(!(false));
            mAgcToggleButton.setOnAction(e -> {
                if(!isLoading())
                {
                    final boolean agc = mAgcToggleButton.isSelected();
                    save();
                    applyDeviceControl("airspyhf-agc", () -> getTuner().getController().setAgc(agc), "Error setting Airspy HF AGC");
                }
            });
        }

        return mAgcToggleButton;
    }

    /**
     * Low Noise Amplifier (LNA) toggle button
     */
    private ToggleButton getLnaToggleButton()
    {
        if(mLnaToggleButton == null)
        {
            mLnaToggleButton = new ToggleButton("LNA");
            mLnaToggleButton.setTooltip(new javafx.scene.control.Tooltip("Low Noise Amplifier"));
            mLnaToggleButton.setDisable(!(false));
            mLnaToggleButton.setOnAction(e -> {
                if(!isLoading())
                {
                    final boolean lna = mLnaToggleButton.isSelected();
                    save();
                    applyDeviceControl("airspyhf-lna", () -> getTuner().getController().setLna(lna), "Error setting Airspy HF LNA");
                }
            });
        }

        return mLnaToggleButton;
    }
}
