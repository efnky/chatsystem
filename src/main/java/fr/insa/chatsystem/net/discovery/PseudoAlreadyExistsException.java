package fr.insa.chatsystem.net.discovery;

import fr.insa.chatsystem.net.NetworkException;

public class PseudoAlreadyExistsException extends NetworkException {

    private final String name;

    public PseudoAlreadyExistsException(String name) {
        super(name);
        this.name = name;
    }

    @Override
    public String toString() {
        return "Pseudo is already used : "+name;
    }
}
