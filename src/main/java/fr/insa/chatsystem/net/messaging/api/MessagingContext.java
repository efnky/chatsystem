package fr.insa.chatsystem.net.messaging.api;

import fr.insa.chatsystem.net.discovery.ContactList;
import fr.insa.chatsystem.net.messaging.events.MessagingEventManager;
import fr.insa.chatsystem.net.transport.tcp.controllers.InterfaceTCP;

/**
 * Runtime context for the Messaging layer.
 *
 * <p>This object groups the dependencies needed by messaging components:
 * transport/session manager, serializers, storage, UI dispatcher, etc.</p>
 *
 * <p>Keep it focused: the context should expose only what message handling and sending needs
 * to avoid becoming a "god object". Prefer small dedicated services.</p>
 */
public class MessagingContext {

    private final MessagingEventManager eventManager;
    private final InterfaceTCP transportService;
    private final ContactList contactList;

    /**
     * Creates a messaging context.
     *
     * <p>In practice, you will inject dependencies through the constructor.</p>
     */
    public MessagingContext(MessagingEventManager eventManager, InterfaceTCP transportService, ContactList contactList) {
        this.eventManager = eventManager;
        this.transportService = transportService;
        this.contactList = contactList;
    }

    public MessagingEventManager getEventManager() {
        return eventManager;
    }

    public InterfaceTCP getTransportService() {
        return transportService;
    }

    public ContactList getContactList() {
        return contactList;
    }
}
