package fr.insa.chatsystem.net.transport.udp;

import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.List;

/**
 * Central event dispatcher for the UDP transport layer.
 * <p>
 * This manager implements the Observer pattern: it keeps track of all
 * {@link UDPListerner} instances and notifies them when a UDP datagram
 * is sent or received by the underlying UDP service.
 * </p>
 */
public class UDPEventManager {

    /*-------------------------------------------------SINGLETON------------------------------------------------------*/

    /** Singleton instance of the event manager. */
    private static final UDPEventManager INSTANCE = new UDPEventManager();

    /**
     * Returns the singleton instance of the UDP event manager.
     *
     * @return the global {@code UDPEventManager} instance
     */
    public static UDPEventManager getInstance() {
        return INSTANCE;
    }

    /*-------------------------------------------------EVENTS-METHODS-------------------------------------------------*/

    /**
     * Registered listeners that will be notified when a UDP datagram
     * is sent or received.
     */
    private final List<UDPListerner> listeners;

    /**
     * Creates a new {@code UDPEventManager}.
     * <p>
     * The constructor is public mainly for testing purposes. In production
     * code, {@link #getInstance()} should be preferred to obtain the
     * singleton instance.
     * </p>
     */
    public UDPEventManager() {
        this.listeners = new ArrayList<>();
    }

    /**
     * Registers a new listener to receive UDP events.
     * <p>
     * If the listener is already registered, this method does nothing.
     * </p>
     *
     * @param newListener the listener to register; ignored if already present
     */
    public void subscribe(UDPListerner newListener) {
        if (listeners.contains(newListener)) {
            return;
        }
        listeners.add(newListener);
    }

    /**
     * Unregisters a listener so it no longer receives UDP events.
     *
     * @param listener the listener to remove
     * @return {@code true} if the listener was registered and has been removed,
     *         {@code false} otherwise
     */
    public boolean unsubscribe(UDPListerner listener) {
        return listeners.remove(listener);
    }

    /**
     * Notifies all registered listeners that a UDP datagram has been received.
     *
     * @param msg the received {@link DatagramPacket}
     */
    public void notifyMsgReceived(DatagramPacket msg) {
        for (UDPListerner l : listeners) {
            l.onMsgReceived(msg);
        }
    }

    /**
     * Notifies all registered listeners that a UDP datagram has been sent.
     *
     * @param msg the sent {@link DatagramPacket}
     */
    public void notifyMsgSent(DatagramPacket msg) {
        for (UDPListerner l : listeners) {
            l.onMsgSent(msg);
        }
    }
}
