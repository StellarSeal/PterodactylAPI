package me.stella.wrappers.enums;

public enum APIModule {

    CLIENT("api/client"),
    APPLICATION("api/application");

    private String url;

    APIModule(String url) {
        this.url = url;
    }

    public String getURL() {
        return this.url;
    }

}
