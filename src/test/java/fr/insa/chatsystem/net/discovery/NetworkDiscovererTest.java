package fr.insa.chatsystem.net.discovery;

import fr.insa.chatsystem.net.discovery.core.NetworkDiscoverer;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class NetworkDiscovererTest {

    private static final InetAddress LOCALHOST = resolveLocalhost();

    @Test
    void connectAs_whenAlone_addsHostUserAndSetsConnected() throws Exception {
        ContactList contactList = resetContactList();
        int port = freeUdpPort();
        InetAddress localhost = localhost();
        User host = new User(UUID.randomUUID(), "host", User.Type.HOST, localhost, port);
        assertNotNull(contactList);
        contactList.setHostUser(host);

        NetworkDiscoverer discoverer = new NetworkDiscoverer(host, LogManager.getLogger("test"), port);
        try {
            boolean connected = discoverer.connectAs("alice");

            assertTrue(connected, "connectAs should succeed when no peers respond");
            assertTrue(discoverer.isConnected(), "Discovery state should be CONNECTED after timeout");

            User stored = contactList.getUserFromUUID(host.getID());
            assertNotNull(stored, "Host user should be registered in ContactList");
            assertEquals("alice", stored.getPseudo(), "Host pseudo should match requested pseudo");
        } finally {
            try {
                discoverer.disconnect();
                discoverer.close();
            } finally {
                resetContactList();
            }
        }
    }

    private static ContactList resetContactList() {
        try {
            java.lang.reflect.Field instanceField = ContactList.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to reset ContactList singleton", e);
        }
        return ContactList.getInstance();
    }

    private static InetAddress resolveLocalhost() {
        try {
            return InetAddress.getByName("127.0.0.1");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to resolve localhost", e);
        }
    }

    private static int freeUdpPort() throws IOException {
        try (DatagramSocket socket = new DatagramSocket(0, LOCALHOST)) {
            return socket.getLocalPort();
        }
    }

    private static InetAddress localhost() throws Exception {
        return InetAddress.getByName("127.0.0.1");
    }

}
