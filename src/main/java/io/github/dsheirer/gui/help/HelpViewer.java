package io.github.dsheirer.gui.help;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class HelpViewer extends BorderPane {

    private static final Logger mLog = LoggerFactory.getLogger(HelpViewer.class);

    public HelpViewer() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/HelpView.fxml"));
            Parent root = loader.load();
            setCenter(root);
        } catch (IOException e) {
            mLog.error("Error loading HelpView.fxml", e);
        }
    }
}
