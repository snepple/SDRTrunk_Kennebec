package io.github.dsheirer.gui.playlist.twotone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;

import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.playlist.TwoToneConfiguration;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.application.Platform;
import javafx.util.converter.NumberStringConverter;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import io.github.dsheirer.audio.broadcast.BroadcastConfiguration;
import io.github.dsheirer.audio.broadcast.BroadcastServerType;
import javafx.collections.ListChangeListener;
import org.controlsfx.control.CheckComboBox;
import org.controlsfx.control.SegmentedButton;


import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class TwoToneEditor extends javafx.scene.layout.BorderPane
{
    private static final Logger mLog = LoggerFactory.getLogger(TwoToneEditor.class);
    public static final double[] MOTOROLA_QCII = {
            330.5, 349.0, 368.5, 389.0, 410.6, 433.7, 457.9, 483.5, 510.5, 539.0,
            569.1, 600.9, 634.5, 669.9, 707.3, 746.8, 788.5, 832.5, 879.0, 928.1,
            321.7, 339.6, 358.6, 378.6, 399.8, 422.1, 445.7, 470.5, 496.8, 1092.4,
            524.6, 553.9, 584.8, 617.4, 651.9, 688.3, 726.8, 767.4, 810.2, 855.5,
            903.2, 953.7, 1006.9, 1063.2, 1122.5, 1185.2, 1251.4, 1321.2, 1395.0, 1472.9,
            1555.2, 1642.4, 1733.7, 1830.5, 1930.2, 2036.0, 2149.2, 2268.6, 2395.0, 2528.5
    };

    public static final double[] PLECTRON = {
            282.2, 294.7, 307.8, 321.4, 335.6, 350.5, 366.0, 382.0, 399.2, 416.9,
            435.3, 454.6, 474.8, 495.8, 517.8, 540.7, 564.7, 589.7, 615.8, 643.0,
            672.0, 701.0, 732.0, 765.0, 799.0, 834.0, 871.0, 910.0, 950.0, 992.0,
            1036.0, 1082.0, 1130.0, 1180.0, 1232.0, 1287.0, 1344.0, 1403.0, 1465.0,
            1530.0, 1598.0, 1669.0, 1743.0, 1820.0, 1901.0, 1985.0, 2073.0, 2164.0,
            2260.0, 2361.0, 2465.0, 2575.0, 2688.0, 2807.0, 2932.0, 3062.0
    };

    private static final List<String> STANDARD_FREQUENCIES;

    public static final List<String> THINLINE_SOUNDS = java.util.Arrays.asList(
        "alert.wav", "Beep.mp3", "chirp_long.wav", "classic.wav", "Click.mp3", 
        "ding.wav", "door_bell.wav", "double_pulse.wav", "fast_beep_long.wav", 
        "fast_beep_short.wav", "five_beep.wav", "MDC-1200.mp3", "modern.wav", 
        "pluck.wav", "pop.wav", "quick_beep.wav", "quiet.wav", "relaxed.wav", 
        "settle_alert.wav", "simple.wav", "smoke_alarm.wav", "startup.wav", "tone.wav"
    );

    static {
        Set<Double> allFreqs = new TreeSet<>();
        for (double d : MOTOROLA_QCII) allFreqs.add(d);
        for (double d : PLECTRON) allFreqs.add(d);
        STANDARD_FREQUENCIES = allFreqs.stream().map(String::valueOf).collect(Collectors.toList());
    }

    private final PlaylistManager mPlaylistManager;
    private TableView<TwoToneConfiguration> mTableView;
    private ObservableList<TwoToneConfiguration> mObservableConfigs;
    private TwoToneAliasSelectionEditor mAliasEditor;
    public TwoToneEditor(PlaylistManager playlistManager)
    {
        mPlaylistManager = playlistManager;

        mObservableConfigs = FXCollections.observableArrayList(TwoToneConfiguration.extractor());
        if (playlistManager.getCurrentPlaylist() != null) {
            mObservableConfigs.addAll(playlistManager.getCurrentPlaylist().getTwoToneConfigurations());
        }

        FilteredList<TwoToneConfiguration> filteredConfigs = new FilteredList<>(mObservableConfigs, p -> true);
        SortedList<TwoToneConfiguration> sortedConfigs = new SortedList<>(filteredConfigs);
        
        mTableView = new TableView<>(sortedConfigs);
        mTableView.getStyleClass().add("preferences-table");
        sortedConfigs.comparatorProperty().bind(mTableView.comparatorProperty());
        mTableView.setPlaceholder(new Label("Click the New button to create a new Two Tone Detector"));

        TableColumn<TwoToneConfiguration, Boolean> enabledCol = new TableColumn<>("Enabled");
        enabledCol.setId("enabled");
        enabledCol.setCellValueFactory(new PropertyValueFactory<>("enabled"));


        TableColumn<TwoToneConfiguration, String> aliasCol = new TableColumn<>("Name");
        aliasCol.setId("alias");
        aliasCol.setCellValueFactory(new PropertyValueFactory<>("alias"));
        aliasCol.setCellFactory(column -> new TableCell<TwoToneConfiguration, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    TwoToneConfiguration config = getTableRow().getItem();
                    if (config != null && config.isAutoDiscovered() && !config.isEnabled()) {
                        setText(item + " (Auto-Discovered)");
                        setStyle("-fx-font-weight: bold; -fx-text-fill: darkorange;");
                    } else {
                        setText(item);
                        setStyle("");
                    }
                }
            }
        });
        TableColumn<TwoToneConfiguration, Double> toneACol = new TableColumn<>("Tone A");
        toneACol.setId("toneA");
        toneACol.setCellValueFactory(new PropertyValueFactory<>("toneA"));
        TableColumn<TwoToneConfiguration, Double> toneBCol = new TableColumn<>("Tone B");
        toneBCol.setId("toneB");
        toneBCol.setCellValueFactory(new PropertyValueFactory<>("toneB"));
        TableColumn<TwoToneConfiguration, Boolean> mqttCol = new TableColumn<>("MQTT Enabled");
        mqttCol.setId("mqtt");
        mqttCol.setCellValueFactory(new PropertyValueFactory<>("enableMqttPublish"));

        mTableView.getColumns().addAll(enabledCol, aliasCol, toneACol, toneBCol, mqttCol);
        mTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        mTableView.setTableMenuButtonVisible(true);
        new io.github.dsheirer.preference.javafx.FxTableColumnMonitor(mPlaylistManager.getUserPreferences(), mTableView, "twoToneTable");


        TextField aliasField = new TextField();
        ComboBox<String> typeSelector = new ComboBox<>();
        typeSelector.getItems().addAll("A/B Tones", "Long A Tone Only");
        typeSelector.getSelectionModel().select(0);
        ComboBox<String> toneAField = new ComboBox<>();
        toneAField.setEditable(true);
        toneAField.getItems().addAll(STANDARD_FREQUENCIES);
        ComboBox<String> toneBField = new ComboBox<>();
        toneBField.setEditable(true);
        toneBField.getItems().addAll(STANDARD_FREQUENCIES);

        javafx.beans.value.ChangeListener<String> filterListenerA = (obs, oldValue, newValue) -> {
            if (newValue == null || !toneAField.isFocused()) return;
            List<String> filtered = STANDARD_FREQUENCIES.stream()
                    .filter(f -> f.startsWith(newValue))
                    .collect(Collectors.toList());
            Platform.runLater(() -> {
                if (filtered.equals(toneAField.getItems())) return;
                String text = toneAField.getEditor().getText();
                int caret = toneAField.getEditor().getCaretPosition();
                toneAField.getItems().setAll(filtered);
                toneAField.getEditor().setText(text);
                toneAField.getEditor().positionCaret(caret);
                if (!filtered.isEmpty() && toneAField.isFocused()) toneAField.show();
            });
        };
        toneAField.getEditor().textProperty().addListener(filterListenerA);
        toneAField.focusedProperty().addListener((obs, o, n) -> {
            if (n) {
                toneAField.getItems().setAll(STANDARD_FREQUENCIES);
            }
        });

        javafx.beans.value.ChangeListener<String> filterListenerB = (obs, oldValue, newValue) -> {
            if (newValue == null || !toneBField.isFocused()) return;
            List<String> filtered = STANDARD_FREQUENCIES.stream()
                    .filter(f -> f.startsWith(newValue))
                    .collect(Collectors.toList());
            Platform.runLater(() -> {
                if (filtered.equals(toneBField.getItems())) return;
                String text = toneBField.getEditor().getText();
                int caret = toneBField.getEditor().getCaretPosition();
                toneBField.getItems().setAll(filtered);
                toneBField.getEditor().setText(text);
                toneBField.getEditor().positionCaret(caret);
                if (!filtered.isEmpty() && toneBField.isFocused()) toneBField.show();
            });
        };
        toneBField.getEditor().textProperty().addListener(filterListenerB);
        toneBField.focusedProperty().addListener((obs, o, n) -> {
            if (n) {
                toneBField.getItems().setAll(STANDARD_FREQUENCIES);
            }
        });

        //Multi-select of Zello channels (broadcast stream names). A detector can alert one or more Zello channels.
        CheckComboBox<String> zelloField = new CheckComboBox<>();
        zelloField.setMaxWidth(Double.MAX_VALUE);
        for (BroadcastConfiguration bc : mPlaylistManager.getBroadcastModel().getBroadcastConfigurations()) {
            if (bc.getBroadcastServerType() == BroadcastServerType.ZELLO_WORK || bc.getBroadcastServerType() == BroadcastServerType.ZELLO) {
                if (bc.getName() != null && !zelloField.getItems().contains(bc.getName())) {
                    zelloField.getItems().add(bc.getName());
                }
            }
        }
        //Guard so programmatic check updates (when switching detectors) don't write back to the model.
        final boolean[] updatingZello = {false};

        CheckBox mqttCheck = new CheckBox("Enable MQTT Publish");
        TextField topicField = new TextField();
        TextArea payloadArea = new TextArea();
        payloadArea.setPrefRowCount(3);

        topicField.disableProperty().bind(mqttCheck.selectedProperty().not());
        payloadArea.disableProperty().bind(mqttCheck.selectedProperty().not());

        CheckBox enabledCheck = new CheckBox("Enable Detector");
        CheckBox showNotificationCheck = new CheckBox("Show Visual Notification");
        
        ComboBox<String> alertFileCombo = new ComboBox<>();
        alertFileCombo.setEditable(true);
        alertFileCombo.getItems().add("");
        alertFileCombo.getItems().addAll(THINLINE_SOUNDS);
        alertFileCombo.getItems().add("Custom File...");

        Button browseAlertBtn = new Button("Browse...");
        browseAlertBtn.setOnAction(e -> {
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.setTitle("Select Alert Audio File");
            java.io.File file = fc.showOpenDialog(null);
            if (file != null) {
                alertFileCombo.setValue(file.getAbsolutePath());
            }
        });

        alertFileCombo.setOnAction(e -> {
            if ("Custom File...".equals(alertFileCombo.getValue())) {
                browseAlertBtn.fire();
            }
        });

        Button previewLocalAlertBtn = new Button("Preview");
        HBox alertFileBox = new HBox(5, alertFileCombo, browseAlertBtn, previewLocalAlertBtn);
        HBox.setHgrow(alertFileCombo, Priority.ALWAYS);

        GridPane generalGrid = new GridPane();
        generalGrid.setHgap(10);
        generalGrid.setVgap(8);
        generalGrid.add(enabledCheck, 0, 0, 2, 1);
        generalGrid.add(new Label("Name:"), 0, 1);
        generalGrid.add(aliasField, 1, 1);
        generalGrid.add(new Label("Type:"), 0, 2);
        generalGrid.add(typeSelector, 1, 2);
        generalGrid.add(new Label("Tone A:"), 0, 3);
        generalGrid.add(toneAField, 1, 3);
        Label toneBLabel = new Label("Tone B:");
        generalGrid.add(toneBLabel, 0, 4);
        generalGrid.add(toneBField, 1, 4);

        Spinner<Double> toneALengthSpinner = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.1, 10.0, 0.6, 0.1));
        toneALengthSpinner.setEditable(true);
        toneALengthSpinner.setPrefWidth(120);

        Spinner<Double> toneBLengthSpinner = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.1, 10.0, 0.6, 0.1));
        toneBLengthSpinner.setEditable(true);
        toneBLengthSpinner.setPrefWidth(120);

        Spinner<Double> toneGapLengthSpinner = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 5.0, 0.0, 0.05));
        toneGapLengthSpinner.setEditable(true);
        toneGapLengthSpinner.setPrefWidth(120);

        Spinner<Double> toneToleranceSpinner = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.001, 0.10, 0.02, 0.01));
        toneToleranceSpinner.setEditable(true);
        toneToleranceSpinner.setPrefWidth(120);
        Label toneToleranceHelp = new Label("(0.02 = 2%)");
        toneToleranceHelp.getStyleClass().add("hig-inline-help");
        toneToleranceSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                toneToleranceHelp.setText(String.format("(%.3f = %.1f%%)", newVal, newVal * 100.0));
            }
        });

        Spinner<Double> ignoreDuplicateSpinner = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 300, 60, 5));
        ignoreDuplicateSpinner.setEditable(true);
        ignoreDuplicateSpinner.setPrefWidth(120);

        //Editable JavaFX spinners do NOT commit text typed into the editor to the spinner value (and therefore to any
        //bound model property) unless the user presses Enter.  Without this, edits to the tone length / gap /
        //tolerance / ignore-duplicate fields appeared to take but were lost when switching detectors, because the
        //bound TwoToneConfiguration property was never updated.  This helper parses and commits the editor text
        //(clamped to the factory range); it is invoked on focus loss below and again before switching the selected
        //detector so in-progress edits are always persisted.
        java.util.function.Consumer<Spinner<Double>> commitSpinner = sp -> {
            if(sp == null || sp.getEditor() == null || sp.getValueFactory() == null) return;
            String text = sp.getEditor().getText();
            if(text == null || text.isBlank()) return;
            try {
                double value = Double.parseDouble(text.trim());
                if(sp.getValueFactory() instanceof SpinnerValueFactory.DoubleSpinnerValueFactory df) {
                    value = Math.max(df.getMin(), Math.min(df.getMax(), value));
                }
                sp.getValueFactory().setValue(value);
            } catch(NumberFormatException nfe) {
                //Ignore invalid input; keep the last committed value.
            }
        };

        for(Spinner<Double> sp : java.util.List.of(toneALengthSpinner, toneBLengthSpinner, toneGapLengthSpinner,
                toneToleranceSpinner, ignoreDuplicateSpinner)) {
            sp.focusedProperty().addListener((o, was, isNow) -> { if(!isNow) commitSpinner.accept(sp); });
        }

        generalGrid.add(new Label("A Tone Length (sec):"), 0, 5);
        generalGrid.add(toneALengthSpinner, 1, 5);

        Label toneBLengthLabel = new Label("B Tone Length (sec):");
        generalGrid.add(toneBLengthLabel, 0, 6);
        generalGrid.add(toneBLengthSpinner, 1, 6);

        Label toneGapLabel = new Label("Tone Gap Length (sec):");
        generalGrid.add(toneGapLabel, 0, 7);
        generalGrid.add(toneGapLengthSpinner, 1, 7);

        generalGrid.add(new Label("Tone Tolerance:"), 0, 8);
        HBox toneToleranceBox = new HBox(10, toneToleranceSpinner, toneToleranceHelp);
        generalGrid.add(toneToleranceBox, 1, 8);

        generalGrid.add(new Label("Ignore Duplicate (sec):"), 0, 9);
        generalGrid.add(ignoreDuplicateSpinner, 1, 9);

        generalGrid.add(new Label("Local Alert Audio:"), 0, 10);
        generalGrid.add(alertFileBox, 1, 10);
        generalGrid.add(showNotificationCheck, 0, 11, 2, 1);

        toneBLabel.disableProperty().bind(Bindings.equal(typeSelector.valueProperty(), "Long A Tone Only"));
        toneBField.disableProperty().bind(Bindings.equal(typeSelector.valueProperty(), "Long A Tone Only"));
        toneBLengthLabel.disableProperty().bind(Bindings.equal(typeSelector.valueProperty(), "Long A Tone Only"));
        toneBLengthSpinner.disableProperty().bind(Bindings.equal(typeSelector.valueProperty(), "Long A Tone Only"));
        toneGapLabel.disableProperty().bind(Bindings.equal(typeSelector.valueProperty(), "Long A Tone Only"));
        toneGapLengthSpinner.disableProperty().bind(Bindings.equal(typeSelector.valueProperty(), "Long A Tone Only"));

        typeSelector.valueProperty().addListener((obs, oldVal, newVal) -> {
            TwoToneConfiguration sel = mTableView.getSelectionModel().getSelectedItem();
            if (sel != null && newVal != null) {
                boolean isLong = newVal.equals("Long A Tone Only");
                sel.setLongATone(isLong);
                if (isLong) {
                    toneBField.getEditor().clear();
                    sel.setToneB(0.0);
                }
            }
        });

        CheckBox textMessageCheck = new CheckBox("Enable Text Message");
        Label textMessageInfo = new Label("Messages are sent to the Zello Channel.");
        textMessageInfo.getStyleClass().add("hig-inline-help");
        Label fieldsInfo = new Label("Available Fields: {Alias}, {Channel Name}, {Frequency}, {Timestamp}");
        fieldsInfo.getStyleClass().add("hig-inline-help");

        TextField templateField = new TextField();

        HBox previewBox = new HBox(5);
        Label previewLabel = new Label("Preview:");
        Label previewText = new Label();
        previewText.setStyle("-fx-font-style: italic;");
        previewBox.getChildren().addAll(previewLabel, previewText);

        Runnable updatePreview = () -> {
            String template = templateField.getText() != null && !templateField.getText().isEmpty() ? templateField.getText() : "Dispatch Received: {Alias}";
            String alias = aliasField.getText() != null && !aliasField.getText().isEmpty() ? aliasField.getText() : "Unknown";
            List<String> checkedChannels = zelloField.getCheckModel().getCheckedItems();
            String channel = (checkedChannels != null && !checkedChannels.isEmpty()) ? checkedChannels.get(0) : "Unknown";
            String freq = "154.145";
            String timestamp = String.valueOf(System.currentTimeMillis());

            String preview = template.replace("%ALIAS%", alias)
                                     .replace("{Alias}", alias)
                                     .replace("{Channel Name}", channel)
                                     .replace("{Frequency}", freq)
                                     .replace("{Timestamp}", timestamp);
            previewText.setText(preview);
        };

        CheckBox zelloAlertCheck = new CheckBox("Enable Zello Alert Tone");
        ComboBox<String> alertToneCombo = new ComboBox<>();
        alertToneCombo.getItems().addAll(
            "FireCallChirp1.wav",
            "Siren1.wav",
            "Siren2.wav",
            "Siren3.wav",
            "Siren4.wav",
            "Siren5.wav",
            "Siren6.wav",
            "ToneEMSCall1.wav",
            "ToneFireCall1.wav",
            "alert1.wav",
            "alert2.wav",
            "fire_pager.mp3",
            "medical_pager.mp3",
            "minitor_v_alert_tone.mp3"
        );
        alertToneCombo.getItems().addAll(THINLINE_SOUNDS);

        java.util.function.BiConsumer<String, Button> playPreview = (selectedFile, btn) -> {
            if (selectedFile != null && !selectedFile.isEmpty() && !"Custom File...".equals(selectedFile)) {
                try {
                    URL resource = null;
                    if (THINLINE_SOUNDS.contains(selectedFile)) {
                        resource = TwoToneEditor.class.getResource("/audio/thinline/" + selectedFile);
                    } else if (!selectedFile.contains("\\") && !selectedFile.contains("/") && !selectedFile.contains(":")) {
                        resource = TwoToneEditor.class.getResource("/audio/" + selectedFile);
                    } else {
                        java.io.File f = new java.io.File(selectedFile);
                        if (f.exists()) resource = f.toURI().toURL();
                    }

                    if (resource != null) {
                        if (selectedFile.toLowerCase().endsWith(".mp3")) {
                            javafx.scene.media.Media media = new javafx.scene.media.Media(resource.toURI().toString());
                            javafx.scene.media.MediaPlayer mediaPlayer = new javafx.scene.media.MediaPlayer(media);
                            btn.getProperties().put("mediaPlayer", mediaPlayer);
                            mediaPlayer.play();
                        } else {
                            AudioInputStream ais = AudioSystem.getAudioInputStream(resource);
                            Clip clip = AudioSystem.getClip();
                            clip.open(ais);
                            clip.start();
                        }
                    } else {
                        System.err.println("Could not find audio resource: " + selectedFile);
                    }
                } catch (Exception ex) {
                    mLog.error("Error playing audio sample", ex);
                }
            }
        };

        Button previewBtn = new Button("Preview");
        previewBtn.setOnAction(ev -> playPreview.accept(alertToneCombo.getValue(), previewBtn));
        previewLocalAlertBtn.setOnAction(ev -> playPreview.accept(alertFileCombo.getValue(), previewLocalAlertBtn));

        alertToneCombo.disableProperty().bind(zelloAlertCheck.selectedProperty().not());
        previewBtn.disableProperty().bind(zelloAlertCheck.selectedProperty().not());

        templateField.disableProperty().bind(textMessageCheck.selectedProperty().not());
        previewBox.visibleProperty().bind(textMessageCheck.selectedProperty());
        previewBox.managedProperty().bind(textMessageCheck.selectedProperty());
        fieldsInfo.visibleProperty().bind(textMessageCheck.selectedProperty());
        fieldsInfo.managedProperty().bind(textMessageCheck.selectedProperty());

        GridPane zelloGrid = new GridPane();
        zelloGrid.setHgap(10);
        zelloGrid.setVgap(8);
        zelloGrid.add(new Label("Zello Channel(s):"), 0, 0);
        zelloGrid.add(zelloField, 1, 0);
        zelloGrid.add(textMessageCheck, 0, 1);
        zelloGrid.add(textMessageInfo, 1, 1);
        zelloGrid.add(new Label("Message Template:"), 0, 2);
        zelloGrid.add(templateField, 1, 2);
        zelloGrid.add(fieldsInfo, 1, 3);
        zelloGrid.add(previewBox, 1, 4);
        zelloGrid.add(zelloAlertCheck, 0, 5, 2, 1);
        zelloGrid.add(new Label("Alert Tone File:"), 0, 6);
        zelloGrid.add(alertToneCombo, 1, 6);
        zelloGrid.add(previewBtn, 2, 6);

        GridPane mqttGrid = new GridPane();
        mqttGrid.setHgap(10);
        mqttGrid.setVgap(8);
        mqttGrid.add(mqttCheck, 0, 0, 2, 1);
        mqttGrid.add(new Label("MQTT Topic:"), 0, 1);
        mqttGrid.add(topicField, 1, 1);
        mqttGrid.add(new Label("MQTT Payload:"), 0, 2);
        mqttGrid.add(payloadArea, 1, 2);

        //Detection History tab: a per-detector, human-readable list of the date/time of every two-tone hit recorded
        //for the selected detector (most recent first).  Declared here, before the selection listener, so the listener
        //can refresh it whenever the selected detector changes.
        ListView<String> historyList = new ListView<>();
        historyList.setPlaceholder(new Label("No detections recorded yet for this detector."));
        Label historyCountLabel = new Label();
        Button refreshHistoryBtn = new Button("Refresh");
        refreshHistoryBtn.getStyleClass().add("kennebec-toolbar-button");
        Button clearHistoryBtn = new Button("Clear History");
        clearHistoryBtn.getStyleClass().add("kennebec-toolbar-button");

        final java.time.format.DateTimeFormatter historyFormatter = java.time.format.DateTimeFormatter
                .ofPattern("EEE MM/dd/yyyy hh:mm:ss a").withZone(java.time.ZoneId.systemDefault());

        Runnable refreshHistory = () -> {
            TwoToneConfiguration sel = mTableView.getSelectionModel().getSelectedItem();
            historyList.getItems().clear();
            if(sel != null) {
                //Snapshot the synchronized list before iterating (it may be appended from the detector thread).
                List<Long> snapshot = new java.util.ArrayList<>(sel.getDetectionHistory());
                for(int i = 0; i < snapshot.size(); i++) {
                    Long ts = snapshot.get(i);
                    if(ts != null) {
                        String when = historyFormatter.format(java.time.Instant.ofEpochMilli(ts));
                        String channel = sel.getDetectionChannel(i);
                        historyList.getItems().add((channel != null && !channel.isEmpty())
                                ? when + "  —  " + channel : when);
                    }
                }
                historyCountLabel.setText(snapshot.size() + (snapshot.size() == 1 ? " detection" : " detections"));
            } else {
                historyCountLabel.setText("");
            }
        };

        refreshHistoryBtn.setOnAction(e -> refreshHistory.run());
        clearHistoryBtn.setOnAction(e -> {
            TwoToneConfiguration sel = mTableView.getSelectionModel().getSelectedItem();
            if(sel != null) {
                sel.clearDetectionHistory();
                refreshHistory.run();
                mPlaylistManager.schedulePlaylistSave();
            }
        });

        SplitPane centerSplitPane = new SplitPane();
        VBox rightPane = new VBox(10);
        rightPane.setPadding(new Insets(10));
        mTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if(mAliasEditor != null) mAliasEditor.setTwoToneConfiguration(newVal);

            if (oldVal != null) {
                //Flush any in-progress spinner text edits to the model (via the active bindings) before unbinding,
                //so switching detectors does not discard typed tone-length / tolerance / gap / ignore values.
                commitSpinner.accept(toneALengthSpinner);
                commitSpinner.accept(toneBLengthSpinner);
                commitSpinner.accept(toneGapLengthSpinner);
                commitSpinner.accept(toneToleranceSpinner);
                commitSpinner.accept(ignoreDuplicateSpinner);

                aliasField.textProperty().unbindBidirectional(oldVal.aliasProperty());
                mqttCheck.selectedProperty().unbindBidirectional(oldVal.enableMqttPublishProperty());
                topicField.textProperty().unbindBidirectional(oldVal.mqttTopicProperty());
                payloadArea.textProperty().unbindBidirectional(oldVal.mqttPayloadProperty());
                zelloAlertCheck.selectedProperty().unbindBidirectional(oldVal.enableZelloAlertProperty());
                alertToneCombo.valueProperty().unbindBidirectional(oldVal.zelloAlertFileProperty());
                templateField.textProperty().unbindBidirectional(oldVal.templateProperty());
                textMessageCheck.selectedProperty().unbindBidirectional(oldVal.enableZelloTextMessageProperty());
                toneALengthSpinner.getValueFactory().valueProperty().unbindBidirectional(oldVal.toneALengthSecProperty().asObject());
                toneBLengthSpinner.getValueFactory().valueProperty().unbindBidirectional(oldVal.toneBLengthSecProperty().asObject());
                toneGapLengthSpinner.getValueFactory().valueProperty().unbindBidirectional(oldVal.toneGapLengthSecProperty().asObject());
                toneToleranceSpinner.getValueFactory().valueProperty().unbindBidirectional(oldVal.toneToleranceProperty().asObject());
                ignoreDuplicateSpinner.getValueFactory().valueProperty().unbindBidirectional(oldVal.ignoreDuplicateSecProperty().asObject());
                enabledCheck.selectedProperty().unbindBidirectional(oldVal.enabledProperty());
                alertFileCombo.valueProperty().unbindBidirectional(oldVal.alertFilePathProperty());
                showNotificationCheck.selectedProperty().unbindBidirectional(oldVal.showNotificationProperty());

                try {
                    oldVal.setToneA(toneAField.getEditor().getText().isEmpty() ? 0 : Double.parseDouble(toneAField.getEditor().getText()));
                } catch (NumberFormatException e) {
                    oldVal.setToneA(0.0);
                }
                try {
                    oldVal.setToneB(toneBField.getEditor().getText().isEmpty() ? 0 : Double.parseDouble(toneBField.getEditor().getText()));
                } catch (NumberFormatException e) {
                    oldVal.setToneB(0.0);
                }
            }
            if (newVal != null) {
                aliasField.textProperty().bindBidirectional(newVal.aliasProperty());
                if (newVal.isLongATone()) {
                    typeSelector.getSelectionModel().select("Long A Tone Only");
                } else {
                    typeSelector.getSelectionModel().select("A/B Tones");
                }
                toneAField.getEditor().setText(String.valueOf(newVal.getToneA()));
                toneAField.setValue(String.valueOf(newVal.getToneA()));
                if (newVal.getToneB() == 0.0) {
                    toneBField.getEditor().setText("");
                    toneBField.setValue(null);
                } else {
                    toneBField.getEditor().setText(String.valueOf(newVal.getToneB()));
                    toneBField.setValue(String.valueOf(newVal.getToneB()));
                }
                //Reflect the detector's selected Zello channels in the multi-select control (guarded so this does not
                //write back to the model).
                updatingZello[0] = true;
                zelloField.getCheckModel().clearChecks();
                for (String channel : newVal.getEffectiveZelloChannels()) {
                    if (!zelloField.getItems().contains(channel)) {
                        zelloField.getItems().add(channel);
                    }
                    zelloField.getCheckModel().check(channel);
                }
                updatingZello[0] = false;
                mqttCheck.selectedProperty().bindBidirectional(newVal.enableMqttPublishProperty());
                topicField.textProperty().bindBidirectional(newVal.mqttTopicProperty());
                payloadArea.textProperty().bindBidirectional(newVal.mqttPayloadProperty());
                zelloAlertCheck.selectedProperty().bindBidirectional(newVal.enableZelloAlertProperty());
                alertToneCombo.valueProperty().bindBidirectional(newVal.zelloAlertFileProperty());
                templateField.textProperty().bindBidirectional(newVal.templateProperty());
                textMessageCheck.selectedProperty().bindBidirectional(newVal.enableZelloTextMessageProperty());
                toneALengthSpinner.getValueFactory().setValue(newVal.getToneALengthSec());
                toneALengthSpinner.getValueFactory().valueProperty().bindBidirectional(newVal.toneALengthSecProperty().asObject());
                toneBLengthSpinner.getValueFactory().setValue(newVal.getToneBLengthSec());
                toneBLengthSpinner.getValueFactory().valueProperty().bindBidirectional(newVal.toneBLengthSecProperty().asObject());
                toneGapLengthSpinner.getValueFactory().setValue(newVal.getToneGapLengthSec());
                toneGapLengthSpinner.getValueFactory().valueProperty().bindBidirectional(newVal.toneGapLengthSecProperty().asObject());
                toneToleranceSpinner.getValueFactory().setValue(newVal.getToneTolerance());
                toneToleranceSpinner.getValueFactory().valueProperty().bindBidirectional(newVal.toneToleranceProperty().asObject());
                ignoreDuplicateSpinner.getValueFactory().setValue(newVal.getIgnoreDuplicateSec());
                ignoreDuplicateSpinner.getValueFactory().valueProperty().bindBidirectional(newVal.ignoreDuplicateSecProperty().asObject());
                enabledCheck.selectedProperty().bindBidirectional(newVal.enabledProperty());
                alertFileCombo.valueProperty().bindBidirectional(newVal.alertFilePathProperty());
                showNotificationCheck.selectedProperty().bindBidirectional(newVal.showNotificationProperty());
                if (!centerSplitPane.getItems().contains(rightPane)) {
                    centerSplitPane.getItems().add(rightPane);
                    centerSplitPane.setDividerPositions(0.4);
                }
            } else {
                aliasField.clear();
                toneAField.getEditor().clear();
                toneAField.setValue(null);
                toneBField.getEditor().clear();
                toneBField.setValue(null);
                typeSelector.getSelectionModel().select("A/B Tones");
                updatingZello[0] = true;
                zelloField.getCheckModel().clearChecks();
                updatingZello[0] = false;
                mqttCheck.setSelected(false);
                topicField.clear();
                payloadArea.clear();
                zelloAlertCheck.setSelected(false);
                alertToneCombo.getSelectionModel().clearSelection();
                templateField.clear();
                textMessageCheck.setSelected(false);
                toneALengthSpinner.getValueFactory().setValue(0.6);
                toneBLengthSpinner.getValueFactory().setValue(0.6);
                toneGapLengthSpinner.getValueFactory().setValue(0.0);
                toneToleranceSpinner.getValueFactory().setValue(0.02);
                ignoreDuplicateSpinner.getValueFactory().setValue(60.0);
                enabledCheck.setSelected(false);
                alertFileCombo.setValue(null);
                showNotificationCheck.setSelected(false);
                if (centerSplitPane.getItems().contains(rightPane)) {
                    centerSplitPane.getItems().remove(rightPane);
                }
            }
            updatePreview.run();
            refreshHistory.run();
        });

        // Basic double conversion listener
        aliasField.textProperty().addListener((obs, o, n) -> updatePreview.run());
        templateField.textProperty().addListener((obs, o, n) -> updatePreview.run());

        //Persist multi-channel Zello selections to the selected detector. The legacy single-channel field is kept in
        //sync (set to the first selected channel) for backward compatibility with older app versions.
        zelloField.getCheckModel().getCheckedItems().addListener((ListChangeListener<String>) c -> {
            updatePreview.run();
            if (updatingZello[0]) {
                return;
            }
            TwoToneConfiguration sel = mTableView.getSelectionModel().getSelectedItem();
            if (sel == null) {
                return;
            }
            List<String> checked = new java.util.ArrayList<>(zelloField.getCheckModel().getCheckedItems());
            sel.setZelloChannels(checked);
            sel.setZelloChannel(checked.isEmpty() ? null : checked.get(0));
        });

        toneAField.getEditor().textProperty().addListener((obs, o, n) -> {
            TwoToneConfiguration sel = mTableView.getSelectionModel().getSelectedItem();
            if (sel != null && !n.isEmpty()) {
                try { sel.setToneA(Double.parseDouble(n)); } catch (Exception ignored) {}
            }
        });
        toneBField.getEditor().textProperty().addListener((obs, o, n) -> {
            TwoToneConfiguration sel = mTableView.getSelectionModel().getSelectedItem();
            if (sel != null && !n.isEmpty()) {
                try { sel.setToneB(Double.parseDouble(n)); } catch (Exception ignored) {}
            }
        });


        // Left Pane (Master): Table + Buttons toolbar
        VBox leftPane = new VBox(5);
        leftPane.setPadding(new Insets(10));
        HBox.setHgrow(mTableView, Priority.ALWAYS);
        VBox.setVgrow(mTableView, Priority.ALWAYS);

        Button newBtn = new Button("New Detector");
        newBtn.getStyleClass().add("kennebec-toolbar-button-primary");
        newBtn.setOnAction(e -> {
            TwoToneConfiguration conf = new TwoToneConfiguration();
            conf.setAlias("New Detector");
            mObservableConfigs.add(conf);
            syncToPlaylist();
            mTableView.getSelectionModel().select(conf);
            mTableView.scrollTo(conf);
        });

        Button delBtn = new Button("Delete");
        delBtn.getStyleClass().add("kennebec-toolbar-button");
        delBtn.setOnAction(e -> {
            TwoToneConfiguration sel = mTableView.getSelectionModel().getSelectedItem();
            if (sel != null) {
                //When the user rejects/deletes an AI-discovered detector, record a tombstone so the AI never
                //regenerates that tone pair (soft-delete exclusion list).
                if (sel.isAutoDiscovered()) {
                    io.github.dsheirer.preference.ai.ToneDiscoveryManager manager =
                        io.github.dsheirer.preference.ai.ToneDiscoveryManager.getInstance();

                    if (manager != null) {
                        manager.recordTombstone(sel.getToneA(), sel.getToneB(), sel.getDiscoveryFrequency());
                    }
                }

                mObservableConfigs.remove(sel);
                syncToPlaylist();
            }
        });

        Button cloneBtn = new Button("Clone");
        cloneBtn.getStyleClass().add("kennebec-toolbar-button");
        cloneBtn.setOnAction(e -> {
            TwoToneConfiguration sel = mTableView.getSelectionModel().getSelectedItem();
            if (sel != null) {
                TwoToneConfiguration copy = sel.copyOf();
                copy.setAlias(copy.getAlias() + " (Copy)");
                mObservableConfigs.add(copy);
                syncToPlaylist();
                mTableView.getSelectionModel().select(copy);
                mTableView.scrollTo(copy);
            }
        });

        Button refreshBtn = new Button("Refresh");
        refreshBtn.getStyleClass().add("kennebec-toolbar-button");
        refreshBtn.setOnAction(e -> {
            mObservableConfigs.setAll(mPlaylistManager.getCurrentPlaylist().getTwoToneConfigurations());
        });

        ToggleButton allBtn = new ToggleButton("All");
        ToggleButton enabledBtn = new ToggleButton("Enabled Only");
        SegmentedButton filterSegmentedBtn = new SegmentedButton(allBtn, enabledBtn);
        filterSegmentedBtn.getStyleClass().add(SegmentedButton.STYLE_CLASS_DARK);
        
        ToggleGroup group = filterSegmentedBtn.getToggleGroup();
        allBtn.setSelected(true);
        group.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                oldVal.setSelected(true); // Don't allow deselection
            } else {
                filteredConfigs.setPredicate(config -> {
                    if (newVal == enabledBtn) return config.isEnabled();
                    return true; // All
                });
            }
        });

        HBox topToolbar = new HBox(10);
        topToolbar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        topToolbar.getStyleClass().addAll("kennebec-filter-toolbar");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        topToolbar.getChildren().addAll(filterSegmentedBtn, spacer, newBtn, cloneBtn, delBtn, refreshBtn);
        setTop(topToolbar);


        leftPane.getChildren().addAll(new Label("Two Tone Paging Detectors"), mTableView);

        // Right Pane (Detail): Configuration Tabs -> TitledPanes

        mAliasEditor = new TwoToneAliasSelectionEditor(mPlaylistManager);

        // Header bar with persistent Save button
        HBox detailHeader = new HBox(10);
        detailHeader.setPadding(new Insets(5, 5, 10, 5));
        detailHeader.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label detailTitle = new Label("Detector Settings");
        detailTitle.getStyleClass().add("kennebec-header");
        detailTitle.setStyle("-fx-font-size: 15px;");

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Button saveBtn = new Button("Save Settings");
        saveBtn.getStyleClass().add("kennebec-toolbar-button-primary");
        saveBtn.setOnAction(e -> {
            mPlaylistManager.schedulePlaylistSave();
            mTableView.refresh();
        });

        detailHeader.getChildren().addAll(detailTitle, headerSpacer, saveBtn);

        TabPane tabPane = new TabPane();
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        Tab generalTab = new Tab("General Setup");
        generalTab.setClosable(false);
        ScrollPane generalScroll = new ScrollPane(generalGrid);
        generalScroll.setFitToWidth(true);
        generalScroll.setStyle("-fx-background-color: transparent;");
        generalGrid.setPadding(new Insets(10));
        generalTab.setContent(generalScroll);

        Tab zelloTab = new Tab("Zello Integration");
        zelloTab.setClosable(false);
        ScrollPane zelloScroll = new ScrollPane(zelloGrid);
        zelloScroll.setFitToWidth(true);
        zelloScroll.setStyle("-fx-background-color: transparent;");
        zelloGrid.setPadding(new Insets(10));
        zelloTab.setContent(zelloScroll);

        Tab mqttTab = new Tab("MQTT Integration");
        mqttTab.setClosable(false);
        ScrollPane mqttScroll = new ScrollPane(mqttGrid);
        mqttScroll.setFitToWidth(true);
        mqttScroll.setStyle("-fx-background-color: transparent;");
        mqttGrid.setPadding(new Insets(10));
        mqttTab.setContent(mqttScroll);

        Tab aliasTab = new Tab("Aliases");
        aliasTab.setClosable(false);
        aliasTab.setContent(mAliasEditor);

        Tab historyTab = new Tab("Detection History");
        historyTab.setClosable(false);
        VBox historyBox = new VBox(8);
        historyBox.setPadding(new Insets(10));
        Label historyHelp = new Label("Date and time of each two-tone detection for this detector (most recent first).");
        historyHelp.getStyleClass().add("hig-inline-help");
        Region historySpacer = new Region();
        HBox.setHgrow(historySpacer, Priority.ALWAYS);
        HBox historyToolbar = new HBox(10, refreshHistoryBtn, clearHistoryBtn, historySpacer, historyCountLabel);
        historyToolbar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        VBox.setVgrow(historyList, Priority.ALWAYS);
        historyBox.getChildren().addAll(historyHelp, historyToolbar, historyList);
        historyTab.setContent(historyBox);

        tabPane.getTabs().addAll(generalTab, zelloTab, mqttTab, aliasTab, historyTab);

        //Refresh the history whenever the user opens the Detection History tab so newly recorded detections appear
        //without having to reselect the detector.
        tabPane.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> {
            if(nv == historyTab) {
                refreshHistory.run();
            }
        });

        rightPane.getChildren().addAll(detailHeader, tabPane);

        // Add to SplitPane
        centerSplitPane.getItems().add(leftPane);
        if (mTableView.getSelectionModel().getSelectedItem() != null) {
            centerSplitPane.getItems().add(rightPane);
            centerSplitPane.setDividerPositions(0.4);
        }
        setCenter(centerSplitPane);
    }
    private void syncToPlaylist() {
        //Write to the PlaylistManager's backing list (the source of truth). getCurrentPlaylist() returns a
        //fresh throwaway PlaylistV2 each call, so the previous getCurrentPlaylist().setTwoToneConfigurations()
        //updated a discarded object and nothing persisted - detectors vanished on refresh/navigation.
        mPlaylistManager.setTwoToneConfigurations(mObservableConfigs);
        mPlaylistManager.schedulePlaylistSave();
    }

    public void process(TwoToneTabRequest request)
    {
    }
}
