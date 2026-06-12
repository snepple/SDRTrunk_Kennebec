package io.github.dsheirer.gui;

import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.scene.Node;

public class DockWindowManager {
    
    public interface DockCallback {
        void onRestore();
    }

    public static void popOut(Node node, String title, Pane originalParent, DockCallback callback) {
        if (originalParent != null) {
            originalParent.getChildren().remove(node);
        }

        Stage stage = new Stage();
        stage.setTitle(title);
        JavaFxWindowManager.applyApplicationIcon(stage);
        
        BorderPane root = new BorderPane();
        root.setCenter(node);
        
        Scene scene = new Scene(root, 800, 600);
        io.github.dsheirer.gui.theme.ThemeManager.registerScene(scene);
        
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> {
            root.getChildren().remove(node);
            if (originalParent != null) {
                originalParent.getChildren().add(node);
            }
            if (callback != null) {
                callback.onRestore();
            }
        });
        stage.show();
    }
}
