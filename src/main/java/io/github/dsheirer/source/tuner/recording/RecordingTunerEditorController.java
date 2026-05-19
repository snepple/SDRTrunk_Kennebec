package io.github.dsheirer.source.tuner.recording;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import io.github.dsheirer.source.tuner.ui.TunerEditor;

public class RecordingTunerEditorController {

    @FXML
    private Label tunerIdLabel;

    @FXML
    private Label tunerStatusLabel;

    @FXML
    private Label recordingPathLabel;

    @FXML
    private VBox buttonPanelContainer;

    @FXML
    private VBox frequencyPanelContainer;

    private RecordingTunerEditor mEditor;

    public void setEditor(RecordingTunerEditor editor) {
        mEditor = editor;
    }

    public Label getTunerIdLabel() {
        return tunerIdLabel;
    }

    public Label getTunerStatusLabel() {
        return tunerStatusLabel;
    }

    public Label getRecordingPathLabel() {
        return recordingPathLabel;
    }

    public VBox getButtonPanelContainer() {
        return buttonPanelContainer;
    }

    public VBox getFrequencyPanelContainer() {
        return frequencyPanelContainer;
    }
}
