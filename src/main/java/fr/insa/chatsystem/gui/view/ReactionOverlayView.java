package fr.insa.chatsystem.gui.view;

import fr.insa.chatsystem.gui.api.ReactionUI;
import fr.insa.chatsystem.gui.view.theme.Theme;
import fr.insa.chatsystem.gui.view.theme.ThemeManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public final class ReactionOverlayView extends JPanel {

    private static final int PAD_X = 5;   // 5 * 1.75 ≈ 9
    private static final int PAD_Y = 4;   // 2 * 1.75 ≈ 4
    private static final int GAP = 4;   // 4 * 1.75 ≈ 7
    private static final int RADIUS = 25; // 14 * 1.75 ≈ 25

    private static final Theme theme = ThemeManager.get();
    private static final Color PILL_BG = theme.overlayPillBg();
    private static final Color PILL_BORDER = theme.overlayPillBorder();

    public ReactionOverlayView(List<ReactionUI> reactions, String myReactionEmojiOrNull) {
        setOpaque(false);
        setLayout(new FlowLayout(FlowLayout.LEFT, GAP, 0));

        if (reactions == null || reactions.isEmpty()) {
            setVisible(false);
            return;
        }
        setVisible(true);

        Font emojiFont = getFont().deriveFont(13f);
        Font countFont = getFont().deriveFont(12f);

        for (ReactionUI r : reactions) {
            // ultra-compact item: [emoji][count?]
            JPanel item = new JPanel();
            item.setOpaque(false);
            item.setLayout(new BoxLayout(item, BoxLayout.X_AXIS));

            JLabel emoji = new JLabel(r.emoji());
            emoji.setFont(emojiFont);
            emoji.setOpaque(false);

            boolean isHeart = "❤️".equals(r.emoji()) || "♥️".equals(r.emoji()) || "♥".equals(r.emoji());
            emoji.setForeground(isHeart ? Color.RED : Color.BLACK);
            item.add(emoji);

            if (r.count() > 1) {
                item.add(Box.createHorizontalStrut(2));
                JLabel count = new JLabel(String.valueOf(r.count()));
                count.setFont(countFont);
                count.setOpaque(false);
                count.setForeground(Color.BLACK); // normal text color
                item.add(count);
            }

            add(item);
        }

        // tight padding for the pill itself
        setBorder(BorderFactory.createEmptyBorder(PAD_Y, PAD_X, PAD_Y, PAD_X));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            g2.setColor(PILL_BG);
            g2.fillRoundRect(0, 0, w - 1, h - 1, theme.popupRadius(),theme.popupRadius());

            g2.setColor(PILL_BORDER);
            g2.drawRoundRect(0, 0, w - 1, h - 1,theme.popupRadius(), theme.popupRadius());
        } finally {
            g2.dispose();
        }

        super.paintComponent(g);
    }
}