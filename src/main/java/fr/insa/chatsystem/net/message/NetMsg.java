package fr.insa.chatsystem.net.message;

import fr.insa.chatsystem.net.JSONSerializable;
import org.json.JSONObject;

import java.net.InetAddress;
import java.util.UUID;

/**
 * Represents a logical message together with addressing information intended
 * to be transported inside a UDP datagram.
 * <p>
 * A {@code DatagramMsg} has:
 * <ul>
 *     <li>a {@link IMsgType} describing the semantic type of the message</li>
 *     <li>target network information (address/port), or broadcast semantics</li>
 *     <li>owner network information (address/port)</li>
 *     <li>a JSON representation of all these fields</li>
 * </ul>
 *
 * @param <T> enum type representing the different possible message types
 */
public class NetMsg<T extends IMsgType> implements JSONSerializable {

    /**
     * Identifier of the user who created and sent this discovery message.
     */
    private final UUID owner;

    /**
     * Target network address.
     * If {@code null}, the message is considered a broadcast.
     */
    protected final InetAddress targetAddress;

    /** Target UDP port. */
    protected final int targetPort;

    /** Owner/sender network address. */
    protected final InetAddress ownerAddress;

    /** Owner/sender UDP port. */
    protected final int ownerPort;

    /** Semantic type of the message. */
    protected final T type;

    /** JSON representation of the message. */
    protected final JSONObject json;

    /** {@code true} if this message is intended for broadcast, {@code false} otherwise. */
    protected final boolean broadcast;

    /*-----------------------------------------Constructors-----------------------------------*/

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
    public NetMsg(UUID owner, T type,
                  InetAddress targetAddress,
                  int targetPort,
                  InetAddress ownerAddress,
                  int ownerPort) {
        this.owner = owner;

        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        if (ownerAddress == null) {
            throw new IllegalArgumentException("ownerAddress cannot be null");
        }
        if (ownerPort <= 0 || ownerPort > 65535) {
            throw new IllegalArgumentException("ownerPort out of range: " + ownerPort);
        }

        this.type = type;
        this.targetAddress = targetAddress;
        this.targetPort = targetPort;
        this.ownerAddress = ownerAddress;
        this.ownerPort = ownerPort;

        this.broadcast = (targetAddress == null);

        // Build JSON representation
        this.json = new JSONObject();
        fillJSON();
    }

    private void fillJSON(){
        json.put("owner", owner);
        json.put("type", type.getLabel());

        if (broadcast) {
            json.put("broadcast", true);
            json.put("targetPort", targetPort);
        } else {
            json.put("targetAddress", targetAddress.getHostAddress());
            json.put("targetPort", targetPort); // corrected: use targetPort, not ownerPort
        }

        json.put("ownerAddress", ownerAddress.getHostAddress());
        json.put("ownerPort", ownerPort);
    }

    /*----------------------------------------Getters---------------------------------------*/

    /**
     * Returns the identifier of the user who created this message.
     *
     * @return the owner id.
     */
    public UUID getOwner() {
        return owner;
    }

    /**
     * Returns the target IP address.
     *
     * @return target {@link InetAddress}, or {@code null} if this is a broadcast message
     */
    public InetAddress getTargetAddress() {
        return targetAddress;
    }

    /**
     * Returns the target UDP port.
     *
     * @return target port number
     */
    public int getTargetPort() {
        return targetPort;
    }

    /**
     * Returns the owner/sender IP address.
     *
     * @return owner {@link InetAddress}
     */
    public InetAddress getOwnerAddress() {
        return ownerAddress;
    }

    /**
     * Returns the owner/sender UDP port.
     *
     * @return owner port number
     */
    public int getOwnerPort() {
        return ownerPort;
    }

    /**
     * Returns the semantic type of the message.
     *
     * @return message type
     */
    public T getType() {
        return type;
    }

    /**
     * Indicates whether this message is intended for broadcast.
     *
     * @return {@code true} if broadcast, {@code false} otherwise
     */
    public boolean isBroadcast() {
        return broadcast;
    }

    /**
     * Returns the JSON representation of this message.
     *
     * @return {@link JSONObject} describing this message
     */
    @Override
    public JSONObject toJSON() {
        return json;
    }

    /**
     * Returns the string representation of this message, which is its JSON string.
     *
     * @return JSON string representation
     */
    @Override
    public String toString() {
        return toJSONString();
    }
}
