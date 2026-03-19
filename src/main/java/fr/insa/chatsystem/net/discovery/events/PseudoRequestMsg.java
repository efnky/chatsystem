package fr.insa.chatsystem.net.discovery.events;

import fr.insa.chatsystem.net.discovery.core.DiscoveryContext;
import fr.insa.chatsystem.net.discovery.core.DiscoveryState;
import fr.insa.chatsystem.net.discovery.ContactList;
import fr.insa.chatsystem.net.discovery.User;
import fr.insa.chatsystem.net.message.NetMsg;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

/**
 * Represent the NetworkMsg for requesting a pseudo change.
 */
public final class PseudoRequestMsg extends NetworkMsg {

    /** pseudo is the request pseudo. */
    protected String requestedPseudo;

    /**
     * Instantiates the PseudoRequestMsg.
     * @param owner is the id of the user who has originally created the PseudoRequesting.
     * @param requestedPseudo is the pseudo requested by the owner.
     * @param targetAddress is the InetAddress of the target.
     * @param targetPort is the port on which the message will be sent.
     * @param ownerAddress is the InetAddress to join the owner of the message.
     * @param ownerPort is the port to join the owner of the message.
     */
    public PseudoRequestMsg(UUID owner, String requestedPseudo, InetAddress targetAddress, int targetPort, InetAddress ownerAddress, int ownerPort){
        super(owner, NetworkMsgType.REQUEST_PSEUDO, targetAddress, targetPort, ownerAddress, ownerPort);
        this.requestedPseudo =  requestedPseudo;
        updateJSON();
    }

    /**
     * Writes message-specific fields into the base JSON payload.
     * Called by constructors to ensure the message is ready to be sent/serialized.
     * It is called by the constructor of the super class {@link NetMsg <T>}
     */
    @Override
    protected void updateJSON() {
        json.put("requestedPseudo", requestedPseudo);
    }

    /**
     * Returns the pseudo of the owner is requesting.
     *
     * @return the pseudo of the owner who is asking for.
     */
    public String getRequestedPseudo(){
        return requestedPseudo;
    }

    @Override
    public void handle(DiscoveryContext cxt) {
        if(cxt.getDiscoveryState() == DiscoveryState.CONNECTED){
            ContactList contactList = ContactList.getInstance();
            System.out.println("[---HOST---] Pseudo change request from "+ getOwnerAddress());

            User user =  contactList.getUserFromUUID(getOwner());
            if(user == null){
                return;
            }

            if(!contactList.containsPseudo(requestedPseudo)){
                /*Accepts requested pseudo*/
                ValidPseudoMsg msg = new ValidPseudoMsg(
                        cxt.getHostUser().getID(),
                        requestedPseudo,
                        ownerAddress,
                        ownerPort,
                        cxt.getHostUser().getAddress(),
                        cxt.getHostUser().getPort()
                );
                contactList.changePseudo(getOwner(), requestedPseudo);
                cxt.getTransportService().send(msg);
                System.out.println("[----------] Accepts new Pseudo from %s "+ownerAddress.getHostAddress()+":"+ getOwnerPort());
                cxt.getEventManager().notifyPseudoValidated(this);
            }else{
                /*Rejects requested pseudo*/
                InvalidPseudoMsg msg = new InvalidPseudoMsg(
                        cxt.getHostUser().getID(),
                        requestedPseudo, ownerAddress,
                        ownerPort,
                        cxt.getHostUser().getAddress(),
                        cxt.getHostUser().getPort()
                );
                cxt.getTransportService().send(msg);
                System.out.println("[----------] Rejects new Pseudo from %s "+ownerAddress.getHostAddress()+":"+ getOwnerPort());
                cxt.getEventManager().notifyPseudoDenied(this);
            }
            // check is the user who is requesting, is in the network
            cxt.getEventManager().notify(this);
        }
    }

    /*--------------------------------------JSON-Factory-----------------------------------------*/

    public static PseudoRequestMsg fromJSON(JSONObject json){
        try {
            if(!json.getString("type").equals(NetworkMsgType.REQUEST_PSEUDO.getLabel())){
                // the json do not correspond to a ConnectionMsg
                throw new IllegalArgumentException("Invalid JSON : the json type do not match with PseudoRequestMsg");
            }
            UUID owner = UUID.fromString(json.getString("owner"));
            String requestedPseudo = json.getString("requestedPseudo");
            InetAddress targetAddress = null; // This is the convention for broadcasting msg.
            int targetPort = json.getInt("targetPort");
            String ownerAddress = json.getString("ownerAddress");
            int ownerPort = json.getInt("ownerPort");
            try{
                return new PseudoRequestMsg(owner, requestedPseudo, targetAddress, targetPort, InetAddress.getByName(ownerAddress), ownerPort);
            }catch (UnknownHostException e){
                throw new IllegalArgumentException("Invalid JSON : An address in the JSON is not recognize : "+ e.getMessage());
            }
        }catch (JSONException e){
            throw new IllegalArgumentException("Invalid JSON attributes: "+e.getMessage());
        }
    }
}
