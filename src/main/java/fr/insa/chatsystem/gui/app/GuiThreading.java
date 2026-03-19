package fr.insa.chatsystem.gui.app;

import javax.swing.SwingUtilities;

/**
 * Swing threading utilities.
 */
public final class GuiThreading {

    private GuiThreading() { }

    public static boolean isEdt() {
        return SwingUtilities.isEventDispatchThread();
    }

    public static void runOnEdt(Runnable task) {
        if (isEdt()) task.run();
        else SwingUtilities.invokeLater(task);
    }
}

