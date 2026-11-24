package de.idiotischer.bob.listener;

import de.craftsblock.craftscore.event.EventHandler;
import de.craftsblock.craftscore.event.EventPriority;
import de.craftsblock.craftscore.event.ListenerAdapter;
import de.idiotischer.bob.BOB;
import de.idiotischer.bob.conference.PeaceConference;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.country.CountryManager;
import de.idiotischer.bob.country.CountryResolver;
import de.idiotischer.bob.country.PuppetState;
import de.idiotischer.bob.faction.Faction;
import de.idiotischer.bob.faction.FactionManager;
import de.idiotischer.bob.focus.Focus;
import de.idiotischer.bob.focus.FocusManager;
import de.idiotischer.bob.focus.FocusTree;
import de.idiotischer.bob.game.GameState;
import de.idiotischer.bob.ideology.Ideology;
import de.idiotischer.bob.ideology.IdeologyManager;
import de.idiotischer.bob.modifier.ModifierManager;
import de.idiotischer.bob.modifiers.Modifier;
import de.idiotischer.bob.networking.packet.PacketRegistry;
import de.idiotischer.bob.networking.packet.impl.*;
import de.idiotischer.bob.networking.packet.impl.pp.ReplyPacket;
import de.idiotischer.bob.player.Player;
import de.idiotischer.bob.player.PlayerManager;
import de.idiotischer.bob.player.persona.AIPersonality;
import de.idiotischer.bob.render.RenderPanel;
import de.idiotischer.bob.scenario.Scenario;
import de.idiotischer.bob.scenario.ScenarioManager;
import de.idiotischer.bob.state.State;
import de.idiotischer.bob.state.StateManager;
import de.idiotischer.bob.tile.Tile;
import de.idiotischer.bob.tile.TileManager;
import de.idiotischer.bob.tile.event.TileChangedEvent;
import de.idiotischer.bob.troop.MoveStatus;
import de.idiotischer.bob.troop.TrainEntry;
import de.idiotischer.bob.troop.TroopManager;
import de.idiotischer.bob.troop.TroopStack;
import de.idiotischer.bob.util.AddressUtil;
import de.idiotischer.bob.war.WarManager;
import de.idiotischer.bob.war.WarStatus;
import it.unimi.dsi.fastutil.Pair;

import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class PacketListener implements ListenerAdapter {

    private static final MoveStatus[] MOVE_STATUSES = MoveStatus.values(); //micro optimization type shiiii
    private static final Color ACCEPTED_COLOR = new Color(80, 235, 75);

    private final Map<Class<?>, BiConsumer<Object, PacketRegistry.PacketReceiveEvent>> handlers = new HashMap<>();

    public PacketListener() {
        register(PongPacket.class, this::onPong);
        register(ScenariosSyncPacket.class, this::onScenariosSync);
        register(ScenarioSyncPacket.class, this::onScenarioSync);
        register(CountriesSyncPacket.class, this::onCountriesSync);
        register(CountrySyncPacket.class, this::onCountrySync);
        register(TilesSyncPacket.class, this::onTilesSync);
        register(TileSyncPacket.class, this::onTileSync);
        register(IdeologiesSyncPacket.class, this::onIdeologiesSync);
        register(IdeologySyncPacket.class, this::onIdeologySync);
        register(StatesSyncPacket.class, this::onStatesSync);
        register(ReplyPacket.class, this::onReply);
        register(PlayerAuthUpdatePacket.class, this::onPlayerAuthUpdate);
        register(PlayerJoinPacket.class, this::onPlayerJoin);
        register(PlayerQuitPacket.class, this::onPlayerQuit);
        register(PlayerChangedCountryPacket.class, this::onPlayerChangedCountry);
        register(TroopStackSyncPacket.class, this::onTroopStackSync);
        register(TroopStacksSyncPacket.class, this::onTroopStacksSync);
        register(CombatSyncPacket.class, this::onCombatSync);
        register(CombatsSyncPacket.class, this::onCombatsSync);
        register(TimeCheckAndCorrectPacket.class, this::onTimeCheck);
        register(FactionSyncPacket.class, this::onFactionSync);
        register(FactionsSyncPacket.class, this::onFactionsSync);
        register(FactionRuleSyncPacket.class, this::onFactionRuleSync);
        register(FactionRulesSyncPacket.class, this::onFactionRulesSync);
        register(ModifierSyncPacket.class, this::onModifierSync);
        register(ModifiersSyncPacket.class, this::onModifiersSync);
        register(FocusTreeSyncPacket.class, this::onFocusTreeSync);
        register(FocusTreesSyncPacket.class, this::onFocusTreesSync);
    }

    private <T> void register(Class<T> type, BiConsumer<T, PacketRegistry.PacketReceiveEvent> handler) {
        handlers.put(type, (packet, event) -> handler.accept(type.cast(packet), event));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreWhenCancelled = true)
    public void onPacketReceive(PacketRegistry.PacketReceiveEvent event) {
        var handle = handlers.get(event.getPacket().getClass());
        if (handle == null) return;

        try {
            handle.accept(event.getPacket(), event);
        } catch (Exception e) {
            System.err.println("Failed to handle packet " + event.getPacket().getClass().getSimpleName());
            e.printStackTrace();
        }
    }

    private void onPong(PongPacket pack, PacketRegistry.PacketReceiveEvent event) {
        System.out.println("Pong received and cancelled at: " + System.nanoTime());
        //event.setCancelled(true);
    }

    private void onScenariosSync(ScenariosSyncPacket pack, PacketRegistry.PacketReceiveEvent event) {
        ScenarioManager manager = BOB.getInstance().getScenarioManager();
        if (manager == null) return;

        List<Scenario> scenarios = new ArrayList<>();

        pack.getScenarioPackets().forEach(scenarioPacket -> {
            Path scenarioDir = scenarioPacket.applyToDisk2(scenarioPacket.getAbbreviation());

            Path baseScenarioDir = Paths.get(scenarioPacket.getAbbreviation());
            boolean server = scenarioPacket.resolveFallbackScenarioDir(baseScenarioDir) == scenarioDir;

            Scenario scenario = new Scenario(server, scenarioPacket.getAbbreviation(), scenarioPacket.getName(), scenarioDir);

            if (scenarioPacket.getTheme() == null) scenario.setTheme(BOB.getInstance().getSettingsTheme());

            scenarios.add(scenario);
        });

        manager.refresh(scenarios);
    }

    private void onScenarioSync(ScenarioSyncPacket packet, PacketRegistry.PacketReceiveEvent event) {
        packet.applyToDisk();

        ScenarioManager manager = BOB.getInstance().getScenarioManager();
        if (manager == null) return;

        Scenario scenario = manager.getScenario(packet.getAbbreviation());
        if (scenario == null) return;

        manager.refreshAddNew(scenario); //hier stand vorher == null, maybe sollte das so, ich glaube es war n bug

        if (packet.getTheme() == null) scenario.setTheme(BOB.getInstance().getSettingsTheme());

        BOB.getInstance().getScenarioSceneLoader().completeSync(scenario);
        BOB.getInstance().getScenarioSceneLoader().load(scenario, false);
    }

    private void onCountriesSync(CountriesSyncPacket packet, PacketRegistry.PacketReceiveEvent event) {
        ModifierManager modifiers = BOB.getInstance().getModifierManager();  //fuck it we ball, i gotta optimize so im doing it correctly
        IdeologyManager ideologies = BOB.getInstance().getIdeologyManager();  //fuck it we ball, i gotta optimize so im doing it correctly
        CountryManager countryManager = BOB.getInstance().getCountryManager();  //fuck it we ball, i gotta optimize so im doing it correctly

        Set<Country> countries = packet.getPackets().stream().map(csp -> csp.getCountry(modifiers, ideologies)).collect(Collectors.toSet());

        countries.forEach(countryManager::registerCountry);
        countryManager.finishReload();
    }

    private void onCountrySync(CountrySyncPacket packet, PacketRegistry.PacketReceiveEvent event) {
        Country country = packet.getCountry(BOB.getInstance().getModifierManager(), BOB.getInstance().getIdeologyManager());

        if (BOB.getInstance().getCountryManager().has(country)) return;

        BOB.getInstance().getCountryManager().registerCountry(country);
    }

    private void onTilesSync(TilesSyncPacket packet, PacketRegistry.PacketReceiveEvent event) {
        TileManager tileManager = BOB.getInstance().getTileManager();
        CompletableFuture<Void> future = tileManager.getAwaitingFuture();

        if (future == null || future.isCancelled() || future.isDone()) return;

        List<TileSyncPacket> packs = packet.getPackets();

        packs.forEach(s -> s.reconstruct(BOB.getInstance().getSharedCore(), BOB.getInstance().getCountryManager()));

        Set<Tile> tiles = packs.stream().map(TileSyncPacket::getTile).collect(Collectors.toSet());

        tiles.forEach(tileManager::registerTile);

        tileManager.finishReload();
    }

    private void onTileSync(TileSyncPacket packet, PacketRegistry.PacketReceiveEvent event) {
        Tile tile = packet.getTile();

        if (BOB.getInstance().getTileManager().has(tile)) return;

        BOB.getInstance().getTileManager().registerTile(tile);
    }

    private void onIdeologiesSync(IdeologiesSyncPacket packet, PacketRegistry.PacketReceiveEvent event) {
        IdeologyManager manager = BOB.getInstance().getIdeologyManager();  //fuck it we ball, i gotta optimize so im doing it correctly
        CompletableFuture<Void> future = manager.getAwaitingFuture();

        if (future == null || future.isCancelled() || future.isDone()) return;

        Set<Ideology> ideologies = packet.getPackets().stream().map(IdeologySyncPacket::getIdeology).collect(Collectors.toSet());

        manager.getIdeologies().addAll(ideologies);
        manager.finishReload();
    }

    private void onIdeologySync(IdeologySyncPacket packet, PacketRegistry.PacketReceiveEvent event) {
        BOB.getInstance().getIdeologyManager().getIdeologies().add(packet.getIdeology());
    }

    private void onStatesSync(StatesSyncPacket packet, PacketRegistry.PacketReceiveEvent event) {
        StateManager manager = BOB.getInstance().getStateManager();
        CompletableFuture<Void> future = manager.getAwaitingFuture();

        if (future == null || future.isCancelled() || future.isDone()) return;

        Set<State> states = packet.getPackets().stream().map(s -> s.getState(BOB.getInstance().getCountryManager(), BOB.getInstance().getTileManager())).collect(Collectors.toSet());

        states.forEach(manager::registerState);

        manager.finishReload(true);
    }

    private void onPlayerAuthUpdate(PlayerAuthUpdatePacket pack, PacketRegistry.PacketReceiveEvent event) {
        if (!pack.isAuthed()) return;

        BOB.getInstance().setPlayer(pack.getUuid());
        BOB.getInstance().getPlayerManager().addPlayer(BOB.getInstance().getPlayer());
    }

    private void onPlayerJoin(PlayerJoinPacket pack, PacketRegistry.PacketReceiveEvent event) {
        PlayerManager players = BOB.getInstance().getPlayerManager(); //fuck it we ball, i gotta optimize so im doing it correctly

        if (Objects.equals(pack.getAddress(), AddressUtil.getThisAddress(event.getChannel()))) players.getPlayer(pack.getAddress()).uuid(pack.getUuid());

        if (players.hasPlayer(pack.getUuid())) {
            System.out.println("UUID already good " + players.getPlayer(pack.getAddress()).uuid() + " " + pack.getUuid());
            return;
        }

        if (players.hasPlayer(pack.getAddress())) {
            //für local sync
            System.out.println("UUID changed " + players.getPlayer(pack.getAddress()).uuid() + " " + pack.getUuid());

            players.getPlayer(pack.getAddress()).uuid(pack.getUuid());
        }

        Player p = players.createPlayer(event.getChannel(), pack.getUuid(), pack.getAddress());

        players.addPlayer(p);
    }

    private void onPlayerQuit(PlayerQuitPacket pack, PacketRegistry.PacketReceiveEvent event) {
        BOB.getInstance().getPlayerManager().removePlayer(pack.getUuid());
    }

    private void onPlayerChangedCountry(PlayerChangedCountryPacket pack, PacketRegistry.PacketReceiveEvent event) {
        Player player = BOB.getInstance().getPlayerManager().getPlayer(pack.getUuid());
        if (player == null) return;

        Country country = BOB.getInstance().getCountryManager().byAbbreviation(pack.getCountryAbbreviation());

        player.country(country == null ? null : country.getAbbreviation());
    }

    private void onTroopStackSync(TroopStackSyncPacket pack, PacketRegistry.PacketReceiveEvent event) {
        var z = pack.getTroopStack(BOB.getInstance().getCountryManager(), BOB.getInstance().getTileManager());
        BOB.getInstance().getTroopManager().addTroopStack(z.value(), z.key());
    }

    private void onTroopStacksSync(TroopStacksSyncPacket pack, PacketRegistry.PacketReceiveEvent event) {
        CountryManager countries = BOB.getInstance().getCountryManager();
        TileManager tiles = BOB.getInstance().getTileManager();

        Map<UUID, TroopStack> map = pack.getPackets().stream().map(d -> {
            var pair = d.getTroopStack(countries, tiles);
            return Map.entry(pair.key(), pair.value());
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        BOB.getInstance().getTroopManager().finishReload(map, true);
    }

    private void onCombatSync(CombatSyncPacket pack, PacketRegistry.PacketReceiveEvent event) {
        BOB.getInstance().getCombatManager().addStatus(pack.getStatus(BOB.getInstance().getTroopManager()));
    }

    private void onCombatsSync(CombatsSyncPacket pack, PacketRegistry.PacketReceiveEvent event) {
        TroopManager troops = BOB.getInstance().getTroopManager();  //fuck it we ball, i gotta optimize so im doing it correctly

        BOB.getInstance().getCombatManager().addStatus(pack.getPackets().stream().map(p -> p.getStatus(troops)).toList());
    }

    private void onTimeCheck(TimeCheckAndCorrectPacket pack, PacketRegistry.PacketReceiveEvent event) {
        BOB.getInstance().getTimeManager().sync(pack.getStartDate(), pack.getTimeTicks(), pack.getSpeed());
    }

    private void onFactionSync(FactionSyncPacket pack, PacketRegistry.PacketReceiveEvent event) {
        BOB.getInstance().getFactionManager().addFaction(pack.getFaction(BOB.getInstance().getFactionManager()));
    }

    private void onFactionsSync(FactionsSyncPacket packet, PacketRegistry.PacketReceiveEvent event) {
        FactionManager manager = BOB.getInstance().getFactionManager();  //fuck it we ball, i gotta optimize so im doing it correctly

        packet.getPackets().forEach(pack -> manager.addFaction(pack.getFaction(manager)));

        manager.finishFactionsReload();
    }

    private void onFactionRuleSync(FactionRuleSyncPacket pack, PacketRegistry.PacketReceiveEvent event) {
        BOB.getInstance().getFactionManager().addFactionRule(pack.getFactionRule(BOB.getInstance().getModifierManager()));
    }

    private void onFactionRulesSync(FactionRulesSyncPacket packet, PacketRegistry.PacketReceiveEvent event) {
        FactionManager manager = BOB.getInstance().getFactionManager();
        ModifierManager modifiers = BOB.getInstance().getModifierManager();

        packet.getPackets().forEach(pack -> manager.addFactionRule(pack.getFactionRule(modifiers)));

        manager.finishRulesReload();
    }

    private void onModifierSync(ModifierSyncPacket pack, PacketRegistry.PacketReceiveEvent event) {
        BOB.getInstance().getModifierManager().addModifier(pack.getModifier());
    }

    private void onModifiersSync(ModifiersSyncPacket packet, PacketRegistry.PacketReceiveEvent event) {
        ModifierManager manager = BOB.getInstance().getModifierManager(); //fuck it we ball, i gotta optimize so im doing it correctly

        packet.getPackets().forEach(pack -> manager.addModifier(pack.getModifier()));

        manager.finishReload();
    }

    private void onFocusTreeSync(FocusTreeSyncPacket pack, PacketRegistry.PacketReceiveEvent event) {
        FocusTree tree = pack.getTree(BOB.getInstance().getModifierManager());
        BOB.getInstance().getFocusManager().add(tree);
    }

    private void onFocusTreesSync(FocusTreesSyncPacket packet, PacketRegistry.PacketReceiveEvent event) {
        FocusManager manager = BOB.getInstance().getFocusManager();
        ModifierManager modifiers = BOB.getInstance().getModifierManager();

        packet.getPackets().forEach(pack -> manager.add(pack.getTree(modifiers)));

        manager.finishReload();
    }

    private void onReply(ReplyPacket pack, PacketRegistry.PacketReceiveEvent event) {
        switch (pack.getReplyType()) {
            case TROOP_TRAIN_UPDATE -> onTroopTrainUpdate(pack);
            case TROOP_UPDATE -> onTroopUpdate(pack);
            case TROOP_TRAIN_START -> onTroopTrainStart(pack);
            case TROOP_TRAIN_END -> onTroopTrainEnd(pack);
            case FOCUSTREE_REMOVE -> BOB.getInstance().getFocusManager().remove(pack.getMessage());
            case MODIFIERS_UPDATE -> onModifiersUpdate(pack);
            case FOCUS_UPDATE -> onFocusUpdate(pack);
            case TIME_RESTART -> onTimeRestart(pack);
            case COUNTRY_UPDATE -> onCountryUpdate(pack);
            case MODIFIER_UPDATE -> onModifierUpdate(pack);
            case FACTION_UPDATE -> onFactionUpdate(pack);
            case PP_UPDATE -> onPpUpdate(pack);
            case GAMESTATE_SYNC -> onGameStateSync(pack);
            case IDEOLOGY_UPDATE -> onIdeologyUpdate(pack);
            case MIL_ACCESS -> onMilAccess(pack);
            case TILE_CONNECTION_CHANGE -> onTileConnectionChange(pack);
            case CONFERENCE_STARTED -> onConferenceStarted(pack);
            case END_CONFERENCE -> onEndConference(pack);
            case ROUND_ENDED -> onRoundEnded(pack);
            case ROUND_UPDATE -> onRoundUpdate(pack);
            case CLEAR_COMBATS -> BOB.getInstance().getCombatManager().clear();
            case COMBAT_OVER -> onCombatOver(pack);
            case TROOP_REMOVE -> onTroopRemove(pack);
            case CAPITULATE_COUNTRY -> onCapitulateCountry(pack);
            case END_WAR -> onEndWar(pack);
            case START_WAR -> onStartWar(pack);
            case WARS_SYNC -> BOB.getInstance().getWarManager().finishReload(pack.getMessage());
            case TROOP_TP -> onTroopTp(pack);
            case TROOPS_MOVE -> onTroopsMove(pack);
            case TILE_UPDATE -> onTileUpdate(pack);
            case TILE_CHANGE -> onTileChange(pack);
            case PLAYER_CHANGE -> onPlayerChange(pack, event);
            case ERROR -> {}
        }
    }

    private void onTroopTrainUpdate(ReplyPacket pack) {
        String[] strings = pack.getMessage().split(";");

        UUID id = UUID.fromString(valueOf(strings[0], "id"));

        TrainEntry e = BOB.getInstance().getTroopManager().getTraining().stream().filter(te -> te.getUuid().equals(id)).findFirst().orElse(null);

        if (e == null) return;

        String s1 = strings[1];

        if (s1.startsWith("pop")) e.setPop(Double.parseDouble(valueOf(s1, "pop")));
    }

    private void onTroopUpdate(ReplyPacket pack) {
        String[] strings = pack.getMessage().split(";");

        UUID id = UUID.fromString(valueOf(strings[0], "id"));

        TroopStack troopStack = BOB.getInstance().getTroopManager().getTroop(id);
        if (troopStack == null) return;

        String s1 = strings[1];

        if (s1.startsWith("exp")) troopStack.setExp(Integer.parseInt(valueOf(s1, "exp")));
        else if (s1.startsWith("template")) troopStack.setTemplate(valueOf(s1, "template"));
        else if (s1.startsWith("controller")) {
            Country c = BOB.getInstance().getCountryManager().getCountry(valueOf(s1, "controller"));
            if (c == null) return;

            troopStack.setController(c);
        } else if (s1.startsWith("owner")) {
            Country c = BOB.getInstance().getCountryManager().getCountry(valueOf(s1, "owner"));
            if (c == null) return;

            troopStack.setOwner(c);
        } else if (s1.startsWith("pop")) troopStack.setPopulation(Long.parseLong(valueOf(s1, "pop")));
    }

    private void onTroopTrainStart(ReplyPacket pack) {
        //uuid + ";" + troop.getName() + ";" + troop.getTemplate() + ";" + troop.getTrainDays()
        String[] strings = pack.getMessage().split("#");

        int troopTrainDays = Integer.parseInt(strings[0]);
        Pair<UUID, TroopStack> troop = TroopStack.deserialize(strings[1], BOB.getInstance().getCountryManager(), BOB.getInstance().getTileManager());
        int startTime = Integer.parseInt(strings[2]);
        long availablePop = Long.parseLong(strings[3]);

        BOB.getInstance().getTroopManager().addTrainEntry(new TrainEntry(startTime, 0, troop.key(), availablePop, troopTrainDays, troop.value()));
    }

    private void onTroopTrainEnd(ReplyPacket pack) {
        String[] strings = pack.getMessage().split(";");

        BOB.getInstance().getTroopManager().removeTrainEntry(UUID.fromString(strings[0]), false);
    }

    private void onTroopRemove(ReplyPacket pack) {
        UUID uuid = parseUuid(pack.getMessage());
        if (uuid == null) return;

        BOB.getInstance().getTroopManager().removeTroopStack(uuid);
    }

    private void onTroopTp(ReplyPacket pack) {
        String[] parts = pack.getMessage().split(";");

        String[] troopPart = parts[0].split("=");
        String[] tilePart = parts[1].split("=");

        Tile tile = BOB.getInstance().getTileManager().byAbbreviation(tilePart[1]);

        if (troopPart[0].equals("troop")) BOB.getInstance().getTroopManager().tp(UUID.fromString(troopPart[1]), tile);
    }

    private void onTroopsMove(ReplyPacket pack) {
        String[] parts = pack.getMessage().split(";");

        String[] troopPart = parts[0].split("=");
        String[] tilePart = parts[1].split("=");
        String[] statusPart = parts[2].split("=");

        Tile tile = BOB.getInstance().getTileManager().byAbbreviation(tilePart[1]);

        if (troopPart[0].equals("troop")) {
            UUID uuid = UUID.fromString(troopPart[1]);
            MoveStatus status = MOVE_STATUSES[Integer.parseInt(statusPart[1])]; //micro optimization type shiiii

            BOB.getInstance().getTroopManager().finishMove(uuid, tile, status);
        } else if (troopPart[0].equals("troops")) {
            String[] uuids = troopPart[1].split(",");
            String[] statuses = statusPart[1].split(",");

            Map<UUID, MoveStatus> results = new HashMap<>(uuids.length * 2);

            for (int i = 0; i < uuids.length; i++) {
                results.put(UUID.fromString(uuids[i]), MOVE_STATUSES[Integer.parseInt(statuses[i])]); //micro optimization type shiiii
            }

            BOB.getInstance().getTroopManager().finishMoveAll(results, tile);
        }
    }

    private void onCombatOver(ReplyPacket pack) {
        UUID uuid = parseUuid(pack.getMessage());
        if (uuid == null) return;

        BOB.getInstance().getCombatManager().remove(uuid);
    }

    private void onModifiersUpdate(ReplyPacket pack) {
        String[] strings = pack.getMessage().split(";");

        Country c = BOB.getInstance().getCountryManager().byAbbreviation(valueOf(strings[0], "id"));
        if (c == null) return;

        String s1 = strings[1];

        if (s1.startsWith("add")) {
            Modifier mod = BOB.getInstance().getModifierManager().by(valueOf(s1, "add"));
            c.addModifier(mod, BOB.getInstance().getTimeManager());
        } else if (s1.startsWith("remove")) {
            Modifier mod = BOB.getInstance().getModifierManager().by(valueOf(s1, "remove"));
            c.removeModifier(mod);
        }
    }

    private void onModifierUpdate(ReplyPacket pack) {
        String[] strings = pack.getMessage().split(";");

        Modifier modifier = BOB.getInstance().getModifierManager().by(valueOf(strings[0], "id"));
        if (modifier == null) return;

        String s1 = strings[1];

        if (s1.startsWith("displayName")) modifier.setDisplayName(valueOf(s1, "displayName"));
        else if (s1.startsWith("modifier")) modifier.setModifier(valueOf(s1, "modifier"));
        else if (s1.startsWith("specificallyFor")) modifier.setSpecificallyFor(valueOf(s1, "specificallyFor"));
        else if (s1.startsWith("type")) modifier.setType(valueOf(s1, "type"));
    }

    private void onFocusUpdate(ReplyPacket pack) {
        String[] strings = pack.getMessage().split(";");

        String id = valueOf(strings[0], "id");
        String treeId = valueOf(strings[1], "treeId");

        FocusManager focusManager = BOB.getInstance().getFocusManager();

        Focus c = focusManager.focusBy(treeId, id);
        if (c == null) return;

        String s1 = strings[2];

        if (s1.startsWith("start")) focusManager.startFocus(valueOf(s1, "start"), id, treeId);
        else if (s1.startsWith("end")) focusManager.endFocus(valueOf(s1, "end"), id, treeId);
        else if (s1.startsWith("cancel")) focusManager.cancelFocus(valueOf(s1, "cancel"), id, treeId);
    }

    private void onTimeRestart(ReplyPacket pack) {
        long timestamp = Long.parseLong(pack.getMessage());

        if (timestamp != -1L) BOB.getInstance().getTimeManager().reload(new Date(timestamp));
    }

    private void onGameStateSync(ReplyPacket pack) {
        if (pack.getMessage() == null) return;

        try {
            BOB.getInstance().getGameManager().setGameState(GameState.valueOf(pack.getMessage()));
        } catch (IllegalArgumentException e) {
            if (BOB.getInstance().isDebug()) System.out.println("couldn't find gamestate named: " + pack.getMessage());
        }
    }
    private void onCountryUpdate(ReplyPacket pack) {
        String[] strings = pack.getMessage().split(";");

        Country c = BOB.getInstance().getCountryManager().byAbbreviation(valueOf(strings[0], "id"));
        if (c == null) return;

        String s1 = strings[1];

        if (s1.startsWith("overlord")) c.setOverlord(valueOf(s1, "overlord"));
        else if (s1.startsWith("manpower")) {
            //System.out.println(pack.getMessage());
            c.setManpower(Long.parseLong(valueOf(s1, "manpower")));
        }
        else if (s1.startsWith("major")) c.setMajor(Boolean.parseBoolean(valueOf(s1, "major")));
        else if (s1.startsWith("conscription")) c.setConscription(Country.Conscription.valueOf(valueOf(s1, "conscription")));
        else if (s1.startsWith("puppetState")) {
            try {
                c.setPuppetState(PuppetState.valueOf(valueOf(s1, "puppetState")));
            } catch (IllegalArgumentException ignored) {}
        } else if (s1.startsWith("personality")) {
            AIPersonality pers = AIPersonality.fromDataString(valueOf(s1, "personality"));

            if (pers == null) pers = new AIPersonality(1, 1, 1, 1);

            c.setPersonality(pers);
        }
    }

    private void onFactionUpdate(ReplyPacket pack) {
        String[] strings = pack.getMessage().split(";");

        Faction faction = BOB.getInstance().getFactionManager().factionBy(valueOf(strings[0], "id"));
        if (faction == null) return;

        String s1 = strings[1];

        if (s1.startsWith("addMember")) faction.addMember(valueOf(s1, "addMember"));
        else if (s1.startsWith("removeMember")) faction.rmMember(valueOf(s1, "removeMember"));
        else if (s1.startsWith("changeName")) faction.setName(valueOf(s1, "changeName"));
    }

    private void onPpUpdate(ReplyPacket pack) {
        String[] strings = pack.getMessage().split(";");

        double pp = Double.parseDouble(strings[1]);

        Country c = BOB.getInstance().getCountryManager().byAbbreviation(strings[0]);
        if (c == null) return;

        c.setPoliticalPower(pp, BOB.getInstance().getPlayer().country());
    }

    private void onIdeologyUpdate(ReplyPacket pack) {
        CountryResolver r = BOB.getInstance().getCountryManager();

        String[] parts = pack.getMessage().split("=", 2);
        if (parts.length != 2) return;

        String action = parts[0];

        String[] values = parts[1].split(";", 2);
        if (values.length != 2) return;

        Country country = r.byAbbreviation(values[0]);
        Ideology ideology = BOB.getInstance().getIdeologyManager().byAbbreviation(values[1]);

        if (country == null || ideology == null) return;

        switch (action) {
            case "start" -> country.setStartIdeologySimple(ideology);
            case "add" -> country.addAvailableIdeologiesSimple(ideology);
            case "remove" -> country.rmAvailableIdeologiesSimple(ideology);
            case "ruling" -> country.setRulingIdeologySimple(ideology);
        }
    }

    private void onMilAccess(ReplyPacket pack) {
        var pair = Country.getAccessUpdate(pack.getMessage());

        Country c1 = BOB.getInstance().getCountryManager().byAbbreviation(pair.key());
        Country c2 = BOB.getInstance().getCountryManager().byAbbreviation(pair.value().key());
        boolean added = pair.value().value();

        if (c1 == null || c2 == null) return;

        RenderPanel panel = BOB.getInstance().getMainRenderer().getGamePanel();

        if (added) {
            panel.showGenericPopup("MILITARY ACCESS GRANTED!", "granted military access to", c1, c2, 5000, ACCEPTED_COLOR);
            c1.addMilAccess(c2.getAbbreviation());
        } else {
            panel.showGenericPopup("MILITARY ACCESS DENIED!", "denied military access to", c1, c2, 5000);
            c1.removeMilAccess(c2.getAbbreviation());
        }

        panel.getHud().updateTabsFr();
    }

    private void onCapitulateCountry(ReplyPacket pack) {
        String[] parts = pack.getMessage().split(";");

        Country country = BOB.getInstance().getCountryManager().byAbbreviation(parts[0]);
        if (country == null) return;

        country.setCapitulated(Boolean.parseBoolean(parts[1]));

        //TODO: show popup that a country capped (idk since its a bit dumb currentlky since peace starts immediatly)
    }

    private void onEndWar(ReplyPacket pack) {
        WarStatus status = WarStatus.fromString(pack.getMessage(), BOB.getInstance().getCountryManager());
        WarManager wm = BOB.getInstance().getWarManager();

        status.getAttackers().forEach(c -> wm.endWar(c.getAbbreviation(), status));
        status.getDefenders().forEach(c -> wm.endWar(c.getAbbreviation(), status));

        //TODO: show popup that a war ended and start peace conference (oh gosh i need to code that) (idk since its a bit dumb currentlky since peace starts immediatly)
    }

    private void onStartWar(ReplyPacket pack) {
        WarStatus status = WarStatus.fromString(pack.getMessage(), BOB.getInstance().getCountryManager());
        WarManager wm = BOB.getInstance().getWarManager();

        status.getAttackers().forEach(c -> wm.addWar(c.getAbbreviation(), status));
        status.getDefenders().forEach(c -> wm.addWar(c.getAbbreviation(), status));

        BOB.getInstance().getMainRenderer().getGamePanel().showGenericPopup("WAR DECLARED!", "declared war on", status.getAttackers().getFirst(), status.getDefenders().getFirst(), 5000);
    }

    private void onConferenceStarted(ReplyPacket pack) {
        PeaceConference conference = PeaceConference.deserialize(BOB.getInstance().getSharedCore(), BOB.getInstance().getCountryManager(), BOB.getInstance().getTileManager(), pack.getMessage());

        BOB.getInstance().getPeaceHelper().addPeaces(conference);
    }

    private void onEndConference(ReplyPacket pack) {
        UUID uuid = parseUuid(pack.getMessage());
        if (uuid == null) return;

        PeaceConference c = BOB.getInstance().getPeaceHelper().getBy(uuid);

        if (c != null) {
            BOB.getInstance().getMainRenderer().getGamePanel().showGenericPopup("PEACE ACHIEVED!", "achieved peace with", c.getWinners().getFirst(), c.getDefeated().getFirst(), 5000, ACCEPTED_COLOR);
        }

        BOB.getInstance().getPeaceHelper().removePeaces(uuid);
    }

    private void onRoundEnded(ReplyPacket pack) {
        PeaceConference conf = BOB.getInstance().getPeaceHelper().getBy(PeaceConference.getUUID(pack.getMessage()));
        if (conf == null) return;

        conf.deserializeUpdate(BOB.getInstance().getTileManager(), pack.getMessage());

        BOB.getInstance().getPeaceHelper().nextRound(conf.getUUID());
    }

    private void onRoundUpdate(ReplyPacket pack) {
        PeaceConference conf = BOB.getInstance().getPeaceHelper().getBy(PeaceConference.getUUID(pack.getMessage()));
        if (conf == null) return;

        conf.deserializeUpdate(BOB.getInstance().getTileManager(), pack.getMessage());
    }

    private void onTileConnectionChange(ReplyPacket pack) {
        String msg = pack.getMessage();

        if (msg.startsWith("broken=")) {
            var pair = Tile.TileConnection.deserializeBroken(msg);
            BOB.getInstance().getTileManager().getConnection(pair.value()).setBrokenSimple(pair.key());
        } else if (msg.startsWith("type=")) {
            var pair = Tile.TileConnection.deserializeType(msg);
            BOB.getInstance().getTileManager().getConnection(pair.value()).setTypeSimple(pair.key());
        }
    }
    private void onTileUpdate(ReplyPacket pack) {
        String msg = pack.getMessage();
        if (msg.isEmpty()) return;

        String[] strings = msg.split(";");

        Tile t = BOB.getInstance().getTileManager().byAbbreviation(valueOf(strings[0], "id"));
        if (t == null) return;

        String s1 = strings[1];

        if (s1.startsWith("population")) t.setPopulation(Double.parseDouble(valueOf(s1, "population")));
        else if (s1.startsWith("addCore")) t.addCore(valueOf(s1, "addCore"));
        else if (s1.startsWith("removeCore")) t.removeCore(valueOf(s1, "removeCore"));
        else if (s1.startsWith("addClaim")) t.addClaim(valueOf(s1, "addClaim"));
        else if (s1.startsWith("removeClaim")) t.removeClaim(valueOf(s1, "removeClaim"));
    }

    private void onTileChange(ReplyPacket pack) {
        String s = pack.getMessage();
        if (s.isEmpty()) return;

        Pair<Tile, Country> pair = Tile.deconstructChange(s, BOB.getInstance().getCountryManager(), BOB.getInstance().getTileManager());

        Tile tile = pair.key();
        if (tile == null) return;

        Country country = pair.value();
        if (country == null) return;

        Color c = country.countryColor() == null ? Color.WHITE : country.countryColor();

        TileChangedEvent.Type t = Tile.getChangeType(s);

        if (Objects.equals(t, TileChangedEvent.Type.OWNER)) tile.setOwner(country);
        else if (Objects.equals(t, TileChangedEvent.Type.CONTROLLER)) {
            tile.setController(country);
            BOB.getInstance().getTileManager().recolorTile(tile, c);
        }
    }

    private void onPlayerChange(ReplyPacket pack, PacketRegistry.PacketReceiveEvent event) {
        String[] parts = pack.getMessage().split(";");

        String abbreviation = parts[1]; // parts[0] is the uuid, ig use this over the channel one?!
        String status = parts[2];

        if (!Boolean.parseBoolean(status)) return;

        Country country = BOB.getInstance().getCountryManager().byAbbreviation(abbreviation);

        //if(country == null) return; ALLOW null countries for spectator

        Player p = BOB.getInstance().getPlayerManager().getPlayer(AddressUtil.getRemoteAddress(event.getChannel()));
        if (p == null) return;

        p.country(country == null ? null : country.getAbbreviation());
    }

    private static String valueOf(String part, String key) {
        return part.substring(key.length() + 1);
    }

    private static UUID parseUuid(String s) {
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}