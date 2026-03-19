package fr.insa.chatsystem.net.transport.tcp.services;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ServerTCP implements Runnable {

    private final IOContextTCP ctx;
    private final int port;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public ServerTCP(IOContextTCP ctx, int port) {
        this.ctx = Objects.requireNonNull(ctx, "ctx");
        if (port <= 0 || port > 65535) throw new IllegalArgumentException("Invalid TCP port: " + port);
        this.port = port;
    }

    @Override
    public void run() {
        running.set(true);

        try (ServerSocket ss = new ServerSocket(port)) {
            ctx.setServerSocket(ss);

            while (running.get()) {
                try {
                    Socket client = ss.accept(); // bloque
                    TCPConnection c = new TCPConnection(client, ctx);
                    ctx.register(c);
                    ctx.getExecutor().submit(c);
                } catch (SocketException se) {
                    // typiquement déclenché par ss.close() lors du stop()
                    if (!running.get()) break;
                    throw se;
                }
            }
        } catch (Exception e) {
            ctx.getEventManager().publishError(e, null);
        } finally {
            running.set(false);
            ctx.closeServerSocketQuietly();
        }
    }

    public void stop() {
        running.set(false);
        ctx.closeServerSocketQuietly(); // débloque accept()
    }

    public boolean isRunning() { return running.get(); }
}
