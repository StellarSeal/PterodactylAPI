package me.stella.wrappers;

public class ServerWrapper {

    private final String name;
    private final String id;
    private final String identifier;

    public ServerWrapper(String name, String id, String identifier) {
        this.name = name;
        this.id = id;
        this.identifier = identifier;
    }

    public final String getId() {
        return this.id;
    }

    public final String getName() {
        return name;
    }

    public final String getIdentifier() {
        return this.identifier;
    }

}
