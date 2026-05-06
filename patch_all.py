import os

# 1. NotificationPreference.java
with open('src/main/java/io/github/dsheirer/preference/notification/NotificationPreference.java', 'r') as f:
    content = f.read()

content = content.replace("    private boolean mHardwareAlertEnabled = false;\n", "")
content = content.replace("    private boolean mSignalAlertEnabled = false;\n", "")
content = content.replace("    private boolean mSystemAlertEnabled = false;\n", "")
content = content.replace("    private boolean mIntegrationAlertEnabled = false;\n", "")

to_remove = [
    """    @JacksonXmlProperty(isAttribute = true, localName = "hardwareAlertEnabled")
    public boolean isHardwareAlertEnabled() {
        return mHardwareAlertEnabled;
    }

    public void setHardwareAlertEnabled(boolean hardwareAlertEnabled) {
        mHardwareAlertEnabled = hardwareAlertEnabled;
    }

""",
    """    @JacksonXmlProperty(isAttribute = true, localName = "signalAlertEnabled")
    public boolean isSignalAlertEnabled() {
        return mSignalAlertEnabled;
    }

    public void setSignalAlertEnabled(boolean signalAlertEnabled) {
        mSignalAlertEnabled = signalAlertEnabled;
    }

""",
    """    @JacksonXmlProperty(isAttribute = true, localName = "systemAlertEnabled")
    public boolean isSystemAlertEnabled() {
        return mSystemAlertEnabled;
    }

    public void setSystemAlertEnabled(boolean systemAlertEnabled) {
        mSystemAlertEnabled = systemAlertEnabled;
    }

""",
    """    @JacksonXmlProperty(isAttribute = true, localName = "integrationAlertEnabled")
    public boolean isIntegrationAlertEnabled() {
        return mIntegrationAlertEnabled;
    }

    public void setIntegrationAlertEnabled(boolean integrationAlertEnabled) {
        mIntegrationAlertEnabled = integrationAlertEnabled;
    }

"""
]

for block in to_remove:
    content = content.replace(block, "")

with open('src/main/java/io/github/dsheirer/preference/notification/NotificationPreference.java', 'w') as f:
    f.write(content)

# 2. NotificationRecipient.java
with open('src/main/java/io/github/dsheirer/preference/notification/NotificationRecipient.java', 'r') as f:
    content = f.read()

if "mHardwareAlertEnabled" not in content:
    content = content.replace(
        "    private boolean mAiAudioMonitoringEnabled = false;\n",
        "    private boolean mAiAudioMonitoringEnabled = false;\n    private boolean mHardwareAlertEnabled = false;\n    private boolean mSignalAlertEnabled = false;\n    private boolean mSystemAlertEnabled = false;\n    private boolean mIntegrationAlertEnabled = false;\n"
    )

    methods = """
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
}"""
    content = content.replace("}\n", methods + "\n", 1)

with open('src/main/java/io/github/dsheirer/preference/notification/NotificationRecipient.java', 'w') as f:
    f.write(content)


# 3. SystemHealthMonitor.java
with open('src/main/java/io/github/dsheirer/health/SystemHealthMonitor.java', 'r') as f:
    content = f.read()

old_trigger = """    private void triggerAlert(SystemHealthAlertEvent.AlertType type, String title, String message) {
        NotificationPreference pref = mUserPreferences.getNotificationPreference();
        if (pref == null) {
            return;
        }

        boolean enabled = false;
        switch (type) {
            case HARDWARE:
                enabled = pref.isHardwareAlertEnabled();
                break;
            case SIGNAL:
                enabled = pref.isSignalAlertEnabled();
                break;
            case SYSTEM:
                enabled = pref.isSystemAlertEnabled();
                break;
            case INTEGRATION:
                enabled = pref.isIntegrationAlertEnabled();
                break;
        }

        if (!enabled) {
            return;
        }

        long now = System.currentTimeMillis();
        long lastAlertTime = mLastAlertTimes.getOrDefault(type, 0L);

        if (now - lastAlertTime >= RATE_LIMIT_MS) {
            // Update the timestamp and send notification
            mLastAlertTimes.put(type, now);
            mLog.warn("System Health Alert Triggered [" + type + "]: " + message);

            // Dispatch to NotificationManager
            NotificationManager.getInstance().showNotification(title, message, TrayIcon.MessageType.ERROR);

            // Dispatch to recipients (for simplicity, we just use existing NotificationManager which handles Desktop popups.
            // A more complete implementation might route through telegram/email recipients if configured, but NotificationManager suffices for the UI alert requirement).
        } else {
            mLog.debug("System Health Alert Suppressed (Rate Limited) [" + type + "]: " + message);
        }
    }"""

new_trigger = """    private void triggerAlert(SystemHealthAlertEvent.AlertType type, String title, String message) {
        NotificationPreference pref = mUserPreferences.getNotificationPreference();
        if (pref == null) {
            return;
        }

        boolean enabled = false;

        if (pref.getRecipients() != null) {
            for (NotificationRecipient recipient : pref.getRecipients()) {
                switch (type) {
                    case HARDWARE:
                        if (recipient.isHardwareAlertEnabled()) enabled = true;
                        break;
                    case SIGNAL:
                        if (recipient.isSignalAlertEnabled()) enabled = true;
                        break;
                    case SYSTEM:
                        if (recipient.isSystemAlertEnabled()) enabled = true;
                        break;
                    case INTEGRATION:
                        if (recipient.isIntegrationAlertEnabled()) enabled = true;
                        break;
                }
                if (enabled) break; // If at least one recipient wants it, we enable the alert popup
            }
        }

        if (!enabled) {
            return;
        }

        long now = System.currentTimeMillis();
        long lastAlertTime = mLastAlertTimes.getOrDefault(type, 0L);

        if (now - lastAlertTime >= RATE_LIMIT_MS) {
            // Update the timestamp and send notification
            mLastAlertTimes.put(type, now);
            mLog.warn("System Health Alert Triggered [" + type + "]: " + message);

            // Dispatch to NotificationManager
            NotificationManager.getInstance().showNotification(title, message, TrayIcon.MessageType.ERROR);

            // Dispatch to recipients (for simplicity, we just use existing NotificationManager which handles Desktop popups.
            // A more complete implementation might route through telegram/email recipients if configured, but NotificationManager suffices for the UI alert requirement).
        } else {
            mLog.debug("System Health Alert Suppressed (Rate Limited) [" + type + "]: " + message);
        }
    }"""

content = content.replace(old_trigger, new_trigger)

with open('src/main/java/io/github/dsheirer/health/SystemHealthMonitor.java', 'w') as f:
    f.write(content)
