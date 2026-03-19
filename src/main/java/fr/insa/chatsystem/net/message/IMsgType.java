package fr.insa.chatsystem.net.message;

import fr.insa.chatsystem.net.JSONSerializable;

/**
 * Common interface for message type enumerations used by {@link NetMsg}.
 * <p>
 * Each message type must provide a label used in JSON representations.
 */
public interface IMsgType extends JSONSerializable {

    /**
     * Returns the label associated with this message type.
     * <p>
     * This label is used when serializing the message type to JSON.
     *
     * @return non-null label string
     */
    String getLabel();
}
