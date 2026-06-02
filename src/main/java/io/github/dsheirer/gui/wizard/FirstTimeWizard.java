package io.github.dsheirer.gui.wizard;

import io.github.dsheirer.gui.UsbMonitorManager;
import io.github.dsheirer.gui.preference.calibration.CalibrationDialog;
import io.github.dsheirer.jmbe.JmbeCreator;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.gui.JavaFxWindowManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.controlsfx.dialog.Wizard;
import org.controlsfx.dialog.WizardPane;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import io.github.dsheirer.jmbe.github.GitHub;
import io.github.dsheirer.jmbe.github.Release;
import javafx.concurrent.Task;

public class FirstTimeWizard {

    private final UserPreferences mUserPreferences;
    private final JavaFxWindowManager mWindowManager;
    private final Stage mOwner;

    public FirstTimeWizard(UserPreferences userPreferences, JavaFxWindowManager windowManager, Stage owner) {
        mUserPreferences = userPreferences;
        mWindowManager = windowManager;
        mOwner = owner;
    }

    public void showAndWait() {
        Wizard wizard = new Wizard();
        wizard.setTitle("SDRTrunk Kennebec - First Time Setup");
        
        List<WizardPane> panes = new ArrayList<>();
        
        panes.add(createWelcomePane());
        panes.add(createJmbePane());
        panes.add(createCalibrationPane());
        
        String osName = System.getProperty("os.name");
        boolean isWindows = osName != null && osName.toLowerCase().contains("win");
        
        if (isWindows) {
            panes.add(createPowerScriptPane());
            panes.add(createWindowsOptimizationPane());
        }
        
        panes.add(createRemoteDesktopPane());
        panes.add(createAIPane());
        panes.add(createMemoryPane()); // Memory is now the last step

        wizard.setFlow(new Wizard.LinearFlow(panes));
        wizard.showAndWait();
        
        java.util.prefs.Preferences p = java.util.prefs.Preferences.userNodeForPackage(io.github.dsheirer.gui.SDRTrunk.class);
        p.putBoolean("sdrtrunk.first.time.wizard.completed", true);
    }

    private WizardPane createWelcomePane() {
        WizardPane pane = new WizardPane();
        pane.setHeaderText("Welcome to SDRTrunk Kennebec");
        Label content = new Label("This wizard will guide you through the essential first-time setup for SDRTrunk Kennebec.\n\n" +
                "We will configure your audio libraries, calibration, system optimizations, and AI features.\n\n" +
                "You can skip any step and configure it later in the User Preferences if you prefer.");
        content.setWrapText(true);
        pane.setContent(content);
        return pane;
    }

    private WizardPane createJmbePane() {
        WizardPane pane = new WizardPane();
        pane.setHeaderText("JMBE Audio Library");
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        
        Label lbl = new Label("The JMBE (Java Multi-Band Excitation) library is required to decode P25 Phase I/II digital audio.\n\n" +
                "Due to patent/licensing restrictions, this library cannot be distributed with the application. You must compile it yourself.");
        lbl.setWrapText(true);
        
        ToggleGroup group = new ToggleGroup();
        RadioButton regularBtn = new RadioButton("Regular (dsheirer)");
        regularBtn.setToggleGroup(group);
        regularBtn.setSelected(true);
        RadioButton bazinetaBtn = new RadioButton("Bazineta Fork (Alternative)");
        bazinetaBtn.setToggleGroup(group);
        
        HBox radioBox = new HBox(10, regularBtn, bazinetaBtn);
        
        TextArea consoleArea = new TextArea();
        consoleArea.setEditable(false);
        consoleArea.setPrefRowCount(8);
        consoleArea.setWrapText(true);
        
        ProgressIndicator progress = new ProgressIndicator();
        progress.setVisible(false);
        progress.setMaxSize(24, 24);
        
        Button createBtn = new Button("Create Library");
        HBox buttonBox = new HBox(10, createBtn, progress);
        
        createBtn.setOnAction(e -> {
            createBtn.setDisable(true);
            progress.setVisible(true);
            boolean useBazineta = bazinetaBtn.isSelected();
            consoleArea.setText("Starting build...\n");
            
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    Release release = GitHub.getLatestRelease(useBazineta ? JmbeCreator.GITHUB_BAZINETA_JMBE_RELEASES_URL : JmbeCreator.GITHUB_JMBE_RELEASES_URL);
                    Path libraryPath = mUserPreferences.getDirectoryPreference().getDirectoryApplicationRoot().resolve("jmbe").resolve("jmbe.jar");
                    JmbeCreator creator = new JmbeCreator(release, libraryPath, useBazineta);
                    
                    creator.consoleOutputProperty().addListener((obs, oldVal, newVal) -> {
                        Platform.runLater(() -> consoleArea.setText(newVal));
                    });
                    
                    creator.execute();
                    
                    // Wait for it to complete
                    while (!creator.completeProperty().get()) {
                        Thread.sleep(100);
                    }
                    
                    Platform.runLater(() -> {
                        progress.setVisible(false);
                        if (creator.hasErrors()) {
                            Alert a = new Alert(Alert.AlertType.ERROR, "Failed to build JMBE library. Check the output.");
                            a.showAndWait();
                            createBtn.setDisable(false);
                        } else {
                            // Try to get JmbePreference via other means if getDecoderPreference is missing. 
                            // Or just save it to preferences directly.
                            java.util.prefs.Preferences p = java.util.prefs.Preferences.userNodeForPackage(io.github.dsheirer.preference.decoder.JmbeLibraryPreference.class);
                            p.put("sdrtrunk.decoder.jmbe.library.path", libraryPath.toAbsolutePath().toString());
                            Alert a = new Alert(Alert.AlertType.INFORMATION, "JMBE Library built and installed successfully! You may now click Next to continue.");
                            a.showAndWait();
                        }
                    });
                    return null;
                }
            };
            new Thread(task).start();
        });
        
        box.getChildren().addAll(lbl, radioBox, buttonBox, new Label("Build Output:"), consoleArea);
        pane.setContent(box);
        return pane;
    }

    private WizardPane createCalibrationPane() {
        WizardPane pane = new WizardPane();
        pane.setHeaderText("SDR Tuner Calibration");
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        
        Label lbl = new Label("SDR hardware tuners often have frequency offsets (PPM error). " +
                "SDRTrunk can automatically calculate and apply this correction factor.");
        lbl.setWrapText(true);
        
        Button calBtn = new Button("Start Calibration Process...");
        calBtn.setOnAction(e -> {
            if (mWindowManager != null) {
                CalibrationDialog dialog = mWindowManager.getCalibrationDialog(mUserPreferences);
                dialog.showAndWait();
            } else {
                Alert a = new Alert(Alert.AlertType.WARNING, "Calibration dialog requires main window to be initialized.");
                a.showAndWait();
            }
        });
        
        box.getChildren().addAll(lbl, calBtn);
        pane.setContent(box);
        return pane;
    }

    private WizardPane createMemoryPane() {
        WizardPane pane = new WizardPane();
        pane.setHeaderText("System Memory Allocation & Setup Complete");
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        
        Label lbl = new Label("SDRTrunk is memory intensive when monitoring many channels or using the waterfall display. " +
                "Select the maximum memory (RAM) the application is allowed to use. A minimum of 4GB is recommended.\n\n" +
                "This is the final step. Applying this setting will prompt a restart of the application.");
        lbl.setWrapText(true);
        
        ComboBox<Integer> memoryCombo = new ComboBox<>();
        memoryCombo.getItems().addAll(2, 4, 6, 8, 12, 16, 24, 32);
        memoryCombo.setValue(6); // Default 6GB
        
        Button applyBtn = new Button("Set Memory Limit & Restart");
        applyBtn.setOnAction(e -> {
            try {
                Path appRoot = mUserPreferences.getDirectoryPreference().getDirectoryApplicationRoot();
                Path memoryFile = appRoot.resolve("SDRTrunk.memory");
                Files.writeString(memoryFile, memoryCombo.getValue().toString());
                
                java.util.prefs.Preferences p = java.util.prefs.Preferences.userNodeForPackage(io.github.dsheirer.gui.SDRTrunk.class);
                p.putBoolean("sdrtrunk.first.time.wizard.completed", true);
                
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Memory limit set to " + memoryCombo.getValue() + "GB.\nSDRTrunk will now exit. Please start it again manually.");
                a.showAndWait();
                System.exit(0);
            } catch (IOException ex) {
                Alert a = new Alert(Alert.AlertType.ERROR, "Failed to write memory settings.");
                a.showAndWait();
            }
        });
        
        box.getChildren().addAll(lbl, new Label("Max Memory (GB):"), memoryCombo, applyBtn);
        pane.setContent(box);
        return pane;
    }

    private WizardPane createPowerScriptPane() {
        WizardPane pane = new WizardPane();
        pane.setHeaderText("Tuner Monitoring Power Script");
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        
        Label lbl = new Label("SDRTrunk can install a background Windows scheduled task that monitors " +
                "for crashed or locked-up USB SDR devices and attempts to automatically reset them. " +
                "Requires Administrator permissions to install.");
        lbl.setWrapText(true);
        
        Button installBtn = new Button("Install Power Script...");
        ProgressIndicator progress = new ProgressIndicator();
        progress.setVisible(false);
        progress.setMaxSize(24, 24);
        HBox buttonBox = new HBox(10, installBtn, progress);
        
        installBtn.setOnAction(e -> {
            installBtn.setDisable(true);
            progress.setVisible(true);
            Task<Boolean> task = new Task<>() {
                @Override
                protected Boolean call() throws Exception {
                    return UsbMonitorManager.install(mUserPreferences);
                }
            };
            task.setOnSucceeded(ev -> {
                progress.setVisible(false);
                if (task.getValue()) {
                    Alert a = new Alert(Alert.AlertType.INFORMATION, "Script successfully installed! You may now click Next to continue.");
                    a.showAndWait();
                } else {
                    Alert a = new Alert(Alert.AlertType.ERROR, "Failed to install script. Make sure you run as Administrator or check logs.");
                    a.showAndWait();
                    installBtn.setDisable(false);
                }
            });
            new Thread(task).start();
        });
        
        box.getChildren().addAll(lbl, buttonBox);
        pane.setContent(box);
        return pane;
    }

    private WizardPane createRemoteDesktopPane() {
        WizardPane pane = new WizardPane();
        pane.setHeaderText("Remote Desktop Optimizations");
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        
        Label lbl = new Label("This will disable some animations and simplify gui when rustdesk is connected to the host computer.");
        lbl.setWrapText(true);
        
        CheckBox optimizeChk = new CheckBox("Enable Remote Desktop Optimizations");
        // We will store this in a preference. Let's use a standard general preference.
        java.util.prefs.Preferences appPrefs = java.util.prefs.Preferences.userNodeForPackage(io.github.dsheirer.gui.SDRTrunk.class);
        optimizeChk.setSelected(appPrefs.getBoolean("sdrtrunk.rdp.optimizations", false));
        optimizeChk.setOnAction(e -> {
            appPrefs.putBoolean("sdrtrunk.rdp.optimizations", optimizeChk.isSelected());
        });
        
        box.getChildren().addAll(lbl, optimizeChk);
        pane.setContent(box);
        return pane;
    }

    private WizardPane createWindowsOptimizationPane() {
        WizardPane pane = new WizardPane();
        pane.setHeaderText("Windows Host Optimizations");
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        
        Label lbl = new Label("These settings tune the Windows OS process priorities to ensure SDRTrunk " +
                "receives consistent CPU cycles for uninterrupted audio decoding. " +
                "It requires Administrator permissions to modify process priorities.");
        lbl.setWrapText(true);
        
        CheckBox optimizeChk = new CheckBox("Enable Windows Process Priority Optimization");
        java.util.prefs.Preferences p = java.util.prefs.Preferences.userNodeForPackage(io.github.dsheirer.preference.application.ApplicationPreference.class);
        optimizeChk.setSelected(p.getBoolean("sdrtrunk.application.watchdog.enabled", false));
        optimizeChk.setOnAction(e -> {
            p.putBoolean("sdrtrunk.application.watchdog.enabled", optimizeChk.isSelected());
        });
        
        box.getChildren().addAll(lbl, optimizeChk);
        pane.setContent(box);
        return pane;
    }

    private WizardPane createAIPane() {
        WizardPane pane = new WizardPane();
        pane.setHeaderText("Gemini AI Features");
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        
        Label lbl = new Label("SDRTrunk Kennebec features AI integrations using Google's Gemini API to summarize transcripts and identify channel activity.\n\n" +
                "A valid Gemini API Key is required. You can get one for free from Google AI Studio.");
        lbl.setWrapText(true);
        
        CheckBox enableAiChk = new CheckBox("Enable AI Features");
        enableAiChk.setSelected(mUserPreferences.getAIPreference().isAIEnabled());
        
        TextField apiKeyField = new TextField(mUserPreferences.getAIPreference().getGeminiApiKey());
        apiKeyField.setPromptText("Enter Gemini API Key");
        
        Button testBtn = new Button("Test API Key");
        ProgressIndicator progress = new ProgressIndicator();
        progress.setVisible(false);
        progress.setMaxSize(24, 24);
        HBox testBox = new HBox(10, testBtn, progress);
        
        testBtn.setOnAction(e -> {
            String key = apiKeyField.getText();
            if (key == null || key.isBlank()) {
                Alert a = new Alert(Alert.AlertType.WARNING, "Please enter an API Key first.");
                a.showAndWait();
                return;
            }
            
            testBtn.setDisable(true);
            progress.setVisible(true);
            
            Task<Boolean> task = new Task<>() {
                @Override
                protected Boolean call() throws Exception {
                    return testApiKey(key);
                }
            };
            task.setOnSucceeded(ev -> {
                progress.setVisible(false);
                boolean valid = task.getValue();
                if (valid) {
                    mUserPreferences.getAIPreference().setGeminiApiKey(key);
                    mUserPreferences.getAIPreference().setAIEnabled(enableAiChk.isSelected());
                    Alert a = new Alert(Alert.AlertType.INFORMATION, "API Key tested successfully! You may now click Next to continue.");
                    a.showAndWait();
                } else {
                    apiKeyField.setText("");
                    mUserPreferences.getAIPreference().setGeminiApiKey("");
                    mUserPreferences.getAIPreference().setAIEnabled(false);
                    enableAiChk.setSelected(false);
                    Alert a = new Alert(Alert.AlertType.ERROR, "API Key test failed. Field cleared.");
                    a.showAndWait();
                    testBtn.setDisable(false);
                }
            });
            new Thread(task).start();
        });
        
        box.getChildren().addAll(lbl, enableAiChk, apiKeyField, testBox);
        pane.setContent(box);
        return pane;
    }

    private boolean testApiKey(String key) {
        // Dummy test for wizard - if we had a real test method we'd call it here
        // We will assume any non-empty key is successfully tested if it passes basic length check
        // The AIPreferenceEditor has the actual HTTP call. For the wizard, we can do a simplified check.
        try {
            java.net.URL url = new java.net.URI("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + key).toURL();
            java.net.HttpURLConnection con = (java.net.HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);
            
            String jsonInputString = "{\"contents\":[{\"parts\":[{\"text\":\"Hello\"}]}]}";
            try(java.io.OutputStream os = con.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);			
            }
            int code = con.getResponseCode();
            return code == 200;
        } catch (Exception e) {
            return false;
        }
    }

    // createSummaryPane removed as Memory is now the last step
}
