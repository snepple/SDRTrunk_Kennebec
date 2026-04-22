package io.github.dsheirer.gui.recordings;

import io.github.dsheirer.gui.JavaFxWindowManager;
import io.github.dsheirer.preference.UserPreferences;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.embed.swing.JFXPanel;
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

import javax.swing.*;
import java.awt.BorderLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.LocalTime;
import java.time.LocalDateTime;

public class AudioRecordingsPanel extends JPanel {
    private final static Logger mLog = LoggerFactory.getLogger(AudioRecordingsPanel.class);
    private UserPreferences mUserPreferences;
    private JFXPanel mJfxPanel;

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

    private io.github.dsheirer.playlist.PlaylistManager mPlaylistManager;

    public AudioRecordingsPanel(UserPreferences userPreferences, io.github.dsheirer.playlist.PlaylistManager playlistManager) {
        mPlaylistManager = playlistManager;
        mUserPreferences = userPreferences;
        setLayout(new BorderLayout());

        mJfxPanel = new JFXPanel();
        add(mJfxPanel, BorderLayout.CENTER);

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

        mRecordings = FXCollections.observableArrayList();
        mFilteredRecordings = new FilteredList<>(mRecordings, p -> true);

        // Top Filter Bar
        javafx.scene.layout.FlowPane filterBox = new javafx.scene.layout.FlowPane(10, 10);
        filterBox.setPadding(new Insets(0, 0, 10, 0));

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
        refreshButton.setOnAction(e -> loadRecordings());

        mStopButton = new Button("Stop Playback");
        mStopButton.setDisable(true);
        mStopButton.setOnAction(e -> stopPlayback());

        filterBox.getChildren().addAll(
            new Label("Filters:"),
            mStartDatePicker, new Label("-"), mEndDatePicker,
            new Label(" Time:"), mStartHourSpinner, new Label(":"), mStartMinuteSpinner, new Label("-"), mEndHourSpinner, new Label(":"), mEndMinuteSpinner,
            mAliasComboBox, mChannelComboBox,
            refreshButton, mStopButton
        );

        root.setTop(filterBox);

        // Table
        mTableView = new TableView<>();
        HBox.setHgrow(mTableView, Priority.ALWAYS);

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

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/sdrtrunk_style.css").toExternalForm());
        mJfxPanel.setScene(scene);

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
                stream.filter(Files::isRegularFile)
                      .filter(p -> {
                          String name = p.getFileName().toString().toLowerCase();
                          return name.endsWith(".wav") || name.endsWith(".mp3");
                      })
                      .forEach(p -> {
                          RecordingItem item = parseFilename(p);
                          Platform.runLater(() -> mRecordings.add(item));
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

        // Parse filename: YYYYMMDD_HHMMSS_SYSTEM_SITE_CHANNEL_TO_alias_FROM_alias...
        String withoutExt = name.substring(0, name.lastIndexOf('.'));
        String[] parts = withoutExt.split("_");

        if (parts.length >= 2) {
            String datePart = parts[0];
            if (datePart.length() == 8) {
                item.setDate(datePart.substring(0, 4) + "-" + datePart.substring(4, 6) + "-" + datePart.substring(6, 8));
            } else {
                item.setDate(datePart);
            }

            String timePart = parts[1];
            if (timePart.length() >= 6) {
                item.setTime(timePart.substring(0, 2) + ":" + timePart.substring(2, 4) + ":" + timePart.substring(4, 6));
            } else {
                item.setTime(timePart);
            }
        }

        int toIdx = withoutExt.indexOf("_TO_");
        int fromIdx = withoutExt.indexOf("_FROM_");

        String channelInfo = "";

        if (parts.length > 2) {
            int endChannelIdx = toIdx != -1 ? toIdx : (fromIdx != -1 ? fromIdx : withoutExt.length());
            // parts[0] is date, parts[1] is time. Length of date + time + 2 underscores
            int startChannelIdx = parts[0].length() + parts[1].length() + 2;
            if (endChannelIdx > startChannelIdx) {
                channelInfo = withoutExt.substring(startChannelIdx, endChannelIdx);
            }
        }
        item.setChannel(channelInfo.replace("_", " ").trim());

        if (toIdx != -1) {
            int toEnd = fromIdx != -1 ? fromIdx : withoutExt.length();
            // check if there's _TONES or something
            int toneIdx = withoutExt.indexOf("_TONES");
            if (toneIdx != -1 && toneIdx < toEnd) {
                toEnd = toneIdx;
            }
            item.setToAlias(withoutExt.substring(toIdx + 4, toEnd).replace("_", " ").trim());
        }

        if (fromIdx != -1) {
            int fromEnd = withoutExt.length();
            int toneIdx = withoutExt.indexOf("_TONES");
            if (toneIdx != -1 && toneIdx > fromIdx) {
                fromEnd = toneIdx;
            }
            item.setFromAlias(withoutExt.substring(fromIdx + 6, fromEnd).replace("_", " ").trim());
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
        mStopButton.setDisable(true);
    }
}
