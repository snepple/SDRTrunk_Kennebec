package io.github.dsheirer.gui.preference.notification;

import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import io.github.dsheirer.util.ThreadPool;
import javafx.application.Platform;
import java.util.Optional;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.notification.NotificationPreference;
import io.github.dsheirer.preference.notification.NotificationRecipient;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import org.controlsfx.control.ToggleSwitch;
import javafx.scene.Node;

public class NotificationPreferenceEditor extends VBox {

    private ListView<NotificationRecipient> mRecipientListView;
    private ObservableList<NotificationRecipient> mRecipientsList;
    private VBox mDetailPane;
    private SplitPane mSplitPane;

    public NotificationPreferenceEditor(UserPreferences userPreferences) {
        NotificationPreference preference = userPreferences.getNotificationPreference();

        setPadding(new Insets(10));
        setSpacing(20);

        // Telegram Section
        VBox telegramSection = new VBox(10);
        telegramSection.setStyle("-fx-border-color: lightgray; -fx-border-radius: 5; -fx-padding: 10; -fx-background-color: white;");
        Label telegramHeader = new Label("Global Telegram Configuration");
        telegramHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 1.1em;");

        CheckBox telegramEnable = new CheckBox("Enable Telegram");
        telegramEnable.setSelected(preference.isTelegramEnabled());
        telegramEnable.selectedProperty().addListener((obs, old, newValue) -> preference.setTelegramEnabled(newValue));

        GridPane telegramGrid = new GridPane();
        telegramGrid.setHgap(10);
        telegramGrid.setVgap(10);

        TextField botTokenField = new TextField(preference.getTelegramBotToken());
        botTokenField.setPrefWidth(300);
        botTokenField.textProperty().addListener((obs, old, newValue) -> preference.setTelegramBotToken(newValue));
        botTokenField.disableProperty().bind(telegramEnable.selectedProperty().not());

        telegramGrid.add(new Label("Bot Token:"), 0, 0);
        telegramGrid.add(botTokenField, 1, 0);

        Button telegramTestBtn = new Button("Send Test");
        telegramTestBtn.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Test Telegram");
            dialog.setHeaderText("Send a test message to Telegram");
            dialog.setContentText("Enter Chat ID:");
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(chatId -> {
                String token = botTokenField.getText();
                if (token == null || token.isEmpty()) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Bot token is not configured.");
                    alert.showAndWait();
                    return;
                }

                ThreadPool.CACHED.submit(() -> {
                    try {
                        String message = "SDRTrunk Telegram Test Message.";
                        String urlString = "https://api.telegram.org/bot" + token + "/sendMessage?chat_id=" + chatId + "&text=" + java.net.URLEncoder.encode(message, "UTF-8");
                        HttpClient client = HttpClient.newHttpClient();
                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(urlString))
                                .GET()
                                .build();

                        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                        Platform.runLater(() -> {
                            if (response.statusCode() == 200) {
                                new Alert(Alert.AlertType.INFORMATION, "Test message sent successfully.").showAndWait();
                            } else {
                                new Alert(Alert.AlertType.ERROR, "Failed to send message: " + response.body()).showAndWait();
                            }
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> {
                            new Alert(Alert.AlertType.ERROR, "Error sending message: " + ex.getMessage()).showAndWait();
                        });
                    }
                });
            });
        });


        telegramSection.getChildren().addAll(telegramHeader, telegramEnable, telegramGrid, telegramTestBtn);

        // Email Section
        VBox emailSection = new VBox(10);
        emailSection.setStyle("-fx-border-color: lightgray; -fx-border-radius: 5; -fx-padding: 10; -fx-background-color: white;");
        Label emailHeader = new Label("Global Email / SMTP Configuration");
        emailHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 1.1em;");

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

        Label emailInstruction = new Label("For Gmail: Host: smtp.gmail.com, Port: 465 or 587.\nUse an App Password instead of your regular account password.");
        emailInstruction.setStyle("-fx-font-size: 0.9em; -fx-text-fill: gray;");


        Button emailTestBtn = new Button("Send Test");
        emailTestBtn.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Test Email");
            dialog.setHeaderText("Send a test email");
            dialog.setContentText("Enter destination email address:");
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(toAddress -> {
                String host = smtpHostField.getText();
                String port = smtpPortField.getText();
                String username = smtpUsernameField.getText();
                String password = smtpPasswordField.getText();
                String fromAddress = smtpFromAddressField.getText();

                if (host == null || host.isEmpty() || port == null || port.isEmpty() || username == null || username.isEmpty() || password == null || password.isEmpty() || fromAddress == null || fromAddress.isEmpty()) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Please configure all SMTP settings before testing.");
                    alert.showAndWait();
                    return;
                }

                ThreadPool.CACHED.submit(() -> {
                    Properties props = new Properties();
                    props.put("mail.smtp.auth", "true");

                    if ("465".equals(port)) {
                        props.put("mail.smtp.ssl.enable", "true");
                        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                        props.put("mail.smtp.socketFactory.port", port);
                        props.put("mail.smtp.port", port);
                    } else {
                        props.put("mail.smtp.starttls.enable", "true");
                        props.put("mail.smtp.port", port);
                    }
                    props.put("mail.smtp.host", host);

                    Session session = Session.getInstance(props, new javax.mail.Authenticator() {
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(username, password);
                        }
                    });

                    try {
                        Message message = new MimeMessage(session);
                        message.setFrom(new InternetAddress(fromAddress));
                        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toAddress));
                        message.setSubject("SDRTrunk Test Email");
                        message.setText("This is a test email from SDRTrunk.");

                        Transport.send(message);

                        Platform.runLater(() -> {
                            new Alert(Alert.AlertType.INFORMATION, "Test email sent successfully.").showAndWait();
                        });
                    } catch (MessagingException ex) {
                        Platform.runLater(() -> {
                            new Alert(Alert.AlertType.ERROR, "Failed to send email: " + ex.getMessage()).showAndWait();
                        });
                    }
                });
            });
        });


        emailSection.getChildren().addAll(emailHeader, emailEnable, emailGrid, emailInstruction, emailTestBtn);

        HBox globalSettingsBox = new HBox(20, telegramSection, emailSection);
        HBox.setHgrow(telegramSection, Priority.ALWAYS);
        HBox.setHgrow(emailSection, Priority.ALWAYS);

        // Recipients Section
        VBox recipientsSection = new VBox(10);
        Label recipientsHeader = new Label("Notification Recipients & Routing");
        recipientsHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 1.2em;");

        mRecipientsList = FXCollections.observableArrayList(preference.getRecipients());
        mRecipientListView = new ListView<>(mRecipientsList);
        mRecipientListView.setCellFactory(param -> new ListCell<NotificationRecipient>() {
            @Override
            protected void updateItem(NotificationRecipient item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String dest = item.getDestination() != null && !item.getDestination().isEmpty() ? item.getDestination() : "New Recipient";
                    setText(item.getDeliveryMethod() + " - " + dest);
                }
            }
        });

        Button addBtn = new Button("Add Recipient");
        addBtn.setOnAction(e -> {
            NotificationRecipient nr = new NotificationRecipient();
            mRecipientsList.add(nr);
            saveRecipients(preference);
            mRecipientListView.getSelectionModel().select(nr);
        });

        Button removeBtn = new Button("Remove Selected");
        removeBtn.setOnAction(e -> {
            NotificationRecipient selected = mRecipientListView.getSelectionModel().getSelectedItem();
            if(selected != null) {
                mRecipientsList.remove(selected);
                saveRecipients(preference);
            }
        });
        removeBtn.disableProperty().bind(mRecipientListView.getSelectionModel().selectedItemProperty().isNull());

        HBox buttonBox = new HBox(10, addBtn, removeBtn);

        VBox masterBox = new VBox(10, mRecipientListView, buttonBox);
        VBox.setVgrow(mRecipientListView, Priority.ALWAYS);

        mDetailPane = new VBox(15);
        mDetailPane.setPadding(new Insets(15));
        mDetailPane.setStyle("-fx-background-color: white; -fx-border-color: lightgray; -fx-border-radius: 5;");

        mSplitPane = new SplitPane();
        mSplitPane.getItems().add(masterBox);
        VBox.setVgrow(mSplitPane, Priority.ALWAYS);

        mRecipientListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                updateDetailPane(newSelection, preference);
                if (!mSplitPane.getItems().contains(mDetailPane)) {
                    mSplitPane.getItems().add(mDetailPane);
                    mSplitPane.setDividerPositions(0.3);
                }
            } else {
                mSplitPane.getItems().remove(mDetailPane);
            }
        });

        recipientsSection.getChildren().addAll(recipientsHeader, mSplitPane);
        VBox.setVgrow(recipientsSection, Priority.ALWAYS);

        getChildren().addAll(globalSettingsBox, recipientsSection);

        // Initial state
        if (!mRecipientsList.isEmpty()) {
            mRecipientListView.getSelectionModel().selectFirst();
        } else {
            mSplitPane.getItems().remove(mDetailPane);
        }
    }

    private void updateDetailPane(NotificationRecipient recipient, NotificationPreference preference) {
        mDetailPane.getChildren().clear();

        Label detailHeader = new Label("Recipient Configuration");
        detailHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 1.1em;");

        GridPane configGrid = new GridPane();
        configGrid.setHgap(10);
        configGrid.setVgap(10);

        ComboBox<NotificationRecipient.DeliveryMethod> methodCombo = new ComboBox<>(FXCollections.observableArrayList(NotificationRecipient.DeliveryMethod.values()));
        methodCombo.setValue(recipient.getDeliveryMethod());
        methodCombo.setOnAction(e -> {
            recipient.setDeliveryMethod(methodCombo.getValue());
            saveRecipients(preference);
            mRecipientListView.refresh();
        });

        TextField destField = new TextField(recipient.getDestination());
        destField.setPromptText("Email address or Chat ID");
        destField.setPrefWidth(250);
        destField.textProperty().addListener((obs, old, newValue) -> {
            recipient.setDestination(newValue);
            saveRecipients(preference);
            mRecipientListView.refresh();
        });

        configGrid.add(new Label("Delivery Method:"), 0, 0);
        configGrid.add(methodCombo, 1, 0);
        configGrid.add(new Label("Destination:"), 0, 1);
        configGrid.add(destField, 1, 1);

        Label routingHeader = new Label("Alert Routing");
        routingHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 1.0em; -fx-padding: 10 0 0 0;");

        VBox routingBox = new VBox(15);
        routingBox.getChildren().addAll(
            createToggleRow("Hardware and Tuner Failures",
                "Notifies if a USB tuner disconnects, overheats, or fails.",
                recipient.isHardwareAlertEnabled(),
                val -> { recipient.setHardwareAlertEnabled(val); saveRecipients(preference); }),

            createToggleRow("Signal and Decoding Issues",
                "Notifies of sustained synchronization loss preventing voice traffic.",
                recipient.isSignalAlertEnabled(),
                val -> { recipient.setSignalAlertEnabled(val); saveRecipients(preference); }),

            createToggleRow("Application and System Errors",
                "Notifies of critical crashes, out-of-memory, or low disk space.",
                recipient.isSystemAlertEnabled(),
                val -> { recipient.setSystemAlertEnabled(val); saveRecipients(preference); }),

            createToggleRow("Integration and Network Failures",
                "Notifies of streaming disconnects or API auth failures.",
                recipient.isIntegrationAlertEnabled(),
                val -> { recipient.setIntegrationAlertEnabled(val); saveRecipients(preference); }),

            createToggleRow("Channel Inactivity",
                "Receive an alert if a channel has no transmissions for a configured duration.",
                recipient.isChannelInactivityEnabled(),
                val -> { recipient.setChannelInactivityEnabled(val); saveRecipients(preference); }),

            createToggleRow("AI Audio Monitoring",
                "Receive an alert if AI detects no audible voices on a channel.",
                recipient.isAiAudioMonitoringEnabled(),
                val -> { recipient.setAiAudioMonitoringEnabled(val); saveRecipients(preference); })
        );

        ScrollPane scrollPane = new ScrollPane(routingBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-control-inner-background: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        mDetailPane.getChildren().addAll(detailHeader, configGrid, routingHeader, scrollPane);
    }

    private Node createToggleRow(String title, String description, boolean initialState, java.util.function.Consumer<Boolean> onToggle) {
        VBox box = new VBox(2);

        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold;");

        ToggleSwitch toggle = new ToggleSwitch();
        toggle.setSelected(initialState);
        toggle.selectedProperty().addListener((obs, old, newVal) -> onToggle.accept(newVal));

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topRow.getChildren().addAll(titleLabel, spacer, toggle);

        Label descLabel = new Label(description);
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-font-size: 0.9em; -fx-text-fill: gray;");

        box.getChildren().addAll(topRow, descLabel);
        return box;
    }

    private void saveRecipients(NotificationPreference preference) {
        preference.setRecipients(new java.util.ArrayList<>(mRecipientsList));
        // Force the preference to save state
        preference.setEmailEnabled(preference.isEmailEnabled());
    }
}
