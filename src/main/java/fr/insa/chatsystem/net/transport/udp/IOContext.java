package fr.insa.chatsystem.net.transport.udp;

import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * I/O context holding shared UDP resources used by the transport layer.
 * <p>
 * This class encapsulates:
 * <ul>
 *     <li>a shared {@link DatagramSocket} bound to a specific port</li>
 *     <li>a {@link UDPEventManager} used to notify UDP listeners</li>
 * </ul>
 */
public class IOContext {

    /** Shared DatagramSocket used for both sending and receiving. */
    private final DatagramSocket socket;

    /** Event manager used to dispatch UDP events to listeners. */
    private final UDPEventManager manager;

    /**
     * Creates an {@code IOContext} with a {@link DatagramSocket} bound to the given port.
     *
     * @param port UDP port to bind
     * @throws RuntimeException if the socket cannot be created or bound
     */
    public IOContext(int port) {
        try {
            this.socket = new DatagramSocket(port);
        } catch (SocketException e) {
            throw new RuntimeException("Failed to create DatagramSocket on port " + port, e);
        }
        this.manager = new UDPEventManager();
    }

    /**
     * Returns the shared {@link DatagramSocket}.
     *
     * @return shared UDP socket
     */
    public DatagramSocket getSocket() {
        return socket;
    }

    /**
     * Returns the UDP event manager associated with this context.
     *
     * @return {@link UDPEventManager} instance
     */
    public UDPEventManager getManager() {
        return manager;
    }
}
