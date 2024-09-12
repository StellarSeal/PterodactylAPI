package me.stella.wrappers.enums;

import java.util.Arrays;
import java.util.List;

public enum ServerStatus {
    STARTING(Arrays.asList("starting", "booting", "start")),
    RUNNING(Arrays.asList("running", "online", "active")),
    STOPPING(Arrays.asList("stopping", "shutting", "stop")),
    STOPPED(Arrays.asList("stopped", "offline", "stop")),
    CRASHED(Arrays.asList("crashed", "crash"));

    private final List<String> aliases;

    ServerStatus(List<String> aliases) {
        this.aliases = aliases;
    }

    public List<String> getAliases() {
        return this.aliases;
    }

    public static ServerStatus parse(String status) {
        status = status.toLowerCase();
        for(ServerStatus statusEnumValue: ServerStatus.values()) {
            if(statusEnumValue.getAliases().contains(status))
                return statusEnumValue;
        }
        throw new RuntimeException("Invalid server status!" + status);
    }
}
