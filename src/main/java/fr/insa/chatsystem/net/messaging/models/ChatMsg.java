package fr.insa.chatsystem.net.messaging.models;

import fr.insa.chatsystem.net.messaging.api.MessagingContext;
import fr.insa.chatsystem.net.message.NetMsg;
import fr.insa.chatsystem.utils.DeterministicUUID;

import java.net.InetAddress;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Base type for all messaging-domain messages (text/image/reaction).
 *
 * <p>This is a sealed hierarchy: only permitted subclasses can exist.
 * This enforces a closed protocol and enables exhaustive pattern matching.</p>
 *
 */
public abstract sealed class ChatMsg extends NetMsg<ChatMsgType>
        permits TextMsg, ImageMsg, ReactionMsg
{

    private final UUID msgId;
    private final UUID targetID;
    private final LocalDate dateStamp;
    private final LocalTime timeStamp;

    /**
     * Instantiates a {@code DatagramMsg} with the given type and addressing information.
     *
     * @param type          message type, must not be {@code null}
     * @param targetAddress IP address of the target, or {@code null} for broadcast
     * @param targetPort    UDP port on which the message will be sent
     * @param ownerAddress  IP address of the owner/sender, must not be {@code null}
     * @param ownerPort     UDP port to reach the owner/sender
     * @throws IllegalArgumentException if {@code type} or {@code ownerAddress} is {@code null},
     *                                  or if {@code ownerPort} is out of valid range
     */
    public ChatMsg(UUID owner, UUID targetID, ChatMsgType type, InetAddress targetAddress, int targetPort, InetAddress ownerAddress, int ownerPort, LocalDate dateStamp, LocalTime timeStamp) {
        super(owner, type, targetAddress, targetPort, ownerAddress, ownerPort);
        this.targetID = targetID;
        this.msgId = DeterministicUUID.from(owner, dateStamp, timeStamp);
        this.dateStamp = dateStamp;
        this.timeStamp = timeStamp;
        json.put("msgId", msgId);
        json.put("targetID", targetID);
        json.put("dateStamp", dateStamp.toString());
        json.put("timeStamp", timeStamp.toString());
    }

    public ChatMsg(UUID msgId, UUID owner, UUID targetID, ChatMsgType type, InetAddress targetAddress, int targetPort, InetAddress ownerAddress, int ownerPort, LocalDate dateStamp, LocalTime timeStamp) {
        super(owner, type, targetAddress, targetPort, ownerAddress, ownerPort);
        this.msgId = msgId;
        this.targetID = targetID;
        this.dateStamp = dateStamp;
        this.timeStamp = timeStamp;
        json.put("msgId", msgId);
        json.put("targetID", targetID);
        json.put("dateStamp", dateStamp.toString());
        json.put("timeStamp", timeStamp.toString());
    }

    /*---------------------------SERIALIZATION----------------------------*/

    /**
     * Serializes subclass-specific fields into the JSON representation.
     * <p>
     * The {@code "owner"} field is already added by {@code NetworkMsg}.
     * Implementations must not remove or override this key.
     */
    protected abstract void updateJSON();

    /*------------------------------BEHAVIOUR-----------------------------*/

    /**
     * Executes the logic associated with this message when it is received.
     * <p>
     * This method must only be called by the discovery transport layer
     * when a valid message has just been received.
     *
     * @param cxt discovery context providing the services required
     *            to process this message.
     */
    public abstract void handle(MessagingContext cxt);

    public UUID getMsgId() {return msgId;}

    public LocalDate getDateStamp() {
        return dateStamp;
    }

    public LocalTime getTimeStamp() {
        return timeStamp;
    }

    public UUID getTargetID() {
        return targetID;
    }
}
