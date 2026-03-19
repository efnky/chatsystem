package fr.insa.chatsystem.gui.view;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Locale;

final class EmojiSupport {

    private static final String[] EMOJI_FONT_CANDIDATES = {
            "Noto Color Emoji",
            "Noto Emoji",
            "Segoe UI Emoji",
            "Apple Color Emoji",
            "Twemoji Mozilla",
            "EmojiOne Color"
    };

    private static final String EMOJI_FONT_NAME = findEmojiFontName();
    private static final boolean HAS_EMOJI_FONT = EMOJI_FONT_NAME != null;

    private EmojiSupport() {}

    static Font emojiFont(Font base, float size) {
        Font sized = base.deriveFont(size);
        if (!HAS_EMOJI_FONT) {
            return sized;
        }
        return new Font(EMOJI_FONT_NAME, Font.PLAIN, Math.round(size));
    }

    static boolean hasEmojiFont() {
        return HAS_EMOJI_FONT;
    }

    static boolean canDisplay(Font font, String text) {
        if (text == null || text.isEmpty()) return true;
        return font.canDisplayUpTo(text) == -1;
    }

    static String fallbackFor(String emoji) {
        if (emoji == null) return "?";
        return switch (emoji) {
            case "\u2764\uFE0F", "\u2665\uFE0F", "\u2665" -> "<3";
            case "\uD83D\uDE02" -> ":D";
            case "\uD83D\uDC4D" -> "+1";
            case "\uD83D\uDC4E" -> "-1";
            case "\uD83D\uDE2E" -> ":O";
            case "\uD83D\uDE22" -> ":'(";
            case "\uD83D\uDD0D" -> "Search";
            default -> "?";
        };
    }

    private static String findEmojiFontName() {
        String[] names = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();
        for (String candidate : EMOJI_FONT_CANDIDATES) {
            for (String name : names) {
                if (name.equalsIgnoreCase(candidate)) {
                    return name;
                }
            }
        }
        return null;
    }
}
