package fr.insa.chatsystem.net.discovery.events;

import fr.insa.chatsystem.net.discovery.core.DiscoveryContext;
import fr.insa.chatsystem.net.discovery.core.DiscoveryState;
import fr.insa.chatsystem.net.discovery.ContactList;
import fr.insa.chatsystem.net.message.NetMsg;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

/**
 * Represents the NetworkMsg for the validation of a pseudo change request.
 */
public final class ValidPseudoMsg extends NetworkMsg {

    /** pseudo is the pseudo which is accepted*/
    protected final String pseudo;

    /**
     * Instantiates the ValidPseudoMsg. It used for according a valid pseudo change to a user which has initiated
     * a pseudo request change.
     *
     * @param owner is the id of the user who has originally created the message.
     * @param pseudo is the pseudo which is accepted.
     * @param targetAddress is the InetAddress of the target.
     * @param targetPort is the port on which the message will be sent.
     * @param ownerAddress is the InetAddress to join the owner of the message.
     * @param ownerPort is the port to join the owner of the message.
     */
    public ValidPseudoMsg(UUID owner, String pseudo, InetAddress targetAddress, int targetPort, InetAddress ownerAddress, int ownerPort){
       super(owner, NetworkMsgType.VALID_PSEUDO, targetAddress, targetPort, ownerAddress, ownerPort);
       this.pseudo = pseudo;
       updateJSON();
    }

    /**
     * Returns the accepted pseudo.
     *
     * @return the accepted pseudo.
     */
    public String getPseudo(){
        return pseudo;
    }

    /**
     * Writes message-specific fields into the base JSON payload.
     * Called by constructors to ensure the message is ready to be sent/serialized.
     * It is called by the constructor of the super class {@link NetMsg <T>}
     */
    @Override
    protected void updateJSON() {
        json.put("pseudo", pseudo);
    }

    @Override
    public void handle(DiscoveryContext cxt) {
        if(cxt.getDiscoveryState() == DiscoveryState.WAITING_PSEUDO_REQUEST_RESPONSE){
            ContactList contactList = ContactList.getInstance();
            System.out.println("[---PEERS--] Pseudo change accepted by "+getOwnerAddress());
            contactList.changePseudo(cxt.getHostUser().getID(), getPseudo());
            cxt.setDiscoveryState(DiscoveryState.VALID_PSEUDO_RECEIVED);
            cxt.getEventManager().notify(this);
        }
    }

    /*--------------------------------------JSON-Factory-----------------------------------------*/

    public static ValidPseudoMsg fromJSON(JSONObject json){
        try {
            if(!json.getString("type").equals(NetworkMsgType.VALID_PSEUDO.getLabel())){
                // the json do not correspond to a ConnectionMsg
                throw new IllegalArgumentException("Invalid JSON : the json type do not match with ValidPseudoMsg");
            }
            UUID owner = UUID.fromString(json.getString("owner"));
            String pseudo = json.getString("pseudo");
            String targetAddress = json.getString("targetAddress");
            int targetPort = json.getInt("targetPort");
            String ownerAddress = json.getString("ownerAddress");
            int ownerPort = json.getInt("ownerPort");
            try{
                return new ValidPseudoMsg(owner, pseudo, InetAddress.getByName(targetAddress), targetPort, InetAddress.getByName(ownerAddress), ownerPort);
            }catch (UnknownHostException e){
                throw new IllegalArgumentException("Invalid JSON : An address in the JSON is not recognize : "+ e.getMessage());
            }
        }catch (JSONException e){
            throw new IllegalArgumentException("Invalid JSON attributes: "+e.getMessage());
        }
    }
}
