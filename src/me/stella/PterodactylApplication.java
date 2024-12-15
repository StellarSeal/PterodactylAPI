package me.stella;

import me.stella.service.PanelCommunication;
import me.stella.wrappers.PropertyPair;
import me.stella.wrappers.ServerWrapper;
import me.stella.wrappers.enums.APIModule;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;


import java.util.function.Function;

/**
 * A wrapper representing a panel app using the Pterodactyl API
 *
 * @author Stella
 */
public class PterodactylApplication {

    private final String panelEndpoint;
    private final String appKey;

    public PterodactylApplication(String panelEndpoint, String clientKey) {
        this.panelEndpoint = panelEndpoint;
        this.appKey = clientKey;
    }

    public CompletableFuture<List<ServerWrapper>> getServers() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JSONObject response = PanelCommunication.requestResponseEndpointWithParameter(buildApplicationEndpoint("servers"),
                        "GET", this.appKey, Collections.singletonList(PropertyPair.parse("per_page", "255")));
                JSONArray serverList = (JSONArray) response.get("data");
                List<ServerWrapper> wrappers = new ArrayList<>();
                for(int i = 0; i < serverList.size(); i++) {
                    JSONObject attr = (JSONObject) ((JSONObject) serverList.get(i)).get("attributes");
                    String name = String.valueOf(attr.get("name"));
                    String id = String.valueOf(attr.get("id"));
                    String identifier = String.valueOf(attr.get("identifier"));
                    wrappers.add(new ServerWrapper(name, id, identifier));
                }
                return wrappers;
            } catch(Throwable t) { t.printStackTrace(); }
            return Collections.emptyList();
        });
    }

    public CompletableFuture<ServerWrapper> getServerByIdentifier(String identifier) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<ServerWrapper> servers = getServers().join();
                for(ServerWrapper wrapper: servers) {
                    if(wrapper.getIdentifier().equals(identifier))
                        return wrapper;
                }
            } catch(Throwable t) { t.printStackTrace(); }
            return null;
        });
    }

    public CompletableFuture<ServerWrapper> getServerByName(String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<ServerWrapper> servers = getServers().join();
                for(ServerWrapper wrapper: servers) {
                    if(wrapper.getName().equals(name))
                        return wrapper;
                }
            } catch(Throwable t) { t.printStackTrace(); }
            return null;
        });
    }

    public CompletableFuture<List<ServerWrapper>> getServersByFilter(Function<ServerWrapper, Boolean> filter) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<ServerWrapper> servers = getServers().join();
                List<ServerWrapper> filtered = new ArrayList<>();
                for(ServerWrapper server: servers) {
                    if(filter.apply(server))
                        filtered.add(server);
                }
                return filtered;
            } catch(Throwable t) { t.printStackTrace(); }
            return Collections.emptyList();
        });
    }

    public CompletableFuture<Boolean> setBackupCount(ServerWrapper server, int amount) {
        return CompletableFuture.supplyAsync(() -> {
           try {
               JSONObject payload = new JSONObject();
               JSONObject featureLimits = new JSONObject();
               payload.put("allocation", 1);
               payload.put("memory", 0);
               payload.put("swap", 0);
               payload.put("io", 500);
               payload.put("cpu", 0);
               payload.put("disk", 0);
               payload.put("threds", null);
               featureLimits.put("databases", 0);
               featureLimits.put("allocations", 0);
               featureLimits.put("backups", amount);
               payload.put("feature_limits", featureLimits);
               return PanelCommunication.requestCodeEndpointWithPayload(buildApplicationEndpoint("servers/" + server.getId() + "/build"),
                       "PATCH", this.appKey, payload) == HttpURLConnection.HTTP_OK;
           } catch(Exception err) { err.printStackTrace(); }
           return false;
        });
    }

    private String buildApplicationEndpoint(String operation) {
        return PanelCommunication.buildRequestEndpoint(this.panelEndpoint, APIModule.APPLICATION, operation);
    }

}
