

package io.github.dsheirer.gui.preference.notification;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;

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
import io.github.dsheirer.gui.preference.layout.SettingsCard;
import io.github.dsheirer.gui.preference.layout.SettingsRow;
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
        Label telegramHeader = new Label("Global Telegram Configuration");
        telegramHeader.getStyleClass().add("hig-section-header");

        SettingsCard telegramCard = new SettingsCard();

        CheckBox telegramEnable = new CheckBox();
        telegramEnable.setSelected(preference.isTelegramEnabled());
        telegramEnable.selectedProperty().addListener((obs, old, newValue) -> preference.setTelegramEnabled(newValue));

        TextField botTokenField = new TextField(preference.getTelegramBotToken() != null ? preference.getTelegramBotToken() : "");
        botTokenField.setPrefWidth(300);
        botTokenField.setTooltip(new Tooltip("The Telegram Bot Token from BotFather."));
        botTokenField.textProperty().addListener((obs, old, newValue) -> preference.setTelegramBotToken(newValue));
        botTokenField.disableProperty().bind(telegramEnable.selectedProperty().not());

        telegramCard.getChildren().add(new SettingsRow("Enable Telegram", telegramEnable));
        telegramCard.getChildren().add(new SettingsRow("Bot Token", botTokenField));

        // Embedded Scaffolding: step-by-step instructions
        VBox telegramScaffolding = new VBox(3);
        telegramScaffolding.setPadding(new Insets(5, 15, 5, 15));
        telegramScaffolding.getStyleClass().add("kennebec-secondary-text");
        Label tScaffoldHeader = new Label("How to get your Bot Token & Chat ID:");
        tScaffoldHeader.setStyle("-fx-font-weight: bold;");
        Label tStep1 = new Label("1. Open Telegram and search for @BotFather.");
        Label tStep2 = new Label("2. Send /newbot and follow the prompts. Copy the token.");
        Label tStep3 = new Label("3. To find your Chat ID, message @userinfobot.");
        Label tStep4 = new Label("   Format: 1234567890  (numeric, no spaces)");
        Hyperlink tLink = new Hyperlink("Open Telegram");
        tLink.setPadding(new Insets(0));
        tLink.setOnAction(ev -> {
            try {
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().browse(new URI("https://t.me/botfather"));
                }
            } catch (Exception ex) { /* ignore */ }
        });
        telegramScaffolding.getChildren().addAll(tScaffoldHeader, tStep1, tStep2, tStep3, tStep4, tLink);

        Button telegramTestBtn = new Button("_Send Test");
        telegramTestBtn.setMnemonicParsing(true);
        telegramTestBtn.setTooltip(new Tooltip("Send a test message to the configured Telegram bot."));
        telegramTestBtn.accessibleTextProperty().set("Send Telegram Test");
        telegramTestBtn.accessibleHelpProperty().set("Sends a test message to the configured Telegram bot to verify connectivity.");
        telegramTestBtn.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Test Telegram");
            dialog.setHeaderText("Send a test message to Telegram");
            dialog.setContentText("Enter Chat ID:");
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(chatId -> {
                String token = botTokenField.getText();
                if (token == null || token.isEmpty()) {
                    new Alert(Alert.AlertType.ERROR, "Bot token is not configured.").showAndWait();
                    return;
                }
                ThreadPool.CACHED.submit(() -> {
                    try {
                        String message = "SDRTrunk Telegram Test Message.";
                        String urlString = "https://api.telegram.org/bot" + token + "/sendMessage?chat_id=" + chatId + "&text=" + java.net.URLEncoder.encode(message, "UTF-8");
                        HttpClient client = HttpClient.newHttpClient();
                        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(urlString)).GET().build();
                        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                        Platform.runLater(() -> {
                            if (response.statusCode() == 200) {
                                new Alert(Alert.AlertType.INFORMATION, "Test message sent successfully.").showAndWait();
                            } else {
                                new Alert(Alert.AlertType.ERROR, "Failed to send message: " + response.body()).showAndWait();
                            }
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, "Error: " + ex.getMessage()).showAndWait());
                    }
                });
            });
        });

        telegramSection.getChildren().addAll(telegramHeader, telegramCard, telegramScaffolding, telegramTestBtn);

        // Email Section
        VBox emailSection = new VBox(10);
        Label emailHeader = new Label("Global Email / SMTP Configuration");
        emailHeader.getStyleClass().add("hig-section-header");

        SettingsCard emailCard = new SettingsCard();

        CheckBox emailEnable = new CheckBox();
        emailEnable.setSelected(preference.isEmailEnabled());
        emailEnable.selectedProperty().addListener((obs, old, newValue) -> preference.setEmailEnabled(newValue));

        TextField smtpHostField = new TextField(preference.getSmtpHost() != null ? preference.getSmtpHost() : "");
        smtpHostField.setTooltip(new Tooltip("e.g. smtp.gmail.com"));
        smtpHostField.textProperty().addListener((obs, old, newValue) -> preference.setSmtpHost(newValue));
        smtpHostField.disableProperty().bind(emailEnable.selectedProperty().not());

        TextField smtpPortField = new TextField(preference.getSmtpPort() != null ? preference.getSmtpPort() : "");
        smtpPortField.setTooltip(new Tooltip("e.g. 465 or 587"));
        smtpPortField.textProperty().addListener((obs, old, newValue) -> preference.setSmtpPort(newValue));
        smtpPortField.disableProperty().bind(emailEnable.selectedProperty().not());

        TextField smtpUsernameField = new TextField(preference.getSmtpUsername() != null ? preference.getSmtpUsername() : "");
        smtpUsernameField.textProperty().addListener((obs, old, newValue) -> preference.setSmtpUsername(newValue));
        smtpUsernameField.disableProperty().bind(emailEnable.selectedProperty().not());

        PasswordField smtpPasswordField = new PasswordField();
        smtpPasswordField.setText(preference.getSmtpPassword() != null ? preference.getSmtpPassword() : "");
        smtpPasswordField.textProperty().addListener((obs, old, newValue) -> preference.setSmtpPassword(newValue));
        smtpPasswordField.disableProperty().bind(emailEnable.selectedProperty().not());

        TextField smtpFromAddressField = new TextField(preference.getSmtpFromAddress() != null ? preference.getSmtpFromAddress() : "");
        smtpFromAddressField.textProperty().addListener((obs, old, newValue) -> preference.setSmtpFromAddress(newValue));
        smtpFromAddressField.disableProperty().bind(emailEnable.selectedProperty().not());

        emailCard.getChildren().add(new SettingsRow("Enable Email", emailEnable));
        emailCard.getChildren().add(new SettingsRow("SMTP Host", smtpHostField));
        emailCard.getChildren().add(new SettingsRow("SMTP Port", smtpPortField));
        emailCard.getChildren().add(new SettingsRow("Username", smtpUsernameField));
        emailCard.getChildren().add(new SettingsRow("Password", smtpPasswordField));
        emailCard.getChildren().add(new SettingsRow("From Address", smtpFromAddressField));

        Label emailInstruction = new Label("For Gmail: Host: smtp.gmail.com, Port: 465 or 587.\nUse an App Password instead of your regular account password.");
        emailInstruction.getStyleClass().add("kennebec-secondary-text");


        Button emailTestBtn = new Button("_Send Test");
        emailTestBtn.setMnemonicParsing(true);
        emailTestBtn.setTooltip(new Tooltip("Send a test message to the configured email server."));
        emailTestBtn.accessibleTextProperty().set("Send Email Test");
        emailTestBtn.accessibleHelpProperty().set("Sends a test message to the configured email server to verify connectivity.");
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
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Please configure all SMTP settings before testing."); io.github.dsheirer.gui.theme.ThemeManager.applyCurrentTheme(alert.getDialogPane());
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


        emailSection.getChildren().addAll(emailHeader, emailCard, emailInstruction, emailTestBtn);

        HBox globalSettingsBox = new HBox(20, telegramSection, emailSection);
        HBox.setHgrow(telegramSection, Priority.ALWAYS);
        HBox.setHgrow(emailSection, Priority.ALWAYS);

        // Recipients Section
        VBox recipientsSection = new VBox(10);
        Label recipientsHeader = new Label("Notification Recipients & Routing");
        recipientsHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 1.2em;");

        mRecipientsList = FXCollections.observableArrayList(preference.getRecipients());
        mRecipientListView = new ListView<>(mRecipientsList);
        
        Label placeholder = new Label("No recipients configured.\nClick 'Add Recipient' to get started.");
        placeholder.setStyle("-fx-text-alignment: center; -fx-text-fill: #8e8e93; -fx-font-size: 14px;");
        placeholder.setAlignment(Pos.CENTER);
        mRecipientListView.setPlaceholder(placeholder);
        
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

        Button addBtn = new Button("_Add Recipient");
        addBtn.setMnemonicParsing(true);
        addBtn.setTooltip(new Tooltip("Create a new notification recipient."));
        addBtn.accessibleTextProperty().set("Add Recipient");
        addBtn.accessibleHelpProperty().set("Creates a new notification recipient entry.");
        addBtn.setOnAction(e -> {
            NotificationRecipient nr = new NotificationRecipient();
            mRecipientsList.add(nr);
            saveRecipients(preference);
            mRecipientListView.getSelectionModel().select(nr);
        });

        Button removeBtn = new Button("_Remove Selected");
        removeBtn.setMnemonicParsing(true);
        removeBtn.setTooltip(new Tooltip("Delete the currently selected notification recipient. This action cannot be undone."));
        removeBtn.accessibleTextProperty().set("Remove Selected Recipient");
        removeBtn.accessibleHelpProperty().set("Deletes the currently selected notification recipient. This action cannot be undone.");
        removeBtn.setOnAction(e -> {
            NotificationRecipient selected = mRecipientListView.getSelectionModel().getSelectedItem();
            if(selected != null) {
                mRecipientsList.remove(selected);
                saveRecipients(preference);
            }
        });
        removeBtn.disableProperty().bind(mRecipientListView.getSelectionModel().selectedItemProperty().isNull());

        HBox buttonBox = new HBox(10, addBtn, removeBtn);
        buttonBox.setAlignment(Pos.CENTER_LEFT);

        VBox masterBox = new VBox(10, mRecipientListView, buttonBox);
        VBox.setVgrow(mRecipientListView, Priority.ALWAYS);
        
        SettingsCard masterCard = new SettingsCard();
        masterCard.getChildren().add(masterBox);
        VBox.setVgrow(masterCard, Priority.ALWAYS);
        
        // Remove padding from masterBox because SettingsCard provides it
        masterBox.setPadding(new Insets(0));

        mDetailPane = new VBox(15);
        mDetailPane.setPadding(new Insets(15));
        mDetailPane.setStyle("-fx-background-color: white; -fx-border-color: lightgray; -fx-border-radius: 5;");

        mSplitPane = new SplitPane();
        mSplitPane.getItems().add(masterCard);
        VBox.setVgrow(mSplitPane, Priority.ALWAYS);
        mSplitPane.setStyle("-fx-background-color: transparent; -fx-padding: 0;");

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

        VBox contentBox = new VBox(20, globalSettingsBox, recipientsSection);
        VBox.setVgrow(recipientsSection, Priority.ALWAYS);

        ScrollPane scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-control-inner-background: transparent; -fx-border-color: transparent;");

        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        getChildren().add(scrollPane);

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
        detailHeader.getStyleClass().add("hig-section-header");

        SettingsCard configCard = new SettingsCard();

        ComboBox<NotificationRecipient.DeliveryMethod> methodCombo = new ComboBox<>(FXCollections.observableArrayList(NotificationRecipient.DeliveryMethod.values()));
        methodCombo.setValue(recipient.getDeliveryMethod());
        methodCombo.setOnAction(e -> {
            recipient.setDeliveryMethod(methodCombo.getValue());
            saveRecipients(preference);
            mRecipientListView.refresh();
        });

        TextField destField = new TextField(recipient.getDestination() != null ? recipient.getDestination() : "");
        destField.setPromptText("Email address or Chat ID");
        destField.setPrefWidth(250);
        destField.textProperty().addListener((obs, old, newValue) -> {
            recipient.setDestination(newValue);
            saveRecipients(preference);
            mRecipientListView.refresh();
        });

        configCard.getChildren().add(new SettingsRow("Delivery Method", methodCombo));
        configCard.getChildren().add(new SettingsRow("Destination", destField));

        Label routingHeader = new Label("Alert Routing");
        routingHeader.getStyleClass().add("hig-section-header");

        VBox routingBox = new VBox(5);
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

        mDetailPane.getChildren().addAll(detailHeader, configCard, routingHeader, routingBox);
    }

    private Node createToggleRow(String title, String description, boolean initialState, java.util.function.Consumer<Boolean> onToggle) {
        VBox box = new VBox();
        box.setSpacing(2);
        box.setPadding(new Insets(10, 15, 10, 15)); // Match standard SettingsRow padding roughly

        ToggleSwitch toggle = new ToggleSwitch();
        toggle.setSelected(initialState);
        toggle.selectedProperty().addListener((obs, old, newVal) -> onToggle.accept(newVal));

        SettingsRow row = new SettingsRow(title, toggle);
        // Override padding for the embedded row to 0 since the VBox handles it
        row.setPadding(Insets.EMPTY);

        Label descLabel = new Label(description);
        descLabel.setWrapText(true);
        descLabel.getStyleClass().add("kennebec-secondary-text");

        box.getChildren().addAll(row, descLabel);
        return box;
    }

    private void saveRecipients(NotificationPreference preference) {
        preference.setRecipients(new java.util.ArrayList<>(mRecipientsList));
        // Force the preference to save state
        preference.setEmailEnabled(preference.isEmailEnabled());
    }
}
