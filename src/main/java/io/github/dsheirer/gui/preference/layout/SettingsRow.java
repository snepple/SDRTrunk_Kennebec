package io.github.dsheirer.gui.preference.layout;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * An HBox styled for a standard HIG row, with the label on the left (leading)
 * and controls on the right (trailing), pushed by a Region with Priority.ALWAYS.
 */
public class SettingsRow extends HBox {

    public SettingsRow(String labelText, Node... trailingControls) {
        getStyleClass().add("hig-settings-row");

        Label label = new Label(labelText);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        getChildren().addAll(label, spacer);
        if (trailingControls != null) {
            getChildren().addAll(trailingControls);
        }
    }
}
