package fr.insa.chatsystem.gui.view.theme;

import java.util.Objects;

public final class ThemeManager {

    private static volatile Theme theme = new LightTheme();

    private ThemeManager() {}

    public static Theme get() {
        return theme;
    }

    // pour plus tard (dark mode). Pour l’instant tu ne l’appelles pas.
    public static void set(Theme t) {
        theme = Objects.requireNonNull(t, "theme");
    }
}