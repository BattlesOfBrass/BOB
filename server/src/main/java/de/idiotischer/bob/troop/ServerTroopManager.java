package de.idiotischer.bob.troop;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import de.idiotischer.bob.Server;
import de.idiotischer.bob.SharedCore;
import de.idiotischer.bob.combat.CombatStatus;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.networking.packet.impl.TroopStackSyncPacket;
import de.idiotischer.bob.networking.packet.impl.pp.ReplyPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;
import de.idiotischer.bob.tile.Tile;
import de.idiotischer.bob.tile.TileResolver;
import de.idiotischer.bob.util.UUIDUtil;

import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

//gets its own thread prob (at least for movement)
public class ServerTroopManager implements TroopResolver{

    private final List<Troop> troops = new ArrayList<>();
    private final Map<UUID, TroopStack> troopStacks = new HashMap<>();

    private final ScheduledExecutorService movementExecutor = Executors.newScheduledThreadPool(2);

    private final Map<TroopStack, List<Tile>> activePathfindings = new ConcurrentHashMap<>();
    private final Map<TroopStack, Integer> pausedMovementIndex = new ConcurrentHashMap<>();

    public void reload() {
        troopStacks.clear();
        pausedMovementIndex.clear();
        activePathfindings.clear();

        try (JsonReader reader = new JsonReader(Files.newBufferedReader(Server.getInstance().getScenarioSceneLoader().getCurrentScenario().getTroopConfig()))) {
            JsonElement root = SharedCore.GSON.fromJson(reader, JsonElement.class);

            root.getAsJsonObject().entrySet().forEach(entry -> {
                String name = entry.getKey();

                JsonObject countryElement = entry.getValue().getAsJsonObject();

                String template = "";
                String tileAbbr = "";
                String ownerAbbr = "";
                String controllerAbbr;
                int count = 1;

                if(countryElement.has("template") && !countryElement.get("template").isJsonNull()) {
                    template = countryElement.get("template").getAsString();
                }

                if(countryElement.has("owner") && !countryElement.get("owner").isJsonNull()) {
                    ownerAbbr = countryElement.get("owner").getAsString();
                }

                if(countryElement.has("controller") && !countryElement.get("controller").isJsonNull()) {
                    controllerAbbr = countryElement.get("controller").getAsString();
                } else controllerAbbr = ownerAbbr;

                if(countryElement.has("tile") && !countryElement.get("tile").isJsonNull()) {
                    tileAbbr = countryElement.get("tile").getAsString();
                }

                if(countryElement.has("count") && !countryElement.get("count").isJsonNull()) {
                    count = countryElement.get("count").getAsInt();
                }

                Tile tile = Server.getInstance().getTileManager().byAbbreviation(tileAbbr);

                Country owner = Server.getInstance().getCountryManager().byAbbreviation(ownerAbbr);

                if(tile != null && owner != null) {
                    Country controller = Server.getInstance().getCountryManager().byAbbreviation(controllerAbbr);

                    if(controller == null) controller = owner;

                    TroopStack stack = new TroopStack(name,tile,owner,controller,count,template);

                    UUID uuid = UUIDUtil.getUnused(troopStacks.keySet());

                    //troopStacks.remove(uuid); if the uuid weren't unique i'd need this here
                    troopStacks.putIfAbsent(uuid,stack);

                    if(Server.getInstance().isDebug())
                        System.out.println("Added troop stack with info:  owner: "
                                + stack.getOwner().getAbbreviation() + " controller: "
                                + stack.getController().getAbbreviation() + " tile: "
                                + stack.getTile().getAbbreviation()
                        );
                } else {
                    if(Server.getInstance().isDebug())
                        System.out.println("Couldn't add troop stack: " + name + " because the owner or tile couldn't be found!");
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public UUID getUuid(TroopStack troopStack) {
        var uuid = troopStacks.entrySet().stream().filter(entry -> entry.getValue() == troopStack).findFirst().get();

        return uuid.getKey();
    }

    public void addTroopStack(TroopStack troop) {
        UUID uuid = UUIDUtil.getUnused(troopStacks.keySet());

        troopStacks.putIfAbsent(uuid, troop);

        Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().getClients(), new TroopStackSyncPacket(uuid, troop));
    }

    public Set<TroopStack> getAt(Tile tile) {
        return troopStacks.values().stream().filter(s -> s.getTile().equals(tile)).collect(Collectors.toSet());
    }

    public boolean hasStack(Tile tile) {
        return getAt(tile) != null && !getAt(tile).isEmpty();
    }


    public List<TroopStack> getEnemy(Country country) {
        return List.of();
    }

    public List<TroopStack> getVisible(Country country) {
        return troopStacks.values().stream().filter(s -> s.getController() != null && s.getController().equals(country) && s.isVisible()).toList();
    }

    public List<TroopStack> getForOwner(Country country) {
        return troopStacks.values().stream().filter(s -> s.getOwner() != null && s.getOwner().equals(country)).toList();
    }

    public List<TroopStack> getForController(Country country) {
        return troopStacks.values().stream().filter(s -> s.getController() != null && s.getController().equals(country)).toList();
    }

    public void removePathfinding(TroopStack troop) {
        activePathfindings.remove(troop);
    }

    public Map<UUID,TroopStack> getTroopStacks() {
        return troopStacks;
    }

    public List<Troop> getTroops() {
        return troops;
    }

    public TroopStack getTroop(UUID uuid) {
        return troopStacks.get(uuid);
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

                if (visited.contains(neighbour)) continue;

                if (canTraverse(troopController, current, neighbour) == MoveStatus.FAILURE) continue;

                visited.add(neighbour);
                previous.put(neighbour, current);
                queue.add(neighbour);
            }
        }

        return null;
    }

    @Override
    public MoveStatus canTraverse(Country troopController, Tile from, Tile to) {
        if(Server.getInstance().getConferenceManager().anyActive()) return MoveStatus.FAILURE;

        Country fromController = from.getController();
        Country toController = to.getController();

        if(!Objects.equals(toController.getAbbreviation(), troopController.getAbbreviation())) if (!Server.getInstance().getWarManager().isAtWar(troopController, toController)) return MoveStatus.FAILURE;

        if(Server.getInstance().getWarManager().fightsTogetherWith(troopController, fromController) || Server.getInstance().getWarManager().isAtWar(fromController, toController)) if(!Objects.equals(fromController.getAbbreviation(), toController.getAbbreviation()) && !Objects.equals(fromController.getAbbreviation(), troopController.getAbbreviation())) return MoveStatus.FAILURE;

        if(hasStack(to)) if(Server.getInstance().getWarManager().isEnemy(troopController, toController)) return MoveStatus.FAILURE_FIGHT;

        return MoveStatus.SUCCESS;
    }

    public void startMovement(TroopStack troopStack) {
        startMovement(troopStack, 1);
    }

    private void startMovement(TroopStack troopStack, int startIndex) {
        List<Tile> path = activePathfindings.get(troopStack);

        if (path == null || path.size() < 2) return;

        final int[] i = {startIndex};
        ScheduledFuture<?>[] scheduledFutures = new ScheduledFuture<?>[1];

        scheduledFutures[0] = movementExecutor.scheduleAtFixedRate(() -> {
            try {
                List<Tile> currentPath = activePathfindings.get(troopStack);

                if (currentPath == null || currentPath.size() < 2) {
                    scheduledFutures[0].cancel(false);
                    return;
                }

                if (i[0] >= currentPath.size()) {
                    activePathfindings.remove(troopStack);
                    scheduledFutures[0].cancel(false);
                    return;
                }

                Tile from = troopStack.getTile();
                Tile to = currentPath.get(i[0]);

                MoveStatus status = canTraverse(troopStack.getController(), from, to);

                if(Server.getInstance().getCombatManager().isInCombat(troopStack)) {
                    activePathfindings.remove(troopStack);
                    scheduledFutures[0].cancel(false);
                    return;
                }

                if (status == MoveStatus.FAILURE) {
                    activePathfindings.remove(troopStack);
                    scheduledFutures[0].cancel(false);
                    return;
                }

                if (status == MoveStatus.FAILURE_FIGHT) {
                    Set<TroopStack> enemyStacks = getAt(to);
                    Set<TroopStack> ownStacks = getAt(troopStack.getTile());

                    CombatStatus combatHere = Server.getInstance().getCombatManager().enterCombat(new ArrayList<>(ownStacks), new ArrayList<>(enemyStacks));

                    if (combatHere == null) return;

                    pausedMovementIndex.put(troopStack, i[0]);

                    Server.getInstance().getCombatManager().onCombatFinished(combat -> {
                        List<Tile> path1 = activePathfindings.get(troopStack);

                        if (path1 == null || path1.size() < 2) return;

                        var attackers = combat.getAttackers();
                        var defenders = combat.getDefenders();

                        List<TroopStack> all = new ArrayList<>();
                        all.addAll(attackers);
                        all.addAll(defenders);

                        //if(combat.whoWon() == CombatStatus.Side.DEFENDER) {
                        //    for (TroopStack stack : all) {
                        //        pausedMovementIndex.remove(stack);
                        //    }
                        //    return;
                        //}

                        Set<TroopStack> pushable = getAt(to);

                        if(!pushable.isEmpty()) {
                            List<Tile> fallbacks = new ArrayList<>(Server.getInstance().getTileManager().findNeighbors(to));

                            fallbacks.removeIf(tile1 -> !Objects.equals(tile1.getController().getAbbreviation(), new ArrayList<>(pushable).getFirst().getController().getAbbreviation()) && !Server.getInstance().getWarManager().fightsTogetherWith(tile1.getController(), new ArrayList<>(pushable).getFirst().getController()));

                            if (!fallbacks.isEmpty()) {
                                Tile tile = fallbacks.getFirst();

                                pushable.forEach(p -> {
                                    String reply = "troop=" + getUuid(p) + ";tile=" + tile.getAbbreviation() + ";type=" + MoveStatus.SUCCESS.ordinal();

                                    Server.getInstance().getTroopManager().removePathfinding(p);
                                    p.setTile(tile);

                                    if (Server.getInstance().getWarManager().isAtWar(tile.getController(), p.getController())) {
                                        Country c = troopStack.getController();

                                        to.setControllerForAll(Server.getInstance().getServerSocket().channels(), c);

                                        Server.getInstance().getWarManager().checkWarOver( troopStack.getController(), tile.getController(), tile);
                                    }

                                    Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().getClients(), new ReplyPacket(Type.TROOPS_MOVE, reply));
                                });

                                for (TroopStack stack : all) {
                                    Integer idx = pausedMovementIndex.remove(stack);
                                    if (idx != null) startMovement(stack, idx);
                                }
                            } else {
                                pushable.forEach(p -> {Server.getInstance().getTroopManager().removeTroop(p);});
                            }
                        }
                    });

                    scheduledFutures[0].cancel(false);
                    return;
                }

                troopStack.setTile(to);
                if (Server.getInstance().getWarManager().isAtWar(to.getController(), troopStack.getController())) {
                    Country def = to.getController();

                    to.setControllerForAll(Server.getInstance().getServerSocket().channels(), troopStack.getController());

                    Server.getInstance().getWarManager().checkWarOver( troopStack.getController(), def, to);
                }

                String reply = "troop=" + getUuid(troopStack) + ";tile=" + to.getAbbreviation() + ";type=" + MoveStatus.SUCCESS_PATHFIND.ordinal();
                Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().getClients(), new ReplyPacket(Type.TROOPS_MOVE, reply));

                i[0]++;

                if (i[0] >= currentPath.size()) {
                    activePathfindings.remove(troopStack);
                    scheduledFutures[0].cancel(false);
                }

            } catch (Exception e) {
                e.printStackTrace();
                activePathfindings.remove(troopStack);
                scheduledFutures[0].cancel(false);
            }
        }, 0, 500, TimeUnit.MILLISECONDS);
    }


    public void removeTroops(Country country) {
        List<UUID> toRemove = troopStacks.entrySet().stream().filter(e -> e.getValue().getController() != null && e.getValue().getController().getAbbreviation().equals(country.getAbbreviation())).map(Map.Entry::getKey).toList();

        toRemove.forEach(this::removeTroop);
    }

    public boolean has(TroopStack stack) {
        return troopStacks.containsKey(getUuid(stack));
    }

    public boolean has(UUID troop) {
        return troopStacks.containsKey(troop);
    }

    public void removeTroop(TroopStack troopStack) {
        UUID uuid = getUuid(troopStack);
        removePathfinding(troopStack);
        troopStack.setAlive(false);
        troopStacks.remove(uuid);

        Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().getClients(), new ReplyPacket(Type.TROOP_REMOVE, uuid.toString()));
    }

    public void removeTroop(UUID uuid) {
        TroopStack troopStack = troopStacks.get(uuid);
        removePathfinding(troopStack);
        troopStack.setAlive(false);
        troopStacks.remove(uuid);

        Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().getClients(), new ReplyPacket(Type.TROOP_REMOVE, uuid.toString()));
    }

    public UUID getUuidSafe(TroopStack troopStack) {
        return troopStacks.entrySet().stream().filter(entry -> entry.getValue() == troopStack).map(Map.Entry::getKey).findFirst().orElse(null);
    }

    public void addTroopPath(TroopStack troopStack, List<Tile> path) {
        if (troopStack == null || path == null) return; // fun fact, i get an npe whe nto doing this bc of the concurrent hashmap which is very strict which wa snew to me atp

        activePathfindings.put(troopStack, path);
    }

    public void setStationedTileForAll(TroopStack troop, Tile t) {
        Server.getInstance().getTroopManager().removePathfinding(troop);
        troop.setTile(t);

        String reply = "troop=" + getUuid(troop) + ";tile=" + t.getAbbreviation();

        Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().getClients(), new ReplyPacket(Type.TROOP_TP, reply));

    }
}
