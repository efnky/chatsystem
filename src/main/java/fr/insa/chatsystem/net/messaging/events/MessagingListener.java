package fr.insa.chatsystem.net.messaging.events;

import fr.insa.chatsystem.net.messaging.models.ImageMsg;
import fr.insa.chatsystem.net.messaging.models.ReactionMsg;
import fr.insa.chatsystem.net.messaging.models.TextMsg;

/**
 * Observer interface for the Messaging layer.
 *
 * <p>Implementations are notified when a chat message has been received, decoded,
 * and validated (wire bytes -> JSON -> {@code ChatMsg} subclass).</p>
 *
 * <p>Rule: callbacks are fast-path. Avoid blocking operations inside these methods.
 * If you need heavy work (IO, DB, image decoding), offload it to another thread.</p>
 */
public interface MessagingListener {

    /**
     * Called when a text message is received.
     *
     * @param msg decoded message (never null)
     */
    void onTextMessageReceived(TextMsg msg);

    void onTextMessageSent(TextMsg msg);

    /**
     * Called when a reaction message is received.
     *
     * @param msg decoded message (never null)
     */
    void onReactionReceived(ReactionMsg msg);

    void onReactionSent(ReactionMsg msg);

    /**
     * Called when an image message is received.
     *
     * @param msg decoded message (never null)
     */
    void onImageReceived(ImageMsg msg);

    void onImageSent(ImageMsg msg);
}
