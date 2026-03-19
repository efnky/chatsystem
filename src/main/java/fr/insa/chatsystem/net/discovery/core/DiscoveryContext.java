package fr.insa.chatsystem.net.discovery.core;

import fr.insa.chatsystem.net.discovery.events.NetworkEventManager;
import fr.insa.chatsystem.net.discovery.events.NetworkMsgType;
import fr.insa.chatsystem.net.discovery.ContactList;
import fr.insa.chatsystem.net.discovery.User;
import fr.insa.chatsystem.net.transport.udp.controllers.InterfaceUDP;

import java.util.Objects;
import org.apache.logging.log4j.Logger;

/**
 * Shared context for the Discovery module.
 *
 * <p>Purpose:
 * <ul>
 *   <li>Centralize long-lived dependencies (transport, event manager, logger).</li>
 *   <li>Centralize shared mutable state (the current {@link DiscoveryState}).</li>
 * </ul>
 *
 * <p>Design notes:
 * <ul>
 *   <li>All dependencies are immutable (final) to keep wiring stable.</li>
 *   <li>The only mutable field is {@code discoveryState} and is protected with a monitor.</li>
 * </ul>
 */
public final class DiscoveryContext {

    /** Current state of the discovery workflow. Only mutable field of this context. */
    private volatile DiscoveryState discoveryState;

    /** UDP transport layer used by the discovery module to send/receive {@link NetworkMsgType} messages. */
    private final InterfaceUDP<NetworkMsgType> transportService;

    /** Observer/dispatcher used to notify high-level discovery events (connection, disconnection, etc.). */
    private final NetworkEventManager eventManager;

    /** Known contacts in the discovered network (shared model). */
    private final ContactList contactList;

    /** Local user running the discovery (identity, pseudo, etc.). */
    private final User hostUser;

    /** Module logger (injected so you can configure handlers/levels outside this package). */
    private final Logger logger;

    /**
     * Builds a new discovery context.
     *
     * @param discoveryState initial discovery state (must not be null)
     * @param transportService UDP transport implementation (must not be null)
     * @param eventManager event dispatcher (must not be null)
     * @param contactList shared contact list (must not be null)
     * @param hostUser local user (must not be null)
     * @param logger logger to use (must not be null)
     */
    public DiscoveryContext(
            DiscoveryState discoveryState,
            InterfaceUDP<NetworkMsgType> transportService,
            NetworkEventManager eventManager,
            ContactList contactList,
            User hostUser,
            Logger logger
    ) {
        // Fail fast: a context with missing dependencies is a ticking time bomb.
        this.discoveryState = Objects.requireNonNull(discoveryState, "discoveryState");
        this.transportService = Objects.requireNonNull(transportService, "transportService");
        this.eventManager = Objects.requireNonNull(eventManager, "eventManager");
        this.contactList = Objects.requireNonNull(contactList, "contactList");
        this.hostUser = Objects.requireNonNull(hostUser, "hostUser");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    /** @return the UDP transport service used by discovery. */
    public InterfaceUDP<NetworkMsgType> getTransportService() {
        return transportService;
    }

    /** @return the event manager used to publish discovery events. */
    public NetworkEventManager getEventManager() {
        return eventManager;
    }

    /** @return the shared contact list (mutable model owned by discovery). */
    public ContactList getContactList() {
        return contactList;
    }

    /** @return the local/host user. */
    public User getHostUser() {
        return hostUser;
    }

    /** @return the logger for the discovery module. */
    public Logger getLogger() {
        return logger;
    }

    /**
     * Thread-safe read of the discovery state.
     * <p>Uses {@code volatile} to guarantee visibility between threads.</p>
     */
    public DiscoveryState getDiscoveryState() {
        return discoveryState;
    }

    /**
     * Thread-safe write of the discovery state.
     *
     * @param state new state (must not be null)
     */
    public synchronized void setDiscoveryState(DiscoveryState state) {
        this.discoveryState = Objects.requireNonNull(state, "state");
    }
}
