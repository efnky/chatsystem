package fr.insa.chatsystem.gui.view;

import javax.swing.*;
import java.awt.*;

public final class RoundedPopupMenu extends JPopupMenu {

    private final int radius;
    private final Color bg;
    private final Color border;

    public RoundedPopupMenu(int radius, Color bg, Color border) {
        this.radius = radius;
        this.bg = bg;
        this.border = border;

        setOpaque(false);
        setBorder(null); // <- IMPORTANT: remove LAF border
        setBackground(new Color(0,0,0,0));

        // some LAFs (Nimbus) draw extra stuff unless this is enabled
        setLightWeightPopupEnabled(true);

        setLayout(new BorderLayout());
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            g2.setColor(bg);
            g2.fillRoundRect(0, 0, w - 1, h - 1, radius, radius);

            g2.setColor(border);
            g2.drawRoundRect(0, 0, w - 1, h - 1, radius, radius);
        } finally {
            g2.dispose();
        }

        super.paintComponent(g);
    }

    @Override
    protected void paintBorder(Graphics g) {
        // <- IMPORTANT: prevent rectangular border from LAF
    }
}