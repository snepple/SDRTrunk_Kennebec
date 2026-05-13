package io.github.dsheirer.gui.help;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class HelpViewer extends JFXPanel {

    private static final Logger mLog = LoggerFactory.getLogger(HelpViewer.class);

    public HelpViewer() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/HelpView.fxml"));
                Parent root = loader.load();
                Scene scene = new Scene(root);
                java.net.URL cssUrl = getClass().getResource("/sdrtrunk_style.css");
                if (cssUrl != null) {
                    scene.getStylesheets().add(cssUrl.toExternalForm());
                }
                setScene(scene);
            } catch (IOException e) {
                mLog.error("Error loading HelpView.fxml", e);
            }
        });
    }
}
