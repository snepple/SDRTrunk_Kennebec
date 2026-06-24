package io.github.dsheirer.gui.wizard;

import io.github.dsheirer.gui.UsbMonitorManager;
import io.github.dsheirer.preference.ai.GeminiModel;
import java.util.List;
import io.github.dsheirer.gui.preference.calibration.CalibrationDialog;
import io.github.dsheirer.jmbe.JmbeCreator;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.gui.JavaFxWindowManager;
import io.github.dsheirer.gui.theme.ThemeManager;
import io.github.dsheirer.jmbe.github.GitHub;
import io.github.dsheirer.jmbe.github.Release;
import io.github.dsheirer.dsp.opencl.GpuDetector;
import io.github.dsheirer.preference.display.DisplayPreference;
import io.github.dsheirer.preference.record.RecordPreference;
import io.github.dsheirer.record.RecordFormat;

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
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
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
        mSteps.add(buildRadioReferenceStep());
        mSteps.add(buildNotificationsStep());
        mSteps.add(buildAudioRecordingStep());
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
        headerBox.getStyleClass().add("wizard-header-box");

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("SansSerif", FontWeight.BOLD, 22));
        titleLabel.setWrapText(true);
        titleLabel.getStyleClass().add("wizard-step-title");

        Label descLabel = new Label(description);
        descLabel.setFont(Font.font("SansSerif", FontWeight.NORMAL, 13));
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

    /**
     * Creates a styled info box with an icon prefix for important notes.
     */
    private HBox createInfoBox(String icon, String text)
    {
        HBox box = new HBox(8);
        box.setPadding(new Insets(10, 14, 10, 14));
        box.getStyleClass().add("wizard-info-box");

        Label iconLabel = new Label(icon);
        iconLabel.setFont(Font.font(16));
        iconLabel.setMinWidth(24);

        Label textLabel = new Label(text);
        textLabel.setFont(Font.font("SansSerif", FontWeight.NORMAL, 12));
        textLabel.setWrapText(true);
        HBox.setHgrow(textLabel, Priority.ALWAYS);

        box.getChildren().addAll(iconLabel, textLabel);
        return box;
    }

    /**
     * Creates a styled warning box for skip consequences.
     */
    private HBox createWarningBox(String text)
    {
        HBox box = new HBox(8);
        box.setPadding(new Insets(10, 14, 10, 14));
        box.getStyleClass().add("wizard-warning-box");

        Label iconLabel = new Label("⚠️");
        iconLabel.setFont(Font.font(16));
        iconLabel.setMinWidth(24);

        Label textLabel = new Label(text);
        textLabel.setFont(Font.font("SansSerif", FontWeight.NORMAL, 12));
        textLabel.setWrapText(true);
        HBox.setHgrow(textLabel, Priority.ALWAYS);

        box.getChildren().addAll(iconLabel, textLabel);
        return box;
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
        mainMsg.getStyleClass().add("wizard-step-title");

        Label subMsg = new Label(
            "This wizard will guide you through the essential first-time setup.\n" +
            "It takes approximately 3–5 minutes to complete.\n\n" +
            "We'll configure your audio libraries, SDR tuner calibration, system\n" +
            "optimizations, AI features, notifications, recording, and memory.\n\n" +
            "You can skip any step and configure it later in User Preferences.");
        subMsg.setWrapText(true);
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
        headerBox.getStyleClass().add("wizard-header-box");

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

        card.setPrefWidth(110);
        card.getStyleClass().add("wizard-feature-card");

        Label emojiLabel = new Label(emoji);
        emojiLabel.setFont(Font.font(24));

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("SansSerif", FontWeight.BOLD, 12));
        titleLabel.getStyleClass().add("wizard-feature-title");

        Label subLabel = new Label(subtitle);
        subLabel.setFont(Font.font("SansSerif", 10));
        subLabel.getStyleClass().add("wizard-feature-subtitle");

        card.getChildren().addAll(emojiLabel, titleLabel, subLabel);
        return card;
    }

    // ── JMBE ──

    private WizardStep buildJmbeStep()
    {
        ToggleGroup group = new ToggleGroup();
        RadioButton regularBtn = new RadioButton("Regular (dsheirer) — Standard, well-tested JMBE library");
        regularBtn.setToggleGroup(group);
        regularBtn.setSelected(true);
        RadioButton bazinetaBtn = new RadioButton("Bazineta Fork — Alternative with enhanced AMBE+ support");
        bazinetaBtn.setToggleGroup(group);
        VBox radioBox = new VBox(8, regularBtn, bazinetaBtn);
        radioBox.setPadding(new Insets(4, 0, 4, 0));

        HBox infoBox = createInfoBox("ℹ️",
            "The Regular (dsheirer) library is the upstream default and recommended for most users. " +
            "The Bazineta Fork is a community alternative that may provide improved audio quality " +
            "in some P25 Phase II scenarios. Both are free and open-source.");

        TextArea consoleArea = new TextArea();
        consoleArea.setEditable(false);
        consoleArea.setPrefRowCount(5);
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

        HBox skipBox = createWarningBox(
            "If you skip this step, P25 Phase I/II digital audio will not play. " +
            "You can build the JMBE library later from User Preferences → JMBE Library.");

        VBox layout = createStepLayout(
            "JMBE Audio Library",
            "The JMBE (Java Multi-Band Excitation) library is required to decode P25 Phase I/II digital audio. " +
            "Due to patent/licensing restrictions, this library cannot be distributed with the application and must be compiled on your machine.",
            new Label("Select library source:"),
            radioBox,
            infoBox,
            createBtn,
            progressBar,
            statusLabel,
            new Label("Build Output:"),
            consoleArea,
            skipBox
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

        HBox prerequisiteBox = createInfoBox("📡",
            "Prerequisites: You need an SDR tuner plugged into a USB port and a known " +
            "reference signal (e.g., an NOAA weather radio station at a known frequency) " +
            "to perform calibration.");

        HBox whyBox = createInfoBox("🎯",
            "Why calibrate? Without calibration, your tuner may be off-frequency by several kHz, " +
            "causing missed transmissions, garbled audio, or failure to lock onto trunked control channels.");

        HBox skipBox = createWarningBox(
            "If you skip this step, your tuner will use the default 0 PPM correction. You can " +
            "calibrate later from User Preferences → SDR Tuner Calibration.");

        VBox layout = createStepLayout(
            "SDR Tuner Calibration",
            "SDR hardware tuners have manufacturing variations that cause slight frequency offsets " +
            "(measured in PPM — parts per million). SDRTrunk can automatically calculate and " +
            "apply this correction factor so your tuner is precisely on-frequency.",
            prerequisiteBox,
            whyBox,
            calBtn,
            statusLabel,
            skipBox
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

        HBox usbInfo = createInfoBox("🔌",
            "USB Monitor: Monitors connected SDR hardware and automatically power-cycles devices " +
            "that stop responding. This prevents lockups that would otherwise require you to " +
            "physically unplug and re-plug the USB dongle.");

        HBox ecoInfo = createInfoBox("⚡",
            "Windows EcoQoS Fix: Windows 11 aggressively throttles CPU-intensive apps to save " +
            "power via the EcoQoS API. This causes audio dropouts and missed radio packets in " +
            "SDRTrunk. This fix tells Windows to treat SDRTrunk as a high-priority application.");

        HBox skipBox = createWarningBox(
            "If you skip this step, the application will still work, but you may experience " +
            "occasional SDR hardware lockups requiring manual intervention, and audio quality " +
            "may degrade on laptops running on battery.");

        VBox layout = createStepLayout(
            "System Optimizations",
            "SDRTrunk Kennebec requires two system optimizations for maximum performance and stability on Windows. " +
            "Applying these requires Administrator permissions — you will receive one UAC prompt for both.",
            usbInfo,
            ecoInfo,
            installBtn,
            progressBar,
            statusLabel,
            skipBox
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

        HBox whenBox = createInfoBox("🖥️",
            "Enable this if you primarily access SDRTrunk via RustDesk, TeamViewer, " +
            "Windows Remote Desktop (RDP), VNC, or AnyDesk.");

        HBox whatBox = createInfoBox("🔧",
            "When enabled, the following optimizations are applied:\n" +
            "• Spectrum waterfall updates are reduced to lower frame rates\n" +
            "• Smooth animations are replaced with instant transitions\n" +
            "• Transparency effects and drop shadows are removed\n" +
            "• Chart rendering complexity is reduced\n\n" +
            "This significantly reduces bandwidth usage and improves responsiveness.");

        HBox skipBox = createWarningBox(
            "If you run SDRTrunk locally (not via remote desktop), leave this disabled. " +
            "Enabling it unnecessarily will reduce visual quality.");

        VBox layout = createStepLayout(
            "Remote Desktop Optimizations",
            "When connecting via remote desktop tools, SDRTrunk can simplify its " +
            "GUI rendering to improve responsiveness over slow network connections.",
            optimizeChk,
            whenBox,
            whatBox,
            skipBox
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

            Task<List<GeminiModel>> task = new Task<>()
            {
                @Override
                protected List<GeminiModel> call() throws Exception
                {
                    return io.github.dsheirer.preference.ai.GeminiApiHelper.fetchAvailableModels(key);
                }
            };

            task.setOnSucceeded(ev -> {
                progressBar.setVisible(false);
                progressBar.setManaged(false);
                setTaskRunning(false);

                List<GeminiModel> models = task.getValue();

                if (models != null && !models.isEmpty())
                {
                    statusLabel.setText("✓  API Key validated successfully!");
                    statusLabel.setTextFill(Color.web(ACCENT_GREEN));
                    
                    // Prompt user to select a model
                    java.util.Optional<String> selectedModel = io.github.dsheirer.gui.preference.ai.GeminiModelSelectionDialog.promptUserForModel(models, mUserPreferences.getAIPreference().getGeminiModel());
                    
                    if (selectedModel.isPresent()) {
                        mUserPreferences.getAIPreference().setGeminiApiKey(key);
                        mUserPreferences.getAIPreference().setGeminiApiKeyTested(true);
                        mUserPreferences.getAIPreference().setGeminiModel(selectedModel.get());
                        mUserPreferences.getAIPreference().setAIEnabled(enableAiChk.isSelected());
                        statusLabel.setText("✓  API Key & Model (" + selectedModel.get() + ") saved!");
                    } else {
                        statusLabel.setText("⚠  API Key validated, but no model selected.");
                        statusLabel.setTextFill(Color.web("#FFCC00"));
                    }
                    testBtn.setDisable(false);
                }
                else
                {
                    apiKeyField.setText("");
                    mUserPreferences.getAIPreference().setGeminiApiKey("");
                    mUserPreferences.getAIPreference().setGeminiApiKeyTested(false);
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

        HBox featuresBox = createInfoBox("🤖",
            "AI features include:\n" +
            "  • Automatic transcript summarization of decoded radio traffic\n" +
            "  • Activity pattern detection across channels\n" +
            "  • Intelligent channel prioritization based on content\n" +
            "  • Automatic discovery and naming of two-tone paging targets");

        Label howToLabel = new Label("How to get a free API Key:");
        howToLabel.setFont(Font.font("SansSerif", FontWeight.BOLD, 13));

        Label stepsLabel = new Label(
            "1. Go to https://aistudio.google.com/apikey\n" +
            "2. Sign in with your Google account (free)\n" +
            "3. Click \"Create API Key\" and select a project\n" +
            "4. Copy the key and paste it in the field above");
        stepsLabel.setWrapText(true);
        stepsLabel.setFont(Font.font("SansSerif", 12));

        HBox costBox = createInfoBox("💰",
            "The Gemini API has a generous free tier. For typical SDRTrunk usage " +
            "(summarizing a few hundred transcripts per day), the free tier is more than " +
            "sufficient. Check Google's Gemini pricing page for heavy usage details.");

        HBox skipBox = createWarningBox(
            "If you skip this step, AI-powered features will be disabled. You can add " +
            "an API key later from User Preferences → AI Features.");

        VBox layout = createStepLayout(
            "Gemini AI Features",
            "SDRTrunk Kennebec features AI integrations powered by Google's Gemini API for " +
            "automatic transcript summarization and activity analysis.",
            enableAiChk,
            featuresBox,
            howToLabel,
            stepsLabel,
            new Label("API Key:"),
            apiKeyField,
            testBtn,
            progressBar,
            statusLabel,
            costBox,
            skipBox
        );

        return new WizardStep("AI Features", layout);
    }



    // ── Radio Reference ──

    private WizardStep buildRadioReferenceStep()
    {
        TextField usernameField = new TextField();
        usernameField.setPromptText("RadioReference.com username");
        usernameField.setMaxWidth(420);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("RadioReference.com password or API key");
        passwordField.setMaxWidth(420);

        CheckBox storeCredentials = new CheckBox("Store credentials for future sessions");
        storeCredentials.setSelected(true);

        Label statusLabel = new Label("");
        statusLabel.setFont(Font.font("SansSerif", FontWeight.SEMI_BOLD, 12));
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);

        Button saveBtn = new Button("▶  Save Credentials");
        saveBtn.setStyle("-fx-background-color: " + ACCENT_BLUE + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 20 8 20; -fx-font-weight: bold; -fx-cursor: hand;");

        saveBtn.setOnAction(e -> {
            String username = usernameField.getText();
            String password = passwordField.getText();

            if (username != null && !username.isBlank() && password != null && !password.isBlank())
            {
                try
                {
                    mUserPreferences.getRadioReferencePreference().setStoreCredentials(storeCredentials.isSelected());
                    mUserPreferences.getRadioReferencePreference().setUserName(username);
                    mUserPreferences.getRadioReferencePreference().setPassword(password);

                    statusLabel.setText("✓  Credentials saved successfully!");
                    statusLabel.setTextFill(Color.web(ACCENT_GREEN));
                    statusLabel.setVisible(true);
                    statusLabel.setManaged(true);
                }
                catch (Exception ex)
                {
                    statusLabel.setText("✗  Failed to save credentials");
                    statusLabel.setTextFill(Color.web("#FF3B30"));
                    statusLabel.setVisible(true);
                    statusLabel.setManaged(true);
                }
            }
            else
            {
                statusLabel.setText("✗  Please enter both username and password/API key");
                statusLabel.setTextFill(Color.web("#FF3B30"));
                statusLabel.setVisible(true);
                statusLabel.setManaged(true);
            }
        });

        HBox whatBox = createInfoBox("📡",
            "RadioReference.com is the largest database of radio system information in North " +
            "America. SDRTrunk can import talkgroup IDs, system configurations, and frequency " +
            "data directly from RadioReference, dramatically simplifying channel setup.");

        HBox whyBox = createInfoBox("⭐",
            "Why set this up? Instead of manually entering hundreds of talkgroup IDs and " +
            "frequencies, you can import entire radio systems in a few clicks. This is the " +
            "fastest way to get started monitoring P25, DMR, and LTR systems.");

        Label howToLabel = new Label("How to get credentials:");
        howToLabel.setFont(Font.font("SansSerif", FontWeight.BOLD, 13));

        Label stepsLabel = new Label(
            "1. Go to https://www.radioreference.com\n" +
            "2. Create an account (free) or sign in\n" +
            "3. Subscribe to a Premium membership ($15/6 months)\n" +
            "   — Premium is required for API access\n" +
            "4. Enter your username and password below");
        stepsLabel.setWrapText(true);
        stepsLabel.setFont(Font.font("SansSerif", 12));

        HBox skipBox = createWarningBox(
            "If you skip this step, you can still use SDRTrunk but will need to manually " +
            "configure all channels, talkgroups, and frequencies. You can add RadioReference " +
            "credentials later from the Radio Reference tab in the Playlist Editor.");

        VBox layout = createStepLayout(
            "RadioReference Integration",
            "Connect your RadioReference.com premium account to import radio system data, " +
            "talkgroup IDs, and frequencies automatically.",
            whatBox,
            whyBox,
            howToLabel,
            stepsLabel,
            new Label("Username:"),
            usernameField,
            new Label("Password / API Key:"),
            passwordField,
            storeCredentials,
            saveBtn,
            statusLabel,
            skipBox
        );

        return new WizardStep("Radio Reference", layout);
    }

    // ── Notifications ──

    private WizardStep buildNotificationsStep()
    {
        CheckBox enableTelegramChk = new CheckBox("Enable Telegram Notifications");

        TextField botTokenField = new TextField();
        botTokenField.setPromptText("Telegram Bot Token (e.g., 123456:ABC-DEF...)");
        botTokenField.setMaxWidth(420);

        Label statusLabel = new Label("");
        statusLabel.setFont(Font.font("SansSerif", FontWeight.SEMI_BOLD, 12));
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);

        Button saveBtn = new Button("▶  Save Notification Settings");
        saveBtn.setStyle("-fx-background-color: " + ACCENT_BLUE + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 20 8 20; -fx-font-weight: bold; -fx-cursor: hand;");

        saveBtn.setOnAction(e -> {
            try
            {
                mUserPreferences.getNotificationPreference().setTelegramEnabled(enableTelegramChk.isSelected());
                String token = botTokenField.getText();
                if (token != null && !token.isBlank())
                {
                    mUserPreferences.getNotificationPreference().setTelegramBotToken(token);
                }

                statusLabel.setText("✓  Notification settings saved!");
                statusLabel.setTextFill(Color.web(ACCENT_GREEN));
                statusLabel.setVisible(true);
                statusLabel.setManaged(true);
            }
            catch (Exception ex)
            {
                statusLabel.setText("✗  Failed to save notification settings");
                statusLabel.setTextFill(Color.web("#FF3B30"));
                statusLabel.setVisible(true);
                statusLabel.setManaged(true);
            }
        });

        HBox whatBox = createInfoBox("🔔",
            "SDRTrunk can send you real-time notifications when specific events occur — " +
            "for example, when a specific talkgroup becomes active, when a priority call " +
            "is detected, or when certain keywords appear in decoded traffic.");

        HBox whyBox = createInfoBox("📱",
            "Telegram notifications let you monitor important radio activity from your " +
            "phone or tablet, even when you're away from the computer. You can set up " +
            "alerts per talkgroup or keyword in the Alias/Notifications editor.");

        Label howToLabel = new Label("How to create a Telegram Bot:");
        howToLabel.setFont(Font.font("SansSerif", FontWeight.BOLD, 13));

        Label stepsLabel = new Label(
            "1. Open Telegram and search for @BotFather\n" +
            "2. Send /newbot and follow the prompts to name your bot\n" +
            "3. BotFather will give you a Bot Token (a long string)\n" +
            "4. Copy the token and paste it in the field below\n" +
            "5. Add recipients (Chat IDs) later in User Preferences → Notifications");
        stepsLabel.setWrapText(true);
        stepsLabel.setFont(Font.font("SansSerif", 12));

        HBox skipBox = createWarningBox(
            "If you skip this step, no external notifications will be sent. You can " +
            "configure Telegram (and email) notifications later from User Preferences → Notifications.");

        VBox layout = createStepLayout(
            "Notifications",
            "Set up Telegram notifications so SDRTrunk can alert you about important " +
            "radio activity on your phone or other devices.",
            whatBox,
            whyBox,
            enableTelegramChk,
            howToLabel,
            stepsLabel,
            new Label("Bot Token:"),
            botTokenField,
            saveBtn,
            statusLabel,
            skipBox
        );

        return new WizardStep("Notifications", layout);
    }

    // ── Audio Recording ──

    private WizardStep buildAudioRecordingStep()
    {
        // Recording format selection
        ToggleGroup formatGroup = new ToggleGroup();
        RadioButton mp3Btn = new RadioButton("MP3 (Recommended) — Smaller files, good quality, widely compatible");
        mp3Btn.setToggleGroup(formatGroup);
        mp3Btn.setSelected(true);
        RadioButton wavBtn = new RadioButton("WAVE (.wav) — Lossless audio, larger files (10x larger than MP3)");
        wavBtn.setToggleGroup(formatGroup);
        VBox formatBox = new VBox(8, mp3Btn, wavBtn);
        formatBox.setPadding(new Insets(4, 0, 4, 0));

        // Recording directory
        Path currentDir = mUserPreferences.getDirectoryPreference().getDirectoryRecording();
        TextField dirField = new TextField(currentDir.toAbsolutePath().toString());
        dirField.setMaxWidth(420);
        dirField.setEditable(false);

        Button browseBtn = new Button("Browse...");
        browseBtn.setStyle("-fx-background-color: #E5E5EA; -fx-text-fill: #3A3A3C; -fx-background-radius: 6; -fx-padding: 6 14 6 14;");

        HBox dirRow = new HBox(8, dirField, browseBtn);
        dirRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(dirField, Priority.ALWAYS);

        browseBtn.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Recording Directory");
            File initial = new File(dirField.getText());
            if (initial.exists()) chooser.setInitialDirectory(initial);
            File selected = chooser.showDialog(mOwner);
            if (selected != null)
            {
                dirField.setText(selected.getAbsolutePath());
                mUserPreferences.getDirectoryPreference().setDirectoryRecording(selected.toPath());
            }
        });

        Label statusLabel = new Label("");
        statusLabel.setFont(Font.font("SansSerif", FontWeight.SEMI_BOLD, 12));
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);

        Button saveBtn = new Button("▶  Save Recording Settings");
        saveBtn.setStyle("-fx-background-color: " + ACCENT_BLUE + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 20 8 20; -fx-font-weight: bold; -fx-cursor: hand;");

        saveBtn.setOnAction(e -> {
            try
            {
                RecordFormat format = mp3Btn.isSelected() ? RecordFormat.MP3 : RecordFormat.WAVE;
                mUserPreferences.getRecordPreference().setAudioRecordFormat(format);

                statusLabel.setText("✓  Recording settings saved! Format: " + format.name());
                statusLabel.setTextFill(Color.web(ACCENT_GREEN));
                statusLabel.setVisible(true);
                statusLabel.setManaged(true);
            }
            catch (Exception ex)
            {
                statusLabel.setText("✗  Failed to save recording settings");
                statusLabel.setTextFill(Color.web("#FF3B30"));
                statusLabel.setVisible(true);
                statusLabel.setManaged(true);
            }
        });

        HBox whatBox = createInfoBox("🔊",
            "SDRTrunk can automatically record all decoded audio calls. Recordings are " +
            "organized by date and channel, and can be played back or exported later. " +
            "You can enable/disable recording per-channel in the Playlist Editor.");

        HBox formatInfo = createInfoBox("💾",
            "MP3 is recommended for most users — files are about 10x smaller than WAV " +
            "with minimal quality loss. Choose WAV only if you need lossless audio for " +
            "forensic analysis or archival purposes.");

        HBox skipBox = createWarningBox(
            "If you skip this step, the default settings (MP3 format, standard directory) " +
            "will be used. You can change these later from User Preferences → Recording.");

        VBox layout = createStepLayout(
            "Audio Recording",
            "Configure how SDRTrunk records decoded audio from monitored channels.",
            whatBox,
            new Label("Recording Format:"),
            formatBox,
            formatInfo,
            new Label("Recording Directory:"),
            dirRow,
            saveBtn,
            statusLabel,
            skipBox
        );

        return new WizardStep("Recording", layout);
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

        HBox whatBox = createInfoBox("🖥️",
            "GPU acceleration offloads intensive DSP (Digital Signal Processing) math " +
            "operations from your CPU to your graphics card via OpenCL. This can reduce " +
            "CPU utilization by 30-60%, allowing you to monitor more channels simultaneously.");

        HBox warningBox = createInfoBox("⚠️",
            "When NOT to enable: If you experience graphical glitches, application crashes, " +
            "or audio artifacts after enabling GPU acceleration, disable this option. Some " +
            "older GPU drivers have buggy OpenCL implementations. Intel integrated graphics " +
            "sometimes have compatibility issues.");

        HBox skipBox = createWarningBox(
            "If you skip or leave this disabled, all DSP processing will use CPU only. " +
            "This works fine for most setups but may limit the number of simultaneous " +
            "channels you can monitor on slower hardware.");

        VBox layout = createStepLayout(
            "Hardware Acceleration (GPU)",
            "SDRTrunk can offload massive DSP math operations to your GPU via OpenCL. " +
            "This dramatically lowers CPU utilization, but requires stable graphics drivers.",
            whatBox,
            progressBar,
            statusLabel,
            enableGpuBox,
            warningBox,
            skipBox
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

        // Recommendation table
        Label guideTitle = new Label("Recommended Settings:");
        guideTitle.setFont(Font.font("SansSerif", FontWeight.BOLD, 13));

        VBox guideTable = new VBox(2);
        guideTable.setPadding(new Insets(8, 0, 8, 0));
        String[] rows = {
            "  1–5 channels, waterfall off    →  4 GB",
            "  1–5 channels, waterfall on     →  6 GB",
            "  6–20 channels, waterfall on    →  8 GB",
            "  20+ channels, waterfall on     →  12–16 GB",
            "  Extreme (50+ channels)         →  24–32 GB"
        };
        for (String row : rows)
        {
            Label rowLabel = new Label(row);
            rowLabel.setFont(Font.font("Consolas", 12));
            guideTable.getChildren().add(rowLabel);
        }

        HBox warningBox = createInfoBox("⚠️",
            "Do not set memory higher than your system's physical RAM. Setting " +
            "it too high can cause the OS to use swap memory, which will make " +
            "the application much slower. Leave at least 2 GB for Windows/macOS.");

        VBox layout = createStepLayout(
            "System Memory Allocation",
            "SDRTrunk is memory intensive when monitoring many channels or using the waterfall display. " +
            "Select the maximum memory (RAM) the application is allowed to use.\n\n" +
            "This is the final step. Applying this setting will prompt a restart of the application.",
            guideTitle,
            guideTable,
            memRow,
            warningBox,
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