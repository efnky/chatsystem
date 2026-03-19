
package fr.insa.chatsystem.db.controllers;

import fr.insa.chatsystem.db.ContactRepository;
import fr.insa.chatsystem.db.MessageRepository;
import fr.insa.chatsystem.db.events.DBEventsManager;
import fr.insa.chatsystem.db.models.*;
import fr.insa.chatsystem.net.discovery.ContactList;
import fr.insa.chatsystem.net.discovery.User;
import fr.insa.chatsystem.net.messaging.api.Messenger;
import fr.insa.chatsystem.net.messaging.events.MessagingListener;
import fr.insa.chatsystem.net.messaging.models.ImageMsg;
import fr.insa.chatsystem.net.messaging.models.ReactionMsg;
import fr.insa.chatsystem.net.messaging.models.TextMsg;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Orchestrates chatrooms and bridges Messaging events with persistence.
 */
public class ChatroomController implements MessagingListener, ContactList.Listener {


    private final ContactRepository contactRepository;
    private final MessageRepository messageRepository;
    private final DBEventsManager eventsManager;

    /**
     * One chatroom per contact (1-to-1 chat).
     */
    private final Map<UUID, Chatroom> chatrooms = new HashMap<>();

    public ChatroomController(
            Messenger messenger,
            ContactRepository contactRepository,
            MessageRepository messageRepository,
            DBEventsManager eventsManager
    ) {
        this.contactRepository = contactRepository;
        this.messageRepository = messageRepository;
        this.eventsManager = eventsManager;

        // Subscribe to incoming messaging events
        messenger.subscribe(this);
        ContactList.getInstance().subscribe(this);
    }

    /**
     * Open (or retrieve) a chatroom for a given contact id.
     */
    public Chatroom openChat(UUID contactID) {
        return chatrooms.computeIfAbsent(
                contactID,
                id -> {
                    Chatroom chatroom = new Chatroom(contactID, messageRepository);
                    chatroom.loadHistory();
                    return chatroom;
                }
        );
    }

    /**D
     * Send a message to a contact.
     */
//    public void sendMessage(Contact contact, String content) {
//        Chatroom chatroom = openChat(contact);
//
//        // Network send
//        messenger.sendMessage(
//            contact.getUserId(),
//            content
//        );
//
//        LocalDateTime timestamp = LocalDateTime.now();
//
//        // Local persistence + memory update
//        messageRepository.saveMessage(contact, content, Direction.SENT, timestamp);
//        chatroom.addSentMessage(content, timestamp);
//    }

    /**
     * Incoming text message from the Messaging layer.
     */
    @Override
    public void onTextMessageReceived(TextMsg msg) {
        if(msg.getOwner() == ContactList.getInstance().getHostUser().getID()){
            return;
        }
        Chatroom chatroom = openChat(msg.getOwner());
        LocalDateTime timestamp = LocalDateTime.of(msg.getDateStamp(), msg.getTimeStamp());

        messageRepository.saveMessage(msg.getMsgId(), msg.getOwner(), msg.getContent(), Direction.RECEIVED, LocalDateTime.now());
        chatroom.addReceivedMessage(msg.getMsgId(),msg.getContent(), timestamp);

        // GUI notification can be triggered here if needed
    }

    @Override
    public void onTextMessageSent(TextMsg msg) {
        Chatroom chatroom = openChat(msg.getOwner());
        LocalDateTime timestamp = LocalDateTime.of(msg.getDateStamp(), msg.getTimeStamp());

        messageRepository.saveMessage(msg.getMsgId(), msg.getTargetID(), msg.getContent(), Direction.SENT, timestamp);
        chatroom.addReceivedMessage(msg.getMsgId(),msg.getContent(), timestamp);
    }

    @Override
    public void onReactionReceived(ReactionMsg msg) {
        // Reactions are handled via onReactionReceived to avoid duplicate processing.
        Chatroom chatroom = openChat(msg.getOwner());
        chatroom.loadHistory();
        try {
            Reaction react = Reaction.from(msg.getReaction());
            chatroom.reactToMessage(msg.getTargetMsgId(), react);
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void onReactionSent(ReactionMsg msg) {
        Chatroom chatroom = openChat(msg.getTargetID());
        chatroom.loadHistory();
        try {
            Reaction react = Reaction.from(msg.getReaction());
            chatroom.reactToMessage(msg.getTargetMsgId(), react);
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
    }

//    @Override
//    public void onReactionReceived(UUID senderId, long messageId, Reaction reaction) {
//        Contact contact = contactRepository
//            .findByUserId(senderId)
//            .orElseGet(() -> {
//                User user = ContactList.getInstance().getUserFromUUID(senderId);
//                String username = (user != null) ? user.getPseudo() : "user-" + senderId;
//                Contact newContact = new Contact(senderId, username);
//                contactRepository.upsertContact(newContact);
//                return newContact;
//            });
//
//        Chatroom chatroom = openChat(contact);
//        chatroom.loadHistory();
//        try {
//            chatroom.reactToMessage(messageId, reaction);
//        } catch (IllegalArgumentException e) {
//            System.out.println(e.getMessage());
//        }
//    }

    @Override
    public void onImageReceived(ImageMsg msg) {
        if(msg.getOwner() == ContactList.getInstance().getHostUser().getID()){
            return;
        }
        // Image handling is out of scope for text chat history
    }

    @Override
    public void onImageSent(ImageMsg msg) {

    }

    private static Reaction parseReaction(String raw) {
        if (raw == null || raw.isBlank()) {
            return Reaction.NONE;
        }

        String normalized = raw.trim().toUpperCase(java.util.Locale.ROOT);
        try {
            return Reaction.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            for (Reaction reaction : Reaction.values()) {
                if (reaction.getEmoji().equals(raw)) {
                    return reaction;
                }
            }
            return Reaction.NONE;
        }
    }

    @Override
    public void onUserAdded(User user) {
        contactRepository.upsertContact(new Contact(user.getID(), user.getPseudo()));
    }

    @Override
    public void onUserRemoved(User user) {

    }

    @Override
    public void onPseudoChange(User user,  String oldPseudo, String newPseudo) {
        contactRepository.upsertContact(new Contact(user.getID(), newPseudo));
    }
}
