package fr.insa.chatsystem.net.messaging.events;

import fr.insa.chatsystem.net.messaging.models.*;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.function.Function;

/**
 * Central registry that maps a {@link ChatMsgType} to a JSON factory able to rebuild
 * the corresponding {@link ChatMsg} subclass.
 *
 * Why this exists:
 *  - You receive a raw JSON payload over UDP.
 *  - You first read the "type" field (via {@link ChatMsgType#fromJSON(JSONObject)}).
 *  - Then you need the correct {@code <MsgClass>.fromJSON(json)} to instantiate the right message.
 *
 * This class avoids:
 *  - a huge switch/cascade of if/else in the decoder
 *  - scattering parsing logic across the codebase
 *
 * Design note:
 *  - This is essentially a "registry + factory" pattern.
 *  - Registration is performed once in the static initializer.
 */
public class ChatMsgRegistry {

    /**
     * Registry table: message type -> factory that builds a ChatMsg from JSON.
     *
     * The wildcard {@code ? extends ChatMsg} allows storing factories for any subclass
     * (AcceptanceMsg, RejectionMsg, etc.) in the same map.
     */
    private static final HashMap<ChatMsgType, Function<JSONObject, ? extends ChatMsg>> FACTORIES_FROM_JSON =
            new HashMap<>();

    /**
     * Static registration block.
     *
     * Executed once when the class is first loaded by the JVM.
     * We bind each {@link ChatMsgType} to its corresponding {@code fromJSON} factory method.
     *
     * Important consequence:
     *  - If this class is never referenced, this block never runs, and the registry stays empty.
     *    (So make sure something loads ChatMsgRegistry before decoding messages.)
     */
    static {
        ChatMsgRegistry.register(ChatMsgType.TEXT_MSG, TextMsg::fromJSON);
        ChatMsgRegistry.register(ChatMsgType.IMAGE_MSG, ImageMsg::fromJSON);
        ChatMsgRegistry.register(ChatMsgType.REACTION_MSG, ReactionMsg::fromJSON);
    }

    /**
     * Utility class: no instances.
     */
    private ChatMsgRegistry() {}

    /**
     * Registers a JSON factory for a message type.
     *
     * Typical usage is inside the static block:
     * {@code register(ChatMsgType.X, XMsg::fromJSON);}
     *
     * @param type message type discriminator found in JSON
     * @param creator factory that rebuilds the corresponding message object from JSON
     * @param <T> concrete message type
     */
    public static <T extends ChatMsg> void register(
            ChatMsgType type,
            Function<JSONObject, T> creator
    ) {
        // Note: this overwrites any previous mapping for the same type.
        // If you want stricter behavior, you could reject duplicates here.
        FACTORIES_FROM_JSON.put(type, creator);
    }

    /**
     * Decodes a raw JSON object into the correct {@link ChatMsg} subclass.
     *
     * Steps:
     *  1) Extract the {@link ChatMsgType} from JSON.
     *  2) Look up the matching factory in the registry.
     *  3) Delegate object creation to that factory.
     *
     * @param json raw JSON representation of a discovery message
     * @return an instantiated {@link ChatMsg} matching the JSON type
     * @throws IllegalArgumentException if the JSON type has no registered factory
     */
    public static ChatMsg fromJSON(JSONObject json) {
        // Determine the message type first (usually by reading a "type" field).
        ChatMsgType type = ChatMsgType.fromJSON(json);

        // Fetch the constructor/factory associated to this type.
        Function<JSONObject, ? extends ChatMsg> factory = FACTORIES_FROM_JSON.get(type);
        if (factory == null) {
            // Fail fast: receiving an unknown type is a protocol/configuration error.
            throw new IllegalArgumentException("No creator registered for type " + type);
        }

        // Delegate the actual parsing/instantiation to the message class.
        return factory.apply(json);
    }
}