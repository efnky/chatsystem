package fr.insa.chatsystem.net.transport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ReceiverUDP}.
 * These tests use a real DatagramSocket for the sender side (clientSocket)
 * and the serverSocket is wrapped by ReceiverUDP. No SenderUDP is used here
 * so that we only validate ReceiverUDP's behavior.
 */
class  ReceiverUDPTest {

    private DatagramSocket serverSocket;
    private DatagramSocket clientSocket;
    private ReceiverUDP receiver;

    @BeforeEach
    void setUp() throws Exception {
        // Arrange shared resources for each test:
        // - serverSocket bound to an ephemeral port (0)
        // - ReceiverUDP wrapping that socket
        // - clientSocket to send packets to the server
        serverSocket = new DatagramSocket(0); // OS chooses a free port
        receiver = new ReceiverUDP(serverSocket);
        clientSocket = new DatagramSocket();
    }

    @AfterEach
    void tearDown() {
        // Ensure sockets are always closed even if a test fails
        if (clientSocket != null && !clientSocket.isClosed()) {
            clientSocket.close();
        }
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
    }

    @Test
    void constructor_withNullSocket_shouldThrowIllegalArgumentException() {

        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> new ReceiverUDP(null),
                "Passing a null socket to the constructor should throw IllegalArgumentException");
    }

    @Test
    void receive_shouldReturnPacketSentToUnderlyingSocket() throws Exception {
        // Arrange
        String payload = "hello-receiver";
        byte[] data = payload.getBytes();
        int serverPort = serverSocket.getLocalPort();
        InetAddress localhost = InetAddress.getByName("127.0.0.1");

        DatagramPacket out = new DatagramPacket(
                data,
                data.length,
                localhost,
                serverPort
        );

        // Act
        // Send a packet from the client to the server
        clientSocket.send(out);

        // Receive it through ReceiverUDP
        DatagramPacket in = receiver.receive();

        // Assert
        assertNotNull(in, "Received packet should not be null");
        assertEquals(payload,
                new String(in.getData(), 0, in.getLength()),
                "Payload should match the data sent by the client");
        assertEquals(clientSocket.getLocalPort(),
                in.getPort(),
                "Source port in the received packet should be the client's local port");
        assertEquals(localhost,
                in.getAddress(),
                "Source address in the received packet should be localhost");
    }

    @Test
    void receive_withIgnoredAddress_shouldReturnNullWhenPacketFromIgnoredAddress() throws Exception {
        // Arrange
        InetAddress localhost = InetAddress.getByName("127.0.0.1");
        int serverPort = serverSocket.getLocalPort();
        String payload = "ignored";
        byte[] data = payload.getBytes();

        DatagramPacket out = new DatagramPacket(
                data,
                data.length,
                localhost,
                serverPort
        );

        // Act
        // Send from localhost which is the address we will ignore
        clientSocket.send(out);
        DatagramPacket result = receiver.receive(localhost);

        // Assert
        // By contract, when a packet comes from the ignored address, receive(InetAddress)
        // should return null instead of the packet.
        assertNull(result,
                "Packet coming from the ignored address should result in null");
    }

    @Test
    void receive_withNullIgnoredAddress_shouldBehaveLikeRegularReceive() throws Exception {
        // Arrange
        InetAddress localhost = InetAddress.getByName("127.0.0.1");
        int serverPort = serverSocket.getLocalPort();
        String payload = "not-ignored";
        byte[] data = payload.getBytes();

        DatagramPacket out = new DatagramPacket(
                data,
                data.length,
                localhost,
                serverPort
        );

        // Act
        clientSocket.send(out);
        // Passing null as ignoredAddress should not filter anything
        DatagramPacket in = receiver.receive(null);

        // Assert
        assertNotNull(in, "Packet should not be filtered when ignoredAddress is null");
        assertEquals(payload,
                new String(in.getData(), 0, in.getLength()),
                "Payload should match the data sent by the client");
    }

    @Test
    void receiveTimeOut_shouldReturnPacketIfItArrivesBeforeTimeout() throws Exception {
        // Arrange
        InetAddress localhost = InetAddress.getByName("127.0.0.1");
        int serverPort = serverSocket.getLocalPort();
        String payload = "within-timeout";
        byte[] data = payload.getBytes();

        DatagramPacket out = new DatagramPacket(
                data,
                data.length,
                localhost,
                serverPort
        );

        // Send the packet before calling receiveTimeOut, so it is already available
        clientSocket.send(out);

        // Act
        int timeoutMs = 200; // Small but sufficient timeout for local UDP
        DatagramPacket in = receiver.receiveTimeOut(timeoutMs);

        // Assert
        assertNotNull(in, "Packet should be received before timeout expires");
        assertEquals(payload,
                new String(in.getData(), 0, in.getLength()),
                "Payload should match the data sent by the client");

        // Also check that the socket timeout has been reset to 0 by receiveTimeOut
        assertEquals(0,
                serverSocket.getSoTimeout(),
                "Socket timeout should be reset to 0 after receiveTimeOut");
    }

    @Test
    void receiveTimeOut_shouldThrowSocketTimeoutExceptionWhenNoPacketArrives() throws Exception {
        // Arrange
        int timeoutMs = 100;

        // Act + Assert
        // No packet is sent: we expect a SocketTimeoutException.
        assertThrows(SocketTimeoutException.class,
                () -> receiver.receiveTimeOut(timeoutMs),
                "When no packet arrives within timeout, a SocketTimeoutException should be thrown");

        // And the timeout should be reset to 0 after the call
        assertEquals(0,
                serverSocket.getSoTimeout(),
                "Socket timeout should be reset to 0 even after a timeout exception");
    }

    @Test
    void receiveTimeOut_withIgnoredAddress_shouldReturnNullForPacketFromIgnoredAddress() throws Exception {
        // Arrange
        InetAddress localhost = InetAddress.getByName("127.0.0.1");
        int serverPort = serverSocket.getLocalPort();
        String payload = "ignored-timeout";
        byte[] data = payload.getBytes();

        DatagramPacket out = new DatagramPacket(
                data,
                data.length,
                localhost,
                serverPort
        );

        // Send from the address we will ignore
        clientSocket.send(out);

        int timeoutMs = 200;

        // Act
        DatagramPacket in = receiver.receiveTimeOut(timeoutMs, localhost);

        // Assert
        assertNull(in,
                "Packet coming from ignored address should result in null even with timeout version");
        assertEquals(0,
                serverSocket.getSoTimeout(),
                "Socket timeout should be reset to 0 after receiveTimeOut with ignored address");
    }

    @Test
    void resetTimeOut_shouldSetSocketTimeoutBackToZero() throws Exception {
        // Arrange
        serverSocket.setSoTimeout(1234);

        // Act
        receiver.resetTimeOut();

        // Assert
        assertEquals(0,
                serverSocket.getSoTimeout(),
                "resetTimeOut() must restore the socket timeout to 0 (infinite)");
    }
}
