package de.idiotischer.bob.state;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import de.idiotischer.bob.Server;
import de.idiotischer.bob.SharedCore;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.networking.packet.impl.StatesSyncPacket;
import de.idiotischer.bob.networking.packet.impl.TilesSyncPacket;
import de.idiotischer.bob.tile.Tile;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ServerStateManager implements StateResolver {

    private final Set<State> stateSet = new HashSet<>();

    public void reload() {
        stateSet.clear();

        try (JsonReader reader = new JsonReader(Files.newBufferedReader(Server.getInstance().getScenarioSceneLoader().getCurrentScenario().getStatesConfig()))) {
            JsonElement root = SharedCore.GSON.fromJson(reader, JsonElement.class);

            root.getAsJsonObject().entrySet().forEach(entry -> {
                String stateAbbreviation = entry.getKey();

                JsonObject stateElement = entry.getValue().getAsJsonObject();

                String name = stateAbbreviation;
                if (stateElement.has("name") && !stateElement.get("name").isJsonNull()) {
                    name = stateElement.get("name").getAsString();
                }

                String ownerS = "";
                if (stateElement.has("owner") && !stateElement.get("owner").isJsonNull()) {
                    ownerS = stateElement.get("owner").getAsString();
                }

                Country owner = Server.getInstance().getCountryManager().getCountry(ownerS);

                List<Tile> tiles = new ArrayList<>();
                JsonElement tilesElement = stateElement.get("tiles");

                if (tilesElement != null && tilesElement.isJsonArray()) {
                    for (JsonElement el : tilesElement.getAsJsonArray()) {
                        if (!el.isJsonNull()) {
                            String tile = el.getAsString();
                            Tile resolvedTile = Server.getInstance().getTileManager().byAbbreviation(tile);
                            if (resolvedTile != null) tiles.add(resolvedTile);
                        }
                    }
                }
                else if (stateElement.has("tile") && !stateElement.get("tile").isJsonNull()) {
                    String tile = stateElement.get("tile").getAsString();
                    Tile resolvedTile = Server.getInstance().getTileManager().byAbbreviation(tile);
                    if (resolvedTile != null) tiles.add(resolvedTile);
                }
                else {
                    Tile resolvedTile = Server.getInstance().getTileManager().byAbbreviation(stateAbbreviation);
                    if (resolvedTile != null) tiles.add(resolvedTile);
                }

                State state = new State(owner, tiles, stateAbbreviation, name);

                stateSet.add(state);

                if(owner != null) {
                    owner.addState(state);
                }

                if(Server.getInstance().isDebug()) System.out.println("Added state: " + state.serialize());
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

        //Server.getInstance().getSendTool().broadcast(
        //        Server.getInstance().getServerSocket().getClients(),
        //        StatesSyncPacket.fromStates(stateSet)
        //);
    }

    public Set<State> getStateSet() {
        return stateSet;
    }

    @Override
    public State resolve(String abbreviation) {
        return stateSet.stream().filter(s -> s.abbreviation().equals(abbreviation)).findFirst().orElse(null);
    }
}
