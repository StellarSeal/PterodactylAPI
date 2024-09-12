package me.stella.wrappers.enums;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum ServerSignal {

    START(Arrays.asList("on", "start", "boot")),
    STOP(Arrays.asList("off", "stop", "shutdown")),
    RESTART(Arrays.asList("restart", "reboot")),
    KILL(Collections.singletonList("kill"));

    private final List<String> aliases;

    ServerSignal(List<String> aliases) {
        this.aliases = aliases;
    }

    public List<String> getAliases() {
        return this.aliases;
    }

    public static ServerSignal parse(String status) {
        status = status.toLowerCase();
        for(ServerSignal signalEnumValue: ServerSignal.values()) {
            if(signalEnumValue.getAliases().contains(status))
                return signalEnumValue;
        }
        throw new RuntimeException("Invalid server status!" + status);
    }

}
