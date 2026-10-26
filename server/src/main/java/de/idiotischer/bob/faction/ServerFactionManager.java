package de.idiotischer.bob.faction;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import de.idiotischer.bob.Server;
import de.idiotischer.bob.SharedCore;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.modifiers.Modifier;

import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

//TODO: countries should be able to be in multiple factions but only of different types
public class ServerFactionManager implements FactionResolver {

    private final Set<Faction> factions = new HashSet<>();

    private final Set<Faction.FactionRule> factionRules = new HashSet<>();

    public void reload() {
        factionRules.clear();
        factions.clear();

        Path p = Server.getInstance().getScenarioSceneLoader().getCurrentScenario().getFactionConfig();
        Path p1 = Server.getInstance().getScenarioSceneLoader().getCurrentScenario().getFactionRulesConfig();

        if(Files.exists(p1)) {
            try (JsonReader reader = new JsonReader(Files.newBufferedReader(p))) {

                JsonElement root = SharedCore.GSON.fromJson(reader, JsonElement.class);

                root.getAsJsonObject().entrySet().forEach(entry -> {
                    String id = entry.getKey();
                    JsonObject rulesElement = entry.getValue().getAsJsonObject();

                    String name = getAsStringSafe(rulesElement, "name");
                    String category = getAsStringSafe(rulesElement, "category"); //maybe make this rather be the getKey and id be another key so we do it like Modifiers

                    Set<Modifier> mods = new HashSet<>();

                    JsonElement modsElement = rulesElement.get("modifiers");

                    if(modsElement != null && modsElement.isJsonArray()) {
                        JsonArray array = modsElement.getAsJsonArray();

                        array.forEach(modifier -> mods.add(Server.getInstance().getModifierManager().by(modifier.getAsString())));
                    }

                    Faction.FactionRule rule = new Faction.FactionRule(id,name,category, mods);
                    factionRules.add(rule);
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if(Files.exists(p)) {
            try (JsonReader reader = new JsonReader(Files.newBufferedReader(p))) {

                JsonElement root = SharedCore.GSON.fromJson(reader, JsonElement.class);

                root.getAsJsonObject().entrySet().forEach(entry -> {
                    String abbreviation = entry.getKey();
                    JsonObject factionElement = entry.getValue().getAsJsonObject();

                    String ownerString = getAsStringSafe(factionElement, "owner");
                    Country owner = Server.getInstance().getCountryManager().byAbbreviation(ownerString);

                    if (owner == null) return;

                    String name = getAsStringSafe(factionElement, "name");

                    Set<String> members = new HashSet<>();

                    JsonElement membersElement = factionElement.get("members");

                    if (membersElement != null && membersElement.isJsonArray()) {
                        JsonArray membersArray = membersElement.getAsJsonArray();

                        membersArray.forEach(mem -> members.add(mem.getAsString())); //ig safety check before adding?
                    }

                    //... read rules
                    Set<Faction.FactionRule> rules = new HashSet<>();

                    JsonElement rulesElement = factionElement.get("rules");

                    if(rulesElement != null && rulesElement.isJsonArray()) {
                        JsonArray array = rulesElement.getAsJsonArray();

                        array.forEach(modifier -> rules.add(ruleBy(modifier.getAsString())));
                    }

                    Faction faction = new Faction(Server.getInstance().getSendTool(), Server.getInstance().getServerSocket(), null, abbreviation, name, owner.getAbbreviation(), members);

                    factions.add(faction);
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public Set<Faction> getFactions() {
        return factions;
    }

    public Set<Faction.FactionRule> getFactionRules() {
        return factionRules;
    }

    public void createFaction(Country c, String factionName) {
        int turn = 0;
        String abbreviation;
        do {
            abbreviation = c.getAbbreviation() + "-" + factionName + "-" + factions.size() + turn;

            turn++;
        } while (abbreviationExists(abbreviation));

        Faction faction = new Faction(Server.getInstance().getSendTool(), Server.getInstance().getServerSocket(), null, abbreviation, factionName, c.getAbbreviation(), new HashSet<>());

        factions.add(faction);
    }

    public boolean abbreviationExists(String abbr) {
        return factions.stream().anyMatch(f -> f.getAbbreviation().equals(abbr));
    }

    private String getAsStringSafe(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) return "";
        return el.getAsString();
    }

    @Override
    public Faction.FactionRule ruleBy(String abbreviation) {
        return factionRules.stream().filter(rule -> rule.id().equals(abbreviation)).findFirst().orElse(null);
    }

    @Override
    public Faction factionBy(String abbreviation) {
        return factions.stream().filter(faction -> faction.getAbbreviation().equals(abbreviation)).findFirst().orElse(null);
    }

    public Set<Country> getFactionMembers(Faction faction) {
        return faction.getMembers().stream().map(m -> Server.getInstance().getCountryManager().byAbbreviation(m)).collect(Collectors.toSet());
    }

    public Set<Faction> getFactionsFor(Country c) {
        return factions.stream().filter(fac -> fac.getMembers().contains(c.getAbbreviation())).collect(Collectors.toSet());
    }
}
