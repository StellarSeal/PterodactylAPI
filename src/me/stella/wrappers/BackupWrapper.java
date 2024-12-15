package me.stella.wrappers;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class BackupWrapper {

    private final UUID uid;
    private final String name;
    private final String sha256;
    private final long size;
    private final OffsetDateTime created;
    private final OffsetDateTime completed;

    public BackupWrapper(UUID uid, String name, String hash, long size, String created, String completed) {
        this.uid = uid;
        this.name = name;
        this.sha256 = hash;
        this.size = size;
        this.created = OffsetDateTime.parse(created, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        this.completed = OffsetDateTime.parse(completed, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    public UUID getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public String getSha256() {
        return sha256;
    }

    public long getSize() {
        return size;
    }

    public OffsetDateTime getCreated() {
        return created;
    }

    public OffsetDateTime getCompleted() {
        return completed;
    }
}
