package me.stella.service;

import me.stella.wrappers.PropertyPair;
import me.stella.wrappers.enums.APIModule;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class PanelCommunication {

    public static String buildRequestEndpoint(String endpoint, APIModule module, String operation) {
        return "http://" + endpoint + "/" + module.getURL() + "/" + operation;
    }

    public static int requestCodeEndpointWithProperty(String endpoint, String method, String authentication, PropertyPair<String, String> property) {
        try {
            URL endpointURL = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) endpointURL.openConnection();
            connection.setRequestMethod(method);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Authorization", "Bearer " + authentication);
            if(property != null)
                connection.setRequestProperty(property.getKey(), property.getValue());
            connection.connect();
            return connection.getResponseCode();
        } catch(Throwable t) { t.printStackTrace(); }
        return -1;
    }

    public static JSONObject requestResponseEndpointWithProperty(String endpoint, String method, String authentication, PropertyPair<String, String> property) {
        try {
            URL endpointURL = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) endpointURL.openConnection();
            connection.setRequestMethod(method);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Authorization", "Bearer " + authentication);
            if(property != null)
                connection.setRequestProperty(property.getKey(), property.getValue());
            connection.connect();
            InputStream responseStream = connection.getInputStream();
            BufferedReader responseReader = new BufferedReader(new InputStreamReader(responseStream));
            StringBuilder response = new StringBuilder(); String data;
            while((data = responseReader.readLine()) != null)
                response.append(data).append("\n");
            return (JSONObject) new JSONParser().parse(response.toString());
        } catch(Throwable t) { t.printStackTrace(); }
        return new JSONObject();
    }

    public static int requestCodeEndpointWithPayload(String endpoint, String method, String authentication, JSONObject payload) {
        try {
            URL endpointURL = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) endpointURL.openConnection();
            connection.setRequestMethod(method);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Authorization", "Bearer " + authentication);
            connection.connect();
            if(payload != null) {
                OutputStream connectionStream = connection.getOutputStream();
                byte[] buffer = payload.toJSONString().getBytes(StandardCharsets.UTF_8);
                connectionStream.write(buffer, 0, buffer.length);
                connectionStream.flush(); connectionStream.close();
            }
            return connection.getResponseCode();
        } catch(Throwable t) { t.printStackTrace(); }
        return -1;
    }

    public static JSONObject requestResponseEndpointWithPayload(String endpoint, String method, String authentication, JSONObject payload) {
        try {
            URL endpointURL = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) endpointURL.openConnection();
            connection.setRequestMethod(method);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Authorization", "Bearer " + authentication);
            connection.connect();
            if(payload != null) {
                OutputStream connectionStream = connection.getOutputStream();
                byte[] buffer = payload.toJSONString().getBytes(StandardCharsets.UTF_8);
                connectionStream.write(buffer, 0, buffer.length);
                connectionStream.flush(); connectionStream.close();
            }
            InputStream responseStream = connection.getInputStream();
            BufferedReader responseReader = new BufferedReader(new InputStreamReader(responseStream));
            StringBuilder response = new StringBuilder(); String data;
            while((data = responseReader.readLine()) != null)
                response.append(data).append("\n");
            return (JSONObject) new JSONParser().parse(response.toString());
        } catch(Throwable t) { t.printStackTrace(); }
        return new JSONObject();
    }
}
