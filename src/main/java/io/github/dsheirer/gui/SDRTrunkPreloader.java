package io.github.dsheirer.gui;

import javafx.application.Preloader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SDRTrunkPreloader extends Preloader {
    private static final Logger mLog = LoggerFactory.getLogger(SDRTrunkPreloader.class);
    private Stage preloaderStage;
    private ProgressBar progressBar;
    private Label progressLabel;

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.preloaderStage = primaryStage;

        // Set Icon for preloader to fix Windows taskbar icon issue
        try {
            primaryStage.getIcons().addAll(
                new Image(SDRTrunkPreloader.class.getResourceAsStream("/images/SDRTrunk_Application_Icon.png"), 16, 16, true, true),
                new Image(SDRTrunkPreloader.class.getResourceAsStream("/images/SDRTrunk_Application_Icon.png"), 32, 32, true, true),
                new Image(SDRTrunkPreloader.class.getResourceAsStream("/images/SDRTrunk_Application_Icon.png"), 64, 64, true, true),
                new Image(SDRTrunkPreloader.class.getResourceAsStream("/images/SDRTrunk_Application_Icon.png"), 128, 128, true, true),
                new Image(SDRTrunkPreloader.class.getResourceAsStream("/images/SDRTrunk_Application_Icon.png"))
            );
        } catch (Exception e) {
            mLog.error("Error setting preloader icon", e);
        }

        // Root container
        javafx.scene.layout.StackPane root = new javafx.scene.layout.StackPane();

        // Background Image
        try {
            Image bgImage = new Image(SDRTrunkPreloader.class.getResourceAsStream("/images/splash_background.jpg"));
            ImageView bgView = new ImageView(bgImage);
            bgView.setFitWidth(800);
            bgView.setFitHeight(450);
            bgView.setPreserveRatio(true);
            root.getChildren().add(bgView);
            
            // Adjust scene size to match the scaled background image
            root.setPrefSize(bgView.getBoundsInLocal().getWidth(), bgView.getBoundsInLocal().getHeight());
        } catch (Exception e) {
            mLog.error("Could not load splash background image", e);
            root.setStyle("-fx-background-color: #1e1e1e;");
            root.setPrefSize(800, 450);
        }

        // Overlay to improve text readability
        javafx.scene.layout.Region overlay = new javafx.scene.layout.Region();
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.4);");
        root.getChildren().add(overlay);

        // Content Container
        VBox contentBox = new VBox(20);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setMaxHeight(VBox.USE_PREF_SIZE);

        // Logo — use the same application icon shown in the taskbar and .exe
        ImageView logoView = new ImageView();
        try {
            Image logoImage = new Image(SDRTrunkPreloader.class.getResourceAsStream("/images/SDRTrunk_Application_Icon.png"));
            logoView.setImage(logoImage);
            logoView.setFitHeight(280);
            logoView.setPreserveRatio(true);
            
            // Drop shadow for logo to stand out
            javafx.scene.effect.DropShadow dropShadow = new javafx.scene.effect.DropShadow();
            dropShadow.setRadius(15);
            dropShadow.setOffsetX(0);
            dropShadow.setOffsetY(4);
            dropShadow.setColor(Color.color(0, 0, 0, 0.7));
            
            logoView.setEffect(dropShadow);
        } catch (Exception e) {
            mLog.error("Could not load splash screen logo", e);
        }

        // Bottom section with text and bar
        VBox bottomBox = new VBox(8);
        bottomBox.setAlignment(Pos.CENTER);
        bottomBox.setStyle("-fx-padding: 15; -fx-background-color: rgba(30, 30, 30, 0.7); -fx-background-radius: 8;");
        bottomBox.setMaxWidth(500);

        // Progress Text
        progressLabel = new Label("Initializing SDRTrunk...");
        progressLabel.setTextFill(Color.WHITE);
        progressLabel.setStyle("-fx-font-family: 'Segoe UI', sans-serif; -fx-font-size: 16px; -fx-font-weight: bold;");

        // Progress Bar
        progressBar = new ProgressBar(0.0);
        progressBar.setPrefWidth(480);
        progressBar.setPrefHeight(12);
        progressBar.setStyle("-fx-accent: #0078d7; -fx-control-inner-background: #444444; -fx-background-color: #444444; -fx-background-radius: 6; -fx-background-insets: 0;");

        bottomBox.getChildren().addAll(progressLabel, progressBar);
        contentBox.getChildren().addAll(logoView, bottomBox);
        root.getChildren().add(contentBox);

        // Version Label
        String appName = io.github.dsheirer.properties.SystemProperties.getInstance().getApplicationName();
        Label versionLabel = new Label(appName.replace("sdrtrunk ", "")); // just show "vK.00.085" or "nightly - ..."
        versionLabel.setTextFill(Color.WHITE);
        versionLabel.setStyle("-fx-font-family: 'Segoe UI', sans-serif; -fx-font-size: 11px; -fx-opacity: 0.7;");
        javafx.scene.layout.StackPane.setAlignment(versionLabel, Pos.BOTTOM_RIGHT);
        javafx.scene.layout.StackPane.setMargin(versionLabel, new javafx.geometry.Insets(0, 10, 10, 0));
        root.getChildren().add(versionLabel);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        
        primaryStage.initStyle(StageStyle.UNDECORATED);
        //Keep the splash above the main window while the latter renders invisibly, so nothing of the still-drawing
        //application shell peeks out around the splash before it is fully ready.
        primaryStage.setAlwaysOnTop(true);
        primaryStage.setScene(scene);
        primaryStage.setTitle("SDRTrunk Starting...");
        primaryStage.setOnHidden(event -> mLog.info("Startup splash hidden"));
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    @Override
    public void handleApplicationNotification(PreloaderNotification info) {
        if (info instanceof ProgressNotification) {
            ProgressNotification progressInfo = (ProgressNotification) info;
            progressBar.setProgress(progressInfo.getProgress());
        } else if (info instanceof StateChangeNotification) {
            StateChangeNotification stateChange = (StateChangeNotification) info;
            if (stateChange.getType() == StateChangeNotification.Type.BEFORE_START) {
                // Ignore automatic BEFORE_START because it hides too early, before UI renders
            }
        } else if (info instanceof TextNotification) {
            TextNotification textInfo = (TextNotification) info;
            progressLabel.setText(textInfo.getText());
        } else if (info instanceof HideNotification) {
            if (preloaderStage != null) {
                mLog.info("Startup splash hide requested");
                preloaderStage.hide();
            }
        }
    }

    /**
     * Custom notification class to send text updates to the preloader.
     */
    public static class TextNotification implements PreloaderNotification {
        private String text;

        public TextNotification(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }

    /**
     * Custom notification class to hide the preloader.
     */
    public static class HideNotification implements PreloaderNotification {
        public HideNotification() {
        }
    }
}
