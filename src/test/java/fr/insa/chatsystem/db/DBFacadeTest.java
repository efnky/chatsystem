package fr.insa.chatsystem.db;

import fr.insa.chatsystem.db.api.DBFacade;
import fr.insa.chatsystem.db.models.Contact;
import fr.insa.chatsystem.net.discovery.ContactList;
import fr.insa.chatsystem.net.discovery.User;
import fr.insa.chatsystem.net.messaging.api.Messenger;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DBFacadeTest {
    private static final int MESSAGING_PORT = 5678;

    @Test
    void updateDB_upsertsContactsAndPersistsPseudoChanges() throws Exception {
        ContactList contactList = resetContactList();
        createHostUser(contactList);
        String session = newSessionId();
        Messenger messenger = new Messenger(contactList);

        try {
            DBFacade db = new DBFacade(messenger, session);
            UUID userId = UUID.randomUUID();
            UUID otherId = UUID.randomUUID();

            db.updateDB(List.of(
                    new Contact(userId, "alice"),
                    new Contact(otherId, "bob")
            ));
            db.updateDB(List.of(new Contact(userId, "alice2")));

            List<Contact> contacts = db.getAllContacts();
            assertEquals(2, contacts.size(), "DB should contain two distinct contacts");
            Contact updated = contacts.stream()
                    .filter(c -> c.getUserId().equals(userId))
                    .findFirst()
                    .orElseThrow();
            assertEquals("alice2", updated.getUsername(), "Pseudo should be updated for existing user");
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

    private static void createHostUser(ContactList contactList) throws Exception {
        User host = new User(UUID.randomUUID(), "host", User.Type.HOST, localhost(), DBFacadeTest.MESSAGING_PORT);
        contactList.setHostUser(host);
    }

    private static InetAddress localhost() throws Exception {
        return InetAddress.getByName("127.0.0.1");
    }

    private static String newSessionId() {
        return "test-" + UUID.randomUUID();
    }

}
