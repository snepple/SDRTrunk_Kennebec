package io.github.dsheirer.gui.wizard;

import io.github.dsheirer.gui.UsbMonitorManager;
import io.github.dsheirer.gui.preference.calibration.CalibrationDialog;
import io.github.dsheirer.jmbe.JmbeCreator;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.gui.JavaFxWindowManager;
import io.github.dsheirer.gui.theme.ThemeManager;
import io.github.dsheirer.jmbe.github.GitHub;
import io.github.dsheirer.jmbe.github.Release;
import io.github.dsheirer.dsp.opencl.GpuDetector;
import io.github.dsheirer.preference.display.DisplayPreference;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * First-time setup wizard with installer-style UI.
 * Features a branded sidebar with step indicators and progress-bar blocking
 * so users cannot advance while a background task is running.
 */
public class FirstTimeWizard
{
    private final UserPreferences mUserPreferences;
    private final JavaFxWindowManager mWindowManager;
    private final Stage mOwner;

    // Wizard state
    private int mCurrentStep = 0;
    private final List<WizardStep> mSteps = new ArrayList<>();
    private StackPane mContentStack;
    private VBox mStepIndicatorContainer;
    private Button mBackButton;
    private Button mNextButton;
    private Button mSkipButton;
    private boolean mTaskRunning = false;

    // Sidebar colors
    private static final String SIDEBAR_DARK = "#1B2838";
    private static final String SIDEBAR_MID = "#1E3A5F";
    private static final String ACCENT_BLUE = "#007AFF";
    private static final String ACCENT_GREEN = "#34C759";

    public FirstTimeWizard(UserPreferences userPreferences, JavaFxWindowManager windowManager, Stage owner)
    {
        mUserPreferences = userPreferences;
        mWindowManager = windowManager;
        mOwner = owner;
    }

    public void showAndWait()
    {
        buildSteps();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("SDRTrunk Kennebec — First Time Setup");
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (mOwner != null)
        {
            dialog.initOwner(mOwner);
        }

        // Build the layout
        BorderPane root = new BorderPane();
        root.setPrefSize(820, 560);
        root.setMinSize(820, 560);

        // ── LEFT SIDEBAR ──
        VBox sidebar = buildSidebar();
        root.setLeft(sidebar);

        // ── CENTER CONTENT ──
        mContentStack = new StackPane();
        mContentStack.getStyleClass().add("wizard-content-area");
        mContentStack.setPadding(new Insets(0));

        for (WizardStep step : mSteps)
        {
            step.contentNode.setVisible(false);
            step.contentNode.setManaged(false);
            mContentStack.getChildren().add(step.contentNode);
        }

        root.setCenter(mContentStack);

        // ── BOTTOM BUTTON BAR ──
        HBox buttonBar = buildButtonBar();
        root.setBottom(buttonBar);

        // Set up the dialog
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setContent(root);
        dialogPane.getStyleClass().add("wizard-dialog-pane");
        dialogPane.setPadding(new Insets(0));
        dialogPane.setMinSize(820, 560);

        // Hide the default button bar (we use our own)
        dialogPane.getButtonTypes().add(ButtonType.CANCEL);
        Node cancelBtn = dialogPane.lookupButton(ButtonType.CANCEL);
        if (cancelBtn != null)
        {
            cancelBtn.setVisible(false);
            cancelBtn.setManaged(false);
        }
        Node builtinBar = dialogPane.lookup(".button-bar");
        if (builtinBar != null) { builtinBar.setVisible(false); builtinBar.setManaged(false); }
        // Hide the built-in button bar container
        for (Node child : dialogPane.getChildren())
        {
            if (child instanceof ButtonBar)
            {
                child.setVisible(false);
                child.setManaged(false);
            }
        }

        // Apply stylesheets
        URL mainCss = getClass().getResource("/sdrtrunk_style.css");
        if (mainCss != null)
        {
            dialogPane.getStylesheets().add(mainCss.toExternalForm());
        }

        // Show the first step
        showStep(0);

        // Register for theming
        dialogPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null)
            {
                ThemeManager.registerScene(newScene);
            }
        });

        dialog.showAndWait();

        // Mark wizard as completed
        Preferences p = Preferences.userNodeForPackage(io.github.dsheirer.gui.SDRTrunk.class);
        p.putBoolean("sdrtrunk.first.time.wizard.completed", true);
    }

    // ═══════════════════════════════════════════════════════
    //  SIDEBAR
    // ═══════════════════════════════════════════════════════

    private VBox buildSidebar()
    {
        VBox sidebar = new VBox(0);
        sidebar.setPrefWidth(220);
        sidebar.setMinWidth(220);
        sidebar.setMaxWidth(220);
        sidebar.getStyleClass().add("wizard-sidebar");
        sidebar.setStyle("-fx-background-color: linear-gradient(to bottom, " + SIDEBAR_DARK + ", " + SIDEBAR_MID + ");");

        // Logo + Title section
        VBox brandBox = new VBox(8);
        brandBox.setAlignment(Pos.CENTER);
        brandBox.setPadding(new Insets(30, 20, 20, 20));

        try
        {
            URL iconUrl = getClass().getResource("/images/SDRTrunk_Application_Icon.png");
            if (iconUrl != null)
            {
                ImageView logo = new ImageView(new Image(iconUrl.toExternalForm(), 80, 80, true, true));
                logo.setEffect(new DropShadow(10, Color.rgb(0, 0, 0, 0.5)));
                brandBox.getChildren().add(logo);
            }
        }
        catch (Exception e)
        {
            // Skip logo if not found
        }

        Label titleLabel = new Label("SDRTrunk");
        titleLabel.setFont(Font.font("SansSerif", FontWeight.BOLD, 18));
        titleLabel.setTextFill(Color.WHITE);

        Label editionLabel = new Label("Kennebec Edition");
        editionLabel.setFont(Font.font("SansSerif", FontWeight.NORMAL, 12));
        editionLabel.setTextFill(Color.web("#8899AA"));

        Label subtitleLabel = new Label("First Time Setup");
        subtitleLabel.setFont(Font.font("SansSerif", FontWeight.SEMI_BOLD, 11));
        subtitleLabel.setTextFill(Color.web("#6688AA"));
        subtitleLabel.setPadding(new Insets(4, 0, 0, 0));

        brandBox.getChildren().addAll(titleLabel, editionLabel, subtitleLabel);
        sidebar.getChildren().add(brandBox);

        // Separator
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setMaxHeight(1);
        sep.setStyle("-fx-background-color: rgba(255,255,255,0.1);");
        HBox.setHgrow(sep, Priority.ALWAYS);
        HBox sepBox = new HBox(sep);
        sepBox.setPadding(new Insets(0, 20, 0, 20));
        HBox.setHgrow(sep, Priority.ALWAYS);
        sidebar.getChildren().add(sepBox);

        // Step indicators
        mStepIndicatorContainer = new VBox(2);
        mStepIndicatorContainer.setPadding(new Insets(16, 12, 16, 12));
        VBox.setVgrow(mStepIndicatorContainer, Priority.ALWAYS);
        sidebar.getChildren().add(mStepIndicatorContainer);

        // Footer
        Label footerLabel = new Label("v00.090");
        footerLabel.setFont(Font.font("SansSerif", FontWeight.NORMAL, 10));
        footerLabel.setTextFill(Color.web("#556677"));
        VBox footerBox = new VBox(footerLabel);
        footerBox.setAlignment(Pos.CENTER);
        footerBox.setPadding(new Insets(10, 20, 16, 20));
        sidebar.getChildren().add(footerBox);

        return sidebar;
    }

    private void refreshStepIndicators()
    {
        mStepIndicatorContainer.getChildren().clear();

        for (int i = 0; i < mSteps.size(); i++)
        {
            WizardStep step = mSteps.get(i);
            boolean isCurrent = (i == mCurrentStep);
            boolean isCompleted = (i < mCurrentStep);

            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(6, 8, 6, 8));

            if (isCurrent)
            {
                row.setStyle("-fx-background-color: rgba(0,122,255,0.2); -fx-background-radius: 8;");
            }

            // Step circle
            StackPane circlePane = new StackPane();
            circlePane.setMinSize(24, 24);
            circlePane.setMaxSize(24, 24);

            Circle circle = new Circle(12);

            if (isCompleted)
            {
                circle.setFill(Color.web(ACCENT_GREEN));
                // Checkmark
                SVGPath check = new SVGPath();
                check.setContent("M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z");
                check.setFill(Color.WHITE);
                check.setScaleX(0.5);
                check.setScaleY(0.5);
                circlePane.getChildren().addAll(circle, check);
            }
            else if (isCurrent)
            {
                circle.setFill(Color.web(ACCENT_BLUE));
                Label num = new Label(String.valueOf(i + 1));
                num.setFont(Font.font("SansSerif", FontWeight.BOLD, 11));
                num.setTextFill(Color.WHITE);
                circlePane.getChildren().addAll(circle, num);
            }
            else
            {
                circle.setFill(Color.TRANSPARENT);
                circle.setStroke(Color.web("#556677"));
                circle.setStrokeWidth(1.5);
                Label num = new Label(String.valueOf(i + 1));
                num.setFont(Font.font("SansSerif", FontWeight.NORMAL, 11));
                num.setTextFill(Color.web("#556677"));
                circlePane.getChildren().addAll(circle, num);
            }

            row.getChildren().add(circlePane);

            // Step label
            Label label = new Label(step.shortTitle);
            label.setFont(Font.font("SansSerif", isCurrent ? FontWeight.SEMI_BOLD : FontWeight.NORMAL, 12));
            label.setTextFill(isCurrent ? Color.WHITE : (isCompleted ? Color.web("#AAC0DD") : Color.web("#667788")));
            label.setWrapText(true);
            label.setMaxWidth(140);
            row.getChildren().add(label);

            mStepIndicatorContainer.getChildren().add(row);
        }
    }

    // ═══════════════════════════════════════════════════════
    //  BUTTON BAR
    // ═══════════════════════════════════════════════════════

    private HBox buildButtonBar()
    {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_RIGHT);
        bar.setPadding(new Insets(14, 24, 14, 24));
        bar.getStyleClass().add("wizard-button-bar");
        bar.setStyle("-fx-background-color: #F2F2F7; -fx-border-color: #E5E5EA transparent transparent transparent; -fx-border-width: 1 0 0 0;");

        mBackButton = new Button("← Back");
        mBackButton.getStyleClass().addAll("wizard-nav-button");
        mBackButton.setOnAction(e -> navigateBack());

        mSkipButton = new Button("Skip");
        mSkipButton.getStyleClass().addAll("wizard-nav-button");
        mSkipButton.setOnAction(e -> navigateNext());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        mNextButton = new Button("Next →");
        mNextButton.getStyleClass().addAll("wizard-nav-button", "wizard-nav-button-primary");
        mNextButton.setOnAction(e -> navigateNext());

        bar.getChildren().addAll(mBackButton, spacer, mSkipButton, mNextButton);

        return bar;
    }

    // ═══════════════════════════════════════════════════════
    //  NAVIGATION
    // ═══════════════════════════════════════════════════════

    private void showStep(int index)
    {
        if (index < 0 || index >= mSteps.size()) return;

        mCurrentStep = index;

        // Show/hide content panes
        for (int i = 0; i < mSteps.size(); i++)
        {
            boolean show = (i == index);
            mSteps.get(i).contentNode.setVisible(show);
            mSteps.get(i).contentNode.setManaged(show);
        }

        // Update buttons
        mBackButton.setDisable(index == 0 || mTaskRunning);
        mSkipButton.setVisible(index > 0 && index < mSteps.size() - 1);
        mSkipButton.setManaged(mSkipButton.isVisible());
        mSkipButton.setDisable(mTaskRunning);

        boolean isLast = (index == mSteps.size() - 1);
        mNextButton.setText(isLast ? "Finish ✓" : "Next →");

        // Refresh sidebar
        refreshStepIndicators();

        // Run auto-start tasks (like GPU detection)
        WizardStep currentStep = mSteps.get(index);
        if (currentStep.autoStartTask != null && !currentStep.autoTaskStarted)
        {
            currentStep.autoTaskStarted = true;
            currentStep.autoStartTask.run();
        }
    }

    private void navigateNext()
    {
        if (mTaskRunning) return;

        if (mCurrentStep < mSteps.size() - 1)
        {
            showStep(mCurrentStep + 1);
        }
        else
        {
            // Final step — close the dialog
            // Find the dialog and close it
            mContentStack.getScene().getWindow().hide();
        }
    }

    private void navigateBack()
    {
        if (mTaskRunning) return;

        if (mCurrentStep > 0)
        {
            showStep(mCurrentStep - 1);
        }
    }

    /**
     * Sets whether a task is currently running, which disables navigation.
     */
    private void setTaskRunning(boolean running)
    {
        mTaskRunning = running;
        Platform.runLater(() -> {
            mNextButton.setDisable(running);
            mBackButton.setDisable(running || mCurrentStep == 0);
            mSkipButton.setDisable(running);
        });
    }

    // ═══════════════════════════════════════════════════════
    //  STEP DEFINITIONS
    // ═══════════════════════════════════════════════════════

    private void buildSteps()
    {
        mSteps.add(buildWelcomeStep());
        mSteps.add(buildJmbeStep());
        mSteps.add(buildCalibrationStep());

        String osName = System.getProperty("os.name");
        boolean isWindows = osName != null && osName.toLowerCase().contains("win");
        if (isWindows)
        {
            mSteps.add(buildSystemOptimizationsStep());
        }

        mSteps.add(buildRemoteDesktopStep());
        mSteps.add(buildAIStep());
        mSteps.add(buildGpuStep());
        mSteps.add(buildMemoryStep());
    }

    // ── Shared step content builder ──

    private VBox createStepLayout(String title, String description, Node... content)
    {
        VBox layout = new VBox(0);
        layout.getStyleClass().add("wizard-step-content");

        // Header area
        VBox headerBox = new VBox(4);
        headerBox.setPadding(new Insets(28, 32, 16, 32));
        headerBox.setStyle("-fx-background-color: linear-gradient(to bottom, #F8F9FB, #FFFFFF);");

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("SansSerif", FontWeight.BOLD, 22));
        titleLabel.setTextFill(Color.web("#1C1C1E"));
        titleLabel.setWrapText(true);
        titleLabel.getStyleClass().add("wizard-step-title");

        Label descLabel = new Label(description);
        descLabel.setFont(Font.font("SansSerif", FontWeight.NORMAL, 13));
        descLabel.setTextFill(Color.web("#8E8E93"));
        descLabel.setWrapText(true);
        descLabel.getStyleClass().add("wizard-step-description");

        headerBox.getChildren().addAll(titleLabel, descLabel);
        layout.getChildren().add(headerBox);

        // Content area
        VBox contentBox = new VBox(12);
        contentBox.setPadding(new Insets(8, 32, 24, 32));
        VBox.setVgrow(contentBox, Priority.ALWAYS);

        for (Node node : content)
        {
            contentBox.getChildren().add(node);
        }

        ScrollPane scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scrollPane.getStyleClass().add("wizard-scroll");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        layout.getChildren().add(scrollPane);

        return layout;
    }

    // ── Welcome ──

    private WizardStep buildWelcomeStep()
    {
        // Welcome graphic
        VBox centerContent = new VBox(16);
        centerContent.setAlignment(Pos.CENTER);
        centerContent.setPadding(new Insets(20, 0, 0, 0));

        Label welcomeEmoji = new Label("🎉");
        welcomeEmoji.setFont(Font.font(48));

        Label mainMsg = new Label("Welcome to SDRTrunk Kennebec");
        mainMsg.setFont(Font.font("SansSerif", FontWeight.BOLD, 20));
        mainMsg.setTextFill(Color.web("#1C1C1E"));
        mainMsg.getStyleClass().add("wizard-step-title");

        Label subMsg = new Label(
            "This wizard will guide you through the essential first-time setup.\n\n" +
            "We'll configure your audio libraries, calibration, system optimizations,\n" +
            "AI features, and memory settings.\n\n" +
            "You can skip any step and configure it later in User Preferences.");
        subMsg.setWrapText(true);
        subMsg.setTextFill(Color.web("#3A3A3C"));
        subMsg.setFont(Font.font("SansSerif", 14));
        subMsg.getStyleClass().add("wizard-step-description");
        subMsg.setMaxWidth(420);
        subMsg.setAlignment(Pos.CENTER);

        // Feature cards
        HBox features = new HBox(16);
        features.setAlignment(Pos.CENTER);
        features.setPadding(new Insets(16, 0, 0, 0));

        features.getChildren().addAll(
            createFeatureCard("🔊", "Audio", "JMBE codec setup"),
            createFeatureCard("📡", "Tuning", "SDR calibration"),
            createFeatureCard("🤖", "AI", "Gemini integration"),
            createFeatureCard("⚡", "Performance", "GPU & memory")
        );

        centerContent.getChildren().addAll(welcomeEmoji, mainMsg, subMsg, features);

        VBox layout = new VBox(0);
        layout.getStyleClass().add("wizard-step-content");

        VBox headerBox = new VBox(4);
        headerBox.setPadding(new Insets(28, 32, 16, 32));
        headerBox.setStyle("-fx-background-color: linear-gradient(to bottom, #F8F9FB, #FFFFFF);");

        layout.getChildren().add(headerBox);

        VBox contentBox = new VBox(centerContent);
        contentBox.setPadding(new Insets(0, 32, 24, 32));
        contentBox.setAlignment(Pos.CENTER);
        VBox.setVgrow(contentBox, Priority.ALWAYS);
        layout.getChildren().add(contentBox);

        return new WizardStep("Welcome", layout);
    }

    private VBox createFeatureCard(String emoji, String title, String subtitle)
    {
        VBox card = new VBox(4);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(12, 16, 12, 16));
        card.setStyle("-fx-background-color: #F2F2F7; -fx-background-radius: 12;");
        card.setPrefWidth(110);
        card.getStyleClass().add("wizard-feature-card");

        Label emojiLabel = new Label(emoji);
        emojiLabel.setFont(Font.font(24));

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("SansSerif", FontWeight.BOLD, 12));
        titleLabel.setTextFill(Color.web("#1C1C1E"));
        titleLabel.getStyleClass().add("wizard-feature-title");

        Label subLabel = new Label(subtitle);
        subLabel.setFont(Font.font("SansSerif", 10));
        subLabel.setTextFill(Color.web("#8E8E93"));
        subLabel.getStyleClass().add("wizard-feature-subtitle");

        card.getChildren().addAll(emojiLabel, titleLabel, subLabel);
        return card;
    }

    // ── JMBE ──

    private WizardStep buildJmbeStep()
    {
        ToggleGroup group = new ToggleGroup();
        RadioButton regularBtn = new RadioButton("Regular (dsheirer)");
        regularBtn.setToggleGroup(group);
        regularBtn.setSelected(true);
        RadioButton bazinetaBtn = new RadioButton("Bazineta Fork (Alternative)");
        bazinetaBtn.setToggleGroup(group);
        HBox radioBox = new HBox(12, regularBtn, bazinetaBtn);
        radioBox.setPadding(new Insets(4, 0, 4, 0));

        TextArea consoleArea = new TextArea();
        consoleArea.setEditable(false);
        consoleArea.setPrefRowCount(6);
        consoleArea.setWrapText(true);
        consoleArea.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 11px;");
        consoleArea.getStyleClass().add("wizard-console");

        ProgressBar progressBar = new ProgressBar();
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setVisible(false);
        progressBar.setManaged(false);
        progressBar.getStyleClass().add("wizard-progress");

        Label statusLabel = new Label("");
        statusLabel.setFont(Font.font("SansSerif", FontWeight.SEMI_BOLD, 12));
        statusLabel.getStyleClass().add("wizard-status-label");
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);

        Button createBtn = new Button("▶  Build JMBE Library");
        createBtn.getStyleClass().addAll("wizard-action-button");
        createBtn.setStyle("-fx-background-color: " + ACCENT_BLUE + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 20 8 20; -fx-font-weight: bold; -fx-cursor: hand;");

        createBtn.setOnAction(e -> {
            createBtn.setDisable(true);
            progressBar.setVisible(true);
            progressBar.setManaged(true);
            statusLabel.setText("Building JMBE library...");
            statusLabel.setTextFill(Color.web(ACCENT_BLUE));
            statusLabel.setVisible(true);
            statusLabel.setManaged(true);
            setTaskRunning(true);
            boolean useBazineta = bazinetaBtn.isSelected();
            consoleArea.setText("Starting build...\n");

            Task<Void> task = new Task<>()
            {
                @Override
                protected Void call() throws Exception
                {
                    Release release = GitHub.getLatestRelease(useBazineta ? JmbeCreator.GITHUB_BAZINETA_JMBE_RELEASES_URL : JmbeCreator.GITHUB_JMBE_RELEASES_URL);
                    Path libraryPath = mUserPreferences.getDirectoryPreference().getDirectoryApplicationRoot().resolve("jmbe").resolve("jmbe.jar");
                    JmbeCreator creator = new JmbeCreator(release, libraryPath, useBazineta);

                    creator.consoleOutputProperty().addListener((obs, oldVal, newVal) -> {
                        Platform.runLater(() -> consoleArea.setText(newVal));
                    });

                    creator.execute();

                    while (!creator.completeProperty().get())
                    {
                        Thread.sleep(100);
                    }

                    Platform.runLater(() -> {
                        progressBar.setVisible(false);
                        progressBar.setManaged(false);
                        setTaskRunning(false);

                        if (creator.hasErrors())
                        {
                            statusLabel.setText("✗  Build failed — check the output above");
                            statusLabel.setTextFill(Color.web("#FF3B30"));
                            createBtn.setDisable(false);
                        }
                        else
                        {
                            Preferences p = Preferences.userNodeForPackage(io.github.dsheirer.preference.decoder.JmbeLibraryPreference.class);
                            p.put("sdrtrunk.decoder.jmbe.library.path", libraryPath.toAbsolutePath().toString());
                            statusLabel.setText("✓  JMBE Library built and installed successfully!");
                            statusLabel.setTextFill(Color.web(ACCENT_GREEN));
                        }
                    });
                    return null;
                }
            };
            new Thread(task).start();
        });

        VBox layout = createStepLayout(
            "JMBE Audio Library",
            "The JMBE (Java Multi-Band Excitation) library is required to decode P25 Phase I/II digital audio. " +
            "Due to patent/licensing restrictions, this library cannot be distributed with the application and must be compiled.",
            new Label("Select library source:"),
            radioBox,
            createBtn,
            progressBar,
            statusLabel,
            new Label("Build Output:"),
            consoleArea
        );

        return new WizardStep("JMBE Library", layout);
    }

    // ── Calibration ──

    private WizardStep buildCalibrationStep()
    {
        Button calBtn = new Button("▶  Start Calibration Process");
        calBtn.setStyle("-fx-background-color: " + ACCENT_BLUE + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 20 8 20; -fx-font-weight: bold; -fx-cursor: hand;");

        Label statusLabel = new Label("");
        statusLabel.setFont(Font.font("SansSerif", FontWeight.SEMI_BOLD, 12));
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);

        calBtn.setOnAction(e -> {
            if (mWindowManager != null)
            {
                CalibrationDialog dialog = mWindowManager.getCalibrationDialog(mUserPreferences);
                dialog.showAndWait();
                statusLabel.setText("✓  Calibration complete");
                statusLabel.setTextFill(Color.web(ACCENT_GREEN));
                statusLabel.setVisible(true);
                statusLabel.setManaged(true);
            }
            else
            {
                statusLabel.setText("✗  Calibration requires the main window to be initialized");
                statusLabel.setTextFill(Color.web("#FF3B30"));
                statusLabel.setVisible(true);
                statusLabel.setManaged(true);
            }
        });

        VBox layout = createStepLayout(
            "SDR Tuner Calibration",
            "SDR hardware tuners often have frequency offsets (PPM error). " +
            "SDRTrunk can automatically calculate and apply this correction factor for accurate tuning.",
            calBtn,
            statusLabel
        );

        return new WizardStep("Calibration", layout);
    }

    // ── System Optimizations ──

    private WizardStep buildSystemOptimizationsStep()
    {
        ProgressBar progressBar = new ProgressBar();
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setVisible(false);
        progressBar.setManaged(false);
        progressBar.getStyleClass().add("wizard-progress");

        Label statusLabel = new Label("");
        statusLabel.setFont(Font.font("SansSerif", FontWeight.SEMI_BOLD, 12));
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);

        Button installBtn = new Button("▶  Apply Recommended Optimizations");
        installBtn.setStyle("-fx-background-color: " + ACCENT_BLUE + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 20 8 20; -fx-font-weight: bold; -fx-cursor: hand;");

        installBtn.setOnAction(e -> {
            installBtn.setDisable(true);
            progressBar.setVisible(true);
            progressBar.setManaged(true);
            statusLabel.setText("Applying optimizations... please approve the UAC prompt.");
            statusLabel.setTextFill(Color.web(ACCENT_BLUE));
            statusLabel.setVisible(true);
            statusLabel.setManaged(true);
            setTaskRunning(true);

            Task<Boolean> task = new Task<>()
            {
                @Override
                protected Boolean call() throws Exception
                {
                    String optScript = io.github.dsheirer.gui.preference.diagnostics.WindowsHostOptimizer.getOptimizationScriptContent();
                    String usbScript = UsbMonitorManager.prepareAndGetScheduledTaskScript(mUserPreferences);

                    if (usbScript.isEmpty()) return false;

                    String combinedScript = optScript + "\n" + usbScript + "\nexit 0;";
                    String encoded = java.util.Base64.getEncoder().encodeToString(combinedScript.getBytes(java.nio.charset.StandardCharsets.UTF_16LE));

                    ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command",
                        "Start-Process powershell.exe -ArgumentList '-NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -EncodedCommand " + encoded + "' -Verb RunAs -Wait");
                    Process p = pb.start();
                    int exitCode = p.waitFor();

                    if (exitCode == 0)
                    {
                        mUserPreferences.getApplicationPreference().setUsbMonitorInstalled(true);
                        mUserPreferences.getApplicationPreference().setUsbMonitorPrompted(true);
                        try
                        {
                            Preferences prefs = Preferences.userNodeForPackage(io.github.dsheirer.gui.SDRTrunk.class);
                            prefs.putBoolean("sdrtrunk.diagnostics.powerthrottling.prompted", true);
                        }
                        catch (Exception ex) { /* ignore */ }
                    }
                    return exitCode == 0;
                }
            };

            task.setOnSucceeded(ev -> {
                progressBar.setVisible(false);
                progressBar.setManaged(false);
                setTaskRunning(false);

                if (task.getValue())
                {
                    statusLabel.setText("✓  Optimizations applied successfully!");
                    statusLabel.setTextFill(Color.web(ACCENT_GREEN));
                }
                else
                {
                    statusLabel.setText("✗  Failed — make sure you click Yes on the UAC prompt");
                    statusLabel.setTextFill(Color.web("#FF3B30"));
                    installBtn.setDisable(false);
                }
            });

            task.setOnFailed(ev -> {
                progressBar.setVisible(false);
                progressBar.setManaged(false);
                setTaskRunning(false);
                statusLabel.setText("✗  An error occurred while applying optimizations");
                statusLabel.setTextFill(Color.web("#FF3B30"));
                installBtn.setDisable(false);
            });

            new Thread(task).start();
        });

        VBox layout = createStepLayout(
            "System Optimizations",
            "SDRTrunk Kennebec requires two system optimizations for maximum performance and stability on Windows:\n\n" +
            "• USB Monitor — automatically resets crashed/locked SDR hardware\n" +
            "• Windows EcoQoS Fix — disables power throttling that causes audio dropouts\n\n" +
            "Applying these requires Administrator permissions. You will receive one UAC prompt for both.",
            installBtn,
            progressBar,
            statusLabel
        );

        return new WizardStep("Optimizations", layout);
    }

    // ── Remote Desktop ──

    private WizardStep buildRemoteDesktopStep()
    {
        Preferences appPrefs = Preferences.userNodeForPackage(io.github.dsheirer.gui.SDRTrunk.class);

        CheckBox optimizeChk = new CheckBox("Enable Remote Desktop Optimizations");
        optimizeChk.setSelected(appPrefs.getBoolean("sdrtrunk.rdp.optimizations", false));
        optimizeChk.setOnAction(e -> appPrefs.putBoolean("sdrtrunk.rdp.optimizations", optimizeChk.isSelected()));

        VBox layout = createStepLayout(
            "Remote Desktop Optimizations",
            "When connecting via RustDesk or other remote desktop tools, this option will disable some animations " +
            "and simplify the GUI to improve responsiveness over slow connections.",
            optimizeChk
        );

        return new WizardStep("Remote Desktop", layout);
    }

    // ── AI ──

    private WizardStep buildAIStep()
    {
        CheckBox enableAiChk = new CheckBox("Enable AI Features");
        enableAiChk.setSelected(mUserPreferences.getAIPreference().isAIEnabled());

        TextField apiKeyField = new TextField(mUserPreferences.getAIPreference().getGeminiApiKey());
        apiKeyField.setPromptText("Enter Gemini API Key");
        apiKeyField.setMaxWidth(420);

        ProgressBar progressBar = new ProgressBar();
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setVisible(false);
        progressBar.setManaged(false);
        progressBar.getStyleClass().add("wizard-progress");

        Label statusLabel = new Label("");
        statusLabel.setFont(Font.font("SansSerif", FontWeight.SEMI_BOLD, 12));
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);

        Button testBtn = new Button("▶  Test API Key");
        testBtn.setStyle("-fx-background-color: " + ACCENT_BLUE + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 20 8 20; -fx-font-weight: bold; -fx-cursor: hand;");

        testBtn.setOnAction(e -> {
            String key = apiKeyField.getText();
            if (key == null || key.isBlank())
            {
                statusLabel.setText("✗  Please enter an API Key first");
                statusLabel.setTextFill(Color.web("#FF3B30"));
                statusLabel.setVisible(true);
                statusLabel.setManaged(true);
                return;
            }

            testBtn.setDisable(true);
            progressBar.setVisible(true);
            progressBar.setManaged(true);
            statusLabel.setText("Testing API key...");
            statusLabel.setTextFill(Color.web(ACCENT_BLUE));
            statusLabel.setVisible(true);
            statusLabel.setManaged(true);
            setTaskRunning(true);

            Task<Boolean> task = new Task<>()
            {
                @Override
                protected Boolean call() throws Exception
                {
                    return testApiKey(key);
                }
            };

            task.setOnSucceeded(ev -> {
                progressBar.setVisible(false);
                progressBar.setManaged(false);
                setTaskRunning(false);

                if (task.getValue())
                {
                    mUserPreferences.getAIPreference().setGeminiApiKey(key);
                    mUserPreferences.getAIPreference().setAIEnabled(enableAiChk.isSelected());
                    statusLabel.setText("✓  API Key validated successfully!");
                    statusLabel.setTextFill(Color.web(ACCENT_GREEN));
                }
                else
                {
                    apiKeyField.setText("");
                    mUserPreferences.getAIPreference().setGeminiApiKey("");
                    mUserPreferences.getAIPreference().setAIEnabled(false);
                    enableAiChk.setSelected(false);
                    statusLabel.setText("✗  API Key test failed — check the key and try again");
                    statusLabel.setTextFill(Color.web("#FF3B30"));
                    testBtn.setDisable(false);
                }
            });

            task.setOnFailed(ev -> {
                progressBar.setVisible(false);
                progressBar.setManaged(false);
                setTaskRunning(false);
                statusLabel.setText("✗  An error occurred while testing the API key");
                statusLabel.setTextFill(Color.web("#FF3B30"));
                testBtn.setDisable(false);
            });

            new Thread(task).start();
        });

        VBox layout = createStepLayout(
            "Gemini AI Features",
            "SDRTrunk Kennebec features AI integrations using Google's Gemini API to summarize " +
            "transcripts and identify channel activity. A valid API Key is required — you can get one for free from Google AI Studio.",
            enableAiChk,
            new Label("API Key:"),
            apiKeyField,
            testBtn,
            progressBar,
            statusLabel
        );

        return new WizardStep("AI Features", layout);
    }

    private boolean testApiKey(String key)
    {
        try
        {
            java.net.URL url = new java.net.URI("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + key).toURL();
            java.net.HttpURLConnection con = (java.net.HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);

            String jsonInputString = "{\"contents\":[{\"parts\":[{\"text\":\"Hello\"}]}]}";
            try (java.io.OutputStream os = con.getOutputStream())
            {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            int code = con.getResponseCode();
            return code == 200;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    // ── GPU ──

    private WizardStep buildGpuStep()
    {
        ProgressBar progressBar = new ProgressBar();
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.getStyleClass().add("wizard-progress");

        Label statusLabel = new Label("Scanning for compatible OpenCL devices...");
        statusLabel.setFont(Font.font("SansSerif", FontWeight.SEMI_BOLD, 12));
        statusLabel.setTextFill(Color.web(ACCENT_BLUE));

        CheckBox enableGpuBox = new CheckBox("Enable OpenCL GPU Acceleration");
        enableGpuBox.setDisable(true);

        enableGpuBox.setOnAction(e -> {
            Preferences p = Preferences.userNodeForPackage(DisplayPreference.class);
            p.putBoolean("opencl.enabled", enableGpuBox.isSelected());
        });

        VBox layout = createStepLayout(
            "Hardware Acceleration (GPU)",
            "SDRTrunk can offload massive DSP math operations to your GPU via OpenCL. " +
            "This dramatically lowers CPU utilization, but requires stable graphics drivers.",
            progressBar,
            statusLabel,
            enableGpuBox
        );

        // The auto-start task that runs when this step becomes visible
        Runnable autoTask = () -> {
            setTaskRunning(true);
            Task<Boolean> checkTask = new Task<>()
            {
                @Override
                protected Boolean call()
                {
                    return GpuDetector.isGpuAvailable();
                }
            };

            checkTask.setOnSucceeded(e -> {
                progressBar.setVisible(false);
                progressBar.setManaged(false);
                setTaskRunning(false);

                boolean hasGpu = checkTask.getValue();
                if (hasGpu)
                {
                    statusLabel.setText("✓  Compatible GPU Detected!");
                    statusLabel.setTextFill(Color.web(ACCENT_GREEN));
                    enableGpuBox.setDisable(false);
                    enableGpuBox.setSelected(true);
                    Preferences p = Preferences.userNodeForPackage(DisplayPreference.class);
                    p.putBoolean("opencl.enabled", true);
                }
                else
                {
                    statusLabel.setText("✗  No compatible OpenCL GPU found");
                    statusLabel.setTextFill(Color.web("#FF3B30"));
                }
            });

            checkTask.setOnFailed(e -> {
                progressBar.setVisible(false);
                progressBar.setManaged(false);
                setTaskRunning(false);
                statusLabel.setText("✗  GPU detection failed");
                statusLabel.setTextFill(Color.web("#FF3B30"));
            });

            Thread t = new Thread(checkTask);
            t.setDaemon(true);
            t.start();
        };

        WizardStep step = new WizardStep("GPU Accel.", layout);
        step.autoStartTask = autoTask;
        return step;
    }

    // ── Memory ──

    private WizardStep buildMemoryStep()
    {
        ComboBox<Integer> memoryCombo = new ComboBox<>();
        memoryCombo.getItems().addAll(2, 4, 6, 8, 12, 16, 24, 32);
        memoryCombo.setValue(6);

        Label statusLabel = new Label("");
        statusLabel.setFont(Font.font("SansSerif", FontWeight.SEMI_BOLD, 12));
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);

        Button applyBtn = new Button("▶  Set Memory Limit & Restart");
        applyBtn.setStyle("-fx-background-color: " + ACCENT_BLUE + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 20 8 20; -fx-font-weight: bold; -fx-cursor: hand;");

        applyBtn.setOnAction(e -> {
            try
            {
                Path appRoot = mUserPreferences.getDirectoryPreference().getDirectoryApplicationRoot();
                Path memoryFile = appRoot.resolve("SDRTrunk.memory");
                Files.writeString(memoryFile, memoryCombo.getValue().toString());

                Preferences p = Preferences.userNodeForPackage(io.github.dsheirer.gui.SDRTrunk.class);
                p.putBoolean("sdrtrunk.first.time.wizard.completed", true);

                statusLabel.setText("✓  Memory limit set to " + memoryCombo.getValue() + "GB — restarting...");
                statusLabel.setTextFill(Color.web(ACCENT_GREEN));
                statusLabel.setVisible(true);
                statusLabel.setManaged(true);

                // Delay exit slightly so user sees the message
                new Thread(() -> {
                    try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                    System.exit(0);
                }).start();
            }
            catch (IOException ex)
            {
                statusLabel.setText("✗  Failed to write memory settings");
                statusLabel.setTextFill(Color.web("#FF3B30"));
                statusLabel.setVisible(true);
                statusLabel.setManaged(true);
            }
        });

        HBox memRow = new HBox(10, new Label("Max Memory (GB):"), memoryCombo);
        memRow.setAlignment(Pos.CENTER_LEFT);

        VBox layout = createStepLayout(
            "System Memory Allocation",
            "SDRTrunk is memory intensive when monitoring many channels or using the waterfall display. " +
            "Select the maximum memory (RAM) the application is allowed to use. A minimum of 4GB is recommended.\n\n" +
            "This is the final step. Applying this setting will prompt a restart of the application.",
            memRow,
            applyBtn,
            statusLabel
        );

        return new WizardStep("Memory", layout);
    }

    // ═══════════════════════════════════════════════════════
    //  STEP MODEL
    // ═══════════════════════════════════════════════════════

    private static class WizardStep
    {
        final String shortTitle;
        final Node contentNode;
        Runnable autoStartTask;
        boolean autoTaskStarted = false;

        WizardStep(String shortTitle, Node contentNode)
        {
            this.shortTitle = shortTitle;
            this.contentNode = contentNode;
        }
    }
}