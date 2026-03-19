package fr.insa.chatsystem.net.transport.tcp.services;

import fr.insa.chatsystem.net.transport.tcp.models.ConnectionInfo;
import fr.insa.chatsystem.net.transport.tcp.models.DisconnectReason;
import fr.insa.chatsystem.net.transport.tcp.models.TCPFrame;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class TCPConnection implements Runnable {

    private final UUID id = UUID.randomUUID();
    private final Socket socket;
    private final IOContextTCP ctx;

    private final ReceiverTCP<TCPFrame> receiver;
    private final SenderTCP<TCPFrame> sender;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public TCPConnection(Socket socket, IOContextTCP ctx) throws IOException {
        this.socket = Objects.requireNonNull(socket, "socket");
        this.ctx = Objects.requireNonNull(ctx, "ctx");
        this.receiver = new ReceiverTCP<>(new DataInputStream(socket.getInputStream()), ctx.getCodec());
        this.sender   = new SenderTCP<>(new DataOutputStream(socket.getOutputStream()), ctx.getCodec());
    }

    public UUID id() { return id; }

    public ConnectionInfo info() {
        return new ConnectionInfo(id, socket.getInetAddress(), socket.getPort());
    }

    public void send(TCPFrame frame) throws IOException {
        sender.send(frame);
    }

    @Override
    public void run() {
        running.set(true);
        ConnectionInfo info = info();
        ctx.getEventManager().publishConnected(info);

        try {
            while (running.get()) {
                TCPFrame frame = receiver.receive();
                if (frame == null) { // EOF : remote closed
                    ctx.getEventManager().publishDisconnected(info, DisconnectReason.REMOTE_CLOSED);
                    break;
                }
                ctx.getEventManager().publishMessage(info, frame);
            }
        } catch (IOException e) {
            if (running.get()) ctx.getEventManager().publishError(e, info);
            ctx.getEventManager().publishDisconnected(info, DisconnectReason.IO_ERROR);
        } finally {
            running.set(false);
            ctx.unregister(id);
            safeClose();
        }
    }

    public void stop() {
        running.set(false);
        safeClose(); // débloque receive()
        ctx.getEventManager().publishDisconnected(info(), DisconnectReason.STOPPED);
    }

    private void safeClose() {
        try { socket.close(); } catch (IOException ignored) {}
    }
}