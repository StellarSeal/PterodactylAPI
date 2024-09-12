package me.stella;

import me.stella.service.MultithreadedStream;
import me.stella.service.PanelCommunication;
import me.stella.wrappers.PropertyPair;
import me.stella.wrappers.ResourceWrapper;
import me.stella.wrappers.ServerWrapper;
import me.stella.wrappers.enums.APIModule;
import me.stella.wrappers.enums.ServerSignal;
import me.stella.wrappers.enums.ServerStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PterodactylClient {

    private final String panelEndpoint;
    private final String clientKey;

    public PterodactylClient(String panelEndpoint, String clientKey) {
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
                JSONObject resourceObject = PanelCommunication.requestResponseEndpointWithProperty("servers/" + server.getIdentifier() + "/resources",
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

    public CompletableFuture<File> downloadFile(ServerWrapper server, String source, File destination, int threads) {
        if(!destination.isDirectory())
            throw new RuntimeException("Please supply a folder, not a file for this function !");
        return CompletableFuture.supplyAsync(() -> {
           try {
               String[] dir = source.split("/"); String fileName = dir[dir.length - 1];
               JSONObject downloadPayload = PanelCommunication.requestResponseEndpointWithProperty(
                       buildClientEndpoint("servers/" + server.getIdentifier() + "/files/download"), "GET", this.clientKey,
                       PropertyPair.parse("file", source));
               final String downloadURL = String.valueOf(((JSONObject)downloadPayload.get("attributes")).get("url"));
               URL downloadReadURL = new URL(downloadURL);
               HttpURLConnection cachingConnection = (HttpURLConnection) downloadReadURL.openConnection();
               cachingConnection.setRequestMethod("HEAD");
               long contentSize = cachingConnection.getContentLengthLong();
               ExecutorService threadBuilder = Executors.newFixedThreadPool(threads);
               long[] partSizes = MultithreadedStream.getSegmentSize(contentSize, threads);
               List<Future<?>> parts = new ArrayList<>();
               Map<Integer, File> downloadedPart = new HashMap<>();
               long read = -1;
               for(int i = 0; i < partSizes.length; i++) {
                   final long byteStart = read + 1;
                   final long byteEnd = read + partSizes[i];
                   final int index = i + 1;
                   parts.add(threadBuilder.submit(() -> {
                       MultithreadedStream.downloadSegment(destination, downloadURL, byteStart, byteEnd)
                               .thenAccept(file -> downloadedPart.put(index, file));
                   }));
                   read += partSizes[i];
               }
               for(Future<?> tasks: parts)
                   tasks.get();
               File output = new File(destination, fileName);
               BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(output));
               for(int x = 1; x <= partSizes.length; x++) {
                   BufferedInputStream partReader = new BufferedInputStream(new FileInputStream(downloadedPart.get(x)));
                   byte[] buffer = new byte[65536]; int data;
                   while((data = partReader.read(buffer)) != -1)
                       outputStream.write(buffer, 0, data);
                   partReader.close();
               }
               outputStream.flush(); outputStream.close(); threadBuilder.shutdown();
               return output.getAbsoluteFile();
           } catch(Throwable t) { t.printStackTrace(); }
           return null;
        });
    }

    public CompletableFuture<String> uploadFile(ServerWrapper server, File file, int threads) {
        if(!file.isFile())
            throw new RuntimeException("Please supply a valid file! Not a direcctory.");
        return CompletableFuture.supplyAsync(() -> {
            try {
                JSONObject downloadPayload = PanelCommunication.requestResponseEndpointWithProperty(
                        buildClientEndpoint("servers/" + server.getIdentifier() + "/files/upload"), "GET", this.clientKey, null);
                final String uploadURL = String.valueOf(((JSONObject)downloadPayload.get("attributes")).get("url"));
                long[] segments = MultithreadedStream.getSegmentSize(file.length(), threads);
                ExecutorService threadFactory = Executors.newFixedThreadPool(threads);
                List<Future<?>> tasks = new ArrayList<>(); long byteCount = -1;
                for (long segment : segments) {
                    final long byteStart = byteCount + 1;
                    final long byteEnd = byteCount + segment;
                    tasks.add(threadFactory.submit(() -> {
                        MultithreadedStream.uploadSegment(file, uploadURL, byteStart, byteEnd).join();
                    }));
                    byteCount += segment;
                }
                for(Future<?> running: tasks)
                    running.get();
                return file.getName();
            } catch(Throwable t) { t.printStackTrace(); }
            return null;
        });
    }

    public CompletableFuture<Boolean> deleteFile(ServerWrapper server, String target) {
        return deleteFiles(server, Collections.singletonList(target));
    }

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

    private CompletableFuture<Boolean> sendSignal(ServerWrapper server, ServerSignal signal) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return PanelCommunication.requestCodeEndpointWithProperty(buildClientEndpoint("servers/" + server.getIdentifier() + "/power"),
                        "POST", this.clientKey, PropertyPair.parse("signal", signal.name().toLowerCase())) == 204;
            } catch(Throwable t) { t.printStackTrace(); }
            return false;
        });
    }

    private String buildClientEndpoint(String operation) {
        return PanelCommunication.buildRequestEndpoint(this.panelEndpoint, APIModule.CLIENT, operation);
    }

}
