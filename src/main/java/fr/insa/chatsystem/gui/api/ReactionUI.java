package fr.insa.chatsystem.gui.api;

import java.util.Objects;

/**
 * Aggregated reaction (emoji + count) used to render iMessage-like reaction bubble.
 */
public record ReactionUI(String emoji, int count) {
    public ReactionUI {
        Objects.requireNonNull(emoji, "emoji");
        if (count <= 0) throw new IllegalArgumentException("count must be > 0");
    }
}

