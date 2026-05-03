package io.github.dsheirer.source.tuner.ui;

public class USBAlertEvent {
    private String message;

    public USBAlertEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
