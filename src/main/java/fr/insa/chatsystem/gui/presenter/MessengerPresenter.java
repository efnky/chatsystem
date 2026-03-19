package fr.insa.chatsystem.gui.presenter;

import fr.insa.chatsystem.gui.api.*;
import fr.insa.chatsystem.gui.state.Store;
import fr.insa.chatsystem.gui.view.MessengerView;

import java.util.ArrayList;

public final class MessengerPresenter {

    private final Store store;
    private final MessengerView view;
    private final GuiActionHandler actions;

    public MessengerPresenter(Store store, MessengerView view, GuiActionHandler actions) {
        this.store = store;
        this.view = view;
        this.actions = actions;

        view.conversationList().setOnSelect(id ->
                actions.onAction(new GuiAction.SelectConversation(id)));

        view.composer().setOnSend(text -> {
            String cid = store.state().selectedConversationId();
            if (cid != null) actions.onAction(new GuiAction.SendText(cid, text));
        });

        view.composer().setOnAttach(fileRef -> {
            String cid = store.state().selectedConversationId();
            if (cid != null) actions.onAction(new GuiAction.SendImage(cid, fileRef));
        });

        view.messageList().setOnReact((messageId, emojiOrNull) -> {
            String cid = store.state().selectedConversationId();
            if (cid != null) actions.onAction(new GuiAction.SetReaction(cid, messageId, emojiOrNull));
        });

        view.setOnDisconnect(() -> actions.onAction(new GuiAction.Disconnect()));

        // dans MessengerPresenter ctor (après les autres)
        view.setOnChangePseudo( newPseudo -> {
            String hid = store.state().getHostId();
            actions.onAction(new GuiAction.ChangePseudo(hid, newPseudo));
        });

        render();
    }

    public void onUpdate(GuiUpdate update) {
        boolean changed = false;

        switch (update) {
            case GuiUpdate.ConversationUpserted u -> {
                store.state().upsertConversation(u.summary());
                changed = true;
            }
            case GuiUpdate.ConversationSelected s -> {
                store.state().selectConversation(s.conversationId());
                changed = true;
            }
            case GuiUpdate.MessageAppended m -> {
                store.state().appendMessage(m.conversationId(), m.message());
                changed = true;
            }
            case GuiUpdate.MessageDeliveryUpdated d -> {
                // Update message by id in the store (sent / failed)
                changed = store.state().updateDelivery(
                        d.conversationId(),
                        d.messageId(),
                        d.delivered(),
                        d.reasonOrNull()
                );
            }
            case GuiUpdate.MessageReactionsUpdated r -> {
                changed = store.state().updateReactions(
                        r.conversationId(), r.messageId(), r.reactions(), r.myReactionEmojiOrNull()
                );
            }
            case GuiUpdate.ConversationPresenceUpdated p -> {
                store.state().setOnline(p.conversationId(), p.online());
                changed = true;
            }
            // dans onUpdate switch :
            case GuiUpdate.PseudoChanged p -> {
                view.closeChangePseudoDialog();
                // optionnel: toast succès
                // actions / store: tu peux aussi stocker le pseudo courant si tu veux l’afficher
                changed = false;
            }
            case GuiUpdate.PseudoChangeFailed err -> {
                view.showChangePseudoError(err.message());
                changed = false;
            }
            default -> { /* ignore */ }
        }

        if (changed) render();
    }

    public void render() {
        var state = store.state();

        var list = new ArrayList<>(state.conversations());

        // iMessage-like: most recent at top (null timestamps go last)
        list.sort((a, b) -> {
            var ta = a.lastTimestampOrNull();
            var tb = b.lastTimestampOrNull();

            if (ta == null && tb == null) return 0;
            if (ta == null) return 1;    // null => bottom
            if (tb == null) return -1;   // null => bottom
            return tb.compareTo(ta);     // DESC
        });

        view.conversationList().render(
                new ArrayList<>(state.conversations()),
                state.selectedConversationId(),
                state::isOnline
        );

        view.messageList().render(
                state.messagesForSelectedConversation(),
                state.selectedConversationId()
        );

        String cid = state.selectedConversationId();
        boolean enabled = cid != null && state.isOnline(cid);
        view.composer().setEnabledForConversation(enabled);
    }
}
