package fr.insa.chatsystem.gui.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Objects;
import java.util.function.Consumer;

public final class LoginView extends JPanel {

    private Consumer<String> onConnect = p -> {};

    private final JTextField pseudo = new JTextField();
    private final JButton connect = new JButton("Connexion");
    private final JLabel status = new JLabel(" ");

    private static final Color IOS_BG_TOP = new Color(0xFFFFFF);
    private static final Color IOS_BG_BOT = new Color(0xF2F2F7);

    private static final Color IOS_CARD = new Color(0xFFFFFF);
    private static final Color IOS_BORDER = new Color(0, 0, 0, 25);

    private static final Color IOS_TEXT = new Color(0x111111);
    private static final Color IOS_SUB = new Color(0x6B6B6B);
    private static final Color IOS_PLACEHOLDER = new Color(0x8E8E93);

    private static final Color IOS_BLUE = new Color(0x1E90FF); // ton bleu bulle
    private static final Color IOS_ERROR = new Color(0xD70015);

    private static final String PLACEHOLDER = "Pseudo";

    public LoginView() {
        setOpaque(true);
        setLayout(new GridBagLayout());

        // --- Card (shadow + rounded) ---
        ShadowCard card = new ShadowCard(28);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(22, 22, 22, 22));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Header (avatar + title)
        JPanel header = new JPanel(new BorderLayout(14, 0));
        header.setOpaque(false);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        AvatarBadge avatar = new AvatarBadge(52);
        header.add(avatar, BorderLayout.WEST);

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("ChatSystem");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        title.setForeground(IOS_TEXT);

        JLabel subtitle = new JLabel("Connecte-toi pour commencer à clavarder");
        subtitle.setFont(subtitle.getFont().deriveFont(13.5f));
        subtitle.setForeground(IOS_SUB);

        titles.add(title);
        titles.add(Box.createVerticalStrut(4));
        titles.add(subtitle);

        header.add(titles, BorderLayout.CENTER);

        // Input pill
        RoundedBubblePanel inputPill = new RoundedBubblePanel(new Color(0xF2F2F7), 22);
        inputPill.setLayout(new BorderLayout(10, 0));
        inputPill.setBorder(new EmptyBorder(10, 12, 10, 12));
        inputPill.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel userIcon = new JLabel("👤");
        userIcon.setForeground(IOS_PLACEHOLDER);
        userIcon.setFont(userIcon.getFont().deriveFont(14f));
        inputPill.add(userIcon, BorderLayout.WEST);

        pseudo.setBorder(BorderFactory.createEmptyBorder());
        pseudo.setOpaque(false);
        pseudo.setFont(pseudo.getFont().deriveFont(14.5f));
        pseudo.setForeground(IOS_TEXT);

        installPlaceholder(pseudo, PLACEHOLDER);

        inputPill.add(pseudo, BorderLayout.CENTER);

        // Button pill
        PillButtonPanel buttonPanel = new PillButtonPanel(IOS_BLUE, 22, connect);
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        connect.setFocusable(false);
        connect.setBorderPainted(false);
        connect.setContentAreaFilled(false);
        connect.setOpaque(false);
        connect.setForeground(Color.WHITE);
        connect.setFont(connect.getFont().deriveFont(Font.BOLD, 14f));
        connect.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Status
        status.setAlignmentX(Component.LEFT_ALIGNMENT);
        status.setFont(status.getFont().deriveFont(12.5f));
        status.setForeground(IOS_ERROR);

        // Little hint row (fun + helpful, subtle)
        JLabel hint = new JLabel("Astuce : Entrée pour valider");
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        hint.setFont(hint.getFont().deriveFont(11.5f));
        hint.setForeground(new Color(0x9A9AA0));

        // Compose card
        card.add(header);
        card.add(Box.createVerticalStrut(18));
        card.add(inputPill);
        card.add(Box.createVerticalStrut(12));
        card.add(buttonPanel);
        card.add(Box.createVerticalStrut(10));
        card.add(status);
        card.add(Box.createVerticalStrut(8));
        card.add(hint);

        // Center it
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.insets = new Insets(24, 24, 24, 24);
        gc.anchor = GridBagConstraints.CENTER;
        add(card, gc);

        // Actions
        connect.addActionListener(e -> tryConnect());
        pseudo.addActionListener(e -> tryConnect());

        pseudo.getDocument().addDocumentListener((SimpleDocumentListener) e -> refreshConnectEnabled());
        refreshConnectEnabled();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Soft vertical gradient background (very iOS/macOS)
            GradientPaint gp = new GradientPaint(0, 0, IOS_BG_TOP, 0, h, IOS_BG_BOT);
            g2.setPaint(gp);
            g2.fillRect(0, 0, w, h);

            // subtle "blob" accents (fun but not flashy)
            g2.setComposite(AlphaComposite.SrcOver.derive(0.10f));
            g2.setColor(new Color(0x1E90FF));
            g2.fillOval((int)(w * 0.65), (int)(h * 0.12), 220, 220);

            g2.setColor(new Color(0xFF2D55)); // iOS pink
            g2.fillOval((int)(w * 0.10), (int)(h * 0.55), 180, 180);

        } finally {
            g2.dispose();
        }
    }

    private void tryConnect() {
        String p = pseudo.getText() == null ? "" : pseudo.getText().trim();
        if (p.isEmpty() || p.equalsIgnoreCase(PLACEHOLDER)) return;
        onConnect.accept(p);
    }

    private void refreshConnectEnabled() {
        String p = pseudo.getText();
        boolean ok = p != null && !p.trim().isEmpty() && !p.trim().equalsIgnoreCase(PLACEHOLDER);
        connect.setEnabled(ok);

        // visual feedback
        connect.setForeground(Color.WHITE);
        connect.repaint();
    }

    public void setOnConnect(Consumer<String> onConnect) {
        this.onConnect = Objects.requireNonNull(onConnect);
    }

    public void setStatus(String textOrBlank) {
        String t = (textOrBlank == null) ? "" : textOrBlank.trim();
        status.setText(t.isEmpty() ? " " : t);
    }

    // ---------- helpers ----------

    private static void installPlaceholder(JTextField field, String placeholder) {
        field.setText(placeholder);
        field.setForeground(IOS_PLACEHOLDER);

        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(IOS_TEXT);
                }
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                if (field.getText().trim().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(IOS_PLACEHOLDER);
                }
            }
        });
    }

    /**
     * White rounded card with soft shadow.
     */
    private static final class ShadowCard extends JPanel {
        private final int radius;

        ShadowCard(int radius) {
            this.radius = radius;
            setOpaque(false);
            setBackground(IOS_CARD);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // shadow (soft)
                g2.setComposite(AlphaComposite.SrcOver.derive(0.10f));
                g2.setColor(Color.BLACK);
                g2.fillRoundRect(4, 6, w - 8, h - 8, radius, radius);

                // card
                g2.setComposite(AlphaComposite.SrcOver);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, w - 1, h - 1, radius, radius);

                // subtle border
                g2.setColor(IOS_BORDER);
                g2.drawRoundRect(0, 0, w - 1, h - 1, radius, radius);

            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            int w = Math.max(380, d.width);
            int h = Math.max(240, d.height);
            return new Dimension(w, h);
        }
    }

    /**
     * “Avatar” badge for fun (gradient circle).
     */
    private static final class AvatarBadge extends JComponent {
        private final int size;

        AvatarBadge(int size) {
            this.size = size;
            setPreferredSize(new Dimension(size, size));
            setMinimumSize(new Dimension(size, size));
            setMaximumSize(new Dimension(size, size));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                GradientPaint gp = new GradientPaint(0, 0, new Color(0x1E90FF), size, size, new Color(0x5AC8FA));
                g2.setPaint(gp);
                g2.fillOval(0, 0, size, size);

                g2.setColor(new Color(255, 255, 255, 80));
                g2.drawOval(1, 1, size - 3, size - 3);

                g2.setFont(getFont().deriveFont(Font.BOLD, 18f));
                FontMetrics fm = g2.getFontMetrics();
                String s = "💬";
                int x = (size - fm.stringWidth(s)) / 2;
                int y = (size - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(s, x, y);

            } finally {
                g2.dispose();
            }
        }
    }

    /**
     * Rounded blue button background (prevents LAF artifacts).
     */
    private static final class PillButtonPanel extends JPanel {
        private final Color bg;
        private final int radius;

        PillButtonPanel(Color bg, int radius, JButton button) {
            this.bg = bg;
            this.radius = radius;
            setOpaque(false);
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(10, 12, 10, 12));
            add(button, BorderLayout.CENTER);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                float a = (getComponents().length > 0 && getComponents()[0] instanceof JButton b && !b.isEnabled())
                        ? 0.45f
                        : 1.0f;

                g2.setComposite(AlphaComposite.SrcOver.derive(a));
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);

                g2.setComposite(AlphaComposite.SrcOver.derive(0.12f));
                g2.setColor(Color.BLACK);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);

            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(Math.max(260, d.width), Math.max(44, d.height));
        }
    }

    @FunctionalInterface
    private interface SimpleDocumentListener extends javax.swing.event.DocumentListener {
        void update(javax.swing.event.DocumentEvent e);
        @Override default void insertUpdate(javax.swing.event.DocumentEvent e) { update(e); }
        @Override default void removeUpdate(javax.swing.event.DocumentEvent e) { update(e); }
        @Override default void changedUpdate(javax.swing.event.DocumentEvent e) { update(e); }
    }
}