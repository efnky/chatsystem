package fr.insa.chatsystem.net.messaging.models;

import fr.insa.chatsystem.net.messaging.api.MessagingContext;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Base64;
import java.util.UUID;

/**
 * Chat message carrying an image payload.
 *
 * <p>Be explicit about representation:</p>
 * <ul>
 *   <li>byte[] payload for small images (beware size).</li>
 *   <li>timestamp of the creation of the message.</li>
 * </ul>
 */
public final class ImageMsg extends ChatMsg {

    private final byte[] blob;

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
    public ImageMsg(UUID owner, UUID targetID, InetAddress targetAddress, int targetPort, InetAddress ownerAddress, int ownerPort, byte[] blob, LocalDate dateStamp, LocalTime timeStamp) {
        super(owner, targetID, ChatMsgType.IMAGE_MSG, targetAddress, targetPort, ownerAddress, ownerPort, dateStamp, timeStamp);
        this.blob = blob;
        updateJSON();
    }

    public ImageMsg(UUID msgId, UUID owner, UUID targetID, InetAddress targetAddress, int targetPort, InetAddress ownerAddress, int ownerPort, byte[] blob, LocalDate dateStamp, LocalTime timeStamp) {
        super(msgId, owner, targetID, ChatMsgType.IMAGE_MSG, targetAddress, targetPort, ownerAddress, ownerPort, dateStamp, timeStamp);
        this.blob = blob;
        updateJSON();
    }

    @Override
    protected void updateJSON() {
        json.put("blob", Base64.getEncoder().encodeToString(blob));
    }

    @Override
    public void handle(MessagingContext cxt) {
        cxt.getEventManager().notifyReceiving(this);
    }

    public byte[] getBlob() {
        return blob;
    }

    /*--------------------------------------JSON-Factory-----------------------------------------*/

    public static ImageMsg fromJSON(JSONObject json){
        try {
            if(!json.getString("type").equals(ChatMsgType.IMAGE_MSG.getLabel())){
                // the json do not correspond to a ConnectionMsg
                throw new IllegalArgumentException("Invalid JSON : the json type do not match with ImageMsg");
            }
            UUID msgId = UUID.fromString(json.getString("msgId"));
            UUID owner = UUID.fromString(json.getString("owner"));
            UUID targetID = UUID.fromString(json.getString("targetID"));
            String targetAddress = json.getString("targetAddress");
            int targetPort = json.getInt("targetPort");
            String ownerAddress = json.getString("ownerAddress");
            int ownerPort = json.getInt("ownerPort");
            String blobStr = json.getString("blob");
            byte[] blob = Base64.getDecoder().decode(blobStr);
            String dateStamp = json.getString("dateStamp");
            String timeStamp = json.getString("timeStamp");
            try{
                return new ImageMsg(
                        msgId,
                        owner,
                        targetID,
                        InetAddress.getByName(targetAddress),
                        targetPort,
                        InetAddress.getByName(ownerAddress),
                        ownerPort,
                        blob,
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
