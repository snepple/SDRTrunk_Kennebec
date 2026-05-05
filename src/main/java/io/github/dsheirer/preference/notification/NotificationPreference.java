package io.github.dsheirer.preference.notification;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.preference.Preference;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.preference.PreferenceType;
import java.util.List;
import java.util.ArrayList;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

public class NotificationPreference extends Preference {
    private boolean mTelegramEnabled = false;
    private String mTelegramBotToken = "";

    private boolean mEmailEnabled = false;
    private String mSmtpHost = "";
    private String mSmtpPort = "";
    private String mSmtpUsername = "";
    private String mSmtpPassword = "";
    private String mSmtpFromAddress = "";
    private boolean mHardwareAlertEnabled = false;
    private boolean mSignalAlertEnabled = false;
    private boolean mSystemAlertEnabled = false;
    private boolean mIntegrationAlertEnabled = false;


    private List<NotificationRecipient> mRecipients = new ArrayList<>();

    public NotificationPreference(Listener<PreferenceType> listener) {
        super(listener);
    }

    @JacksonXmlProperty(isAttribute = true, localName = "telegramEnabled")
    public boolean isTelegramEnabled() {
        return mTelegramEnabled;
    }

    public void setTelegramEnabled(boolean telegramEnabled) {
        mTelegramEnabled = telegramEnabled;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "telegramBotToken")
    public String getTelegramBotToken() {
        return mTelegramBotToken;
    }

    public void setTelegramBotToken(String telegramBotToken) {
        mTelegramBotToken = telegramBotToken;
    }



    @JacksonXmlProperty(isAttribute = true, localName = "emailEnabled")
    public boolean isEmailEnabled() {
        return mEmailEnabled;
    }

    public void setEmailEnabled(boolean emailEnabled) {
        mEmailEnabled = emailEnabled;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "smtpHost")
    public String getSmtpHost() {
        return mSmtpHost;
    }

    public void setSmtpHost(String smtpHost) {
        mSmtpHost = smtpHost;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "smtpPort")
    public String getSmtpPort() {
        return mSmtpPort;
    }

    public void setSmtpPort(String smtpPort) {
        mSmtpPort = smtpPort;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "smtpUsername")
    public String getSmtpUsername() {
        return mSmtpUsername;
    }

    public void setSmtpUsername(String smtpUsername) {
        mSmtpUsername = smtpUsername;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "smtpPassword")
    public String getSmtpPassword() {
        return mSmtpPassword;
    }

    public void setSmtpPassword(String smtpPassword) {
        mSmtpPassword = smtpPassword;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "smtpFromAddress")
    public String getSmtpFromAddress() {
        return mSmtpFromAddress;
    }

    public void setSmtpFromAddress(String smtpFromAddress) {
        mSmtpFromAddress = smtpFromAddress;
    }
    @JacksonXmlProperty(isAttribute = true, localName = "hardwareAlertEnabled")
    public boolean isHardwareAlertEnabled() {
        return mHardwareAlertEnabled;
    }

    public void setHardwareAlertEnabled(boolean hardwareAlertEnabled) {
        mHardwareAlertEnabled = hardwareAlertEnabled;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "signalAlertEnabled")
    public boolean isSignalAlertEnabled() {
        return mSignalAlertEnabled;
    }

    public void setSignalAlertEnabled(boolean signalAlertEnabled) {
        mSignalAlertEnabled = signalAlertEnabled;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "systemAlertEnabled")
    public boolean isSystemAlertEnabled() {
        return mSystemAlertEnabled;
    }

    public void setSystemAlertEnabled(boolean systemAlertEnabled) {
        mSystemAlertEnabled = systemAlertEnabled;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "integrationAlertEnabled")
    public boolean isIntegrationAlertEnabled() {
        return mIntegrationAlertEnabled;
    }

    public void setIntegrationAlertEnabled(boolean integrationAlertEnabled) {
        mIntegrationAlertEnabled = integrationAlertEnabled;
    }






    @JacksonXmlElementWrapper(localName = "recipients")
    @JacksonXmlProperty(localName = "recipient")
    public List<NotificationRecipient> getRecipients() {
        return mRecipients;
    }

    public void setRecipients(List<NotificationRecipient> recipients) {
        if(recipients != null) {
            mRecipients = recipients;
        }
    }

    @Override
    public PreferenceType getPreferenceType() {
        return PreferenceType.NOTIFICATIONS;
    }
}
