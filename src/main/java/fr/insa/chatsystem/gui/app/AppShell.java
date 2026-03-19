package fr.insa.chatsystem.gui.app;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;

/**
 * Main application window (JFrame).
 */
public final class AppShell {

    private final JFrame frame;
    private final ScreenRouter router;

    private Runnable onClose = null;

    public AppShell(String title, ScreenRouter router) {
        this.router = Objects.requireNonNull(router, "router");
        this.frame = new JFrame(Objects.requireNonNull(title, "title"));

        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.setMinimumSize(new Dimension(900, 600));
        frame.setLocationRelativeTo(null);
        frame.setContentPane(router.root());

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.out.println("WINDOW CLOSING EVENT");
                System.out.println("onClose=" + onClose);
                Runnable cb = onClose;
                if (cb != null) cb.run();
            }
        });
    }

    /**
     * Callback invoked when the window is closing (user clicks the close button).
     * This is the right place to notify the core (e.g., Disconnect).
     */
    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    public void show() {
        frame.setVisible(true);
    }

    public void close() {
        frame.dispose();
    }
}
