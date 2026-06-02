package io.github.dsheirer.gui.playlist.streaming;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.paint.Color;

public class ZelloAuditChecklistPanel extends VBox {

    public ZelloAuditChecklistPanel() {
        setPadding(new Insets(20));
        setSpacing(15);
        setStyle("-fx-background-color: #F5F5F7;"); // Apple HIG light gray background

        Label title = new Label("Configuration Audit Checklist");
        title.setFont(Font.font("-apple-system", FontWeight.BOLD, 22));
        title.setTextFill(Color.web("#1D1D1F"));

        VBox checklist = new VBox(10);
        checklist.setPadding(new Insets(10));
        checklist.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 2);");

        checklist.getChildren().add(createCheckItem("Zello Subdomain Configured"));
        checklist.getChildren().add(createCheckItem("Username & Password Set"));
        checklist.getChildren().add(createCheckItem("Stream Guard Time > 0"));
        checklist.getChildren().add(createCheckItem("Valid Opus Bitrate (e.g. 28000)"));

        ScrollPane scrollPane = new ScrollPane(checklist);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        Button generateDocsBtn = new Button("Generate Documentation");
        generateDocsBtn.setMinHeight(44);
        generateDocsBtn.setStyle("-fx-background-radius: 22; -fx-background-color: #007AFF; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 0 20;");
        generateDocsBtn.setOnAction(e -> {
            try {
                io.github.dsheirer.module.control.ZelloConfigOrchestrator.generateDocumentation("zello_docs.json");
                javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION, "Documentation generated at zello_docs.json");
                a.show();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        HBox btnBox = new HBox(generateDocsBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        getChildren().addAll(title, scrollPane, btnBox);
    }

    private HBox createCheckItem(String text) {
        CheckBox cb = new CheckBox();
        cb.setStyle("-fx-font-family: '-apple-system';");
        Label l = new Label(text);
        l.setFont(Font.font("-apple-system", 16));
        HBox box = new HBox(10, cb, l);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(5));
        return box;
    }
}
