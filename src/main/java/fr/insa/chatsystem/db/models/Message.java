package fr.insa.chatsystem.db.models;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Message {

    private final UUID id;
    private final String content;
    private final LocalDateTime timestamp;
    private final Direction direction;
    private Reaction reaction;

    public Message(UUID id, String content, LocalDateTime timestamp, Direction direction) {
        this.id = id;
        this.content = content;
        this.timestamp = timestamp;
        this.direction = direction;
        this.reaction = Reaction.NONE;
    }

    public Message(UUID id, String content, LocalDateTime timestamp, Direction direction, Reaction reaction) {
        this.id = id;
        this.content = content;
        this.timestamp = timestamp;
        this.direction = direction;
        this.reaction = reaction;
    }

    public UUID getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Direction getDirection() {
        return direction;
    }

    public Reaction getReaction() {
        return reaction;
    }

    public void setReaction(Reaction reaction) {
        this.reaction = Objects.requireNonNull(reaction, "reaction");
    }
}
