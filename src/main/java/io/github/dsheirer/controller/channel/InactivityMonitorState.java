package io.github.dsheirer.controller.channel;

/**
 * Finite state machine states for per-channel inactivity monitoring.
 */
public enum InactivityMonitorState
{
    NORMAL_OPERATION,
    PENDING_INACTIVE,
    OBSERVING_RESTART,
    INACTIVE_ALERT,
    RECOVERED
}
