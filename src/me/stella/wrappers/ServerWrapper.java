package me.stella.wrappers;

public class ServerWrapper {

    private final String name;
    private final String identifier;

    public ServerWrapper(String name, String identifier) {
        this.name = name;
        this.identifier = identifier;
    }

    public final String getName() {
        return name;
    }

    public final String getIdentifier() {
        return this.identifier;
    }

}
