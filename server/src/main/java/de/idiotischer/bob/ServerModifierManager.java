package de.idiotischer.bob;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import de.idiotischer.bob.faction.Faction;
import de.idiotischer.bob.modifiers.Modifier;
import de.idiotischer.bob.modifiers.ModifierResolver;

import javax.print.DocFlavor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class ServerModifierManager implements ModifierResolver {

    private final Set<Modifier> modifiers = new HashSet<>();

    public void reload() {
        modifiers.clear();

        Path p = Server.getInstance().getScenarioSceneLoader().getCurrentScenario().getModifierConfig();

        if(Files.exists(p)) {
            try (JsonReader reader = new JsonReader(Files.newBufferedReader(p))) {

                JsonElement root = SharedCore.GSON.fromJson(reader, JsonElement.class);

                root.getAsJsonObject().entrySet().forEach(entry -> {
                    String type = entry.getKey();
                    JsonObject typeElement = entry.getValue().getAsJsonObject();

                    typeElement.entrySet().forEach(ent -> {
                        String abbr = ent.getKey();
                        JsonObject modifierElement = ent.getValue().getAsJsonObject();

                        String name = getAsStringSafe(modifierElement, "name");

                        float modifierF = 0;
                        if(modifierElement.has("modifier")) modifierF = modifierElement.get("modifier").getAsFloat();

                        Modifier modifier;

                        if(modifierElement.has("specificFor")) modifier = new Modifier(Server.getInstance().getServerSocket(), Server.getInstance().getSendTool(), abbr, name, modifierElement.get("specificFor").getAsString(), modifierF, type);
                        else modifier = new Modifier(Server.getInstance().getServerSocket(), Server.getInstance().getSendTool(), abbr, name, modifierF, type);

                        modifiers.add(modifier);
                    });
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean idExists(String id) {
        return modifiers.stream().anyMatch(f -> f.getId().equals(id));
    }

    public Set<Modifier> getModifiers() {
        return modifiers;
    }

    private String getAsStringSafe(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) return "";
        return el.getAsString();
    }

    @Override
    public Modifier by(String id) {
        return modifiers.stream().filter(mod -> mod.getId().equals(id)).findFirst().orElse(null);
    }
}
