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
 * Chat message carrying plain text content.
 *
 * <p>Typical fields:</p>
 * <ul>
 *   <li>{@code content}: the text body</li>
 *   <li>{@code timestamp}: creation time (if not already provided by {@code NetMsg})</li>
 * </ul>
 */
public final class TextMsg extends ChatMsg {

    private final String content;

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
    public TextMsg(UUID owner, UUID targetID,  InetAddress targetAddress, int targetPort, InetAddress ownerAddress, int ownerPort, String content, LocalDate dateStamp, LocalTime timeStamp) {
        super(owner, targetID, ChatMsgType.TEXT_MSG,targetAddress, targetPort, ownerAddress, ownerPort, dateStamp, timeStamp);
        this.content = content;
        updateJSON();
    }

    public TextMsg(UUID msgId, UUID owner, UUID targetID, InetAddress targetAddress, int targetPort, InetAddress ownerAddress, int ownerPort, String content, LocalDate dateStamp, LocalTime timeStamp) {
        super(msgId, owner, targetID, ChatMsgType.TEXT_MSG,targetAddress, targetPort, ownerAddress, ownerPort, dateStamp, timeStamp);
        this.content = content;
        updateJSON();
    }

    @Override
    protected void updateJSON() {
        json.put("content", content);
    }

    @Override
    public void handle(MessagingContext cxt) {
        cxt.getEventManager().notifyReceiving(this);
    }

    public String getContent() {
        return content;
    }

    /*--------------------------------------JSON-Factory-----------------------------------------*/

    public static TextMsg fromJSON(JSONObject json){
        try {
            if(!json.getString("type").equals(ChatMsgType.TEXT_MSG.getLabel())){
                // the json do not correspond to a ConnectionMsg
                throw new IllegalArgumentException("Invalid JSON : the json type do not match with TextMsg");
            }
            UUID msgId = UUID.fromString(json.getString("msgId"));
            UUID owner = UUID.fromString(json.getString("owner"));
            UUID targetID = UUID.fromString(json.getString("targetID"));
            String targetAddress = json.getString("targetAddress");
            int targetPort = json.getInt("targetPort");
            String ownerAddress = json.getString("ownerAddress");
            int ownerPort = json.getInt("ownerPort");
            String content = json.getString("content");
            String dateStamp = json.getString("dateStamp");
            String timeStamp = json.getString("timeStamp");
            try{
                return new TextMsg(
                        msgId,
                        owner,
                        targetID,
                        InetAddress.getByName(targetAddress),
                        targetPort,
                        InetAddress.getByName(ownerAddress),
                        ownerPort,
                        content,
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
