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

# Example

## Get a list of your servers
```java
application.getServers().thenAccept(serverList -> {
  serverList.forEach(server -> {
    System.out.println("Server Name: " + server.getName() + " - Server ID: " + server.getIdentifier());
  }); 
}).join();
```

## Start/Stop a multiple servers with similar names
```java
application.getServersByFilter((server) -> server.getName().contains("BEDWARS"))
  .thenAccept(servers -> {
    servers.forEach(client::stopServer);
  });
System.out.println("Stopped all BedWars servers!");

application.getServersByFilter((server) -> server.getName().contains("BEDWARS"))
  .thenAccept(servers -> {
    servers.forEach(client::startServer);
  });
System.out.println("Started all BedWars servers!");
```

Any further usages can be explored via the source code, or with your IDE :)
