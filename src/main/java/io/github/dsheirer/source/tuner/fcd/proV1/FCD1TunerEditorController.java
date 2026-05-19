package io.github.dsheirer.source.tuner.fcd.proV1;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.VBox;
import io.github.dsheirer.source.tuner.fcd.proV1.FCD1TunerController.LNAGain;
import io.github.dsheirer.source.tuner.fcd.proV1.FCD1TunerController.LNAEnhance;
import io.github.dsheirer.source.tuner.fcd.proV1.FCD1TunerController.MixerGain;

public class FCD1TunerEditorController {

    @FXML
    private Label tunerIdLabel;

    @FXML
    private Label tunerStatusLabel;

    @FXML
    private VBox buttonPanelContainer;

    @FXML
    private VBox frequencyPanelContainer;

    @FXML
    private ComboBox<LNAGain> lnaGainCombo;

    @FXML
    private ComboBox<LNAEnhance> lnaEnhanceCombo;

    @FXML
    private ComboBox<MixerGain> mixerGainCombo;

    @FXML
    private Spinner<Double> dcCorrectionSpinnerI;

    @FXML
    private Spinner<Double> dcCorrectionSpinnerQ;

    @FXML
    private Spinner<Double> gainCorrectionSpinner;

    @FXML
    private Spinner<Double> phaseCorrectionSpinner;

    private FCD1TunerEditor mEditor;

    public void setEditor(FCD1TunerEditor editor) {
        mEditor = editor;
    }

    public Label getTunerIdLabel() {
        return tunerIdLabel;
    }

    public Label getTunerStatusLabel() {
        return tunerStatusLabel;
    }

    public VBox getButtonPanelContainer() {
        return buttonPanelContainer;
    }

    public VBox getFrequencyPanelContainer() {
        return frequencyPanelContainer;
    }

    public ComboBox<LNAGain> getLnaGainCombo() {
        return lnaGainCombo;
    }

    public ComboBox<LNAEnhance> getLnaEnhanceCombo() {
        return lnaEnhanceCombo;
    }

    public ComboBox<MixerGain> getMixerGainCombo() {
        return mixerGainCombo;
    }

    public Spinner<Double> getDcCorrectionSpinnerI() {
        return dcCorrectionSpinnerI;
    }

    public Spinner<Double> getDcCorrectionSpinnerQ() {
        return dcCorrectionSpinnerQ;
    }

    public Spinner<Double> getGainCorrectionSpinner() {
        return gainCorrectionSpinner;
    }

    public Spinner<Double> getPhaseCorrectionSpinner() {
        return phaseCorrectionSpinner;
    }
}
