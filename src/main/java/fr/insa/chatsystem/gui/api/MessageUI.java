package fr.insa.chatsystem.gui.api;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Message model understood by the GUI.
 *
 * <p>Notes:
 * <ul>
 *   <li>TEXT uses text != null</li>
 *   <li>IMAGE uses imageRef != null (local path or resource id)</li>
 *   <li>GIF can be added later by extending MessageType + adding a payload field</li>
 * </ul>
 */
public record MessageUI(
        String messageId,
        String conversationId,
        boolean isMine,
        Instant timestamp,
        MessageType type,
        String textOrNull,
        String imageRefOrNull,
        boolean delivered,
        String failedReasonOrNull,
        List<ReactionUI> reactions,
        String myReactionEmojiOrNull
) {
    public MessageUI {
        Objects.requireNonNull(messageId, "messageId");
        Objects.requireNonNull(conversationId, "conversationId");
        Objects.requireNonNull(timestamp, "timestamp");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(reactions, "reactions");

        // Basic payload sanity
        switch (type) {
            case TEXT -> {
                if (textOrNull == null) throw new IllegalArgumentException("TEXT requires textOrNull");
            }
            case IMAGE -> {
                if (imageRefOrNull == null) throw new IllegalArgumentException("IMAGE requires imageRefOrNull");
            }
        }

        // If delivered is true, failed reason should typically be null (not enforced strictly)
    }
}

