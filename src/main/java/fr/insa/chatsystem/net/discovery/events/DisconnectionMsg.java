package fr.insa.chatsystem.net.discovery.events;

import fr.insa.chatsystem.net.discovery.core.DiscoveryContext;
import fr.insa.chatsystem.net.discovery.core.DiscoveryState;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

/**
 * Represents the NetworkMsg for warning the network, the disconnection of the user.
 */
public final class DisconnectionMsg extends NetworkMsg {

    /**
     * Instantiates the DisconnectionMsg
     *
     * @param owner is the id of the user who is warning for disconnection.
     * @param targetAddress is the InetAddress of the target.
     * @param targetPort is the port on which the message will be sent.
     * @param ownerAddress is the InetAddress to join the owner of the message.
     * @param ownerPort is the port to join the owner of the message.
     */
    public DisconnectionMsg(UUID owner, InetAddress targetAddress, int targetPort, InetAddress ownerAddress, int ownerPort){
        super(owner, NetworkMsgType.INIT_DISCONNECTION, targetAddress, targetPort, ownerAddress, ownerPort);
        updateJSON();
    }

    @Override
    protected void updateJSON() {
        //nothing to update
    }

    /**
     * If the client is connected then it removes from the contact the user which has sent this message.
     *
     * @param cxt is the context of the NetworkDiscovery package. It provides the elements the message command
     *            requires.
     */
    @Override
    public void handle(DiscoveryContext cxt) {
        if(cxt.getDiscoveryState() == DiscoveryState.CONNECTED){
            System.out.println("[---HOST---] Disconnection from "+getOwnerAddress().getHostAddress());
            String pseudo = cxt.getContactList().getUserFromUUID(getOwner()).getPseudo();
            if(pseudo == null){
                System.out.println("disconnection init by someone who was not in the contact list");
                return;
            }
            cxt.getContactList().removeUser(pseudo);
            cxt.getEventManager().notify(this);
        }
    }

    /*--------------------------------------JSON-Factory-----------------------------------------*/


    public static DisconnectionMsg fromJSON(JSONObject json){
        try {
            if(!json.getString("type").equals(NetworkMsgType.INIT_DISCONNECTION.getLabel())){
                // the json do not correspond to a ConnectionMsg
                throw new IllegalArgumentException("Invalid JSON : the json type do not match with DisconnectionMsg");
            }
            UUID owner = UUID.fromString(json.getString("owner"));
            InetAddress targetAddress = null; // this is the convention for broadcast message
            int targetPort = json.getInt("targetPort");
            String ownerAddress = json.getString("ownerAddress");
            int ownerPort = json.getInt("ownerPort");
            try{
                return new DisconnectionMsg(owner, targetAddress, targetPort, InetAddress.getByName(ownerAddress), ownerPort);
            }catch (UnknownHostException e){
                throw new IllegalArgumentException("Invalid JSON : An address in the JSON is not recognize : "+ e.getMessage());
            }
        }catch (JSONException e){
            throw new IllegalArgumentException("Invalid JSON attributes: "+e.getMessage());
        }
    }
}
