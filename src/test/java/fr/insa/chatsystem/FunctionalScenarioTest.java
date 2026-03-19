package fr.insa.chatsystem;

import fr.insa.chatsystem.db.ContactRepository;
import fr.insa.chatsystem.db.MessageRepository;
import fr.insa.chatsystem.db.SQLiteDatabase;
import fr.insa.chatsystem.db.controllers.ChatroomController;
import fr.insa.chatsystem.db.events.DBEventsManager;
import fr.insa.chatsystem.db.models.Chatroom;
import fr.insa.chatsystem.db.models.Contact;
import fr.insa.chatsystem.db.models.Direction;
import fr.insa.chatsystem.db.models.Message;
import fr.insa.chatsystem.net.discovery.core.NetworkDiscoverer;
import fr.insa.chatsystem.net.discovery.events.ConnectionInitMsg;
import fr.insa.chatsystem.net.discovery.events.DisconnectionMsg;
import fr.insa.chatsystem.net.discovery.events.PseudoRequestMsg;
import fr.insa.chatsystem.net.discovery.ContactList;
import fr.insa.chatsystem.net.discovery.User;
import fr.insa.chatsystem.net.messaging.api.Messenger;
import fr.insa.chatsystem.net.messaging.models.TextMsg;
import fr.insa.chatsystem.net.transport.tcp.models.TCPFrame;
import fr.insa.chatsystem.net.transport.tcp.services.BasicFrameCodec;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class FunctionalScenarioTest {

    private static final InetAddress LOCALHOST = resolveLocalhost();

    @BeforeEach
    void resetSingletons() {
        resetContactList();
    }

    @Test
    void udpConnectionInit_addsContactToRepository() throws Exception {
        ContactList contactList = ContactList.getInstance();
        int discoveryPort = freeUdpPort();
        int messagingPort = freeTcpPort();
        User host = new User(UUID.randomUUID(), "host", User.Type.HOST, loopbackAlias(), discoveryPort);
        contactList.setHostUser(host);

        NetworkDiscoverer discoverer = new NetworkDiscoverer(host, LogManager.getLogger("test"), discoveryPort);
        SQLiteDatabase database = null;
        Messenger messenger = null;
        String session = null;
        try {
            assertTrue(discoverer.connectAs("host"));

            session = newSessionId();
            database = new SQLiteDatabase(session);
            DBEventsManager events = new DBEventsManager();
            MessageRepository messageRepository = new MessageRepository(database, events);
            ContactRepository contactRepository = new ContactRepository(database);
            messenger = new Messenger(contactList, messagingPort);
            new ChatroomController(messenger, contactRepository, messageRepository, events);

            UUID remoteId = UUID.randomUUID();
            ConnectionInitMsg msg = new ConnectionInitMsg(
                    remoteId,
                    "alice",
                    null,
                    discoveryPort,
                    LOCALHOST,
                    9999
            );
            sendUdp(msg.toJSONString(), discoveryPort);

            awaitCondition(
                    () -> contactRepository.findByUserId(remoteId).isPresent(),
                    Duration.ofSeconds(2),
                    "Contact was not persisted after UDP connection init"
            );

            Contact stored = contactRepository.findByUserId(remoteId).orElseThrow();
            assertEquals("alice", stored.getUsername());
        } finally {
            if (messenger != null) {
                messenger.close();
            }
            if (database != null) {
                database.getConnection().close();
            }
            discoverer.disconnect();
            discoverer.close();
            resetContactList();
            deleteDbFile(session);
        }
    }

    @Test
    void udpDisconnection_removesUserFromContactList() throws Exception {
        ContactList contactList = ContactList.getInstance();
        int discoveryPort = freeUdpPort();
        User host = new User(UUID.randomUUID(), "host", User.Type.HOST, loopbackAlias(), discoveryPort);
        contactList.setHostUser(host);

        NetworkDiscoverer discoverer = new NetworkDiscoverer(host, LogManager.getLogger("test"), discoveryPort);
        try {
            assertTrue(discoverer.connectAs("host"));

            UUID peerId = UUID.randomUUID();
            contactList.addUser(peerId, "bob", LOCALHOST, 9999);
            assertNotNull(contactList.getUserFromUUID(peerId));

            DisconnectionMsg msg = new DisconnectionMsg(peerId, null, discoveryPort, LOCALHOST, 9999);
            sendUdp(msg.toJSONString(), discoveryPort);

            awaitCondition(
                    () -> contactList.getUserFromUUID(peerId) == null,
                    Duration.ofSeconds(2),
                    "Peer was not removed after UDP disconnection"
            );
            assertEquals(1, contactList.size());
        } finally {
            discoverer.disconnect();
            discoverer.close();
            resetContactList();
        }
    }

    @Test
    void udpPseudoChange_updatesRepositoryWithoutDuplicate() throws Exception {
        ContactList contactList = ContactList.getInstance();
        int discoveryPort = freeUdpPort();
        int messagingPort = freeTcpPort();
        User host = new User(UUID.randomUUID(), "host", User.Type.HOST, loopbackAlias(), discoveryPort);
        contactList.setHostUser(host);

        NetworkDiscoverer discoverer = new NetworkDiscoverer(host, LogManager.getLogger("test"), discoveryPort);
        SQLiteDatabase database = null;
        Messenger messenger = null;
        String session = null;
        try {
            assertTrue(discoverer.connectAs("host"));

            session = newSessionId();
            database = new SQLiteDatabase(session);
            DBEventsManager events = new DBEventsManager();
            MessageRepository messageRepository = new MessageRepository(database, events);
            ContactRepository contactRepository = new ContactRepository(database);
            messenger = new Messenger(contactList, messagingPort);
            new ChatroomController(messenger, contactRepository, messageRepository, events);

            UUID peerId = UUID.randomUUID();
            contactList.addUser(peerId, "alice", LOCALHOST, 9999);

            PseudoRequestMsg msg = new PseudoRequestMsg(peerId, "alice2", null, discoveryPort, LOCALHOST, 9999);
            sendUdp(msg.toJSONString(), discoveryPort);

            awaitCondition(
                    () -> contactRepository.findByUserId(peerId)
                            .map(c -> "alice2".equals(c.getUsername()))
                            .orElse(false),
                    Duration.ofSeconds(2),
                    "Contact pseudo was not updated after UDP pseudo request"
            );

            List<Contact> contacts = contactRepository.findAll();
            long count = contacts.stream().filter(c -> c.getUserId().equals(peerId)).count();
            assertEquals(1, count);
        } finally {
            if (messenger != null) {
                messenger.close();
            }
            if (database != null) {
                database.getConnection().close();
            }
            discoverer.disconnect();
            discoverer.close();
            resetContactList();
            deleteDbFile(session);
        }
    }

    @Test
    void hostConnectDisconnect_updatesDiscoveryStateAndContactList() throws Exception {
        ContactList contactList = ContactList.getInstance();
        int discoveryPort = freeUdpPort();
        User host = new User(UUID.randomUUID(), "host", User.Type.HOST, loopbackAlias(), discoveryPort);
        contactList.setHostUser(host);

        try (NetworkDiscoverer discoverer = new NetworkDiscoverer(host, LogManager.getLogger("test"), discoveryPort)) {
            assertTrue(discoverer.connectAs("host"));
            assertTrue(discoverer.isConnected());

            User stored = contactList.getUserFromUUID(host.getID());
            assertNotNull(stored);
            assertEquals("host", stored.getPseudo());

            discoverer.disconnect();

            awaitCondition(
                    () -> !discoverer.isConnected(),
                    Duration.ofSeconds(2),
                    "Discovery did not transition to disconnected"
            );
            assertTrue(contactList.isEmpty());
        } finally {
            resetContactList();
        }
    }

    @Test
    void sqliteReload_persistsAcrossNewDatabaseInstance() throws Exception {
        String session = newSessionId();
        SQLiteDatabase database = new SQLiteDatabase(session);
        DBEventsManager events = new DBEventsManager();
        MessageRepository messageRepository = new MessageRepository(database, events);

        UUID contactId = UUID.randomUUID();
        UUID msgId = UUID.randomUUID();
        LocalDateTime timestamp = LocalDateTime.of(2024, 1, 2, 10, 30);
        messageRepository.saveMessage(msgId, contactId, "persist", Direction.RECEIVED, timestamp);
        database.getConnection().close();

        SQLiteDatabase reloaded = new SQLiteDatabase(session);
        MessageRepository reloadedRepo = new MessageRepository(reloaded, events);
        try {
            List<Message> messages = reloadedRepo.findMessagesByUUID(contactId);
            assertEquals(1, messages.size());
            Message restored = messages.getFirst();
            assertEquals(msgId, restored.getId());
            assertEquals("persist", restored.getContent());
            assertEquals(Direction.RECEIVED, restored.getDirection());
            assertEquals(timestamp, restored.getTimestamp());
        } finally {
            reloaded.getConnection().close();
            deleteDbFile(session);
        }
    }

    @Test
    void tcpIncomingMessage_persistsAndChatroomVisible() throws Exception {
        ContactList contactList = ContactList.getInstance();
        int messagingPort = freeTcpPort();
        User host = new User(UUID.randomUUID(), "host", User.Type.HOST, LOCALHOST, messagingPort);
        contactList.setHostUser(host);

        String session = newSessionId();
        SQLiteDatabase database = new SQLiteDatabase(session);
        DBEventsManager events = new DBEventsManager();
        MessageRepository messageRepository = new MessageRepository(database, events);
        ContactRepository contactRepository = new ContactRepository(database);
        Messenger messenger = new Messenger(contactList, messagingPort);
        ChatroomController controller = new ChatroomController(messenger, contactRepository, messageRepository, events);

        try {
            UUID peerId = UUID.randomUUID();
            contactList.addUser(peerId, "alice", LOCALHOST, 9999);
            Chatroom chatroom = controller.openChat(peerId);
            waitForTcpServer(messagingPort, Duration.ofSeconds(2));

            UUID msgId = UUID.randomUUID();
            TextMsg msg = new TextMsg(
                    msgId,
                    peerId,
                    host.getID(),
                    LOCALHOST,
                    messagingPort,
                    LOCALHOST,
                    messagingPort,
                    "hello",
                    LocalDate.of(2026, 1, 16),
                    LocalTime.of(16, 26)
            );
            sendTcpFrame(messagingPort, new TCPFrame("text", msg.toJSONString().getBytes(StandardCharsets.UTF_8)));

            awaitCondition(
                    () -> !messageRepository.findMessagesByUUID(peerId).isEmpty(),
                    Duration.ofSeconds(2),
                    "Incoming TCP message was not persisted"
            );

            chatroom.loadHistory();
            Message inMemory = chatroom.getMessages().getFirst();
            assertEquals(msgId, inMemory.getId());
            assertEquals(Direction.RECEIVED, inMemory.getDirection());

            Chatroom reloaded = new Chatroom(peerId, messageRepository);
            reloaded.loadHistory();
            assertEquals(1, reloaded.getMessages().size());
            assertEquals(msgId, reloaded.getMessages().getFirst().getId());
            assertEquals(Direction.RECEIVED, reloaded.getMessages().getFirst().getDirection());
        } finally {
            messenger.close();
            database.getConnection().close();
            resetContactList();
            deleteDbFile(session);
        }
    }

    @Test
    void tcpOutgoingMessage_persistsAsSent() throws Exception {
        ContactList contactList = ContactList.getInstance();
        int messagingPort = freeTcpPort();
        User host = new User(UUID.randomUUID(), "host", User.Type.HOST, LOCALHOST, messagingPort);
        contactList.setHostUser(host);

        String session = newSessionId();
        SQLiteDatabase database = new SQLiteDatabase(session);
        DBEventsManager events = new DBEventsManager();
        MessageRepository messageRepository = new MessageRepository(database, events);
        ContactRepository contactRepository = new ContactRepository(database);
        Messenger messenger = new Messenger(contactList, messagingPort);
        ChatroomController controller = new ChatroomController(messenger, contactRepository, messageRepository, events);

        try {
            UUID peerId = UUID.randomUUID();
            TextMsg msg = new TextMsg(
                    UUID.randomUUID(),
                    host.getID(),
                    peerId,
                    LOCALHOST,
                    messagingPort,
                    LOCALHOST,
                    messagingPort,
                    "ping",
                    LocalDate.of(2024, 1, 1),
                    LocalTime.of(12, 0)
            );
            controller.onTextMessageSent(msg);

            awaitCondition(
                    () -> !messageRepository.findMessagesByUUID(peerId).isEmpty(),
                    Duration.ofSeconds(2),
                    "Outgoing message was not persisted"
            );

            Message persisted = messageRepository.findMessagesByUUID(peerId).getFirst();
            assertEquals(Direction.SENT, persisted.getDirection());
        } finally {
            messenger.close();
            database.getConnection().close();
            resetContactList();
            deleteDbFile(session);
        }
    }

    private static void resetContactList() {
        try {
            java.lang.reflect.Field instanceField = ContactList.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to reset ContactList singleton", e);
        }
        ContactList.getInstance();
    }

    private static InetAddress loopbackAlias() {
        try {
            // Use a loopback alias to avoid UDP self-ignore in discovery.
            return InetAddress.getByName("127.0.0.2");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to resolve loopback alias", e);
        }
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

    private static int freeTcpPort() throws IOException {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static String newSessionId() {
        return "test-" + UUID.randomUUID();
    }

    private static void deleteDbFile(String session) throws IOException {
        if (session == null) {
            return;
        }
        Path path = Paths.get("chat" + session + ".db");
        Files.deleteIfExists(path);
    }

    private static void sendUdp(String payload, int port) throws IOException {
        byte[] data = payload.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(data, data.length, FunctionalScenarioTest.LOCALHOST, port);
        try (DatagramSocket socket = new DatagramSocket(0, LOCALHOST)) {
            socket.send(packet);
        }
    }

    private static void sendTcpFrame(int port, TCPFrame frame) throws IOException {
        BasicFrameCodec codec = new BasicFrameCodec();
        byte[] payload = codec.encode(frame);
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(LOCALHOST, port), 1000);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeInt(payload.length);
            out.write(payload);
            out.flush();
        }
    }

    private static void waitForTcpServer(int port, Duration timeout) throws InterruptedException {
        awaitCondition(
                () -> isTcpServerReady(port),
                timeout,
                "TCP server did not start"
        );
    }

    private static boolean isTcpServerReady(int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(LOCALHOST, port), 100);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static void awaitCondition(BooleanSupplier condition, Duration timeout, String failureMessage)
            throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            TimeUnit.MILLISECONDS.sleep(20);
        }
        fail(failureMessage);
    }
}
