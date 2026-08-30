package de.idiotischer.bob.listener;

import de.craftsblock.craftscore.event.EventHandler;
import de.craftsblock.craftscore.event.EventPriority;
import de.craftsblock.craftscore.event.ListenerAdapter;
import de.idiotischer.bob.BOB;
import de.idiotischer.bob.Server;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.networking.packet.PacketRegistry;
import de.idiotischer.bob.networking.packet.impl.*;
import de.idiotischer.bob.networking.packet.impl.pp.ReplyPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;
import de.idiotischer.bob.player.Player;
import de.idiotischer.bob.scenario.Scenario;
import de.idiotischer.bob.scenario.ScenarioManager;
import de.idiotischer.bob.state.State;
import de.idiotischer.bob.tile.Tile;
import de.idiotischer.bob.tile.TileManager;
import de.idiotischer.bob.troop.MoveStatus;
import de.idiotischer.bob.util.AddressUtil;
import de.idiotischer.bob.war.WarStatus;
import it.unimi.dsi.fastutil.Pair;

import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class PacketListener implements ListenerAdapter {

    @EventHandler(priority = EventPriority.LOWEST, ignoreWhenCancelled = true)
    public void onPacketReceive(PacketRegistry.PacketReceiveEvent event) {
        if(event.getPacket() instanceof PongPacket) {
            System.out.println("Pong received and cancelled at: " + System.nanoTime());
            //event.setCancelled(true);
        } else if(event.getPacket() instanceof ScenariosSyncPacket pack) {
            ScenarioManager manager = BOB.getInstance().getScenarioManager();

            if(manager == null) return;

            List<Scenario> scenarios = new ArrayList<>();

            pack.getScenarioPackets().forEach(scenarioPacket -> {
                Path scenarioDir = scenarioPacket.applyToDisk2(scenarioPacket.getAbbreviation());
                boolean server = false;

                Path baseScenarioDir = Paths.get(scenarioPacket.getAbbreviation());
                if(scenarioPacket.resolveFallbackScenarioDir(baseScenarioDir) == scenarioDir) {
                    server = true;
                }

                Scenario scenario = new Scenario(server, scenarioPacket.getAbbreviation(), scenarioPacket.getName(), scenarioDir);

                scenarios.add(scenario);
            });

            manager.refresh(scenarios);
        } else if(event.getPacket() instanceof ScenarioSyncPacket packet) {
            packet.applyToDisk();

            ScenarioManager manager = BOB.getInstance().getScenarioManager();

            if(manager == null) return;

            Scenario scenario = manager.getScenario(packet.getAbbreviation());

            if(scenario == null) manager.refreshAddNew(scenario);

            BOB.getInstance().getScenarioSceneLoader().completeSync(scenario);

            BOB.getInstance().getScenarioSceneLoader().load(scenario, false);
        } else if(event.getPacket() instanceof CountriesSyncPacket packet) {
            Set<Country> countries = packet.getPackets().stream().map(CountrySyncPacket::getCountry).collect(Collectors.toSet());

            countries.forEach(c -> BOB.getInstance().getCountryManager().registerCountry(c));

            BOB.getInstance().getCountryManager().finishReload();
        } else if(event.getPacket() instanceof TilesSyncPacket packet) {

            if(BOB.getInstance().getTileManager().getAwaitingFuture() == null
                    || BOB.getInstance().getTileManager().getAwaitingFuture().isCancelled()
                    || BOB.getInstance().getTileManager().getAwaitingFuture().isDone()
            ) return;

            List<TileSyncPacket> packs = packet.getPackets();

            packs.forEach(s -> s.reconstruct(BOB.getInstance().getSharedCore(), BOB.getInstance().getCountryManager()));

            Set<Tile> tiles = packs.stream().map(TileSyncPacket::getTile).collect(Collectors.toSet());

            tiles.forEach(s -> {
                BOB.getInstance().getTileManager().registerTile(s);
            });

            BOB.getInstance().getTileManager().finishReload();
        } else if(event.getPacket() instanceof StatesSyncPacket packet) {

            if(BOB.getInstance().getStateManager().getAwaitingFuture() == null
                    || BOB.getInstance().getStateManager().getAwaitingFuture().isCancelled()
                    || BOB.getInstance().getStateManager().getAwaitingFuture().isDone()
            ) return;

            List<StateSyncPacket> packs = packet.getPackets();

            Set<State> states = packs.stream().map(s -> s.getState(BOB.getInstance().getCountryManager(), BOB.getInstance().getTileManager())).collect(Collectors.toSet());

            states.forEach(s -> {
                BOB.getInstance().getStateManager().registerState(s);
            });

            BOB.getInstance().getStateManager().finishReload(true);
        } else if(event.getPacket() instanceof CountrySyncPacket packet) {
            Country country = packet.getCountry();

            if(BOB.getInstance().getCountryManager().has(country)) return;

            BOB.getInstance().getCountryManager().registerCountry(country);

        } else if(event.getPacket() instanceof TileSyncPacket packet) {
            Tile tile = packet.getTile();

            if(BOB.getInstance().getTileManager().has(tile)) return;

            BOB.getInstance().getTileManager().registerTile(tile);
        } else if(event.getPacket() instanceof ReplyPacket pack) {
            switch (pack.getReplyType()) {
                case CLEAR_COMBATS -> {
                    BOB.getInstance().getCombatManager().clear();
                }
                case COMBAT_OVER -> {
                    String uuidString = pack.getMessage();

                    UUID uuid;

                    try {
                        uuid = UUID.fromString(uuidString);
                    } catch (IllegalArgumentException e) {
                        return;
                    }

                    BOB.getInstance().getCombatManager().remove(uuid);
                }
                case TROOP_REMOVE -> {
                    String uuidString = pack.getMessage();

                    UUID uuid;

                    try {
                        uuid = UUID.fromString(uuidString);
                    } catch (IllegalArgumentException e) {
                        return;
                    }

                    BOB.getInstance().getTroopManager().removeTroopStack(uuid);
                }
                case CAPITULATE_COUNTRY -> {
                    String[] parts = pack.getMessage().split(";");

                    String countryAbbr = parts[0];
                    boolean capped = Boolean.parseBoolean(parts[1]);

                    Country country = BOB.getInstance().getCountryManager().byAbbreviation(countryAbbr);

                    if(country == null) return;

                    country.setCapitulated(capped);

                    //TODO: show popup that a country capped
                }
                case END_WAR -> {
                    WarStatus status = WarStatus.fromString(pack.getMessage(), BOB.getInstance().getCountryManager());

                    status.getAttackers().forEach(c -> BOB.getInstance().getWarManager().endWar(c.getAbbreviation(),status));
                    status.getDefenders().forEach(c -> BOB.getInstance().getWarManager().endWar(c.getAbbreviation(),status));

                    //TODO: show popup that a war ended and start peace conference (oh gosh i need to code that)
                }
                case START_WAR -> {
                    WarStatus status = WarStatus.fromString(pack.getMessage(), BOB.getInstance().getCountryManager());

                    status.getAttackers().forEach(c -> {
                        BOB.getInstance().getWarManager().addWar(c.getAbbreviation(), status);
                    });

                    status.getDefenders().forEach(c -> {
                        BOB.getInstance().getWarManager().addWar(c.getAbbreviation(), status);
                    });
                    //TODO: show popup that a war happened
                }
                case WARS_SYNC -> {
                    BOB.getInstance().getWarManager().finishReload(pack.getMessage());
                }
                case TROOPS_MOVE -> {
                    String[] parts = pack.getMessage().split(";");

                    String[] troopPart = parts[0].split("=");
                    String[] tilePart = parts[1].split("=");
                    String[] statusPart = parts[2].split("=");

                    Tile tile = BOB.getInstance().getTileManager().byAbbreviation(tilePart[1]);

                    if (troopPart[0].equals("troop")) {
                        UUID uuid = UUID.fromString(troopPart[1]);

                        MoveStatus status =
                                MoveStatus.values()[Integer.parseInt(statusPart[1])];

                        BOB.getInstance().getTroopManager().finishMove(uuid, tile, status);
                    }

                    else if (troopPart[0].equals("troops")) {

                        String[] uuids = troopPart[1].split(",");
                        String[] statuses = statusPart[1].split(",");

                        Map<UUID, MoveStatus> results = new HashMap<>();

                        for (int i = 0; i < uuids.length; i++) {
                            UUID uuid = UUID.fromString(uuids[i]);
                            MoveStatus status = MoveStatus.values()[Integer.parseInt(statuses[i])];

                            results.put(uuid, status);
                        }

                        BOB.getInstance().getTroopManager().finishMoveAll(results, tile);
                    }
                }
                case TILE_CHANGE -> {
                    String s = pack.getMessage();

                    if(s.isEmpty()) return;

                    Pair<Tile, Country> pair = Tile.deconstructChange(s, Server.getInstance().getCountryManager(), Server.getInstance().getTileManager());

                    Tile tile = pair.key();

                    if(tile == null) return;

                    Country country = pair.value();

                    if(country == null) return;

                    Color c = country.countryColor() == null ? Color.WHITE : country.countryColor() ;

                    tile.setControllerFinish(country, BOB.getInstance().isDebug());
                    TileManager.recolorTile(tile, c);
                }
                case PLAYER_CHANGE -> {

                    String[] parts = pack.getMessage().split(";");

                    String uuid = parts[0];
                    String abbreviation = parts[1];
                    String status = parts[2];

                    if(!Boolean.parseBoolean(status)) return;

                    Country country = Server.getInstance().getCountryManager().byAbbreviation(abbreviation);

                    if(country == null) {
                        return;
                    }

                    Player p = Server.getInstance().getPlayerManager().getPlayer(AddressUtil.getRemoteAddress(event.getChannel()));

                    if(p == null) {
                        return;
                    }

                    p.country(country);
                }
                case ERROR -> {}
            }
        } else if(event.getPacket() instanceof PlayerAuthUpdatePacket pack) {
            if(!pack.isAuthed()) return;

            BOB.getInstance().setPlayer(pack.getUuid());
            BOB.getInstance().getPlayerManager().addPlayer(BOB.getInstance().getPlayer());
        }
        else if(event.getPacket() instanceof PlayerJoinPacket pack) {
            //System.out.println(
            //        "join packet " +
            //                pack.getAddress() +
            //                " " +
            //                pack.getUuid()
            //);

            if(pack.getAddress() == AddressUtil.getThisAddress(event.getChannel())) {
                BOB.getInstance().getPlayerManager().getPlayer(pack.getAddress()).uuid(pack.getUuid());
            }

            if(BOB.getInstance().getPlayerManager().hasPlayer(pack.getUuid())) {
                System.out.println(
                        "UUID already good " +
                                BOB.getInstance().getPlayerManager().getPlayer(pack.getAddress()).uuid() +
                                " " +
                                pack.getUuid()
                );
                return;
            }

            if(BOB.getInstance().getPlayerManager().hasPlayer(pack.getAddress())) {
                //für local sync
                System.out.println(
                        "UUID changed " +
                                BOB.getInstance().getPlayerManager().getPlayer(pack.getAddress()).uuid() +
                                " " +
                                pack.getUuid()
                );

                BOB.getInstance().getPlayerManager().getPlayer(pack.getAddress()).uuid(pack.getUuid());
            }

            Player p = BOB.getInstance().getPlayerManager().createPlayer(event.getChannel(), pack.getUuid(), pack.getAddress());

            BOB.getInstance().getPlayerManager().addPlayer(p);
        } else if(event.getPacket() instanceof PlayerQuitPacket pack) {
            BOB.getInstance().getPlayerManager().removePlayer(pack.getUuid());
        } else if (event.getPacket() instanceof PlayerChangedCountryPacket pack) {
            UUID uuid = pack.getUuid();
            String abbreviation = pack.getCountryAbbreviation();

            Player player = BOB.getInstance().getPlayerManager().getPlayer(uuid);
            Country country = BOB.getInstance().getCountryManager().byAbbreviation(abbreviation);

            if(player == null || country == null) return;

            player.country(country);
        } else if(event.getPacket() instanceof TroopStackSyncPacket pack) {
            var z = pack.getTroopStack(BOB.getInstance().getCountryManager(), BOB.getInstance().getTileManager());
            BOB.getInstance().getTroopManager().addTroopStack(z.key(), z.value());
        } else if(event.getPacket() instanceof TroopStacksSyncPacket pack) {
            BOB.getInstance().getTroopManager().finishReload(pack.getPackets().stream().map(d -> {
                var pair = d.getTroopStack(BOB.getInstance().getCountryManager(), BOB.getInstance().getTileManager());
                return Map.entry(pair.key(), pair.value());
            }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)), true);
        } else if(event.getPacket() instanceof CombatSyncPacket pack) {
            BOB.getInstance().getCombatManager().addStatus(pack.getStatus(BOB.getInstance().getTroopManager()));
        } else if(event.getPacket() instanceof CombatsSyncPacket pack) {
            BOB.getInstance().getCombatManager().addStatus(pack.getPackets().stream().map(p -> p.getStatus(BOB.getInstance().getTroopManager())).toList());
        }
    }
}