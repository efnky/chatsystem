package fr.insa.chatsystem.gui.view.theme;

import java.awt.*;

public final class LightTheme implements Theme {

    @Override public Color appBackground() { return Color.WHITE; }

    @Override public Color bubbleMineBg() { return new Color(0x1E90FF); }
    @Override public Color bubbleOtherBg() { return new Color(0xEDEDED); }
    @Override public Color bubbleMineText() { return Color.WHITE; }
    @Override public Color bubbleOtherText() { return Color.BLACK; }

    @Override public Color danger() { return Color.RED; }

    @Override public Color popupBg() { return new Color(0xF2F2F2); }
    @Override public Color popupBorder() { return new Color(0xD0D0D0); }

    @Override public Color overlayPillBg() { return new Color(0xF2F2F2); }
    @Override public Color overlayPillBorder() { return new Color(0xD0D0D0); }

    @Override
    public Color whisperTextFont() {
        return new Color(0x8E8E93);
    }

    @Override
    public Color highlightColor() {
        return new Color(0x8E8E93);
    }

    @Override
    public Color inputTextBg() {
        return new Color(0xF2F2F7);
    }

    @Override public int bubbleRadius() { return 20; }
    @Override public int popupRadius() { return 12; }

    @Override public float messageFontSize() { return 14f; }
    @Override public float reactionEmojiFontSize() { return 23f; } // x1.75 version
    @Override public float reactionCountFontSize() { return 21f; }
}