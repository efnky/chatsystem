package fr.insa.chatsystem.net.transport;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

/**
 * Service responsible for receiving UDP datagrams on a {@link DatagramSocket}.
 * <p>
 * This class provides blocking receive methods, with optional timeout and
 * address filtering.
 */
public class ReceiverUDP {

    /** Buffer used to store incoming datagram data. */
    private byte[] buf;

    /** Socket used to receive datagrams. */
    private final DatagramSocket socket;

    /*------------------------------------------------Constructor-----------------------------------------------------*/

    /**
     * Instantiates a {@code ReceiverUDP} which listens on the given {@code port}.
     *
     * @param port port on which the {@code ReceiverUDP} will listen
     * @throws IOException if the socket could not be opened or bound to the port
     */
    public ReceiverUDP(int port) throws IOException {
        this.socket = new DatagramSocket(port);
    }

    /**
     * Instantiates a {@code ReceiverUDP} which uses the given {@code socket} for listening.
     *
     * @param socket {@code DatagramSocket} used for listening
     * @throws IllegalArgumentException if {@code socket} is {@code null}
     */
    public ReceiverUDP(DatagramSocket socket) {
        if (socket == null) {
            throw new IllegalArgumentException("Socket passed in arguments is null");
        }
        this.socket = socket;
    }

    /*----------------------------------------------Getters-------------------------------------------------*/

    /**
     * Returns the local port on which the underlying socket is bound.
     *
     * @return local port number
     */
    public int getPort() {
        return socket.getLocalPort();
    }

    /**
     * Returns the underlying {@link DatagramSocket}.
     *
     * @return socket used for receiving datagrams
     */
    public DatagramSocket getSocket() {
        return socket;
    }

    /*----------------------------------------------------Methods-----------------------------------------------------*/

    /**
     * Blocks until a datagram is received on the socket then returns the
     * {@link DatagramPacket} filled with the received data.
     *
     * @return the packet received by the socket
     * @throws IOException if an I/O error occurs during reception
     */
    public DatagramPacket receive() throws IOException {
        // Allocate a buffer large enough for most UDP payloads
        buf = new byte[65535];
        DatagramPacket inPacket = new DatagramPacket(buf, buf.length);
        socket.receive(inPacket);
        return inPacket;
    }

    /**
     * Blocks until a datagram is received on the socket, then returns the packet,
     * unless it originates from the given {@code ignoredAddress}, in which case
     * {@code null} is returned.
     *
     * @param ignoredAddress {@link InetAddress} to ignore, may be {@code null}
     * @return the received {@link DatagramPacket}, or {@code null} if the packet
     *         was sent by {@code ignoredAddress}
     * @throws IOException if an I/O error occurs during reception
     */
    public DatagramPacket receive(InetAddress ignoredAddress) throws IOException {
        DatagramPacket inPacket = receive();
        if (ignoredAddress != null && inPacket.getAddress().equals(ignoredAddress)) {
            return null;
        }
        return inPacket;
    }

    /**
     * Receives a datagram within the specified timeout.
     * <p>
     * The socket timeout is restored to infinite (0) after the call, regardless
     * of success or failure.
     *
     * @param timeout timeout in milliseconds
     * @return the received {@link DatagramPacket}
     * @throws SocketTimeoutException if the timeout expires before a packet is received
     * @throws IOException            if {@link DatagramSocket#receive(DatagramPacket)} fails
     */
    public DatagramPacket receiveTimeOut(int timeout) throws IOException {
        socket.setSoTimeout(timeout);
        try {
            return this.receive();
        } finally {
            resetTimeOut();
        }
    }

    /**
     * Receives a datagram within the specified timeout, returning {@code null}
     * if the packet originates from {@code ignoredAddress}.
     * <p>
     * The socket timeout is restored to infinite (0) after the call, regardless
     * of success or failure.
     *
     * @param timeout        timeout in milliseconds
     * @param ignoredAddress {@link InetAddress} to ignore, may be {@code null}
     * @return the received {@link DatagramPacket}, or {@code null} if the packet
     *         was sent by {@code ignoredAddress}
     * @throws SocketTimeoutException if the timeout expires before a packet is received
     * @throws IOException            if {@link DatagramSocket#receive(DatagramPacket)} fails
     */
    public DatagramPacket receiveTimeOut(int timeout, InetAddress ignoredAddress) throws IOException {
        socket.setSoTimeout(timeout);
        try {
            return this.receive(ignoredAddress);
        } finally {
            resetTimeOut();
        }
    }

    /**
     * Resets the socket timeout to infinite (0).
     * <p>
     * Any exception is printed to {@link System#err}.
     */
    public void resetTimeOut() {
        try {
            socket.setSoTimeout(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
