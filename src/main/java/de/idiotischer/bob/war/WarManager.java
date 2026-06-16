package de.idiotischer.bob.war;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.Server;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.networking.packet.impl.pp.ReplyPacket;
import de.idiotischer.bob.networking.packet.impl.pp.RequestPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;
import de.idiotischer.bob.render.menu.impl.select.ScenarioSelectMenu;
import de.idiotischer.bob.tile.Tile;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class WarManager {

    private final Map<String, Set<WarStatus>> activeWars = new ConcurrentHashMap<>();

    private CompletableFuture<Void> awaitingFuture = new CompletableFuture<>();
    private boolean switchMM;

    public CompletableFuture<Void> reload() {
        awaitingFuture = new CompletableFuture<>();

        activeWars.clear();

        if(BOB.getInstance().getMainRenderer() != null) BOB.getInstance().getMainRenderer().getGamePanel().selected.clear();

        BOB.getInstance().getSendTool().send(BOB.getInstance().getClient().getChannel(), new RequestPacket(Type.WARS_SYNC, ""));

        return awaitingFuture;
    }

    public Set<WarStatus> getWars(Country c) {
        return activeWars.getOrDefault(c.getAbbreviation(), Collections.emptySet());
    }

    public boolean isAtWar(Country a, Country b) {
        if(Objects.equals(a.getAbbreviation(), b.getAbbreviation())) return false;
        return !Collections.disjoint(getWars(a), getWars(b));
    }

    public boolean fightsTogetherWith(Country one, Country two) {
        return getWars(one).stream().anyMatch(w ->
                w.getAttackers().stream().anyMatch(ally ->
                        ally.getAbbreviation().equals(two.getAbbreviation())
                )
        );
    }

    private Set<WarStatus> getOrCreateWars(Country c) {
        return activeWars.computeIfAbsent(
                c.getAbbreviation(),
                k -> ConcurrentHashMap.newKeySet()
        );
    }

    public void finishReload(String message) {
        deserializeWars(message);

        if(!BOB.getInstance().isInitialized()) {
            BOB.getInstance().setup();
            switchMM = false;
        }

        if(BOB.getInstance().getMainRenderer() == null) return;

        if(switchMM) {
            BOB.getInstance().getMainRenderer().getGamePanel().setEscMenu(false);
            BOB.getInstance().getMainRenderer().setMainMenu(false);
            BOB.getInstance().getMainRenderer().getMenuPanel().setInScenarioSelect(false);
            BOB.getInstance().getMainRenderer().getMenuPanel().setScenarioSelectMenu(new ScenarioSelectMenu(BOB.getInstance().getScenarioSceneLoader().getCurrentScenario()));
        }

        if(BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getMapImage() != null) {
            //TODO: check if i need this 2x
            BOB.getInstance().getMainRenderer().setMap(BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getMapImage());//, currentScenario.getBackgroundImage());

            SwingUtilities.invokeLater(() -> {
                if( BOB.getInstance().getMainRenderer().getGamePanel() == null) return;
                BOB.getInstance().getMainRenderer().setMap(BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getMapImage());//,currentScenario.getBackgroundImage());
                BOB.getInstance().getMainRenderer().getCamera().zoomToMin();
            });
        }

        BOB.getInstance().getTileManager().colorAllDefault();

        switchMM = true;
        if(awaitingFuture == null || awaitingFuture.isDone()) return;
        awaitingFuture.complete(null);
    }

    public boolean isSwitchMM() {
        return switchMM;
    }

    public void setSwitchMM(boolean switchMM) {
        this.switchMM = switchMM;
    }

    public void deserializeWars(String data) {
        activeWars.clear();

        if (data == null || data.isEmpty()) return;

        for (String line : data.split("\n")) {
            if (line.isBlank()) continue;

            String[] parts = line.split("#", 2);

            String countryAbbr = parts[0];
            String warData = parts[1];

            WarStatus war = WarStatus.fromString(warData, Server.getInstance().getCountryManager());

            activeWars.computeIfAbsent(countryAbbr, k -> ConcurrentHashMap.newKeySet()).add(war);
        }
    }

    public void addWar(String abbreviation, WarStatus status) {
        activeWars.computeIfAbsent(abbreviation, k -> ConcurrentHashMap.newKeySet()).add(status);
    }

    public void endWar(String abbreviation, WarStatus status) {
        activeWars.computeIfPresent(abbreviation, (k, wars) -> {
            wars.remove(status);
            return wars.isEmpty() ? null : wars;
        });
    }
}