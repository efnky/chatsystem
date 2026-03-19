package fr.insa.chatsystem.gui.api;

import java.time.Instant;
import java.util.Objects;

/**
 * Sidebar conversation summary (iMessage-like list).
 */
public record ConversationSummaryUI(
        String conversationId,
        String title,
        String lastPreview,
        Instant lastTimestampOrNull,
        int unreadCount
) {
    public ConversationSummaryUI {
        Objects.requireNonNull(conversationId, "conversationId");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(lastPreview, "lastPreview");
        if (unreadCount < 0) throw new IllegalArgumentException("unreadCount must be >= 0");
    }


}
