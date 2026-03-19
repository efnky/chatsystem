package fr.insa.chatsystem.net.transport.tcp.models;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class TCPFrame {

    private final String type;      // ex: "TEXT", "IMAGE", "REACTION"
    private final byte[] payload;   // bytes bruts (json, binaire, etc.)

    public TCPFrame(String type, byte[] payload) {
        this.type = Objects.requireNonNull(type, "type");
        this.payload = Objects.requireNonNull(payload, "payload");
    }

    public String type() { return type; }
    public byte[] payload() { return payload; }

    public static TCPFrame ofText(String type, String textUtf8) {
        return new TCPFrame(type, textUtf8.getBytes(StandardCharsets.UTF_8));
    }

    public String payloadAsUtf8() {
        return new String(payload, StandardCharsets.UTF_8);
    }
}

