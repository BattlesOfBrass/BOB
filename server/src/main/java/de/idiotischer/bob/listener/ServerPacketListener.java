package de.idiotischer.bob.listener;

import de.craftsblock.craftscore.event.EventHandler;
import de.craftsblock.craftscore.event.ListenerAdapter;
import de.idiotischer.bob.Server;
import de.idiotischer.bob.conference.PeaceConference;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.networking.packet.PacketRegistry;
import de.idiotischer.bob.networking.packet.impl.*;
import de.idiotischer.bob.networking.packet.impl.pp.ReplyPacket;
import de.idiotischer.bob.networking.packet.impl.pp.RequestPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;
import de.idiotischer.bob.player.Player;
import de.idiotischer.bob.scenario.Scenario;
import de.idiotischer.bob.scenario.ServerScenarioManager;
import de.idiotischer.bob.scenario.ServerScenarioSceneLoader;
import de.idiotischer.bob.tile.Tile;
import de.idiotischer.bob.tile.event.TileChangedEvent;
import de.idiotischer.bob.troop.MoveStatus;
import de.idiotischer.bob.troop.TroopStack;
import de.idiotischer.bob.troop.TroopValidator;
import de.idiotischer.bob.util.AddressUtil;
import it.unimi.dsi.fastutil.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ServerPacketListener implements ListenerAdapter {
    @EventHandler
    public void onPacketReceive(PacketRegistry.PacketReceiveEvent event) {
        Player player = Server.getInstance().getPlayerManager().resolve(event.getChannel());

        if(player != null && !player.authorized()) {
            if(event.getPacket() instanceof LoginPacket pack) {
                Server.getInstance().getPlayerManager().authPlayer(player, pack.getCredentials());
            } else {
                try {
                    event.getChannel().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if(event.getPacket() instanceof PingPacket) {
            System.out.println("Ping packet received at: " + System.nanoTime());
            Server.getInstance().getSendTool().send(event.getChannel(), new PongPacket());

        } else if(event.getPacket() instanceof RequestPacket pack) {
            //so ping pong like
            switch (pack.getRequestType()) {
                case SEND_DEMANDS -> {
                    PeaceConference.Demands demands = Server.getInstance().getConferenceManager().readDemands(Server.getInstance().getCountryManager(), Server.getInstance().getTileManager(), pack.getMessage());

                    Server.getInstance().getConferenceManager().sendDemands(demands);
                    Server.getInstance().getConferenceManager().getBy(demands.peaceId()).addFinished(demands.country());
                }
                case END_CONFERENCE -> {
                    UUID id = UUID.fromString(pack.getMessage());

                    if(player == null) return;

                    Country c = player.country();

                    if(c == null) return;

                    Server.getInstance().getConferenceManager().getBy(id).addEnded(c);
                }
                case SPAWN_TROOP -> {
                    String[] strings = pack.getMessage().split(";");

                    String tileString = strings[0];
                    String ownerString = strings[1];
                    int count = Integer.parseInt(strings[2]);

                    Tile tile = Server.getInstance().getTileManager().byAbbreviation(tileString);
                    Country owner = Server.getInstance().getCountryManager().byAbbreviation(ownerString);

                    if(tile == null || owner == null) return;

                    TroopStack stack = new TroopStack("dojfsnsdoi", tile,owner,owner,count, "none");

                    Server.getInstance().getTroopManager().addTroopStack(stack);
                }
                case WARS_SYNC -> {
                    Server.getInstance().getSendTool().send(event.getChannel(), new ReplyPacket(Type.WARS_SYNC, Server.getInstance().getWarManager().serializeWars()));
                }
                case START_WAR -> {
                    String[] parts = pack.getMessage().split(";");

                    String abbr = parts[0];
                    String country = parts[1]; //in the case that the client should be desynced i just send the right country with it
                    boolean callAllies = Boolean.parseBoolean(parts[2]);

                    Tile declaredTile = Server.getInstance().getTileManager().byAbbreviation(abbr);

                    if(declaredTile == null) return;

                    Country controller = Server.getInstance().getCountryManager().byAbbreviation(country);

                    if(controller == null) return;

                    Player p = Server.getInstance().getPlayerManager().resolve(event.getChannel());

                    if(p == null) return;

                    Server.getInstance().getWarManager().declareWar(callAllies, declaredTile, controller, p.country());

                }
                case TROOPS_MOVE -> {
                    String[] parts = pack.getMessage().split(";");

                    String[] firstPart = parts[0].split("=");
                    String[] tilePart = parts[1].split("=");

                    Tile tile = Server.getInstance().getTileManager().byAbbreviation(tilePart[1]);
                    if (tile == null) return;

                    Player p = Server.getInstance().getPlayerManager().resolve(event.getChannel());

                    if (firstPart[0].equals("troop")) {
                        UUID uuid = UUID.fromString(firstPart[1]);

                        TroopStack troopStack = Server.getInstance().getTroopManager().getTroop(uuid);
                        if (troopStack == null) return;

                        MoveStatus moveStatus = TroopValidator.validate(p, troopStack, tile, Server.getInstance().getTroopManager(), Server.getInstance().getTileManager());

                        String reply = "troop=" + uuid + ";tile=" + tile.getAbbreviation() + ";type=" + moveStatus.ordinal();

                        if (moveStatus == MoveStatus.FAILURE_FIGHT || moveStatus == MoveStatus.FAILURE_NO_CONTROL || moveStatus == MoveStatus.FAILURE || moveStatus == MoveStatus.FAILURE_KICKED || moveStatus == MoveStatus.FAILURE_IN_COMBAT || moveStatus == MoveStatus.FAILURE_STARTED_PATHFINDING) return;

                        Server.getInstance().getTroopManager().removePathfinding(troopStack);
                        troopStack.setTile(tile);

                        Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().getClients(), new ReplyPacket(Type.TROOPS_MOVE, reply));
                    } else if (firstPart[0].equals("troops")) {
                        List<String> statuses = new ArrayList<>();

                        for (String uuidString : firstPart[1].split(",")) {
                            UUID uuid = UUID.fromString(uuidString);

                            TroopStack troopStack = Server.getInstance().getTroopManager().getTroop(uuid);

                            MoveStatus moveStatus;

                            if (troopStack == null) {
                                moveStatus = MoveStatus.FAILURE;
                            } else {
                                moveStatus = TroopValidator.validate(p, troopStack, tile, Server.getInstance().getTroopManager(), Server.getInstance().getTileManager());

                                if (moveStatus != MoveStatus.FAILURE_FIGHT && moveStatus != MoveStatus.FAILURE_NO_CONTROL && moveStatus != MoveStatus.FAILURE
                                        && moveStatus != MoveStatus.FAILURE_KICKED && moveStatus != MoveStatus.FAILURE_IN_COMBAT && moveStatus != MoveStatus.FAILURE_STARTED_PATHFINDING) {
                                    Server.getInstance().getTroopManager().removePathfinding(troopStack);
                                    troopStack.setTile(tile);
                                }
                            }

                            statuses.add(String.valueOf(moveStatus.ordinal()));
                        }

                        String reply = "troops=" + firstPart[1] + ";tile=" + tile.getAbbreviation() + ";types=" + String.join(",", statuses);

                        Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().getClients(), new ReplyPacket(Type.TROOPS_MOVE, reply));
                    }
                }case PLAYER_CHANGE -> {
                    //System.out.println("Player change packet received at: " + System.nanoTime());
                    //System.out.println(pack.getMessage());
                    //Server.getInstance().getPlayerManager().getPlayers().forEach(p -> System.out.println(p.uuid().toString()));

                    String[] parts = pack.getMessage().split(";");

                    String uuid = parts[0];
                    String abbreviation = parts[1];

                    Country country = Server.getInstance().getCountryManager().byAbbreviation(abbreviation);

                    if(country == null) {
                        Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().getClients(),
                                new ReplyPacket(Type.PLAYER_CHANGE, pack.getMessage() + ";type=false"));
                        return;
                    }

                    Player p = Server.getInstance().getPlayerManager().getPlayer(AddressUtil.getRemoteAddress(event.getChannel()));

                    if(p == null) {
                        Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().getClients(),
                                new ReplyPacket(Type.PLAYER_CHANGE, pack.getMessage() + ";type=false"));
                        return;
                    }

                    p.country(country);

                    Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().getClients(),
                            new ReplyPacket(Type.PLAYER_CHANGE, pack.getMessage() + ";type=true"));
                }
                case TROOPS_SYNC -> {
                    Server.getInstance().getSendTool().send(event.getChannel(), TroopStacksSyncPacket.fromStates(Server.getInstance().getTroopManager().getTroopStacks()));
                }
                case SCENARIOS -> {
                    if(!Server.getInstance().getServerSocket().isLocal()) return; // only sync 1 scenario for remote not all like on the client

                    Server.getInstance().getSendTool().send(event.getChannel(), ScenariosSyncPacket.fromScenarios(Server.getInstance().getScenarioManager().getScenarios()));
                }
                case TILES_SYNC -> {
                    TilesSyncPacket syncPacket = TilesSyncPacket.fromTiles(Server.getInstance().getTileManager().getTileSet());

                    Server.getInstance().getSendTool().send(event.getChannel(), syncPacket);
                }
                case STATES_SYNC -> {
                    StatesSyncPacket syncPacket = StatesSyncPacket.fromStates(Server.getInstance().getStateManager().getStateSet());

                    Server.getInstance().getSendTool().send(event.getChannel(), syncPacket);
                }
                case STATE_SYNC -> {}
                case COUNTRIES_SYNC -> {
                    CountriesSyncPacket syncPacket = CountriesSyncPacket.fromCountries(Server.getInstance().getCountryManager().getCountries());

                    Server.getInstance().getSendTool().send(event.getChannel(), syncPacket);
                }
                case SCENARIO_SYNC -> {
                    ServerScenarioSceneLoader loader = Server.getInstance().getScenarioSceneLoader();

                    if(loader == null) return;

                    Scenario scenario = loader.getCurrentScenario();

                    Server.getInstance().getSendTool().send(event.getChannel(), new ScenarioSyncPacket(scenario));
                }
                case SCENARIO_LOAD ->  {
                    ServerScenarioManager manager = Server.getInstance().getScenarioManager();

                    if(manager == null) return;

                    Scenario scenario = manager.getScenario(pack.getMessage());

                    if(scenario == null) return;

                    Server.getInstance().getScenarioSceneLoader().loadNew(scenario);
                }
                case TILE_CHANGE -> {
                    String s = pack.getMessage();

                    if(s.isEmpty()) return;

                    Pair<Tile, Country> pair = Tile.deconstructChange(s, Server.getInstance().getCountryManager(), Server.getInstance().getTileManager());

                    Tile tile = pair.key();

                    if(tile == null) return;

                    Country country = pair.value();

                    if(country == null) return;

                    if(Server.getInstance().getTileValidator().isChangeValid(tile, tile.getController(), country)) {
                        if(Objects.equals(Tile.getChangeType(s), TileChangedEvent.Type.OWNER)) {
                            tile.setOwnerForAll(Server.getInstance().getServerSocket().getClients(), country);
                            return;
                        }

                        Country oldController = tile.getController();

                        tile.setControllerForAll(Server.getInstance().getServerSocket().getClients(), country);
                        Server.getInstance().getWarManager().checkWarOver(country, oldController, tile);
                    }
                }
            }
        }
    }

}

