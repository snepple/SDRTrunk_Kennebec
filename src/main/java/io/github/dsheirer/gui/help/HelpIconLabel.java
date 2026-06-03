
package io.github.dsheirer.gui.help;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.scene.control.Label;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;

import java.awt.Dimension;

public class HelpIconLabel extends javafx.scene.layout.Pane {
    private static final Logger mLog = LoggerFactory.getLogger(HelpIconLabel.class);

    public HelpIconLabel(String htmlHelpText) {
        final String cleanText = htmlHelpText.replace("<html>", "").replace("</html>", "").replace("<b>", "").replace("</b>", "").replace("<br>", "\n");

        setPrefSize(100, 100); // new Dimension(20, 20));

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/io/github/dsheirer/gui/help/HelpIconLabel.fxml"));
                StackPane root = loader.load();
                HelpIconLabelController controller = loader.getController();
                controller.setHelpText(cleanText);

                Scene scene = new Scene(root);
                scene.setFill(null);

                /*  setScene(scene); */
                setBackground(javafx.scene.layout.Background.EMPTY); // transparent swing bg
            } catch (Exception e) {
                mLog.error("Error initializing help icon", e);
            }
        });
    }
    public void setHelpText(String text) {}
}

