package io.github.dsheirer.health;

public class SystemHealthAlertEvent {

    public enum AlertType {
        HARDWARE,
        SIGNAL,
        SYSTEM,
        INTEGRATION
    }

    private final AlertType mType;
    private final String mTitle;
    private final String mMessage;

    public SystemHealthAlertEvent(AlertType type, String title, String message) {
        this.mType = type;
        this.mTitle = title;
        this.mMessage = message;
    }

    public AlertType getType() {
        return mType;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getMessage() {
        return mMessage;
    }
}
