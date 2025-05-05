package me.stella.wrappers.enums;

public enum RequestProtocol {

    HTTP("http://"),
    HTTPS("https://");

    private final String header;

    RequestProtocol(String header) {
        this.header = header;
    }

    public String getHeader() {
        return this.header;
    }

}
