package io.github.dsheirer.gui.playlist.channel;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Conceptual HIG-compliant redesign of the ChannelConfigurationEditor.
 * Features card-based layouts, distinct visual hierarchy, and clear action prioritization.
 */
public class HIGChannelConfigurationEditor extends VBox {

    public HIGChannelConfigurationEditor() {
        // HIG Spacing: 24px between major sections, 24px padding around the main view
        setSpacing(24);
        setPadding(new Insets(24));

        // Header
        Label headerLabel = new Label("Channel Configuration");
        headerLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1C1C1E;"); // HIG Primary Text Color

        // Group 1: General Info Card
        VBox generalInfoCard = new VBox(12);
        generalInfoCard.setStyle("-fx-background-color: white; -fx-background-radius: 10px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 2); -fx-padding: 16px;");

        Label generalLabel = new Label("General Information");
        generalLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #3A3A3C;"); // HIG Secondary Text Color

        HBox nameRow = createFormRow("Name", new TextField());
        HBox systemRow = createFormRow("System", new TextField());
        HBox siteRow = createFormRow("Site", new TextField());

        generalInfoCard.getChildren().addAll(generalLabel, nameRow, systemRow, siteRow);

        // Group 2: Alias & Actions Card
        VBox aliasCard = new VBox(12);
        aliasCard.setStyle("-fx-background-color: white; -fx-background-radius: 10px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 2); -fx-padding: 16px;");

        Label aliasLabel = new Label("Alias Management");
        aliasLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #3A3A3C;");

        HBox aliasRow = createFormRow("Alias List", new ComboBox<String>());

        aliasCard.getChildren().addAll(aliasLabel, aliasRow);

        // Actions
        HBox actionBox = new HBox(12);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        Button resetBtn = new Button("Reset");
        resetBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #007AFF; -fx-border-color: transparent; -fx-font-size: 13px;"); // Destructive/Secondary action (HIG Blue text)

        Button saveBtn = new Button("Save");
        saveBtn.setStyle("-fx-background-color: #007AFF; -fx-text-fill: white; -fx-background-radius: 6px; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 6 16 6 16;"); // HIG Primary Action Button

        actionBox.getChildren().addAll(resetBtn, saveBtn);

        getChildren().addAll(headerLabel, generalInfoCard, aliasCard, actionBox);
    }

    /**
     * Helper to create consistently aligned form rows.
     */
    private HBox createFormRow(String labelText, Control field) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label(labelText);
        label.setPrefWidth(120); // Consistent label width for alignment
        label.setAlignment(Pos.CENTER_RIGHT);
        label.setStyle("-fx-text-fill: #8E8E93; -fx-font-size: 13px;"); // HIG Tertiary Text Color

        field.setPrefWidth(250);

        row.getChildren().addAll(label, field);
        return row;
    }
}
