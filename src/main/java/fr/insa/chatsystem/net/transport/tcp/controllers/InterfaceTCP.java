package fr.insa.chatsystem.net.transport.tcp.controllers;

import fr.insa.chatsystem.net.transport.tcp.events.TCPEventManager;
import fr.insa.chatsystem.net.transport.tcp.events.TCPListener;
import fr.insa.chatsystem.net.transport.tcp.models.TCPFrame;
import fr.insa.chatsystem.net.transport.tcp.services.BasicFrameCodec;
import fr.insa.chatsystem.net.transport.tcp.services.IOContextTCP;
import fr.insa.chatsystem.net.transport.tcp.services.ServerTCP;
import fr.insa.chatsystem.net.transport.tcp.services.TCPConnection;

import java.io.IOException;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class InterfaceTCP implements TCPListener{

    private final IOContextTCP ctx;
    private final TCPEventManager eventManager;
    private final AtomicInteger workerId = new AtomicInteger(1);

    private ServerTCP server;
    private Thread serverThread;

    public InterfaceTCP() {
        this.eventManager = new TCPEventManager();
        ExecutorService executor = Executors.newCachedThreadPool(r -> new Thread(r, "tcp-worker-" + workerId.getAndIncrement()));
        this.ctx = new IOContextTCP(eventManager, new BasicFrameCodec(), executor);
        this.ctx.getEventManager().subscribe(this); // keep
    }

    // Observer
    public void subscribe(TCPListener l) { eventManager.subscribe(l); }
    public void unsubscribe(TCPListener l) { eventManager.unsubscribe(l); }

    // Server side
    public void startServer(int port) {
        if (serverThread != null && serverThread.isAlive()) return;
        server = new ServerTCP(ctx, port);
        serverThread = new Thread(server, "tcp-acceptor-" + port);
        serverThread.start();
    }

    // Client side (optionnel mais pratique en P2P)
    public UUID connect(String host, int port) throws IOException {
        Socket s = new Socket(host, port);
        TCPConnection c = new TCPConnection(s, ctx);
        ctx.register(c);
        ctx.getExecutor().submit(c);
        return c.id();
    }

    public void send(UUID connectionId, TCPFrame frame) throws IOException {
        Objects.requireNonNull(connectionId, "connectionId");
        Objects.requireNonNull(frame, "frame");
        TCPConnection c = ctx.getConnections().get(connectionId);
        if (c == null) throw new IllegalStateException("Unknown connection: " + connectionId);
        c.send(frame);
    }

    public void broadcast(TCPFrame frame) {
        Objects.requireNonNull(frame, "frame");
        for (TCPConnection c : ctx.getConnections().values()) {
            try { c.send(frame); } catch (IOException e) {
                eventManager.publishError(e, c.info());
            }
        }
    }

    public void disconnect(UUID connectionId) {
        TCPConnection c = ctx.getConnections().get(connectionId);
        if (c != null) c.stop();
    }

    public void stop() {
        // stop server accept loop
        if (server != null) server.stop();

        // stop all connections
        for (TCPConnection c : List.copyOf(ctx.getConnections().values())) c.stop();

        // stop executor
        ctx.getExecutor().shutdownNow();
        try {
            ctx.getExecutor().awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

        // join server thread quickly
        if (serverThread != null) {
            try { serverThread.join(1000); } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
