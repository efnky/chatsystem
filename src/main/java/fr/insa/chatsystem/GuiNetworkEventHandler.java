package fr.insa.chatsystem;

import fr.insa.chatsystem.gui.api.ConversationSummaryUI;
import fr.insa.chatsystem.gui.api.GuiUpdate;
import fr.insa.chatsystem.net.discovery.events.NetworkListener;
import fr.insa.chatsystem.net.discovery.events.PseudoRequestMsg;

import java.time.Instant;


public class GuiNetworkEventHandler implements NetworkListener {

    private final AppContext appContext;

    public GuiNetworkEventHandler(AppContext appContext) {
        this.appContext = appContext;
    }

    @Override
    public void onPseudoValidated(PseudoRequestMsg msg) {
        appContext.gui().apply(new GuiUpdate.ConversationUpserted(
                new ConversationSummaryUI(
                        msg.getOwner().toString(),
                        msg.getRequestedPseudo(),
                        "",
                        Instant.now(),
                        0
                )
        ));

        appContext.gui().apply(
                new GuiUpdate.ConversationPresenceUpdated(
                        msg.getOwner().toString(),
                        true
                )
        );
    }

    public void onNetworkConnected() {}

    public void onNetworkDisconnected() {}

    public void onNetworkConnectionLost() {}

    public void onNetworkConnectionFailed() {}
}
