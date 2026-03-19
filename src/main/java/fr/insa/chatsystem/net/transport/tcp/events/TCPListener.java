package fr.insa.chatsystem.net.transport.tcp.events;

import fr.insa.chatsystem.net.transport.tcp.models.ConnectionInfo;
import fr.insa.chatsystem.net.transport.tcp.models.DisconnectReason;
import fr.insa.chatsystem.net.transport.tcp.models.TCPFrame;

public interface TCPListener {
    default void onClientConnected(ConnectionInfo c) {}
    default void onClientDisconnected(ConnectionInfo c, DisconnectReason reason) {}
    void onMessage(ConnectionInfo c, TCPFrame frame);
    default void onError(Throwable error, ConnectionInfo c) {}
}
