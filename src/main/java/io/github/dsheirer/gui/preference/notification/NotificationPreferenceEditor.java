package io.github.dsheirer.gui.preference.notification;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.notification.NotificationPreference;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class NotificationPreferenceEditor extends VBox {
    public NotificationPreferenceEditor(UserPreferences userPreferences) {
        NotificationPreference preference = userPreferences.getNotificationPreference();

        setPadding(new Insets(10));
        setSpacing(20);

        // Telegram Section
        VBox telegramSection = new VBox(10);
        Label telegramHeader = new Label("Telegram Alerts");
        telegramHeader.setStyle("-fx-font-weight: bold;");

        CheckBox telegramEnable = new CheckBox("Telegram Notifications");
        telegramEnable.setSelected(preference.isTelegramEnabled());
        telegramEnable.selectedProperty().addListener((obs, old, newValue) -> preference.setTelegramEnabled(newValue));

        GridPane telegramGrid = new GridPane();
        telegramGrid.setHgap(10);
        telegramGrid.setVgap(10);

        TextField botTokenField = new TextField(preference.getTelegramBotToken());
        botTokenField.textProperty().addListener((obs, old, newValue) -> preference.setTelegramBotToken(newValue));
        botTokenField.disableProperty().bind(telegramEnable.selectedProperty().not());

        TextField chatIdField = new TextField(preference.getTelegramChatId());
        chatIdField.textProperty().addListener((obs, old, newValue) -> preference.setTelegramChatId(newValue));
        chatIdField.disableProperty().bind(telegramEnable.selectedProperty().not());

        telegramGrid.add(new Label("Telegram Bot Token:"), 0, 0);
        telegramGrid.add(botTokenField, 1, 0);
        telegramGrid.add(new Label("Telegram Chat ID:"), 0, 1);
        telegramGrid.add(chatIdField, 1, 1);

        telegramSection.getChildren().addAll(telegramHeader, telegramEnable, telegramGrid);

        // Email / SMTP Section
        VBox emailSection = new VBox(10);
        Label emailHeader = new Label("Email / SMTP");
        emailHeader.setStyle("-fx-font-weight: bold;");

        CheckBox emailEnable = new CheckBox("Email Notifications");
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

        TextField smtpToAddressField = new TextField(preference.getSmtpToAddress());
        smtpToAddressField.textProperty().addListener((obs, old, newValue) -> preference.setSmtpToAddress(newValue));
        smtpToAddressField.disableProperty().bind(emailEnable.selectedProperty().not());

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
        emailGrid.add(new Label("To Address:"), 0, 5);
        emailGrid.add(smtpToAddressField, 1, 5);

        emailSection.getChildren().addAll(emailHeader, emailEnable, emailGrid);

        getChildren().addAll(telegramSection, emailSection);
    }
}
