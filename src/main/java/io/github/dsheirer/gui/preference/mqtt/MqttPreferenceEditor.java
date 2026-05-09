package io.github.dsheirer.gui.preference.mqtt;

import io.github.dsheirer.gui.preference.layout.SettingsCard;
import io.github.dsheirer.gui.preference.layout.SettingsRow;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.mqtt.MqttPreference;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;

public class MqttPreferenceEditor extends VBox
{
    private UserPreferences mUserPreferences;

    public MqttPreferenceEditor(UserPreferences userPreferences)
    {
        mUserPreferences = userPreferences;
        MqttPreference preference = userPreferences.getMqttPreference();

        setPadding(new Insets(10, 10, 10, 10));
        setSpacing(20);

        Label header = new Label("MQTT Settings");
        header.getStyleClass().add("hig-section-header");
        getChildren().add(header);

        SettingsCard mainCard = new SettingsCard();

        CheckBox enableCheckBox = new CheckBox("Enable MQTT");
        enableCheckBox.setSelected(preference.isEnabled());
        enableCheckBox.setTooltip(new Tooltip("Enables publishing SDRTrunk events to an MQTT broker"));

        TextField serverField = new TextField(preference.getServer());
        serverField.setPromptText("tcp://localhost:1883");
        serverField.setTooltip(new Tooltip("Enter the MQTT broker URL, e.g., tcp://localhost:1883"));

        TextField usernameField = new TextField(preference.getUsername());
        usernameField.setPromptText("Optional");
        usernameField.setTooltip(new Tooltip("Optional username for MQTT broker authentication"));

        PasswordField passwordField = new PasswordField();
        passwordField.setText(preference.getPassword());
        passwordField.setPromptText("Optional");
        passwordField.setTooltip(new Tooltip("Optional password for MQTT broker authentication"));

        serverField.disableProperty().bind(enableCheckBox.selectedProperty().not());
        usernameField.disableProperty().bind(enableCheckBox.selectedProperty().not());
        passwordField.disableProperty().bind(enableCheckBox.selectedProperty().not());

        mainCard.getChildren().add(new SettingsRow("Publish Events", enableCheckBox));
        mainCard.getChildren().add(new SettingsRow("Server/Host", serverField));
        mainCard.getChildren().add(new SettingsRow("Username", usernameField));
        mainCard.getChildren().add(new SettingsRow("Password", passwordField));

        enableCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> preference.setEnabled(newValue));
        serverField.textProperty().addListener((observable, oldValue, newValue) -> preference.setServer(newValue));
        usernameField.textProperty().addListener((observable, oldValue, newValue) -> preference.setUsername(newValue));
        passwordField.textProperty().addListener((observable, oldValue, newValue) -> preference.setPassword(newValue));

        getChildren().add(mainCard);
    }
}
