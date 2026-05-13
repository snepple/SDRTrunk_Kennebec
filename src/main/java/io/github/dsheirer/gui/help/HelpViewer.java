package io.github.dsheirer.gui.help;

import javafx.application.Platform;
import javafx.scene.layout.VBox;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class HelpViewer extends VBox {

    private static final Logger mLog = LoggerFactory.getLogger(HelpViewer.class);

    public HelpViewer() {
                try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/HelpView.fxml"));
            Parent root = loader.load();
            this.getChildren().add(root);
            java.net.URL cssUrl = getClass().getResource("/sdrtrunk_style.css");
            if (cssUrl != null) {
                this.getStylesheets().add(cssUrl.toExternalForm());
            }
        } catch (IOException e) {
            mLog.error("Error loading HelpView.fxml", e);
        }
    }
}
