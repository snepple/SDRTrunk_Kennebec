


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
import javafx.scene.control.Slider;
import javafx.util.Duration;
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
    private ComboBox<String> mTypeComboBox;



    private RecordingPlayer mPlayer;
    private javafx.animation.Timeline mProgressTimer;
    private Button mStopButton;
    private Label mStatusLabel;
    private Label mDiskUsageLabel;

    //Playback transport bar (Apple Music style)
    private HBox mPlayerBar;
    private Label mNowPlayingLabel;
    private Button mPlayPauseButton;
    private Slider mSeekSlider;
    private Label mCurrentTimeLabel;
    private Label mTotalTimeLabel;
    private RecordingItem mCurrentItem;
    private boolean mUserSeeking = false;

    //Known channel names from the playlist, used to display a clean channel name in the table.
    private final java.util.Set<String> mKnownChannelNames = new java.util.LinkedHashSet<>();

    private io.github.dsheirer.playlist.PlaylistManager mPlaylistManager;
    private io.github.dsheirer.audio.playback.AudioPlaybackManager mAudioPlaybackManager;

    public AudioRecordingsPanel(UserPreferences userPreferences, io.github.dsheirer.playlist.PlaylistManager playlistManager,
                                io.github.dsheirer.audio.playback.AudioPlaybackManager audioPlaybackManager) {
        mPlaylistManager = playlistManager;
        mUserPreferences = userPreferences;
        mAudioPlaybackManager = audioPlaybackManager;
        setMinSize(0, 0);

        Platform.runLater(this::initFx);
    }

    /**
     * Resolves the audio output device SDRTrunk currently uses for live audio, so recordings play through the same
     * speakers/headphones rather than the system default.  Returns null when unknown (e.g., no device configured),
     * in which case playback falls back to the default mixer.
     */
    private javax.sound.sampled.Mixer.Info getLiveAudioMixerInfo() {
        if (mAudioPlaybackManager != null && mAudioPlaybackManager.getAudioPlaybackDevice() != null) {
            return mAudioPlaybackManager.getAudioPlaybackDevice().getMixerInfo();
        }
        return null;
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

        mTypeComboBox = new ComboBox<>();
        mTypeComboBox.setPromptText("Filter Type");
        mTypeComboBox.getItems().addAll("All", "Audio", "Baseband I/Q");
        mTypeComboBox.getSelectionModel().select("All");
        mTypeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> updateFilters());

        Button refreshButton = new Button("_Refresh");
        refreshButton.setMnemonicParsing(true);
        refreshButton.accessibleTextProperty().set("Refresh Recordings");
        refreshButton.accessibleHelpProperty().set("Reloads the recordings list from disk.");
        refreshButton.setTooltip(new Tooltip("Reloads the recordings list from disk."));
        refreshButton.setOnAction(e -> loadRecordings());

        mStopButton = new Button("Sto_p Playback");
        mStopButton.setMnemonicParsing(true);
        mStopButton.accessibleTextProperty().set("Stop Playback");
        mStopButton.accessibleHelpProperty().set("Stop the currently playing recording.");
        mStopButton.setTooltip(new Tooltip("Stop the currently playing recording."));
        mStopButton.setDisable(true);
        mStopButton.setOnAction(e -> stopPlayback());

        Button clearFiltersButton = new Button("Clear _Filters");
        clearFiltersButton.setMnemonicParsing(true);
        clearFiltersButton.accessibleTextProperty().set("Clear Filters");
        clearFiltersButton.accessibleHelpProperty().set("Reset all search filters to default.");
        clearFiltersButton.setTooltip(new Tooltip("Reset all search filters to default."));
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
            mTypeComboBox.getSelectionModel().select("All");
        });

        Button deleteSelectedButton = new Button("Delete _Selected");
        deleteSelectedButton.setMnemonicParsing(true);
        deleteSelectedButton.accessibleTextProperty().set("Delete Selected Recordings");
        deleteSelectedButton.accessibleHelpProperty().set("Permanently delete the selected recordings. This action cannot be undone.");
        deleteSelectedButton.setTooltip(new Tooltip("Permanently delete the selected recordings.\nThis action cannot be undone."));
        deleteSelectedButton.setOnAction(e -> {
            List<RecordingItem> selectedItems = new ArrayList<>(mTableView.getSelectionModel().getSelectedItems());
            if (selectedItems.isEmpty()) return;

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION); io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane());
            alert.setTitle("Delete Confirmation");
            alert.setHeaderText("Delete Selected Recordings");
            alert.setContentText("Are you sure you want to delete " + selectedItems.size() + " selected recording(s)?\nThis action cannot be undone.");

            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    deleteSelectedButton.disableProperty().unbind();
                    deleteSelectedButton.setDisable(true);
                    deleteSelectedButton.setText("Deleting...");
                    List<RecordingItem> snapshot = new ArrayList<>(selectedItems);
                    io.github.dsheirer.util.ThreadPool.CACHED.submit(() -> {
                        List<RecordingItem> successfullyDeleted = new ArrayList<>();
                        for (RecordingItem item : snapshot) {
                            try {
                                Files.deleteIfExists(item.getFile());
                                successfullyDeleted.add(item);
                            } catch (IOException ex) {
                                mLog.error("Failed to delete recording: " + item.getFile(), ex);
                            }
                        }
                        javafx.application.Platform.runLater(() -> {
                            mRecordings.removeAll(successfullyDeleted);
                            deleteSelectedButton.setText("Delete Selected");
                            deleteSelectedButton.disableProperty().bind(javafx.beans.binding.Bindings.isEmpty(mTableView.getSelectionModel().getSelectedItems()));
                        });
                    });
                }
            });
        });

        deleteSelectedButton.disableProperty().bind(
            javafx.beans.binding.Bindings.isEmpty(mTableView.getSelectionModel().getSelectedItems())
        );

        Button deleteAllButton = new Button("Delete _All");
        deleteAllButton.setMnemonicParsing(true);
        deleteAllButton.accessibleTextProperty().set("Delete All Recordings");
        deleteAllButton.accessibleHelpProperty().set("Permanently delete all currently displayed recordings. This action cannot be undone.");
        deleteAllButton.setTooltip(new Tooltip("Permanently delete all currently displayed recordings.\nThis action cannot be undone."));
        deleteAllButton.setOnAction(e -> {
            List<RecordingItem> itemsToDelete = new ArrayList<>(mFilteredRecordings);
            if (itemsToDelete.isEmpty()) return;

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION); io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane());
            alert.setTitle("Delete Confirmation");
            alert.setHeaderText("Delete ALL Recordings");
            alert.setContentText("Are you sure you want to delete all " + itemsToDelete.size() + " recording(s) currently displayed?\nThis action cannot be undone.");

            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    deleteAllButton.disableProperty().unbind();
                    deleteAllButton.setDisable(true);
                    deleteAllButton.setText("Deleting...");
                    List<RecordingItem> snapshot = new ArrayList<>(itemsToDelete);
                    io.github.dsheirer.util.ThreadPool.CACHED.submit(() -> {
                        List<RecordingItem> successfullyDeleted = new ArrayList<>();
                        for (RecordingItem item : snapshot) {
                            try {
                                Files.deleteIfExists(item.getFile());
                                successfullyDeleted.add(item);
                            } catch (IOException ex) {
                                mLog.error("Failed to delete recording: " + item.getFile(), ex);
                            }
                        }
                        javafx.application.Platform.runLater(() -> {
                            mRecordings.removeAll(successfullyDeleted);
                            deleteAllButton.setText("Delete All");
                            deleteAllButton.disableProperty().bind(javafx.beans.binding.Bindings.isEmpty(mFilteredRecordings));
                        });
                    });
                }
            });
        });

        deleteAllButton.disableProperty().bind(
            javafx.beans.binding.Bindings.isEmpty(mFilteredRecordings)
        );

        // Row 1: Date and Time filters
        HBox controlsBox1 = new HBox(16);
        controlsBox1.getStyleClass().add("kennebec-filter-toolbar");
        
        //Group each date with its time so it reads as "Start = this date at this time" and
        //"End = this date at this time", rather than a separate date range and time range.
        HBox startBox = new HBox(8);
        startBox.setAlignment(Pos.CENTER_LEFT);
        Label startLabel = new Label("Start:");
        startLabel.getStyleClass().add("kennebec-toolbar-label");
        startLabel.setMinWidth(Region.USE_PREF_SIZE);
        Label startColon = new Label(":");
        startBox.getChildren().addAll(startLabel, mStartDatePicker, mStartHourSpinner, startColon, mStartMinuteSpinner);

        HBox endBox = new HBox(8);
        endBox.setAlignment(Pos.CENTER_LEFT);
        Label endLabel = new Label("End:");
        endLabel.getStyleClass().add("kennebec-toolbar-label");
        endLabel.setMinWidth(Region.USE_PREF_SIZE);
        Label endColon = new Label(":");
        endBox.getChildren().addAll(endLabel, mEndDatePicker, mEndHourSpinner, endColon, mEndMinuteSpinner);

        controlsBox1.getChildren().addAll(startBox, endBox);

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

        HBox typeBox = new HBox(8);
        typeBox.setAlignment(Pos.CENTER_LEFT);
        Label typeLabel = new Label("Type:");
        typeLabel.getStyleClass().add("kennebec-toolbar-label");
        typeLabel.setMinWidth(Region.USE_PREF_SIZE);
        typeBox.getChildren().addAll(typeLabel, mTypeComboBox);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        refreshButton.getStyleClass().add("kennebec-toolbar-button-primary");
        mStopButton.getStyleClass().add("kennebec-toolbar-button");
        deleteSelectedButton.getStyleClass().add("kennebec-toolbar-button");
        deleteAllButton.getStyleClass().add("kennebec-toolbar-button");

        HBox buttonBox = new HBox(8);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        buttonBox.getChildren().addAll(refreshButton, mStopButton, clearFiltersButton, deleteSelectedButton, deleteAllButton);

        controlsBox2.getChildren().addAll(aliasBox, channelBox, typeBox, spacer, buttonBox);

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

        TableColumn<RecordingItem, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> data.getValue().typeProperty());

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
                    //Baseband (I/Q) captures aren't playable as audio - disable the action for them.
                    boolean baseband = item.isBaseband();
                    playBtn.setDisable(baseband);
                    playBtn.setTooltip(baseband
                        ? new Tooltip("Baseband (I/Q) recording - not playable as audio")
                        : null);
                    setGraphic(playBtn);
                }
            }
        });

        mTableView.getColumns().addAll(dateCol, timeCol, channelCol, typeCol, toAliasCol, fromAliasCol, lengthCol, actionCol);
        mTableView.setTableMenuButtonVisible(true);

        SortedList<RecordingItem> sortedData = new SortedList<>(mFilteredRecordings);
        sortedData.comparatorProperty().bind(mTableView.comparatorProperty());
        mTableView.setItems(sortedData);

        //Selecting a recording loads it into the transport bar (ready to play), so the user gets the
        //Apple Music style controls without having to press the row's Play button first.
        mTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null && newSel != mCurrentItem) {
                loadMedia(newSel, false);
            }
        });

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

        //Transport/playback bar sits just above the status bar; hidden until a recording is selected.
        mPlayerBar = buildPlayerBar();

        VBox bottomContainer = new VBox();
        bottomContainer.getChildren().addAll(mPlayerBar, statusBar);
        root.setBottom(bottomContainer);

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

        //Do NOT add sdrtrunk_style.css to this panel's own root.  It is already applied at the scene level
        //(see SDRTrunk main scene setup).  Adding it again to a child Parent gives it higher CSS precedence
        //than the scene-level night-mode.css, which is what prevented dark mode from theming this page.

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
        String typeFilter = mTypeComboBox.getValue() != null && !mTypeComboBox.getValue().equals("All") ? mTypeComboBox.getValue() : null;

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        mFilteredRecordings.setPredicate(item -> {
            //Type filter applies regardless of whether the date/time parsed successfully.
            if (typeFilter != null && !typeFilter.equals(item.getType())) return false;

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

        //Snapshot known channel names so the loader thread can map a verbose recording filename to a
        //clean channel name for the Channel column.
        mKnownChannelNames.clear();
        if (mPlaylistManager != null && mPlaylistManager.getChannelModel() != null) {
            mPlaylistManager.getChannelModel().getChannels().forEach(c -> {
                if (c.getName() != null && !c.getName().isEmpty()) {
                    mKnownChannelNames.add(c.getName());
                }
            });
        }

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
                          item.setChannel(simplifyChannelName(item.getChannel()));
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

        //Baseband (I/Q) captures are named "<channel>_baseband_...".  Flag them so the UI can distinguish
        //them from playable call audio (separate Type column, Type filter, no Play action).
        boolean isBaseband = name.toLowerCase().contains("baseband");
        item.setBaseband(isBaseband);
        item.setType(isBaseband ? "Baseband I/Q" : "Audio");

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

    /**
     * Maps a verbose parsed channel string to a clean channel name by matching against the known channel
     * names from the playlist.  Falls back to the original string when no confident match is found.
     */
    private String simplifyChannelName(String raw) {
        if (raw == null || raw.isEmpty() || mKnownChannelNames.isEmpty()) {
            return raw;
        }

        String rawNorm = raw.toLowerCase().replaceAll("[^a-z0-9]", "");
        String best = null;
        for (String name : mKnownChannelNames) {
            if (name == null || name.length() < 3) {
                continue;
            }
            if (raw.equalsIgnoreCase(name)) {
                return name;
            }
            String nameNorm = name.toLowerCase().replaceAll("[^a-z0-9]", "");
            if (!nameNorm.isEmpty() && rawNorm.contains(nameNorm)
                    && (best == null || name.length() > best.length())) {
                best = name;
            }
        }
        return best != null ? best : raw;
    }

    /**
     * Builds the Apple Music style transport bar: now-playing label, skip back/forward, play/pause,
     * stop, a seek slider, and current/total time labels.  Hidden until a recording is selected.
     */
    private HBox buildPlayerBar() {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(6, 12, 6, 12));
        bar.getStyleClass().add("kennebec-filter-toolbar");

        mNowPlayingLabel = new Label("");
        mNowPlayingLabel.getStyleClass().add("kennebec-toolbar-label");
        mNowPlayingLabel.setMinWidth(160);
        mNowPlayingLabel.setMaxWidth(280);

        Button skipBackBtn = new Button("⏪");
        skipBackBtn.getStyleClass().add("kennebec-toolbar-button");
        skipBackBtn.setTooltip(new Tooltip("Back 10 seconds"));
        skipBackBtn.setOnAction(e -> skipBy(-10));

        mPlayPauseButton = new Button("▶");
        mPlayPauseButton.getStyleClass().add("kennebec-toolbar-button-primary");
        mPlayPauseButton.setTooltip(new Tooltip("Play / Pause"));
        mPlayPauseButton.setOnAction(e -> togglePlayPause());

        Button skipFwdBtn = new Button("⏩");
        skipFwdBtn.getStyleClass().add("kennebec-toolbar-button");
        skipFwdBtn.setTooltip(new Tooltip("Forward 10 seconds"));
        skipFwdBtn.setOnAction(e -> skipBy(10));

        Button stopBtn = new Button("⏹");
        stopBtn.getStyleClass().add("kennebec-toolbar-button");
        stopBtn.setTooltip(new Tooltip("Stop"));
        stopBtn.setOnAction(e -> stopPlayback());

        mCurrentTimeLabel = new Label("0:00");
        mCurrentTimeLabel.getStyleClass().add("kennebec-toolbar-label");
        mTotalTimeLabel = new Label("0:00");
        mTotalTimeLabel.getStyleClass().add("kennebec-toolbar-label");

        mSeekSlider = new Slider(0, 1, 0);
        mSeekSlider.setDisable(true);
        mSeekSlider.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(mSeekSlider, Priority.ALWAYS);
        mSeekSlider.setOnMousePressed(e -> mUserSeeking = true);
        mSeekSlider.setOnMouseReleased(e -> {
            if (mPlayer != null) {
                mPlayer.seekSeconds(mSeekSlider.getValue());
            }
            mUserSeeking = false;
        });
        mSeekSlider.valueProperty().addListener((obs, o, n) -> {
            if (mUserSeeking) {
                mCurrentTimeLabel.setText(formatTime(Duration.seconds(n.doubleValue())));
            }
        });

        bar.getChildren().addAll(mNowPlayingLabel, skipBackBtn, mPlayPauseButton, skipFwdBtn, stopBtn,
            mCurrentTimeLabel, mSeekSlider, mTotalTimeLabel);

        bar.setVisible(false);
        bar.setManaged(false);
        return bar;
    }

    private void playRecording(RecordingItem item) {
        loadMedia(item, true);
    }

    /**
     * Loads a recording into the media player and transport bar, optionally starting playback.
     */
    private void loadMedia(RecordingItem item, boolean autoPlay) {
        if (item == null) {
            return;
        }

        disposePlayer();
        mCurrentItem = item;
        showPlayerBar(true);

        String channelName = (item.getChannel() != null && !item.getChannel().isEmpty())
            ? item.getChannel() : item.getFile().getFileName().toString();
        mNowPlayingLabel.setText(channelName);
        mSeekSlider.setValue(0);
        mSeekSlider.setDisable(true);
        mCurrentTimeLabel.setText("0:00");
        mTotalTimeLabel.setText("0:00");
        mPlayPauseButton.setText("▶");

        //Baseband (I/Q) recordings are raw captures, not playable audio.  Tell the user instead of
        //silently doing nothing (which was the prior behaviour).
        if (item.isBaseband()) {
            mNowPlayingLabel.setText("Baseband (I/Q) recording - not playable as audio");
            mPlayPauseButton.setDisable(true);
            mStopButton.setDisable(true);
            return;
        }

        mPlayPauseButton.setDisable(false);

        try {
            final RecordingItem loadingItem = item;
            mPlayer = RecordingPlayer.create(item.getFile(), getLiveAudioMixerInfo());
            mPlayer.setListener(new RecordingPlayer.Listener() {
                @Override
                public void onReady(double durationSeconds) {
                    //Ignore late callbacks from a player that has since been replaced by a different selection.
                    if (mCurrentItem != loadingItem) return;
                    if (durationSeconds > 0) {
                        mSeekSlider.setMax(durationSeconds);
                        mTotalTimeLabel.setText(formatTime(Duration.seconds(durationSeconds)));
                    }
                    mSeekSlider.setDisable(false);
                }

                @Override
                public void onEndOfMedia() {
                    if (mCurrentItem != loadingItem) return;
                    stopProgressTimer();
                    if (mPlayer != null) {
                        mPlayer.stop();
                    }
                    mSeekSlider.setValue(0);
                    mCurrentTimeLabel.setText("0:00");
                    mPlayPauseButton.setText("▶");
                    mStopButton.setDisable(true);
                }

                @Override
                public void onError(String message) {
                    if (mCurrentItem != loadingItem) return;
                    mLog.error("Error playing recording {}: {}", loadingItem.getFile(), message);
                    mNowPlayingLabel.setText("Unable to play: " + channelName);
                    mPlayPauseButton.setText("▶");
                    mStopButton.setDisable(true);
                }

                @Override
                public void onPlayingChanged(boolean playing) {
                    if (mCurrentItem != loadingItem) return;
                    mPlayPauseButton.setText(playing ? "⏸" : "▶");
                    if (playing) {
                        mStopButton.setDisable(false);
                        startProgressTimer();
                    } else {
                        stopProgressTimer();
                        updateProgressFromPlayer();
                    }
                }
            });

            mPlayer.prepare(autoPlay);

            if (autoPlay) {
                mStopButton.setDisable(false);
            }
        } catch (Exception e) {
            mLog.error("Error playing recording: " + item.getFile(), e);
            mNowPlayingLabel.setText("Unable to play: " + channelName);
        }
    }

    private void startProgressTimer() {
        if (mProgressTimer == null) {
            mProgressTimer = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(Duration.millis(200), e -> updateProgressFromPlayer()));
            mProgressTimer.setCycleCount(javafx.animation.Animation.INDEFINITE);
        }
        mProgressTimer.play();
    }

    private void stopProgressTimer() {
        if (mProgressTimer != null) {
            mProgressTimer.pause();
        }
    }

    private void updateProgressFromPlayer() {
        if (mPlayer == null || mUserSeeking) return;
        double pos = mPlayer.getPositionSeconds();
        mSeekSlider.setValue(pos);
        mCurrentTimeLabel.setText(formatTime(Duration.seconds(pos)));
    }

    private void togglePlayPause() {
        if (mPlayer == null) {
            if (mCurrentItem != null) {
                loadMedia(mCurrentItem, true);
            }
            return;
        }
        if (mPlayer.isPlaying()) {
            mPlayer.pause();
        } else {
            mPlayer.play();
            mStopButton.setDisable(false);
        }
    }

    private void skipBy(double seconds) {
        if (mPlayer == null) {
            return;
        }
        double target = mPlayer.getPositionSeconds() + seconds;
        if (target < 0) {
            target = 0;
        }
        double total = mPlayer.getDurationSeconds();
        if (total > 0 && target > total) {
            target = total;
        }
        mPlayer.seekSeconds(target);
        updateProgressFromPlayer();
    }

    private void stopPlayback() {
        stopProgressTimer();
        if (mPlayer != null) {
            mPlayer.stop();
        }
        if (mSeekSlider != null) {
            mSeekSlider.setValue(0);
        }
        if (mCurrentTimeLabel != null) {
            mCurrentTimeLabel.setText("0:00");
        }
        if (mPlayPauseButton != null) {
            mPlayPauseButton.setText("▶");
        }
        if (mStopButton != null) {
            mStopButton.setDisable(true);
        }
    }

    private void disposePlayer() {
        stopProgressTimer();
        if (mPlayer != null) {
            mPlayer.dispose();
            mPlayer = null;
        }
        if (mStopButton != null) {
            mStopButton.setDisable(true);
        }
    }

    private void showPlayerBar(boolean show) {
        if (mPlayerBar != null) {
            mPlayerBar.setVisible(show);
            mPlayerBar.setManaged(show);
        }
    }

    private static String formatTime(Duration duration) {
        if (duration == null || duration.isUnknown() || duration.isIndefinite()) {
            return "0:00";
        }
        long totalSeconds = (long) Math.floor(duration.toSeconds());
        return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
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

