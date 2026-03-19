package fr.insa.chatsystem.gui.view;

import fr.insa.chatsystem.gui.api.ConversationSummaryUI;
import fr.insa.chatsystem.gui.view.theme.Theme;
import fr.insa.chatsystem.gui.view.theme.ThemeManager;

import javax.swing.*;
import javax.swing.Box;
import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class ConversationRowView extends JPanel {

    private final ConversationSummaryUI c;
    private final boolean selected;
    private final boolean online;

    private final Theme th = ThemeManager.get();

    // iMessage-like hour
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    public ConversationRowView(ConversationSummaryUI c, boolean selected, boolean online) {
        this.c = c;
        this.selected = selected;
        this.online = online;

        setLayout(new BorderLayout(10, 0));
        setOpaque(true);
        setBackground(selected ? new Color(0xE9E9EB) : Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        // --- LEFT: status dot (centered vertically)
        JPanel left = new JPanel(new GridBagLayout());
        left.setOpaque(false);
        left.add(new StatusDot(online));
        add(left, BorderLayout.WEST);

        // --- CENTER: title + preview (stack)
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        JLabel title = new JLabel(c.title());
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14.5f));
        title.setForeground(new Color(0x111111));

        JLabel preview = new JLabel(safe(c.lastPreview()));
        preview.setFont(preview.getFont().deriveFont(Font.PLAIN, 12.5f));
        preview.setForeground(new Color(0x6D6D72));

        // little spacing between title and preview
        center.add(title);
        center.add(Box.createVerticalStrut(2));
        center.add(preview);

        add(center, BorderLayout.CENTER);

        // --- EAST: time + unread badge (right aligned)
        JPanel east = new JPanel();
        east.setOpaque(false);
        east.setLayout(new BoxLayout(east, BoxLayout.Y_AXIS));

        JLabel time = new JLabel(formatTime(c.lastTimestampOrNull()));
        time.setFont(time.getFont().deriveFont(Font.PLAIN, 11.5f));
        time.setForeground(new Color(0x8E8E93));
        time.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JComponent badge = buildUnreadBadge(c.unreadCount());
        badge.setAlignmentX(Component.RIGHT_ALIGNMENT);

        east.add(time);
        east.add(Box.createVerticalStrut(6));
        east.add(badge);

        add(east, BorderLayout.EAST);
    }

    private static String safe(String s) {
        return (s == null) ? "" : s;
    }

    private static String formatTime(Instant ts) {
        if (ts == null) return "";
        return TIME_FMT.format(ts);
    }

    private static JComponent buildUnreadBadge(int unread) {
        if (unread <= 0) return (JComponent) Box.createRigidArea(new Dimension(0, 0));

        JLabel lbl = new JLabel(unread > 99 ? "99+" : String.valueOf(unread));
        lbl.setForeground(Color.WHITE);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 11.5f));
        lbl.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel pill = new JPanel(new BorderLayout()) {
            @Override public boolean isOpaque() { return false; }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(0x007AFF)); // iOS blue
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 999, 999);
                } finally {
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };

        pill.setBorder(BorderFactory.createEmptyBorder(2, 7, 2, 7));
        pill.add(lbl, BorderLayout.CENTER);
        pill.setPreferredSize(new Dimension(Math.max(24, lbl.getPreferredSize().width + 14), 18));
        return pill;
    }

    private static final class StatusDot extends JComponent {
        private final boolean online;

        StatusDot(boolean online) {
            this.online = online;
            setPreferredSize(new Dimension(10, 10));
            setMinimumSize(new Dimension(10, 10));
            setOpaque(false);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color fill = online ? new Color(0x34C759) : new Color(0xFF3B30); // iOS green/red
                g2.setColor(fill);
                g2.fillOval(0, 0, getWidth(), getHeight());

                // subtle outline
                g2.setColor(new Color(0, 0, 0, 35));
                g2.drawOval(0, 0, getWidth() - 1, getHeight() - 1);
            } finally {
                g2.dispose();
            }
        }
    }
}
