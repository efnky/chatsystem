package fr.insa.chatsystem.net.transport.udp.controllers;

import fr.insa.chatsystem.net.message.NetMsg;
import fr.insa.chatsystem.net.message.IMsgType;
import fr.insa.chatsystem.net.transport.udp.IOContext;
import fr.insa.chatsystem.net.transport.udp.UDPListerner;
import fr.insa.chatsystem.net.transport.SenderUDP;
import fr.insa.chatsystem.net.transport.ServerUDP;

import java.io.IOException;

/**
 * High-level UDP interface that ties together a shared {@link java.net.DatagramSocket},
 * a receiving server thread and a sender service.
 * <p>
 * Implementations must provide concrete behavior for {@link UDPListerner} callbacks.
 *
 * @param <T> message type enum implementing {@link IMsgType}
 */
public abstract class InterfaceUDP<T extends IMsgType> implements UDPListerner {

    /** UDP port used for both send and receive operations. */
    protected final int port;

    /** IO context holding the shared DatagramSocket and the event manager. */
    protected final IOContext ioCxt;

    /** UDP server responsible for listening to incoming datagrams. */
    protected final ServerUDP serverUDP;

    /** UDP sender responsible for sending datagrams using the shared socket. */
    protected final SenderUDP senderUDP;

    /** Thread running the server listening loop. */
    protected Thread listeningThread;

    /**
     * Creates a new UDP interface bound to the given port.
     * <p>
     * This constructor creates an {@link IOContext}, a {@link ServerUDP} and a {@link SenderUDP}
     * sharing the same underlying {@link java.net.DatagramSocket}. It also subscribes this
     * instance to the {@link fr.insa.chatsystem.net.transport.udp.UDPEventManager}.
     *
     * @param port UDP port to bind for both sending and receiving
     */
    public InterfaceUDP(int port) {
        this.port = port;
        this.ioCxt = new IOContext(port);

        this.serverUDP = new ServerUDP(ioCxt);
        this.senderUDP = new SenderUDP(ioCxt.getSocket());


        ioCxt.getManager().subscribe(this);
    }

    /**
     * Starts the UDP services: opens the sender and starts the listening thread.
     *
     * @throws RuntimeException if the underlying sender fails to open
     */
    public void start() {
        try {
            senderUDP.open();
        } catch (IOException e) {
            throw new RuntimeException("Failed to open UDP sender", e);
        }

        this.listeningThread = new Thread(serverUDP, "udp-listener-" + port);
        listeningThread.start();
    }

    /**
     * Stops the UDP services.
     * <ul>
     *     <li>Signals the server to stop listening</li>
     *     <li>Closes the shared socket to unblock any pending receive</li>
     *     <li>Marks the sender as closed</li>
     *     <li>Interrupts the listening thread as a last resort</li>
     * </ul>
     */
    public void stop() {
        serverUDP.stop();
        if (!ioCxt.getSocket().isClosed()) {
            ioCxt.getSocket().close();
        }
        senderUDP.close();
        if (listeningThread != null) {
            listeningThread.interrupt();
            try {
                listeningThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Sends a unicast message using the underlying {@link SenderUDP}.
     *
     * @param msg message to send, containing target address and port
     * @return {@code true} if the message was successfully queued to the socket,
     *         {@code false} if an I/O error occurred
     */
    public boolean send(NetMsg<T> msg) {
        try {
            senderUDP.sendMessage(
                    msg.toJSONString(),
                    msg.getTargetAddress(),
                    msg.getTargetPort()
            );
            return true;
        } catch (IOException e) {
            // Could be logged if needed
            return false;
        }
    }

    /**
     * Broadcasts a message on the target port specified by the {@link NetMsg}.
     * <p>
     * The {@link NetMsg} is expected to have a valid target port; the target address
     * is ignored and replaced with the global broadcast address (255.255.255.255).
     *
     * @param msg message to broadcast
     * @return {@code true} if the message was successfully queued to the socket,
     *         {@code false} if an I/O error occurred
     */
    public boolean broadcast(NetMsg<T> msg) {
        try {
            senderUDP.BroadcastMessage(
                    msg.toJSONString(),
                    msg.getTargetPort()
            );
            return true;
        } catch (IOException e) {
            // Could be logged if needed
            System.out.println("BroadCast failed: " + e);
            return false;
        }
    }

    /**
     * Adds an IP address to the ignore list of the listener of the listener.
     *
     * @param address IP address to remove, may be {@code null}
     */
    public void addIgnoredAddress(String address) {
        if (address != null) {
            serverUDP.addIgnoredAddress(address);
        }
    }

    /**
     * Removes an IP address from the ignore list of the listener.
     *
     * @param address IP address to remove, may be {@code null}
     */
    public void removeIgnoredAddress(String address) {
        if (address != null) {
            serverUDP.removeIgnoredAddress(address);
        }
    }

    public boolean isStarted(){
        return this.isStarted();
    }
}
