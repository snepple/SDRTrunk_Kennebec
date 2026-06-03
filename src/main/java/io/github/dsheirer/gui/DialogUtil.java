
package io.github.dsheirer.gui;
import javafx.scene.control.Button;

import io.github.dsheirer.gui.theme.ThemeManager;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;
import java.util.Optional;

public class DialogUtil {

    private static void applyTheme(Alert alert) {
        ThemeManager.applyCurrentTheme(alert.getDialogPane());
    }

    public static void showError(Window owner, String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        if (owner != null) alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        applyTheme(alert);
        alert.showAndWait();
    }

    public static void showWarning(Window owner, String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        if (owner != null) alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        applyTheme(alert);
        alert.showAndWait();
    }

    public static void showInfo(Window owner, String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        if (owner != null) alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        applyTheme(alert);
        alert.showAndWait();
    }

    public static boolean showConfirmation(Window owner, String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        if (owner != null) alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        applyTheme(alert);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}
