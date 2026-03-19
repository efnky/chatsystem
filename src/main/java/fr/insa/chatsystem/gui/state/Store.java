package fr.insa.chatsystem.gui.state;

public final class Store {

    private final AppState state = new AppState();

    public AppState state() {
        return state;
    }
}
