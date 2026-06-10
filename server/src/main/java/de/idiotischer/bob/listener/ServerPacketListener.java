package de.idiotischer.bob.listener;

import de.craftsblock.craftscore.event.EventHandler;
import de.craftsblock.craftscore.event.ListenerAdapter;
import de.idiotischer.bob.Server;
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
import de.idiotischer.bob.state.State;
import de.idiotischer.bob.tile.Tile;
import de.idiotischer.bob.troop.TroopStack;
import de.idiotischer.bob.troop.TroopValidator;
import it.unimi.dsi.fastutil.Pair;

import java.io.IOException;
import java.util.UUID;

public class ServerPacketListener implements ListenerAdapter {
    @EventHandler
    public void onPacketReceive(PacketRegistry.PacketReceiveEvent event) {
        Player player = Server.getInstance().getPlayerManager().resolve(event.getChannel());

        if(player != null && !player.authorized()) {
            if(event.getPacket() instanceof LoginPacket pack) {
                Server.getInstance().getPlayerManager().authPlayer(player,pack.getCredentials());
                //player.authorize(pack.getCredentials());
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
                case TROOPS_MOVE -> {
                    String[] parts = pack.getMessage().split(";");

                    String[] uuidPart = parts[0].split("=");
                    String[] tilePart = parts[1].split("=");

                    UUID uuid = UUID.fromString(uuidPart[0]);

                    Tile tile = Server.getInstance().getTileManager().byAbbreviation(tilePart[1]);
                    TroopStack troopStack = Server.getInstance().getTroopManager().getTroop(uuid);

                    if(troopStack == null) return;
                    if(tile == null) return;

                    String reply = "troop=" + uuid + ";tile=" + tile.getAbbreviation() + ";type=" + TroopValidator.validate(troopStack,tile);

                    Server.getInstance().getSendTool().send(event.getChannel(), new ReplyPacket(Type.TROOPS_MOVE, reply));
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
                        tile.setControllerForAll(Server.getInstance().getServerSocket().getClients(), country);
                    }
                }
            }
        }
    }

}

