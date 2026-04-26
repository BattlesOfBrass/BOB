package de.idiotischer.bob.listener;

import de.craftsblock.craftscore.event.EventHandler;
import de.craftsblock.craftscore.event.ListenerAdapter;
import de.idiotischer.bob.Server;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.event.ClientConnectEvent;
import de.idiotischer.bob.networking.packet.PacketRegistry;
import de.idiotischer.bob.networking.packet.impl.*;
import de.idiotischer.bob.networking.packet.impl.pp.RequestPacket;
import de.idiotischer.bob.scenario.Scenario;
import de.idiotischer.bob.scenario.ServerScenarioManager;
import de.idiotischer.bob.scenario.ServerScenarioSceneLoader;
import de.idiotischer.bob.state.State;
import it.unimi.dsi.fastutil.Pair;

public class ServerPacketListener implements ListenerAdapter {
    @EventHandler
    public void onPacketReceive(PacketRegistry.PacketReceiveEvent event) {
        if(event.getPacket() instanceof PingPacket) {
            System.out.println("Ping packet received at: " + System.nanoTime());
            Server.getInstance().getSendTool().send(event.getChannel(), new PongPacket());

        } else if(event.getPacket() instanceof RequestPacket pack) {
            //so ping pong like

            switch (pack.getRequestType()) {
                case SCENARIOS -> {
                    if(!Server.getInstance().getServerSocket().isLocal()) return;

                    Server.getInstance().getSendTool().send(event.getChannel(), ScenariosSyncPacket.fromScenarios(Server.getInstance().getScenarioManager().getScenarios()));
                }
                case STATES_SYNC -> {
                    StatesSyncPacket syncPacket = StatesSyncPacket.fromStates(Server.getInstance().getStateManager().getStateSet());

                    Server.getInstance().getSendTool().send(event.getChannel(), syncPacket);
                }
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
                case STATE_CHANGE -> {
                    String s = pack.getMessage();

                    if(s.isEmpty()) return;

                    Pair<State, Country> pair = State.deconstructChange(s, Server.getInstance().getCountryManager(), Server.getInstance().getStateManager());

                    State state = pair.key();


                    if(state == null) return;

                    Country country = pair.value();

                    if(country == null) return;

                    if(Server.getInstance().getStateValidator().isChangeValid(state, state.getController(), country)) {
                        state.setControllerForAll(Server.getInstance().getServerSocket().getClients(), country);
                    }
                }
            }
        }
    }

}

