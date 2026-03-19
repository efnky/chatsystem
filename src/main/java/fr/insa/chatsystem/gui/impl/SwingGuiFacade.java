package fr.insa.chatsystem.gui.impl;

import fr.insa.chatsystem.gui.api.*;
import fr.insa.chatsystem.gui.app.GuiBootstrap;
import fr.insa.chatsystem.gui.app.GuiThreading;
import fr.insa.chatsystem.gui.presenter.MessengerPresenter;
import fr.insa.chatsystem.gui.presenter.RootPresenter;
import fr.insa.chatsystem.gui.state.Store;

import java.util.ArrayDeque;
import java.util.Objects;

/**
 * Swing implementation of GuiFacade.
 * Guarantees EDT safety for start/stop/apply.
 */
public final class SwingGuiFacade implements GuiFacade {

    private final GuiBootstrap bootstrap;

    private GuiActionHandler actionHandler = GuiActionHandler.noop();
    private final ArrayDeque<GuiUpdate> pendingUpdates = new ArrayDeque<>();

    private boolean started = false;

    private final Store store = new Store();
    private RootPresenter rootPresenter;


    public SwingGuiFacade(GuiBootstrap bootstrap, String hostId) {
        store.state().setHostId(hostId);
        this.bootstrap = Objects.requireNonNull(bootstrap, "bootstrap");
    }

    @Override
    public void start() {
        GuiThreading.runOnEdt(() -> {
            if (started) return;

            bootstrap.setActionHandler(actionHandler);

            // Notify the core when the user closes the window.
            bootstrap.setOnClose(() -> actionHandler.onAction(new GuiAction.Close()));

            bootstrap.start();
            started = true;
            rootPresenter = new fr.insa.chatsystem.gui.presenter.RootPresenter(bootstrap, store, actionHandler);

            // Flush pending updates that arrived before start()
            while (!pendingUpdates.isEmpty()) {
                applyOnEdt(pendingUpdates.removeFirst());
            }
        });
    }

    @Override
    public void stop() {
        GuiThreading.runOnEdt(() -> {
            if (!started) return;
            bootstrap.stop();
            started = false;
            rootPresenter = null;
            pendingUpdates.clear();
        });
    }

    @Override
    public void setActionHandler(GuiActionHandler handler) {
        this.actionHandler = Objects.requireNonNull(handler, "handler");
        GuiThreading.runOnEdt(() -> bootstrap.setActionHandler(this.actionHandler));
    }

    @Override
    public void apply(GuiUpdate update) {
        System.out.println("APPLYING : " + update);
        Objects.requireNonNull(update, "update");
        GuiThreading.runOnEdt(() -> {
            if (!started) {
                pendingUpdates.addLast(update);
                return;
            }
            applyOnEdt(update);
        });

    }

    private void applyOnEdt(GuiUpdate update) {
       if (rootPresenter != null) rootPresenter.onUpdate(update);
    }

}
