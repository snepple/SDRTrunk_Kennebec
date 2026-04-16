package io.github.dsheirer.gui.playlist.twotone;

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
import javafx.util.converter.NumberStringConverter;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.net.URL;

public class TwoToneEditor extends VBox
{
    private final PlaylistManager mPlaylistManager;
    private TableView<TwoToneConfiguration> mTableView;
    private ObservableList<TwoToneConfiguration> mObservableConfigs;

    public TwoToneEditor(PlaylistManager playlistManager)
    {
        mPlaylistManager = playlistManager;
        setSpacing(10);
        setPadding(new Insets(10));

        mObservableConfigs = FXCollections.observableArrayList(TwoToneConfiguration.extractor());
        if (playlistManager.getCurrentPlaylist() != null) {
            mObservableConfigs.addAll(playlistManager.getCurrentPlaylist().getTwoToneConfigurations());
        }

        mTableView = new TableView<>(mObservableConfigs);

        TableColumn<TwoToneConfiguration, String> aliasCol = new TableColumn<>("Alias");
        aliasCol.setCellValueFactory(new PropertyValueFactory<>("alias"));
        TableColumn<TwoToneConfiguration, TwoToneConfiguration.SequenceType> sequenceTypeCol = new TableColumn<>("Sequence Type");
        sequenceTypeCol.setCellValueFactory(new PropertyValueFactory<>("sequenceType"));
        TableColumn<TwoToneConfiguration, Double> toneACol = new TableColumn<>("Tone A");
        toneACol.setCellValueFactory(new PropertyValueFactory<>("toneA"));
        TableColumn<TwoToneConfiguration, Double> toneBCol = new TableColumn<>("Tone B");
        toneBCol.setCellValueFactory(new PropertyValueFactory<>("toneB"));
        TableColumn<TwoToneConfiguration, Boolean> mqttCol = new TableColumn<>("MQTT Enabled");
        mqttCol.setCellValueFactory(new PropertyValueFactory<>("enableMqttPublish"));

        mTableView.getColumns().addAll(aliasCol, sequenceTypeCol, toneACol, toneBCol, mqttCol);
        VBox.setVgrow(mTableView, Priority.ALWAYS);

        GridPane editorGrid = new GridPane();
        editorGrid.setHgap(10);
        editorGrid.setVgap(5);

        TextField aliasField = new TextField();
        ComboBox<TwoToneConfiguration.SequenceType> sequenceTypeCombo = new ComboBox<>();
        sequenceTypeCombo.getItems().addAll(TwoToneConfiguration.SequenceType.values());
        TextField toneAField = new TextField();
        TextField toneBField = new TextField();
        TextField zelloField = new TextField();

        CheckBox mqttCheck = new CheckBox("Enable MQTT Publish");
        TextField topicField = new TextField();
        TextArea payloadArea = new TextArea();
        payloadArea.setPrefRowCount(3);

        topicField.disableProperty().bind(mqttCheck.selectedProperty().not());
        payloadArea.disableProperty().bind(mqttCheck.selectedProperty().not());

        editorGrid.add(new Label("Alias:"), 0, 0);
        editorGrid.add(aliasField, 1, 0);
        editorGrid.add(new Label("Sequence Type:"), 0, 1);
        editorGrid.add(sequenceTypeCombo, 1, 1);
        editorGrid.add(new Label("Tone A:"), 0, 2);
        editorGrid.add(toneAField, 1, 2);
        editorGrid.add(new Label("Tone B:"), 0, 3);
        editorGrid.add(toneBField, 1, 3);
        editorGrid.add(new Label("Zello Channel:"), 0, 4);
        editorGrid.add(zelloField, 1, 4);

        editorGrid.add(mqttCheck, 2, 0, 2, 1);
        editorGrid.add(new Label("MQTT Topic:"), 2, 1);
        editorGrid.add(topicField, 3, 1);
        editorGrid.add(new Label("MQTT Payload:"), 2, 2);
        editorGrid.add(payloadArea, 3, 2, 1, 2);

        CheckBox zelloAlertCheck = new CheckBox("Enable Zello Alert Tone");
        ComboBox<String> alertToneCombo = new ComboBox<>();
        alertToneCombo.getItems().addAll("alert1.wav", "alert2.wav");

        Button previewBtn = new Button("Preview");
        previewBtn.setOnAction(ev -> {
            String selectedFile = alertToneCombo.getValue();
            if (selectedFile != null && !selectedFile.isEmpty()) {
                try {
                    URL resource = TwoToneEditor.class.getResource("/audio/" + selectedFile);
                    if (resource != null) {
                        AudioInputStream ais = AudioSystem.getAudioInputStream(resource);
                        Clip clip = AudioSystem.getClip();
                        clip.open(ais);
                        clip.start();
                    } else {
                        System.err.println("Could not find audio resource: /audio/" + selectedFile);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        alertToneCombo.disableProperty().bind(zelloAlertCheck.selectedProperty().not());
        previewBtn.disableProperty().bind(zelloAlertCheck.selectedProperty().not());

        editorGrid.add(zelloAlertCheck, 0, 5, 2, 1);
        editorGrid.add(new Label("Alert Tone File:"), 0, 6);
        editorGrid.add(alertToneCombo, 1, 6);
        editorGrid.add(previewBtn, 2, 6);

        toneBField.disableProperty().bind(sequenceTypeCombo.valueProperty().isEqualTo(TwoToneConfiguration.SequenceType.LONG_A));

        mTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (oldVal != null) {
                aliasField.textProperty().unbindBidirectional(oldVal.aliasProperty());
                sequenceTypeCombo.valueProperty().unbindBidirectional(oldVal.sequenceTypeProperty());
                zelloField.textProperty().unbindBidirectional(oldVal.zelloChannelProperty());
                mqttCheck.selectedProperty().unbindBidirectional(oldVal.enableMqttPublishProperty());
                topicField.textProperty().unbindBidirectional(oldVal.mqttTopicProperty());
                payloadArea.textProperty().unbindBidirectional(oldVal.mqttPayloadProperty());
                zelloAlertCheck.selectedProperty().unbindBidirectional(oldVal.enableZelloAlertProperty());
                alertToneCombo.valueProperty().unbindBidirectional(oldVal.zelloAlertFileProperty());

                oldVal.setToneA(toneAField.getText().isEmpty() ? 0 : Double.parseDouble(toneAField.getText()));
                oldVal.setToneB(toneBField.getText().isEmpty() ? 0 : Double.parseDouble(toneBField.getText()));
            }
            if (newVal != null) {
                aliasField.textProperty().bindBidirectional(newVal.aliasProperty());
                sequenceTypeCombo.valueProperty().bindBidirectional(newVal.sequenceTypeProperty());
                toneAField.setText(String.valueOf(newVal.getToneA()));
                toneBField.setText(String.valueOf(newVal.getToneB()));
                zelloField.textProperty().bindBidirectional(newVal.zelloChannelProperty());
                mqttCheck.selectedProperty().bindBidirectional(newVal.enableMqttPublishProperty());
                topicField.textProperty().bindBidirectional(newVal.mqttTopicProperty());
                payloadArea.textProperty().bindBidirectional(newVal.mqttPayloadProperty());
                zelloAlertCheck.selectedProperty().bindBidirectional(newVal.enableZelloAlertProperty());
                alertToneCombo.valueProperty().bindBidirectional(newVal.zelloAlertFileProperty());
            } else {
                aliasField.clear();
                sequenceTypeCombo.getSelectionModel().clearSelection();
                toneAField.clear();
                toneBField.clear();
                zelloField.clear();
                mqttCheck.setSelected(false);
                topicField.clear();
                payloadArea.clear();
                zelloAlertCheck.setSelected(false);
                alertToneCombo.getSelectionModel().clearSelection();
            }
        });

        // Basic double conversion listener
        toneAField.textProperty().addListener((obs, o, n) -> {
            TwoToneConfiguration sel = mTableView.getSelectionModel().getSelectedItem();
            if (sel != null && !n.isEmpty()) {
                try { sel.setToneA(Double.parseDouble(n)); } catch (Exception ignored) {}
            }
        });
        toneBField.textProperty().addListener((obs, o, n) -> {
            TwoToneConfiguration sel = mTableView.getSelectionModel().getSelectedItem();
            if (sel != null && !n.isEmpty()) {
                try { sel.setToneB(Double.parseDouble(n)); } catch (Exception ignored) {}
            }
        });

        HBox btnBox = new HBox(10);
        Button addBtn = new Button("Add");
        addBtn.setOnAction(e -> {
            TwoToneConfiguration conf = new TwoToneConfiguration();
            conf.setAlias("New Detector");
            mObservableConfigs.add(conf);
            syncToPlaylist();
            mTableView.getSelectionModel().select(conf);
        });

        Button delBtn = new Button("Delete");
        delBtn.setOnAction(e -> {
            TwoToneConfiguration sel = mTableView.getSelectionModel().getSelectedItem();
            if (sel != null) {
                mObservableConfigs.remove(sel);
                syncToPlaylist();
            }
        });
        btnBox.getChildren().addAll(addBtn, delBtn);

        getChildren().addAll(new Label("Two Tone Paging Detectors"), mTableView, editorGrid, btnBox);
    }

    private void syncToPlaylist() {
        if (mPlaylistManager.getCurrentPlaylist() != null) {
            mPlaylistManager.getCurrentPlaylist().setTwoToneConfigurations(mObservableConfigs);
            // PlaylistManager will listen to this change normally or we trigger a save if needed
        }
    }

    public void process(TwoToneTabRequest request)
    {
    }
}
