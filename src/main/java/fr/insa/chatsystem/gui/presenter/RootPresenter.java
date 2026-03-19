package fr.insa.chatsystem.gui.presenter;

import fr.insa.chatsystem.gui.api.*;
import fr.insa.chatsystem.gui.state.Store;
import fr.insa.chatsystem.gui.app.GuiBootstrap;

import java.util.Objects;

public final class RootPresenter {

    private final GuiBootstrap bootstrap;
    private final MessengerPresenter messengerPresenter;

    public RootPresenter(GuiBootstrap bootstrap, Store store, GuiActionHandler actions) {
        this.bootstrap = Objects.requireNonNull(bootstrap, "bootstrap");
        this.messengerPresenter = new MessengerPresenter(store, bootstrap.messengerView(), actions);
    }

    public void onUpdate(GuiUpdate update) {
        switch (update) {
            case GuiUpdate.ShowScreen s -> bootstrap.showScreen(s.screen());

            case GuiUpdate.ConnectionStateChanged c -> {
                if (c.state() == ConnectionState.ERROR) {
                    bootstrap.setLoginStatus(c.messageOrNull() == null ? "Connection error" : c.messageOrNull());
                    bootstrap.showScreen(Screen.LOGIN);
                } else if (c.state() == ConnectionState.CONNECTED) {
                    bootstrap.setLoginStatus(" ");
                    bootstrap.showScreen(Screen.MESSENGER);
                } else {
                    bootstrap.setLoginStatus(" ");
                    bootstrap.showScreen(Screen.LOGIN);
                }
            }

            default -> messengerPresenter.onUpdate(update);
        }
    }
}
