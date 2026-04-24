package de.idiotischer.bob.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import de.idiotischer.bob.SharedCore;

import java.nio.file.Files;

public class HostUtil {

    private int localPort = 3995;
    private int remotePort = 2776;
    private String host = "localhost";

    private boolean useSpecifications = false;
    private boolean multiplayerEnabled = false;

    public HostUtil() {
        reload();
    }

    public void reload() {
        try (JsonReader reader = new JsonReader(Files.newBufferedReader(FileUtil.getHostConfig()))) {
            JsonElement root = SharedCore.GSON.fromJson(reader, JsonElement.class);

            root.getAsJsonObject().entrySet().forEach(entry -> {
                JsonObject obj = entry.getValue().getAsJsonObject();

                if (entry.getKey().equals("remote")) {
                    if (obj.has("remotePort") && !obj.get("remotePort").isJsonNull()) {
                        remotePort = obj.get("remotePort").getAsInt();
                    }
                    if (obj.has("remoteHost") && !obj.get("remoteHost").isJsonNull()) {
                        host = obj.get("remoteHost").getAsString();
                    }
                }

                if (entry.getKey().equals("local")) {
                    if (obj.has("localPort") && !obj.get("localPort").isJsonNull()) {
                        localPort = obj.get("localPort").getAsInt();
                    }
                    if (obj.has("useSpecifications") && !obj.get("useSpecifications").isJsonNull()) {
                        useSpecifications = obj.get("useSpecifications").getAsBoolean();
                    }
                    if (obj.has("multiplayerEnabled") && !obj.get("multiplayerEnabled").isJsonNull()) {
                        multiplayerEnabled = obj.get("multiplayerEnabled").getAsBoolean();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getHost() {
        return host;
    }

    public boolean isUseSpecifications() {
        return useSpecifications;
    }

    public boolean isMultiplayerEnabled() {
        return multiplayerEnabled;
    }

    public int getLocalPort() {
        return localPort;
    }

    public int getRemotePort() {
        return remotePort;
    }
}
