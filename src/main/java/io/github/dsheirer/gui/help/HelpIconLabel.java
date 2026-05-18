package io.github.dsheirer.gui.help;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;

import java.awt.Dimension;

public class HelpIconLabel extends JFXPanel {

    public HelpIconLabel(String htmlHelpText) {
        final String cleanText = htmlHelpText.replace("<html>", "").replace("</html>", "").replace("<b>", "").replace("</b>", "").replace("<br>", "\n");

        setPreferredSize(new Dimension(20, 20));

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/io/github/dsheirer/gui/help/HelpIconLabel.fxml"));
                StackPane root = loader.load();
                HelpIconLabelController controller = loader.getController();
                controller.setHelpText(cleanText);

                Scene scene = new Scene(root);
                scene.setFill(null);

                setScene(scene);
                setBackground(new java.awt.Color(0, 0, 0, 0)); // transparent swing bg
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
