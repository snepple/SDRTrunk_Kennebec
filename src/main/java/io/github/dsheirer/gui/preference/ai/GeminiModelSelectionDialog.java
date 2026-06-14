package io.github.dsheirer.gui.preference.ai;

import io.github.dsheirer.preference.ai.GeminiModel;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Callback;

import java.util.List;
import java.util.Optional;

public class GeminiModelSelectionDialog extends Dialog<String> {

    public GeminiModelSelectionDialog(List<GeminiModel> availableModels, String currentSelectedModel) {
        setTitle("Select Gemini AI Model");
        setHeaderText("Choose an AI model for transcript summarization and analysis.");

        ButtonType selectButtonType = new ButtonType("Select", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(selectButtonType, ButtonType.CANCEL);

        VBox contentBox = new VBox(15);
        contentBox.setPadding(new Insets(10));
        contentBox.setPrefWidth(450);

        Label recommendationLabel = new Label("Model Guide:\n• Flash Models (Recommended): Optimized for speed and low cost. Best for real-time channel prioritization.\n• Pro Models: Higher reasoning accuracy for complex radio jargon, but slower and more expensive.");
        recommendationLabel.setWrapText(true);
        recommendationLabel.setTextFill(Color.web("#005FB8"));
        recommendationLabel.setStyle("-fx-background-color: #E6F3FF; -fx-padding: 10; -fx-background-radius: 5;");

        ListView<GeminiModel> listView = new ListView<>();
        listView.getItems().addAll(availableModels);
        listView.setPrefHeight(250);

        listView.setCellFactory(new Callback<>() {
            @Override
            public ListCell<GeminiModel> call(ListView<GeminiModel> param) {
                return new ListCell<>() {
                    @Override
                    protected void updateItem(GeminiModel item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            VBox cellBox = new VBox(5);
                            cellBox.setPadding(new Insets(5, 0, 5, 0));
                            
                            Label nameLabel = new Label(item.getDisplayName() != null ? item.getDisplayName() : item.getName());
                            nameLabel.setFont(Font.font("SansSerif", FontWeight.BOLD, 14));
                            
                            if (item.getName().toLowerCase().contains("flash")) {
                                Label recBadge = new Label(" RECOMMENDED (Fast) ");
                                recBadge.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-size: 10px; -fx-background-radius: 3; -fx-font-weight: bold;");
                                HBox nameRow = new HBox(10, nameLabel, recBadge);
                                nameRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                                cellBox.getChildren().add(nameRow);
                            } else if (item.getName().toLowerCase().contains("pro")) {
                                Label proBadge = new Label(" HIGH ACCURACY (Slower) ");
                                proBadge.setStyle("-fx-background-color: #005FB8; -fx-text-fill: white; -fx-font-size: 10px; -fx-background-radius: 3; -fx-font-weight: bold;");
                                HBox nameRow = new HBox(10, nameLabel, proBadge);
                                nameRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                                cellBox.getChildren().add(nameRow);
                            } else {
                                cellBox.getChildren().add(nameLabel);
                            }

                            Label descLabel = new Label(item.getDescription() != null ? item.getDescription() : "");
                            descLabel.setFont(Font.font("SansSerif", 12));
                            descLabel.setTextFill(Color.DARKGRAY);
                            descLabel.setWrapText(true);
                            
                            Label idLabel = new Label("ID: " + item.getName());
                            idLabel.setFont(Font.font("Monospaced", 11));
                            idLabel.setTextFill(Color.GRAY);

                            cellBox.getChildren().addAll(descLabel, idLabel);
                            setGraphic(cellBox);
                        }
                    }
                };
            }
        });

        // Pre-select current or recommended
        GeminiModel toSelect = null;
        for (GeminiModel model : availableModels) {
            if (model.getName().equals(currentSelectedModel)) {
                toSelect = model;
                break;
            }
        }
        if (toSelect == null) {
            for (GeminiModel model : availableModels) {
                if (model.getName().toLowerCase().contains("flash")) {
                    toSelect = model;
                    break;
                }
            }
        }
        if (toSelect == null && !availableModels.isEmpty()) {
            toSelect = availableModels.get(0);
        }
        
        if (toSelect != null) {
            listView.getSelectionModel().select(toSelect);
        }

        contentBox.getChildren().addAll(recommendationLabel, new Label("Available Models:"), listView);
        getDialogPane().setContent(contentBox);

        setResultConverter(dialogButton -> {
            if (dialogButton == selectButtonType) {
                GeminiModel selected = listView.getSelectionModel().getSelectedItem();
                return selected != null ? selected.getName() : null;
            }
            return null;
        });
    }

    public static Optional<String> promptUserForModel(List<GeminiModel> models, String currentSelectedModel) {
        if (models == null || models.isEmpty()) {
            return Optional.empty();
        }
        GeminiModelSelectionDialog dialog = new GeminiModelSelectionDialog(models, currentSelectedModel);
        return dialog.showAndWait();
    }
}
