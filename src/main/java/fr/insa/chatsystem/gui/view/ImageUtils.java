package fr.insa.chatsystem.gui.view;

import javax.swing.*;
import java.awt.*;

public final class ImageUtils {

    private ImageUtils() {}

    public static ImageIcon loadScaled(String path, int maxW, int maxH) {
        ImageIcon icon = new ImageIcon(path);
        int w = icon.getIconWidth();
        int h = icon.getIconHeight();
        if (w <= 0 || h <= 0) return icon;

        double scale = Math.min((double) maxW / w, (double) maxH / h);
        if (scale >= 1.0) return icon;

        int nw = Math.max(1, (int) (w * scale));
        int nh = Math.max(1, (int) (h * scale));
        Image scaled = icon.getImage().getScaledInstance(nw, nh, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }
}
