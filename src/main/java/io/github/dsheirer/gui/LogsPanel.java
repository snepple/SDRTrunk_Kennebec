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
import javafx.scene.control.Button;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

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

        HBox btnPanel = new HBox(10);
        btnPanel.setPadding(new Insets(10));
        btnPanel.setAlignment(Pos.CENTER_RIGHT);
        btnPanel.getChildren().addAll(refreshBtn);

        Label instructionsLabel = new Label("Double-click a log file to open it in your text editor");
        instructionsLabel.setPadding(new Insets(10));
        instructionsLabel.setAlignment(Pos.CENTER);
        instructionsLabel.setMaxWidth(Double.MAX_VALUE);
        instructionsLabel.getStyleClass().add("kennebec-secondary-text");

        setTop(instructionsLabel);
        setCenter(mTabbedPane);
        setBottom(btnPanel);
    }
    private void analyzeLog(LogFile logFile, Button analyzeBtn) {
        if (logFile == null || !logFile.getFile().exists()) {
            return;
        }

        analyzeBtn.setDisable(true);
        analyzeBtn.setText("Analyzing...");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("AI Log Analysis");
        alert.setHeaderText("Analyzing log with Gemini AI...");

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(50, 50);
        VBox vbox = new VBox(progressIndicator);
        vbox.setAlignment(Pos.CENTER);
        vbox.setPrefSize(600, 400);
        alert.getDialogPane().setContent(vbox);

        // Disable the OK button while analyzing
        Button okButton = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(true);

        // Prevent closing the alert while analysis is ongoing
        alert.setOnCloseRequest(event -> {
            if (okButton.isDisabled()) {
                event.consume();
            }
        });

        alert.show();

        Thread worker = new Thread(() -> {
            String logContent;
            try {
                List<String> lines = Files.readAllLines(logFile.getFile().toPath());
                int maxLines = 500;
                if (lines.size() > maxLines) {
                    lines = lines.subList(lines.size() - maxLines, lines.size());
                }
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

                        WebView webView = new WebView();
                        webView.getEngine().loadContent("<html><body style='font-family: sans-serif;'>" + htmlResult + "</body></html>");
                        webView.setPrefSize(600, 400);

                        if (!alert.isShowing()) {
                            alert.show(); // Show again if closed
                        }
                        alert.setHeaderText(null);
                        alert.getDialogPane().setContent(webView);
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


        TableColumn<LogFile, Void> actionCol = new TableColumn<>("Action");
        actionCol.setPrefWidth(120);
        actionCol.setMaxWidth(150);
        actionCol.setMinWidth(100);
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button analyzeBtn = new Button("AI Review");

            {
                analyzeBtn.getStyleClass().add("kennebec-primary-button");
                analyzeBtn.setOnAction(event -> {
                    LogFile logFile = getTableView().getItems().get(getIndex());
                    analyzeLog(logFile, analyzeBtn);
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

                    setGraphic(analyzeBtn);
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
