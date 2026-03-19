package fr.insa.chatsystem.gui.state;

import fr.insa.chatsystem.gui.api.ConversationSummaryUI;
import fr.insa.chatsystem.gui.api.MessageType;
import fr.insa.chatsystem.gui.api.MessageUI;
import fr.insa.chatsystem.gui.api.ReactionUI;

import java.time.Instant;
import java.util.*;
import java.util.List;

public final class AppState {

    private String hostId;

    private String selectedConversationId;
    private final Map<String, ConversationSummaryUI> conversations = new LinkedHashMap<>();
    private final Map<String, List<MessageUI>> messagesByConversation = new HashMap<>();

    private final Map<String, Boolean> onlineByConversation = new HashMap<>();

    public boolean isOnline(String conversationId) {
        return onlineByConversation.getOrDefault(conversationId, false);
    }

    public void setOnline(String conversationId, boolean online) {
        onlineByConversation.put(conversationId, online);
    }

    public void setHostId(String hostId){
        this.hostId = hostId;
    }

    public String getHostId(){
        return this.hostId;
    }


    public String selectedConversationId() {
        return selectedConversationId;
    }

    public void selectConversation(String conversationId) {
        this.selectedConversationId = conversationId;

        // 7.3: selecting a conversation clears unread
        if (conversationId != null) {
            clearUnread(conversationId);
        }
    }

    public Collection<ConversationSummaryUI> conversations() {
        return conversations.values();
    }

    public List<MessageUI> messagesForSelectedConversation() {
        if (selectedConversationId == null) return List.of();
        return messagesByConversation.getOrDefault(selectedConversationId, List.of());
    }

    public void upsertConversation(ConversationSummaryUI c) {
        // Merge policy: keep existing unreadCount if present (GUI manages unread)
        ConversationSummaryUI old = conversations.get(c.conversationId());
        int unread = (old != null) ? old.unreadCount() : c.unreadCount();

        ConversationSummaryUI merged = new ConversationSummaryUI(
                c.conversationId(),
                c.title(),
                c.lastPreview(),
                c.lastTimestampOrNull(),
                unread
        );
        conversations.put(c.conversationId(), merged);
    }

    public void appendMessage(String conversationId, MessageUI m) {
        messagesByConversation
                .computeIfAbsent(conversationId, k -> new ArrayList<>())
                .add(m);

        // 7.3: update conversation preview + timestamp
        bumpConversationFromMessage(conversationId, m);

        // 7.3: unread++ only for incoming messages in non-selected conversation
        boolean incoming = !m.isMine();
        boolean notSelected = selectedConversationId == null || !conversationId.equals(selectedConversationId);

        if (incoming && notSelected) {
            incrementUnread(conversationId);
        } else if (!notSelected) {
            // if the user is currently in this conversation, keep unread at 0
            clearUnread(conversationId);
        }
    }

    private void bumpConversationFromMessage(String conversationId, MessageUI m) {
        ConversationSummaryUI old = conversations.get(conversationId);
        if (old == null) return;

        String preview = previewOf(m);
        Instant ts = m.timestamp();

        ConversationSummaryUI updated = new ConversationSummaryUI(
                old.conversationId(),
                old.title(),
                preview,
                ts,
                old.unreadCount()
        );
        conversations.put(conversationId, updated);
    }

    private static String previewOf(MessageUI m) {
        if (m.type() == MessageType.IMAGE) return "[image]";
        String t = m.textOrNull();
        if (t == null) return "";
        t = t.trim();
        return t;
    }

    private void incrementUnread(String conversationId) {
        ConversationSummaryUI old = conversations.get(conversationId);
        if (old == null) return;

        ConversationSummaryUI updated = new ConversationSummaryUI(
                old.conversationId(),
                old.title(),
                old.lastPreview(),
                old.lastTimestampOrNull(),
                old.unreadCount() + 1
        );
        conversations.put(conversationId, updated);
    }

    private void clearUnread(String conversationId) {
        ConversationSummaryUI old = conversations.get(conversationId);
        if (old == null) return;
        if (old.unreadCount() == 0) return;

        ConversationSummaryUI updated = new ConversationSummaryUI(
                old.conversationId(),
                old.title(),
                old.lastPreview(),
                old.lastTimestampOrNull(),
                0
        );
        conversations.put(conversationId, updated);
    }

    /**
     * Updates delivery status of an existing message.
     *
     * @return true if a message was found and updated, false otherwise
     */
    public boolean updateDelivery(String conversationId, String messageId, boolean delivered, String reasonOrNull) {
        System.out.println("updateDelivery hit: " + messageId + " delivered=" + delivered + " reason=" + reasonOrNull);
        List<MessageUI> list = messagesByConversation.get(conversationId);
        if (list == null || list.isEmpty()) return false;

        for (int i = 0; i < list.size(); i++) {
            MessageUI cur = list.get(i);
            if (!cur.messageId().equals(messageId)) continue;

            MessageUI updated = new MessageUI(
                    cur.messageId(),
                    cur.conversationId(),
                    cur.isMine(),
                    cur.timestamp(),
                    cur.type(),
                    cur.textOrNull(),
                    cur.imageRefOrNull(),
                    delivered,
                    reasonOrNull,
                    cur.reactions(),
                    cur.myReactionEmojiOrNull()
            );

            list.set(i, updated);
            return true;
        }
        return false;
    }

    public boolean updateReactions(String conversationId, String messageId,
                                   List<ReactionUI> reactions, String myReactionEmojiOrNull) {
        List<MessageUI> list = messagesByConversation.get(conversationId);
        if (list == null || list.isEmpty()) return false;

        for (int i = 0; i < list.size(); i++) {
            MessageUI cur = list.get(i);
            if (!cur.messageId().equals(messageId)) continue;

            MessageUI updated = new MessageUI(
                    cur.messageId(),
                    cur.conversationId(),
                    cur.isMine(),
                    cur.timestamp(),
                    cur.type(),
                    cur.textOrNull(),
                    cur.imageRefOrNull(),
                    cur.delivered(),
                    cur.failedReasonOrNull(),
                    reactions,
                    myReactionEmojiOrNull
            );
            list.set(i, updated);
            return true;
        }
        return false;
    }
}