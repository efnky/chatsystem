package fr.insa.chatsystem.gui.app;

import fr.insa.chatsystem.gui.api.Screen;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * Simple screen router based on CardLayout.
 */
public final class ScreenRouter {

    private final CardLayout layout = new CardLayout();
    private final JPanel root = new JPanel(layout);

    public JPanel root() {
        return root;
    }

    public void register(Screen screen, JComponent component) {
        Objects.requireNonNull(screen, "screen");
        Objects.requireNonNull(component, "component");
        root.add(component, screen.name());
    }

    public void show(Screen screen) {
        Objects.requireNonNull(screen, "screen");
        layout.show(root, screen.name());
    }
}
