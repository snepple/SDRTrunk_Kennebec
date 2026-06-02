package io.github.dsheirer.preference.notification;

import io.github.dsheirer.preference.UserPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class NotificationRouter {
    private static final Logger mLog = LoggerFactory.getLogger(NotificationRouter.class);
    private final UserPreferences mUserPreferences;

    public NotificationRouter(UserPreferences userPreferences) {
        this.mUserPreferences = userPreferences;
    }

    public void dispatch(SystemAlert alert) {
        NotificationPreference prefs = mUserPreferences.getNotificationPreference();

        for (NotificationRecipient recipient : prefs.getRecipients()) {
            if (shouldSend(alert.getCategory(), recipient)) {
                if (recipient.getDeliveryMethod() == NotificationRecipient.DeliveryMethod.TELEGRAM && prefs.isTelegramEnabled()) {
                    sendTelegram(recipient.getDestination(), alert.getMessage(), prefs.getTelegramBotToken());
                } else if (recipient.getDeliveryMethod() == NotificationRecipient.DeliveryMethod.EMAIL && prefs.isEmailEnabled()) {
                    sendEmail(recipient.getDestination(), alert.getMessage(), prefs);
                }
            }
        }
    }

    private boolean shouldSend(AlertCategory category, NotificationRecipient recipient) {
        switch (category) {
            case HARDWARE:
                return recipient.isHardwareAlertEnabled();
            case SIGNAL:
                return recipient.isSignalAlertEnabled();
            case SYSTEM:
                return recipient.isSystemAlertEnabled();
            case INTEGRATION:
                return recipient.isIntegrationAlertEnabled();
            case CHANNEL_INACTIVITY:
                return recipient.isChannelInactivityEnabled();
            case AI_DIAGNOSTICS:
                return recipient.isAiAudioMonitoringEnabled();
            default:
                return false;
        }
    }

    private void sendTelegram(String chatId, String message, String botToken) {
        if (botToken == null || botToken.isEmpty()) return;
        try {
            String urlString = "https://api.telegram.org/bot" + botToken + "/sendMessage?chat_id=" + chatId + "&text=" + java.net.URLEncoder.encode(message, "UTF-8");
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(urlString)).GET().build();
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception ex) {
            mLog.error("Error sending Telegram message", ex);
        }
    }

    private void sendEmail(String toAddress, String text, NotificationPreference prefs) {
        String host = prefs.getSmtpHost();
        String port = prefs.getSmtpPort();
        String username = prefs.getSmtpUsername();
        String password = prefs.getSmtpPassword();
        String fromAddress = prefs.getSmtpFromAddress();

        if (host == null || host.isEmpty() || port == null || port.isEmpty() || username == null || username.isEmpty() || password == null || password.isEmpty() || fromAddress == null || fromAddress.isEmpty()) {
            return;
        }

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
            message.setSubject("SDRTrunk Alert");
            message.setText(text);

            Transport.send(message);
        } catch (MessagingException ex) {
            mLog.error("Failed to send email alert", ex);
        }
    }
}
