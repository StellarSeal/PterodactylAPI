# About this API
This is a wrapper of the Pterodactyl API, made for Java, specifically designed for the LuckyVN network server's structure, with Pterodactyl as a core component

This wrapper uses the Pterodactyl documentation as reference, which can be found <a href="https://dashflo.net/docs/api/pterodactyl/v1/">here</a>.

> [!NOTE]
> All dependencies of this API is included in the JAR itseelf, so feel free to just add it to your project.

# How to use?
To fully utilize the API, you'll need a `Client Key` and an `Application Key`, which can both be created on the Pterodactyl panel.

Depending on your use case, you can use either <a href="https://github.com/LUCKYVN-NETWORK/PterodactylAPI/blob/main/src/me/stella/PterodactylApplication.java">PterodactylApplication</a> or <a href="https://github.com/LUCKYVN-NETWORK/PterodactylAPI/blob/main/src/me/stella/PterodactylClient.java">PterodactylClient</a>.
```java
PterodactylApplication application = new PterodactylApplication(YOUR_PANEL_URL, YOUR_APPLICATION_KEY);
```
```java
PterodactylClient client = new PterodactylClient(YOUR_PANEL_URL, YOUR_CLIENT_KEY);
```

> [!TIP]
> For methods returning a **CompletableFuture**, use the **join()** function to force the thread to complete the task before shutting down.

# Example - Using PterodactylApplication

## Get a list of your servers
```java
application.getServers().thenAccept(serverList -> {
  serverList.forEach(server -> {
    System.out.println("Server Name: " + server.getName() + " - Server ID: " + server.getIdentifier());
  }); 
}).join();
```

## Get a server by their name (must be exact match - case-sensitive)
```java
application.getServerByName("BEDWARS").thenAccept(server -> {
    System.out.println("Server " + server.getName() + " - Identifier: " + server.getIdentifier());
}).join();
```

### Get a server by their identifier (Found in URL: http://<panel_url>/<server_identifier>)
```java
application.getServerByIdentifier("a312072c").thenAccept(server -> {
    System.out.println("Server " + server.getName() + " - Identifier: " + server.getIdentifier());
}).join();
```

### Get a server using a custom filter
```java
// Get servers whose name starts with [181]:
application.getServerByFilter((server) -> {
    String name = server.getName();
    return name.startsWith("[181]");
}).thenAccept(servers -> {
    servers.forEach(server -> {
        System.out.println("Server " + server.getName() + ":" + server.getIdentifier() + " has their name starts with [181]");
    });
}).join();

// Get servers whose identifier contains the letter 'b'
application.getServerByFilter((server) -> {
    String id = server.getIdentifier();
    return id.contains("b")
}).thenAccept(servers -> {
    servers.forEach(server -> {
        System.out.println("Server " + server.getName() + ":" + server.getIdentifier() + " has a 'b' in their identifier!");
    });
}).join();
```


# Example - Using PterodactylClient

### Changing a server's power signal

```java
// Starting a server
client.startServer(server).join();

// Stopping a server
client.startServer(server).join();

// Restarting a server
client.startServer(server).join();

// Killing a server
client.startServer(server).join();
```

### Getting a server's status / Resource usage
```java
client.getResourceUsage(server).thenAccept(usage -> {
    System.out.println("Server status > " + usage.getStatus());
    System.out.println("RAM usage > " + usage.getMemoryUsage() + " bytes");
    System.out.println("Get CPU usage > " + usage.getCpuUsage() + "%");
    System.out.println("Network (Inbound): " + usage.getBytesInbound() + " b/s");
    System.out.println("Network (Outbound): " + usage.getBytesOutbound() + " b/s");
}).join();
```

### Sending a console command
```java
client.sendConsoleCommand(server, "bc sup").thenAccept(val -> {
  if(val)
    System.out.println("Said 'sup' to the " + server.getName() + " server node!");
}).join();
```

Any further usages can be explored via the source code, or with your IDE :)
