package io.github.dsheirer.gui.help;

import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
// We can just use text since kordamp is not available
public class HelpIconLabelFactory {
    public static Label createHelpIcon(String tooltipText) {
        Label label = new Label("?");
        label.setStyle("-fx-text-fill: gray; -fx-font-weight: bold; -fx-padding: 0 0 0 5;");
        Tooltip tooltip = new Tooltip(tooltipText);
        label.setTooltip(tooltip);
        return label;
    }
}
