package me.stella.wrappers;

public class FileWrapper {

    private final String name;
    private final long size;
    private final boolean file;
    private final String created;
    private final String lastModified;

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public boolean isFile() {
        return file;
    }

    public String getCreated() {
        return created;
    }

    public String getLastModified() {
        return lastModified;
    }

    public FileWrapper(String name, long size, boolean file, String creation, String mod) {
        this.name = name;
        this.size = size;
        this.file = file;
        this.created = creation;
        this.lastModified = mod;
    }

}
