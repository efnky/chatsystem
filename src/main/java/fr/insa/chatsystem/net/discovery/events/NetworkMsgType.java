package fr.insa.chatsystem.net.discovery.events;

import fr.insa.chatsystem.net.JSONSerializable;
import fr.insa.chatsystem.net.message.IMsgType;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumerates all message types used by the Discovery protocol.
 *
 * This enum is used as:
 *  - a protocol discriminator (so a receiver can decide which NetworkMsg subclass to instantiate)
 *  - a stable "label" exposed to the rest of the code (via {@link #getLabel()} / {@link #toString()})
 *  - a JSON-serializable value that can be embedded in UDP payloads
 *
 * ⚠ Important: the JSON representation must be consistent across sender/receiver.
 * Any mismatch between {@link #toJSON()} and {@link #fromJSON(JSONObject)} will break decoding.
 */
public enum NetworkMsgType implements IMsgType {

    /** Sent by a peer that wants to join the network (handshake start). */
    INIT_CONNECTION(0, "init-connection"),

    /** Sent as a positive response to {@link #INIT_CONNECTION}. */
    ACCEPT_CONNECTION(1, "accept-connection"),

    /** Sent as a negative response to {@link #INIT_CONNECTION}. */
    REJECT_CONNECTION(2, "reject-connection"),

    /** Sent by a peer that is leaving the network (graceful shutdown). */
    INIT_DISCONNECTION(3, "init-disconnection"),

    /** Sent to request a pseudonym change (or to validate a proposed pseudonym depending on your protocol). */
    REQUEST_PSEUDO(5, "request-pseudo"),

    /** Sent as a positive response to {@link #REQUEST_PSEUDO}. */
    VALID_PSEUDO(6, "valid-pseudo"),

    /** Sent as a negative response to {@link #REQUEST_PSEUDO}. */
    INVALID_PSEUDO(7, "invalid-pseudo"),
    ;

    /** Numeric identifier intended to be stable across versions of the protocol. */
    private final int code;

    /** Human-readable label (also used as a canonical external string identifier). */
    private final String string;

    /**
     * Reverse lookup table: label -> enum value.
     * This avoids iterating over {@link #values()} on each parse.
     */
    private static final Map<String, NetworkMsgType> MAP = new HashMap<>();

    /**
     * Static init: populate the reverse lookup map once at class loading.
     */
    static {
        for (NetworkMsgType t : values()) {
            MAP.put(t.string, t);
        }
    }

    /**
     * @param code numeric protocol code
     * @param string canonical string label (must be unique)
     */
    NetworkMsgType(int code, String string) {
        this.code = code;
        this.string = string;
    }

    /**
     * Serializes the type into JSON.
     *
     * Note: this method currently writes {@code "type"} as the numeric {@link #code}.
     * If your protocol expects the string label instead, this must be changed accordingly
     * (and must stay consistent with {@link #fromJSON(JSONObject)}).
     *
     * @return JSON object containing the type discriminator
     */
    @Override
    public JSONObject toJSON() {
        return new JSONObject().put("type", this.code);
    }

    /** @return the JSON representation as a compact String. */
    @Override
    public String toJSONString() {
        return toJSON().toString();
    }

    /**
     * Parses the message type from JSON and returns the matching enum.
     *
     * Current behavior:
     *  - reads {@code "type"} as a String label
     *  - looks it up in {@link #MAP}
     *
     * ⚠ Consistency warning:
     *  - {@link #toJSON()} writes an int
     *  - {@link #fromJSON(JSONObject)} reads a String
     * This inconsistency will cause runtime failures unless the JSON actually stores a string.
     *
     * @param json JSON object that contains a {@code "type"} field
     * @return corresponding {@link NetworkMsgType}
     * @throws JSONException if the field is missing or unknown
     */
    public static NetworkMsgType fromJSON(JSONObject json) throws JSONException {
        String str = json.getString("type");
        NetworkMsgType t = MAP.get(str);
        if (t == null) throw new JSONException("Unknown NetworkMsgType: " + str);
        return t;
    }

    /**
     * @return the canonical string label (useful for logs and debug output).
     */
    @Override
    public String toString() {
        return this.string;
    }

    /**
     * Required by {@link IMsgType}: label used by transport / higher layers.
     *
     * @return canonical label for this message type
     */
    @Override
    public String getLabel() {
        return this.string;
    }
}
