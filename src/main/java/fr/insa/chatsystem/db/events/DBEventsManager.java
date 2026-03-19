package fr.insa.chatsystem.db.events;

import fr.insa.chatsystem.db.api.DBListener;
import fr.insa.chatsystem.db.models.Contact;
import fr.insa.chatsystem.db.models.Message;
import fr.insa.chatsystem.db.models.Reaction;

import java.util.ArrayList;

public class DBEventsManager{

    /**
     * List of all subscribed listeners.
     * Each received event is broadcast to every listener in this list.
     */
    ArrayList<DBListener> listeners = new ArrayList<>();

    /**
     * Subscribes a listener if it is not already present.
     *
     * @param listener observer to register (must not be null ideally)
     */
    public void subscribe(DBListener listener) {
        // Prevent duplicates: the same listener should not receive events twice.
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Unsubscribes a listener.
     *
     * @param listener observer to remove
     * @return true if the listener was registered and has been removed, false otherwise
     */
    public boolean unsubscribe(DBListener listener) {
        return listeners.remove(listener);
    }

    public void notifyOnNewContact(Contact contact) {
        // Broadcast to all observers (simple model).
        for (DBListener l : listeners) {
            l.onNewContact(contact);
        }
    }

    public void notifyOnMessageArchived(Message message) {
        for (DBListener l : listeners) {
            l.onMessageArchived(message);
        }
    }

    public void notifyOnReactionArchived(Reaction reaction) {
        for (DBListener l : listeners) {
            l.onReactionArchived(reaction);
        }
    }

    public void notifyOnPseudoChangeArchived(Contact contact) {
        for (DBListener l : listeners) {
            l.onPseudoChangedArchived(contact);
        }
    }
}
