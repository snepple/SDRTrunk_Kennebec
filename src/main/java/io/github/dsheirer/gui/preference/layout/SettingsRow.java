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

        label.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        getChildren().addAll(label, spacer);
        if (trailingControls != null) {
            for (Node n : trailingControls) {
                if (n instanceof javafx.scene.control.Button) {
                    ((javafx.scene.control.Button) n).setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
                } else if (n instanceof Label) {
                    Label l = (Label) n;
                    l.setWrapText(true);
                    l.setStyle("-fx-font-size: 0.9em;");
                }
            }
            getChildren().addAll(trailingControls);
        }
    }
}
