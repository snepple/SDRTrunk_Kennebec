package io.github.dsheirer.gui.help;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;

public class HelpIconLabelController {
    @FXML
    private Label fxLabel;
    @FXML
    private Tooltip fxTooltip;

    public void setHelpText(String text) {
        if (fxTooltip != null) {
            fxTooltip.setText(text);
        }
    }
}
