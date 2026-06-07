package io.github.dsheirer.module.decode.tetra;

/**
 * State machine for TETRA processing translated from GopherTrunk's process.go
 */
public class TetraProcessState {
    public enum State {
        SEARCHING_SYNC,
        DECODING_MAC_BLOCK,
        PROCESSING_PAYLOAD,
        ERROR_RECOVERY
    }

    private State currentState;

    public TetraProcessState() {
        this.currentState = State.SEARCHING_SYNC;
    }

    public State getCurrentState() {
        return currentState;
    }

    public void transitionTo(State newState) {
        this.currentState = newState;
    }
}
