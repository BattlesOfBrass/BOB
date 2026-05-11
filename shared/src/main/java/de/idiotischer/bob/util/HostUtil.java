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
    private boolean coop = false;

    public HostUtil() {
        reload();
    }

    public void reload() {
        boolean changed = false;

        try {
            JsonObject root;

            if (Files.exists(FileUtil.getHostConfig())) {
                try (JsonReader reader = new JsonReader(
                        Files.newBufferedReader(FileUtil.getHostConfig())
                )) {
                    JsonElement parsed = SharedCore.GSON.fromJson(reader, JsonElement.class);

                    if (parsed != null && parsed.isJsonObject()) {
                        root = parsed.getAsJsonObject();
                    } else {
                        root = new JsonObject();
                        changed = true;
                    }
                }
            } else {
                root = new JsonObject();
                changed = true;
            }

            JsonObject local = root.has("local") && root.get("local").isJsonObject() ? root.getAsJsonObject("local") : new JsonObject();git 
            JsonObject remote = root.has("remote") && root.get("remote").isJsonObject() ? root.getAsJsonObject("remote") : new JsonObject();

            if (!root.has("local")) {
                root.add("local", local);
                changed = true;
            }

            if (!root.has("remote")) {
                root.add("remote", remote);
                changed = true;
            }

            if (remote.has("remotePort") && !remote.get("remotePort").isJsonNull()) {
                remotePort = remote.get("remotePort").getAsInt();
            } else {
                remotePort = 2776;
                remote.addProperty("remotePort", remotePort);
                changed = true;
            }

            if (remote.has("remoteHost") && !remote.get("remoteHost").isJsonNull()) {
                host = remote.get("remoteHost").getAsString();
            } else {
                host = "localhost";
                remote.addProperty("remoteHost", host);
                changed = true;
            }

            if (remote.has("coop") && !remote.get("coop").isJsonNull()) {
                coop = remote.get("coop").getAsBoolean();
            } else {
                coop = false;
                remote.addProperty("coop", coop);
                changed = true;
            }

            if (local.has("localPort") && !local.get("localPort").isJsonNull()) {
                localPort = local.get("localPort").getAsInt();
            } else {
                localPort = 3995;
                local.addProperty("localPort", localPort);
                changed = true;
            }

            if (local.has("useSpecifications") && !local.get("useSpecifications").isJsonNull()) {
                useSpecifications = local.get("useSpecifications").getAsBoolean();
            } else {
                useSpecifications = false;
                local.addProperty("useSpecifications", useSpecifications);
                changed = true;
            }

            if (local.has("multiplayerEnabled") && !local.get("multiplayerEnabled").isJsonNull()) {
                multiplayerEnabled = local.get("multiplayerEnabled").getAsBoolean();
            } else {
                multiplayerEnabled = false;
                local.addProperty("multiplayerEnabled", multiplayerEnabled);
                changed = true;
            }

            if (changed) {
                try (var writer = Files.newBufferedWriter(FileUtil.getHostConfig())) {
                    SharedCore.GSON.toJson(root, writer);
                }
            }

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

    public boolean isCoop() {
        return coop;
    }
}
