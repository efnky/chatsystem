package fr.insa.chatsystem.db.models;

import fr.insa.chatsystem.db.MessageRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Logical chatroom representing a 1-to-1 conversation with a single contact.
 * This class is NOT persisted directly in the database.
 */
public class Chatroom {

    private final UUID contactID;
    private final MessageRepository messageRepository;
    private final List<Message> messages;

    public Chatroom(UUID contactID, MessageRepository messageRepository) {
        this.contactID = contactID;
        this.messageRepository = messageRepository;
        this.messages = new ArrayList<>();
    }

    /**
     * Load message history from the database.
     * Should be called when opening the chatroom.
     */
    public void loadHistory() {
        messages.clear();
        messages.addAll(messageRepository.findMessagesByUUID(contactID));
    }

    /**
     * Add a sent message (local user).
     */
    public void addSentMessage(UUID msgId, String content) {
        addSentMessage(msgId, content, LocalDateTime.now());
    }

    /**
     * Add a sent message using the provided timestamp.
     */
    public void addSentMessage(UUID msgId, String content, LocalDateTime timestamp) {
        addMessage(msgId, content, Direction.SENT, timestamp);
    }

    /**
     * Add a received message (remote contact).
     */
    public void addReceivedMessage(UUID msgId, String content) {
        addReceivedMessage(msgId, content, LocalDateTime.now());
    }

    /**
     * Add a received message using the provided timestamp.
     */
    public void addReceivedMessage(UUID msgId, String content, LocalDateTime timestamp) {
        addMessage(msgId, content, Direction.RECEIVED, timestamp);
    }

    private void addMessage(UUID msgId, String content, Direction direction, LocalDateTime timestamp) {
        messages.add(new Message(msgId, content, timestamp, direction));
    }

    public void reactToMessage(UUID messageId, Reaction reaction) {
        Objects.requireNonNull(reaction, "reaction");

        Message message = findMessageByUUId(messageId);
        if (message == null) {
            throw new IllegalArgumentException("Message not found: " + messageId);
        }

        messageRepository.updateReaction(messageId, reaction);
        message.setReaction(reaction);
    }

    private Message findMessageByUUId(UUID messageId) {
        for (Message message : messages) {
            if (message.getId().equals(messageId)) {
                return message;
            }
        }
        return null;
    }

    public UUID getContactId() {
        return contactID;
    }

    /**
     * Returns the messages to be displayed by the GUI.
     */
    public List<Message> getMessages() {
        return List.copyOf(messages);
    }
}
