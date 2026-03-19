package fr.insa.chatsystem.net.messaging.view;

import fr.insa.chatsystem.db.models.Reaction;
import fr.insa.chatsystem.net.messaging.api.MessagingContext;
import fr.insa.chatsystem.net.messaging.events.MessagingListener;
import fr.insa.chatsystem.net.messaging.models.ImageMsg;
import fr.insa.chatsystem.net.messaging.models.ReactionMsg;
import fr.insa.chatsystem.net.messaging.models.TextMsg;
import org.apache.logging.log4j.Level;

public class MessagingView implements MessagingListener {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";


    private MessagingView(){}

    public static void initialize(MessagingContext cxt){
        MessagingView v = new MessagingView();
        cxt.getEventManager().subscribe(v);
    }

    private void printlnBlue(String string){
        System.out.println(ANSI_BLUE + string + ANSI_RESET);
    }

    @Override
    public void onReactionReceived(ReactionMsg msg) {
        // Reaction details are logged in onReactionReceived.
        printlnBlue("[MESSENGER-IN] Reaction received from " + msg.getOwner().toString() + " on message " + msg.getMsgId().toString() + ": " + msg.getReaction());
    }

    @Override
    public void onReactionSent(ReactionMsg msg) {
        printlnBlue("[MESSENGER-OUT] Reaction sent to " + msg.getTargetID().toString() + " on message " + msg.getMsgId().toString() + ": " + msg.getReaction());
    }

    @Override
    public void onImageReceived(ImageMsg msg) {
        printlnBlue("[MESSENGER-IN] New Image received from "+msg.getOwner()+", its blob is: " + msg.getBlob().toString());
    }

    @Override
    public void onImageSent(ImageMsg msg) {
        printlnBlue("[MESSENGER-OUT] New Image sent to " + msg.getTargetID() +", its blob is: " + msg.getBlob().toString());
    }

    @Override
    public void onTextMessageReceived(TextMsg msg) {
        printlnBlue("[MESSENGER-IN] New Text Message received from "+ msg.getOwner() +": " + msg.getContent());;
    }

    @Override
    public void onTextMessageSent(TextMsg msg) {
        printlnBlue("[MESSENGER-OUT] New Text Message sent to "+msg.getTargetID()+": " + msg.getContent());
    }
}
