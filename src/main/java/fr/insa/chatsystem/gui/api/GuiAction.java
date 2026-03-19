package fr.insa.chatsystem.gui.api;

import java.util.Objects;
import java.util.UUID;

/**
 * User intents emitted by the GUI to the core.
 */
public sealed interface GuiAction
        permits GuiAction.ChangePseudo, GuiAction.Close, GuiAction.Connect, GuiAction.Disconnect, GuiAction.SelectConversation, GuiAction.SendImage, GuiAction.SendText, GuiAction.SetReaction {

    record Connect(String requestedPseudo) implements GuiAction {
        public Connect {
            Objects.requireNonNull(requestedPseudo, "requestedPseudo");
        }
    }

    record Disconnect() implements GuiAction { }

    record Close() implements GuiAction {}

    record SelectConversation(String conversationId) implements GuiAction {
        public SelectConversation {
            Objects.requireNonNull(conversationId, "conversationId");
        }
    }

    record SendText(String conversationId, String text) implements GuiAction {
        public SendText {
            Objects.requireNonNull(conversationId, "conversationId");
            Objects.requireNonNull(text, "text");
        }
    }

    /**
     * Asks the core to send an image selected by the user.
     * fileRef is a local reference (e.g., absolute path).
     */
    record SendImage(String conversationId, String fileRef) implements GuiAction {
        public SendImage {
            Objects.requireNonNull(conversationId, "conversationId");
            Objects.requireNonNull(fileRef, "fileRef");
        }
    }

    /**
     * Sets (or removes) the current user's reaction on a message.
     *
     * <p>Contract: one reaction per user per message.
     * <ul>
     *   <li>emoji != null: set or replace the reaction</li>
     *   <li>emoji == null: remove the reaction</li>
     * </ul>
     */
    record SetReaction(String conversationId, String messageId, String emojiOrNull) implements GuiAction {
        public SetReaction {
            Objects.requireNonNull(conversationId, "conversationId");
            Objects.requireNonNull(messageId, "messageId");
            // emojiOrNull intentionally nullable
        }
    }

    record ChangePseudo(String conversationId, String newPseudo) implements GuiAction {
        public ChangePseudo {
            Objects.requireNonNull(newPseudo, "newPseudo");
            Objects.requireNonNull(conversationId, "conversationId");
        }
    }
}

