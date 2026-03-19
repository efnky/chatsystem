package fr.insa.chatsystem.net.transport;

// package fr.insa.chatsystem.net.transport.udp.services;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SenderUDP}.
 * These tests treat SenderUDP as a black box:
 * - We only use its public API (constructors, open/close, sendMessage, BroadcastMessage, isOpened, getPort).
 * - We do NOT rely on any internal fields or implementation details.
 * - For networking, we use a test double (CapturingDatagramSocket) instead of real network I/O.
 */
class SenderUDPTest {

    /**
     * Test double for DatagramSocket used to capture outgoing packets
     * instead of sending them over the real network.
     */
    private static class CapturingDatagramSocket extends DatagramSocket {

        /** Last packet "sent" through this socket. */
        private DatagramPacket lastPacket;

        /** Flag indicating whether close() has been invoked. */
        private boolean closeCalled = false;

        /**
         * Creates a capturing socket bound to an ephemeral port.
         *
         * @throws SocketException if the socket cannot be created
         */
        CapturingDatagramSocket() throws SocketException {
            super(0); // bind to an ephemeral port chosen by the OS
        }

        @Override
        public synchronized void send(DatagramPacket p) {
            // Capture the packet instead of performing real network I/O.
            byte[] copy = new byte[p.getLength()];
            System.arraycopy(p.getData(), p.getOffset(), copy, 0, p.getLength());
            this.lastPacket = new DatagramPacket(copy, copy.length, p.getAddress(), p.getPort());
        }

        @Override
        public synchronized void close() {
            // Track that close() has been called.
            closeCalled = true;
            super.close();
        }

        DatagramPacket getLastPacket() {
            return lastPacket;
        }

        boolean isCloseCalled() {
            return closeCalled;
        }
    }

    // ---------------------------------------------------------------------
    // Constructor behavior
    // ---------------------------------------------------------------------

    @Test
    void constructor_withNullSocket_shouldThrowIllegalArgumentException() {

        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> new SenderUDP(null),
                "Passing a null DatagramSocket to the constructor must throw IllegalArgumentException");
    }

    @Test
    void constructor_withPort_shouldStartClosedAndExposeAPort() throws Exception {
        // Arrange
        int port = 0; // letting the OS pick an ephemeral port is safer for tests

        // Act
        SenderUDP sender = new SenderUDP(port);

        // Assert
        // At construction time, the sender should not be logically opened.
        assertFalse(sender.isOpened(),
                "Sender should not be opened immediately after construction");
        // getPort() should return a valid UDP port number (not 0 and in range).
        assertTrue(sender.getPort() > 0 && sender.getPort() <= 65535,
                "getPort() should return a valid local UDP port");
    }

    @Test
    void constructor_withExistingSocket_shouldStartClosed() throws Exception {
        // Arrange
        CapturingDatagramSocket socket = new CapturingDatagramSocket();

        // Act
        SenderUDP sender = new SenderUDP(socket);

        // Assert
        assertFalse(sender.isOpened(),
                "Sender created with an existing socket should start in closed state");
        assertEquals(socket.getLocalPort(), sender.getPort(),
                "getPort() should reflect the underlying socket's local port");
    }

    // ---------------------------------------------------------------------
    // open() / close() behavior
    // ---------------------------------------------------------------------

    @Test
    void open_shouldMarkSenderAsOpened_forPortConstructor() throws Exception {
        // Arrange
        SenderUDP sender = new SenderUDP(0);

        // Act
        sender.open();

        // Assert
        assertTrue(sender.isOpened(), "open() should mark the sender as opened");
    }

    @Test
    void close_shouldMarkSenderAsClosed_forPortConstructor() throws Exception {
        // Arrange
        SenderUDP sender = new SenderUDP(0);
        sender.open();

        // Act
        sender.close();

        // Assert
        assertFalse(sender.isOpened(), "close() should mark the sender as closed");
    }

    @Test
    void open_shouldMarkSenderAsOpened_forSocketConstructor() throws Exception {
        // Arrange
        CapturingDatagramSocket socket = new CapturingDatagramSocket();
        SenderUDP sender = new SenderUDP(socket);

        // Act
        sender.open();

        // Assert
        assertTrue(sender.isOpened(), "open() should mark the sender as opened");
    }

    @Test
    void close_withExternalSocket_shouldNotCloseProvidedSocket() throws Exception {
        // Arrange
        CapturingDatagramSocket socket = new CapturingDatagramSocket();
        SenderUDP sender = new SenderUDP(socket);
        sender.open();

        // Act
        sender.close();

        // Assert
        // According to the contract, SenderUDP must NOT close an externally provided socket.
        assertFalse(socket.isCloseCalled(),
                "SenderUDP.close() must not call close() on an externally provided DatagramSocket");
        assertFalse(sender.isOpened(),
                "Sender should be logically closed after close()");
    }

    // ---------------------------------------------------------------------
    // sendMessage() behavior
    // ---------------------------------------------------------------------

    @Test
    void sendMessage_withoutOpen_shouldThrowIOException() throws Exception {
        // Arrange
        CapturingDatagramSocket socket = new CapturingDatagramSocket();
        SenderUDP sender = new SenderUDP(socket);

        InetAddress targetAddress = InetAddress.getByName("127.0.0.1");
        int targetPort = 12345;
        String message = "payload";

        // Act + Assert
        assertThrows(IOException.class,
                () -> sender.sendMessage(message, targetAddress, targetPort),
                "Calling sendMessage() while sender is not opened should throw IOException");
        assertNull(socket.getLastPacket(),
                "No packet should be sent when sender is not opened");
    }

    @Test
    void sendMessage_withOpen_shouldPopulatePacketWithCorrectAddressPortAndPayload() throws Exception {
        // Arrange
        CapturingDatagramSocket socket = new CapturingDatagramSocket();
        SenderUDP sender = new SenderUDP(socket);
        sender.open();

        InetAddress targetAddress = InetAddress.getByName("192.0.2.1"); // test address (documentation range)
        int targetPort = 4242;
        String message = "hello-sender";

        // Act
        sender.sendMessage(message, targetAddress, targetPort);

        // Assert
        DatagramPacket sent = socket.getLastPacket();
        assertNotNull(sent,
                "A packet should be captured when sendMessage() is called on an opened sender");
        assertEquals(targetPort, sent.getPort(),
                "Packet must be addressed to the target port given to sendMessage()");
        assertEquals(targetAddress, sent.getAddress(),
                "Packet must be addressed to the target address given to sendMessage()");
        assertEquals(message,
                new String(sent.getData(), 0, sent.getLength()),
                "Packet payload must match the message passed to sendMessage()");
    }

    @Test
    void sendMessage_afterClose_shouldThrowIOException() throws Exception {
        // Arrange
        CapturingDatagramSocket socket = new CapturingDatagramSocket();
        SenderUDP sender = new SenderUDP(socket);
        sender.open();
        sender.close();

        InetAddress targetAddress = InetAddress.getByName("127.0.0.1");
        int targetPort = 9999;
        String message = "after-close";

        // Act + Assert
        assertThrows(IOException.class,
                () -> sender.sendMessage(message, targetAddress, targetPort),
                "After close(), sendMessage() should not succeed and should throw IOException");
        assertNull(socket.getLastPacket(),
                "After close(), no new packet should be captured");
    }

    // ---------------------------------------------------------------------
    // BroadcastMessage() behavior
    // ---------------------------------------------------------------------

    @Test
    void broadcastMessage_withoutOpen_shouldThrowIOException() throws Exception {
        // Arrange
        CapturingDatagramSocket socket = new CapturingDatagramSocket();
        SenderUDP sender = new SenderUDP(socket);

        String message = "broadcast-closed";
        int targetPort = 5555;

        // Act + Assert
        assertThrows(IOException.class,
                () -> sender.BroadcastMessage(message, targetPort),
                "BroadcastMessage() should throw IOException if sender is not opened");
        assertNull(socket.getLastPacket(),
                "No packet should be sent by BroadcastMessage() when sender is not opened");
    }

    @Test
    void broadcastMessage_withOpen_shouldUseBroadcastAddressAndTargetPort() throws Exception {
        // Arrange
        CapturingDatagramSocket socket = new CapturingDatagramSocket();
        SenderUDP sender = new SenderUDP(socket);
        sender.open();

        String message = "broadcast-msg";
        int targetPort = 6000;

        // Act
        sender.BroadcastMessage(message, targetPort);

        // Assert
        DatagramPacket sent = socket.getLastPacket();
        assertNotNull(sent,
                "A packet should be captured when BroadcastMessage() is called on an opened sender");

        InetAddress expectedBroadcast = InetAddress.getByName("255.255.255.255");

        assertEquals(expectedBroadcast, sent.getAddress(),
                "BroadcastMessage() must use the global broadcast address 255.255.255.255");
        assertEquals(targetPort, sent.getPort(),
                "BroadcastMessage() must set the packet's port to the target port argument");
        assertEquals(message,
                new String(sent.getData(), 0, sent.getLength()),
                "BroadcastMessage() must use the given message as packet payload");
    }
}
