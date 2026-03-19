package fr.insa.chatsystem.net.discovery.events;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Event dispatcher for the Discovery layer (Observer pattern).
 *
 * Responsibilities:
 *  - Maintain a list of {@link NetworkListener} observers.
 *  - Dispatch incoming {@link NetworkMsg} instances to the correct callback method.
 *
 * Notes:
 *  - This class assumes messages are already decoded (JSON -> NetworkMsg) before notification.
 *  - Callbacks should be considered "fast-path": listeners should avoid blocking operations.
 *  - Current implementation broadcasts every event to every subscriber.
 *
 * Implementation detail:
 *  - Uses Java pattern matching for switch on sealed hierarchy ({@link NetworkMsg} subclasses),
 *    which gives compile-time exhaustiveness if NetworkMsg is sealed.
 */
public class NetworkEventManager {

    /**
     * (Currently unused) Map-based structure that could support per-type subscriptions.
     * Example: subscribe(listener, NetworkMsgType.INIT_CONNECTION).
     *
     * If you don't plan to implement filtered subscriptions, remove it to avoid dead fields.
     */
    HashMap<NetworkMsgType, ArrayList<NetworkListener>> listenersMap = new HashMap<>();

    /**
     * List of all subscribed listeners.
     * Each received event is broadcast to every listener in this list.
     */
    ArrayList<NetworkListener> listeners = new ArrayList<>();

    /**
     * Subscribes a listener if it is not already present.
     *
     * @param listener observer to register (must not be null ideally)
     */
    public void subscribe(NetworkListener listener) {
        // Prevent duplicates: the same listener should not receive events twice.
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Unsubscribes a listener.
     *
     * @param listener observer to remove
     * @return true if the listener was registered and has been removed, false otherwise
     */
    public boolean unsubscribe(NetworkListener listener) {
        return listeners.remove(listener);
    }

    /**
     * Dispatches a decoded network message to all subscribed listeners.
     *
     * Routing is done by the runtime type of {@code msg}:
     *  - ConnectionInitMsg  -> onConnectionRequest
     *  - DisconnectionMsg   -> onDisconnection
     *  - PseudoRequestMsg   -> onPseudoChangeRequest
     *  - AcceptanceMsg      -> onConnectionAcceptance
     *  - RejectionMsg       -> onConnectionRejection
     *  - ValidPseudoMsg     -> onValidPseudo
     *  - InvalidPseudoMsg   -> onInvalidPseudo
     *
     * @param msg decoded discovery message to dispatch
     */
    public void notify(NetworkMsg msg) {
        // Broadcast to all observers (simple model).
        for (NetworkListener l : listeners) {

            // Pattern matching switch on sealed message hierarchy.
            // Each case calls the corresponding method on the listener.
            switch (msg) {
                case ConnectionInitMsg e -> l.onConnectionRequest(e);
                case DisconnectionMsg e -> l.onDisconnection(e);
                case PseudoRequestMsg e -> l.onPseudoChangeRequest(e);
                case AcceptanceMsg e -> l.onConnectionAcceptance(e);
                case RejectionMsg e -> l.onConnectionRejection(e);
                case ValidPseudoMsg e -> l.onValidPseudo(e);
                case InvalidPseudoMsg e -> l.onInvalidPseudo(e);
            }
        }
    }

    public void notifyNewUserAccepted(ConnectionInitMsg msg){
        for (NetworkListener l: listeners){
            l.onNewUserAccepted(msg);
        }
    }

    public void notifyNewUserDenied(ConnectionInitMsg msg){
        for (NetworkListener l: listeners){
            l.onNewUserDenied(msg);
        }
    }

    public void notifyPseudoValidated(PseudoRequestMsg msg){
        for (NetworkListener l: listeners){
            l.onPseudoValidated(msg);
        }
    }

    public void notifyPseudoDenied(PseudoRequestMsg msg){
        for (NetworkListener l: listeners){
            l.onPseudoDenied(msg);
        }
    }
}
