package fr.insa.chatsystem.gui.view;

import javax.swing.*;
import java.awt.*;

public final class RoundedBubblePanel extends JPanel {

    private final int radius;
    private final Color bg;

    public RoundedBubblePanel(Color bg, int radius) {
        this.bg = bg;
        this.radius = radius;
        setOpaque(false);
        setLayout(new BorderLayout());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
        } finally {
            g2.dispose();
        }
    }
}
