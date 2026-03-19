package fr.insa.chatsystem.net.discovery;

import fr.insa.chatsystem.net.JSONSerializable;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a participant in the discovery network.
 *
 * <p>A {@code User} combines:
 * <ul>
 *   <li>a stable identifier ({@link #getID()}) used to uniquely identify a peer,</li>
 *   <li>a public pseudonym ({@link #getPseudo()}) displayed to other users,</li>
 *   <li>a network endpoint ({@link #getAddress()}, {@link #getPort()}) used for communication,</li>
 *   <li>a local role ({@link Type}) distinguishing the local host from remote peers.</li>
 * </ul>
 *
 * <h2>Serialization</h2>
 * <p>This class implements {@link JSONSerializable}. The JSON representation includes:
 * <ul>
 *   <li>{@code "id"}: integer user identifier</li>
 *   <li>{@code "pseudo"}: user pseudonym</li>
 *   <li>{@code "address"}: IPv4/IPv6 string representation</li>
 *   <li>{@code "port"}: UDP port number</li>
 * </ul>
 *
 * <p>The {@link Type} is intentionally not serialized by default because it is a local concern:
 * the same remote user is always a {@link Type#PEER} from our perspective, while our local user
 * is {@link Type#HOST}. If you want to serialize the type, you can add it to {@link #toJSON()}.
 *
 * <h2>Equality</h2>
 * <p>{@link #equals(Object)} and {@link #hashCode()} are based on {@code id} only. This assumes
 * {@code id} is globally stable and unique in your network model.</p>
 */
public final class User implements JSONSerializable {

    /**
     * Local role of the user.
     * <ul>
     *   <li>{@link #HOST}: the user representing the local machine running the program</li>
     *   <li>{@link #PEER}: a remote user discovered on the network</li>
     * </ul>
     */
    public enum Type {
        HOST,
        PEER
    }

    /** JSON key for the user identifier. */
    private static final String K_ID = "id";
    /** JSON key for the user pseudonym. */
    private static final String K_PSEUDO = "pseudo";
    /** JSON key for the user IP address. */
    private static final String K_ADDRESS = "address";
    /** JSON key for the user UDP port. */
    private static final String K_PORT = "port";
    // Optional if you want to serialize it:
    // private static final String K_TYPE = "type";

    /** Stable identifier (expected to be unique in your network model). */
    private final UUID id;

    /** Local classification (host vs peer). Not serialized by default. */
    private final Type type;

    /** Public pseudonym displayed to other peers (mutable via {@link #setPseudo(String)}). */
    private String pseudo;

    /** Network endpoint address of this user. */
    private final InetAddress address;

    /** Network endpoint port of this user (1..65535). */
    private final int port;

    /**
     * Creates a new {@code User}.
     *
     * @param id unique user identifier (must be stable for correct {@link #equals(Object)} behavior)
     * @param pseudo public pseudonym (non-null, non-blank)
     * @param type local role of this user (non-null)
     * @param address network address of this user (non-null)
     * @param port UDP port of this user (1..65535)
     * @throws NullPointerException if {@code type}, {@code address} or {@code pseudo} is null
     * @throws IllegalArgumentException if {@code pseudo} is blank or {@code port} is out of range
     */
    public User(UUID id, String pseudo, Type type, InetAddress address, int port) {
        this.id = id;
        this.pseudo = requireNonBlank(pseudo, "pseudo");
        this.type = Objects.requireNonNull(type, "type");
        this.address = Objects.requireNonNull(address, "address");
        this.port = validatePort(port);
    }

    /**
     * Builds a {@link Type#PEER} {@code User} instance from JSON.
     *
     * <p>This factory intentionally creates a {@link Type#PEER} because the JSON represents a remote user
     * from the local machine point of view.</p>
     *
     * @param json JSON object containing at least {@code id}, {@code pseudo}, {@code address}, {@code port}
     * @return a new {@code User} instance with {@link Type#PEER}
     * @throws JSONException if a required field is missing, has the wrong type, or the address is invalid
     */
    public static User peerFromJSON(JSONObject json) throws JSONException {
        UUID id = UUID.fromString(json.getString(K_ID));
        String pseudo = json.getString(K_PSEUDO);
        int port = json.getInt(K_PORT);

        InetAddress addr;
        try {
            addr = InetAddress.getByName(json.getString(K_ADDRESS));
        } catch (UnknownHostException e) {
            throw new JSONException("Unknown host: " + e.getMessage());
        }

        return new User(id, pseudo, Type.PEER, addr, port);
    }

    /** @return the stable identifier of this user. */
    public UUID getID() { return id; }

    /** @return the current pseudonym of this user. */
    public String getPseudo() { return pseudo; }

    /** @return the local role of this user (HOST or PEER). */
    public Type getType() { return type; }

    /** @return the network address of this user. */
    public InetAddress getAddress() { return address; }

    /** @return the network port of this user. */
    public int getPort() { return port; }

    /**
     * Updates the pseudonym of this user.
     *
     * <p>This is typically used when the protocol accepts a pseudo change request ("np").</p>
     *
     * @param pseudo new pseudonym (non-null, non-blank)
     * @throws NullPointerException if {@code pseudo} is null
     * @throws IllegalArgumentException if {@code pseudo} is blank
     */
    public void setPseudo(String pseudo) {
        this.pseudo = requireNonBlank(pseudo, "pseudo");
    }

    /**
     * Serializes this user to JSON.
     *
     * @return a {@link JSONObject} containing the user's id, pseudonym, address and port
     */
    @Override
    public JSONObject toJSON() {
        return new JSONObject()
                .put(K_ID, id)
                .put(K_PSEUDO, pseudo)
                .put(K_ADDRESS, address.getHostAddress())
                .put(K_PORT, port);
        // If you want to serialize it:
        // .put(K_TYPE, type.name().toLowerCase());
    }

    /** @return compact JSON string representation of {@link #toJSON()}. */
    @Override
    public String toJSONString() {
        return toJSON().toString();
    }

    /**
     * Returns a debug-friendly string representation.
     *
     * @return a string including id, pseudo, address, port and type
     */
    @Override
    public String toString() {
        return "User{id=" + id +
                ", pseudo='" + pseudo + '\'' +
                ", address=" + address.getHostAddress() +
                ", port=" + port +
                ", type=" + type +
                '}';
    }

    /**
     * Equality is based solely on {@code id}.
     *
     * @param o other object
     * @return true if the other object is a {@code User} with the same id
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return id == other.id;
    }

    /** @return hash code derived from {@code id}. */
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * Validates a UDP port number.
     *
     * @param port port to validate
     * @return the port if valid
     * @throws IllegalArgumentException if port is not in range 1..65535
     */
    private static int validatePort(int port) {
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid port: " + port);
        }
        return port;
    }

    /**
     * Ensures a string is non-null and non-blank.
     *
     * @param s input string
     * @param name field name used for error messages
     * @return the same string if valid
     * @throws NullPointerException if {@code s} is null
     * @throws IllegalArgumentException if {@code s} is blank
     */
    private static String requireNonBlank(String s, String name) {
        Objects.requireNonNull(s, name);
        if (s.isBlank()) throw new IllegalArgumentException(name + " cannot be blank");
        return s;
    }
}
