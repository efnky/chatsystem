package fr.insa.chatsystem.net.discovery;

import org.junit.jupiter.api.Test;
import org.json.JSONObject;

import java.net.InetAddress;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the User class.
 * Tests the correctness of accessors, mutators, and toString() representation.
 */
class UserTest {

    @Test
    void setUserPseudo() {
        // Create a User instance with an initial pseudo
        User user = new User(UUID.randomUUID(), "Alice", User.Type.PEER, InetAddress.getLoopbackAddress(), 8080);

        // Verify the initial pseudo
        assertEquals("Alice", user.getPseudo());

        // Change the pseudo
        user.setPseudo("Bob");

        // Verify that the pseudo has been correctly updated
        assertEquals("Bob", user.getPseudo());
    }

    @Test
    void getUserID() {
        UUID id = UUID.randomUUID();
        User user = new User(id, "Charlie", User.Type.HOST, InetAddress.getLoopbackAddress(), 9000);
        assertEquals(id, user.getID());
    }

    @Test
    void getUserPseudo() {
        User user = new User(UUID.randomUUID(), "Diana", User.Type.PEER, InetAddress.getLoopbackAddress(), 7000);
        assertEquals("Diana", user.getPseudo());
    }

    @Test
    void testToString() {
        // Create a user with deterministic address and port
        InetAddress address = InetAddress.getLoopbackAddress();
        UUID id = UUID.randomUUID();
        User user = new User(id, "Eve", User.Type.PEER, address, 12345);

        String repr = user.toString();
        assertNotNull(repr);
        assertTrue(repr.contains(id.toString()));
        assertTrue(repr.contains("pseudo='Eve'"));
        assertTrue(repr.contains("address=" + address.getHostAddress()));
        assertTrue(repr.contains("port=12345"));
    }

    @Test
    void peerFromJSON_roundTrip() {
        InetAddress address = InetAddress.getLoopbackAddress();
        UUID id = UUID.randomUUID();

        JSONObject json = new JSONObject()
                .put("id", id.toString())
                .put("pseudo", "Frank")
                .put("address", address.getHostAddress())
                .put("port", 5555);

        User user = User.peerFromJSON(json);
        assertEquals(id, user.getID());
        assertEquals("Frank", user.getPseudo());
        assertEquals(address.getHostAddress(), user.getAddress().getHostAddress());
        assertEquals(5555, user.getPort());
        assertEquals(User.Type.PEER, user.getType());
    }

    @Test
    void constructor_rejectsBlankPseudo() {
        assertThrows(IllegalArgumentException.class, () ->
                new User(UUID.randomUUID(), "  ", User.Type.PEER, InetAddress.getLoopbackAddress(), 1234)
        );
    }
}
