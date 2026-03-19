package fr.insa.chatsystem.net.transport;

import java.io.IOException;
import java.net.*;

/**
 * Service responsible for sending UDP datagrams.
 * <p>
 * This class can either own its own {@link DatagramSocket} (when created with a port)
 * or reuse an existing shared socket (when created with a socket).
 */
public class SenderUDP {

    /** DatagramSocket used to send datagrams. */
    private DatagramSocket socket;

    /** Indicates whether this sender is logically open. */
    private boolean isOpened = false;

    /** Indicates whether this instance owns the socket and may close it. */
    private final boolean ownsSocket;

    /* -----------------------------------Constructors---------------------------------------------- */

    /**
     * Instantiates a new {@code SenderUDP} by binding a new {@link DatagramSocket}
     * on the specified port. In this mode, the {@code SenderUDP} owns the socket
     * and is allowed to close it.
     *
     * @param port port on which the socket will be bound
     * @throws SocketException if the port is invalid or binding fails
     */
    public SenderUDP(int port) throws SocketException {
        this.socket = new DatagramSocket(port);
        this.ownsSocket = true;
    }

    /**
     * Instantiates a new {@code SenderUDP} by reusing an existing {@link DatagramSocket}.
     * In this mode, the {@code SenderUDP} does not own the socket and will not close it.
     *Discoverer
     * @param socket {@link DatagramSocket} used to send messages
     * @throws IllegalArgumentException if {@code socket} is {@code null}
     */
    public SenderUDP(DatagramSocket socket) {
        if (socket == null) {
            throw new IllegalArgumentException("socket cannot be null");
        }
        this.socket = socket;
        this.ownsSocket = false;
    }

    /*--------------------------------Getters--------------------------------------------*/

    /**
     * Returns the local port of the underlying socket.
     *
     * @return local port number
     */
    public int getPort() {
        return socket.getLocalPort();
    }

    /**
     * Indicates whether this sender is logically opened.
     *
     * @return {@code true} if opened, {@code false} otherwise
     */
    public boolean isOpened() {
        return isOpened;
    }

    /*---------------------------------- Methods-----------------------------------------------*/

    /**
     * Opens the sender for use.
     * <p>
     * When the socket is owned by this instance and already closed,
     * this method will throw a {@link SocketException}.
     *
     * @throws SocketException if the owned socket is closed or cannot be used
     */
    public void open() throws SocketException {
        if (socket == null) {
            throw new SocketException("Socket is null");
        }
        if (socket.isClosed() && ownsSocket) {
            throw new SocketException("Owned socket is already closed");
        }
        this.isOpened = true;
    }

    /**
     * Closes the sender.
     * <p>
     * If the sender owns the socket, the underlying socket is closed.
     * Otherwise, only the logical state is updated.
     */
    public void close() {
        if (ownsSocket && socket != null && !socket.isClosed()) {
            socket.close();
        }
        this.isOpened = false;
    }

    /**
     * Sends a unicast message to the specified address and port.
     *
     * @param message       message payload as a {@link String}
     * @param targetAddress target IP address
     * @param targetPort    target UDP port
     * @throws IOException if an I/O error occurs or the sender is not opened
     */
    public void sendMessage(String message, InetAddress targetAddress, int targetPort) throws IOException {
        if (!isOpened) {
            throw new IOException("[SenderUDP] Sender is not opened");
        }
        byte[] buffer = message.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, targetAddress, targetPort);
        socket.send(packet);
    }

    /**
     * Broadcasts a message on the specified port using the global broadcast address
     * {@code 255.255.255.255}.
     *
     * @param message    message payload as a {@link String}
     * @param targetPort target UDP port
     * @throws IOException if an I/O error occurs or the sender is not opened
     */
    public void BroadcastMessage(String message, int targetPort) throws IOException {
        if (!isOpened) {
            throw new IOException("[SenderUDP] Sender is not opened");
        }
        socket.setBroadcast(true);
        byte[] buffer = message.getBytes();

        InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, targetPort);
        socket.send(packet);
    }
}
