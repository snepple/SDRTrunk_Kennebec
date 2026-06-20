

package io.github.dsheirer.gui;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.log.LogFile;
import io.github.dsheirer.monitor.DiagnosticMonitor;
import io.github.dsheirer.log.ListViewLogAppender;
import io.github.dsheirer.module.log.ai.AILogAnalyzer;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.util.ThreadPool;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.controlsfx.control.SegmentedButton;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class LogsViewController {
    private static final org.slf4j.Logger mLog = LoggerFactory.getLogger(LogsViewController.class);

    private java.util.Timer mHealthTimer;
    private UserPreferences mUserPreferences;
    //Volatile: may be set after construction (see setDiagnosticMonitor) and is read from a background
    //thread when generating the processing diagnostic report.
    private volatile DiagnosticMonitor mDiagnosticMonitor;

    @FXML private TabPane mTabbedPane;

    @FXML private ListView<String> logListView;
    private ObservableList<String> logData = FXCollections.observableArrayList();
    private ListViewLogAppender appender;
    private ConcurrentLinkedQueue<String> logQueue = new ConcurrentLinkedQueue<>();
    private AnimationTimer batchTimer;

    @FXML private TableView<LogFile> mAppTable;
    @FXML private TextField mAppSearchField;
    private ObservableList<LogFile> mAppListModel = FXCollections.observableArrayList();
    private FilteredList<LogFile> mAppFiltered;

    @FXML private TableView<LogFile> mEventTable;
    @FXML private TextField mEventSearchField;
    private ObservableList<LogFile> mEventListModel = FXCollections.observableArrayList();
    private FilteredList<LogFile> mEventFiltered;

    @FXML private TableView<LogFile> mTwoToneTable;
    @FXML private TextField mTwoToneSearchField;
    private ObservableList<LogFile> mTwoToneListModel = FXCollections.observableArrayList();
    private FilteredList<LogFile> mTwoToneFiltered;
    
    @FXML private ListView<String> mAppLogListView;
    @FXML private TextArea mLiveInspectorView;
    @FXML private TextField mLiveSearchField;
    @FXML private HBox mLogLevelFilterContainer;
    private String mSelectedLevel = "All";
    private FilteredList<String> mLiveFiltered;

    /**
     * Sets (or updates) the diagnostic monitor.  Allows the monitor to be supplied after this controller
     * has been created so the processing diagnostic report uses the full report instead of the fallback.
     */
    public void setDiagnosticMonitor(DiagnosticMonitor diagnosticMonitor) {
        mDiagnosticMonitor = diagnosticMonitor;
    }

    public void init(UserPreferences userPreferences, DiagnosticMonitor diagnosticMonitor) {
        mUserPreferences = userPreferences;
        mDiagnosticMonitor = diagnosticMonitor;

        // Live logs with filtering
        mLiveFiltered = new FilteredList<>(logData, p -> true);
        logListView.setItems(mLiveFiltered);
        logListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Log level filter - SegmentedButton (matches Two Tone Detector filter style)
        if (mLogLevelFilterContainer != null) {
            ToggleButton allBtn = new ToggleButton("All");
            ToggleButton errorBtn = new ToggleButton("ERROR");
            ToggleButton warnBtn = new ToggleButton("WARN");
            ToggleButton infoBtn = new ToggleButton("INFO");
            ToggleButton debugBtn = new ToggleButton("DEBUG");

            SegmentedButton levelSegmented = new SegmentedButton(allBtn, errorBtn, warnBtn, infoBtn, debugBtn);
            levelSegmented.getStyleClass().add(SegmentedButton.STYLE_CLASS_DARK);
            allBtn.setSelected(true);

            levelSegmented.getToggleGroup().selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == null) {
                    oldVal.setSelected(true);
                } else {
                    mSelectedLevel = ((ToggleButton) newVal).getText();
                    updateLiveFilter();
                }
            });

            mLogLevelFilterContainer.getChildren().add(levelSegmented);
        }

        // Live search field
        if (mLiveSearchField != null) {
            mLiveSearchField.textProperty().addListener((obs, oldVal, newVal) -> updateLiveFilter());
        }

        // Color-coded log level highlighting using CSS classes
        logListView.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("log-error", "log-warning", "log-debug");
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setText(item);
                javafx.scene.shape.Circle circle = new javafx.scene.shape.Circle(5);
                if (item.contains(" ERROR ") || item.contains(" FATAL ")) {
                    getStyleClass().add("log-error");
                    circle.setFill(javafx.scene.paint.Color.RED);
                } else if (item.contains(" WARN ")) {
                    getStyleClass().add("log-warning");
                    circle.setFill(javafx.scene.paint.Color.ORANGE);
                } else if (item.contains(" DEBUG ")) {
                    getStyleClass().add("log-debug");
                    circle.setFill(javafx.scene.paint.Color.GRAY);
                } else {
                    circle.setFill(javafx.scene.paint.Color.GREEN);
                }
                setGraphic(circle);
            }
        });
        
        logListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (mLiveInspectorView != null) {
                mLiveInspectorView.setText(newVal != null ? buildInspectorText(newVal) : "");
            }
        });

        MyEventBus.getGlobalEventBus().register(this);

        MenuItem copyItem = new MenuItem("Copy");
        copyItem.setOnAction(event -> copySelectedLogs());
        ContextMenu contextMenu = new ContextMenu(copyItem);
        logListView.setContextMenu(contextMenu);

        KeyCombination copyShortcut = new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN);
        logListView.setOnKeyPressed(event -> {
            if (copyShortcut.match(event)) {
                copySelectedLogs();
            }
        });

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        appender = new ListViewLogAppender("ListViewLogAppender");
        appender.setContext(loggerContext);
        appender.start();
        rootLogger.addAppender(appender);

        batchTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!logQueue.isEmpty()) {
                    List<String> batch = new ArrayList<>();
                    String msg;
                    int count = 0;
                    // Limit to 250 log lines per frame to prevent JavaFX EDT from freezing during log floods
                    while (count < 250 && (msg = logQueue.poll()) != null) {
                        batch.add(msg);
                        count++;
                    }
                    if (!batch.isEmpty()) {
                        logData.addAll(batch);
                        if (logData.size() > 10000) {
                            logData.remove(0, logData.size() - 10000);
                        }
                        // Auto-scroll to bottom
                        logListView.scrollTo(logData.size() - 1);
                    }
                }
            }
        };
        batchTimer.start();

        // Initialize File Tables
        mAppFiltered = new FilteredList<>(mAppListModel, p -> true);
        setupTable(mAppTable, mAppFiltered);
        setupSearchField(mAppSearchField, mAppFiltered);
        
        mAppTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (mAppLogListView != null) {
                if (newVal != null && newVal.getFile() != null && newVal.getFile().exists()) {
                    try {
                        List<String> lines = Files.readAllLines(newVal.getFile().toPath());
                        int maxLines = 1000;
                        if (lines.size() > maxLines) lines = lines.subList(lines.size() - maxLines, lines.size());
                        mAppLogListView.setItems(FXCollections.observableArrayList(lines));
                    } catch (IOException e) {
                        mAppLogListView.setItems(FXCollections.observableArrayList("Error reading file: " + e.getMessage()));
                    }
                } else {
                    mAppLogListView.setItems(FXCollections.emptyObservableList());
                }
            }
        });

        mEventFiltered = new FilteredList<>(mEventListModel, p -> true);
        setupTable(mEventTable, mEventFiltered);
        setupSearchField(mEventSearchField, mEventFiltered);

        mTwoToneFiltered = new FilteredList<>(mTwoToneListModel, p -> true);
        setupTable(mTwoToneTable, mTwoToneFiltered);
        setupSearchField(mTwoToneSearchField, mTwoToneFiltered);

        // System Health Tab
        if (mUserPreferences.getAIPreference().isAIEnabled() && mUserPreferences.getAIPreference().isSystemHealthAdvisorEnabled()) {
            VBox performancePanel = new VBox();
            performancePanel.setAlignment(Pos.CENTER);
            performancePanel.setPadding(new Insets(30));
            performancePanel.setSpacing(10);

            Label titleLabel = new Label("System Health & Performance Advisor");
            titleLabel.setFont(Font.font("System", FontWeight.BOLD, 18));

            Label performanceLabel = new Label("Status: Monitoring...\nCPU Usage: OK\nMemory Usage: OK\n\nOptimization Suggestions:\nNo suggestions at this time.");
            performanceLabel.setWrapText(true);
            performanceLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

            performancePanel.getChildren().addAll(titleLabel, performanceLabel);

            Tab healthTab = new Tab("System Health", performancePanel);
            mTabbedPane.getTabs().add(0, healthTab);

            mHealthTimer = new java.util.Timer(true);
            mHealthTimer.scheduleAtFixedRate(new SystemHealthAdvisorTask(performanceLabel), 0, 5000);
        }

        // Diagnostics Tab
        VBox diagBox = new VBox(10);
        diagBox.setPadding(new Insets(20));

        Label diagTitle = new Label("Diagnostic Reports");
        diagTitle.setFont(Font.font("System", FontWeight.BOLD, 14));

        Label diagDesc = new Label("Generate diagnostic reports to help troubleshoot issues. Reports are displayed below and also saved to the application log directory.");
        diagDesc.setWrapText(true);

        TextArea diagOutput = new TextArea();
        diagOutput.setEditable(false);
        diagOutput.setWrapText(false);
        diagOutput.setFont(Font.font("Monospaced", 11));
        VBox.setVgrow(diagOutput, Priority.ALWAYS);

        Button processingReportBtn = new Button("Processing Diagnostic Report");
        processingReportBtn.setOnAction(e -> generateProcessingDiagnosticReport(diagOutput, processingReportBtn));

        Button threadDumpBtn = new Button("Thread Dump Report");
        threadDumpBtn.setOnAction(e -> generateThreadDumpReport(diagOutput, threadDumpBtn));

        HBox diagButtons = new HBox(10, processingReportBtn, threadDumpBtn);

        diagBox.getChildren().addAll(diagTitle, diagDesc, diagButtons, diagOutput);

        Tab diagTab = new Tab("Diagnostics", diagBox);
        mTabbedPane.getTabs().add(diagTab);

        loadLogs();
    }

    private static final DateTimeFormatter LIVE_LOG_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Subscribe
    public void onLogEvent(ILoggingEvent event) {
        String timestamp = LIVE_LOG_TIME_FORMAT.format(
            java.time.Instant.ofEpochMilli(event.getTimeStamp()).atZone(java.time.ZoneId.systemDefault()));
        logQueue.add(timestamp + "  " + event.getLevel() + "  " + event.getFormattedMessage());
    }

    private static final DateTimeFormatter FRIENDLY_TIME_FORMAT =
        DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' h:mm:ss a");

    /**
     * Builds the Detail Inspector content for a selected log line: a friendly date/time, the level and
     * message, and (when recognized) a plain-language explanation of what it means and how to fix it.
     */
    private String buildInspectorText(String line) {
        StringBuilder sb = new StringBuilder();
        String message = line;

        try {
            if (line.length() > 19) {
                java.time.LocalDateTime ts = java.time.LocalDateTime.parse(line.substring(0, 19), LIVE_LOG_TIME_FORMAT);
                String rest = line.substring(19).trim();   // "LEVEL  message"
                int sp = rest.indexOf("  ");
                String level = sp > 0 ? rest.substring(0, sp).trim() : "";
                message = sp > 0 ? rest.substring(sp).trim() : rest;

                sb.append(ts.format(FRIENDLY_TIME_FORMAT));
                if (!level.isEmpty()) {
                    sb.append("    [").append(level).append("]");
                }
                sb.append("\n\n");
            }
        } catch (Exception e) {
            //Not a timestamped line; fall back to showing it as-is.
            message = line;
        }

        sb.append(message);

        String explanation = explainLogLine(message);
        if (explanation != null) {
            sb.append("\n\n----------------------------------------\n");
            sb.append("What this means and how to fix it:\n\n");
            sb.append(explanation);
        }

        return sb.toString();
    }

    /**
     * Returns a plain-language explanation and remediation for recognized log messages, or null when
     * the message isn't one we have specific guidance for.
     */
    private String explainLogLine(String message) {
        if (message == null) {
            return null;
        }
        String m = message.toLowerCase();

        if (m.contains("jmbe") && (m.contains("not configured") || m.contains("missing") || m.contains("silent"))) {
            return "Digital voice (P25 / DMR) audio needs the optional JMBE library, which isn't set up. "
                + "Open User Preferences > JMBE Audio Library (or re-run the first-time wizard) to install it. "
                + "Until then, digital voice channels will decode but stay silent.";
        }
        if (m.contains("no sdr tuner detected") || m.contains("no tuner available")) {
            return "No SDR tuner was found. Check that the device is plugged in, that the correct driver is "
                + "installed (for RTL-SDR on Windows, replace the driver with WinUSB using Zadig), and that no "
                + "other program is using the device.";
        }
        if (m.contains("processor overloaded") || m.contains("cannot keep up")) {
            return "The CPU can't process the incoming sample rate fast enough, so samples are being dropped. "
                + "Lower the tuner sample rate, reduce the number of simultaneously decoding channels, or close "
                + "other CPU-heavy programs.";
        }
        if (m.contains("gc pressure") || m.contains("memory at")) {
            return "The application is running low on memory and spending time garbage-collecting. Increase the "
                + "allocated memory in User Preferences > Memory, then restart SDRTrunk.";
        }
        if (m.contains("websockethandshakeexception") || (m.contains("zello") && m.contains("failed"))) {
            return "A streaming connection failed to connect. Verify the stream's network name, username and "
                + "password, and confirm the machine has internet access. SDRTrunk will keep retrying automatically.";
        }
        if (m.contains("address already in use")) {
            return "Another instance of SDRTrunk is already running and holding the local network port. Close the "
                + "other instance; only one copy should run at a time.";
        }
        if (m.contains("unable to source channel")) {
            return "No tuner could provide this channel's frequency. Ensure a tuner that covers that frequency is "
                + "enabled and isn't already fully subscribed by other channels.";
        }
        if (m.contains("auto-repairing stream")) {
            return "A stream dropped into an error state and is being reconnected automatically. No action is "
                + "needed unless it keeps recurring, which usually points to bad credentials or a network issue.";
        }
        if (m.contains("configuration backup created")) {
            return "A routine automatic backup of your configuration was saved. No action needed.";
        }
        if (m.contains("auto-optimized sample rate")) {
            return "SDRTrunk automatically chose a tuner sample rate that covers the active channels while using "
                + "less CPU. This is informational.";
        }
        return null;
    }

    private void updateLiveFilter() {
        String levelSelection = mSelectedLevel != null ? mSelectedLevel : "All";
        String searchText = (mLiveSearchField != null && mLiveSearchField.getText() != null)
            ? mLiveSearchField.getText().toLowerCase() : "";

        mLiveFiltered.setPredicate(line -> {
            if (line == null) return false;
            // Level filter
            if (!"All".equals(levelSelection)) {
                if (!line.contains(" " + levelSelection + " ") && !line.startsWith(levelSelection + " ")) {
                    return false;
                }
            }
            // Text search filter
            if (!searchText.isEmpty()) {
                return line.toLowerCase().contains(searchText);
            }
            return true;
        });
    }

    private void setupTable(TableView<LogFile> table, FilteredList<LogFile> filteredData) {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        SortedList<LogFile> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedData);

        TableColumn<LogFile, LocalDate> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getDate()));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        dateCol.setCellFactory(column -> new TableCell<LogFile, LocalDate>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(formatter.format(item));
                }
            }
        });
        dateCol.setPrefWidth(120);
        dateCol.setMaxWidth(150);
        dateCol.setMinWidth(100);

        TableColumn<LogFile, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));

        TableColumn<LogFile, Void> actionCol = new TableColumn<>("Action");
        actionCol.setPrefWidth(200);
        actionCol.setMaxWidth(250);
        actionCol.setMinWidth(150);
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button analyzeBtn = new Button("AI Review");
            private final Button openBtn = new Button("O_pen");
            private final HBox container = new HBox(5, openBtn, analyzeBtn);

            {
                openBtn.setMnemonicParsing(true);
                openBtn.accessibleTextProperty().set("Open Log");
                openBtn.accessibleHelpProperty().set("Opens the log file in the system default text editor.");
                analyzeBtn.getStyleClass().add("kennebec-primary-button");
                analyzeBtn.setOnAction(event -> {
                    LogFile logFile = getTableView().getItems().get(getIndex());
                    analyzeLog(logFile, analyzeBtn);
                });

                openBtn.setOnAction(event -> {
                    LogFile logFile = getTableView().getItems().get(getIndex());
                    if (logFile != null && logFile.getFile() != null) {
                        openLog(logFile.getFile());
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    boolean aiEnabled = mUserPreferences.getAIPreference().isAIEnabled() && mUserPreferences.getAIPreference().isAILogAnalysisEnabled();
                    boolean hasApiKey = !mUserPreferences.getAIPreference().getGeminiApiKey().trim().isEmpty();

                    analyzeBtn.setDisable(!(aiEnabled && hasApiKey));
                    if (!aiEnabled) {
                        analyzeBtn.setTooltip(new Tooltip("Enable AI and Log Analysis in User Preferences."));
                    } else if (!hasApiKey) {
                        analyzeBtn.setTooltip(new Tooltip("Gemini API Key is missing in User Preferences."));
                    } else {
                        analyzeBtn.setTooltip(new Tooltip("Analyze log file using AI."));
                    }
                    openBtn.setTooltip(new Tooltip("Open log file in default text editor"));

                    setGraphic(container);
                }
            }
        });

        table.getColumns().addAll(dateCol, nameCol, actionCol);

        table.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                LogFile selected = table.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openLog(selected.getFile());
                }
            }
        });

        table.setFixedCellSize(30);
        table.setPlaceholder(new Label("No logs found"));
    }

    private void setupSearchField(TextField searchField, FilteredList<LogFile> filteredData) {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) {
                filteredData.setPredicate(logFile -> true);
                return;
            }

            Pattern searchPattern;
            try {
                searchPattern = Pattern.compile(newValue, Pattern.CASE_INSENSITIVE);
            } catch (PatternSyntaxException e) {
                searchPattern = null;
            }

            final Pattern pattern = searchPattern;
            final String lowerCaseFilter = newValue.toLowerCase();

            filteredData.setPredicate(logFile -> {
                if (pattern != null) {
                    if (pattern.matcher(logFile.getName()).find()) return true;
                    if (logFile.getDate() != null && pattern.matcher(logFile.getDate().toString()).find()) return true;
                }
                if (logFile.getName().toLowerCase().contains(lowerCaseFilter)) return true;
                if (logFile.getDate() != null && logFile.getDate().toString().toLowerCase().contains(lowerCaseFilter)) return true;
                return false;
            });
        });
    }

    private void openLog(File logFile) {
        if (logFile != null && logFile.exists()) {
            try {
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) {
                    new ProcessBuilder("notepad.exe", logFile.getAbsolutePath()).start();
                } else if (os.contains("mac")) {
                    new ProcessBuilder("open", logFile.getAbsolutePath()).start();
                } else {
                    new ProcessBuilder("xdg-open", logFile.getAbsolutePath()).start();
                }
            } catch (Exception ex) {
                mLog.error("Error opening log file: {}", logFile.getAbsolutePath(), ex);
            }
        }
    }

    @FXML
    private void loadLogs() {
        List<LogFile> appLogs = new ArrayList<>();
        List<LogFile> twoToneLogs = new ArrayList<>();
        List<LogFile> eventLogs = new ArrayList<>();

        File appDir = mUserPreferences.getDirectoryPreference().getDirectoryApplicationLog().toFile();
        if (appDir.exists() && appDir.isDirectory()) {
            File[] files = appDir.listFiles((dir, name) -> name.endsWith(".log"));
            if (files != null) {
                for (File file : files) {
                    LogFile logFile = new LogFile(file, appDir.toPath());
                    if (file.getName().contains("sdrtrunk_twotone")) {
                        twoToneLogs.add(logFile);
                    } else {
                        appLogs.add(logFile);
                    }
                }
            }
        }

        File eventDir = mUserPreferences.getDirectoryPreference().getDirectoryEventLog().toFile();
        if (eventDir.exists() && eventDir.isDirectory()) {
            File[] files = eventDir.listFiles((dir, name) -> name.endsWith(".log"));
            if (files != null) {
                for (File file : files) {
                    eventLogs.add(new LogFile(file, eventDir.toPath()));
                }
            }
        }

        Platform.runLater(() -> {
            mAppListModel.setAll(appLogs);
            mTwoToneListModel.setAll(twoToneLogs);
            mEventListModel.setAll(eventLogs);
        });
    }

    private void analyzeLog(LogFile logFile, Button analyzeBtn) {
        if (logFile == null || !logFile.getFile().exists()) return;
        analyzeBtn.setDisable(true);
        analyzeBtn.setText("Analyzing...");
        Alert alert = new Alert(Alert.AlertType.INFORMATION); io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane());
        alert.setTitle("AI Log Analysis");
        alert.setHeaderText("Analyzing log with Gemini AI...");
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(50, 50);
        VBox vbox = new VBox(progressIndicator);
        vbox.setAlignment(Pos.CENTER);
        vbox.setPrefSize(600, 400);
        alert.getDialogPane().setContent(vbox);
        Button okButton = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(true);
        alert.setOnCloseRequest(event -> { if (okButton.isDisabled()) event.consume(); });
        alert.show();

        Thread worker = new Thread(() -> {
            String logContent;
            try {
                List<String> lines = Files.readAllLines(logFile.getFile().toPath());
                int maxLines = 500;
                if (lines.size() > maxLines) lines = lines.subList(lines.size() - maxLines, lines.size());
                logContent = String.join("\n", lines);
            } catch (IOException e) {
                Platform.runLater(() -> {
                    alert.close();
                    showError("Error reading log file: " + e.getMessage());
                    resetAnalyzeButton(analyzeBtn);
                });
                return;
            }

            try {
                AILogAnalyzer analyzer = new AILogAnalyzer(mUserPreferences);
                String result = analyzer.analyze(logContent);
                Platform.runLater(() -> {
                    try {
                        Parser parser = Parser.builder().build();
                        Node document = parser.parse(result);
                        HtmlRenderer renderer = HtmlRenderer.builder().build();
                        String htmlResult = renderer.render(document);
                        // Use TextArea instead of WebView to avoid heavyweight dependency
                        TextArea resultArea = new TextArea(result);
                        resultArea.setEditable(false);
                        resultArea.setWrapText(true);
                        resultArea.setPrefSize(600, 400);
                        resultArea.getStyleClass().add("log-inspector-text");
                        if (!alert.isShowing()) alert.show();
                        alert.setHeaderText(null);
                        alert.getDialogPane().setContent(resultArea);
                        alert.getDialogPane().lookupButton(ButtonType.OK).setDisable(false);
                    } catch (Exception ex) {
                        alert.close();
                        showError("Error formatting AI response:\n" + ex.getMessage());
                    } finally {
                        resetAnalyzeButton(analyzeBtn);
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    alert.close();
                    showError("Error analyzing log:\n" + ex.getMessage());
                    resetAnalyzeButton(analyzeBtn);
                });
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void resetAnalyzeButton(Button analyzeBtn) {
        Platform.runLater(() -> {
            analyzeBtn.setText("AI Review");
            boolean aiEnabled = mUserPreferences.getAIPreference().isAIEnabled() && mUserPreferences.getAIPreference().isAILogAnalysisEnabled();
            boolean hasApiKey = !mUserPreferences.getAIPreference().getGeminiApiKey().trim().isEmpty();
            analyzeBtn.setDisable(!(aiEnabled && hasApiKey));
        });
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR); io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane());
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void copySelectedLogs() {
        ObservableList<String> selectedItems = logListView.getSelectionModel().getSelectedItems();
        if (selectedItems != null && !selectedItems.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String item : selectedItems) {
                sb.append(item).append("\n");
            }
            ClipboardContent content = new ClipboardContent();
            content.putString(sb.toString());
            Clipboard.getSystemClipboard().setContent(content);
        }
    }

    private void generateProcessingDiagnosticReport(TextArea output, Button btn) {
        btn.setDisable(true);
        btn.setText("Generating...");
        output.setText("Generating Processing Diagnostic Report...");
        ThreadPool.CACHED.submit(() -> {
            try {
                java.nio.file.Path path = null;
                String report;
                if (mDiagnosticMonitor != null) {
                    path = mDiagnosticMonitor.generateProcessingDiagnosticReport("Generated from Logs > Diagnostics panel");
                    report = new String(java.nio.file.Files.readAllBytes(path));
                } else {
                    report = buildFallbackDiagnosticReport();
                }
                final String finalReport = report;
                final java.nio.file.Path finalPath = path;
                Platform.runLater(() -> {
                    output.setText(finalReport);
                    if (finalPath != null) {
                        mLog.info("Processing diagnostic report saved to: {}", finalPath);
                    }
                    btn.setText("Processing Diagnostic Report");
                    btn.setDisable(false);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    output.setText("Error generating report: " + ex.getMessage());
                    btn.setText("Processing Diagnostic Report");
                    btn.setDisable(false);
                });
            }
        });
    }

    private void generateThreadDumpReport(TextArea output, Button btn) {
        btn.setDisable(true);
        btn.setText("Generating...");
        output.setText("Generating Thread Dump Report...");
        ThreadPool.CACHED.submit(() -> {
            try {
                java.nio.file.Path path = null;
                String report;
                if (mDiagnosticMonitor != null) {
                    path = mDiagnosticMonitor.generateThreadDumpReport();
                    report = new String(java.nio.file.Files.readAllBytes(path));
                } else {
                    report = buildThreadDump();
                }
                final String finalReport = report;
                final java.nio.file.Path finalPath = path;
                Platform.runLater(() -> {
                    output.setText(finalReport);
                    if (finalPath != null) {
                        mLog.info("Thread dump report saved to: {}", finalPath);
                    }
                    btn.setText("Thread Dump Report");
                    btn.setDisable(false);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    output.setText("Error generating report: " + ex.getMessage());
                    btn.setText("Thread Dump Report");
                    btn.setDisable(false);
                });
            }
        });
    }

    private String buildFallbackDiagnosticReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("sdrtrunk Processing Diagnostic Report\n");
        sb.append("Generated: ").append(java.time.LocalDateTime.now()).append("\n\n");
        sb.append("=========================================================================\n\n");
        sb.append("JVM Environment:\n");
        sb.append("  OS Name:        ").append(System.getProperty("os.name")).append("\n");
        sb.append("  OS Arch:        ").append(System.getProperty("os.arch")).append("\n");
        sb.append("  OS Version:     ").append(System.getProperty("os.version")).append("\n");
        sb.append("  CPU Cores:      ").append(Runtime.getRuntime().availableProcessors()).append("\n");
        sb.append("  Max Memory:     ").append(Runtime.getRuntime().maxMemory() / 1024 / 1024).append(" MB\n");
        sb.append("  Total Memory:   ").append(Runtime.getRuntime().totalMemory() / 1024 / 1024).append(" MB\n");
        sb.append("  Free Memory:    ").append(Runtime.getRuntime().freeMemory() / 1024 / 1024).append(" MB\n\n");
        sb.append("=========================================================================\n\n");
        sb.append(buildThreadDump());
        return sb.toString();
    }

    private String buildThreadDump() {
        StringBuilder sb = new StringBuilder();
        sb.append("Thread Dump Report\n\n");
        for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
            sb.append(entry.getKey()).append(" ").append(entry.getKey().getState()).append("\n");
            for (StackTraceElement ste : entry.getValue()) {
                sb.append("\tat ").append(ste).append("\n");
            }
            sb.append("\n\n");
        }
        return sb.toString();
    }

    public void destroy() {
        if (mHealthTimer != null) {
            mHealthTimer.cancel();
        }
        if (batchTimer != null) {
            batchTimer.stop();
        }
        MyEventBus.getGlobalEventBus().unregister(this);
        if (appender != null) {
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
            rootLogger.detachAppender(appender);
            appender.stop();
        }
    }
}
