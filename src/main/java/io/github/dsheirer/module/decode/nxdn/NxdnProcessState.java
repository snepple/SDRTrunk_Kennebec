package io.github.dsheirer.module.decode.nxdn;

/**
 * State machine for NXDN processing translated from GopherTrunk's process.go
 */
public class NxdnProcessState {
    public enum State {
        SEARCHING_FSW,
        DECODING_LICH,
        PROCESSING_PAYLOAD,
        ERROR_RECOVERY
    }

    private State currentState;

    public NxdnProcessState() {
        this.currentState = State.SEARCHING_FSW;
    }

    public State getCurrentState() {
        return currentState;
    }

    public void transitionTo(State newState) {
        this.currentState = newState;
    }
}
