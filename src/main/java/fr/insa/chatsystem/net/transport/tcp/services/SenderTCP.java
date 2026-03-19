package fr.insa.chatsystem.net.transport.tcp.services;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;

public final class SenderTCP<T> {

    private final DataOutputStream out;
    private final TCPCodec<T> codec;

    public SenderTCP(DataOutputStream out, TCPCodec<T> codec) {
        this.out = Objects.requireNonNull(out, "out");
        this.codec = Objects.requireNonNull(codec, "codec");
    }

    public void send(T msg) throws IOException {
        byte[] payload = codec.encode(msg);
        synchronized (out) { // garantit que 2 threads ne mélangent pas les frames
            out.writeInt(payload.length);
            out.write(payload);
            out.flush();
        }
    }
}
