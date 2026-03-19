package fr.insa.chatsystem.net.discovery.events;

/**
 * Observer interface for the Discovery layer.
 *
 * A {@code NetworkListener} receives callbacks when a {@link NetworkMsg} is received and decoded
 * by the discovery subsystem. Implementations should treat these callbacks as "event handlers":
 * keep them fast, avoid blocking I/O, and delegate heavy work to another layer/thread if needed.
 *
 * The callbacks are grouped by the role of the local machine at the time the message is received:
 *  - "Server-side": events where *another peer* initiates an action toward us.
 *  - "Client-side": responses to actions that *we* initiated earlier.
 *
 * Note: The listener does not decide how messages are transported (UDP) nor parsed (JSON),
 * it only reacts to already-validated message objects.
 */
public interface NetworkListener {

    /* ================================================================================================================
     * Server-side events (peer -> us): we received an unsolicited request/notification from the network.
     * ============================================================================================================== */

    /**
     * Called when a peer asks to join the network (initial handshake request).
     *
     * Typical reactions:
     *  - verify requested pseudonym availability / rules
     *  - accept or reject the connection by sending an appropriate response message
     *
     * @param msg decoded connection-init message sent by the peer
     */
    default void onConnectionRequest(ConnectionInitMsg msg){}

    default void onNewUserAccepted(ConnectionInitMsg msg){}

    default void onNewUserDenied(ConnectionInitMsg msg){}

    /**
     * Called when a peer notifies that it is leaving the network.
     *
     * Typical reactions:
     *  - remove the peer from internal contact list/state
     *  - notify UI / higher-level components about the departure
     *
     * @param msg decoded disconnection message sent by the peer
     */
    default void onDisconnection(DisconnectionMsg msg){}

    /**
     * Called when a peer requests to change its pseudonym (broadcasted or addressed request depending on protocol).
     *
     * Typical reactions:
     *  - validate the new pseudonym (uniqueness, format, length, etc.)
     *  - respond with either {@link ValidPseudoMsg} or {@link InvalidPseudoMsg}
     *
     * @param msg decoded pseudo-change request message sent by the peer
     */
    default void onPseudoChangeRequest(PseudoRequestMsg msg){}

    /**
     * Calls when a Pseudo change request is received and then Accepted.
     * @param msg
     */
    default void onPseudoValidated(PseudoRequestMsg msg){};

    /**
     * Calls when a Pseudo change request is received and the denied.
     *
     * @param msg
     */
    default void onPseudoDenied(PseudoRequestMsg msg){};

    /* ================================================================================================================
     * Client-side events (network -> us): responses to an action we previously initiated.
     * ============================================================================================================== */

    /**
     * Called when our connection request has been accepted by the network/peer(s).
     *
     * Typical reactions:
     *  - transition discovery state to "connected"
     *  - initialize local contact list using the information in the acceptance message
     *
     * @param msg decoded acceptance message received in response to our {@link ConnectionInitMsg}
     */
    default void onConnectionAcceptance(AcceptanceMsg msg){}
    /**
     * Called when our connection request has been rejected (e.g., pseudo already used, policy rejection, etc.).
     *
     * Typical reactions:
     *  - keep state as disconnected
     *  - prompt user for a new pseudonym or display the rejection reason (if available)
     *
     * @param msg decoded rejection message received in response to our {@link ConnectionInitMsg}
     */
    default void onConnectionRejection(RejectionMsg msg){}

    /**
     * Called when our request to change pseudonym has been accepted.
     *
     * Typical reactions:
     *  - update local user identity / UI prompt
     *  - update internal contact list entry for self if applicable
     *
     * @param msg decoded "valid pseudo" response to our pseudo-change request
     */
    default void onValidPseudo(ValidPseudoMsg msg){}

    /**
     * Called when our request to change pseudonym has been rejected.
     *
     * Typical reactions:
     *  - keep current pseudonym unchanged
     *  - inform the user and optionally suggest another pseudonym
     *
     * @param msg decoded "invalid pseudo" response to our pseudo-change request
     */
    default void onInvalidPseudo(InvalidPseudoMsg msg){}

}
