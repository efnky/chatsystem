package fr.insa.chatsystem.net.discovery.events;

import fr.insa.chatsystem.net.discovery.core.DiscoveryContext;
import fr.insa.chatsystem.net.discovery.core.DiscoveryState;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

/**
 * Represents the NetworkMsg for the rejecting a pseudo change.
 */
public final class InvalidPseudoMsg extends NetworkMsg {

    /** pseudo is the pseudo which is refused. */
    private String pseudo;

    /**
     * Instantiates the InvalidPseudoMsg.
     *
     * @param owner is the id of the user who has originally created the message.
     * @param pseudo is the pseudo which is accepted.
     * @param targetAddress is the InetAddress of the target.
     * @param targetPort is the port on which the message will be sent.
     * @param ownerAddress is the InetAddress to join the owner of the message.
     * @param ownerPort is the port to join the owner of the message.
     */
    public InvalidPseudoMsg(UUID owner, String pseudo, InetAddress targetAddress, int targetPort, InetAddress ownerAddress, int ownerPort){
        super(owner, NetworkMsgType.INVALID_PSEUDO, targetAddress, targetPort, ownerAddress, ownerPort);
        this.pseudo = pseudo;
        updateJSON();
    }

    /**
     * Returns the rejected pseudo.
     *
     * @return the rejected pseudo.
     */
    public String getPseudo(){
        return pseudo;
    }


    @Override
    protected void updateJSON() {
        json.put("pseudo", pseudo);
    }

    @Override
    public void handle(DiscoveryContext cxt) {
        if(cxt.getDiscoveryState() == DiscoveryState.WAITING_PSEUDO_REQUEST_RESPONSE){
            System.out.println("[---PEERS--] Pseudo change rejected by "+getOwnerAddress());
            cxt.setDiscoveryState(DiscoveryState.INVALID_PSEUDO_RECEIVED);
            cxt.getEventManager().notify(this);
        }
    }

    /*--------------------------------------JSON-Factory-----------------------------------------*/

    public static InvalidPseudoMsg fromJSON(JSONObject json){
        try {
            if(!json.getString("type").equals(NetworkMsgType.INVALID_PSEUDO.getLabel())){
                // the json do not correspond to a ConnectionMsg
                throw new IllegalArgumentException("Invalid JSON : the json type do not match with InvalidPseudoMsg");
            }
            UUID owner = UUID.fromString(json.getString("owner"));
            String pseudo = json.getString("pseudo");
            String targetAddress = json.getString("targetAddress");
            int targetPort = json.getInt("targetPort");
            String ownerAddress = json.getString("ownerAddress");
            int ownerPort = json.getInt("ownerPort");
            try{
                return new InvalidPseudoMsg(owner, pseudo, InetAddress.getByName(targetAddress), targetPort, InetAddress.getByName(ownerAddress), ownerPort);
            }catch (UnknownHostException e){
                throw new IllegalArgumentException("Invalid JSON : An address in the JSON is not recognize : "+ e.getMessage());
            }
        }catch (JSONException e){
            throw new IllegalArgumentException("Invalid JSON attributes: "+e.getMessage());
        }
    }
}

