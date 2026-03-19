package fr.insa.chatsystem.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import fr.insa.chatsystem.db.models.Contact;

public class ContactRepository {

    private final SQLiteDatabase database;

    public ContactRepository(SQLiteDatabase database) {
        this.database = database;
    }

    /**
     * Insert a new contact if unknown, or update username if it already exists.
     */
    public void upsertContact(Contact contact) {
        String sql = """
            INSERT INTO contact (user_id, username)
            VALUES (?, ?)
            ON CONFLICT(user_id)
            DO UPDATE SET username = excluded.username;
        """;

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, contact.getUserId().toString());
            ps.setString(2, contact.getUsername());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to upsert contact: " + contact.getUserId(), e);
        }
    }

    /**
     * Find a contact by its logical user_id.
     */
    public Optional<Contact> findByUserId(UUID userId) {
        String sql = "SELECT user_id, username FROM contact WHERE user_id = ?";

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, userId.toString());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Contact contact = new Contact(
                        UUID.fromString(rs.getString("user_id")),
                        rs.getString("username")
                    );
                    return Optional.of(contact);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find contact: " + userId, e);
        }

        return Optional.empty();
    }

    /**
     * Return all known contacts.
     */
    public List<Contact> findAll() {
        String sql = "SELECT user_id, username FROM contact";
        List<Contact> contacts = new ArrayList<>();

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                contacts.add(new Contact(
                    UUID.fromString(rs.getString("user_id")),
                    rs.getString("username")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load contacts", e);
        }

        return contacts;
    }
}