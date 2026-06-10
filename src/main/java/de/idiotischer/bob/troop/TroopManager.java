package de.idiotischer.bob.troop;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.networking.packet.impl.pp.RequestPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;
import de.idiotischer.bob.render.menu.components.button.TroopVisualButton;
import de.idiotischer.bob.render.menu.impl.select.ScenarioSelectMenu;
import de.idiotischer.bob.tile.Tile;
import de.idiotischer.bob.util.UUIDUtil;
import it.unimi.dsi.fastutil.Pair;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TroopManager {

    private final Map<UUID, TroopStack> troops = new HashMap<>();

    private CompletableFuture<Void> awaitingFuture = new CompletableFuture<>();

    private Map<UUID, CompletableFuture<Pair<TroopStack, Tile>>> requests = new HashMap<>();

    private boolean switchMM;

    public TroopManager() {
        //reload();
    }

    public CompletableFuture<Pair<TroopStack, Tile>> move(TroopStack selected, Tile newTile) {
        selected.setTile(newTile);

        UUID uuid = getUuid(selected);

        BOB.getInstance().getSendTool().send(BOB.getInstance().getClient().getChannel(), new RequestPacket(Type.TROOPS_MOVE, "troop=" + uuid.toString() + ";tile=" + newTile.getAbbreviation()));

        requests.put(uuid, new CompletableFuture<>());

        return CompletableFuture.completedFuture(null);
    }

    public UUID getUuid(TroopStack troopStack) {
        var uuid = troops.entrySet().stream().filter(entry -> entry.getValue() == troopStack).findFirst().get();

        return uuid.getKey();
    }

    public void finishMove(UUID troopId, Tile newTile) {
        TroopStack troop = troops.get(troopId);

        if(troop == null) return;

        troop.setTile(newTile);
        requests.get(troopId).complete(Pair.of(troop, newTile));
    }

    public CompletableFuture<Void> reload() {
        awaitingFuture = new CompletableFuture<>();

        troops.clear();

        if(BOB.getInstance().getMainRenderer() != null) BOB.getInstance().getMainRenderer().getGamePanel().selected.clear();

        BOB.getInstance().getSendTool().send(BOB.getInstance().getClient().getChannel(), new RequestPacket(Type.TROOPS_SYNC, ""));

        return awaitingFuture;
    }

    public boolean isSwitchMM() {
        return switchMM;
    }

    public void setSwitchMM(boolean switchMM) {
        this.switchMM = switchMM;
    }

    public void finishReload(Map<UUID,TroopStack> troops, boolean withInit) {
        this.troops.putAll(troops);

        if(withInit && !BOB.getInstance().isInitialized()) {
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

        BOB.getInstance().getTileManager().colorAllDefault(); //TODO: gucken ob man das hier für immer lassen kann

        switchMM = true;
        if(awaitingFuture == null || awaitingFuture.isDone()) return;
        awaitingFuture.complete(null);
    }

    public void addTroopStack(TroopStack stack) {
        troops.put(UUIDUtil.getUnused(troops.keySet()), stack);
    }

    public List<TroopStack> getEnemy() {
        return List.of();
    }

    public Map<UUID, TroopStack> getVisible(Country country) {
        return troops;
    }

    public List<TroopStack> getFor(Country country) {
        return List.of();
    }

    public List<TroopStack> getAll() {
        return new ArrayList<>(troops.values());
    }
}
