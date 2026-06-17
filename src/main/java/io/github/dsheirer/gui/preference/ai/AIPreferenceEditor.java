package io.github.dsheirer.gui.preference.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.gui.preference.layout.SettingsCard;
import io.github.dsheirer.gui.preference.layout.SettingsRow;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
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
    private static final Logger mLog = LoggerFactory.getLogger(AIPreferenceEditor.class);

    private final UserPreferences mUserPreferences;

    public AIPreferenceEditor(UserPreferences userPreferences) {
        mUserPreferences = userPreferences;

        setPadding(new Insets(10));
        setSpacing(20);

        Label headerLabel = new Label("Artificial Intelligence");
        headerLabel.getStyleClass().add("hig-section-header");

        ToggleSwitch enableAiSwitch = new ToggleSwitch();
        enableAiSwitch.setSelected(mUserPreferences.getAIPreference().isAIEnabled());
        enableAiSwitch.selectedProperty().addListener((observable, oldValue, newValue) -> {
            mUserPreferences.getAIPreference().setAIEnabled(newValue);
        });
        
        SettingsCard mainCard = new SettingsCard();
        mainCard.getChildren().add(new SettingsRow("Enable AI Features", enableAiSwitch));
        
        Label mainExplanation = new Label("Enables AI features like intelligent log analysis, automated DSP remediation, and audio transcriptions.");
        mainExplanation.getStyleClass().add("kennebec-secondary-text");
        mainExplanation.setPadding(new Insets(5, 15, 5, 15));
        mainExplanation.setWrapText(true);

        VBox settingsBox = new VBox(20);
        settingsBox.visibleProperty().bind(enableAiSwitch.selectedProperty());
        settingsBox.managedProperty().bind(enableAiSwitch.selectedProperty());

        // Features Card
        Label featuresLabel = new Label("Features");
        featuresLabel.getStyleClass().add("hig-section-header");
        
        ToggleSwitch enableLogAnalysisSwitch = new ToggleSwitch();
        enableLogAnalysisSwitch.setSelected(mUserPreferences.getAIPreference().isAILogAnalysisEnabled());
        enableLogAnalysisSwitch.selectedProperty().addListener((observable, oldValue, newValue) -> {
            mUserPreferences.getAIPreference().setAILogAnalysisEnabled(newValue);
        });
        
        ToggleSwitch enableSystemHealthSwitch = new ToggleSwitch();
        enableSystemHealthSwitch.setSelected(mUserPreferences.getAIPreference().isSystemHealthAdvisorEnabled());
        enableSystemHealthSwitch.selectedProperty().addListener((observable, oldValue, newValue) -> {
            mUserPreferences.getAIPreference().setSystemHealthAdvisorEnabled(newValue);
        });

        ToggleSwitch enableTranscriptionSwitch = new ToggleSwitch();
        enableTranscriptionSwitch.setSelected(mUserPreferences.getAIPreference().isTranscriptionEnabled());
        enableTranscriptionSwitch.selectedProperty().addListener((observable, oldValue, newValue) -> {
            mUserPreferences.getAIPreference().setTranscriptionEnabled(newValue);
        });

        ToggleSwitch enableToneDiscoverySwitch = new ToggleSwitch();
        enableToneDiscoverySwitch.setSelected(mUserPreferences.getAIPreference().isAIToneDiscoveryEnabled());
        enableToneDiscoverySwitch.selectedProperty().addListener((observable, oldValue, newValue) -> {
            mUserPreferences.getAIPreference().setAIToneDiscoveryEnabled(newValue);
        });

        Button manageIgnoredBtn = new Button("Manage Ignored Tones");
        manageIgnoredBtn.setOnAction(e -> {
            SmartIgnoreListDialog dialog = new SmartIgnoreListDialog(getScene().getWindow());
            dialog.showAndWait();
        });
        manageIgnoredBtn.disableProperty().bind(enableToneDiscoverySwitch.selectedProperty().not());

        HBox toneDiscoveryControls = new HBox(10, enableToneDiscoverySwitch, manageIgnoredBtn);
        toneDiscoveryControls.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        ToggleSwitch enableNbfmAutoOptimizeSwitch = new ToggleSwitch();
        enableNbfmAutoOptimizeSwitch.setSelected(mUserPreferences.getAIPreference().isNBFMAudioAutoOptimizeEnabled());
        enableNbfmAutoOptimizeSwitch.selectedProperty().addListener((observable, oldValue, newValue) -> {
            mUserPreferences.getAIPreference().setNBFMAudioAutoOptimizeEnabled(newValue);
        });

        ToggleSwitch enableGainAdvisorSwitch = new ToggleSwitch();
        enableGainAdvisorSwitch.setSelected(mUserPreferences.getAIPreference().isGainAdvisorEnabled());
        enableGainAdvisorSwitch.selectedProperty().addListener((observable, oldValue, newValue) -> {
            mUserPreferences.getAIPreference().setGainAdvisorEnabled(newValue);
        });

        ToggleSwitch enableSquelchAdvisorSwitch = new ToggleSwitch();
        enableSquelchAdvisorSwitch.setSelected(mUserPreferences.getAIPreference().isSquelchAdvisorEnabled());
        enableSquelchAdvisorSwitch.selectedProperty().addListener((observable, oldValue, newValue) -> {
            mUserPreferences.getAIPreference().setSquelchAdvisorEnabled(newValue);
        });

        SettingsCard featuresCard = new SettingsCard();
        featuresCard.getChildren().addAll(
            new SettingsRow("Intelligent Log Analysis", enableLogAnalysisSwitch),
            new SettingsRow("System Health Advisor & Auto-Remediation", enableSystemHealthSwitch),
            new SettingsRow("Audio Transcriptions", enableTranscriptionSwitch),
            new SettingsRow("AI Two-Tone Paging Discovery", toneDiscoveryControls),
            new SettingsRow("Auto-Optimize NBFM Audio Filters (runs up to twice a day per channel)", enableNbfmAutoOptimizeSwitch),
            new SettingsRow("Adaptive Gain Advisor (monitors I/Q levels, recommends tuner gain changes)", enableGainAdvisorSwitch),
            new SettingsRow("Squelch Advisor (enables the Calibrate Squelch button on NBFM channels)", enableSquelchAdvisorSwitch)
        );

        // API Key Card with Embedded Scaffolding
        Label apiHeaderLabel = new Label("Gemini Integration");
        apiHeaderLabel.getStyleClass().add("hig-section-header");

        PasswordField apiKeyField = new PasswordField();
        apiKeyField.setText(mUserPreferences.getAIPreference().getGeminiApiKey());
        apiKeyField.setPrefWidth(200);
        apiKeyField.textProperty().addListener((observable, oldValue, newValue) -> {
            mUserPreferences.getAIPreference().setGeminiApiKey(newValue);
            mUserPreferences.getAIPreference().setGeminiApiKeyTested(false);
        });

        Button testButton = new Button("Test");
        Label testResultLabel = new Label("");
        HBox apiKeyInputBox = new HBox(10, apiKeyField, testButton, testResultLabel);
        apiKeyInputBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        ComboBox<String> modelComboBox = new ComboBox<>();
        modelComboBox.setEditable(true);
        modelComboBox.setValue(mUserPreferences.getAIPreference().getGeminiModel());
        modelComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                mUserPreferences.getAIPreference().setGeminiModel(newValue);
            }
        });

        SettingsCard apiCard = new SettingsCard();
        apiCard.getChildren().addAll(
            new SettingsRow("API Key", apiKeyInputBox),
            new SettingsRow("Model", modelComboBox)
        );

        // Transcription Service Card
        Label transcriptionHeaderLabel = new Label("Transcription Service");
        transcriptionHeaderLabel.getStyleClass().add("hig-section-header");
        
        ComboBox<String> engineComboBox = new ComboBox<>();
        engineComboBox.getItems().addAll("WHISPER", "GOOGLE");
        engineComboBox.setValue(mUserPreferences.getAIPreference().getTranscriptionEngine());
        engineComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            mUserPreferences.getAIPreference().setTranscriptionEngine(newValue);
        });

        PasswordField whisperApiKeyField = new PasswordField();
        whisperApiKeyField.setText(mUserPreferences.getAIPreference().getWhisperApiKey());
        whisperApiKeyField.setPrefWidth(200);
        whisperApiKeyField.textProperty().addListener((observable, oldValue, newValue) -> {
            mUserPreferences.getAIPreference().setWhisperApiKey(newValue);
        });

        PasswordField googleApiKeyField = new PasswordField();
        googleApiKeyField.setText(mUserPreferences.getAIPreference().getGoogleSttApiKey());
        googleApiKeyField.setPrefWidth(200);
        googleApiKeyField.textProperty().addListener((observable, oldValue, newValue) -> {
            mUserPreferences.getAIPreference().setGoogleSttApiKey(newValue);
        });

        SettingsRow whisperKeyRow = new SettingsRow("OpenAI Whisper API Key", whisperApiKeyField);
        whisperKeyRow.managedProperty().bind(engineComboBox.valueProperty().isEqualTo("WHISPER"));
        whisperKeyRow.visibleProperty().bind(engineComboBox.valueProperty().isEqualTo("WHISPER"));

        SettingsRow googleKeyRow = new SettingsRow("Google STT API Key", googleApiKeyField);
        googleKeyRow.managedProperty().bind(engineComboBox.valueProperty().isEqualTo("GOOGLE"));
        googleKeyRow.visibleProperty().bind(engineComboBox.valueProperty().isEqualTo("GOOGLE"));

        SettingsCard transcriptionCard = new SettingsCard();
        transcriptionCard.getChildren().addAll(
            new SettingsRow("Engine", engineComboBox),
            whisperKeyRow,
            googleKeyRow
        );

        // Embedded Scaffolding VBox
        VBox scaffoldingBox = new VBox(5);
        scaffoldingBox.setPadding(new Insets(5, 15, 5, 15));
        Label scaffoldingHeader = new Label("How to get a Gemini API Key:");
        scaffoldingHeader.setStyle("-fx-font-weight: bold;");
        Label scaffoldingStep1 = new Label("1. Go to Google AI Studio (aistudio.google.com).");
        Label scaffoldingStep2 = new Label("2. Sign in with your Google Account.");
        Label scaffoldingStep3 = new Label("3. Click 'Create API key' in the left menu.");
        Hyperlink apiKeyLink = new Hyperlink("Open Google AI Studio");
        apiKeyLink.setPadding(new Insets(0));
        apiKeyLink.setOnAction(e -> {
            try {
                if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                    java.awt.Desktop.getDesktop().browse(new URI("https://aistudio.google.com/app/apikey"));
                }
            } catch (Exception ex) {
                mLog.error("Error opening API key URL in browser", ex);
            }
        });
        scaffoldingBox.getChildren().addAll(scaffoldingHeader, scaffoldingStep1, scaffoldingStep2, scaffoldingStep3, apiKeyLink);
        scaffoldingBox.getStyleClass().add("kennebec-secondary-text");

        testButton.setOnAction(e -> {
            testResultLabel.setText("Testing...");
            String apiKey = apiKeyField.getText();
            if (apiKey == null || apiKey.trim().isEmpty()) {
                testResultLabel.setText("Please enter an API key.");
                return;
            }

            java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                return io.github.dsheirer.preference.ai.GeminiApiHelper.fetchAvailableModels(apiKey);
            }).thenAccept(models -> {
                Platform.runLater(() -> {
                    if (models != null && !models.isEmpty()) {
                        mUserPreferences.getAIPreference().setGeminiApiKeyTested(true);
                        testResultLabel.setText("Passed");
                        testResultLabel.setTextFill(javafx.scene.paint.Color.GREEN);
                        
                        // Populate combobox with filtered models
                        modelComboBox.getItems().clear();
                        for (io.github.dsheirer.preference.ai.GeminiModel model : models) {
                            modelComboBox.getItems().add(model.getName());
                        }

                        // Prompt user
                        java.util.Optional<String> selectedModel = GeminiModelSelectionDialog.promptUserForModel(models, modelComboBox.getValue());
                        if (selectedModel.isPresent()) {
                            modelComboBox.setValue(selectedModel.get());
                            mUserPreferences.getAIPreference().setGeminiModel(selectedModel.get());
                        }
                    } else {
                        mUserPreferences.getAIPreference().setGeminiApiKeyTested(false);
                        apiKeyField.setText("");
                        testResultLabel.setText("Invalid API Key, please retry.");
                        testResultLabel.setTextFill(javafx.scene.paint.Color.RED);
                    }
                });
            }).exceptionally(ex -> {
                Platform.runLater(() -> {
                    testResultLabel.setText("Error");
                    testResultLabel.setTextFill(javafx.scene.paint.Color.RED);
                });
                return null;
            });
        });

        settingsBox.getChildren().addAll(featuresLabel, featuresCard, transcriptionHeaderLabel, transcriptionCard, apiHeaderLabel, apiCard, scaffoldingBox);
        getChildren().addAll(headerLabel, mainCard, mainExplanation, settingsBox);

        sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene == null) {
                if (mUserPreferences.getAIPreference().isAIEnabled() && !mUserPreferences.getAIPreference().isGeminiApiKeyTested()) {
                    mUserPreferences.getAIPreference().setAIEnabled(false);
                    enableAiSwitch.setSelected(false);
                }
            }
        });
    }
}
