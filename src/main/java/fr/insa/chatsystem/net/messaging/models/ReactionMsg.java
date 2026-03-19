package fr.insa.chatsystem.net.messaging.models;

import fr.insa.chatsystem.net.messaging.api.MessagingContext;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Chat message representing a reaction to a previously sent message.
 *
 * <p>Typical fields:</p>
 * <ul>
 *   <li>{@code targetMsgId}: id of the message being reacted to</li>
 *   <li>{@code reaction}: emoji or short code</li>
 *   <li>{@code timestamp}: reaction time</li>
 * </ul>
 */
public final class ReactionMsg extends ChatMsg {

    private final UUID targetMsgId;
    private final String reaction;

    /**
     * Instantiates a {@code DatagramMsg} with the given type and addressing information.
     *
     * @param owner is the unique integer the identifies the user owner (i.e. the creator) of the message.
     * @param targetAddress IP address of the target, or {@code null} for broadcast
     * @param targetPort    UDP port on which the message will be sent
     * @param ownerAddress  IP address of the owner/sender, must not be {@code null}
     * @param ownerPort     UDP port to reach the owner/sender
     * @throws IllegalArgumentException if {@code type} or {@code ownerAddress} is {@code null},
     *                                  or if {@code ownerPort} is out of valid range
     */
    public ReactionMsg(UUID owner,
                       UUID targetID,
                       InetAddress targetAddress,
                       int targetPort,
                       InetAddress ownerAddress,
                       int ownerPort,
                       UUID targetMsgId,
                       String reaction,
                       LocalDate dateStamp,
                       LocalTime timeStamp) {
        super(owner, targetID, ChatMsgType.REACTION_MSG, targetAddress, targetPort, ownerAddress, ownerPort, dateStamp, timeStamp);
        this.targetMsgId = targetMsgId;
        this.reaction = reaction;
        updateJSON();
    }

    public ReactionMsg(UUID msgId,
                       UUID owner,
                       UUID targetID,
                       InetAddress targetAddress,
                       int targetPort,
                       InetAddress ownerAddress,
                       int ownerPort,
                       UUID targetMsgId,
                       String reaction,
                       LocalDate dateStamp,
                       LocalTime timeStamp) {
        super(msgId, owner, targetID, ChatMsgType.REACTION_MSG, targetAddress, targetPort, ownerAddress, ownerPort, dateStamp, timeStamp);
        this.targetMsgId = targetMsgId;
        this.reaction = reaction;
        updateJSON();
    }

    @Override
    protected void updateJSON() {
        json.put("targetMsgId", targetMsgId);
        json.put("reaction", reaction);
    }

    @Override
    public void handle(MessagingContext cxt) {
        cxt.getEventManager().notifyReceiving(this);
    }

    public UUID getTargetMsgId() {
        return targetMsgId;
    }

    public String getReaction() {
        return reaction;
    }

    /*--------------------------------------JSON-Factory-----------------------------------------*/

    public static ReactionMsg fromJSON(JSONObject json){
        try {
            if(!json.getString("type").equals(ChatMsgType.REACTION_MSG.getLabel())){
                // the json do not correspond to a ConnectionMsg
                throw new IllegalArgumentException("Invalid JSON : the json type do not match with ReactionMsg");
            }
            UUID msgId = UUID.fromString(json.getString("msgId"));
            UUID owner = UUID.fromString(json.getString("owner"));
            UUID targetID = UUID.fromString(json.getString("targetID"));
            String targetAddress = json.getString("targetAddress");
            int targetPort = json.getInt("targetPort");
            String ownerAddress = json.getString("ownerAddress");
            int ownerPort = json.getInt("ownerPort");
            UUID targetMsgId = UUID.fromString(json.getString("targetMsgId"));
            String reaction = json.getString("reaction");
            String dateStamp = json.getString("dateStamp");
            String timeStamp = json.getString("timeStamp");
            try{
                return new ReactionMsg(
                        msgId,
                        owner,
                        targetID,
                        InetAddress.getByName(targetAddress),
                        targetPort,
                        InetAddress.getByName(ownerAddress),
                        ownerPort,
                        targetMsgId,
                        reaction,
                        LocalDate.parse(dateStamp),
                        LocalTime.parse(timeStamp));
            }catch (UnknownHostException e){
                throw new IllegalArgumentException("Invalid JSON : An address in the JSON is not recognize : "+ e.getMessage());
            }
        }catch (JSONException e){
            throw new IllegalArgumentException("Invalid JSON attributes: "+e.getMessage());
        }
    }
}
