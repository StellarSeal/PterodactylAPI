package me.stella.wrappers;

import me.stella.wrappers.enums.ServerStatus;

public class ResourceWrapper {

    private final ServerStatus status;
    private final long memoryUsage;
    private final double cpuUsage;
    private final long bytesInbound;
    private final long bytesOutbound;

    public ResourceWrapper(ServerStatus status, long memory, double cpu, long inbound, long outbound) {
        this.status = status;
        this.memoryUsage = memory;
        this.cpuUsage = cpu;
        this.bytesInbound = inbound;
        this.bytesOutbound = outbound;
    }

    public ServerStatus getStatus() {
        return this.status;
    }

    public long getMemoryUsage() {
        return memoryUsage;
    }

    public double getCpuUsage() {
        return cpuUsage;
    }

    public long getBytesInbound() {
        return bytesInbound;
    }

    public long getBytesOutbound() {
        return bytesOutbound;
    }
}
