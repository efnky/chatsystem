package fr.insa.chatsystem.net.discovery.events;

import fr.insa.chatsystem.net.discovery.core.DiscoveryContext;
import fr.insa.chatsystem.net.message.NetMsg;

import java.net.InetAddress;
import java.util.UUID;

/**
 * Base class for all discovery messages exchanged by the {@code NetworkDiscoverer}.
 * <p>
 * Each concrete {@code NetworkMsg}:
 * <ul>
 *     <li>encapsulates the payload of a discovery message,</li>
 *     <li>serializes its content into the underlying JSON structure,</li>
 *     <li>and defines the command to execute upon reception via {@link #handle(DiscoveryContext)}.</li>
 * </ul>
 */
public abstract sealed class NetworkMsg extends NetMsg<NetworkMsgType>
        permits AcceptanceMsg, ConnectionInitMsg, DisconnectionMsg,
        InvalidPseudoMsg, PseudoRequestMsg, RejectionMsg, ValidPseudoMsg {

    /**
     * Creates a new discovery message of the given type.
     *
     * @param owner         id of the user who owns (emits) this message
     * @param type          logical type of the discovery message
     * @param targetAddress IP address of the target host
     * @param targetPort    UDP port on which the message will be sent
     * @param ownerAddress  IP address to reach the message owner
     * @param ownerPort     UDP port to reach the message owner
     */
    protected NetworkMsg(
            UUID owner,
            NetworkMsgType type,
            InetAddress targetAddress,
            int targetPort,
            InetAddress ownerAddress,
            int ownerPort
    ) {
        super(owner, type, targetAddress, targetPort, ownerAddress, ownerPort);
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
    public abstract void handle(DiscoveryContext cxt);
}
