package fr.insa.chatsystem.db.api;

import fr.insa.chatsystem.db.ContactRepository;
import fr.insa.chatsystem.db.MessageRepository;
import fr.insa.chatsystem.db.SQLiteDatabase;
import fr.insa.chatsystem.db.controllers.ChatroomController;
import fr.insa.chatsystem.db.events.DBEventsManager;
import fr.insa.chatsystem.db.models.Chatroom;
import fr.insa.chatsystem.db.models.Contact;
import fr.insa.chatsystem.db.models.Direction;
import fr.insa.chatsystem.db.models.Message;
import fr.insa.chatsystem.net.discovery.events.ConnectionInitMsg;
import fr.insa.chatsystem.net.discovery.events.NetworkListener;
import fr.insa.chatsystem.net.discovery.events.PseudoRequestMsg;
import fr.insa.chatsystem.net.messaging.api.Messenger;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class DBFacade implements NetworkListener {

    private final ChatroomController controller;
    private final ContactRepository contactRepository;
    private final MessageRepository messageRepository;
    private final SQLiteDatabase DB;

    private final DBEventsManager eventsManager = new DBEventsManager();

    public DBFacade(Messenger messenger, String session) throws SQLException {
        DB = new SQLiteDatabase(session);
        contactRepository = new ContactRepository(DB);
        messageRepository = new MessageRepository(DB, eventsManager);
        controller = new ChatroomController(messenger, contactRepository, messageRepository, eventsManager);
    }

    /**
     * Takes a list of contacts and for each contact:
     *  - adds the contact to the DB if the id is a new one
     *  - modifies the pseudo if the id is already store in the DB and the pseudo are different
     *
     * @param contacts
     */
    public void updateDB(List<Contact> contacts) {
        for(Contact contact : contacts) {
            contactRepository.upsertContact(contact);
        }
    }

    public Chatroom openChatroom(UUID uuid) {
        return controller.openChat(uuid);
    }

    public void saveSentMessage(UUID msgId, UUID contactId, String message) {
        messageRepository.saveMessage(msgId, contactId, message, Direction.SENT);
    }

    public void sendReceivedMessage(UUID msgId, UUID contactId, String message) {
        messageRepository.saveMessage(msgId, contactId, message, Direction.RECEIVED);
    }

    public List<Contact> getAllContacts(){
        return contactRepository.findAll();
    }

    public Chatroom getChatroom(UUID chatroomID){
        return controller.openChat(chatroomID);
    }

    public Message getLastMessageFrom(UUID contactId){
        return messageRepository.getLastMessageFrom(contactId);
    }

    @Override
    public void onNewUserAccepted(ConnectionInitMsg msg) {
        contactRepository.upsertContact(new Contact(msg.getOwner(), msg.getRequestedPseudo()));
    }

    @Override
    public void onPseudoValidated(PseudoRequestMsg msg) {
        Contact newContact = new Contact(msg.getOwner(), msg.getRequestedPseudo());
        contactRepository.upsertContact(newContact);
        eventsManager.notifyOnPseudoChangeArchived(newContact);
    }

    /*-----------------------------------------------------Events-----------------------------------------------------*/

    public void subscribe(DBListener dbListener) {
        eventsManager.subscribe(dbListener);
    }

    public void unsubscribe(DBListener dbListener) {
        eventsManager.unsubscribe(dbListener);
    }

    public String getPseudoFromUUID(UUID id){
        Contact contact =  contactRepository.findByUserId(id).orElseGet(
                () -> new Contact(
                        UUID.randomUUID(),
                        "ERROR PSEUDO USER FROM UUID: This user has no pseudo stored"
                )
        );
        return contact.getUsername();
    }
}
