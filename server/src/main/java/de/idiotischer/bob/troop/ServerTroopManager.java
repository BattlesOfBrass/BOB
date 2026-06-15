package de.idiotischer.bob.troop;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import de.idiotischer.bob.Server;
import de.idiotischer.bob.SharedCore;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.networking.packet.impl.pp.ReplyPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;
import de.idiotischer.bob.tile.Tile;
import de.idiotischer.bob.util.UUIDUtil;

import java.awt.*;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

//gets its own thread prob (at least for movement)
public class ServerTroopManager {

    private final List<Troop> troops = new ArrayList<>();
    private final Map<UUID, TroopStack> troopStacks = new HashMap<>();

    public void reload() {
        troopStacks.clear();

        try (JsonReader reader = new JsonReader(Files.newBufferedReader(Server.getInstance().getScenarioSceneLoader().getCurrentScenario().getTroopConfig()))) {
            JsonElement root = SharedCore.GSON.fromJson(reader, JsonElement.class);

            root.getAsJsonObject().entrySet().forEach(entry -> {
                String name = entry.getKey();

                JsonObject countryElement = entry.getValue().getAsJsonObject();

                String template = "";
                String tileAbbr = "";
                String ownerAbbr = "";
                String controllerAbbr;
                int count = 1;

                if(countryElement.has("template") && !countryElement.get("template").isJsonNull()) {
                    template = countryElement.get("template").getAsString();
                }

                if(countryElement.has("owner") && !countryElement.get("owner").isJsonNull()) {
                    ownerAbbr = countryElement.get("owner").getAsString();
                }

                if(countryElement.has("controller") && !countryElement.get("controller").isJsonNull()) {
                    controllerAbbr = countryElement.get("controller").getAsString();
                } else controllerAbbr = ownerAbbr;

                if(countryElement.has("tile") && !countryElement.get("tile").isJsonNull()) {
                    tileAbbr = countryElement.get("tile").getAsString();
                }

                if(countryElement.has("count") && !countryElement.get("count").isJsonNull()) {
                    count = countryElement.get("count").getAsInt();
                }

                Tile tile = Server.getInstance().getTileManager().byAbbreviation(tileAbbr);

                Country owner = Server.getInstance().getCountryManager().byAbbreviation(ownerAbbr);

                if(tile != null && owner != null) {
                    Country controller = Server.getInstance().getCountryManager().byAbbreviation(controllerAbbr);

                    if(controller == null) controller = owner;

                    TroopStack stack = new TroopStack(name,tile,owner,controller,count,template);

                    UUID uuid = UUIDUtil.getUnused(troopStacks.keySet());

                    //troopStacks.remove(uuid); if the uuid weren't unique i'd need this here
                    troopStacks.putIfAbsent(uuid,stack);

                    if(Server.getInstance().isDebug())
                        System.out.println("Added troop stack with info:  owner: "
                                + stack.getOwner().getAbbreviation() + " controller: "
                                + stack.getController().getAbbreviation() + " tile: "
                                + stack.getTile().getAbbreviation()
                        );
                } else {
                    if(Server.getInstance().isDebug())
                        System.out.println("Couldn't add troop stack: " + name + " because the owner or tile couldn't be found!");
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public UUID getUuid(TroopStack troopStack) {
        var uuid = troopStacks.entrySet().stream().filter(entry -> entry.getValue() == troopStack).findFirst().get();

        return uuid.getKey();
    }

    public void addTroopStack(TroopStack troop) {
        troopStacks.putIfAbsent(UUIDUtil.getUnused(troopStacks.keySet()), troop);
    }

    public void addTroop(Troop troop) {
        troops.add(troop);

        updateStacks();
    }

    private void updateStacks() {
        troopStacks.clear();

        for (Troop troop : troops) {

            TroopStack stack = troopStacks.values().stream()
                    .filter(s ->
                            s.getTemplate().equals(troop.getTemplate()) &&
                                    s.getController().equals(troop.getController()) && s.getTile().equals(troop.getTile()))
                    .findFirst()
                    .orElseGet(() -> {
                        TroopStack newStack = new TroopStack(troop.getTile(),troop.getController(),new ArrayList<>());
                        troopStacks.put(UUIDUtil.getUnused(troopStacks.keySet()),newStack);
                        return newStack;
                    });

            stack.getTroops().add(troop);
        }
    }

    public List<TroopStack> getEnemy(Country country) {
        return List.of();
    }

    public List<TroopStack> getVisible(Country country) {
        return troopStacks.values().stream().filter(s -> s.getController() != null && s.getController().equals(country) && s.isVisible()).toList();
    }

    public List<TroopStack> getForController(Country country) {
        return troopStacks.values().stream().filter(s -> s.getController() != null && s.getController().equals(country)).toList();
    }

    public Map<UUID,TroopStack> getTroopStacks() {
        return troopStacks;
    }

    public List<Troop> getTroops() {
        return troops;
    }

    public TroopStack getTroop(UUID uuid) {
        return troopStacks.get(uuid);
    }

    public void removeTroops(Country country) {
        List<UUID> toRemove = troopStacks.entrySet().stream()
                .filter(e -> e.getValue().getController() != null
                        && e.getValue().getController().getAbbreviation()
                        .equals(country.getAbbreviation()))
                .map(Map.Entry::getKey)
                .toList();

        toRemove.forEach(this::removeTroop);
    }

    public void removeTroop(UUID uuid) {
        troopStacks.remove(uuid);

        Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().getClients(), new ReplyPacket(Type.TROOP_REMOVE, uuid.toString()));
    }
}
