package fr.insa.chatsystem.net.discovery;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the ContactList class.
 * Tests include:
 *  - Singleton behavior (getInstance)
 *  - addUser and removeUser logic
 *  - String serialization (toString)
 *  - JSON round-trip (toJSON/copyFromJSON)
 *  - Size tracking
 */
class ContactListTest {

    @BeforeEach
    void resetSingleton() {
        resetContactList();
    }

    @Test
    void getInstance() {
        ContactList instance = ContactList.getInstance();
        assertNotNull(instance);
        assertEquals(0, instance.size());
    }

    @Test
    void addUser() {
        ContactList instance = ContactList.getInstance();

        InetAddress localhost = InetAddress.getLoopbackAddress();

        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        UUID user3 = UUID.randomUUID();

        // Add two distinct users
        assertTrue(instance.addUser(user1, "userTest1", localhost, 7777));
        assertEquals(1, instance.size());
        assertTrue(instance.addUser(user2, "userTest2", localhost, 7778));
        assertEquals(2, instance.size());

        // Try adding a user with an existing pseudo -> should fail
        assertFalse(instance.addUser(user3, "userTest2", localhost, 7779));
        assertEquals(2, instance.size());

        // Remove the user, then add again -> should succeed
        instance.removeUser("userTest2");
        assertTrue(instance.addUser(user2, "userTest2", localhost, 7778));
        assertEquals(2, instance.size());
    }

    @Test
    void removeUser() {
        ContactList instance = ContactList.getInstance();
        InetAddress localhost = InetAddress.getLoopbackAddress();

        instance.addUser(UUID.randomUUID(), "userTest1", localhost, 7777);
        instance.addUser(UUID.randomUUID(), "userTest2", localhost, 7778);

        assertEquals(2, instance.size());
        instance.removeUser("userTest2");
        assertEquals(1, instance.size());

        // Removing a non-existent user should not throw or change the size
        instance.removeUser("userTest2");
        assertEquals(1, instance.size());

        instance.removeUser("userTest1");
        assertEquals(0, instance.size());
    }

    @Test
    void testToString() {
        ContactList instance = ContactList.getInstance();
        InetAddress localhost = InetAddress.getLoopbackAddress();

        instance.addUser(UUID.randomUUID(), "alice", localhost, 9000);
        instance.addUser(UUID.randomUUID(), "bob", localhost, 9001);

        String s = instance.toString();
        assertNotNull(s);
        assertFalse(s.isBlank());

        // Since HashMap iteration order is not guaranteed, we only check for markers
        assertTrue(s.contains("alice"));
        assertTrue(s.contains("bob"));
        assertTrue(s.contains("id="));
        assertTrue(s.contains("pseudo="));
        assertTrue(s.contains("address="));
        assertTrue(s.contains("port="));
    }

    @Test
    void copyFromJSON_valid_singleUser_roundTrip() {
        ContactList instance = ContactList.getInstance();
        InetAddress localhost = InetAddress.getLoopbackAddress();

        UUID id = UUID.randomUUID();
        JSONObject json = new JSONObject()
                .put("charlie", new JSONObject()
                        .put("id", id.toString())
                        .put("pseudo", "charlie")
                        .put("address", localhost.getHostAddress())
                        .put("port", 7777)
                );

        instance.copyFromJSON(json);

        assertEquals(1, instance.size());
        User restored = instance.getUserFromUUID(id);
        assertNotNull(restored);
        assertEquals("charlie", restored.getPseudo());
    }

    @Test
    void copyFromJSON_valid_multiUser_orderAgnostic() {
        ContactList instance = ContactList.getInstance();
        InetAddress localhost = InetAddress.getLoopbackAddress();

        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        JSONObject json = new JSONObject()
                .put("u1", new JSONObject()
                        .put("id", id1.toString())
                        .put("pseudo", "u1")
                        .put("address", localhost.getHostAddress())
                        .put("port", 8001)
                )
                .put("u2", new JSONObject()
                        .put("id", id2.toString())
                        .put("pseudo", "u2")
                        .put("address", localhost.getHostAddress())
                        .put("port", 8002)
                );

        instance.copyFromJSON(json);

        List<User> users = instance.getConnectedUsers();
        assertEquals(2, users.size());
        assertNotNull(instance.getUserFromUUID(id1));
        assertNotNull(instance.getUserFromUUID(id2));
    }

    @Test
    void copyFromJSON_invalid_throws() {
        ContactList instance = ContactList.getInstance();

        JSONObject invalid = new JSONObject().put("bad", new JSONObject().put("pseudo", "bad"));
        assertThrows(org.json.JSONException.class, () -> instance.copyFromJSON(invalid));
    }

    @Test
    void size() {
        ContactList instance = ContactList.getInstance();
        assertEquals(0, instance.size());
        InetAddress localhost = InetAddress.getLoopbackAddress();

        assertTrue(instance.addUser(UUID.randomUUID(), "p1", localhost, 7001));
        assertEquals(1, instance.size());

        assertTrue(instance.addUser(UUID.randomUUID(), "p2", localhost, 7002));
        assertEquals(2, instance.size());

        // Duplicate pseudo should not increase size
        assertFalse(instance.addUser(UUID.randomUUID(), "p2", localhost, 7003));
        assertEquals(2, instance.size());

        instance.removeUser("p1");
        assertEquals(1, instance.size());

        instance.removeUser("p2");
        assertEquals(0, instance.size());
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
}
