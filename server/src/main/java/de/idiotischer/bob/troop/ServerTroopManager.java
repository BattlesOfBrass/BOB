package de.idiotischer.bob.troop;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import de.idiotischer.bob.Server;
import de.idiotischer.bob.SharedCore;
import de.idiotischer.bob.country.Country;
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

    public void reload() {
        troopStacks.clear();

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
        troopStacks.putIfAbsent(UUIDUtil.getUnused(troopStacks.keySet()), troop);
    }

    public void addTroop(Troop troop) {
        troops.add(troop);

        updateStacks();
    }

    private void updateStacks() {
        troopStacks.clear();

        for (Troop troop : troops) {

            TroopStack stack = troopStacks.values().stream()
                    .filter(s ->
                            s.getTemplate().equals(troop.getTemplate()) &&
                                    s.getController().equals(troop.getController()) && s.getTile().equals(troop.getTile()))
                    .findFirst()
                    .orElseGet(() -> {
                        TroopStack newStack = new TroopStack(troop.getTile(),troop.getController(),new ArrayList<>());
                        troopStacks.put(UUIDUtil.getUnused(troopStacks.keySet()),newStack);
                        return newStack;
                    });

            stack.getTroops().add(troop);
        }
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

    public void startMovement(TroopStack troopStack) {
        List<Tile> path = activePathfindings.get(troopStack);

        if (path == null || path.size() < 2) {
            return;
        }

        final int[] i = {1};
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

                if (status == MoveStatus.FAILURE) {
                    activePathfindings.remove(troopStack);
                    scheduledFutures[0].cancel(false);
                    return;
                }

                if(status == MoveStatus.FAILURE_FIGHT) {
                    Set<TroopStack> troops = getAt(to);
                    Set<TroopStack> these = getAt(troopStack.getTile());

                    if(!Server.getInstance().getCombatManager().whoWins(troops, these).contains(troopStack)) {
                        activePathfindings.remove(troopStack);
                        scheduledFutures[0].cancel(false);
                        return;
                    } else {
                        Server.getInstance().getTroopManager().removeTroops(troops);
                    }
                }

                troopStack.setTile(to);

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
        List<UUID> toRemove = troopStacks.entrySet().stream()
                .filter(e -> e.getValue().getController() != null
                        && e.getValue().getController().getAbbreviation()
                        .equals(country.getAbbreviation()))
                .map(Map.Entry::getKey)
                .toList();

        toRemove.forEach(this::removeTroop);
    }

    public void removeTroop(UUID uuid) {
        troopStacks.remove(uuid);

        Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().getClients(), new ReplyPacket(Type.TROOP_REMOVE, uuid.toString()));
    }

    public void removeTroops(Set<TroopStack> stacks) {
        if (stacks == null || stacks.isEmpty()) return;

        for (TroopStack stack : stacks) {
            if (stack == null) continue;

            UUID uuid = getUuidSafe(stack);
            if (uuid != null) {
                removeTroop(uuid);
            }
        }
    }

    public UUID getUuidSafe(TroopStack troopStack) {
        return troopStacks.entrySet().stream()
                .filter(entry -> entry.getValue() == troopStack)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    public void addTroopPath(TroopStack troopStack, List<Tile> path) {
        if (troopStack == null || path == null) {
            return; // fun fact, i get an npe whe nto doing this bc of the concurrent hashmap which is very strict which wa snew to me atp
        }

        activePathfindings.put(troopStack, path);
    }
}
