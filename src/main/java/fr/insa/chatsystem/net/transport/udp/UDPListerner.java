package fr.insa.chatsystem.net.transport.udp;

import java.net.DatagramPacket;

/**
 * Listener interface for UDP events.
 * <p>
 * Implementations can be registered to the {@link UDPEventManager}
 * to be notified when datagrams are sent or received.
 */
public interface UDPListerner {

    /**
     * Callback invoked when the UDP service receives a datagram.
     *
     * @param msg received {@link DatagramPacket}
     */
    void onMsgReceived(DatagramPacket msg);

    /**
     * Callback invoked when the UDP service sends a datagram.
     *
     * @param msg sent {@link DatagramPacket}
     */
    void onMsgSent(DatagramPacket msg);

}
