package fr.insa.chatsystem.gui.view;

import fr.insa.chatsystem.gui.api.MessageUI;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

public final class MessageListView extends JPanel {

    private JScrollPane owningScroll;
    private List<MessageUI> lastMessages = List.of();

    // needed for "conversation changed => jump bottom"
    private String lastConversationId = null;

    private BiConsumer<String, String> onReact = (msgId, emojiOrNull) -> {};

    public MessageListView() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(true);
        setBackground(Color.WHITE); // TODO Theme later

        // Re-render when resized (keeps bubble width correct)
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) {
                if (!lastMessages.isEmpty()) {
                    render(lastMessages, lastConversationId);
                }
            }
        });
    }

    public void setOnReact(BiConsumer<String, String> onReact) {
        this.onReact = Objects.requireNonNull(onReact, "onReact");
    }

    public void setOwningScrollPane(JScrollPane scrollPane) {
        this.owningScroll = scrollPane;
    }

    /**
     * Renders messages and applies iMessage-like autoscroll behavior.
     *
     * Rules:
     * - If conversation changes: jump to bottom.
     * - Else if user was already at bottom: keep sticking to bottom.
     * - Else: do not jump (user is reading history).
     */
    public void render(List<MessageUI> messages, String conversationId) {
        this.lastMessages = messages;

        boolean wasAtBottom = isScrolledToBottom();
        boolean conversationChanged =
                conversationId != null
                        && lastConversationId != null
                        && !conversationId.equals(lastConversationId);

        this.lastConversationId = conversationId;

        removeAll();

        int maxBubbleWidth = computeMaxBubbleWidth();

        for (var m : messages) {
            add(Box.createVerticalStrut(6));
            add(new MessageBubbleView(
                    m,
                    maxBubbleWidth,
                    (msgId, emojiOrNull) -> onReact.accept(msgId, emojiOrNull)
            ));
            add(Box.createVerticalStrut(2)); // petite respiration avant l'heure
            add(buildTimestampRow(m));
        }

        add(Box.createVerticalStrut(6));

        revalidate();
        repaint();

        if (conversationChanged || wasAtBottom) {
            scrollToBottom();
        }
    }

    private JComponent buildTimestampRow(MessageUI m) {
        boolean mine = m.isMine();

        JLabel time = new JLabel(formatTime(m.timestamp()));
        time.setFont(time.getFont().deriveFont(Font.PLAIN, 11.5f));
        time.setForeground(new Color(0x8E8E93)); // iOS secondary label

        JPanel row = new JPanel(new FlowLayout(mine ? FlowLayout.RIGHT : FlowLayout.LEFT, 6, 0));
        row.setOpaque(false);
        row.add(time);

        // marge horizontale comme tes bulles (optionnel)
        row.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        return row;
    }

    private static String formatTime(java.time.Instant ts) {
        if (ts == null) return "";
        var paris = java.time.ZoneId.of("Europe/Paris");
        var t = java.time.ZonedDateTime.ofInstant(ts, paris).toLocalTime();
        return java.time.format.DateTimeFormatter.ofPattern("HH:mm").format(t);
    }

    private int computeMaxBubbleWidth() {
        int w = 700;
        if (owningScroll != null && owningScroll.getViewport() != null) {
            int vw = owningScroll.getViewport().getWidth();
            if (vw > 0) w = vw;
        }
        return Math.max(220, (int) (w * 0.65));
    }

    private boolean isScrolledToBottom() {
        if (owningScroll == null) return true;

        JViewport vp = owningScroll.getViewport();
        if (vp == null) return true;

        Component view = vp.getView();
        if (view == null) return true;

        int viewH = view.getHeight();
        int extentH = vp.getExtentSize().height;
        int y = vp.getViewPosition().y;

        // bottom when the viewport's bottom is close to view's bottom
        return (y + extentH) >= (viewH - 24);
    }

    private void scrollToBottom() {
        if (owningScroll == null) return;
        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = owningScroll.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }
}