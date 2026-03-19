package fr.insa.chatsystem.net.messaging.models;

import fr.insa.chatsystem.net.discovery.events.NetworkMsgType;
import fr.insa.chatsystem.net.message.IMsgType;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Type discriminator for chat messages.
 *
 * <p>This enum is used to identify the concrete {@link ChatMsg} subtype during decoding.</p>
 *
 * <p>Protocol stability:</p>
 * <ul>
 *   <li>{@link #code} is a stable numeric identifier (compact, version-friendly).</li>
 *   <li>{@link #label} is a human-readable identifier (logs/debug).</li>
 * </ul>
 *
 * <p>Pick ONE JSON representation and keep it consistent across versions:
 * either store {@code code} or store {@code label}. Do not mix both.</p>
 */
public enum ChatMsgType implements IMsgType {

    TEXT_MSG(0, "text"),
    IMAGE_MSG(1, "image"),
    REACTION_MSG(2, "reaction"),
    ;

    /** Numeric identifier intended to be stable across versions of the protocol. */
    private final int code;

    /** Human-readable label (also used as a canonical external string identifier). */
    private final String label;

    /**
     * Reverse lookup table: label -> enum value.
     * This avoids iterating over {@link #values()} on each parse.
     */
    private static final Map<String, ChatMsgType> MAP = new HashMap<>();

    /**
     * Static init: populate the reverse lookup map once at class loading.
     */
    static {
        for (ChatMsgType t : values()) {
            MAP.put(t.label, t);
        }
    }

    ChatMsgType(int code, String label){
        this.code = code;
        this.label = label;
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
    public static ChatMsgType fromJSON(JSONObject json) throws JSONException {
        String str = json.getString("type");
        ChatMsgType t = MAP.get(str);
        if (t == null) throw new JSONException("Unknown NetworkMsgType: " + str);
        return t;
    }

    /**
     * @return the canonical string label (useful for logs and debug output).
     */
    @Override
    public String toString() {
        return this.label;
    }

    /**
     * Required by {@link IMsgType}: label used by transport / higher layers.
     *
     * @return canonical label for this message type
     */
    @Override
    public String getLabel() {
        return this.label;
    }


}

