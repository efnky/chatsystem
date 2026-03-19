package fr.insa.chatsystem.net.discovery.core;

import fr.insa.chatsystem.net.discovery.config.DiscoveryConfig;
import fr.insa.chatsystem.net.discovery.events.*;
import fr.insa.chatsystem.net.discovery.User;
import fr.insa.chatsystem.net.discovery.views.DiscoveryView;
import fr.insa.chatsystem.net.transport.udp.controllers.InterfaceUDP;

import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

/**
 * Facade of the discovery module.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Start/stop the UDP discovery transport.</li>
 *   <li>Expose high-level operations: connect, disconnect, change pseudo.</li>
 *   <li>Route incoming UDP packets to {@link NetworkMsg} handlers.</li>
 * </ul>
 *
 * <p>This class assumes the discovery workflow is implemented as a state machine
 * via {@link DiscoveryState} and {@link DiscoveryContext}.</p>
 */
public final class NetworkDiscoverer implements AutoCloseable {

    /** Default UDP port used by the discovery protocol. */
    public static final int DEFAULT_DISCOVERY_PORT = 8888;

    /** Connection handshake timeout (ms). */
    private static final long CONNECT_TIMEOUT_MS = 500;

    /** Pseudo change request timeout (ms). */
    private static final long PSEUDO_TIMEOUT_MS = 500;

    private final int discoveryPort;
    private final DiscoveryContext cxt;
    private final Logger logger;

    /**
     * Builds the discovery facade using the default discovery port.
     *
     * @param hostUser local user identity (must not be null)
     * @param logger module logger (must not be null)
     */
    public NetworkDiscoverer(User hostUser, Logger logger) {
        this(hostUser, logger, DEFAULT_DISCOVERY_PORT);
    }

    /**
     * Builds the discovery facade.
     *
     * @param hostUser local user identity (must not be null)
     * @param logger module logger (must not be null)
     * @param discoveryPort UDP port used for discovery (must be in [1..65535])
     */
    public NetworkDiscoverer(User hostUser, Logger logger, int discoveryPort) {
        this.discoveryPort = validatePort(discoveryPort);
        this.logger = Objects.requireNonNull(logger, "logger");

        Objects.requireNonNull(hostUser, "hostUser");

        // Transport is created first, then injected into the context.
        InterfaceUDP<NetworkMsgType> transport = new InterfaceUDP<>(this.discoveryPort) {

            @Override
            public void onMsgReceived(DatagramPacket packet) {
                // Defensive: never let an exception kill the receiver thread.
                try {
                    handleIncomingPacket(packet);
                } catch (Exception e) {
                    NetworkDiscoverer.this.logger.log(
                            Level.WARN,
                            "Unhandled exception while processing incoming discovery packet",
                            e
                    );
                }
            }

            @Override
            public void onMsgSent(DatagramPacket msg) {
                // Intentionally empty (could be used for metrics/tracing).
            }
        };

        // Create context (wiring all dependencies in one place).
        this.cxt = DiscoveryConfig.createDiscoveryContext(transport, hostUser, this.logger);
        this.cxt.getContactList().setHostUser(hostUser);

        // Initialize UI / view layer (if your architecture wants it here).
        DiscoveryView.initialize(cxt);

        // Ignore our own packets (best-effort; transport may still receive them depending on OS/network stack).
        cxt.getTransportService().addIgnoredAddress(cxt.getHostUser().getAddress().getHostAddress());

        // Start listening.
        cxt.getTransportService().start();
    }

    /**
     * Decode + parse + dispatch one UDP packet into the discovery message layer.
     */
    private void handleIncomingPacket(DatagramPacket packet) {
        if (!cxt.getDiscoveryState().isListeningAllowed()) {
            return;
        }

        // Use a deterministic charset (UTF-8) so behavior is stable across machines.
        String json = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);

        NetworkMsg netMsg;
        try {
            netMsg = NetworkMsgFactory.fromJSONString(json);
        } catch (Exception e) {
            logger.log(Level.DEBUG, "Dropping invalid discovery message (JSON parse failed): " + json, e);
            return;
        }

        if (netMsg == null) {
            logger.log(Level.DEBUG, "Dropping null discovery message (factory returned null).");
            return;
        }

        // Delegate business logic to the message handler.
        netMsg.handle(cxt);
    }

    /**
     * Connect the local user with the requested pseudo.
     *
     * <p>Workflow:
     * <ul>
     *   <li>Broadcast a {@link ConnectionInitMsg}.</li>
     *   <li>Wait for acceptance/rejection or timeout.</li>
     *   <li>On timeout: assume "alone in the network" (your current behavior).</li>
     * </ul>
     *
     * @param pseudo requested pseudo
     * @return true if connected, false otherwise
     */
    public boolean connectAs(String pseudo) {
        Objects.requireNonNull(pseudo, "pseudo");

        if (cxt.getDiscoveryState() != DiscoveryState.DISCONNECTED) {
            return false;
        }

        ConnectionInitMsg msg = new ConnectionInitMsg(
                cxt.getHostUser().getID(),
                pseudo,
                null, // broadcast target
                discoveryPort,
                cxt.getHostUser().getAddress(),
                cxt.getHostUser().getPort()
        );

        boolean response = cxt.getTransportService().broadcast(msg);
        if(!response)
            return false;
        cxt.setDiscoveryState(DiscoveryState.WAITING_CONNECTION_RESPONSE);

        boolean responded = waitWhileState(DiscoveryState.WAITING_CONNECTION_RESPONSE, CONNECT_TIMEOUT_MS);

        if (!responded) {
            // Timeout: assume we're alone. (This is your current semantic.)
            logger.log(Level.INFO, "Connection timeout: assuming we are alone in the network.");
            User host = cxt.getHostUser();
            cxt.getContactList().addUser(host.getID(), pseudo, host.getAddress(), host.getPort());
            cxt.getHostUser().setPseudo(pseudo);
            cxt.setDiscoveryState(DiscoveryState.CONNECTED);
            return true;
        }

        // Response received: state must be one of the expected terminal states.
        DiscoveryState state = cxt.getDiscoveryState();
        return switch (state) {
            case ACCEPTANCE_RECEIVED -> {
                cxt.setDiscoveryState(DiscoveryState.CONNECTED);
                cxt.getHostUser().setPseudo(pseudo);
                yield true;
            }
            case REJECTION_RECEIVED -> {
                cxt.setDiscoveryState(DiscoveryState.DISCONNECTED);
                yield false;
            }
            default -> throw new IllegalStateException(
                    "Invalid discovery state after connect response: " + state);
        };
    }

    /**
     * Disconnect from the network (best-effort).
     *
     * <p>Note: this implementation broadcasts then stops the transport immediately.</p>
     */
    public void disconnect() {
        if (cxt.getDiscoveryState() != DiscoveryState.CONNECTED) {
            return;
        }

        DisconnectionMsg msg = new DisconnectionMsg(
                cxt.getHostUser().getID(),
                null,
                discoveryPort,
                cxt.getHostUser().getAddress(),
                cxt.getHostUser().getPort()
        );

        cxt.getTransportService().broadcast(msg);
        cxt.getContactList().clear();
        cxt.setDiscoveryState(DiscoveryState.DISCONNECTED);
    }

    public boolean stop(){
        boolean success = true;
        try {
            if (cxt.getDiscoveryState() == DiscoveryState.CONNECTED) {
                disconnect();
            } else if (cxt.getDiscoveryState() != DiscoveryState.DISCONNECTED) {
                cxt.setDiscoveryState(DiscoveryState.DISCONNECTED);
            }
        } catch (Exception e) {
            logger.log(Level.WARN, "Error while disconnecting discovery", e);
            success = false;
        } finally {
            try {
                cxt.getTransportService().stop();
            } catch (Exception e) {
                logger.log(Level.WARN, "Error while stopping discovery transport", e);
                success = false;
            }
        }
        return success;
    }

    public boolean isConnected(){
        return cxt.getDiscoveryState() == DiscoveryState.CONNECTED;
    }

    /**
     * Request a pseudo change.
     *
     * @param pseudo requested pseudo
     * @return true if accepted by the network, false otherwise
     */
    public boolean changePseudo(String pseudo) {
        Objects.requireNonNull(pseudo, "pseudo");

        if (cxt.getDiscoveryState() != DiscoveryState.CONNECTED) {
            logger.log(Level.DEBUG, "Cannot change pseudo: not connected.");
            return false;
        }

        PseudoRequestMsg msg = new PseudoRequestMsg(
                cxt.getHostUser().getID(),
                pseudo,
                null,
                discoveryPort,
                cxt.getHostUser().getAddress(),
                cxt.getHostUser().getPort()
        );

        cxt.getTransportService().broadcast(msg);
        cxt.setDiscoveryState(DiscoveryState.WAITING_PSEUDO_REQUEST_RESPONSE);

        boolean responded = waitWhileState(DiscoveryState.WAITING_PSEUDO_REQUEST_RESPONSE, PSEUDO_TIMEOUT_MS);

        if (!responded) {
            // Timeout: alone in network => we can update locally.
            cxt.setDiscoveryState(DiscoveryState.CONNECTED);
            if (cxt.getContactList().size() == 1) {
                cxt.getContactList().changePseudo(cxt.getHostUser().getID(), pseudo);
                return true;
            }
            logger.log(Level.INFO, "Pseudo change timeout: no response from network.");
            return false;
        }

        DiscoveryState state = cxt.getDiscoveryState();
        cxt.setDiscoveryState(DiscoveryState.CONNECTED);

        return switch (state) {
            case VALID_PSEUDO_RECEIVED -> true;
            case INVALID_PSEUDO_RECEIVED -> false;
            default -> throw new IllegalStateException(
                    "Invalid discovery state after pseudo response: " + state);
        };
    }

    /** @return current discovery state (thread-safe). */
    public DiscoveryState getState() {
        return cxt.getDiscoveryState();
    }

    /**
     * Subscribes a listener to discovery/network events.
     *
     * <p>The given {@link NetworkListener} will be notified by the underlying event manager
     * whenever the discovery module emits an event (e.g., connection accepted/rejected,
     * user joined/left, pseudo validation results, etc.).</p>
     *
     * @param listener the listener to register (must not be null)
     * @throws NullPointerException if {@code listener} is null
     */
    public void subscribe(NetworkListener listener) {
        Objects.requireNonNull(listener, "listener");
        cxt.getEventManager().subscribe(listener);
    }

    /**
     * Unsubscribes a listener from discovery/network events.
     *
     * <p>After this call, the listener will no longer receive notifications from the discovery module.</p>
     *
     * @param listener the listener to unregister (must not be null)
     * @throws NullPointerException if {@code listener} is null
     */
    public void unsubscribe(NetworkListener listener) {
        Objects.requireNonNull(listener, "listener");
        cxt.getEventManager().unsubscribe(listener);
    }

    /**
     * Stop the transport (usable with try-with-resources).
     */
    @Override
    public void close() {
        try {
            cxt.getTransportService().stop();
        } catch (Exception e) {
            logger.log(Level.DEBUG, "Error while stopping discovery transport", e);
        }
    }

    /**
     * Wait while the context is still in a given state.
     *
     * <p>This relies on {@link DiscoveryContext#setDiscoveryState(DiscoveryState)} calling notifyAll().</p>
     *
     * @param state state to wait on
     * @param timeoutMs max wait duration
     * @return true if state changed before timeout, false if timeout expired
     */
    private boolean waitWhileState(DiscoveryState state, long timeoutMs) {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);

        synchronized (cxt) {
            while (cxt.getDiscoveryState() == state) {
                long remainingNs = deadline - System.nanoTime();
                if (remainingNs <= 0) {
                    return false;
                }
                try {
                    TimeUnit.NANOSECONDS.timedWait(cxt, remainingNs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Validates a UDP port number.
     *
     * <p>A valid port is in the range {@code 1..65535} (0 is reserved and negative values are invalid).</p>
     *
     * @param port the port number to validate
     * @return the same {@code port} value if it is valid
     * @throws IllegalArgumentException if {@code port} is not in {@code 1..65535}
     */
    private static int validatePort(int port) {
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid UDP port: " + port);
        }
        return port;
    }
}
