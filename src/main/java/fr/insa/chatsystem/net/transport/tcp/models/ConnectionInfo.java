package fr.insa.chatsystem.net.transport.tcp.models;

import java.net.InetAddress;
import java.util.UUID;

public record ConnectionInfo(UUID id, InetAddress address, int port) {}
