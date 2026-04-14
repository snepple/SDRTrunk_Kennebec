import sys

with open('src/main/java/io/github/dsheirer/gui/playlist/twotone/TwoToneEditor.java', 'r') as f:
    content = f.read()

# From master we see this code was supposed to be there:
# TextField topicField = new TextField();
# TextArea payloadArea = new TextArea();
# payloadArea.setPrefRowCount(3);
# ...
# And the listener was using:
# payloadArea.textProperty().bindBidirectional(...)

# Our code changed `payloadArea` (TextArea) to `messageField` (TextField) and messed up the binding blocks. Let's strictly follow master's layout except we will add our CheckBox and ComboBox underneath.

start = content.find("        mObservableConfigs = FXCollections.observableArrayList(TwoToneConfiguration.extractor());")
end = content.find("    public void process(TwoToneTabRequest request)")

new_block = """        mObservableConfigs = FXCollections.observableArrayList(TwoToneConfiguration.extractor());
        if (playlistManager.getCurrentPlaylist() != null) {
            mObservableConfigs.addAll(playlistManager.getCurrentPlaylist().getTwoToneConfigurations());
        }

        mTableView = new TableView<>(mObservableConfigs);

        TableColumn<TwoToneConfiguration, String> aliasCol = new TableColumn<>("Alias");
        aliasCol.setCellValueFactory(new PropertyValueFactory<>("alias"));
        TableColumn<TwoToneConfiguration, Double> toneACol = new TableColumn<>("Tone A");
        toneACol.setCellValueFactory(new PropertyValueFactory<>("toneA"));
        TableColumn<TwoToneConfiguration, Double> toneBCol = new TableColumn<>("Tone B");
        toneBCol.setCellValueFactory(new PropertyValueFactory<>("toneB"));
        TableColumn<TwoToneConfiguration, Boolean> mqttCol = new TableColumn<>("MQTT Enabled");
        mqttCol.setCellValueFactory(new PropertyValueFactory<>("enableMqttPublish"));

        mTableView.getColumns().addAll(aliasCol, toneACol, toneBCol, mqttCol);
        VBox.setVgrow(mTableView, Priority.ALWAYS);

        GridPane editorGrid = new GridPane();
        editorGrid.setHgap(10);
        editorGrid.setVgap(5);

        TextField aliasField = new TextField();
        TextField toneAField = new TextField();
        TextField toneBField = new TextField();
        TextField zelloField = new TextField();

        CheckBox mqttCheck = new CheckBox("Enable MQTT Publish");
        TextField topicField = new TextField();
        TextArea payloadArea = new TextArea();
        payloadArea.setPrefRowCount(3);

        topicField.disableProperty().bind(mqttCheck.selectedProperty().not());
        payloadArea.disableProperty().bind(mqttCheck.selectedProperty().not());

        mZelloAlertEnabledBox = new CheckBox("Enable Zello Alert Tone");
        mAlertToneFileBox = new ComboBox<>();
        populateAlertFiles();
        mAlertToneFileBox.disableProperty().bind(mZelloAlertEnabledBox.selectedProperty().not());
        Button previewBtn = new Button("Preview");
        previewBtn.setOnAction(e -> previewAudio());
        HBox fileBox = new HBox(10, mAlertToneFileBox, previewBtn);

        editorGrid.add(new Label("Alias:"), 0, 0);
        editorGrid.add(aliasField, 1, 0);
        editorGrid.add(new Label("Tone A:"), 0, 1);
        editorGrid.add(toneAField, 1, 1);
        editorGrid.add(new Label("Tone B:"), 0, 2);
        editorGrid.add(toneBField, 1, 2);
        editorGrid.add(new Label("Zello Channel:"), 0, 3);
        editorGrid.add(zelloField, 1, 3);

        editorGrid.add(mqttCheck, 2, 0, 2, 1);
        editorGrid.add(new Label("MQTT Topic:"), 2, 1);
        editorGrid.add(topicField, 3, 1);
        editorGrid.add(new Label("MQTT Payload:"), 2, 2);
        editorGrid.add(payloadArea, 3, 2, 1, 2);

        editorGrid.add(mZelloAlertEnabledBox, 0, 4, 2, 1);
        editorGrid.add(new Label("Alert Tone:"), 0, 5);
        editorGrid.add(fileBox, 1, 5, 3, 1);

        mTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (oldVal != null) {
                aliasField.textProperty().unbindBidirectional(oldVal.aliasProperty());
                zelloField.textProperty().unbindBidirectional(oldVal.zelloChannelProperty());
                mqttCheck.selectedProperty().unbindBidirectional(oldVal.enableMqttPublishProperty());
                topicField.textProperty().unbindBidirectional(oldVal.mqttTopicProperty());
                payloadArea.textProperty().unbindBidirectional(oldVal.mqttPayloadProperty());
                Bindings.unbindBidirectional(mZelloAlertEnabledBox.selectedProperty(), oldVal.zelloAlertToneEnabledProperty());
                Bindings.unbindBidirectional(mAlertToneFileBox.valueProperty(), oldVal.alertToneFileProperty());

                oldVal.setToneA(toneAField.getText().isEmpty() ? 0 : Double.parseDouble(toneAField.getText()));
                oldVal.setToneB(toneBField.getText().isEmpty() ? 0 : Double.parseDouble(toneBField.getText()));
            }
            if (newVal != null) {
                aliasField.textProperty().bindBidirectional(newVal.aliasProperty());
                toneAField.setText(String.valueOf(newVal.getToneA()));
                toneBField.setText(String.valueOf(newVal.getToneB()));
                zelloField.textProperty().bindBidirectional(newVal.zelloChannelProperty());
                mqttCheck.selectedProperty().bindBidirectional(newVal.enableMqttPublishProperty());
                topicField.textProperty().bindBidirectional(newVal.mqttTopicProperty());
                payloadArea.textProperty().bindBidirectional(newVal.mqttPayloadProperty());
                Bindings.bindBidirectional(mZelloAlertEnabledBox.selectedProperty(), newVal.zelloAlertToneEnabledProperty());
                Bindings.bindBidirectional(mAlertToneFileBox.valueProperty(), newVal.alertToneFileProperty());
            } else {
                aliasField.clear();
                toneAField.clear();
                toneBField.clear();
                zelloField.clear();
                mqttCheck.setSelected(false);
                topicField.clear();
                payloadArea.clear();
                mZelloAlertEnabledBox.setSelected(false);
                mAlertToneFileBox.setValue(null);
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

"""

content = content[:start] + new_block + content[end:]

with open('src/main/java/io/github/dsheirer/gui/playlist/twotone/TwoToneEditor.java', 'w') as f:
    f.write(content)
