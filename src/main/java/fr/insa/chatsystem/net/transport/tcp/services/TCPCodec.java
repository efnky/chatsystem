package fr.insa.chatsystem.net.transport.tcp.services;

public interface TCPCodec<T> {
    byte[] encode(T msg);
    T decode(byte[] payload);
}
