package de.idiotischer.bob.conference;

import de.idiotischer.bob.Server;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.country.CountryResolver;
import de.idiotischer.bob.tile.Tile;
import de.idiotischer.bob.tile.TileResolver;
import de.idiotischer.bob.util.UUIDUtil;
import de.idiotischer.bob.war.WarStatus;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class ServerConferenceManager {

    private final Set<PeaceConference> conferences = new HashSet<>();

    public void start(WarStatus status) {
        PeaceConference conf = PeaceConference.fromWar(Server.getInstance().getSharedCore(), UUIDUtil.getUnused(conferences.stream().map(PeaceConference::getUUID).collect(Collectors.toSet())), Server.getInstance().getServerSocket(), Server.getInstance().getCountryManager(), status);

        if(conf == null) return;

        conf.setEndHook(() -> {
            remove(conf);

            conf.getWinners().forEach(winner -> {
                Server.getInstance().getTroopManager().getForController(winner).forEach(troop -> {
                    if(!troop.getTile().getController().getAbbreviation().equals(winner.getAbbreviation())) {
                        var l = Server.getInstance().getCountryManager().getOwned(troop.getController());

                        Tile t = l.get(ThreadLocalRandom.current().nextInt(l.size()));

                        troop.setTile(t);
                    }
                });
            });

            conf.getDefeated().forEach(looser -> {
                Server.getInstance().getTroopManager().getForController(looser).forEach(troop -> {
                    if(!troop.getTile().getController().getAbbreviation().equals(looser.getAbbreviation())) {
                        var l = Server.getInstance().getCountryManager().getOwned(troop.getController());

                        Tile t = l.get(ThreadLocalRandom.current().nextInt(l.size()));

                        Server.getInstance().getTroopManager().setStationedTileForAll(troop, t);
                    }
                });
            });
        });

        this.conferences.add(conf);

        conf.getDefeated().forEach(c -> Server.getInstance().getCountryManager().getOwned(c).forEach(t -> t.setControllerForAll(Server.getInstance().getServerSocket().channels(), c)));

        conf.startConference();

    }

    public void remove(PeaceConference conference) {
        this.conferences.remove(conference);
    }

    public boolean anyActive() {
        return !this.conferences.isEmpty();
    }

    public PeaceConference.Demands readDemands(CountryResolver r1, TileResolver r, String s) {
        String[] parts = s.split(";");

        Country c = r1.byAbbreviation(parts[0]);

        UUID uuid = UUID.fromString(parts[1]);

        TakeTileType type = TakeTileType.valueOf(parts[2]);

        Set<Tile> tiles = Arrays.stream(parts, 3, parts.length).map(r::byAbbreviation).collect(Collectors.toSet());

        return new PeaceConference.Demands(c, type, tiles, uuid);
    }

    public PeaceConference getBy(UUID uuid) {
        return conferences.stream().filter(p -> p.getUUID().equals(uuid)).findFirst().orElse(null);
    }

    public void sendDemands(PeaceConference.Demands demands) {
        Country country = demands.country();
        Set<Tile> tiles = demands.tiles();
        TakeTileType type = demands.type();
        PeaceConference conf = getBy(demands.peaceId());

        tiles.forEach(tile -> conf.proposeTileTake(tile,country,type));
    }
}
