package fr.insa.chatsystem.net.discovery.config;

import fr.insa.chatsystem.net.discovery.core.DiscoveryContext;
import fr.insa.chatsystem.net.discovery.core.DiscoveryState;
import fr.insa.chatsystem.net.discovery.events.NetworkEventManager;
import fr.insa.chatsystem.net.discovery.events.NetworkMsgType;
import fr.insa.chatsystem.net.discovery.ContactList;
import fr.insa.chatsystem.net.discovery.User;
import fr.insa.chatsystem.net.transport.udp.controllers.InterfaceUDP;
import org.apache.logging.log4j.Logger;

/**
 * Factory / wiring class for the Discovery module.
 *
 * Goal:
 *  - Centralize the creation of a fully-initialized {@link DiscoveryContext}
 *  - Keep constructors of high-level classes clean (no "new soup" scattered everywhere)
 *
 * This is basically the "composition root" of the discovery package: it decides which concrete
 * dependencies are created and how they are assembled.
 */
public class DiscoveryConfig {

    /**
     * Creates and returns a ready-to-use {@link DiscoveryContext} for the discovery subsystem.
     *
     * Initialization decisions:
     *  - initial state is {@link DiscoveryState#DISCONNECTED}
     *  - a fresh {@link NetworkEventManager} instance is created to manage observers/listeners
     *  - the {@link ContactList} is retrieved through its singleton (shared contact list)
     *
     * @param transportService the UDP transport abstraction used to send/receive {@link NetworkMsgType} messages
     * @param hostUser the local user (identity + address/port) that will participate in the discovery network
     * @param logger the logger used by discovery components for diagnostics
     * @return a newly created {@link DiscoveryContext} containing all dependencies and initial state
     */
    public static DiscoveryContext createDiscoveryContext(
            InterfaceUDP<NetworkMsgType> transportService,
            User hostUser,
            Logger logger
    ) {
        // The context is created with its initial state + all required dependencies.
        return new DiscoveryContext(
                DiscoveryState.DISCONNECTED,     // initial discovery state
                transportService,                // UDP transport facade
                new NetworkEventManager(),       // event manager (observer pattern)
                ContactList.getInstance(),       // shared contact list (singleton)
                hostUser,                        // identity of this host in the network
                logger                           // logging sink
        );
    }
}
