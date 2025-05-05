package me.stella;

import me.stella.service.PanelCommunication;
import me.stella.wrappers.*;
import me.stella.wrappers.enums.APIModule;
import me.stella.wrappers.enums.RequestProtocol;
import me.stella.wrappers.enums.ServerSignal;
import me.stella.wrappers.enums.ServerStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

public class PterodactylClient {

    private final RequestProtocol protocol;
    private final String panelEndpoint;
    private final String clientKey;

    public PterodactylClient(RequestProtocol protocol, String panelEndpoint, String clientKey) {
        this.protocol = protocol;
        this.panelEndpoint = panelEndpoint;
        this.clientKey = clientKey;
    }

    public CompletableFuture<Boolean> startServer(ServerWrapper server) {
        return CompletableFuture.supplyAsync(() -> sendSignal(server, ServerSignal.START).join());
    }

    public CompletableFuture<Boolean> stopServer(ServerWrapper server) {
        return CompletableFuture.supplyAsync(() -> sendSignal(server, ServerSignal.STOP).join());
    }

    public CompletableFuture<Boolean> restartServer(ServerWrapper server) {
        return CompletableFuture.supplyAsync(() -> sendSignal(server, ServerSignal.RESTART).join());
    }

    public CompletableFuture<Boolean> killServer(ServerWrapper server) {
        return CompletableFuture.supplyAsync(() -> sendSignal(server, ServerSignal.KILL).join());
    }

    public CompletableFuture<ResourceWrapper> getResourceUsage(ServerWrapper server) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JSONObject resourceObject = PanelCommunication.requestResponseEndpointWithProperty(buildClientEndpoint("servers/" + server.getIdentifier() + "/resources"),
                        "GET", this.clientKey, null);
                ServerStatus status = ServerStatus.parse(String.valueOf(((JSONObject)resourceObject.get("attributes")).get("current_state")));
                JSONObject resources = (JSONObject) ((JSONObject) resourceObject.get("attributes")).get("resources");
                long memoryBytes = Long.parseLong(String.valueOf(resources.get("memory_bytes")));
                double cpuAbsolute = Double.parseDouble(String.valueOf(resources.get("cpu_absolute")));
                long inbound = Long.parseLong(String.valueOf(resources.get("network_rx_bytes")));
                long outbound = Long.parseLong(String.valueOf(resources.get("network_tx_bytes")));
                return new ResourceWrapper(status, memoryBytes, cpuAbsolute, inbound, outbound);
            } catch(Throwable t) { t.printStackTrace(); }
            return null;
        });
    }

    public CompletableFuture<List<FileWrapper>> getFiles(ServerWrapper server, String directory, int pageLimit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JSONObject fileResponseObject = PanelCommunication.requestResponseEndpointWithParameter(buildClientEndpoint("servers/" + server.getIdentifier() + "/files/list"),
                        "GET", this.clientKey, Arrays.asList(PropertyPair.parse("directory", directory), PropertyPair.parse("per_page", Integer.toString(pageLimit))));
                List<FileWrapper> files = new ArrayList<>();
                JSONArray jsonFileResponse = (JSONArray) fileResponseObject.get("data");
                for(int i = 0; i < jsonFileResponse.size(); i++) {
                    JSONObject fileInfo = (JSONObject) ((JSONObject) jsonFileResponse.get(i)).get("attributes");
                    String name = String.valueOf(fileInfo.get("name"));
                    long size = Long.parseLong(String.valueOf(fileInfo.get("size")));
                    boolean file = Boolean.parseBoolean(String.valueOf(fileInfo.get("is_file")));
                    String creation = String.valueOf(fileInfo.get("created_at"));
                    String lastEdit = String.valueOf(fileInfo.get("modified_at"));
                    files.add(new FileWrapper(name, size, file, creation, lastEdit));
                }
                return files;
            } catch(Throwable t) { t.printStackTrace(); }
            return null;
        });
    }

    public CompletableFuture<Boolean> sendConsoleCommand(ServerWrapper wrapper, String command) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JSONObject payloadCommand = new JSONObject();
                payloadCommand.put("command", command);
                int response = PanelCommunication.requestCodeEndpointWithPayload(buildClientEndpoint("servers/" + wrapper.getIdentifier() + "/command"),
                        "POST", this.clientKey, payloadCommand);
                return response != 502;
            } catch(Exception err) { err.printStackTrace(); }
            return false;
        });
    }

    public CompletableFuture<File> downloadFile(ServerWrapper server, String source, File destination) {
        if(!destination.isDirectory())
            throw new RuntimeException("Please supply a folder, not a file for this function !");
        return CompletableFuture.supplyAsync(() -> {
           try {
               String[] dir = source.split( "/"); String fileName = dir[dir.length - 1];
               JSONObject downloadPayload = PanelCommunication.requestResponseEndpointWithParameter(
                       buildClientEndpoint("servers/" + server.getIdentifier() + "/files/download"), "GET", this.clientKey,
                       Collections.singletonList(PropertyPair.parse("file", source)));
               final String downloadURL = String.valueOf(((JSONObject)downloadPayload.get("attributes")).get("url"));
               URL downloadReadURL = new URL(downloadURL);
               InputStream downloadStream = downloadReadURL.openStream();
               BufferedInputStream streamReader = new BufferedInputStream(downloadStream);
               File output = new File(destination, fileName);
               FileOutputStream outputStream = new FileOutputStream(output);
               byte[] buffer = new byte[65536]; int data;
               while((data = streamReader.read(buffer, 0, 65536)) != -1)
                   outputStream.write(buffer, 0, data);
               outputStream.flush(); outputStream.close(); streamReader.close(); downloadStream.close();
               return output.getAbsoluteFile();
           } catch(Throwable t) { t.printStackTrace(); }
           return null;
        });
    }

    @SuppressWarnings("unchecked")
    public CompletableFuture<String> renameFile(ServerWrapper wrapper, String oldName, String newName) {
        return CompletableFuture.supplyAsync(() -> {
           try {
               JSONObject payload = new JSONObject();
               JSONArray files = new JSONArray();
               payload.put("root", "/");
               JSONObject moveExp = new JSONObject();
               moveExp.put("from", oldName);
               moveExp.put("to", newName);
               files.add(moveExp);
               payload.put("files", files);
               PanelCommunication.requestCodeEndpointWithPayload(buildClientEndpoint("servers/" + wrapper.getIdentifier() + "/files/rename"),
                       "PUT", this.clientKey, payload);

               return newName;
           } catch(Exception err) { err.printStackTrace(); }
           return oldName;
        });
    }

    public CompletableFuture<String> uploadFile(ServerWrapper server, String directory, File file) {
        if (!file.isFile()) throw new RuntimeException("Invalid file.");
        return CompletableFuture.supplyAsync(() -> {
            HttpURLConnection connection = null;
            try {
                JSONObject uploadPayload = PanelCommunication.requestResponseEndpointWithProperty(
                        buildClientEndpoint("servers/" + server.getIdentifier() + "/files/upload"), "GET", this.clientKey, null);
                String uploadURL = String.valueOf(((JSONObject) uploadPayload.get("attributes")).get("url"));
                String boundary = "----WebKitFormBoundary" + UUID.randomUUID().toString().replaceAll("-", "");
                URL url = new URL(uploadURL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                connection.setDoOutput(true);
                try (OutputStream outputStream = connection.getOutputStream(); FileInputStream fileInput = new FileInputStream(file)) {
                    String formDataHeader = "--" + boundary + "\r\n" +
                            "Content-Disposition: form-data; name=\"files\"; filename=\"" + file.getName() + "\"\r\n" +
                            "Content-Type: application/octet-stream\r\n\r\n";
                    outputStream.write(formDataHeader.getBytes());

                    byte[] buffer = new byte[65536];
                    int bytesRead;
                    while ((bytesRead = fileInput.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }

                    String formDataFooter = "\r\n--" + boundary + "--\r\n";
                    outputStream.write(formDataFooter.getBytes());
                    outputStream.flush();
                }

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                        StringBuilder response = new StringBuilder(); String inputLine;
                        while ((inputLine = in.readLine()) != null) response.append(inputLine);
                        throw new RuntimeException("Upload failed! Code: " + responseCode + " - Trace:" + response);
                    }
                }
                String dest = directory + "/" + file.getName();
                if(!directory.isEmpty()) {
                    deleteFile(server, dest).join();
                    renameFile(server, file.getName(), dest).join();
                    // bleh
                }
                return dest;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null)
                    connection.disconnect();
            }
            return null;
        });
    }


    public CompletableFuture<Boolean> deleteFile(ServerWrapper server, String target) {
        return deleteFiles(server, Collections.singletonList(target));
    }

    @SuppressWarnings("unchecked")
    public CompletableFuture<Boolean> deleteFiles(ServerWrapper server, List<String> targets) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JSONArray fileList = new JSONArray();
                JSONObject body = new JSONObject();
                targets.forEach(file -> fileList.add(file));
                body.put("root", "/");
                body.put("files", fileList);
                return PanelCommunication.requestCodeEndpointWithPayload(buildClientEndpoint("servers/" + server.getIdentifier() + "/files/delete"),
                        "POST", this.clientKey, body) == 204;
            } catch(Throwable t) { t.printStackTrace(); }
            return false;
        });
    }

    public CompletableFuture<String> compressFile(ServerWrapper server, String target) {
        return compressFile(server, Collections.singletonList(target));
    }

    public CompletableFuture<String> compressFile(ServerWrapper server, List<String> targets) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JSONArray fileList = new JSONArray();
                JSONObject body = new JSONObject();
                targets.forEach(file -> fileList.add(file));
                body.put("root", "/");
                body.put("files", fileList);
                JSONObject reponse = PanelCommunication.requestResponseEndpointWithPayload(buildClientEndpoint("servers/" + server.getIdentifier() + "/files/compress"),
                        "POST", this.clientKey, body);
                return String.valueOf(((JSONObject)reponse.get("attributes")).get("name"));
            } catch(Throwable t) { t.printStackTrace(); }
            return null;
        });
    }

    public CompletableFuture<Boolean> decompressFile(ServerWrapper server, String target) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("root", "/");
                payload.put("file", target);
                return PanelCommunication.requestCodeEndpointWithPayload(buildClientEndpoint("servers/" + server.getIdentifier() + "/files/decompress"),
                        "POST", this.clientKey, payload) == 204;
            } catch(Throwable t) { t.printStackTrace(); }
            return false;
        });
    }

    public CompletableFuture<List<BackupWrapper>> getBackups(ServerWrapper server) {
        return CompletableFuture.supplyAsync(() -> {
           try {
               List<BackupWrapper> backups = new ArrayList<>();
               JSONObject response = PanelCommunication.requestResponseEndpointWithParameter(buildClientEndpoint("servers/" + server.getIdentifier() + "/backups"),
                       "GET", this.clientKey, Collections.singletonList(PropertyPair.parse("per_page", "64")));
               JSONArray backupJSONArray = (JSONArray) response.get("data");
               for(Object arrayElementObject: backupJSONArray) {
                   JSONObject backupJSONObject = (JSONObject) arrayElementObject;
                   JSONObject attributes = (JSONObject) backupJSONObject.get("attributes");
                   UUID uid = UUID.fromString(String.valueOf(attributes.get("uuid")));
                   String name = String.valueOf(attributes.get("name"));
                   String sha256_hash = String.valueOf(attributes.get("sha256_hash"));
                   long byteSize = Long.parseLong(String.valueOf(attributes.get("bytes")));
                   String created = String.valueOf(attributes.get("created_at"));
                   String completed = String.valueOf(attributes.get("completed_at"));
                   backups.add(new BackupWrapper(uid, name, sha256_hash, byteSize, created, completed));
               }
               return backups;
           } catch(Exception err) { err.printStackTrace(); }
           return Collections.emptyList();
        });
    }

    public CompletableFuture<Boolean> createBackup(ServerWrapper server, String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("name", name);
                payload.put("ignored", "");
                payload.put("is_locked", false);
                return PanelCommunication.requestCodeEndpointWithPayload(buildClientEndpoint("servers/" + server.getIdentifier() + "/backups"),
                        "POST", this.clientKey, payload) == 204;
            } catch(Throwable t) { t.printStackTrace(); }
            return false;
        });
    }

    public CompletableFuture<Boolean> deleteBackup(ServerWrapper server, BackupWrapper backup) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return PanelCommunication.requestCodeEndpointWithParameter(buildClientEndpoint("servers/" + server.getIdentifier() + "/backups/" + backup.getUid().toString()),
                        "DELETE", this.clientKey, null) == 204;
            } catch(Throwable t) { t.printStackTrace(); }
            return false;
        });
    }

    private CompletableFuture<Boolean> sendSignal(ServerWrapper server, ServerSignal signal) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("signal", signal.name().toLowerCase());
                return PanelCommunication.requestCodeEndpointWithPayload(buildClientEndpoint("servers/" + server.getIdentifier() + "/power"),
                        "POST", this.clientKey, payload) == 204;
            } catch(Throwable t) { t.printStackTrace(); }
            return false;
        });
    }

    private String buildClientEndpoint(String operation) {
        return PanelCommunication.buildRequestEndpoint(this.protocol, this.panelEndpoint, APIModule.CLIENT, operation);
    }

}
