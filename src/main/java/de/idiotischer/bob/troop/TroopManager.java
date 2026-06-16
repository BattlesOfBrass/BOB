package de.idiotischer.bob.troop;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.networking.packet.impl.pp.RequestPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;
import de.idiotischer.bob.render.menu.impl.select.ScenarioSelectMenu;
import de.idiotischer.bob.tile.Tile;
import de.idiotischer.bob.util.UUIDUtil;
import it.unimi.dsi.fastutil.Pair;

import javax.swing.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TroopManager {

    private final Map<UUID, TroopStack> troops = new HashMap<>();

    private CompletableFuture<Void> awaitingFuture = new CompletableFuture<>();

    private Map<UUID, CompletableFuture<Pair<TroopStack, Pair<Tile, MoveStatus>>>> requests = new HashMap<>();

    public TroopManager() {
        //reload();
    }

    public CompletableFuture<Pair<TroopStack, Pair<Tile, MoveStatus>>> move(TroopStack selected, Tile newTile) {
        UUID uuid = getUuid(selected);

        CompletableFuture<Pair<TroopStack, Pair<Tile, MoveStatus>>> future =
                new CompletableFuture<>();

        //requests.remove(uuid); could possibly cause bugs?
        requests.put(uuid, future);

        BOB.getInstance().getSendTool().send(BOB.getInstance().getClient().getChannel(), new RequestPacket(Type.TROOPS_MOVE, "troop=" + uuid + ";tile=" + newTile.getAbbreviation()));

        return future;
    }

    public UUID getUuid(TroopStack troopStack) {
        var uuid = troops.entrySet().stream().filter(entry -> entry.getValue() == troopStack).findFirst().get();

        return uuid.getKey();
    }

    public void finishMove(UUID troopId, Tile newTile, MoveStatus moveStatus) {
        TroopStack troop = troops.get(troopId);

        requests.getOrDefault(troopId, new CompletableFuture<>() /*I'm too lazy to null handle this*/).complete(Pair.of(troop, Pair.of(newTile, moveStatus)));
        requests.remove(troopId);

        if(troop == null) return;
        if(newTile == null) return;

        if(moveStatus == MoveStatus.FAILURE_FIGHT || moveStatus == MoveStatus.FAILURE_NO_CONTROL || moveStatus == MoveStatus.FAILURE || moveStatus == MoveStatus.FAILURE_KICKED
                || moveStatus == MoveStatus.FAILURE_IN_COMBAT || moveStatus == MoveStatus.FAILURE_STARTED_PATHFINDING) return;
        troop.setTile(newTile);

        //theoretically already set in the request on the server
        newTile.setControllerClient(BOB.getInstance().getClient().getChannel(), BOB.getInstance().getPlayer().country()); //TODO: combine with troop movement
    }

    public CompletableFuture<Void> reload() {
        awaitingFuture = new CompletableFuture<>();

        troops.clear();

        if(BOB.getInstance().getMainRenderer() != null) BOB.getInstance().getMainRenderer().getGamePanel().selected.clear();

        BOB.getInstance().getSendTool().send(BOB.getInstance().getClient().getChannel(), new RequestPacket(Type.TROOPS_SYNC, ""));

        return awaitingFuture;
    }

    public void finishReload(Map<UUID,TroopStack> troops, boolean withInit) {
        this.troops.putAll(troops);

        BOB.getInstance().getWarManager().reload();

        if(awaitingFuture == null || awaitingFuture.isDone()) return;
        awaitingFuture.complete(null);
    }

    public void addTroopStack(UUID uuid, TroopStack stack) {
        troops.putIfAbsent(uuid, stack);
    }

    public void addTroopStack(TroopStack stack) {
        troops.putIfAbsent(UUIDUtil.getUnused(troops.keySet()), stack);
    }

    public void removeTroopStack(UUID uuid) {
        troops.remove(uuid);
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
