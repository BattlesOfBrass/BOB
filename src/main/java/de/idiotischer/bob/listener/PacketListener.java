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
import de.idiotischer.bob.player.Player;
import de.idiotischer.bob.scenario.Scenario;
import de.idiotischer.bob.scenario.ScenarioManager;
import de.idiotischer.bob.state.State;
import de.idiotischer.bob.state.StateManager;
import it.unimi.dsi.fastutil.Pair;

import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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

            BOB.getInstance().getScenarioSceneLoader().load(scenario, false);
        } else if(event.getPacket() instanceof CountriesSyncPacket packet) {
            Set<Country> countries = packet.getPackets().stream().map(CountrySyncPacket::getCountry).collect(Collectors.toSet());

            countries.forEach(c -> BOB.getInstance().getCountryManager().registerCountry(c));

            BOB.getInstance().getCountryManager().finishReload();
        } else if(event.getPacket() instanceof StatesSyncPacket packet) {

            if(BOB.getInstance().getStateManager().getAwaitingFuture() == null
                    || BOB.getInstance().getStateManager().getAwaitingFuture().isCancelled()
                    || BOB.getInstance().getStateManager().getAwaitingFuture().isDone()
            ) return;

            List<StateSyncPacket> packs = packet.getPackets();

            packs.forEach(s -> s.reconstruct(BOB.getInstance().getSharedCore(), BOB.getInstance().getCountryManager()));

            Set<State> states = packs.stream().map(StateSyncPacket::getState).collect(Collectors.toSet());

            states.forEach(s -> {
                BOB.getInstance().getStateManager().registerState(s);
            });

            BOB.getInstance().getStateManager().finishReload(true);
        } else if(event.getPacket() instanceof CountrySyncPacket packet) {
            Country country = packet.getCountry();

            if(BOB.getInstance().getCountryManager().has(country)) return;

            BOB.getInstance().getCountryManager().registerCountry(country);

        } else if(event.getPacket() instanceof StateSyncPacket packet) {
            State state = packet.getState();

            if(BOB.getInstance().getStateManager().has(state)) return;

            BOB.getInstance().getStateManager().registerState(state);
        } else if(event.getPacket() instanceof ReplyPacket pack) {
            switch (pack.getReplyType()) {
                case STATE_CHANGE -> {
                    String s = pack.getMessage();

                    if(s.isEmpty()) return;

                    Pair<State, Country> pair = State.deconstructChange(s, Server.getInstance().getCountryManager(), Server.getInstance().getStateManager());

                    State state = pair.key();

                    if(state == null) return;

                    Country country = pair.value();

                    if(country == null) return;

                    Color c = country.countryColor() == null ? Color.WHITE : country.countryColor() ;

                    state.setControllerFinish(country, BOB.getInstance().isDebug());
                    StateManager.recolorState(state, c);
                }
                case ERROR -> {}
            }
        } else if(event.getPacket() instanceof PlayerJoinPacket pack) {
            if(BOB.getInstance().getPlayerManager().hasPlayer(pack.getUuid())) return;

            if(BOB.getInstance().getPlayerManager().hasPlayer(pack.getAddress())) {
                //für local sync
                BOB.getInstance().getPlayerManager().getPlayer(pack.getAddress()).uuid(pack.getUuid());
            }

            Player p = BOB.getInstance().getPlayerManager().createPlayer(pack.getUuid(), pack.getAddress());

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
        }
    }
}