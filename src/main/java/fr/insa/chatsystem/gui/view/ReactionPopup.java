package fr.insa.chatsystem.gui.view;

import fr.insa.chatsystem.gui.api.MessageUI;
import fr.insa.chatsystem.gui.view.theme.Theme;
import fr.insa.chatsystem.gui.view.theme.ThemeManager;
import fr.insa.chatsystem.db.models.Reaction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;

public final class ReactionPopup {

    private final JWindow window;
    private static final Theme th = ThemeManager.get();
    private final AWTEventListener outsideClickCloser;

    public ReactionPopup(Window owner, MessageUI m, MessageBubbleView.ReactionHandler onReact) {
        window = new JWindow(owner);
        window.setAlwaysOnTop(true);
        window.setBackground(new Color(0, 0, 0, 0)); // transparent
        window.setType(Window.Type.POPUP);

        RoundedBubblePanel card = new RoundedBubblePanel(th.popupBg(), th.popupRadius());
        card.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        row.setOpaque(false);

        String[] emojis = {"❤️", "😂", "👍", "👎", "😮", "😢"};
        for (String e : emojis) {
            JButton b = new JButton();
            b.setFocusPainted(false);
            b.setBorderPainted(false);
            b.setContentAreaFilled(false);
            b.setOpaque(false);
            b.setMargin(new Insets(0, 0, 0, 0));
            b.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

            b.setPreferredSize(new Dimension(22, 22));

            b.setFont(b.getFont().deriveFont(14f));
            if ("❤️".equals(e)) b.setText("<html><span style='color:#ff0000;'>❤️</span></html>");
            else b.setText(e);

            b.addActionListener(ev -> {
                String next = (m.myReactionEmojiOrNull() != null && m.myReactionEmojiOrNull().equals(e))
                        ? Reaction.NONE.name()
                        : e;
                onReact.onReact(m.messageId(), next);
                hide();
            });
            row.add(b);
        }

        JButton remove = new JButton("Remove reaction");
        remove.setFocusPainted(false);
        remove.setBorderPainted(false);
        remove.setContentAreaFilled(false);
        remove.setOpaque(false);
        remove.setMargin(new Insets(0, 0, 0, 0));
        remove.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        remove.setFont(remove.getFont().deriveFont(11f));
        remove.addActionListener(ev -> {
            onReact.onReact(m.messageId(), Reaction.NONE.name());
            hide();
        });

        card.add(row);
        card.add(Box.createVerticalStrut(4));
        card.add(new JSeparator());
        card.add(Box.createVerticalStrut(2));
        card.add(remove);

        window.setContentPane(card);
        window.pack();

        // close on outside click
        outsideClickCloser = event -> {
            if (!(event instanceof java.awt.event.MouseEvent me)) return;
            if (me.getID() != java.awt.event.MouseEvent.MOUSE_PRESSED) return;
            if (!window.isVisible()) return;

            Point p = me.getLocationOnScreen();
            Rectangle r = window.getBounds();
            if (!r.contains(p)) hide();
        };
    }

    public void showAt(Component anchor, int xOnScreen, int yOnScreen) {
        window.setLocation(xOnScreen, yOnScreen);
        window.setVisible(true);
        Toolkit.getDefaultToolkit().addAWTEventListener(outsideClickCloser, AWTEvent.MOUSE_EVENT_MASK);
    }

    public void hide() {
        if (!window.isVisible()) return;
        window.setVisible(false);
        Toolkit.getDefaultToolkit().removeAWTEventListener(outsideClickCloser);
    }
}
