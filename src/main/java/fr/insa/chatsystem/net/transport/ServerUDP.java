package fr.insa.chatsystem.net.transport;

import fr.insa.chatsystem.net.transport.udp.IOContext;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * UDP server responsible for listening for incoming datagrams on a shared
 * {@link java.net.DatagramSocket} and notifying listeners through the
 * {@link fr.insa.chatsystem.net.transport.udp.UDPEventManager}.
 * <p>
 * The server runs in a dedicated thread via {@link #run()}.
 */
public final class ServerUDP implements Runnable {

    /** Flag indicating whether the server should keep listening. */
    private volatile boolean listening;

    /** Service used to receive incoming datagrams. */
    private final ReceiverUDP receiver;

    /** IO context providing access to the socket and the event manager. */
    private final IOContext ioContext;

    /** Set of IP addresses (string form) to be ignored by the server. */
    private final Set<String> ignoredAddresses;

    /**
     * Creates a {@code ServerUDP} using the socket and manager from the given {@link IOContext}.
     *
     * @param ioCxt IO context providing the shared socket and the event manager
     */
    public ServerUDP(IOContext ioCxt) {
        this.receiver = new ReceiverUDP(ioCxt.getSocket());
        this.ioContext = ioCxt;
        this.ignoredAddresses = Collections.synchronizedSet(new HashSet<>());
    }

    /**
     * Starts the listening loop. This method blocks until {@link #stop()} is called
     * and the underlying socket is closed, or an unrecoverable I/O error occurs.
     */
    public void listen() {
        this.listening = true;
        System.out.println("[CONNECTION] Listening for new connection!");

        while (listening) {
            try {
                DatagramPacket packet = receiver.receive();
                String addr = packet.getAddress().getHostAddress();

                // Skip packets originating from ignored addresses
                if (ignoredAddresses.contains(addr)) {
                    continue;
                }

                ioContext.getManager().notifyMsgReceived(packet);
            } catch (SocketException e) {
                if (listening) {
                    System.out.println("[SERVER-UDP] Socket error, stopping listening: " + e.getMessage());
                }
                listening = false;
            } catch (IOException e) {
                System.out.println("[SERVER-UDP] I/O error, stopping listening: " + e.getMessage());
                listening = false;
            }
        }
    }

    /**
     * Requests the server to stop listening.
     * <p>
     * This method only updates the internal flag. The listening loop will effectively
     * stop once the current blocking receive call is unblocked (e.g. by closing the socket).
     */
    public void stop() {
        listening = false;
    }

    /**
     * Returns whether the server is currently in the listening state.
     *
     * @return {@code true} if listening, {@code false} otherwise
     */
    public boolean isListening() {
        return listening;
    }

    /**
     * Adds an IP address (string form, e.g. "192.168.0.10") to the ignore list.
     * Packets originating from this address will be skipped.
     *
     * @param address IP address to ignore, may be {@code null}
     */
    public void addIgnoredAddress(String address) {
        if (address != null) {
            ignoredAddresses.add(address);
        }
    }

    /**
     * Removes an IP address from the ignore list.
     *
     * @param address IP address to remove, may be {@code null}
     */
    public void removeIgnoredAddress(String address) {
        if (address != null) {
            ignoredAddresses.remove(address);
        }
    }

    /**
     * Entry point for the server thread. Calls {@link #listen()}.
     */
    @Override
    public void run() {
        this.listen();
    }
}
