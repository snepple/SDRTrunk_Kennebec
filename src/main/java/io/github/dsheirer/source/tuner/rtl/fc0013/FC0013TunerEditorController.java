package io.github.dsheirer.source.tuner.rtl.fc0013;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import io.github.dsheirer.source.tuner.rtl.RTL2832TunerController.SampleRate;

public class FC0013TunerEditorController {

    @FXML
    private Label tunerIdLabel;

    @FXML
    private Label tunerStatusLabel;

    @FXML
    private VBox buttonPanelContainer;

    @FXML
    private VBox frequencyPanelContainer;

    @FXML
    private ComboBox<SampleRate> sampleRateCombo;

    @FXML
    private ToggleButton biasTToggleButton;

    @FXML
    private ToggleButton agcToggleButton;

    @FXML
    private ComboBox<io.github.dsheirer.source.tuner.rtl.fc0013.FC0013EmbeddedTuner.LNAGain> lnaGainCombo;

    @FXML
    private Button changeSerialButton;

    private FC0013TunerEditor mEditor;

    public void setEditor(FC0013TunerEditor editor) {
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

    public ComboBox<SampleRate> getSampleRateCombo() {
        return sampleRateCombo;
    }

    public ToggleButton getBiasTToggleButton() {
        return biasTToggleButton;
    }

    public ToggleButton getAgcToggleButton() {
        return agcToggleButton;
    }

    public ComboBox<io.github.dsheirer.source.tuner.rtl.fc0013.FC0013EmbeddedTuner.LNAGain> getLnaGainCombo() {
        return lnaGainCombo;
    }

    public Button getChangeSerialButton() {
        return changeSerialButton;
    }
}
