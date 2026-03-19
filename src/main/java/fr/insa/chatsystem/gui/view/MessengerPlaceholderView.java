package fr.insa.chatsystem.gui.view;

import javax.swing.*;
import java.awt.*;

/**
 * Temporary placeholder for messenger screen.
 * Will be replaced by real iMessage-like layout later.
 */
public final class MessengerPlaceholderView extends JPanel {

    public MessengerPlaceholderView() {
        setLayout(new BorderLayout());
        add(new JLabel("Messenger (placeholder)", SwingConstants.CENTER), BorderLayout.CENTER);
    }
}

