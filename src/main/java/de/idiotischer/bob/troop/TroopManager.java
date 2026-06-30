package de.idiotischer.bob.troop;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.Server;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.networking.packet.impl.pp.RequestPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;
import de.idiotischer.bob.render.menu.impl.select.ScenarioSelectMenu;
import de.idiotischer.bob.tile.Tile;
import de.idiotischer.bob.tile.TileResolver;
import de.idiotischer.bob.util.UUIDUtil;
import it.unimi.dsi.fastutil.Pair;

import javax.swing.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class TroopManager implements TroopResolver{

    private final Map<UUID, TroopStack> troops = new HashMap<>();

    private CompletableFuture<Void> awaitingFuture = new CompletableFuture<>();

    private Map<UUID, CompletableFuture<Pair<TroopStack, Pair<Tile, MoveStatus>>>> requests = new HashMap<>();

    private final Map<Set<UUID>, CompletableFuture<Set<Pair<TroopStack, Pair<Tile, MoveStatus>>>>> bundledRequests = new HashMap<>();

    public CompletableFuture<Set<Pair<TroopStack, Pair<Tile, MoveStatus>>>> moveAll(Set<TroopStack> troops, Tile tile) {
        CompletableFuture<Set<Pair<TroopStack, Pair<Tile, MoveStatus>>>> future = new CompletableFuture<>();

        Set<UUID> uuids = troops.stream().map(this::getUuid).collect(Collectors.toSet());

        bundledRequests.put(uuids, future);

        String uuidString = uuids.stream().map(UUID::toString).collect(Collectors.joining(","));

        BOB.getInstance().getSendTool().send(BOB.getInstance().getClient().getChannel(), new RequestPacket(Type.TROOPS_MOVE, "troops=" + uuidString + ";tile=" + tile.getAbbreviation()));

        return future;
    }

    public CompletableFuture<Pair<TroopStack, Pair<Tile, MoveStatus>>> move(TroopStack selected, Tile newTile) {
        UUID uuid = getUuid(selected);

        CompletableFuture<Pair<TroopStack, Pair<Tile, MoveStatus>>> future = new CompletableFuture<>();

        //requests.remove(uuid); could possibly cause bugs?
        requests.put(uuid, future);

        BOB.getInstance().getSendTool().send(BOB.getInstance().getClient().getChannel(), new RequestPacket(Type.TROOPS_MOVE, "troop=" + uuid + ";tile=" + newTile.getAbbreviation()));

        return future;
    }

    @Override
    public List<Tile> findPath(TroopStack troopStack, Tile destination, TileResolver resolver) {
        Country troopController = troopStack.getController();

        Map<Tile, Tile> previous = new HashMap<>();
        Set<Tile> visited = new HashSet<>();
        Queue<Tile> queue = new LinkedList<>();

        Tile start = troopStack.getTile();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            Tile current = queue.poll();

            if (current.equals(destination)) {
                LinkedList<Tile> path = new LinkedList<>();

                Tile step = destination;

                while (step != null) {
                    path.addFirst(step);
                    step = previous.get(step);
                }

                return path;
            }

            for (Tile neighbour : resolver.findNeighbors(current)) {

                if (visited.contains(neighbour)) {
                    continue;
                }

                if (canTraverse(troopController, current, neighbour) == MoveStatus.FAILURE) {
                    continue;
                }

                visited.add(neighbour);
                previous.put(neighbour, current);
                queue.add(neighbour);
            }
        }

        return null;
    }

    @Override
    public MoveStatus canTraverse(Country troopController, Tile from, Tile to) {
        Country fromController = from.getController();
        Country toController = to.getController();

        if(!Objects.equals(toController.getAbbreviation(), troopController.getAbbreviation())) {
            if (!Server.getInstance().getWarManager().isAtWar(troopController, toController)) return MoveStatus.FAILURE;
        }

        if(Server.getInstance().getWarManager().fightsTogetherWith(troopController, fromController) ||
                Server.getInstance().getWarManager().isAtWar(fromController, toController)) {
            if(!Objects.equals(fromController.getAbbreviation(), toController.getAbbreviation()) &&
                    !Objects.equals(fromController.getAbbreviation(), troopController.getAbbreviation())) {
                return MoveStatus.FAILURE;
            }
        }

        if(hasStack(to)) {
            if(Server.getInstance().getWarManager().isEnemy(troopController, toController)) {
                return MoveStatus.FAILURE_FIGHT;
            }
        }

        return MoveStatus.SUCCESS;
    }

    public Set<TroopStack> getAt(Tile tile) {
        return troops.values().stream().filter(s -> s.getTile().equals(tile)).collect(Collectors.toSet());
    }

    public boolean hasStack(Tile tile) {
        return getAt(tile) != null && !getAt(tile).isEmpty();
    }


    public UUID getUuid(TroopStack troopStack) {
        var uuid = troops.entrySet().stream().filter(entry -> entry.getValue() == troopStack).findFirst().get();

        return uuid.getKey();
    }

    public TroopStack getTroop(UUID uuid) {
        return troops.get(uuid);
    }

    public void finishMoveAll(Map<UUID, MoveStatus> results, Tile newTile) {
        Set<Pair<TroopStack, Pair<Tile, MoveStatus>>> resultSet = new HashSet<>();

        for (var entry : results.entrySet()) {
            UUID troopId = entry.getKey();
            MoveStatus moveStatus = entry.getValue();

            TroopStack troop = troops.get(troopId);

            resultSet.add(Pair.of(troop, Pair.of(newTile, moveStatus)));

            if (troop == null || newTile == null)
                continue;

            if (moveStatus == MoveStatus.FAILURE_FIGHT || moveStatus == MoveStatus.FAILURE_NO_CONTROL || moveStatus == MoveStatus.FAILURE
                    || moveStatus == MoveStatus.FAILURE_KICKED || moveStatus == MoveStatus.FAILURE_IN_COMBAT || moveStatus == MoveStatus.FAILURE_STARTED_PATHFINDING)
                continue;

            troop.setTile(newTile);

            newTile.setControllerClient(BOB.getInstance().getClient().getChannel(), troop.getController());
        }

        CompletableFuture<Set<Pair<TroopStack, Pair<Tile, MoveStatus>>>> future =
                bundledRequests.remove(new HashSet<>(results.keySet()));

        if (future != null) {
            future.complete(resultSet);
        }
    }
    public void finishMove(UUID troopId, Tile newTile, MoveStatus moveStatus) {
        TroopStack troop = troops.get(troopId);

        requests.getOrDefault(troopId, new CompletableFuture<>()/*I'm too lazy to null handle this*/).complete(Pair.of(troop, Pair.of(newTile, moveStatus)));
        requests.remove(troopId);

        if(troop == null) return;
        if(newTile == null) return;

        if(moveStatus == MoveStatus.FAILURE_FIGHT || moveStatus == MoveStatus.FAILURE_NO_CONTROL || moveStatus == MoveStatus.FAILURE || moveStatus == MoveStatus.FAILURE_KICKED
                || moveStatus == MoveStatus.FAILURE_IN_COMBAT || moveStatus == MoveStatus.FAILURE_STARTED_PATHFINDING) return;
        troop.setTile(newTile);

        //theoretically already set in the request on the server
        newTile.setControllerClient(BOB.getInstance().getClient().getChannel(), troop.getController());
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
