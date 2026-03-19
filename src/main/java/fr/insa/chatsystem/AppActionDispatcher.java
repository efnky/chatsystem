package fr.insa.chatsystem;

import fr.insa.chatsystem.db.models.*;
import fr.insa.chatsystem.gui.api.*;
import fr.insa.chatsystem.net.discovery.ContactList;
import fr.insa.chatsystem.net.discovery.User;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

public class AppActionDispatcher {

    private AppContext appContext;
    private final GuiFacade gui;

    public AppActionDispatcher(AppContext appContext, GuiFacade gui) {
        this.appContext = appContext;
        this.gui = gui;
    }

    private void handleHostUserConnection(GuiAction.Connect c) {
        if (c.requestedPseudo().isBlank()) {
            gui.apply(new GuiUpdate.ConnectionStateChanged(ConnectionState.ERROR, "Pseudo required"));
            return;
        }

        boolean isConnected = appContext.networker().connectAs(c.requestedPseudo());
        if(!isConnected){
            gui.apply(new GuiUpdate.ConnectionStateChanged(ConnectionState.ERROR, "Pseudo is already used"));
            return;
        }

        appContext.contactList().getHostUser().setPseudo(c.requestedPseudo());
        gui.apply(new GuiUpdate.ConnectionStateChanged(ConnectionState.CONNECTED, "Connection succeed with "+ c.requestedPseudo()));

        List<User> users = ContactList.getInstance().getConnectedUsers();
        List<Contact> contacts = new ArrayList<>();
        for(User u : users){
            contacts.add(new Contact(u.getID(), u.getPseudo()));
        }
        appContext.db().updateDB(contacts);
        return;
    }

    private void handleHostUserDisconnection(GuiAction.Disconnect c) {
        System.out.println("=> Core received Disconnect. (stop networking / cleanup here)");

        if(appContext.networker() != null && appContext.networker().isConnected()){
            appContext.networker().disconnect();
        }

        // Reset UI to login
        gui.apply(new GuiUpdate.ConnectionStateChanged(ConnectionState.DISCONNECTED, null));
        gui.apply(new GuiUpdate.ShowScreen(Screen.LOGIN));

        List<Contact> contacts =appContext.db().getAllContacts();
        for (Contact co :contacts){
            gui.apply(
                    new GuiUpdate.ConversationPresenceUpdated(
                            co.getUserId().toString(),
                            false
                    )
            );
        }
    }

    private void handlePseudoChange(GuiAction.ChangePseudo cp){
        String pseudo = cp.newPseudo().trim();
        if (pseudo.isEmpty()) {
            gui.apply(new GuiUpdate.PseudoChangeFailed("Pseudo required"));
            return;
        }

        //boolean ok = backend.tryChangePseudo(pseudo); // <-- ta fonction bool
        boolean ok = appContext.networker().changePseudo(pseudo);
        if (!ok) {
            gui.apply(new GuiUpdate.PseudoChangeFailed("Pseudo is already used"));
            return;
        }

        gui.apply(new GuiUpdate.PseudoChanged(pseudo));
        gui.apply(new GuiUpdate.ConversationUpserted(
                new ConversationSummaryUI(
                        cp.conversationId(),
                        pseudo,
                        "",
                        Instant.now(),
                        0
                )
        ));

        gui.apply(
                new GuiUpdate.ConversationPresenceUpdated(
                        cp.conversationId(),
                        true
                )
        );
        // optionnel : toast
        // gui.apply(new GuiUpdate.Toast(ToastLevel.INFO, "Pseudo updated"));
        return;
    }

    private void handleClosure(GuiAction.Close c){
        Thread shutdownThread = new Thread(appContext::requestShutdown, "app-shutdown");
        shutdownThread.start();
    }

    private void handleSendImage(GuiAction.SendImage i){
        i.fileRef();
        Path path = Path.of(i.fileRef());
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(path);
            System.out.println("=> Sending image : "+bytes.toString());

            //appContext.messenger().sendImage(UUID.fromString(i.conversationId()), bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



    public void setActionHandler() {
        gui.setActionHandler(action -> {
            switch(action){
                case GuiAction.Connect c -> {
                    handleHostUserConnection(c);
                }
                case GuiAction.Disconnect d-> {
                    handleHostUserDisconnection(d);
                }
                case GuiAction.Close c -> {
                    handleClosure(c);
                }
                case GuiAction.SendText t ->{// selon ton archi
                    if (appContext.contactList().getUserFromUUID(UUID.fromString(t.conversationId())) == null) {
                        gui.apply(new GuiUpdate.Toast(ToastLevel.ERROR, "USER "+t.conversationId()+" is offline"));
                        return;
                    }
                    appContext.messenger().sendMessage(UUID.fromString(t.conversationId()), t.text());
                }
                case GuiAction.SendImage i -> {
                    handleSendImage(i);
                }
                case GuiAction.SetReaction r -> {
                    if (appContext.contactList().getUserFromUUID(UUID.fromString(r.conversationId())) == null) {
                        gui.apply(new GuiUpdate.Toast(ToastLevel.ERROR, "USER "+r.conversationId()+" is offline"));
                        return;
                    }

                    String key = r.conversationId() + "::" + r.messageId();

                    // Map<String, String> myReactions = new HashMap<>();
                    String prev = appContext.myReactions().get(key);
                    Reaction reaction = Reaction.from(r.emojiOrNull());
                    String payload = (reaction == Reaction.NONE) ? Reaction.NONE.name() : reaction.getEmoji();

                    boolean succeed = appContext.messenger().reactToMsg(
                            UUID.fromString(r.conversationId()),
                            UUID.fromString(r.messageId()),
                            payload
                    );
                    if(succeed){
                        if (reaction == Reaction.NONE) {
                            appContext.myReactions().remove(key);
                        } else {
                            appContext.myReactions().put(key, reaction.getEmoji());
                        }

                        // Rebuild counts (only "me" in this test)
                        Map<String, Integer> counts = new LinkedHashMap<>();
                        String now = appContext.myReactions().get(key);
                        if (now != null) counts.put(now, 1);

                        List<ReactionUI> list = counts.entrySet().stream()
                                .map(e -> new ReactionUI(e.getKey(), e.getValue()))
                                .toList();
                        gui.apply(new GuiUpdate.MessageReactionsUpdated(
                                r.conversationId(),
                                r.messageId(),
                                list,
                                now
                        ));
                    }
                }
                case GuiAction.SelectConversation c-> {
                    gui.apply(new GuiUpdate.ConversationSelected(c.conversationId()));
                    appContext.setSelectedChatroom(appContext.db().openChatroom(UUID.fromString(c.conversationId())));
                }
                case GuiAction.ChangePseudo cp -> {
                    handlePseudoChange(cp);
                }
            }
        });
    }

    /*-------------------------------------------------------UTILS----------------------------------------------------*/



}
