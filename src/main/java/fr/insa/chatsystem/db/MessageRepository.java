package fr.insa.chatsystem.db;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import fr.insa.chatsystem.db.events.DBEventsManager;
import fr.insa.chatsystem.db.models.Contact;
import fr.insa.chatsystem.db.models.Direction;
import fr.insa.chatsystem.db.models.Message;
import fr.insa.chatsystem.db.models.Reaction;

public class MessageRepository {

    private final SQLiteDatabase database;
    private final DBEventsManager eventsManager;

    public MessageRepository(SQLiteDatabase database, DBEventsManager eventsManager) {
        this.database = database;
        this.eventsManager = eventsManager;
    }

    /**
     * Save a message for a given contact.
     */
    public void saveMessage(UUID msgId, UUID contactId, String content, Direction direction) {
        saveMessage(msgId, contactId, content, direction, LocalDateTime.now());
    }

    /**
     * Save a message using an explicit timestamp (useful to keep in-memory and persisted copies aligned).
     */
    public void saveMessage(UUID msgId, UUID contactId, String content, Direction direction, LocalDateTime timestamp) {
        String sql = """
            INSERT INTO message (id, contact_id, content, timestamp, direction, reaction)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, msgId.toString());
            ps.setString(2, contactId.toString());
            ps.setString(3, content);
            ps.setString(4, timestamp.toString());
            ps.setString(5, direction.name());
            ps.setString(6, Reaction.NONE.name());
            ps.executeUpdate();
            eventsManager.notifyOnMessageArchived(new Message(
                    contactId,
                    content,
                    timestamp,
                    Direction.RECEIVED
            ));
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            //throw new RuntimeException("Failed to save message", e);
        }
    }

    public List<Message> findMessagesByUUID(UUID contactID) {
        String sql = """
        SELECT id, content, timestamp, direction, reaction
        FROM message
        WHERE contact_id = ?
        ORDER BY timestamp ASC
    """;

        List<Message> messages = new ArrayList<>();

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, contactID.toString());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    messages.add(new Message(
                            UUID.fromString(rs.getString("id")),
                            rs.getString("content"),
                            LocalDateTime.parse(rs.getString("timestamp")),
                            Direction.valueOf(rs.getString("direction")),
                            Reaction.from(rs.getString("reaction"))
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load messages", e);
        }

        return messages;
    }

    public Message getLastMessageFrom(UUID contactId){
        return findMessagesByUUID(contactId).getLast();
    }

    /**
     * Resolve SQLite contact.id from logical user_id.
     */
    private int getContactDbId(Contact contact) throws SQLException {
        String sql = "SELECT id FROM contact WHERE user_id = ?";

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, contact.getUserId().toString());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }

        throw new IllegalStateException(
            "Contact not found in database: " + contact.getUserId()
        );
    }

    public void updateReaction(UUID messageId, Reaction reaction) {
        if (reaction == null) {
            throw new IllegalArgumentException("reaction must not be null");
        }
        String sql = "UPDATE message SET reaction = ? WHERE id = ?";
        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, reaction.name());
            ps.setString(2, messageId.toString());
            ps.executeUpdate();
            eventsManager.notifyOnReactionArchived(reaction);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update reaction", e);
        }
    }

    /**
     * Retourne tous les messages d'une conversation à partir de l'UUID utilisateur.
     * Méthode destinée à la couche Core / GUI.
     */
    public List<Message> findConversationWithUser(UUID userId) {
        String sql = """
            SELECT m.id, m.content, m.timestamp, m.direction, m.reaction
            FROM message m
            JOIN contact c ON m.contact_id = c.id
            WHERE c.user_id = ?
            ORDER BY m.id ASC
        """;

        List<Message> messages = new ArrayList<>();

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, userId.toString());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Message message = new Message(
                            UUID.fromString(rs.getString("id")),
                            rs.getString("content"),
                            LocalDateTime.parse(rs.getString("timestamp")),
                            Direction.valueOf(rs.getString("direction"))
                    );
                    message.setReaction(Reaction.valueOf(rs.getString("reaction")));
                    messages.add(message);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to load conversation for user " + userId, e
            );
        }

        return messages;
    }
}
