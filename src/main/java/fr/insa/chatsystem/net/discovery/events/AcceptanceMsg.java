package fr.insa.chatsystem.net.discovery.events;

import fr.insa.chatsystem.net.discovery.core.DiscoveryContext;
import fr.insa.chatsystem.net.discovery.core.DiscoveryState;
import fr.insa.chatsystem.net.discovery.ContactList;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

/**
 * Acceptance message sent by a peer to confirm a connection request.
 *
 * <p>This message transports a snapshot of the current {@link ContactList} so the requester
 * can synchronize its local view immediately after the handshake.</p>
 *
 * <p><b>Expected lifecycle:</b> built by the acceptor and handled by the requester when it
 * is waiting for a connection response.</p>
 * <p><b>Handling contract:</b> the requester should only apply this message while it is
 *  * in {@link DiscoveryState#WAITING_CONNECTION_RESPONSE}. Otherwise, it is ignored.</p>
 */
public final class AcceptanceMsg extends NetworkMsg {

    /**
     * Contact list snapshot carried by this message, serialized as JSON.
     *
     * <p>{@link JSONObject} is mutable, so this field must not be exposed directly.
     * {@link #getContactList()} returns a defensive copy.</p>
     */
    private final JSONObject contactListJSON;

    /**
     * Builds an acceptance message with the current global {@link ContactList} snapshot.
     *
     * @param owner         id of the peer emitting the acceptance.
     * @param targetAddress destination IP of the requester (receiver of this message).
     * @param targetPort    destination port of the requester.
     * @param ownerAddress  address to reach the accepting peer afterward.
     * @param ownerPort     port to reach the accepting peer afterward.
     */
    public AcceptanceMsg(UUID owner,
                         InetAddress targetAddress, int targetPort,
                         InetAddress ownerAddress, int ownerPort) {
        super(owner,
                NetworkMsgType.ACCEPT_CONNECTION,
                targetAddress, validatePort(targetPort, "targetPort"),
                ownerAddress, validatePort(ownerPort, "ownerPort"));

        // Snapshot taken at construction time (sender side).
        this.contactListJSON = ContactList.getInstance().toJSON();

        // Populates the underlying JSON payload of the base message.
        updateJSON();
    }

    /**
     * Internal constructor used by the JSON factory.
     * <p>It preserves the exact payload received over the network.</p>
     */
    private AcceptanceMsg(UUID owner,
                          JSONObject contactListJSON,
                          InetAddress targetAddress, int targetPort,
                          InetAddress ownerAddress, int ownerPort) {
        super(owner,
                NetworkMsgType.ACCEPT_CONNECTION,
                targetAddress, validatePort(targetPort, "targetPort"),
                ownerAddress, validatePort(ownerPort, "ownerPort"));

        this.contactListJSON = contactListJSON;
        updateJSON();
    }

    /**
     * Returns the contact list payload carried by this message.
     *
     * <p>A defensive copy is returned to prevent external mutation of the message content.</p>
     *
     * @return a deep copy (string round-trip) of the stored {@link JSONObject}.
     */
    public JSONObject getContactList() {
        return new JSONObject(contactListJSON.toString());
    }

    @Override
    protected void updateJSON() {
        json.put("contactList", contactListJSON);
    }

    /**
     * Applies the command semantics of this message.
     *
     * <p>If the local peer is currently waiting for a connection response, this method:</p>
     * <ul>
     *   <li>logs the acceptance</li>
     *   <li>updates the context's {@link ContactList} using the received snapshot</li>
     *   <li>moves the discovery state to {@link DiscoveryState#ACCEPTANCE_RECEIVED}</li>
     *   <li>notifies the discovery event manager</li>
     * </ul>
     *
     * <p>If the state does not match, the message is ignored (defensive behavior).</p>
     *
     * @param cxt discovery context providing shared services (logger, contact list, event manager, state).
     */
    @Override
    public void handle(DiscoveryContext cxt) {
        // Ignore messages that arrive in an unexpected state.
        if (cxt.getDiscoveryState() != DiscoveryState.WAITING_CONNECTION_RESPONSE) {
            return;
        }

        // Domain code should not write to stdout: use the context logger.
        cxt.getLogger().info("[---PEERS--] Connection accepted by " + getOwnerAddress().getHostAddress());

        // Avoid singletons in handlers: shared state should come from the context.
        cxt.getContactList().copyFromJSON(getContactList());

        // Update state then emit the event.
        cxt.setDiscoveryState(DiscoveryState.ACCEPTANCE_RECEIVED);
        cxt.getEventManager().notify(this);
    }

    /*-------------------------------------- JSON factory --------------------------------------*/


    /**
     * Builds an {@link AcceptanceMsg} from its JSON representation.
     *
     * <p>Expected attributes:
     * type, owner, contactList, targetAddress, targetPort, ownerAddress, ownerPort.</p>
     *
     * @param json JSON payload to parse.
     * @return a fully constructed {@link AcceptanceMsg}.
     * @throws IllegalArgumentException if JSON is malformed, has wrong type, invalid ports, or invalid addresses.
     */
    public static AcceptanceMsg fromJSON(JSONObject json) {
        try {
            // More robust than getString: avoids JSONException if "type" is missing.
            String type = json.optString("type", "");
            if (!NetworkMsgType.ACCEPT_CONNECTION.getLabel().equals(type)) {
                throw new IllegalArgumentException("Invalid JSON: type does not match AcceptanceMsg");
            }

            UUID owner = UUID.fromString(json.getString("owner"));
            JSONObject contactListJSON = json.getJSONObject("contactList");

            String targetAddress = json.getString("targetAddress");
            int targetPort = validatePort(json.getInt("targetPort"), "targetPort");

            String ownerAddress = json.getString("ownerAddress");
            int ownerPort = validatePort(json.getInt("ownerPort"), "ownerPort");

            try {
                return new AcceptanceMsg(
                        owner,
                        contactListJSON,
                        InetAddress.getByName(targetAddress), targetPort,
                        InetAddress.getByName(ownerAddress), ownerPort
                );
            } catch (UnknownHostException e) {
                throw new IllegalArgumentException("Invalid JSON: unrecognized address: " + e.getMessage());
            }
        } catch (JSONException e) {
            throw new IllegalArgumentException("Invalid JSON attributes: " + e.getMessage());
        }
    }

    /**
     * Validates a UDP/TCP port value.
     *
     * @param port      the port to validate.
     * @param fieldName field name used in error messages.
     * @return the same port if valid.
     * @throws IllegalArgumentException if port is outside [0..65535].
     */
    private static int validatePort(int port, String fieldName) {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid " + fieldName + ": " + port + " (expected 0..65535)");
        }
        return port;
    }

}
