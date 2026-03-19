package fr.insa.chatsystem.gui.api;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Public entry-point of the GUI module.
 *
 * <p>Design goals:
 * <ul>
 *   <li>GUI is a pure presentation layer: it never talks to networking/database directly.</li>
 *   <li>Core -> GUI uses incremental {@link GuiUpdate} events.</li>
 *   <li>GUI -> Core uses {@link GuiAction} intents via a {@link GuiActionHandler}.</li>
 *   <li>Threading: implementations MUST ensure Swing EDT safety internally.</li>
 * </ul>
 */
public interface GuiFacade {

    /**
     * Starts the GUI (creates window, applies theme, shows initial screen).
     * Implementations must be idempotent.
     */
    void start();

    /**
     * Stops the GUI and releases resources. Implementations must be idempotent.
     */
    void stop();

    /**
     * Registers the action handler used by the GUI to emit user intents to the core.
     * Can be set once during bootstrap, but allowing rebind is convenient for tests.
     *
     * @param handler non-null handler
     */
    void setActionHandler(GuiActionHandler handler);

    /**
     * Applies an incremental update coming from the core.
     * Implementations MUST be safe to call from any thread (EDT rebinding is internal).
     *
     * @param update non-null update
     */
    void apply(GuiUpdate update);

    /**
     * Optional helper: apply a batch of updates in order.
     */
    default void applyAll(List<GuiUpdate> updates) {
        Objects.requireNonNull(updates, "updates");
        for (GuiUpdate u : updates) {
            apply(u);
        }
    }
}
