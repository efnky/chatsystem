package fr.insa.chatsystem.net.messaging.events;

import fr.insa.chatsystem.net.messaging.models.ChatMsg;
import org.json.JSONObject;

/**
 * Small helper responsible for turning a raw JSON String into a typed {@link ChatMsg}.
 *
 * Role in the decoding pipeline:
 *  1) UDP layer (or any transport) receives a String payload
 *  2) {@link fr.insa.chatsystem.net.discovery.events.ChatMsgFactory#fromJSONString(String)} parses it into a {@link JSONObject}
 *  3) {@link ChatMsgRegistry#fromJSON(JSONObject)} selects the correct message class
 *     and delegates construction to the appropriate {@code <Msg>.fromJSON(...)} method.
 *
 * Why this class exists:
 *  - Keeps parsing concerns out of higher-level code (controllers, services)
 *  - Centralizes the "String -> JSONObject -> ChatMsg" workflow
 *
 * Note:
 *  - If invalid JSON is provided, {@link JSONObject} will throw an exception.
 *    You can decide to catch it here and wrap it into your own protocol exception
 *    if you want cleaner error handling at call sites.
 */
public class ChatMsgFactory {

    /** Utility class: no instances. */
    private ChatMsgFactory() {}

    /**
     * Decodes a JSON string into the corresponding {@link ChatMsg} subtype.
     *
     * Expected input:
     *  - a valid JSON string containing at least a "type" field (used by {@link ChatMsgRegistry})
     *
     * @param jsonString raw JSON payload (typically received over the Chat)
     * @return decoded {@link ChatMsg} instance (actual runtime type depends on the "type" field)
     * @throws org.json.JSONException if jsonString is not valid JSON or misses required fields
     * @throws IllegalArgumentException if the message type is unknown/unregistered in {@link ChatMsgRegistry}
     */
    public static ChatMsg fromJSONString(String jsonString) {
        // Parse raw text into a JSON object. This is the only place where we depend on "org.json".
        JSONObject json = new JSONObject(jsonString);

        // Delegate message instantiation to the registry (type -> factory).
        return ChatMsgRegistry.fromJSON(json);
    }
}


