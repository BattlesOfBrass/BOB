package de.idiotischer.bob.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import de.idiotischer.bob.SharedCore;
import java.nio.file.Files;

public class MainConfigUtil {

    boolean isDebug = false;
    boolean replaceIfNotExisting = false;

    public MainConfigUtil() {
        reload();
    }

    public void reload() {
        try (
            JsonReader reader = new JsonReader(
                Files.newBufferedReader(FileUtil.getDefaultConfig())
            )
        ) {
            JsonElement root = SharedCore.GSON.fromJson(
                reader,
                JsonElement.class
            );

            if (root == null || !root.isJsonObject()) {
                throw new IllegalStateException(
                    "Config root is not a JSON object!"
                );
            }

            JsonObject obj = root.getAsJsonObject();

            if (
                obj.has("replaceIfNotExisting") &&
                !obj.get("replaceIfNotExisting").isJsonNull()
            ) {
                this.replaceIfNotExisting = obj
                    .get("replaceIfNotExisting")
                    .getAsBoolean();
            } else {
                this.replaceIfNotExisting = false;
            }

            if (obj.has("debug") && !obj.get("debug").isJsonNull()) {
                this.isDebug = obj.get("debug").getAsBoolean();
            } else {
                this.isDebug = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isDebug() {
        return isDebug;
    }
}
