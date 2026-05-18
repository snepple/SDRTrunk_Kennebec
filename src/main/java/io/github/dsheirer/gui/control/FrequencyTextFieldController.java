package io.github.dsheirer.gui.control;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import java.util.function.UnaryOperator;

public class FrequencyTextFieldController {
    @FXML
    public TextField fxTextField;

    public void setupFilter(UnaryOperator<TextFormatter.Change> filter) {
        if (fxTextField != null) {
            TextFormatter<String> textFormatter = new TextFormatter<>(filter);
            fxTextField.setTextFormatter(textFormatter);
        }
    }
}
