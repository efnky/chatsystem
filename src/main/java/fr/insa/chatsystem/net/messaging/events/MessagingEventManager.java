package fr.insa.chatsystem.net.messaging.events;

import fr.insa.chatsystem.net.messaging.models.ChatMsg;
import fr.insa.chatsystem.net.messaging.models.ChatMsgType;
import fr.insa.chatsystem.net.messaging.models.ImageMsg;
import fr.insa.chatsystem.net.messaging.models.ReactionMsg;
import fr.insa.chatsystem.net.messaging.models.TextMsg;
import fr.insa.chatsystem.db.models.Reaction;

import java.util.*;

/**
 * Event dispatcher for the Messaging layer (Observer pattern).
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Maintain a set/list of {@link MessagingListener} observers.</li>
 *   <li>Dispatch each incoming {@link ChatMsg} to the correct callback.</li>
 * </ul>
 *
 * <p>Notes:</p>
 * <ul>
 *   <li>This class assumes messages are already decoded before notification.</li>
 *   <li>Current implementation is a broadcast dispatcher.</li>
 *   <li>If you later want per-type subscriptions, the map below can be used.</li>
 * </ul>
 */
public class MessagingEventManager {

    /**
     * Optional structure for filtered subscriptions by message type.
     *
     * <p>If you do not plan per-type subscriptions, remove this field.</p>
     */
    private final Map<ChatMsgType, List<MessagingListener>> listenersByType = new HashMap<>();

    /**
     * Global listeners (broadcast to all).
     *
     * <p>Consider {@link java.util.concurrent.CopyOnWriteArrayList} if callbacks can subscribe/unsubscribe
     * concurrently with dispatch, or if dispatch happens from multiple threads.</p>
     */
    private final List<MessagingListener> listeners = new ArrayList<>();

    /**
     * Subscribes a listener if it is not already registered.
     *
     * @param listener observer to register
     * @throws NullPointerException if listener is null
     */
    public void subscribe(MessagingListener listener) {
        Objects.requireNonNull(listener, "listener");
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Unsubscribes a listener.
     *
     * @param listener observer to remove
     * @return true if removed, false if it was not registered
     * @throws NullPointerException if listener is null
     */
    public boolean unsubscribe(MessagingListener listener) {
        Objects.requireNonNull(listener, "listener");
        return listeners.remove(listener);
    }

    /**
     * Dispatches a decoded chat message to all subscribed listeners.
     *
     * <p>Routing is done by the runtime type of {@code msg} (sealed hierarchy).</p>
     *
     * @param msg decoded message to dispatch
     * @throws NullPointerException if msg is null
     */
    public void notifyReceiving(ChatMsg msg) {
        Objects.requireNonNull(msg, "msg");
        for (MessagingListener l : listeners) {
            switch (msg) {
                case TextMsg e -> l.onTextMessageReceived(e);
                case ImageMsg e -> l.onImageReceived(e);
                case ReactionMsg e -> {
                    l.onReactionReceived(e);
                }
            }
        }
    }

    public void notifySending(ChatMsg msg) {
        Objects.requireNonNull(msg, "msg");
        for (MessagingListener l : listeners) {
            switch (msg) {
                case TextMsg e -> l.onTextMessageSent(e);
                case ImageMsg e -> l.onImageSent(e);
                case ReactionMsg e -> {
                    l.onReactionSent(e);
                }
            }
        }
    }

    private static Reaction parseReaction(String raw) {
        if (raw == null || raw.isBlank()) {
            return Reaction.NONE;
        }

        String normalized = raw.trim().toUpperCase(Locale.ROOT);
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
}
