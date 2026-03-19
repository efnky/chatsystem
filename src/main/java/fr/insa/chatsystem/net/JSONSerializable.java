package fr.insa.chatsystem.net;
import org.json.JSONObject;


/**
 * Provides the interfaces for representing an object in a JSON format.
 */
public interface JSONSerializable {

    /**
     * Converts the Object to A JSON Object
     *
     * @return the JSONObjet which represents the Object
     */
    JSONObject toJSON();

    /**
     * Returns the String of The object in JSON format
     *
     * @return the string of The object in JSON format
     */
    default String toJSONString(){
        return toJSON().toString();
    };
}

