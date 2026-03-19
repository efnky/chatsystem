package fr.insa.chatsystem.net.messaging.api;

import fr.insa.chatsystem.net.discovery.ContactList;
import fr.insa.chatsystem.net.discovery.User;
import fr.insa.chatsystem.net.messaging.events.ChatMsgFactory;
import fr.insa.chatsystem.net.messaging.events.MessagingEventManager;
import fr.insa.chatsystem.net.messaging.events.MessagingListener;
import fr.insa.chatsystem.net.messaging.models.ChatMsg;
import fr.insa.chatsystem.net.messaging.models.ImageMsg;
import fr.insa.chatsystem.net.messaging.models.ReactionMsg;
import fr.insa.chatsystem.net.messaging.models.TextMsg;
import fr.insa.chatsystem.net.messaging.view.MessagingView;
import fr.insa.chatsystem.net.transport.tcp.controllers.InterfaceTCP;
import fr.insa.chatsystem.net.transport.tcp.models.ConnectionInfo;
import fr.insa.chatsystem.net.transport.tcp.models.TCPFrame;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Facade of the Messaging layer.
 *
 * <p>This class exposes a small, user-oriented API to send chat interactions
 * (text messages, images, reactions) while hiding all transport details
 * (TCP sessions, framing, retries, serialization, etc.).</p>
 *
 * <p>Design intent:</p>
 * <ul>
 *   <li>{@code Messenger} builds domain messages (e.g. {@code TextMsg})</li>
 *   <li>delegates actual IO to services living in {@code MessagingContext}</li>
 *   <li>and returns whether the request was accepted for sending.</li>
 * </ul>
 *
 * <p>Important: a {@code true} return value should mean "queued/accepted",
 * not necessarily "delivered", unless you implement explicit acknowledgements.</p>
 */
public class Messenger {

    /** Context providing transport/session services and dependencies for messaging. */
    private final MessagingContext cxt;
    private static final int DEFAULT_LISTENING_PORT = 5678;
    private final int listeningPort;

    /**
     * Creates a new messenger facade.
     *
     * @param contactList is the ContactList of the connected users
     */
    public Messenger(ContactList contactList) {
        this(contactList, DEFAULT_LISTENING_PORT);
    }

    public Messenger(ContactList contactList, int listeningPort) {
        this.listeningPort = validatePort(listeningPort);
        this.cxt = new MessagingContext(new MessagingEventManager(), new InterfaceTCP() {
            @Override
            public void onMessage(ConnectionInfo c, TCPFrame frame) {
                ChatMsg msg = ChatMsgFactory.fromJSONString(new String(frame.payload()));
                msg.handle(cxt);
            }
        }, contactList);

        MessagingView.initialize(cxt);
        cxt.getTransportService().startServer(this.listeningPort);
    }

    /**
     * Gracefully shuts down the messaging layer (TCP server + sessions).
     * Safe to call multiple times.
     */
    public void close() {
        try {
            // Stop accepting new TCP connections and close existing ones
            cxt.getTransportService().stop();
        } catch (Exception ignored) {
        }
    }

    /**
     * Sends a plain text message to a user.
     *
     * @param targetUserId recipient user id
     * @param text message content
     * @return true if the message was accepted for sending, false otherwise
     * @throws NullPointerException if text is null
     */
    public boolean sendMessage(UUID targetUserId, String text) {
        java.util.Objects.requireNonNull(text, "text");
        User targetUser = cxt.getContactList().getUserFromUUID(targetUserId);
        UUID connectionID = null;
        try {
            connectionID = cxt.getTransportService().connect(targetUser.getAddress().getHostAddress(), listeningPort);
        } catch (IOException e) {
            System.out.println("TCP connection failed during sending text message." + e.getMessage() + "!!!");
            return false;
        }

        // sends the text messages.
        User hostUser = cxt.getContactList().getHostUser();
        TextMsg textMsg = new TextMsg(
                hostUser.getID(),
                targetUser.getID(),
                targetUser.getAddress(),
                targetUser.getPort(),
                hostUser.getAddress(),
                hostUser.getPort(),
                text,
                LocalDate.now(),
                LocalTime.now()
                );
        System.out.println(textMsg);
        try {
            cxt.getTransportService().send(connectionID, new TCPFrame("text", textMsg.toJSONString().getBytes()));
        } catch (IOException e) {
            System.out.println("TCP sending failed during sending text message. :" + e.getMessage() + "!!!!");
            return false;
        }
        cxt.getTransportService().disconnect(connectionID);
        cxt.getEventManager().notifySending(textMsg);
        return true;
    }

    /**
     * Sends a reaction to an existing message.
     *
     * @param targetUserId recipient user id (owner of the conversation / peer)
     * @param msgId id of the message being reacted to
     * @param reaction reaction payload (emoji, short code, etc.)
     * @return true if accepted for sending, false otherwise
     * @throws NullPointerException if reaction is null
     */
    public boolean reactToMsg(UUID targetUserId, UUID targetMsgId, String reaction) {
        User targetUser = cxt.getContactList().getUserFromUUID(targetUserId);
        // connection
        UUID connectionID = null;
        try {
            connectionID = cxt.getTransportService().connect(targetUser.getAddress().getHostAddress(), listeningPort);
        } catch (IOException e) {
            return false;
        }

        // sends the text messages.
        User hostUser = cxt.getContactList().getHostUser();
        ReactionMsg reactionMsg = new ReactionMsg(
                hostUser.getID(),
                targetUser.getID(),
                targetUser.getAddress(),
                targetUser.getPort(),
                hostUser.getAddress(),
                hostUser.getPort(),
                targetMsgId,
                reaction,
                LocalDate.now(),
                LocalTime.now()
        );
        try {
            cxt.getTransportService().send(connectionID, new TCPFrame("reaction", reactionMsg.toJSONString().getBytes()));
        } catch (IOException e) {
            return false;
        }
        cxt.getTransportService().disconnect(connectionID);
        cxt.getEventManager().notifySending(reactionMsg);
        return true;
    }

    /**
     * Sends an image message.
     *
     * <p>Do not use {@code String} for raw image bytes. Prefer:</p>
     * <ul>
     *   <li>a {@code byte[]} payload (small images only), or</li>
     *   <li>a {@code mediaId} referencing an uploaded blob, or</li>
     *   <li>a chunked transfer handled by the transport layer.</li>
     * </ul>
     *
     * @param targetUserId recipient user id
     * @param image image payload reference (encoding must be defined by your protocol)
     * @return true if accepted for sending, false otherwise
     * @throws NullPointerException if image is null
     */
    public boolean sendImage(UUID targetUserId, byte[] image) {
        java.util.Objects.requireNonNull(image, "image");
        User targetUser = cxt.getContactList().getUserFromUUID(targetUserId);
        // connection
        UUID connectionID = null;
        try {
            connectionID = cxt.getTransportService().connect(targetUser.getAddress().getHostAddress(), listeningPort);
        } catch (IOException e) {
            return false;
        }

        // sends the text messages.
        User hostUser = cxt.getContactList().getHostUser();
        ImageMsg imgMsg = new ImageMsg(
                hostUser.getID(),
                targetUser.getID(),
                targetUser.getAddress(),
                targetUser.getPort(),
                hostUser.getAddress(),
                hostUser.getPort(),
                image,
                LocalDate.now(),
                LocalTime.now()
                );
        try {
            cxt.getTransportService().send(connectionID, new TCPFrame("image", imgMsg.toJSONString().getBytes()));
        } catch (IOException e) {
            return false;
        }
        cxt.getTransportService().disconnect(connectionID);
        cxt.getEventManager().notifySending(imgMsg);
        return true;
    }

    /**
     * Subscribes a listener to messaging events.
     *
     * <p>The given {@link MessagingListener} will be notified by the underlying event manager
     * whenever the messaging module emits an event (e.g., text message received, imaged received,
     * received).</p>
     *
     * @param listener the listener to register (must not be null)
     * @throws NullPointerException if {@code listener} is null
     */
    public void subscribe(MessagingListener listener) {
        Objects.requireNonNull(listener, "listener");
        cxt.getEventManager().subscribe(listener);
    }

    /**
     * Unsubscribes a listener from messaging events.
     *
     * <p>After this call, the listener will no longer receive notifications from the messaging module.</p>
     *
     * @param listener the listener to unregister (must not be null)
     * @throws NullPointerException if {@code listener} is null
     */
    public void unsubscribe(MessagingListener listener) {
        Objects.requireNonNull(listener, "listener");
        cxt.getEventManager().unsubscribe(listener);
    }

    private static int validatePort(int port) {
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid TCP port: " + port);
        }
        return port;
    }
}
