package fr.insa.chatsystem.net.discovery.events;

import fr.insa.chatsystem.net.discovery.core.DiscoveryContext;
import fr.insa.chatsystem.net.discovery.core.DiscoveryState;
import fr.insa.chatsystem.net.message.NetMsg;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

/**
 * Represents the message for rejecting a connection from a new user.
 */
public final class RejectionMsg extends NetworkMsg {

    /** pseudo is the requested pseudo of the user who initialised the rejected connection. */
    protected String pseudo;
    /** reason describes the reasons of the rejection. */
    protected String reason;

    /**
     * Instantiates the RejectionMsg
     *
     * @param owner is the id of the user who has rejected the connection.
     * @param pseudo is the pseudo of the rejected user.
     * @param reason is the reason why the user has been rejected.
     * @param targetAddress is the InetAddress of the target.
     * @param targetPort is the port on which the message will be sent.
     * @param ownerAddress is the InetAddress to join the owner of the message.
     * @param ownerPort is the port to join the owner of the message.
     */
    public RejectionMsg(UUID owner, String pseudo, String reason, InetAddress targetAddress, int targetPort, InetAddress ownerAddress, int ownerPort){
        super(owner, NetworkMsgType.REJECT_CONNECTION, targetAddress, targetPort, ownerAddress, ownerPort);
        this.pseudo = pseudo;
        this.reason = reason;
        updateJSON();
    }

    /**
     * Returns the pseudo of the rejected user.
     * @return the pseudo of the user who has been rejected.
     */
    public String getPseudo(){
        return pseudo;
    }

    /**
     * Return the reason why the user has been rejected.
     *
     * @return the description of the rejection.
     */
    public String getReason(){
        return reason;
    }

    /**
     * Writes message-specific fields into the base JSON payload.
     * Called by constructors to ensure the message is ready to be sent/serialized.
     * It is called by the constructor of the super class {@link NetMsg <T>}
     */
    @Override
    protected void updateJSON() {
        json.put("pseudo", pseudo);
        json.put("reason", reason);
    }

    @Override
    public void handle(DiscoveryContext cxt) {
        if(cxt.getDiscoveryState() == DiscoveryState.WAITING_CONNECTION_RESPONSE){
            System.out.println("[---PEERS--] Connection rejected by "+ getOwnerAddress());
            cxt.setDiscoveryState(DiscoveryState.REJECTION_RECEIVED);
            cxt.getEventManager().notify(this);
        }
    }

    /*--------------------------------------JSON-Factory-----------------------------------------*/


    public static RejectionMsg fromJSON(JSONObject json){
        try {
            if(!json.getString("type").equals(NetworkMsgType.REJECT_CONNECTION.getLabel())){
                // the json do not correspond to a ConnectionMsg
                throw new IllegalArgumentException("Invalid JSON : the json type do not match with RejectionMsg");
            }
            UUID owner = UUID.fromString(json.getString("owner"));
            String pseudo = json.getString("pseudo");
            String reason = json.getString("reason");
            String targetAddress = json.getString("targetAddress");
            int targetPort = json.getInt("targetPort");
            String ownerAddress = json.getString("ownerAddress");
            int ownerPort = json.getInt("ownerPort");
            try{
                return new RejectionMsg(owner, pseudo, reason, InetAddress.getByName(targetAddress), targetPort, InetAddress.getByName(ownerAddress), ownerPort);
            }catch (UnknownHostException e){
                throw new IllegalArgumentException("Invalid JSON : An address in the JSON is not recognize : "+ e.getMessage());
            }
        }catch (JSONException e){
            throw new IllegalArgumentException("Invalid JSON attributes: "+e.getMessage());
        }
    }
}
