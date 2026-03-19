package fr.insa.chatsystem.gui.view;

import fr.insa.chatsystem.gui.api.MessageType;
import fr.insa.chatsystem.gui.api.MessageUI;
import fr.insa.chatsystem.gui.view.theme.Theme;
import fr.insa.chatsystem.gui.view.theme.ThemeManager;

import javax.swing.*;
import java.awt.*;

public final class MessageBubbleView extends JPanel {

    private static final Theme th = ThemeManager.get();

    @FunctionalInterface
    public interface ReactionHandler {
        void onReact(String messageId, String emojiOrNull);
    }

    public MessageBubbleView(MessageUI m, int maxBubbleWidthPx, ReactionHandler onReact) {
        setOpaque(false);
        setLayout(new BorderLayout());

        boolean mine = m.isMine();

        Color bubbleBg = mine ? th.bubbleMineBg() : th.bubbleOtherBg();
        Color textColor = mine ? th.bubbleMineText() : th.bubbleOtherText();

        // --- Bubble panel (rounded) ---
        RoundedBubblePanel bubble = new RoundedBubblePanel(bubbleBg, th.bubbleRadius());
        bubble.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JComponent content;
        if (m.type() == MessageType.TEXT) {
            JTextArea ta = new JTextArea(m.textOrNull());
            ta.setLineWrap(true);
            ta.setWrapStyleWord(true);
            ta.setEditable(false);
            ta.setOpaque(false);
            ta.setForeground(textColor);
            ta.setFont(ta.getFont().deriveFont(th.messageFontSize()));

            // hard constraint: wrap width
            ta.setSize(new Dimension(maxBubbleWidthPx - 24, Short.MAX_VALUE));
            content = ta;
        } else {
            JLabel img = new JLabel();
            img.setOpaque(false);
            img.setHorizontalAlignment(SwingConstants.CENTER);

            try {
                img.setIcon(ImageUtils.loadScaled(m.imageRefOrNull(), maxBubbleWidthPx, 260));
            } catch (Exception e) {
                img.setText("[image]");
                img.setForeground(textColor);
            }
            content = img;
        }

        bubble.add(content, BorderLayout.CENTER);

        // --- Reaction overlay (iMessage-like) ---
        ReactionOverlayView overlay = new ReactionOverlayView(m.reactions(), m.myReactionEmojiOrNull());
        overlay.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent e) { e.consume(); }
            @Override public void mouseReleased(java.awt.event.MouseEvent e) { e.consume(); }
        });

        // --- Layered bubble + overlay ---
        JLayeredPane layered = new JLayeredPane();
        layered.setLayout(null);
        layered.setOpaque(false);

        Dimension bsz = bubble.getPreferredSize();
        Dimension osz = overlay.getPreferredSize();

        int extraTop = osz.height / 2;

        bubble.setBounds(0, extraTop, bsz.width, bsz.height);
        layered.add(bubble, Integer.valueOf(0));

        int ox = Math.max(0, bsz.width - osz.width - 6);
        int oy = 0;
        overlay.setBounds(ox, oy, osz.width, osz.height);
        layered.add(overlay, Integer.valueOf(1));

        layered.setPreferredSize(new Dimension(bsz.width, bsz.height + extraTop));

        // --- Fail indicator ---
        boolean showFail = !m.delivered() && m.failedReasonOrNull() != null;
        JLabel fail = new JLabel("!");
        fail.setVisible(showFail);
        fail.setForeground(th.danger());
        fail.setToolTipText(m.failedReasonOrNull() == null ? "Failed" : m.failedReasonOrNull());

        // --- Right-click => custom ReactionPopup (ONLY on message bubble) ---
        bubble.addMouseListener(new java.awt.event.MouseAdapter() {
            private void maybeShow(java.awt.event.MouseEvent e) {
                if (!e.isPopupTrigger()) return;

                Window owner = SwingUtilities.getWindowAncestor(bubble);
                if (owner == null) return;

                ReactionPopup popup = new ReactionPopup(owner, m, (msgId, emojiOrNull) ->
                        onReact.onReact(msgId, emojiOrNull));

                // show near cursor (screen coords)
                Point p = e.getLocationOnScreen();
                popup.showAt(bubble, p.x, p.y);
            }

            @Override public void mousePressed(java.awt.event.MouseEvent e) { maybeShow(e); }
            @Override public void mouseReleased(java.awt.event.MouseEvent e) { maybeShow(e); }
        });

        // (optional) right-click on text/image should also open popup, but still within the bubble
        content.addMouseListener(new java.awt.event.MouseAdapter() {
            private void maybeShow(java.awt.event.MouseEvent e) {
                if (!e.isPopupTrigger()) return;

                Window owner = SwingUtilities.getWindowAncestor(bubble);
                if (owner == null) return;

                ReactionPopup popup = new ReactionPopup(owner, m, (msgId, emojiOrNull) -> onReact.onReact(msgId, emojiOrNull));

                Point p = e.getLocationOnScreen();
                popup.showAt(bubble, p.x, p.y);
            }

            @Override public void mousePressed(java.awt.event.MouseEvent e) { maybeShow(e); }
            @Override public void mouseReleased(java.awt.event.MouseEvent e) { maybeShow(e); }
        });

        // --- Row alignment ---
        JPanel row = new JPanel(new FlowLayout(mine ? FlowLayout.RIGHT : FlowLayout.LEFT, 6, 0));
        row.setOpaque(false);

        if (mine) {
            row.add(layered);
            row.add(fail);
        } else {
            row.add(layered);
        }

        add(row, BorderLayout.CENTER);
        setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
    }
}