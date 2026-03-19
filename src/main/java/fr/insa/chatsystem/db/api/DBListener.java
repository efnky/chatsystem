package fr.insa.chatsystem.db.api;

import fr.insa.chatsystem.db.models.Contact;
import fr.insa.chatsystem.db.models.Message;
import fr.insa.chatsystem.db.models.Reaction;

public interface DBListener {

    void onNewContact(Contact contact);
    void onMessageArchived(Message message);
    void onReactionArchived(Reaction reaction);
    void onPseudoChangedArchived(Contact contact);
}
