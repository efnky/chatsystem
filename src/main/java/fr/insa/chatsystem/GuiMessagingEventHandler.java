package fr.insa.chatsystem;

import fr.insa.chatsystem.gui.api.*;
import fr.insa.chatsystem.net.messaging.events.MessagingListener;
import fr.insa.chatsystem.net.messaging.models.ImageMsg;
import fr.insa.chatsystem.net.messaging.models.ReactionMsg;
import fr.insa.chatsystem.net.messaging.models.TextMsg;
import fr.insa.chatsystem.db.models.Reaction;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GuiMessagingEventHandler implements MessagingListener {

    private final AppContext appContext;
    private final GuiFacade gui;

    public GuiMessagingEventHandler(AppContext appContext, GuiFacade gui) {
        this.appContext = appContext;
        this.gui = gui;
    }


    /**
     * @param msg decoded message (never null)
     */
    @Override
    public void onTextMessageReceived(TextMsg msg) {

        gui.apply(
                new GuiUpdate.MessageAppended(
                        msg.getOwner().toString(),
                        textMsg(
                                msg.getMsgId().toString(),
                                msg.getOwner().toString(),
                                false,
                                msg.getContent(),
                                true,
                                null
                        )
                )
        );

        // maj de la conv.
        gui.apply(new GuiUpdate.ConversationUpserted(
                new ConversationSummaryUI(msg.getOwner().toString(), appContext.db().getPseudoFromUUID(msg.getOwner()),
                        msg.getContent(), Instant.now(), 0)
        ));
    }

    /**
     * @param msg
     */
    @Override
    public void onTextMessageSent(TextMsg msg) {
        gui.apply(new GuiUpdate.MessageAppended(
                msg.getTargetID().toString(),
                textMsg(msg.getMsgId().toString(),msg.getTargetID().toString(), true, msg.getContent(), false, null)
        ));

        boolean succeed = true;
        // display the success or the fail
        if(succeed){
            gui.apply(new GuiUpdate.MessageDeliveryUpdated(
                    msg.getTargetID().toString(),
                    msg.getMsgId().toString(),
                    true,
                    null
            ));
        }else{
            gui.apply(new GuiUpdate.MessageDeliveryUpdated(
                    msg.getTargetID().toString(),
                    msg.getMsgId().toString(),
                    false,
                    "Network Error"
            ));
        }

        // maj de la conv.
        gui.apply(new GuiUpdate.ConversationUpserted(
                new ConversationSummaryUI(msg.getTargetID().toString(), appContext.db().getPseudoFromUUID(msg.getTargetID()),
                        msg.getContent(), Instant.now(), 0)
        ));
    }

    /**
     * @param msg decoded message (never null)
     */
    @Override
    public void onReactionReceived(ReactionMsg msg) {
        String key = msg.getOwner().toString()+"::" +msg.getMsgId().toString();

        // Map<String, String> myReactions = new HashMap<>();
        String prev = appContext.myReactions().get(key);


        Reaction reaction = Reaction.from(msg.getReaction());
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
                msg.getOwner().toString(),
                msg.getTargetMsgId().toString(),
                list,
                now
        ));
    }

    /**
     * @param msg
     */
    @Override
    public void onReactionSent(ReactionMsg msg) {

    }

    /**
     * @param msg decoded message (never null)
     */
    @Override
    public void onImageReceived(ImageMsg msg) {

    }

    /**
     * @param msg
     */
    @Override
    public void onImageSent(ImageMsg msg) {

    }

    private static MessageUI textMsg(
            String msgId,
            String convId,
            boolean isMine,
            String text,
            boolean delivered,
            String failReason
    ) {
        return new MessageUI(
                msgId,
                convId,
                isMine,
                Instant.now(),
                MessageType.TEXT,
                text,
                null,
                delivered,
                failReason,
                List.of(),
                null
        );
    }

    private static MessageUI imageMsg(
            String msgId,
            String convId,
            boolean isMine,
            String fileRef,
            boolean delivered,
            String failReason
    ) {
        return new MessageUI(
                msgId,
                convId,
                isMine,
                Instant.now(),
                MessageType.IMAGE,
                null,
                fileRef,
                delivered,
                failReason,
                List.of(),
                null
        );
    }
}
