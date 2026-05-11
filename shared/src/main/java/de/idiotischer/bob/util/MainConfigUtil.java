package de.idiotischer.bob.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import de.idiotischer.bob.SharedCore;
import java.nio.file.Files;

public class MainConfigUtil {

    boolean isDebug = false;
    boolean replaceIfNotExisting = false;
    private boolean isCacheOnRegister = false;

    public MainConfigUtil() {
        reload();
    }

    public void reload() {
        boolean changed = false;

        try {
            JsonObject obj;

            if (Files.exists(FileUtil.getDefaultConfig())) {
                try (JsonReader reader = new JsonReader(
                        Files.newBufferedReader(FileUtil.getDefaultConfig())
                )) {
                    JsonElement root = SharedCore.GSON.fromJson(reader, JsonElement.class);

                    if (root != null && root.isJsonObject()) {
                        obj = root.getAsJsonObject();
                    } else {
                        obj = new JsonObject();
                        changed = true;
                    }
                }
            } else {
                obj = new JsonObject();
                changed = true;
            }

            if (obj.has("debug") && !obj.get("debug").isJsonNull()) {
                this.isDebug = obj.get("debug").getAsBoolean();
            } else {
                this.isDebug = false;
                obj.addProperty("debug", this.isDebug);
                changed = true;
            }

            if (obj.has("replaceIfNotExisting") && !obj.get("replaceIfNotExisting").isJsonNull()) {
                this.replaceIfNotExisting = obj.get("replaceIfNotExisting").getAsBoolean();
            } else {
                this.replaceIfNotExisting = false;
                obj.addProperty("replaceIfNotExisting", this.replaceIfNotExisting);
                changed = true;
            }

            if (obj.has("cacheOnRegister") && !obj.get("cacheOnRegister").isJsonNull()) {
                this.isCacheOnRegister = obj.get("cacheOnRegister").getAsBoolean();
            } else {
                this.isCacheOnRegister = true;
                obj.addProperty("cacheOnRegister", this.isCacheOnRegister);
                changed = true;
            }

            if (changed) {
                try (var writer = Files.newBufferedWriter(FileUtil.getDefaultConfig())) {
                    SharedCore.GSON.toJson(obj, writer);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isDebug() {
        return isDebug;
    }

    public boolean isCacheOnRegister() {
        return isCacheOnRegister;
    }
}
