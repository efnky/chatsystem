package fr.insa.chatsystem.gui.api;

import java.util.Objects;

/**
 * Callback implemented by the core to receive GUI user intents.
 */
@FunctionalInterface
public interface GuiActionHandler {

    /**
     * Receives a user intent emitted by the GUI.
     *
     * @param action non-null action
     */
    void onAction(GuiAction action);

    static GuiActionHandler noop() {
        return action -> Objects.requireNonNull(action, "action");
    }

}

