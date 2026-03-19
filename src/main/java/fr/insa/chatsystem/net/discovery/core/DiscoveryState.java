package fr.insa.chatsystem.net.discovery.core;

/**
 * Discovery finite-state machine (FSM) states.
 *
 * <p>This enum represents the lifecycle of the discovery/handshake process.
 * Each state also indicates whether the discovery service is allowed to keep
 * listening for incoming messages (typically on UDP) while being in that state.</p>
 *
 * <p>Important: keep state names stable because they are often used in logs,
 * debug traces, and sometimes in tests.</p>
 */
public enum DiscoveryState {

    /** Fully connected to the network; incoming messages are expected. */
    CONNECTED(true),

    /** Not connected; may still listen for broadcasts or join requests depending on your design. */
    DISCONNECTED(false),

    /** Connection init was sent; waiting for acceptance/rejection. */
    WAITING_CONNECTION_RESPONSE(true),

    /** Acceptance message received (transition state, usually immediately followed by CONNECTED). */
    ACCEPTANCE_RECEIVED(false),

    /** Rejection message received (transition state, usually followed by DISCONNECTED). */
    REJECTION_RECEIVED(false),

    /** Pseudo validation request was sent; waiting for valid/invalid response. */
    WAITING_PSEUDO_REQUEST_RESPONSE(true),

    /** Pseudo accepted by the network (transition state). */
    VALID_PSEUDO_RECEIVED(false),

    /** Pseudo rejected by the network (transition state). */
    INVALID_PSEUDO_RECEIVED(false);

    /**
     * Whether the discovery service is allowed to read/process incoming messages
     * while being in this state.
     *
     * <p>This is NOT a security rule. It's an internal policy to reduce noise and
     * avoid handling messages at the wrong time in the handshake.</p>
     */
    private final boolean listeningAllowed;

    DiscoveryState(boolean listeningAllowed) {
        this.listeningAllowed = listeningAllowed;
    }

    /** @return true if the service should keep listening for incoming discovery messages in this state. */
    public boolean isListeningAllowed() {
        return listeningAllowed;
    }
}

