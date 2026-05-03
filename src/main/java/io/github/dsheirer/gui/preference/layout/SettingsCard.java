package io.github.dsheirer.gui.preference.layout;

import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;

/**
 * A reusable VBox styled as a rounded white card that automatically adds separators between its children,
 * matching Apple's Human Interface Guidelines.
 */
public class SettingsCard extends VBox {

    private boolean mUpdatingSeparators = false;

    public SettingsCard() {
        getStyleClass().add("hig-settings-card");

        // Listen for changes to children to automatically insert/manage separators
        getChildren().addListener((ListChangeListener<Node>) c -> {
            if (mUpdatingSeparators) {
                return;
            }
            while (c.next()) {
                if (c.wasAdded() || c.wasRemoved()) {
                    mUpdatingSeparators = true;
                    try {
                        updateSeparators();
                    } finally {
                        mUpdatingSeparators = false;
                    }
                }
            }
        });
    }

    private void updateSeparators() {
        // Remove existing separators to rebuild them correctly
        getChildren().removeIf(node -> node instanceof Separator);

        int size = getChildren().size();
        // Iterate backwards so we can insert without messing up indices of remaining items to process
        for (int i = size - 1; i > 0; i--) {
            Separator separator = new Separator();
            separator.getStyleClass().add("hig-separator");
            getChildren().add(i, separator);
        }
    }
}
