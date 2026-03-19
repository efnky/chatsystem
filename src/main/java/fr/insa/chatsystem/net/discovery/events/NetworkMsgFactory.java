package fr.insa.chatsystem.net.discovery.events;

import org.json.JSONObject;

/**
 * Small helper responsible for turning a raw JSON String into a typed {@link NetworkMsg}.
 *
 * Role in the decoding pipeline:
 *  1) UDP layer (or any transport) receives a String payload
 *  2) {@link NetworkMsgFactory#fromJSONString(String)} parses it into a {@link JSONObject}
 *  3) {@link NetworkMsgRegistry#fromJSON(JSONObject)} selects the correct message class
 *     and delegates construction to the appropriate {@code <Msg>.fromJSON(...)} method.
 *
 * Why this class exists:
 *  - Keeps parsing concerns out of higher-level code (controllers, services)
 *  - Centralizes the "String -> JSONObject -> NetworkMsg" workflow
 *
 * Note:
 *  - If invalid JSON is provided, {@link JSONObject} will throw an exception.
 *    You can decide to catch it here and wrap it into your own protocol exception
 *    if you want cleaner error handling at call sites.
 */
public class NetworkMsgFactory {

    /** Utility class: no instances. */
    private NetworkMsgFactory() {}

    /**
     * Decodes a JSON string into the corresponding {@link NetworkMsg} subtype.
     *
     * Expected input:
     *  - a valid JSON string containing at least a "type" field (used by {@link NetworkMsgRegistry})
     *
     * @param jsonString raw JSON payload (typically received over the network)
     * @return decoded {@link NetworkMsg} instance (actual runtime type depends on the "type" field)
     * @throws org.json.JSONException if jsonString is not valid JSON or misses required fields
     * @throws IllegalArgumentException if the message type is unknown/unregistered in {@link NetworkMsgRegistry}
     */
    public static NetworkMsg fromJSONString(String jsonString) {
        // Parse raw text into a JSON object. This is the only place where we depend on "org.json".
        JSONObject json = new JSONObject(jsonString);

        // Delegate message instantiation to the registry (type -> factory).
        return NetworkMsgRegistry.fromJSON(json);
    }
}

