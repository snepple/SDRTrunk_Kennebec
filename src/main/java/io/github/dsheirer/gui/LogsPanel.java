package io.github.dsheirer.gui;

import io.github.dsheirer.gui.log.LogFile;
import io.github.dsheirer.module.log.ai.AILogAnalyzer;
import io.github.dsheirer.preference.UserPreferences;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class LogsPanel extends BorderPane {
    private static final Logger mLog = LoggerFactory.getLogger(LogsPanel.class);

    private java.util.Timer mHealthTimer;
    private UserPreferences mUserPreferences;

    private ObservableList<LogFile> mAppListModel = FXCollections.observableArrayList();
    private TableView<LogFile> mAppTable;
    private FilteredList<LogFile> mAppFiltered;

    private ObservableList<LogFile> mEventListModel = FXCollections.observableArrayList();
    private TableView<LogFile> mEventTable;
    private FilteredList<LogFile> mEventFiltered;

    private ObservableList<LogFile> mTwoToneListModel = FXCollections.observableArrayList();
    private TableView<LogFile> mTwoToneTable;
    private FilteredList<LogFile> mTwoToneFiltered;

    private TextField mAppSearchField;
    private TextField mEventSearchField;
    private TextField mTwoToneSearchField;

    private TabPane mTabbedPane;
    private Button mAnalyzeBtn;

    public LogsPanel(UserPreferences userPreferences) {
        mUserPreferences = userPreferences;

        // Apply HIG-like styling
        getStyleClass().add("kennebec-grouped-bg");

        mAppFiltered = new FilteredList<>(mAppListModel, p -> true);
        mAppTable = createLogTable(mAppFiltered);
        mAppSearchField = createSearchField(mAppFiltered);

        mEventFiltered = new FilteredList<>(mEventListModel, p -> true);
        mEventTable = createLogTable(mEventFiltered);
        mEventSearchField = createSearchField(mEventFiltered);

        mTwoToneFiltered = new FilteredList<>(mTwoToneListModel, p -> true);
        mTwoToneTable = createLogTable(mTwoToneFiltered);
        mTwoToneSearchField = createSearchField(mTwoToneFiltered);

        loadLogs();

        mTabbedPane = new TabPane();
        mTabbedPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

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
            mTabbedPane.getTabs().add(healthTab);

            mHealthTimer = new java.util.Timer(true);
            mHealthTimer.scheduleAtFixedRate(new SystemHealthAdvisorTask(performanceLabel), 0, 5000);
        }

        mTabbedPane.getTabs().add(new Tab("Application Logs", createTabPanel(mAppTable, mAppSearchField)));
        mTabbedPane.getTabs().add(new Tab("Channel Event Logs", createTabPanel(mEventTable, mEventSearchField)));
        mTabbedPane.getTabs().add(new Tab("Two-Tone Logs", createTabPanel(mTwoToneTable, mTwoToneSearchField)));

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setOnAction(e -> loadLogs());

        mAnalyzeBtn = new Button("Analyze Error");
        mAnalyzeBtn.setOnAction(e -> analyzeSelectedLog());
        // HIG primary action styling
        mAnalyzeBtn.getStyleClass().add("kennebec-primary-button");

        updateAnalyzeButtonState();
        mAppTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> updateAnalyzeButtonState());
        mEventTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> updateAnalyzeButtonState());
        mTwoToneTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> updateAnalyzeButtonState());
        mTabbedPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> updateAnalyzeButtonState());

        HBox btnPanel = new HBox(10);
        btnPanel.setPadding(new Insets(10));
        btnPanel.setAlignment(Pos.CENTER_RIGHT);
        btnPanel.getChildren().addAll(refreshBtn, mAnalyzeBtn);

        Label instructionsLabel = new Label("Double-click a log file to open it in your text editor");
        instructionsLabel.setPadding(new Insets(10));
        instructionsLabel.setAlignment(Pos.CENTER);
        instructionsLabel.setMaxWidth(Double.MAX_VALUE);
        instructionsLabel.getStyleClass().add("kennebec-secondary-text");

        setTop(instructionsLabel);
        setCenter(mTabbedPane);
        setBottom(btnPanel);
    }

    private void updateAnalyzeButtonState() {
        boolean aiEnabled = mUserPreferences.getAIPreference().isAIEnabled() && mUserPreferences.getAIPreference().isAILogAnalysisEnabled();
        boolean hasApiKey = !mUserPreferences.getAIPreference().getGeminiApiKey().trim().isEmpty();
        boolean hasSelection = getSelectedLogFile() != null;

        mAnalyzeBtn.setDisable(!(aiEnabled && hasApiKey && hasSelection));

        if (!aiEnabled) {
            mAnalyzeBtn.setTooltip(new Tooltip("Enable AI and Log Analysis in User Preferences."));
        } else if (!hasApiKey) {
            mAnalyzeBtn.setTooltip(new Tooltip("Gemini API Key is missing in User Preferences."));
        } else if (!hasSelection) {
            mAnalyzeBtn.setTooltip(new Tooltip("Select a log file to analyze."));
        } else {
            mAnalyzeBtn.setTooltip(new Tooltip("Analyze selected log file using AI."));
        }
    }

    private LogFile getSelectedLogFile() {
        Tab selectedTab = mTabbedPane.getSelectionModel().getSelectedItem();
        if (selectedTab != null) {
            String tabText = selectedTab.getText();
            if ("Application Logs".equals(tabText)) {
                return mAppTable.getSelectionModel().getSelectedItem();
            } else if ("Channel Event Logs".equals(tabText)) {
                return mEventTable.getSelectionModel().getSelectedItem();
            } else if ("Two-Tone Logs".equals(tabText)) {
                return mTwoToneTable.getSelectionModel().getSelectedItem();
            }
        }
        return null;
    }

    private void analyzeSelectedLog() {
        LogFile logFile = getSelectedLogFile();
        if (logFile == null || !logFile.getFile().exists()) {
            return;
        }

        mAnalyzeBtn.setDisable(true);
        mAnalyzeBtn.setText("Analyzing...");

        Thread worker = new Thread(() -> {
            String content;
            try {
                List<String> lines = Files.readAllLines(logFile.getFile().toPath());
                int maxLines = 500;
                if (lines.size() > maxLines) {
                    lines = lines.subList(lines.size() - maxLines, lines.size());
                }
                content = String.join("\n", lines);
            } catch (IOException e) {
                showError("Error reading log file: " + e.getMessage());
                resetAnalyzeButton();
                return;
            }

            try {
                AILogAnalyzer analyzer = new AILogAnalyzer(mUserPreferences);
                String result = analyzer.analyze(content);

                Platform.runLater(() -> {
                    TextArea textArea = new TextArea(result);
                    textArea.setEditable(false);
                    textArea.setWrapText(true);
                    textArea.setPrefSize(600, 400);

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("AI Log Analysis");
                    alert.setHeaderText(null);
                    alert.getDialogPane().setContent(textArea);
                    alert.showAndWait();

                    resetAnalyzeButton();
                });
            } catch (Exception ex) {
                showError("Error analyzing log:\n" + ex.getMessage());
                resetAnalyzeButton();
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void resetAnalyzeButton() {
        Platform.runLater(() -> {
            mAnalyzeBtn.setText("Analyze Error");
            updateAnalyzeButtonState();
        });
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private TableView<LogFile> createLogTable(FilteredList<LogFile> filteredData) {
        TableView<LogFile> table = new TableView<>();
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

        table.getColumns().addAll(dateCol, nameCol);

        table.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                LogFile selected = table.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openLog(selected.getFile());
                }
            }
        });

        // Set row height for HIG Clarity principle
        table.setFixedCellSize(30);

        return table;
    }

    private VBox createTabPanel(TableView<LogFile> table, TextField searchField) {
        VBox panel = new VBox();
        panel.setPadding(new Insets(10));
        panel.setSpacing(10);

        HBox searchPanel = new HBox(10);
        searchPanel.setAlignment(Pos.CENTER_LEFT);
        Label searchLabel = new Label("Filter by Date or Name:");
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchPanel.getChildren().addAll(searchLabel, searchField);

        VBox.setVgrow(table, Priority.ALWAYS);
        panel.getChildren().addAll(searchPanel, table);
        return panel;
    }

    private TextField createSearchField(FilteredList<LogFile> filteredData) {
        TextField searchField = new TextField();
        searchField.setPromptText("Search...");
        searchField.getStyleClass().add("kennebec-search-field");

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(logFile -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                try {
                    Pattern pattern = Pattern.compile(newValue, Pattern.CASE_INSENSITIVE);
                    if (pattern.matcher(logFile.getName()).find()) {
                        return true;
                    }
                    if (logFile.getDate() != null && pattern.matcher(logFile.getDate().toString()).find()) {
                        return true;
                    }
                } catch (PatternSyntaxException e) {
                    // Ignore invalid regex while typing
                }

                String lowerCaseFilter = newValue.toLowerCase();
                if (logFile.getName().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                if (logFile.getDate() != null && logFile.getDate().toString().contains(lowerCaseFilter)) {
                    return true;
                }

                return false;
            });
        });
        return searchField;
    }

    private void openLog(File logFile) {
        if (logFile != null && logFile.exists()) {
            try {
                Desktop.getDesktop().open(logFile);
            } catch (Exception ex) {
                mLog.error("Error opening log file: {}", logFile.getAbsolutePath(), ex);
            }
        }
    }

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
}
