package fr.insa.chatsystem.net.transport.tcp.services;

import fr.insa.chatsystem.net.transport.tcp.events.TCPEventManager;
import fr.insa.chatsystem.net.transport.tcp.models.TCPFrame;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.*;

public final class IOContextTCP {

    private final TCPEventManager eventManager;
    private final TCPCodec<TCPFrame> codec;
    private final ExecutorService executor;
    private final ConcurrentMap<UUID, TCPConnection> connections = new ConcurrentHashMap<>();

    private volatile ServerSocket serverSocket;

    public IOContextTCP(TCPEventManager eventManager, TCPCodec<TCPFrame> codec, ExecutorService executor) {
        this.eventManager = Objects.requireNonNull(eventManager, "eventManager");
        this.codec = Objects.requireNonNull(codec, "codec");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    public TCPEventManager getEventManager() { return eventManager; }
    public TCPCodec<TCPFrame> getCodec() { return codec; }
    public ExecutorService getExecutor() { return executor; }

    public void setServerSocket(ServerSocket ss) { this.serverSocket = ss; }
    public ServerSocket getServerSocket() { return serverSocket; }

    public Map<UUID, TCPConnection> getConnections() { return connections; }

    public void register(TCPConnection c) { connections.put(c.id(), c); }
    public void unregister(UUID id) { connections.remove(id); }

    public void closeServerSocketQuietly() {
        ServerSocket ss = serverSocket;
        if (ss != null && !ss.isClosed()) {
            try { ss.close(); } catch (IOException ignored) {}
        }
    }
}
