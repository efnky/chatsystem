package fr.insa.chatsystem.gui.api;

import java.util.List;
import java.util.Objects;

/**
 * Incremental events emitted by the core to update the GUI.
 */
public sealed interface GuiUpdate
        permits GuiUpdate.ConnectionStateChanged, GuiUpdate.ConversationPresenceUpdated, GuiUpdate.ConversationRemoved, GuiUpdate.ConversationSelected, GuiUpdate.ConversationUpserted, GuiUpdate.MessageAppended, GuiUpdate.MessageDeliveryUpdated, GuiUpdate.MessageReactionsUpdated, GuiUpdate.PseudoChangeFailed, GuiUpdate.PseudoChanged, GuiUpdate.ShowScreen, GuiUpdate.Toast {

    record ShowScreen(Screen screen) implements GuiUpdate {
        public ShowScreen {
            Objects.requireNonNull(screen, "screen");
        }
    }

    record ConnectionStateChanged(ConnectionState state, String messageOrNull) implements GuiUpdate {
        public ConnectionStateChanged {
            Objects.requireNonNull(state, "state");
            // messageOrNull optional
        }
    }

    /**
     * Adds or updates a conversation summary in the sidebar.
     */
    record ConversationUpserted(ConversationSummaryUI summary) implements GuiUpdate {
        public ConversationUpserted {
            Objects.requireNonNull(summary, "summary");
        }
    }

    record ConversationRemoved(String conversationId) implements GuiUpdate {
        public ConversationRemoved {
            Objects.requireNonNull(conversationId, "conversationId");
        }
    }

    /**
     * Optional: the core can force a selection (e.g., after connect).
     */
    record ConversationSelected(String conversationId) implements GuiUpdate {
        public ConversationSelected {
            Objects.requireNonNull(conversationId, "conversationId");
        }
    }

    /**
     * Appends a message to a conversation.
     */
    record MessageAppended(String conversationId, MessageUI message) implements GuiUpdate {
        public MessageAppended {
            Objects.requireNonNull(conversationId, "conversationId");
            Objects.requireNonNull(message, "message");
        }
    }

    /**
     * Updates delivery status: you requested "sent or not", no "sending".
     * delivered=false can represent failure; use reasonOrNull for user feedback.
     */
    record MessageDeliveryUpdated(String conversationId, String messageId, boolean delivered, String reasonOrNull)
            implements GuiUpdate {
        public MessageDeliveryUpdated {
            Objects.requireNonNull(conversationId, "conversationId");
            Objects.requireNonNull(messageId, "messageId");
        }
    }

    /**
     * Updates reactions aggregation + the current user's own reaction.
     *
     * @param reactions aggregated reactions (emoji + count)
     * @param myReactionEmojiOrNull the current user's reaction emoji, or null if none
     */
    record MessageReactionsUpdated(
            String conversationId,
            String messageId,
            List<ReactionUI> reactions,
            String myReactionEmojiOrNull
    ) implements GuiUpdate {
        public MessageReactionsUpdated {
            Objects.requireNonNull(conversationId, "conversationId");
            Objects.requireNonNull(messageId, "messageId");
            Objects.requireNonNull(reactions, "reactions");
        }
    }

    record Toast(ToastLevel level, String text) implements GuiUpdate {
        public Toast {
            Objects.requireNonNull(level, "level");
            Objects.requireNonNull(text, "text");
        }
    }

    record ConversationPresenceUpdated(String conversationId, boolean online) implements GuiUpdate {
        public ConversationPresenceUpdated {
            java.util.Objects.requireNonNull(conversationId, "conversationId");
        }
    }

    record PseudoChanged(String newPseudo) implements GuiUpdate {}

    record PseudoChangeFailed(String message) implements GuiUpdate {}
}

