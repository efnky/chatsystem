package fr.insa.chatsystem.db.models;

public enum Reaction {
    HEART("❤️"),
    LOL("😂"),
    UP("👍"),
    DOWN("👎"),
    OMG("😮"),
    SAD("🥲"),
    NONE("")
    ;

    private final String emoji;
    Reaction(String label) {
        this.emoji = label;
    }

    public String getEmoji() {
        return emoji;
    }

    public static Reaction from(String raw) {
        if (raw == null || raw.isBlank()) {
            return NONE;
        }

        String normalized = raw.trim().toUpperCase(java.util.Locale.ROOT);
        try {
            return Reaction.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            for (Reaction reaction : Reaction.values()) {
                if (reaction.getEmoji().equals(raw)) {
                    return reaction;
                }
            }
            return NONE;
        }
    }
}
