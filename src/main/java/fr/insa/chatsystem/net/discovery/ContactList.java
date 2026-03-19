package fr.insa.chatsystem.net.discovery;

import fr.insa.chatsystem.net.JSONSerializable;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


/**
 * This singleton stores All the Users in Network and offer an interface to modify the user Connected to the Network.
 * It also guarantees the uniqueness of a pseudonym.
 */
public class ContactList implements JSONSerializable {

    /** Provides interface for Observer design pattern. */
    public interface Listener{
        void onUserAdded(User user);
        void onUserRemoved(User user);
        void onPseudoChange(User user, String oldPseudo, String newPseudo);
    }

    /** listeners is the notify list. It stores every instance which has subscribed for listening the ContactList. */
    private ArrayList<Listener> listeners = new ArrayList<Listener>();

    /** instance is the only instance of ContactList as ContactList is a singleton */
    private static ContactList instance;
    private User hostUser;

    /** connectedUserList stores all the User connected to the network. */
    private HashMap<String, User> connectedUsersList;


    /*------------------------------------------------Constructors-------------------------------------------------- **/

    /** Instantiates the only instance of ContactList */
    private ContactList(){
        this.connectedUsersList = new HashMap<>();
    }

    /**
     * Instantiates the only instance of ContactList by copying an existing List of connected users.
     * @param connectedUsersList
     */
    private ContactList(HashMap<String, User> connectedUsersList){
        this.connectedUsersList = connectedUsersList;
    }

    /**
     * Return the only instance of ContactList. It instantiates it if it is has not already been instantiated.
     *
     * @return the only instance of Contact List.
     */
    public static ContactList getInstance(){
        if(instance == null){
            instance = new ContactList();
        }
        return ContactList.instance;
    }

    public void clear(){
        connectedUsersList.clear();
    }

    public User getHostUser(){
        if(hostUser == null){
            throw new RuntimeException("HostUser Is null");
        }
        return hostUser;
    }

    public void setHostUser(User hostUser){
        this.hostUser = hostUser;
    }

    /*---------------------------------------------ListenersManagement------------------------------------------------*/

    /**
     * Subscribes an Object implementing ContactList.Listener to the notify list.
     *
     * @param newListener is the new subscriber.
     */
    public void subscribe(Listener newListener){
        if(!listeners.contains(newListener)){
            listeners.add(newListener);
        }
    }

    /**
     * Unsubscribes an Object implementing ContactList.Listener from the notify list.
     *
     * @param listener is the subscriber to remove form the notify list.
     * @return true if listener had been in the list and was removed else false.
     */
    public boolean unsubscribe(Listener listener){
        return listeners.remove(listener);
    }

    /**
     * Calls the onUserAdded() method for every subscriber.
     */
    protected void notifyUserAdded(User user){
        for(Listener l : listeners){
            l.onUserAdded(user);
        }
    }

    /**
     * Calls the onUserRemoved() method for every subscriber.
     */
    protected void notifyUserRemoved(User user){
        for(Listener l : listeners){
            l.onUserRemoved(user);
        }
    }

    /**
     * Calls the onPseudoChange() method for every subscriber.
     */
    private void notifyPseudoChange(User  user, String oldPseudo, String newPseudo){
        for(Listener l : listeners){
            l.onPseudoChange(user, oldPseudo,  newPseudo);
        }
    }

    /*----------------------------------------------------Methods-----------------------------------------------------*/

    /**
     * Returns the size of the ContactList i.e. the number of users connected to the network.
     *
     * @return the size of the ContactList.
     */
    public int size(){
        return this.connectedUsersList.size();
    }

    /**
     * Returns if there is no users connected to the network.
     *
     * @return true is the ContactList is empty else false.
     */
    public boolean isEmpty(){
        return this.connectedUsersList.isEmpty();
    }

    /**
     * Return if
     * @param pseudo which is tested for being already used in the network.
     * @return true if pseudo is already used else false.
     */
    public boolean containsPseudo(Object pseudo){
        return this.connectedUsersList.containsKey(pseudo);
    }

    /**
     * Tries to addUsers depending on if the user pseudo is already used by another user in the ContactList.
     * It returns if it achieves to add the user.
     *
     * @param id is the id of the user it is trying to add.
     * @param pseudo is the pseudo of the user it is trying to add.
     * @param address is the address of the user it is trying to add.
     * @param port is the port of the user it is trying to add.
     * @return true if the user was successfully added else false.
     */
    public boolean addUser(UUID id, String pseudo, InetAddress address,int port){
        if(this.connectedUsersList.containsKey(pseudo)) {
            return false;
        }
        User user = new User(id, pseudo, User.Type.PEER, address, port);
        this.connectedUsersList.put(pseudo, user);
        notifyUserAdded(user);
        return true;
    }

    public List<User> getConnectedUsers(){
        return  this.connectedUsersList.values().stream().collect(Collectors.toList());
    }

    /**
     * Changes the pseudo of the User with the ID=id by new_pseudo if it has not already used.
     * It notifies the listeners only if the pseudo has changed, not when the new pseudo
     * is also the old pseudo.
     *
     * @param id is the unique integer that represent a User.
     * @param new_pseudo is the requested pseudo from the user.
     * @return true if the pseudo could be changes or new_pseudo is also the old pseudo else false.
     */
    public boolean changePseudo(UUID id, String new_pseudo){
        User user = this.getUserFromUUID(id);
        // The user does not exist
        if(user == null){
            return false;
        }
        String formerPseudo = user.getPseudo();
        // The user request the same pseudo he already has.
        if(formerPseudo.equals(new_pseudo)){
            return true;
        }

        // Pseudo change
        this.connectedUsersList.remove(formerPseudo);

        user.setPseudo(new_pseudo);
        this.connectedUsersList.put(new_pseudo, user);
        notifyPseudoChange(user, formerPseudo, new_pseudo);
        return true;
    }

    /**
     * Remove the user with the pseudo passed in arguments from the ContactList. It returns if the concerned user
     * was registered in the list or not.
     *
     * @param pseudo is the pseudo of the user to be removed.
     * @return true if the pseudo was stored in the list else false.
     */
    public boolean removeUser(String pseudo){
        User value = this.connectedUsersList.remove(pseudo);
        if(value == null){
            // the pseudo was not stored in the list.
            return false;
        }
        notifyUserRemoved(value);
        return true;
    }

    /**
     * Returns the user registered in the contact list with the corresponding ID.
     *
     * @param nId is the id
     * @return the User with the matched id else null
     */
    public User getUserFromUUID(final UUID nId){
        for(String pseudo : this.connectedUsersList.keySet()) {
            User user = this.connectedUsersList.get(pseudo);
            if (user.getID().equals(nId))
                return user;
        }
        return null;
    }

    /**
     * Print the ContactList.
     */
    public void print(){
        System.out.println(this);
    }

    /*----------------------------------------------JSONSerializable--------------------------------------------------*/

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        for(String key : this.connectedUsersList.keySet()){
            json.put(key, this.connectedUsersList.get(key).toJSON());
        }
        return json;
    }

    public void copyFromJSON(JSONObject json) throws JSONException {
        this.connectedUsersList = new HashMap<>();

        for(String key : json.keySet()){
            User newuser = User.peerFromJSON(json.getJSONObject(key));
            this.connectedUsersList.put(key, newuser);
            notifyUserAdded(newuser);
        }
    }

    /*------------------------------------------------Overrides-------------------------------------------------------*/

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        int count = 1;
        for(String key : this.connectedUsersList.keySet()) {
            sb.append(key + ": " + this.connectedUsersList.get(key).toString());
            if(count < this.connectedUsersList.keySet().size()){
                sb.append("\n");
            }
            count++;
        }
        return sb.toString();
    }
}
