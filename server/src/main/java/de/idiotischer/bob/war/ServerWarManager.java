package de.idiotischer.bob.war;

import de.idiotischer.bob.Server;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.networking.packet.impl.pp.ReplyPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;
import de.idiotischer.bob.tile.Tile;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerWarManager {

    private final ExecutorService warExecutorService = Executors.newSingleThreadExecutor();

    private final Map<Country, Set<WarStatus>> activeWars = new HashMap<>();

    public void reload() {
        activeWars.clear();
    }

    public Set<WarStatus> getWars(Country c) {
        return activeWars.entrySet().stream()
                .filter(entry -> entry.getKey().getAbbreviation().equals(c.getAbbreviation()))
                .findFirst().map(Map.Entry::getValue).orElseGet(HashSet::new);
    }

    public boolean isAtWar(Country a, Country b) {
        return !Collections.disjoint(getWars(a), getWars(b));
    }

    public boolean fightsTogetherWith(Country one, Country two) {
        return getWars(one).stream().anyMatch(w -> w.alliesCalledIn().stream().anyMatch(ally -> ally.getAbbreviation().equals(two.getAbbreviation())));
    }

    private Set<WarStatus> getOrCreateWars(Country c) {
        return activeWars.entrySet().stream()
                .filter(entry -> entry.getKey().getAbbreviation().equals(c.getAbbreviation()))
                .findFirst().map(Map.Entry::getValue)
                .orElseGet(() -> {
                    Set<WarStatus> wars = new HashSet<>();
                    activeWars.put(c, wars);
                    return wars;
                });
    }

    public boolean declareWar(boolean callAllies, Tile declaredTile, Country controller, Country aggressor) {

        if (isAtWar(controller, aggressor)) {
            return false;
        }

        warExecutorService.submit(() -> {

            String name = controller.countryName() + "–" + aggressor.countryName() + " War";
            String abbr = controller.getAbbreviation() + "-" + aggressor.getAbbreviation();

            List<Country> enemies = new ArrayList<>();
            enemies.add(aggressor);

            List<Country> allies = new ArrayList<>();

            if (callAllies) {
                // TODO: add factions, puppets etc and than make them get a message on whether they want to join or not
            }

            WarStatus status = new WarStatus(name, abbr, enemies, allies);

            getOrCreateWars(controller).add(status);
            getOrCreateWars(aggressor).add(status);
        });

        Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().getClients(), new ReplyPacket(Type.START_WAR, aggressor.getAbbreviation() + ";" + controller.getAbbreviation()));

        return true;
    }
}