


package io.github.dsheirer.gui.recordings;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;
import javafx.scene.paint.*;
import javafx.geometry.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import io.github.dsheirer.gui.JavaFxWindowManager;
import io.github.dsheirer.preference.UserPreferences;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.layout.Pane;
import javafx.scene.control.SelectionMode;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Spinner;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AudioRecordingsPanel extends VBox {
    private final static Logger mLog = LoggerFactory.getLogger(AudioRecordingsPanel.class);
    private UserPreferences mUserPreferences;


    private ObservableList<RecordingItem> mRecordings;
    private FilteredList<RecordingItem> mFilteredRecordings;
    private TableView<RecordingItem> mTableView;
    private DatePicker mStartDatePicker;
    private DatePicker mEndDatePicker;
    private Spinner<Integer> mStartHourSpinner;
    private Spinner<Integer> mStartMinuteSpinner;
    private Spinner<Integer> mEndHourSpinner;
    private Spinner<Integer> mEndMinuteSpinner;
    private ComboBox<String> mAliasComboBox;
    private ComboBox<String> mChannelComboBox;



    private MediaPlayer mMediaPlayer;
    private Button mStopButton;
    private Label mStatusLabel;
    private Label mDiskUsageLabel;

    private io.github.dsheirer.playlist.PlaylistManager mPlaylistManager;

    public AudioRecordingsPanel(UserPreferences userPreferences, io.github.dsheirer.playlist.PlaylistManager playlistManager) {
        mPlaylistManager = playlistManager;
        mUserPreferences = userPreferences;
        setMinSize(0, 0);

        Platform.runLater(this::initFx);
    }


    private void populateFilterOptions() {
        if (mPlaylistManager == null) return;

        Platform.runLater(() -> {
            mAliasComboBox.getItems().clear();
            mAliasComboBox.getItems().add("All");
            mPlaylistManager.getAliasModel().getAliases().forEach(alias -> {
                mAliasComboBox.getItems().add(alias.getName());
            });
            mAliasComboBox.getSelectionModel().select("All");

            mChannelComboBox.getItems().clear();
            mChannelComboBox.getItems().add("All");
            mPlaylistManager.getChannelModel().getChannels().forEach(channel -> {
                mChannelComboBox.getItems().add(channel.getName());
            });
            mChannelComboBox.getSelectionModel().select("All");
        });
    }

    private void initFx() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        mTableView = new TableView<>();
        mRecordings = FXCollections.observableArrayList();
        mFilteredRecordings = new FilteredList<>(mRecordings, p -> true);

        // Top Filter Bar
        VBox filterContainer = new VBox(2);
        filterContainer.getStyleClass().add("kennebec-card");

        mStartDatePicker = new DatePicker();
        mStartDatePicker.setPromptText("Start Date");
        mStartDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> updateFilters());

        mEndDatePicker = new DatePicker();
        mEndDatePicker.setPromptText("End Date");
        mEndDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> updateFilters());

        mStartHourSpinner = new Spinner<>(0, 23, 0);
        mStartHourSpinner.setPrefWidth(60);
        mStartHourSpinner.setEditable(true);
        mStartHourSpinner.valueProperty().addListener((obs, oldVal, newVal) -> updateFilters());
        mStartMinuteSpinner = new Spinner<>(0, 59, 0);
        mStartMinuteSpinner.setPrefWidth(60);
        mStartMinuteSpinner.setEditable(true);
        mStartMinuteSpinner.valueProperty().addListener((obs, oldVal, newVal) -> updateFilters());

        mEndHourSpinner = new Spinner<>(0, 23, 23);
        mEndHourSpinner.setPrefWidth(60);
        mEndHourSpinner.setEditable(true);
        mEndHourSpinner.valueProperty().addListener((obs, oldVal, newVal) -> updateFilters());
        mEndMinuteSpinner = new Spinner<>(0, 59, 59);
        mEndMinuteSpinner.setPrefWidth(60);
        mEndMinuteSpinner.setEditable(true);
        mEndMinuteSpinner.valueProperty().addListener((obs, oldVal, newVal) -> updateFilters());

        mAliasComboBox = new ComboBox<>();
        mAliasComboBox.setPromptText("Filter Alias");
        mAliasComboBox.getItems().add("All");
        mAliasComboBox.getSelectionModel().select("All");
        mAliasComboBox.valueProperty().addListener((obs, oldVal, newVal) -> updateFilters());

        mChannelComboBox = new ComboBox<>();
        mChannelComboBox.setPromptText("Filter Channel");
        mChannelComboBox.getItems().add("All");
        mChannelComboBox.getSelectionModel().select("All");
        mChannelComboBox.valueProperty().addListener((obs, oldVal, newVal) -> updateFilters());

        Button refreshButton = new Button("Refresh");
        refreshButton.setTooltip(new Tooltip("Reload audio recordings from disk"));
        refreshButton.setOnAction(e -> loadRecordings());

        mStopButton = new Button("Stop Playback");
        mStopButton.setTooltip(new Tooltip("Stop the currently playing audio recording"));
        mStopButton.setDisable(true);
        mStopButton.setOnAction(e -> stopPlayback());

        Button clearFiltersButton = new Button("Clear Filters");
        clearFiltersButton.setTooltip(new Tooltip("Reset all date, time, and alias filters"));
        clearFiltersButton.getStyleClass().add("kennebec-toolbar-button");
        clearFiltersButton.setOnAction(e -> {
            mStartDatePicker.setValue(null);
            mEndDatePicker.setValue(null);
            mStartHourSpinner.getValueFactory().setValue(0);
            mStartMinuteSpinner.getValueFactory().setValue(0);
            mEndHourSpinner.getValueFactory().setValue(23);
            mEndMinuteSpinner.getValueFactory().setValue(59);
            mAliasComboBox.getSelectionModel().select("All");
            mChannelComboBox.getSelectionModel().select("All");
        });

        Button deleteSelectedButton = new Button("Delete Selected");
        deleteSelectedButton.setTooltip(new Tooltip("Permanently delete the selected recording(s). This action cannot be undone."));
        deleteSelectedButton.setOnAction(e -> {
            List<RecordingItem> selectedItems = new ArrayList<>(mTableView.getSelectionModel().getSelectedItems());
            if (selectedItems.isEmpty()) return;

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION); io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane());
            alert.setTitle("Delete Confirmation");
            alert.setHeaderText("Delete Selected Recordings");
            alert.setContentText("Are you sure you want to delete " + selectedItems.size() + " selected recording(s)?\nThis action cannot be undone.");

            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    for (RecordingItem item : selectedItems) {
                        try {
                            Files.deleteIfExists(item.getFile());
                            mRecordings.remove(item);
                        } catch (IOException ex) {
                            mLog.error("Failed to delete recording: " + item.getFile(), ex);
                        }
                    }
                }
            });
        });

        deleteSelectedButton.disableProperty().bind(
            javafx.beans.binding.Bindings.isEmpty(mTableView.getSelectionModel().getSelectedItems())
        );

        Button deleteAllButton = new Button("Delete All");
        deleteAllButton.setTooltip(new Tooltip("Permanently delete all currently displayed recording(s). This action cannot be undone."));
        deleteAllButton.setOnAction(e -> {
            List<RecordingItem> itemsToDelete = new ArrayList<>(mFilteredRecordings);
            if (itemsToDelete.isEmpty()) return;

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION); io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane());
            alert.setTitle("Delete Confirmation");
            alert.setHeaderText("Delete ALL Recordings");
            alert.setContentText("Are you sure you want to delete all " + itemsToDelete.size() + " recording(s) currently displayed?\nThis action cannot be undone.");

            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    for (RecordingItem item : itemsToDelete) {
                        try {
                            Files.deleteIfExists(item.getFile());
                            mRecordings.remove(item);
                        } catch (IOException ex) {
                            mLog.error("Failed to delete recording: " + item.getFile(), ex);
                        }
                    }
                }
            });
        });

        deleteAllButton.disableProperty().bind(
            javafx.beans.binding.Bindings.isEmpty(mFilteredRecordings)
        );

        // Row 1: Date and Time filters
        HBox controlsBox1 = new HBox(16);
        controlsBox1.getStyleClass().add("kennebec-filter-toolbar");
        
        HBox dateBox = new HBox(8);
        dateBox.setAlignment(Pos.CENTER_LEFT);
        Label dateLabel = new Label("Date Range:");
        dateLabel.getStyleClass().add("kennebec-toolbar-label");
        dateLabel.setMinWidth(Region.USE_PREF_SIZE);
        Label dateToLabel = new Label("to");
        dateToLabel.getStyleClass().add("kennebec-toolbar-label");
        dateBox.getChildren().addAll(dateLabel, mStartDatePicker, dateToLabel, mEndDatePicker);

        HBox timeBox = new HBox(8);
        timeBox.setAlignment(Pos.CENTER_LEFT);
        Label timeLabel = new Label("Time Range:");
        timeLabel.getStyleClass().add("kennebec-toolbar-label");
        timeLabel.setMinWidth(Region.USE_PREF_SIZE);
        Label timeColon1 = new Label(":");
        Label timeTo = new Label("to");
        timeTo.getStyleClass().add("kennebec-toolbar-label");
        Label timeColon2 = new Label(":");
        timeBox.getChildren().addAll(timeLabel, mStartHourSpinner, timeColon1, mStartMinuteSpinner, timeTo, mEndHourSpinner, timeColon2, mEndMinuteSpinner);
        
        controlsBox1.getChildren().addAll(dateBox, timeBox);

        // Row 2: Alias, Channel filters + action buttons
        HBox controlsBox2 = new HBox(16);
        controlsBox2.getStyleClass().add("kennebec-filter-toolbar");
        
        HBox aliasBox = new HBox(8);
        aliasBox.setAlignment(Pos.CENTER_LEFT);
        Label aliasLabel = new Label("Alias:");
        aliasLabel.getStyleClass().add("kennebec-toolbar-label");
        aliasLabel.setMinWidth(Region.USE_PREF_SIZE);
        aliasBox.getChildren().addAll(aliasLabel, mAliasComboBox);

        HBox channelBox = new HBox(8);
        channelBox.setAlignment(Pos.CENTER_LEFT);
        Label channelLabel = new Label("Channel:");
        channelLabel.getStyleClass().add("kennebec-toolbar-label");
        channelLabel.setMinWidth(Region.USE_PREF_SIZE);
        channelBox.getChildren().addAll(channelLabel, mChannelComboBox);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        refreshButton.getStyleClass().add("kennebec-toolbar-button-primary");
        mStopButton.getStyleClass().add("kennebec-toolbar-button");
        deleteSelectedButton.getStyleClass().add("kennebec-toolbar-button");
        deleteAllButton.getStyleClass().add("kennebec-toolbar-button");

        HBox buttonBox = new HBox(8);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        buttonBox.getChildren().addAll(refreshButton, mStopButton, clearFiltersButton, deleteSelectedButton, deleteAllButton);

        controlsBox2.getChildren().addAll(aliasBox, channelBox, spacer, buttonBox);

        filterContainer.getChildren().addAll(controlsBox1, controlsBox2);

        root.setTop(filterContainer);

        // Table
        mTableView.setPlaceholder(new Label("No audio recordings found"));
        mTableView.getStyleClass().add("hig-data-table");
        HBox.setHgrow(mTableView, Priority.ALWAYS);
        mTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        TableColumn<RecordingItem, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(data -> data.getValue().dateProperty());

        TableColumn<RecordingItem, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(data -> data.getValue().timeProperty());

        TableColumn<RecordingItem, String> channelCol = new TableColumn<>("Channel");
        channelCol.setCellValueFactory(data -> data.getValue().channelProperty());

        TableColumn<RecordingItem, String> toAliasCol = new TableColumn<>("To Alias");
        toAliasCol.setCellValueFactory(data -> data.getValue().toAliasProperty());

        TableColumn<RecordingItem, String> fromAliasCol = new TableColumn<>("From Alias");
        fromAliasCol.setCellValueFactory(data -> data.getValue().fromAliasProperty());

        TableColumn<RecordingItem, String> lengthCol = new TableColumn<>("Size");
        lengthCol.setCellValueFactory(data -> data.getValue().lengthProperty());

        TableColumn<RecordingItem, RecordingItem> actionCol = new TableColumn<>("Action");
        actionCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue()));
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button playBtn = new Button("Play");

            {
                playBtn.setTooltip(new Tooltip("Play this recording"));
                playBtn.setOnAction(e -> {
                    RecordingItem item = getItem();
                    if (item != null) {
                        playRecording(item);
                    }
                });
            }

            @Override
            protected void updateItem(RecordingItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    setGraphic(playBtn);
                }
            }
        });

        mTableView.getColumns().addAll(dateCol, timeCol, channelCol, toAliasCol, fromAliasCol, lengthCol, actionCol);

        SortedList<RecordingItem> sortedData = new SortedList<>(mFilteredRecordings);
        sortedData.comparatorProperty().bind(mTableView.comparatorProperty());
        mTableView.setItems(sortedData);

        root.setCenter(mTableView);

        // Status bar at bottom
        HBox statusBar = new HBox(16);
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setPadding(new Insets(6, 10, 6, 10));
        statusBar.getStyleClass().add("kennebec-filter-toolbar");

        mStatusLabel = new Label("Showing 0 of 0 recordings");
        mStatusLabel.getStyleClass().add("kennebec-toolbar-label");

        mDiskUsageLabel = new Label("Disk Usage: calculating...");
        mDiskUsageLabel.getStyleClass().add("kennebec-toolbar-label");

        Region statusSpacer = new Region();
        HBox.setHgrow(statusSpacer, Priority.ALWAYS);

        statusBar.getChildren().addAll(mStatusLabel, statusSpacer, mDiskUsageLabel);
        root.setBottom(statusBar);

        // Update status label when filtered list changes
        mFilteredRecordings.addListener((javafx.collections.ListChangeListener<RecordingItem>) change -> {
            updateStatusLabel();
        });
        mRecordings.addListener((javafx.collections.ListChangeListener<RecordingItem>) change -> {
            updateStatusLabel();
        });

        root.setMinSize(0, 0);
        mTableView.setMinSize(0, 0);
        VBox.setVgrow(root, Priority.ALWAYS);

        root.getStylesheets().add(getClass().getResource("/sdrtrunk_style.css").toExternalForm());

        // Stop playback when panel is removed from scene
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                stopPlayback();
            }
        });

        getChildren().add(root);

        loadRecordings();
        populateFilterOptions();
    }

    private void updateFilters() {
        LocalDate startDate = mStartDatePicker.getValue();
        LocalDate endDate = mEndDatePicker.getValue();
        LocalTime startTime = LocalTime.of(mStartHourSpinner.getValue(), mStartMinuteSpinner.getValue());
        LocalTime endTime = LocalTime.of(mEndHourSpinner.getValue(), mEndMinuteSpinner.getValue());

        String aliasFilter = mAliasComboBox.getValue() != null && !mAliasComboBox.getValue().equals("All") ? mAliasComboBox.getValue() : null;
        String channelFilter = mChannelComboBox.getValue() != null && !mChannelComboBox.getValue().equals("All") ? mChannelComboBox.getValue() : null;

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        mFilteredRecordings.setPredicate(item -> {
            try {
                LocalDate itemDate = LocalDate.parse(item.getDate(), dateFormatter);
                LocalTime itemTime = LocalTime.parse(item.getTime(), timeFormatter);

                if (startDate != null && itemDate.isBefore(startDate)) return false;
                if (endDate != null && itemDate.isAfter(endDate)) return false;

                if (startTime.isBefore(endTime) || startTime.equals(endTime)) {
                    if (itemTime.isBefore(startTime) || itemTime.isAfter(endTime)) return false;
                } else {
                    // Time range wraps around midnight
                    if (itemTime.isBefore(startTime) && itemTime.isAfter(endTime)) return false;
                }

                if (channelFilter != null && !item.getChannel().equals(channelFilter)) return false;

                if (aliasFilter != null) {
                    boolean matchesTo = item.getToAlias().equals(aliasFilter);
                    boolean matchesFrom = item.getFromAlias().equals(aliasFilter);
                    if (!matchesTo && !matchesFrom) return false;
                }
            } catch (Exception e) {
                // If parsing fails, don't filter it out
                mLog.warn("Could not parse date/time for filter: " + item.getDate() + " " + item.getTime());
            }
            return true;
        });
    }

    public void loadRecordings() {
        mRecordings.clear();
        Path dir = mUserPreferences.getDirectoryPreference().getDirectoryRecording();
        if (dir == null || !Files.exists(dir)) {
            return;
        }

        Thread loaderThread = new Thread(() -> {
            try (Stream<Path> stream = Files.list(dir)) {
                List<RecordingItem> items = new ArrayList<>();
                stream.filter(Files::isRegularFile)
                      .filter(p -> {
                          String name = p.getFileName().toString().toLowerCase();
                          return name.endsWith(".wav") || name.endsWith(".mp3");
                      })
                      .forEach(p -> {
                          RecordingItem item = parseFilename(p);
                          items.add(item);
                      });
                Platform.runLater(() -> {
                    mRecordings.addAll(items);
                    updateDiskUsage(dir);
                    performAutoCleanup(dir);
                });
            } catch (IOException e) {
                mLog.error("Error reading recordings directory", e);
            }
        });
        loaderThread.setDaemon(true);
        loaderThread.start();
    }

    private RecordingItem parseFilename(Path path) {
        RecordingItem item = new RecordingItem(path);
        String name = path.getFileName().toString();

        try {
            long size = Files.size(path);
            item.setLength(String.format("%.2f MB", size / (1024.0 * 1024.0)));
        } catch (IOException e) {
            item.setLength("Unknown");
        }

        // Parse filename
        String withoutExt = name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name;
        String[] parts = withoutExt.split("_");

        if (parts.length < 2) {
            // Not enough parts to parse - just use the filename as channel
            item.setChannel(withoutExt);
            return item;
        }

        // Find the date and time parts by scanning for an 8-digit numeric date (yyyyMMdd)
        // followed by a 6+ digit numeric time (HHmmss). The recording manager always outputs
        // the timestamp as "yyyyMMdd_HHmmss" at the start of the filename, but older recordings
        // or other tools may place it differently.
        int dateIdx = -1;
        int timeIdx = -1;

        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].length() == 8 && parts[i].matches("\\d{8}")) {
                // Validate it looks like a plausible date (year 1900-2099)
                int year = Integer.parseInt(parts[i].substring(0, 4));
                int month = Integer.parseInt(parts[i].substring(4, 6));
                int day = Integer.parseInt(parts[i].substring(6, 8));
                if (year >= 1900 && year <= 2099 && month >= 1 && month <= 12 && day >= 1 && day <= 31) {
                    // Check if the next part looks like a time (6+ digits)
                    if (parts[i + 1].length() >= 6 && parts[i + 1].substring(0, 6).matches("\\d{6}")) {
                        dateIdx = i;
                        timeIdx = i + 1;
                        break;
                    }
                }
            }
        }

        // Check for baseband format: FREQ_baseband_yyyyMMdd_HHmmss
        boolean isBaseband = parts.length >= 4 && parts[1].equals("baseband");

        if (dateIdx >= 0 && timeIdx >= 0) {
            // Found date and time parts
            String datePart = parts[dateIdx];
            item.setDate(datePart.substring(0, 4) + "-" + datePart.substring(4, 6) + "-" + datePart.substring(6, 8));

            String timePart = parts[timeIdx];
            item.setTime(timePart.substring(0, 2) + ":" + timePart.substring(2, 4) + ":" + timePart.substring(4, 6));

            // Build channel info from parts between time and _TO_/_FROM_ markers,
            // excluding date and time parts
            int toIdx = withoutExt.indexOf("_TO_");
            int fromIdx = withoutExt.indexOf("_FROM_");
            int endChannelIdx = toIdx != -1 ? toIdx : (fromIdx != -1 ? fromIdx : withoutExt.length());

            // Calculate the character offset right after the time part
            int startChannelOffset = 0;
            for (int i = 0; i <= timeIdx; i++) {
                startChannelOffset += parts[i].length();
                if (i < timeIdx) {
                    startChannelOffset += 1; // underscore
                }
            }
            startChannelOffset += 1; // trailing underscore after time part

            if (isBaseband) {
                item.setChannel(parts[0] + " baseband");
            } else if (endChannelIdx > startChannelOffset && startChannelOffset <= withoutExt.length()) {
                String channelInfo = withoutExt.substring(startChannelOffset, endChannelIdx);
                item.setChannel(channelInfo.replace("_", " ").trim());
            }

            // Parse TO alias
            if (toIdx != -1) {
                int toEnd = fromIdx != -1 ? fromIdx : withoutExt.length();
                int toneIdx = withoutExt.indexOf("_TONES");
                if (toneIdx != -1 && toneIdx < toEnd) {
                    toEnd = toneIdx;
                }
                if (toIdx + 4 < toEnd) {
                    item.setToAlias(withoutExt.substring(toIdx + 4, toEnd).replace("_", " ").trim());
                }
            }

            // Parse FROM alias
            if (fromIdx != -1) {
                int fromEnd = withoutExt.length();
                int toneIdx = withoutExt.indexOf("_TONES");
                if (toneIdx != -1 && toneIdx > fromIdx) {
                    fromEnd = toneIdx;
                }
                if (fromIdx + 6 < fromEnd) {
                    item.setFromAlias(withoutExt.substring(fromIdx + 6, fromEnd).replace("_", " ").trim());
                }
            }
        } else {
            // Could not find a valid date/time pattern - try using file modification time
            try {
                java.time.Instant lastModified = Files.getLastModifiedTime(path).toInstant();
                java.time.LocalDateTime ldt = java.time.LocalDateTime.ofInstant(lastModified, java.time.ZoneId.systemDefault());
                item.setDate(ldt.toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                item.setTime(ldt.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            } catch (IOException e) {
                item.setDate("");
                item.setTime("");
            }
            // Use the whole filename (minus extension) as channel info
            item.setChannel(withoutExt.replace("_", " ").trim());
        }

        return item;
    }

    private void playRecording(RecordingItem item) {
        stopPlayback();
        try {
            String uri = item.getFile().toUri().toString();
            Media media = new Media(uri);
            mMediaPlayer = new MediaPlayer(media);
            mMediaPlayer.setOnEndOfMedia(() -> {
                mStopButton.setDisable(true);
            });
            mMediaPlayer.play();
            mStopButton.setDisable(false);
        } catch (Exception e) {
            mLog.error("Error playing recording: " + item.getFile(), e);
        }
    }

    private void stopPlayback() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.dispose();
            mMediaPlayer = null;
        }
        if (mStopButton != null) {
            mStopButton.setDisable(true);
        }
    }

    private void updateStatusLabel() {
        if (mStatusLabel != null) {
            mStatusLabel.setText("Showing " + mFilteredRecordings.size() + " of " + mRecordings.size() + " recordings");
        }
    }

    private void updateDiskUsage(Path dir) {
        if (mDiskUsageLabel == null || dir == null) return;
        Thread t = new Thread(() -> {
            try {
                long totalBytes = 0;
                try (Stream<Path> stream = Files.list(dir)) {
                    totalBytes = stream.filter(Files::isRegularFile)
                        .mapToLong(p -> { try { return Files.size(p); } catch (IOException e) { return 0; } })
                        .sum();
                }
                long usageMB = totalBytes / (1024 * 1024);
                int maxMB = mUserPreferences.getDirectoryPreference().getDirectoryMaxUsageRecordings();
                final String text = String.format("Disk Usage: %d MB / %d MB", usageMB, maxMB);
                Platform.runLater(() -> mDiskUsageLabel.setText(text));
            } catch (IOException e) {
                Platform.runLater(() -> mDiskUsageLabel.setText("Disk Usage: error"));
            }
        });
        t.setDaemon(true);
        t.start();
    }

    /**
     * Auto-cleanup: if recordings directory exceeds the configured max disk usage threshold,
     * deletes oldest recordings until usage is below the threshold.
     */
    private void performAutoCleanup(Path dir) {
        if (dir == null || !Files.exists(dir)) return;
        Thread t = new Thread(() -> {
            try {
                int maxMB = mUserPreferences.getDirectoryPreference().getDirectoryMaxUsageRecordings();
                long maxBytes = (long) maxMB * 1024 * 1024;

                // Calculate current usage
                long totalBytes;
                List<Path> allFiles = new ArrayList<>();
                try (Stream<Path> stream = Files.list(dir)) {
                    stream.filter(Files::isRegularFile)
                          .filter(p -> {
                              String name = p.getFileName().toString().toLowerCase();
                              return name.endsWith(".wav") || name.endsWith(".mp3");
                          })
                          .forEach(allFiles::add);
                }

                totalBytes = allFiles.stream()
                    .mapToLong(p -> { try { return Files.size(p); } catch (IOException e) { return 0; } })
                    .sum();

                if (totalBytes <= maxBytes) return;

                // Sort by last modified time (oldest first)
                allFiles.sort(Comparator.comparingLong(p -> {
                    try { return Files.getLastModifiedTime(p).toMillis(); } catch (IOException e) { return 0; }
                }));

                int deletedCount = 0;
                long freedBytes = 0;
                for (Path file : allFiles) {
                    if (totalBytes <= maxBytes) break;
                    try {
                        long fileSize = Files.size(file);
                        Files.delete(file);
                        totalBytes -= fileSize;
                        freedBytes += fileSize;
                        deletedCount++;
                    } catch (IOException e) {
                        mLog.warn("Auto-cleanup: failed to delete " + file.getFileName(), e);
                    }
                }

                if (deletedCount > 0) {
                    final int count = deletedCount;
                    final long freed = freedBytes;
                    mLog.info("Auto-cleanup: deleted {} recordings, freed {} MB",
                        count, freed / (1024 * 1024));
                    Platform.runLater(() -> {
                        loadRecordings(); // Reload to reflect deletions
                    });
                }
            } catch (IOException e) {
                mLog.error("Auto-cleanup error", e);
            }
        });
        t.setDaemon(true);
        t.start();
    }
}

