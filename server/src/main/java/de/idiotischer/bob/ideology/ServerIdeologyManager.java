package de.idiotischer.bob.ideology;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import de.idiotischer.bob.Server;
import de.idiotischer.bob.SharedCore;

import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

public class ServerIdeologyManager {

    private final Set<Ideology> ideologies = new HashSet<>();

    public void reload() {
        ideologies.clear();

        var path = Server.getInstance().getScenarioSceneLoader().getCurrentScenario().getIdeologiesConfig();

        if(path == null) return;

        try (JsonReader reader = new JsonReader(Files.newBufferedReader(path))) {
            JsonElement root = SharedCore.GSON.fromJson(reader, JsonElement.class);

            root.getAsJsonObject().entrySet().forEach(entry -> {
                String abbreviation = entry.getKey();

                JsonObject element = entry.getValue().getAsJsonObject();

                String name = "no name";

                float stabilityGainBuff = 0;
                float politicalPowerGainBuff = 0;
                float justifyWarGoalBuff = 0;

                if(element.has("name")) name = element.get("name").getAsString();

                if(element.has("buffs")) {
                    JsonObject obj = element.getAsJsonObject("buffs");

                    if(obj.has("politicalPowerGainBuff")) politicalPowerGainBuff = obj.get("politicalPowerGainBuff").getAsFloat();
                    if(obj.has("stabilityGainBuff")) stabilityGainBuff = obj.get("stabilityGainBuff").getAsFloat();
                    if(obj.has("justifyWarGoalBuff")) justifyWarGoalBuff = obj.get("justifyWarGoalBuff").getAsFloat();
                }

                Ideology ideology = new Ideology(abbreviation,name, new Ideology.Buffs(stabilityGainBuff,politicalPowerGainBuff,justifyWarGoalBuff));

                ideologies.add(ideology);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Set<Ideology> getIdeologies() {
        return ideologies;
    }
}
