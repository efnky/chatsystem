package fr.insa.chatsystem.net.transport.tcp.events;

import fr.insa.chatsystem.net.transport.tcp.models.ConnectionInfo;
import fr.insa.chatsystem.net.transport.tcp.models.DisconnectReason;
import fr.insa.chatsystem.net.transport.tcp.models.TCPFrame;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class TCPEventManager {

    private final CopyOnWriteArrayList<TCPListener> listeners = new CopyOnWriteArrayList<>();

    public void subscribe(TCPListener l) {
        listeners.add(Objects.requireNonNull(l, "listener"));
    }

    public void unsubscribe(TCPListener l) {
        listeners.remove(Objects.requireNonNull(l, "listener"));
    }

    public void publishConnected(ConnectionInfo c) {
        for (TCPListener l : listeners) l.onClientConnected(c);
    }

    public void publishDisconnected(ConnectionInfo c, DisconnectReason reason) {
        for (TCPListener l : listeners) l.onClientDisconnected(c, reason);
    }

    public void publishMessage(ConnectionInfo c, TCPFrame frame) {
        for (TCPListener l : listeners) l.onMessage(c, frame);
    }

    public void publishError(Throwable t, ConnectionInfo c) {
        for (TCPListener l : listeners) l.onError(t, c);
    }
}