package fr.insa.chatsystem.gui.app;

import fr.insa.chatsystem.gui.api.GuiAction;
import fr.insa.chatsystem.gui.api.GuiActionHandler;
import fr.insa.chatsystem.gui.api.Screen;
import fr.insa.chatsystem.gui.view.LoginView;
import fr.insa.chatsystem.gui.view.MessengerPlaceholderView;
import fr.insa.chatsystem.gui.view.MessengerView;

import java.util.Objects;

/**
 * Builds the Swing UI graph (views + shell + router) and wires callbacks.
 */
public final class GuiBootstrap {

    private final ScreenRouter router = new ScreenRouter();
    private final AppShell shell = new AppShell("ChatSystem", router);

    private final LoginView loginView = new LoginView();
    private final MessengerView messengerView = new MessengerView();
    public MessengerView messengerView() { return messengerView; }


    private GuiActionHandler actionHandler = GuiActionHandler.noop();

    public GuiBootstrap() {
        router.register(Screen.LOGIN, loginView);
        router.register(Screen.MESSENGER, messengerView);

        loginView.setOnConnect(pseudo -> actionHandler.onAction(new GuiAction.Connect(pseudo)));
    }

    public void setActionHandler(GuiActionHandler handler) {
        this.actionHandler = Objects.requireNonNull(handler, "handler");
    }

    /**
     * Called by the facade to hook the window closing event.
     */
    public void setOnClose(Runnable onClose) {
        shell.setOnClose(onClose);
    }

    public void start() {
        router.show(Screen.LOGIN);
        shell.show();
    }

    public void stop() {
        shell.close();
    }

    // minimal hooks used by the facade implementation
    public void showScreen(Screen screen) {
        router.show(screen);
    }

    public void setLoginStatus(String textOrBlank) {
        loginView.setStatus(textOrBlank);
    }
}
