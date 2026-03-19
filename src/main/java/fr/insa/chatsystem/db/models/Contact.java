package fr.insa.chatsystem.db.models;

import java.util.UUID;

public class Contact {

    private final UUID userId;
    private String username;

    public Contact(UUID userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }
}
