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
package io.github.dsheirer.source.tuner.fcd.proV1;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.fcd.FCDTuner;
import io.github.dsheirer.source.tuner.fcd.proV1.FCD1TunerController.LNAEnhance;
import io.github.dsheirer.source.tuner.fcd.proV1.FCD1TunerController.LNAGain;
import io.github.dsheirer.source.tuner.fcd.proV1.FCD1TunerController.MixerGain;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.ui.TunerEditor;
import java.text.DecimalFormat;

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
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;




/**
 * Funcube Dongle Pro tuner editor
 */
public class FCD1TunerEditor extends TunerEditor<FCDTuner,FCD1TunerConfiguration>
{
    private static final long serialVersionUID = 1L;
    private final static Logger mLog = LoggerFactory.getLogger(FCD1TunerEditor.class);
    private ComboBox<LNAGain> mLnaGainCombo;
    private ComboBox<LNAEnhance> mLnaEnhanceCombo;
    private ComboBox<MixerGain> mMixerGainCombo;
    private CorrectionSpinner mCorrectionDCI;
    private CorrectionSpinner mCorrectionDCQ;
    private CorrectionSpinner mGainCorrectionSpinner;
    private CorrectionSpinner mPhaseCorrectionSpinner;

    /**
     * Constructs an instance
     * @param userPreferences for wide-band recordings
     * @param tunerManager to save configuration
     * @param discoveredTuner to control
     */
    public FCD1TunerEditor(UserPreferences userPreferences, TunerManager tunerManager, DiscoveredTuner discoveredTuner)
    {
        super(userPreferences, tunerManager, discoveredTuner);
        init();
        tunerStatusUpdated();
    }

    private FCD1TunerController getController()
    {
        if(hasTuner())
        {
            return (FCD1TunerController)getTuner().getController();
        }

        return null;
    }

    @Override
    public long getMinimumTunableFrequency()
    {
        return FCD1TunerController.MINIMUM_TUNABLE_FREQUENCY_HZ;
    }

    @Override
    public long getMaximumTunableFrequency()
    {
        return FCD1TunerController.MAXIMUM_TUNABLE_FREQUENCY_HZ;
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

        if(hasTuner() && hasConfiguration())
        {
            getMixerGainCombo().setValue(getConfiguration().getMixerGain());
            getLnaEnhanceCombo().setValue(getConfiguration().getLNAEnhance());
            getLnaGainCombo().setValue(getConfiguration().getLNAGain());
            getDcCorrectionSpinnerI().getValueFactory().setValue(getConfiguration().getInphaseDCCorrection());
            getDcCorrectionSpinnerQ().getValueFactory().setValue(getConfiguration().getQuadratureDCCorrection());
            getPhaseCorrectionSpinner().getValueFactory().setValue(getConfiguration().getGainCorrection());
            getGainCorrectionSpinner().getValueFactory().setValue(getConfiguration().getPhaseCorrection());
        }

        getMixerGainCombo().setDisable(!(hasTuner()));
        getLnaEnhanceCombo().setDisable(!(hasTuner()));
        getLnaGainCombo().setDisable(!(hasTuner()));
        getDcCorrectionSpinnerI().setDisable(!(hasTuner()));
        getDcCorrectionSpinnerQ().setDisable(!(hasTuner()));
        getPhaseCorrectionSpinner().setDisable(!(hasTuner()));
        getGainCorrectionSpinner().setDisable(!(hasTuner()));

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
        getChildren().add(new Label("Gain"));

        GridPane gainGrid = new GridPane();
        gainGrid.setHgap(10);
        gainGrid.setVgap(4);
        gainGrid.add(new Label("LNA:"), 0, 0);
        gainGrid.add(getLnaGainCombo(), 1, 0);
        gainGrid.add(new Label("Enhance:"), 0, 1);
        gainGrid.add(getLnaEnhanceCombo(), 1, 1);
        gainGrid.add(new Label("Mixer:"), 0, 2);
        gainGrid.add(getMixerGainCombo(), 1, 2);
        getChildren().add(gainGrid);

        getChildren().add(new Separator());
        getChildren().add(new Label("Correction"));

        GridPane corrGrid = new GridPane();
        corrGrid.setHgap(10);
        corrGrid.setVgap(4);
        corrGrid.add(new Label("DC Inphase:"), 0, 0);
        corrGrid.add(getDcCorrectionSpinnerI(), 1, 0);
        corrGrid.add(new Label("DC Quadrature:"), 0, 1);
        corrGrid.add(getDcCorrectionSpinnerQ(), 1, 1);
        corrGrid.add(new Label("Gain:"), 0, 2);
        corrGrid.add(getGainCorrectionSpinner(), 1, 2);
        corrGrid.add(new Label("Phase:"), 0, 3);
        corrGrid.add(getPhaseCorrectionSpinner(), 1, 3);
        getChildren().add(corrGrid);
    }

    public ComboBox getLnaGainCombo()
    {
        if(mLnaGainCombo == null)
        {
            mLnaGainCombo = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(LNAGain.values())))));
            mLnaGainCombo.setDisable(!(false));
            mLnaGainCombo.setTooltip(new javafx.scene.control.Tooltip("Adjust the low noise amplifier gain setting."));
            mLnaGainCombo.setOnAction(arg0 ->
            {
                if(hasTuner() && !isLoading())
                {
                    LNAGain gain = (LNAGain) mLnaGainCombo.getValue();
                    save();
                    applyDeviceControl("fcd1-lna-gain", () -> {
                        try { getController().setLNAGain(gain); }
                        catch(Exception ex) { throw new RuntimeException(ex); }
                    }, "FuncubeDonglePro Controller - error setting LNA gain [" + gain + "]");
                }
            });
        }

        return mLnaGainCombo;
    }

    public ComboBox getLnaEnhanceCombo()
    {
        if(mLnaEnhanceCombo == null)
        {
            mLnaEnhanceCombo = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(LNAEnhance.values())))));
            mLnaEnhanceCombo.setDisable(!(false));
            mLnaEnhanceCombo.setTooltip(new javafx.scene.control.Tooltip("Adjust the LNA enhance setting.  Default value is OFF"));
            mLnaEnhanceCombo.setOnAction(arg0 ->
            {
                if(hasTuner() && !isLoading())
                {
                    LNAEnhance enhance = (LNAEnhance) mLnaEnhanceCombo.getValue();
                    save();
                    applyDeviceControl("fcd1-lna-enhance", () -> {
                        try { getController().setLNAEnhance(enhance); }
                        catch(Exception ex) { throw new RuntimeException(ex); }
                    }, "FCDPro - error setting LNA enhance [" + enhance + "]");
                }
            });
        }

        return mLnaEnhanceCombo;
    }

    public ComboBox getMixerGainCombo()
    {
        if(mMixerGainCombo == null)
        {
            mMixerGainCombo = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(javafx.collections.FXCollections.observableArrayList(MixerGain.values())))));
            mMixerGainCombo.setDisable(!(false));
            mMixerGainCombo.setTooltip(new javafx.scene.control.Tooltip("Adjust mixer gain setting"));
            mMixerGainCombo.setOnAction(arg0 ->
            {
                if(hasTuner() && !isLoading())
                {
                    MixerGain gain = (MixerGain) mMixerGainCombo.getValue();
                    save();
                    applyDeviceControl("fcd1-mixer-gain", () -> {
                        try { getController().setMixerGain(gain); }
                        catch(Exception ex) { throw new RuntimeException(ex); }
                    }, "FCDPro - error setting mixer gain [" + gain + "]");
                }
            });
        }

        return mMixerGainCombo;
    }

    public CorrectionSpinner getDcCorrectionSpinnerQ()
    {
        if(mCorrectionDCQ == null)
        {
            mCorrectionDCQ = new CorrectionSpinner(Correction.DC_QUADRATURE, 0.0, 0.00001, 5);
            mCorrectionDCQ.setDisable(!(false));
            mCorrectionDCQ.setTooltip(new javafx.scene.control.Tooltip("DC Bias Correction/Quadrature Component: valid values are -1.0 to 1.0 (default: 0.0)"));
        }

        return mCorrectionDCQ;
    }

    public CorrectionSpinner getDcCorrectionSpinnerI()
    {
        if(mCorrectionDCI == null)
        {
            mCorrectionDCI = new CorrectionSpinner(Correction.DC_INPHASE, 0.0, 0.00001, 5);
            mCorrectionDCI.setDisable(!(false));
            mCorrectionDCI.setTooltip(new javafx.scene.control.Tooltip("DC Bias Correction/Inphase Component: valid values are -1.0 to 1.0 (default: 0.0)"));
        }

        return mCorrectionDCI;
    }

    public CorrectionSpinner getGainCorrectionSpinner()
    {
        if(mGainCorrectionSpinner == null)
        {
            mGainCorrectionSpinner = new CorrectionSpinner(Correction.GAIN, 0.0, 0.00001, 5);
            mGainCorrectionSpinner.setDisable(!(false));
            mGainCorrectionSpinner.setTooltip(new javafx.scene.control.Tooltip("Gain Correction: valid values are -1.0 to 1.0 (default: 0.0)"));
        }

        return mGainCorrectionSpinner;
    }

    public CorrectionSpinner getPhaseCorrectionSpinner()
    {
        if(mPhaseCorrectionSpinner == null)
        {
            mPhaseCorrectionSpinner = new CorrectionSpinner(Correction.PHASE, 0.0, 0.00001, 5);
            mPhaseCorrectionSpinner.setDisable(!(false));
            mPhaseCorrectionSpinner.setTooltip(new javafx.scene.control.Tooltip("Phase Correction: valid values are -1.0 to 1.0 (default: 0.0)"));
        }

        return mPhaseCorrectionSpinner;
    }

    @Override
    public void setTunerLockState(boolean locked)
    {
        getFrequencyPanel().updateControls();
    }

protected String getTunerInfo()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("<html><h3>Funcube Dongle Pro Tuner</h3>");

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
            getConfiguration().setLNAEnhance((LNAEnhance) mLnaEnhanceCombo.getValue());
            getConfiguration().setLNAGain((LNAGain) mLnaGainCombo.getValue());
            getConfiguration().setMixerGain((MixerGain) mMixerGainCombo.getValue());
            double dci = (Double)mCorrectionDCI.getValue();
            getConfiguration().setInphaseDCCorrection(dci);
            double dcq = (Double)mCorrectionDCQ.getValue();
            getConfiguration().setQuadratureDCCorrection(dcq);
            double gain = (Double) mGainCorrectionSpinner.getValue();
            getConfiguration().setGainCorrection(gain);
            double phase = (Double) mPhaseCorrectionSpinner.getValue();
            getConfiguration().setPhaseCorrection(phase);
            saveConfiguration();
        }
    }

    public enum Correction {GAIN, PHASE, DC_INPHASE, DC_QUADRATURE};

    public class CorrectionSpinner extends Spinner
    {
        private static final long serialVersionUID = 1L;
        private static final double MIN_VALUE = -1.0d;
        private static final double MAX_VALUE = 1.0d;

        private Correction mCorrectionComponent;

        public CorrectionSpinner(Correction component, double initialValue, double step, int decimalPlaces)
        {
            mCorrectionComponent = component;

            SpinnerValueFactory model = new javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory((int)MIN_VALUE, (int)MAX_VALUE, (int)initialValue, (int)step);
            setValueFactory(model);

            // // Spinner.NumberEditor editor = (Spinner.NumberEditor)getEditor();

            // DecimalFormat format = editor.getFormat();
            // format.setMinimumFractionDigits(decimalPlaces);

            // editor.getTextField().setHorizontalAlignment...

//             // addChangeListener(e ->
//             {
//                 if(!isLoading())
//                 {
//                     double value = 0; // (Double)getModel().getValue();
// 
//                     try
//                     {
//                         switch(mCorrectionComponent)
//                         {
//                             case DC_INPHASE:
//                                 getController().setDCCorrectionInPhase(value);
//                                 break;
//                             case DC_QUADRATURE:
//                                 getController().setDCCorrectionQuadrature(value);
//                                 break;
//                             case GAIN:
//                                 getController().setGainCorrection(value);
//                                 break;
//                             case PHASE:
//                                 getController().setPhaseCorrection(value);
//                                 break;
//                         }
// 
//                         save();
//                     }
//                     catch(Exception e1)
//                     {
//                         Platform.runLater(() -> { Alert alert = new Alert(Alert.AlertType.INFORMATION); io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane()); alert.setContentText(String.valueOf("FCDPro - error "
//                                 + "applying " + mCorrectionComponent.toString() + " correction value [" + value + "]")); alert.showAndWait(); });
// 
//                         mLog.error("FCDPro - error applying " + mCorrectionComponent.toString() + " correction value [" +
//                                 value + "]", e1);
//                     }
//                 }
//             });
//         }
    }
}
}
