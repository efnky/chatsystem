package fr.insa.chatsystem.net.transport.tcp.services;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Objects;

public final class ReceiverTCP<T> {

    private final DataInputStream in;
    private final TCPCodec<T> codec;

    public ReceiverTCP(DataInputStream in, TCPCodec<T> codec) {
        this.in = Objects.requireNonNull(in, "in");
        this.codec = Objects.requireNonNull(codec, "codec");
    }

    /**
     * @return null si la connexion est fermée proprement (EOF)
     */
    public T receive() throws IOException {
        try {
            int len = in.readInt(); // bloque
            if (len < 0 || len > 50_000_000) throw new IOException("Invalid frame length: " + len);
            byte[] payload = in.readNBytes(len);
            if (payload.length != len) throw new EOFException("Truncated frame");
            return codec.decode(payload);
        } catch (EOFException eof) {
            return null;
        }
    }
}
