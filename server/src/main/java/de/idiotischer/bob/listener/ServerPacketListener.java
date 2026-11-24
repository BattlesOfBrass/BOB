package de.idiotischer.bob.listener;

import de.craftsblock.craftscore.event.EventHandler;
import de.craftsblock.craftscore.event.ListenerAdapter;
import de.idiotischer.bob.Server;
import de.idiotischer.bob.conference.PeaceConference;
import de.idiotischer.bob.conference.ServerConferenceManager;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.focus.Focus;
import de.idiotischer.bob.focus.ServerFocusManager;
import de.idiotischer.bob.game.GameState;
import de.idiotischer.bob.networking.packet.PacketRegistry;
import de.idiotischer.bob.networking.packet.impl.*;
import de.idiotischer.bob.networking.packet.impl.pp.ReplyPacket;
import de.idiotischer.bob.networking.packet.impl.pp.RequestPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;
import de.idiotischer.bob.player.Player;
import de.idiotischer.bob.scenario.Scenario;
import de.idiotischer.bob.scenario.ServerScenarioManager;
import de.idiotischer.bob.scenario.ServerScenarioSceneLoader;
import de.idiotischer.bob.tile.ServerTileManager;
import de.idiotischer.bob.tile.Tile;
import de.idiotischer.bob.tile.event.TileChangedEvent;
import de.idiotischer.bob.troop.MoveStatus;
import de.idiotischer.bob.troop.ServerTroopManager;
import de.idiotischer.bob.troop.TroopStack;
import de.idiotischer.bob.troop.TroopValidator;
import de.idiotischer.bob.util.AddressUtil;
import de.idiotischer.bob.util.MilAccessRules;
import it.unimi.dsi.fastutil.Pair;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.*;

public class ServerPacketListener implements ListenerAdapter {

    @EventHandler
    public void onPacketReceive(PacketRegistry.PacketReceiveEvent event) {
        Player player = Server.getInstance().getPlayerManager().resolve(event.getChannel());

        if (player != null && !player.authorized()) {
            handleUnauthorized(player, event);
            return; // unauthorized players must not get any further
            //when tf did i write that comment and what kidn of pills did i take... im not a brit :skull:
        }

        try {
            Object packet = event.getPacket();

            if (packet instanceof PingPacket) onPing(event);
            else if (packet instanceof RequestPacket pack) onRequ(pack, player, event);
        } catch (Exception e) {
            System.err.println("Failed to handle packet " + event.getPacket().getClass().getSimpleName());
            e.printStackTrace();
        }
    }

    private void handleUnauthorized(Player player, PacketRegistry.PacketReceiveEvent event) {
        if (event.getPacket() instanceof LoginPacket pack) {
            Server.getInstance().getPlayerManager().authPlayer(player, pack.getCredentials());
            return;
        }

        try {
            event.getChannel().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void onPing(PacketRegistry.PacketReceiveEvent event) {
        System.out.println("Ping packet received at: " + System.nanoTime());
        Server.getInstance().getSendTool().send(event.getChannel(), new PongPacket());
    }

    private void onRequ(RequestPacket pack, Player player, PacketRegistry.PacketReceiveEvent event) {
        //so ping pong like
        switch (pack.getRequestType()) {
            case FOCUS_UPDATE -> onFocusUpdate(pack);
            case FOCUSES_SYNC -> Server.getInstance().getSendTool().send(event.getChannel(), FocusTreesSyncPacket.fromFocuses(Server.getInstance().getFocusManager().getTrees()));
            case MODIFIERS_SYNC -> Server.getInstance().getSendTool().send(event.getChannel(), ModifiersSyncPacket.fromModifiers(Server.getInstance().getModifierManager().getModifiers()));
            case FACTION_RULES_SYNC -> Server.getInstance().getSendTool().send(event.getChannel(), FactionRulesSyncPacket.fromFactionRules(Server.getInstance().getFactionManager().getFactionRules()));
            case FACTIONS_SYNC -> Server.getInstance().getSendTool().send(event.getChannel(), FactionsSyncPacket.fromFactions(Server.getInstance().getFactionManager().getFactions()));
            case GAMESTATE_SET -> onGameStateSet(pack, player);
            case GAMESPEED_CHANGE -> onGameSpeedChange(pack, player);
            case DEPLOY_TROOP -> onDeployTroop(pack, player);
            case SEND_DEMANDS -> onSendDemands(pack);
            case END_CONFERENCE -> onEndConference(pack, player);
            case TRAIN_TROOP -> onTrainTroop(pack);
            case WARS_SYNC -> Server.getInstance().getSendTool().send(event.getChannel(), new ReplyPacket(Type.WARS_SYNC, Server.getInstance().getWarManager().serializeWars()));
            case MIL_ACCESS -> onMilAccess(pack, player);
            case START_WAR -> onStartWar(pack, event);
            case TROOPS_MOVE -> onTroopsMove(pack, event);
            case PLAYER_CHANGE -> onPlayerChange(pack, event);
            case TROOPS_SYNC -> Server.getInstance().getSendTool().send(event.getChannel(), TroopStacksSyncPacket.fromStates(Server.getInstance().getTroopManager().getTroopStacks()));
            case SCENARIOS -> onScenarios(event);
            case TILES_SYNC -> Server.getInstance().getSendTool().send(event.getChannel(), TilesSyncPacket.fromTiles(Server.getInstance().getTileManager().getTileSet()));
            case IDEOLOGIES_SYNC -> Server.getInstance().getSendTool().send(event.getChannel(), IdeologiesSyncPacket.fromIdeologies(Server.getInstance().getIdeologyManager().getIdeologies()));
            case STATES_SYNC -> Server.getInstance().getSendTool().send(event.getChannel(), StatesSyncPacket.fromStates(Server.getInstance().getStateManager().getStateSet()));
            case STATE_SYNC -> {}
            case COUNTRIES_SYNC -> Server.getInstance().getSendTool().send(event.getChannel(), CountriesSyncPacket.fromCountries(Server.getInstance().getCountryManager().getCountries()));
            case SCENARIO_SYNC -> onScenarioSync(event);
            case SCENARIO_LOAD -> onScenarioLoad(pack);
            case TILE_CHANGE -> onTileChange(pack);
        }
    }

    private void onFocusUpdate(RequestPacket pack) {
        String[] strings = pack.getMessage().split(";");

        String id = valueOf(strings[0], "id");
        String treeId = valueOf(strings[1], "treeId");

        ServerFocusManager focusManager = Server.getInstance().getFocusManager();

        Focus c = focusManager.focusBy(treeId, id);
        if (c == null) return;

        String s1 = strings[2];

        if (s1.startsWith("start")) focusManager.startFocus(valueOf(s1, "start"), id, treeId);
        //else if(s1.startsWith("end")) {
        //    focusManager.endFocus(valueOf(s1, "end"), id, treeId);
        //}
        else if (s1.startsWith("cancel")) focusManager.cancelFocus(valueOf(s1, "cancel"), id, treeId);
    }

    private void onGameStateSet(RequestPacket pack, Player player) {
        if (player == null) return;

        try {
            Server.getInstance().getGameManager().setState(GameState.valueOf(pack.getMessage()));
        } catch (IllegalArgumentException e) {
            if (Server.getInstance().isDebug()) System.out.println("couldn't find gamestate named: " + pack.getMessage());
        }
    }

    private void onGameSpeedChange(RequestPacket pack, Player player) {
        if (player == null) return;

        int speed = Integer.parseInt(pack.getMessage());

        if (player.hasOP() || Server.getInstance().getPlayerManager().getPlayers().size() <= 1) Server.getInstance().getGameManager().setSpeed(speed);
    }

    private void onDeployTroop(RequestPacket pack, Player player) {
        if (player == null) return;

        Server.getInstance().getTroopManager().deployNow(UUID.fromString(pack.getMessage()));
    }

    private void onTrainTroop(RequestPacket pack) {
        String[] strings = pack.getMessage().split(";");

        Tile tile = Server.getInstance().getTileManager().byAbbreviation(strings[0]);
        Country owner = Server.getInstance().getCountryManager().byAbbreviation(strings[1]);
        int count = Integer.parseInt(strings[2]); // currently unused, see comment below

        if (tile == null || owner == null) return;

        TroopStack stack = new TroopStack("Troop", tile, owner, owner, 1 /*make it not use this, you need to train in pairs of 1*/, "none");

        Server.getInstance().getTroopManager().addTroopTraining(stack);
    }

    private void onTroopsMove(RequestPacket pack, PacketRegistry.PacketReceiveEvent event) {
        String[] parts = pack.getMessage().split(";");

        String[] firstPart = parts[0].split("=");
        String[] tilePart = parts[1].split("=");

        Tile tile = Server.getInstance().getTileManager().byAbbreviation(tilePart[1]);
        if (tile == null) return;

        Player p = Server.getInstance().getPlayerManager().resolve(event.getChannel());

        if (firstPart[0].equals("troop")) moveSingleTroop(UUID.fromString(firstPart[1]), tile, p);
        else if (firstPart[0].equals("troops")) moveMultipleTroops(firstPart[1], tile, p);
    }

    private void moveSingleTroop(UUID uuid, Tile tile, Player p) {
        ServerTroopManager troops = Server.getInstance().getTroopManager();

        TroopStack troopStack = troops.getTroop(uuid);
        if (troopStack == null) return;

        MoveStatus moveStatus = TroopValidator.validate(p, troopStack, tile, troops, Server.getInstance().getTileManager());

        if (failed(moveStatus)) return;

        troops.removePathfinding(troopStack);
        troopStack.setTile(tile);

        String reply = "troop=" + uuid + ";tile=" + tile.getAbbreviation() + ";type=" + moveStatus.ordinal();

        Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().getClients(), new ReplyPacket(Type.TROOPS_MOVE, reply));
    }

    private void moveMultipleTroops(String uuidList, Tile tile, Player p) {
        ServerTroopManager troops = Server.getInstance().getTroopManager();
        ServerTileManager tiles = Server.getInstance().getTileManager();

        List<String> statuses = new ArrayList<>();

        for (String uuidString : uuidList.split(",")) {
            TroopStack troopStack = troops.getTroop(UUID.fromString(uuidString));

            MoveStatus moveStatus;

            if (troopStack == null) moveStatus = MoveStatus.FAILURE;
            else {
                moveStatus = TroopValidator.validate(p, troopStack, tile, troops, tiles);

                if (!failed(moveStatus)) {
                    troops.removePathfinding(troopStack);
                    troopStack.setTile(tile);
                }
            }

            statuses.add(String.valueOf(moveStatus.ordinal()));
        }

        String reply = "troops=" + uuidList + ";tile=" + tile.getAbbreviation() + ";types=" + String.join(",", statuses);

        Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().getClients(), new ReplyPacket(Type.TROOPS_MOVE, reply));
    }

    private void onSendDemands(RequestPacket pack) {
        ServerConferenceManager conferences = Server.getInstance().getConferenceManager();

        PeaceConference.Demands demands = conferences.readDemands(Server.getInstance().getCountryManager(), Server.getInstance().getTileManager(), pack.getMessage());

        conferences.sendDemands(demands);

        PeaceConference conference = conferences.getBy(demands.peaceId());
        if (conference == null) return;

        conference.addFinished(demands.country());
    }

    private void onEndConference(RequestPacket pack, Player player) {
        UUID id = UUID.fromString(pack.getMessage());

        if (player == null) return;

        Country c = player.country();
        if (c == null) return;

        PeaceConference conference = Server.getInstance().getConferenceManager().getBy(id);
        if (conference == null) return;

        conference.addEnded(c);
    }

    private void onMilAccess(RequestPacket pack, Player player) {
        Country c = Server.getInstance().getCountryManager().byAbbreviation(pack.getMessage());

        if (c == null) return;
        if (player == null) return;
        if (player.country() == null) return;

        if (c.hasCountryMilAccess(player.country())) return;

        if (MilAccessRules.canHave(Server.getInstance().getWarManager(), c, player.country())) return;

        c.addMilAccess(player.country().getAbbreviation());
        String acc = c.serializeAccessUpdate(player.country().getAbbreviation(), true);

        ReplyPacket p = new ReplyPacket(Type.MIL_ACCESS, acc);
        Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().channels(), p);
    }

    private void onStartWar(RequestPacket pack, PacketRegistry.PacketReceiveEvent event) {
        String[] parts = pack.getMessage().split(";");

        String abbr = parts[0];
        String country = parts[1]; //in the case that the client should be desynced i just send the right country with it
        boolean callAllies = Boolean.parseBoolean(parts[2]);

        Tile declaredTile = Server.getInstance().getTileManager().byAbbreviation(abbr);
        if (declaredTile == null) return;

        Country controller = Server.getInstance().getCountryManager().byAbbreviation(country);
        if (controller == null) return;

        Player p = Server.getInstance().getPlayerManager().resolve(event.getChannel());
        if (p == null) return;

        Server.getInstance().getWarManager().declareWar(callAllies, declaredTile, controller, p.country());
    }

    private void onPlayerChange(RequestPacket pack, PacketRegistry.PacketReceiveEvent event) {
        String[] parts = pack.getMessage().split(";");

        String abbreviation = parts[1]; // parts[0] is the uuid

        Country country = Server.getInstance().getCountryManager().byAbbreviation(abbreviation);

        //Allow null Country so that spectator is possible

        Player p = Server.getInstance().getPlayerManager().getPlayer(AddressUtil.getRemoteAddress(event.getChannel()));

        if (p == null) {
            Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().getClients(), new ReplyPacket(Type.PLAYER_CHANGE, pack.getMessage() + ";type=false"));
            return;
        }

        p.country(country == null ? null : country.getAbbreviation());

        Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().getClients(), new ReplyPacket(Type.PLAYER_CHANGE, pack.getMessage() + ";type=true"));
    }

    private void onScenarios(PacketRegistry.PacketReceiveEvent event) {
        if (!Server.getInstance().getServerSocket().isLocal()) return; // only sync 1 scenario for remote not all like on the client

        Server.getInstance().getSendTool().send(event.getChannel(), ScenariosSyncPacket.fromScenarios(Server.getInstance().getScenarioManager().getScenarios()));
    }

    private void onScenarioSync(PacketRegistry.PacketReceiveEvent event) {
        ServerScenarioSceneLoader loader = Server.getInstance().getScenarioSceneLoader();
        if (loader == null) return;

        Scenario scenario = loader.getCurrentScenario();

        Server.getInstance().getSendTool().send(event.getChannel(), new ScenarioSyncPacket(scenario));
    }

    private void onScenarioLoad(RequestPacket pack) {
        ServerScenarioManager manager = Server.getInstance().getScenarioManager();
        if (manager == null) return;

        Scenario scenario = manager.getScenario(pack.getMessage());
        if (scenario == null) return;

        Server.getInstance().getScenarioSceneLoader().loadNew(scenario);
    }

    private void onTileChange(RequestPacket pack) {
        //TODO: i think this is obsolete
        String s = pack.getMessage();
        if (s.isEmpty()) return;

        Pair<Tile, Country> pair = Tile.deconstructChange(s, Server.getInstance().getCountryManager(), Server.getInstance().getTileManager());

        Tile tile = pair.key();
        if (tile == null) return;

        Country country = pair.value();
        if (country == null) return;

        if (!Server.getInstance().getTileValidator().isChangeValid(tile, tile.getController(), country)) return;

        Set<AsynchronousSocketChannel> clients = Server.getInstance().getServerSocket().getClients();

        if (Objects.equals(Tile.getChangeType(s), TileChangedEvent.Type.OWNER)) {
            tile.setOwnerForAll(clients, country);
            return;
        }

        Country oldController = tile.getController();

        tile.setControllerForAll(clients, country);
        Server.getInstance().getWarManager().checkWarOver(country, oldController, tile);
    }

    private static String valueOf(String part, String key) {
        return part.substring(key.length() + 1);
    }

    private static boolean failed(MoveStatus status) {
        return status == MoveStatus.FAILURE || status == MoveStatus.FAILURE_FIGHT || status == MoveStatus.FAILURE_NO_CONTROL || status == MoveStatus.FAILURE_KICKED || status == MoveStatus.FAILURE_IN_COMBAT || status == MoveStatus.FAILURE_STARTED_PATHFINDING;
    }
}