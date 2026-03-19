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
 * Represents the NetworkMsg for Initializing a Connection. It is sent by someone who wants to join
 * the network and it is received by a peer listeners.
 */
public final class ConnectionInitMsg extends NetworkMsg {

    /** the pseudo the User want to use in the Network */
    private final String requestedPseudo;

    /**
     * Instantiates the ConnectionMsg.
     *
     * @param owner is the id of the user who originally created the ConnectionMsg.
     * @param requestedPseudo is the pseudo the user who owns the message wants to use.
     * @param targetAddress is the InetAddress of the target.
     * @param targetPort is the port on which the message will be sent.
     * @param ownerAddress is the InetAddress to join the owner of the message.
     * @param ownerPort is the port to join the owner of the message.
     */
    public ConnectionInitMsg(UUID owner, String requestedPseudo, InetAddress targetAddress, int targetPort, InetAddress ownerAddress, int ownerPort){
        super(owner, NetworkMsgType.INIT_CONNECTION, targetAddress, targetPort, ownerAddress, ownerPort);
        this.requestedPseudo = requestedPseudo;
        updateJSON();
    }

    /**
     * Returns the pseudo of the user.
     *
     * @return the pseudo of the user who ask for connection.
     */
    public String getRequestedPseudo(){
        return requestedPseudo;
    }


    @Override
    protected void updateJSON() {
        json.put("requestedPseudo", requestedPseudo);
    }

    /**
     *
     *
     * @param cxt is the context of the NetworkDiscovery package. It provides the elements the message command
     *            requires.
     */
    @Override
    public void handle(DiscoveryContext cxt) {
        if(cxt.getDiscoveryState() == DiscoveryState.CONNECTED){
            ContactList contactList = cxt.getContactList();
            System.out.println("[---HOST---] Connection initialize");
            UUID id = getOwner();
            String pseudo = getRequestedPseudo();

            boolean isPseudoAvailable = contactList.addUser(id, pseudo, getOwnerAddress(),  getOwnerPort());
            if(isPseudoAvailable){
                /* Accepts Connection */
                AcceptanceMsg msg = new AcceptanceMsg(
                        cxt.getHostUser().getID(),
                        this.getOwnerAddress(),
                        this.getOwnerPort(),
                        cxt.getHostUser().getAddress(),
                        cxt.getHostUser().getPort()
                );
                cxt.getTransportService().send(msg);
                System.out.println("[----------] Accepts connection");
                cxt.getEventManager().notifyNewUserAccepted(this);
            }else{
                /* Reject Connection */
                RejectionMsg msg = new RejectionMsg(
                        cxt.getHostUser().getID(),
                        getRequestedPseudo(),
                        "Pseudo Already Used",
                        getOwnerAddress(),
                        getOwnerPort(),
                        cxt.getHostUser().getAddress(),
                        cxt.getHostUser().getPort()
                );
                cxt.getTransportService().send(msg);
                System.out.println("[----------] Rejects connection");
                cxt.getEventManager().notifyNewUserDenied(this);
            }
            cxt.getEventManager().notify(this);
        }
    }

    /*--------------------------------------JSON-Factory-----------------------------------------*/


    public static ConnectionInitMsg fromJSON(JSONObject json){
        try {
            if(!json.getString("type").equals(NetworkMsgType.INIT_CONNECTION.getLabel())){
                // the json do not correspond to a ConnectionMsg
                throw new IllegalArgumentException("Invalid JSON : the json type do not match with ConnectionMsg");
            }
            UUID owner = UUID.fromString(json.getString("owner"));
            String requestedPseudo = json.getString("requestedPseudo");
            InetAddress targetAddress = null; // this is convention for broadcast msg;
            int targetPort = json.getInt("targetPort");
            String ownerAddress = json.getString("ownerAddress");
            int ownerPort = json.getInt("ownerPort");
            try{
                return new ConnectionInitMsg(owner, requestedPseudo, targetAddress, targetPort, InetAddress.getByName(ownerAddress), ownerPort);
            }catch (UnknownHostException e){
                throw new IllegalArgumentException("Invalid JSON : An address in the JSON is not recognize : "+ e.getMessage());
            }
        }catch (JSONException e){
            throw new IllegalArgumentException("Invalid JSON attributes: "+e.getMessage());
        }
    }
}
