package io.github.dsheirer.eventbus;

public class RemoteDesktopModeEvent {
    private final boolean active;

    public RemoteDesktopModeEvent(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }
}
