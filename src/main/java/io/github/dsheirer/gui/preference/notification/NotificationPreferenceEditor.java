package io.github.dsheirer.gui.preference.notification;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.notification.NotificationPreference;
import io.github.dsheirer.preference.notification.NotificationRecipient;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;

public class NotificationPreferenceEditor extends VBox {

    private TableView<NotificationRecipient> mTableView;
    private ObservableList<NotificationRecipient> mRecipientsList;

    public NotificationPreferenceEditor(UserPreferences userPreferences) {
        NotificationPreference preference = userPreferences.getNotificationPreference();

        setPadding(new Insets(10));
        setSpacing(20);

        // Telegram Section
        VBox telegramSection = new VBox(10);
        Label telegramHeader = new Label("Global Telegram Configuration");
        telegramHeader.setStyle("-fx-font-weight: bold;");

        CheckBox telegramEnable = new CheckBox("Enable Telegram");
        telegramEnable.setSelected(preference.isTelegramEnabled());
        telegramEnable.selectedProperty().addListener((obs, old, newValue) -> preference.setTelegramEnabled(newValue));

        GridPane telegramGrid = new GridPane();
        telegramGrid.setHgap(10);
        telegramGrid.setVgap(10);

        TextField botTokenField = new TextField(preference.getTelegramBotToken());
        botTokenField.textProperty().addListener((obs, old, newValue) -> preference.setTelegramBotToken(newValue));
        botTokenField.disableProperty().bind(telegramEnable.selectedProperty().not());

        telegramGrid.add(new Label("Bot Token:"), 0, 0);
        telegramGrid.add(botTokenField, 1, 0);

        telegramSection.getChildren().addAll(telegramHeader, telegramEnable, telegramGrid);

        // Email / SMTP Section
        VBox emailSection = new VBox(10);
        Label emailHeader = new Label("Global Email / SMTP Configuration");
        emailHeader.setStyle("-fx-font-weight: bold;");

        CheckBox emailEnable = new CheckBox("Enable Email");
        emailEnable.setSelected(preference.isEmailEnabled());
        emailEnable.selectedProperty().addListener((obs, old, newValue) -> preference.setEmailEnabled(newValue));

        GridPane emailGrid = new GridPane();
        emailGrid.setHgap(10);
        emailGrid.setVgap(10);

        TextField smtpHostField = new TextField(preference.getSmtpHost());
        smtpHostField.textProperty().addListener((obs, old, newValue) -> preference.setSmtpHost(newValue));
        smtpHostField.disableProperty().bind(emailEnable.selectedProperty().not());

        TextField smtpPortField = new TextField(preference.getSmtpPort());
        smtpPortField.textProperty().addListener((obs, old, newValue) -> preference.setSmtpPort(newValue));
        smtpPortField.disableProperty().bind(emailEnable.selectedProperty().not());

        TextField smtpUsernameField = new TextField(preference.getSmtpUsername());
        smtpUsernameField.textProperty().addListener((obs, old, newValue) -> preference.setSmtpUsername(newValue));
        smtpUsernameField.disableProperty().bind(emailEnable.selectedProperty().not());

        PasswordField smtpPasswordField = new PasswordField();
        smtpPasswordField.setText(preference.getSmtpPassword());
        smtpPasswordField.textProperty().addListener((obs, old, newValue) -> preference.setSmtpPassword(newValue));
        smtpPasswordField.disableProperty().bind(emailEnable.selectedProperty().not());

        TextField smtpFromAddressField = new TextField(preference.getSmtpFromAddress());
        smtpFromAddressField.textProperty().addListener((obs, old, newValue) -> preference.setSmtpFromAddress(newValue));
        smtpFromAddressField.disableProperty().bind(emailEnable.selectedProperty().not());

        emailGrid.add(new Label("SMTP Host:"), 0, 0);
        emailGrid.add(smtpHostField, 1, 0);
        emailGrid.add(new Label("SMTP Port:"), 0, 1);
        emailGrid.add(smtpPortField, 1, 1);
        emailGrid.add(new Label("Username:"), 0, 2);
        emailGrid.add(smtpUsernameField, 1, 2);
        emailGrid.add(new Label("Password:"), 0, 3);
        emailGrid.add(smtpPasswordField, 1, 3);
        emailGrid.add(new Label("From Address:"), 0, 4);
        emailGrid.add(smtpFromAddressField, 1, 4);

        emailSection.getChildren().addAll(emailHeader, emailEnable, emailGrid);

        // Recipients Section
        VBox recipientsSection = new VBox(10);
        Label recipientsHeader = new Label("Notification Recipients & Routing");
        recipientsHeader.setStyle("-fx-font-weight: bold;");

        mRecipientsList = FXCollections.observableArrayList(preference.getRecipients());
        mTableView = new TableView<>(mRecipientsList);
        mTableView.setEditable(true);
        mTableView.setPlaceholder(new Label("Click the Add Recipient button to create a new recipient"));

        TableColumn<NotificationRecipient, NotificationRecipient.DeliveryMethod> methodCol = new TableColumn<>("Method");
        methodCol.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getDeliveryMethod()));
        methodCol.setCellFactory(ComboBoxTableCell.forTableColumn(NotificationRecipient.DeliveryMethod.values()));
        methodCol.setOnEditCommit(event -> {
            event.getRowValue().setDeliveryMethod(event.getNewValue());
            saveRecipients(preference);
        });
        methodCol.setPrefWidth(100);

        TableColumn<NotificationRecipient, String> destCol = new TableColumn<>("Destination (Email/Chat ID)");
        destCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDestination()));
        destCol.setCellFactory(TextFieldTableCell.forTableColumn());
        destCol.setOnEditCommit(event -> {
            event.getRowValue().setDestination(event.getNewValue());
            saveRecipients(preference);
        });
        destCol.setPrefWidth(200);

        TableColumn<NotificationRecipient, Boolean> inactivityCol = new TableColumn<>("Channel Inactivity");
        inactivityCol.setCellValueFactory(cellData -> {
            SimpleBooleanProperty prop = new SimpleBooleanProperty(cellData.getValue().isChannelInactivityEnabled());
            prop.addListener((obs, old, newValue) -> {
                cellData.getValue().setChannelInactivityEnabled(newValue);
                saveRecipients(preference);
            });
            return prop;
        });
        inactivityCol.setCellFactory(CheckBoxTableCell.forTableColumn(inactivityCol));

        Label inactivityGraphic = new Label("Channel Inactivity");
        inactivityGraphic.setTooltip(new Tooltip("Receive an alert if a channel has no transmissions for a configured duration."));
        inactivityCol.setGraphic(inactivityGraphic);
        inactivityCol.setPrefWidth(120);

        TableColumn<NotificationRecipient, Boolean> aiAudioCol = new TableColumn<>("AI Audio Monitoring");
        aiAudioCol.setCellValueFactory(cellData -> {
            SimpleBooleanProperty prop = new SimpleBooleanProperty(cellData.getValue().isAiAudioMonitoringEnabled());
            prop.addListener((obs, old, newValue) -> {
                cellData.getValue().setAiAudioMonitoringEnabled(newValue);
                saveRecipients(preference);
            });
            return prop;
        });
        aiAudioCol.setCellFactory(CheckBoxTableCell.forTableColumn(aiAudioCol));

        Label aiAudioGraphic = new Label("AI Audio Monitoring");
        aiAudioGraphic.setTooltip(new Tooltip("Receive an alert if the AI detects no audible voices on a channel for consecutive checks."));
        aiAudioCol.setGraphic(aiAudioGraphic);
        aiAudioCol.setPrefWidth(140);

        mTableView.getColumns().addAll(methodCol, destCol, inactivityCol, aiAudioCol);
        mTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(mTableView, Priority.ALWAYS);

        Button addBtn = new Button("Add Recipient");
        addBtn.setOnAction(e -> {
            NotificationRecipient nr = new NotificationRecipient();
            mRecipientsList.add(nr);
            saveRecipients(preference);
        });

        Button removeBtn = new Button("Remove Selected");
        removeBtn.setOnAction(e -> {
            NotificationRecipient selected = mTableView.getSelectionModel().getSelectedItem();
            if(selected != null) {
                mRecipientsList.remove(selected);
                saveRecipients(preference);
            }
        });
        removeBtn.disableProperty().bind(mTableView.getSelectionModel().selectedItemProperty().isNull());

        HBox buttonBox = new HBox(10, addBtn, removeBtn);
        recipientsSection.getChildren().addAll(recipientsHeader, mTableView, buttonBox);

        getChildren().addAll(telegramSection, emailSection, recipientsSection);
    }

    private void saveRecipients(NotificationPreference preference) {
        preference.setRecipients(new java.util.ArrayList<>(mRecipientsList));
        // Force the preference to save state
        preference.setEmailEnabled(preference.isEmailEnabled());
    }
}
