package fr.insa.chatsystem;

import fr.insa.chatsystem.db.models.*;
import fr.insa.chatsystem.gui.api.*;
import fr.insa.chatsystem.gui.app.GuiBootstrap;
import fr.insa.chatsystem.gui.impl.SwingGuiFacade;
import fr.insa.chatsystem.net.discovery.ContactList;
import fr.insa.chatsystem.net.discovery.User;

import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

public class Gui implements ContactList.Listener{

    private final AppContext appContext;
    private final GuiBootstrap bootstrap;
    private final SwingGuiFacade gui;

    private final Map<String, String> myReactions = new HashMap<>();

    private Chatroom selectedChatroom;

    private GuiNetworkEventHandler networkEventHandler;
    private GuiMessagingEventHandler messagingEventHandler;
    private AppActionDispatcher appActionDispatcher;

    public Gui(AppContext appContext) {
        this.appContext = appContext;
        this.bootstrap = new GuiBootstrap();
        this.gui = new SwingGuiFacade(bootstrap, appContext.contactList().getHostUser().getID().toString());
        this.appContext.setGui(gui);

        appContext.networker().subscribe(networkEventHandler =  new GuiNetworkEventHandler(appContext));
        appContext.messenger().subscribe(messagingEventHandler = new GuiMessagingEventHandler(appContext, gui));
        this.appActionDispatcher = new AppActionDispatcher(appContext,gui);

        this.selectedChatroom = null;
        appContext.contactList().subscribe(this);

        loadAllConversations();
    }

    public void run() {
        appActionDispatcher.setActionHandler();
        gui.start();
    }

    private void loadAllConversations(){
        List<Contact> contacts = appContext.db().getAllContacts();

        for(Contact c : contacts){
            Chatroom chatroom = appContext.db().openChatroom(c.getUserId());
            chatroom.loadHistory();
            gui.apply(new GuiUpdate.ConversationUpserted(
                    new ConversationSummaryUI(
                            c.getUserId().toString(),
                            c.getUsername(),
                            "",
                            Instant.now(),
                            0
                    )
            ));

            Message lastMessage = null;

            /**
             * Load Messages
             */
            for(Message msg : chatroom.getMessages()){

                if(lastMessage == null){
                    lastMessage = msg;
                }else if(lastMessage.getTimestamp().isBefore(msg.getTimestamp())){
                    lastMessage = msg;
                }


                ArrayList<ReactionUI> reacts = new ArrayList<>();
                String myReactionEmojiOrNull = null;
                if (msg.getReaction() != Reaction.NONE) {
                    reacts.add(new ReactionUI(msg.getReaction().getEmoji(), 1));
                    myReactionEmojiOrNull = msg.getReaction().getEmoji();
                }

                Instant instant = msg.getTimestamp().atZone(ZoneId.of("Europe/Paris")).toInstant();
                gui.apply(new GuiUpdate.MessageAppended(
                        c.getUserId().toString(),
                        new MessageUI(
                                String.valueOf(msg.getId()),
                                c.getUserId().toString(),
                                msg.getDirection().equals(Direction.SENT),
                                instant,
                                MessageType.TEXT,
                                msg.getContent(),
                                null,
                                true,
                                null,
                                reacts,
                                myReactionEmojiOrNull
                        )
                ));
            }

            /**
             * displays the last message
             */
            if(lastMessage != null){
                Instant instant = lastMessage.getTimestamp().atZone(ZoneId.of("Europe/Paris")).toInstant();
                gui.apply(new GuiUpdate.ConversationUpserted(
                        new ConversationSummaryUI(
                                c.getUserId().toString(),
                                c.getUsername(),
                                lastMessage.getContent(),
                                instant,
                                0
                        )
                ));
            }
        }
    }


    /**
     * @param user
     */
    @Override
    public void onUserAdded(User user) {
        String lastMessage;
        try{
             lastMessage = appContext.db().getLastMessageFrom(user.getID()).getContent();
        }catch(NoSuchElementException e){
             lastMessage = "";
        }

        // Displays the conversation of the user
        gui.apply(new GuiUpdate.ConversationUpserted(
                new ConversationSummaryUI(
                        user.getID().toString(),
                        user.getPseudo(),
                        lastMessage,
                        Instant.now(),
                        0
                )
        ));

        // Display the green light = user connected

        gui.apply(
                new GuiUpdate.ConversationPresenceUpdated(
                        user.getID().toString(),
                        true
                )
        );
    }

    /**
     *
     */
    @Override
    public void onUserRemoved(User user) {
        // Switch the light of the user in green.
        gui.apply(
                new GuiUpdate.ConversationPresenceUpdated(
                        user.getID().toString(),
                        false
                )
        );
    }

    /**
     *
     */
    @Override
    public void onPseudoChange(User user, String oldPseudo, String newPseudo) {

    }
}
