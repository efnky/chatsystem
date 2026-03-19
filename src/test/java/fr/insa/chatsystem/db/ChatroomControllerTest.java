package fr.insa.chatsystem.db;

import fr.insa.chatsystem.db.controllers.ChatroomController;
import fr.insa.chatsystem.db.events.DBEventsManager;
import fr.insa.chatsystem.db.models.Chatroom;
import fr.insa.chatsystem.db.models.Direction;
import fr.insa.chatsystem.db.models.Message;
import fr.insa.chatsystem.net.discovery.ContactList;
import fr.insa.chatsystem.net.discovery.User;
import fr.insa.chatsystem.net.messaging.api.Messenger;
import fr.insa.chatsystem.net.messaging.models.TextMsg;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChatroomControllerTest {
    private static final int MESSAGING_PORT = 5678;

    @Test
    void incomingTextMessage_persistsAndUpdatesChatroom() throws Exception {
        ContactList contactList = resetContactList();
        User host = createHostUser(contactList);
        String session = newSessionId();
        Messenger messenger = new Messenger(contactList);
        SQLiteDatabase database = new SQLiteDatabase(session);
        DBEventsManager events = new DBEventsManager();
        MessageRepository messageRepository = new MessageRepository(database, events);
        ContactRepository contactRepository = new ContactRepository(database);
        ChatroomController controller = new ChatroomController(messenger, contactRepository, messageRepository, events);

        InetAddress localhost = localhost();
        UUID senderId = UUID.randomUUID();
        UUID msgId = UUID.randomUUID();
        TextMsg msg = new TextMsg(
                msgId,
                senderId,
                host.getID(),
                localhost,
                MESSAGING_PORT,
                localhost,
                MESSAGING_PORT,
                "hello",
                LocalDate.of(2024, 1, 1),
                LocalTime.of(12, 0)
        );

        try {
            controller.onTextMessageReceived(msg);

            Chatroom chatroom = controller.openChat(senderId);
            assertEquals(1, chatroom.getMessages().size(), "Chatroom should contain one message");
            Message inMemory = chatroom.getMessages().getFirst();
            assertEquals(msgId, inMemory.getId());
            assertEquals("hello", inMemory.getContent());
            assertEquals(Direction.RECEIVED, inMemory.getDirection());

            Chatroom reloaded = new Chatroom(senderId, messageRepository);
            reloaded.loadHistory();
            assertEquals(1, reloaded.getMessages().size(), "Persisted history should contain one message");
            Message persisted = reloaded.getMessages().getFirst();
            assertEquals(msgId, persisted.getId());
            assertEquals(Direction.RECEIVED, persisted.getDirection());
        } finally {
            messenger.close();
            resetContactList();
        }
    }

    private static ContactList resetContactList() {
        try {
            Field instanceField = ContactList.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to reset ContactList singleton", e);
        }
        return ContactList.getInstance();
    }

    private static User createHostUser(ContactList contactList) throws Exception {
        User host = new User(UUID.randomUUID(), "host", User.Type.HOST, localhost(), ChatroomControllerTest.MESSAGING_PORT);
        contactList.setHostUser(host);
        return host;
    }

    private static InetAddress localhost() throws Exception {
        return InetAddress.getByName("127.0.0.1");
    }

    private static String newSessionId() {
        return "test-" + UUID.randomUUID();
    }
}
