package io.github.dsheirer.preference.notification;

public class SystemAlert {
    private final AlertCategory category;
    private final String message;
    private final String faultSignature;

    public SystemAlert(AlertCategory category, String message, String faultSignature) {
        this.category = category;
        this.message = message;
        this.faultSignature = faultSignature;
    }

    public AlertCategory getCategory() {
        return category;
    }

    public String getMessage() {
        return message;
    }

    public String getFaultSignature() {
        return faultSignature;
    }
}
