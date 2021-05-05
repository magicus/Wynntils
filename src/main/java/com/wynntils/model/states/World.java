package com.wynntils.model.states;

public class World {
    public final static World NONE = fromName("");
    public final static World LOBBY = fromName("Lobby");

    private final String name;

    public static World fromName(String name) {
        return new World(name);
    }

    public String getName() {
        return name;
    }

    private World(String name) {
        this.name = name;
    }

    public boolean isLobby() {
        return this == LOBBY;
    }
}
