package fr.insa.chatsystem.net.transport.tcp.services;

import fr.insa.chatsystem.net.transport.tcp.models.TCPFrame;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Codec minimal: payload = UTF-8 "type\n" + bytes.
 * C’est volontairement simple. Tu pourras remplacer par JSON plus tard.
 */
public final class BasicFrameCodec implements TCPCodec<TCPFrame> {

    @Override
    public byte[] encode(TCPFrame msg) {
        Objects.requireNonNull(msg, "msg");
        byte[] header = (msg.type() + "\n").getBytes(StandardCharsets.UTF_8);
        byte[] body = msg.payload();

        ByteBuffer bb = ByteBuffer.allocate(header.length + body.length);
        bb.put(header);
        bb.put(body);
        return bb.array();
    }

    @Override
    public TCPFrame decode(byte[] payload) {
        Objects.requireNonNull(payload, "payload");
        int i = 0;
        while (i < payload.length && payload[i] != (byte) '\n') i++;
        if (i == payload.length) {
            return new TCPFrame("RAW", payload);
        }
        String type = new String(payload, 0, i, StandardCharsets.UTF_8);
        byte[] body = java.util.Arrays.copyOfRange(payload, i + 1, payload.length);
        return new TCPFrame(type, body);
    }
}
