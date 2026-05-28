package io.github.dsheirer.gui.preference.ai;

import io.github.dsheirer.preference.UserPreferences;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ComboBox;
import io.github.dsheirer.gui.preference.layout.SettingsCard;
import io.github.dsheirer.gui.preference.layout.SettingsRow;
import javafx.scene.layout.VBox;
import jiconfont.javafx.IconNode;
import jiconfont.icons.font_awesome.FontAwesome;
import javafx.scene.paint.Color;
import javafx.scene.control.ContentDisplay;

import javafx.scene.layout.HBox;
import org.controlsfx.control.ToggleSwitch;
import javafx.application.Platform;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

public class AIPreferenceEditor extends VBox {

    private final UserPreferences mUserPreferences;

    public AIPreferenceEditor(UserPreferences userPreferences) {
        mUserPreferences = userPreferences;

        setPadding(new Insets(10, 10, 10, 10));
        setSpacing(20);

        Label headerLabel = new Label("AI Settings");
        headerLabel.getStyleClass().add("hig-section-header");
        getChildren().add(headerLabel);

        SettingsCard mainCard = new SettingsCard();

        ToggleSwitch enableAiSwitch = new ToggleSwitch();
        enableAiSwitch.setTooltip(new Tooltip("Enables all AI features and services."));
        enableAiSwitch.setSelected(mUserPreferences.getAIPreference().isAIEnabled());
        enableAiSwitch.selectedProperty().addListener((observable, oldValue, newValue) -> {
            mUserPreferences.getAIPreference().setAIEnabled(newValue);
        });

        mainCard.getChildren().add(new SettingsRow(createLabelWithHelp("Enable AI Features", "Turns on all AI services such as intelligent log analysis and system health advisor."), enableAiSwitch));

        Label explanationLabel = new Label("If turned on, the application will save the last 5 audio files from each channel on the computer's hard drive (to allow for review of audio).");
        explanationLabel.setWrapText(true);
        explanationLabel.getStyleClass().add("kennebec-secondary-text");
        explanationLabel.setPadding(new Insets(0, 15, 5, 15));
        mainCard.getChildren().add(explanationLabel);

        getChildren().add(mainCard);

        VBox settingsBox = new VBox(20);
        SettingsCard aiCard = new SettingsCard();

        ToggleSwitch enableLogAnalysisSwitch = new ToggleSwitch();
        enableLogAnalysisSwitch.setSelected(mUserPreferences.getAIPreference().isAILogAnalysisEnabled());
        enableLogAnalysisSwitch.selectedProperty().addListener((observable, oldValue, newValue) -> {
            mUserPreferences.getAIPreference().setAILogAnalysisEnabled(newValue);
        });

        Label logExplanationLabel = new Label("Translates cryptic stack traces and warning logs into plain-English explanations with actionable fixes.");
        logExplanationLabel.setWrapText(true);
        logExplanationLabel.getStyleClass().add("kennebec-secondary-text");
        logExplanationLabel.setPadding(new Insets(0, 15, 5, 15));

        PasswordField apiKeyField = new PasswordField();
        apiKeyField.setText(mUserPreferences.getAIPreference().getGeminiApiKey());
        apiKeyField.textProperty().addListener((observable, oldValue, newValue) -> {
            mUserPreferences.getAIPreference().setGeminiApiKey(newValue);
        });

        Button testButton = new Button("Test");
        testButton.setTooltip(new Tooltip("Test the provided Gemini API key and connection"));
        Label testResultLabel = new Label("");

        ComboBox<String> modelComboBox = new ComboBox<>();
        modelComboBox.setEditable(true);
        modelComboBox.setValue(mUserPreferences.getAIPreference().getGeminiModel());
        modelComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                mUserPreferences.getAIPreference().setGeminiModel(newValue);
            }
        });

        Hyperlink apiKeyLink = new Hyperlink("Get a Gemini API Key here");
        apiKeyLink.setOnAction(e -> {
            try {
                if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                    java.awt.Desktop.getDesktop().browse(new URI("https://aistudio.google.com/app/apikey"));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        testButton.setOnAction(e -> {
            testResultLabel.setText("Testing...");
            String apiKey = apiKeyField.getText();
            if (apiKey == null || apiKey.trim().isEmpty()) {
                testResultLabel.setText("Please enter an API key first.");
                return;
            }

            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models?key=" + apiKey.trim()))
                    .GET()
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        Platform.runLater(() -> {
                            if (response.statusCode() == 200) {
                                testResultLabel.setText("Test passed");
                                try {
                                    ObjectMapper mapper = new ObjectMapper();
                                    JsonNode root = mapper.readTree(response.body());
                                    JsonNode models = root.get("models");
                                    if (models != null && models.isArray()) {
                                        modelComboBox.getItems().clear();
                                        for (JsonNode model : models) {
                                            JsonNode nameNode = model.get("name");
                                            if (nameNode != null) {
                                                modelComboBox.getItems().add(nameNode.asText());
                                            }
                                        }
                                        if (!modelComboBox.getItems().contains(modelComboBox.getValue()) && !modelComboBox.getItems().isEmpty()) {
                                            modelComboBox.setValue(modelComboBox.getItems().get(0));
                                        }
                                    }
                                } catch (Exception ex) {
                                    testResultLabel.setText("Test passed, but failed to parse models.");
                                }
                            } else {
                                testResultLabel.setText("Test failed: " + response.statusCode());
                            }
                        });
                    }).exceptionally(ex -> {
                        Platform.runLater(() -> testResultLabel.setText("Test failed: " + ex.getMessage()));
                        return null;
                    });
        });

        HBox apiKeyBox = new HBox(10, apiKeyField, testButton, testResultLabel);
        apiKeyBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        ToggleSwitch enableSystemHealthSwitch = new ToggleSwitch();
        enableSystemHealthSwitch.setSelected(mUserPreferences.getAIPreference().isSystemHealthAdvisorEnabled());
        enableSystemHealthSwitch.selectedProperty().addListener((observable, oldValue, newValue) -> {
            mUserPreferences.getAIPreference().setSystemHealthAdvisorEnabled(newValue);
        });

        Label systemHealthExplanationLabel = new Label("If turned on, a background AI agent will monitor system metrics and suggest configuration optimizations.");
        systemHealthExplanationLabel.setWrapText(true);
        systemHealthExplanationLabel.getStyleClass().add("kennebec-secondary-text");
        systemHealthExplanationLabel.setPadding(new Insets(0, 15, 5, 15));

        aiCard.getChildren().add(new SettingsRow(createLabelWithHelp("Intelligent Log Analysis", "Translates cryptic stack traces and warning logs into plain-English explanations with actionable fixes."), enableLogAnalysisSwitch));
        aiCard.getChildren().add(logExplanationLabel);
        aiCard.getChildren().add(new SettingsRow(createLabelWithHelp("Enable System Health Advisor", "A background AI agent monitors system metrics and suggests configuration optimizations."), enableSystemHealthSwitch));
        aiCard.getChildren().add(systemHealthExplanationLabel);
        aiCard.getChildren().add(new SettingsRow(createLabelWithHelp("Gemini API Key", "API key required to access Google Gemini AI services."), apiKeyBox));
        aiCard.getChildren().add(new SettingsRow("", apiKeyLink));
        aiCard.getChildren().add(new SettingsRow(createLabelWithHelp("Gemini Model", "Select the specific Gemini AI model to use for analysis."), modelComboBox));

        settingsBox.getChildren().add(aiCard);

        settingsBox.visibleProperty().bind(enableAiSwitch.selectedProperty());
        settingsBox.managedProperty().bind(enableAiSwitch.selectedProperty());

        getChildren().addAll(settingsBox);
    }

    private Label createHelpIcon(String tooltipText) {
        IconNode iconNode = new IconNode(FontAwesome.INFO_CIRCLE);
        iconNode.setIconSize(14);
        iconNode.setFill(Color.GRAY);
        Label label = new Label("", iconNode);
        Tooltip tooltip = new Tooltip(tooltipText);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(400);
        label.setTooltip(tooltip);
        label.setContentDisplay(ContentDisplay.RIGHT);
        return label;
    }

    private Label createLabelWithHelp(String text, String tooltipText) {
        Label label = new Label(text, createHelpIcon(tooltipText));
        label.setContentDisplay(ContentDisplay.RIGHT);
        return label;
    }
}