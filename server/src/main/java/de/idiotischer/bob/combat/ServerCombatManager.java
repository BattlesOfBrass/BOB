package de.idiotischer.bob.combat;

import de.idiotischer.bob.Server;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.networking.packet.impl.CombatSyncPacket;
import de.idiotischer.bob.networking.packet.impl.pp.ReplyPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;
import de.idiotischer.bob.tile.Tile;
import de.idiotischer.bob.troop.MoveStatus;
import de.idiotischer.bob.troop.TroopStack;
import de.idiotischer.bob.util.UUIDUtil;
import de.idiotischer.bob.war.WarStatus;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//TODO: troops move in tiles they normally cant bc they are surrounded, why? idk
public class ServerCombatManager {
    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

    private final Set<CombatStatus> activeCombats = ConcurrentHashMap.newKeySet();
    private final Set<Consumer<CombatStatus>> combatListeners = ConcurrentHashMap.newKeySet();
    private final Set<Consumer<TroopStack>> troopOutOfHealthListeners = ConcurrentHashMap.newKeySet();

    private static final double DEFENDER_DAMAGE_NEGATION = 0.4;
    private static final int ORG_RM_PER_TICK = 5;
    private static final double ORG_DAMAGE_MULTIPLIER = 1.5;
    private static final double DAMAGE_VARIANCE = 0.25;

    public void reload() {
        for (CombatStatus combat : activeCombats) {
            ScheduledFuture<?> task = combat.getTask();
            if (task != null) task.cancel(false);
        }

        activeCombats.clear();

        Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().getClients(), new ReplyPacket(Type.CLEAR_COMBATS));

        executor.shutdownNow();
        executor = Executors.newScheduledThreadPool(2);
    }

    public CombatStatus enterCombat(List<TroopStack> attackers, List<TroopStack> defenders) {
        List<TroopStack> validAttackers = attackers.stream().filter(stack -> !isInCombat(stack)).toList();

        List<TroopStack> validDefenders = defenders.stream().filter(stack -> !isInCombat(stack)).toList();

        if (validAttackers.isEmpty() || validDefenders.isEmpty()) return null;

        Set<String> attackerSet = validAttackers.stream().map(t -> t.getController().getAbbreviation()).collect(Collectors.toSet());
        for (TroopStack defender : validDefenders) {
            if (attackerSet.contains(defender.getController().getAbbreviation())) return null;
        }

        CombatStatus existing = findCombat(validAttackers, validDefenders);

        if (existing != null) {
            mergeIntoCombat(existing, validAttackers, validDefenders);
            return existing;
        }

        UUID id = UUIDUtil.getUnused(activeCombats.stream().map(CombatStatus::getUuid).collect(Collectors.toSet()));

        CombatStatus combat = new CombatStatus(id, new HashSet<>(validAttackers), new HashSet<>(validDefenders));

        activeCombats.add(combat);

        broadcast(combat);

        ScheduledFuture<?> future = executor.scheduleAtFixedRate(() -> tickCombat(combat), 1, 1, TimeUnit.SECONDS);

        combat.setTask(future);

        return combat;
    }

    private void mergeIntoCombat(CombatStatus combat, List<TroopStack> attackers, List<TroopStack> defenders) {
        for (TroopStack stack : attackers) combat.addAttacker(stack);

        for (TroopStack stack : defenders) combat.addDefender(stack);

        broadcast(combat);
    }

    public void removeByWar(WarStatus war) {
        Set<String> attackers = war.getAttackers().stream().map(Country::getAbbreviation).collect(Collectors.toSet());

        Set<String> defenders = war.getDefenders().stream().map(Country::getAbbreviation).collect(Collectors.toSet());

        for (CombatStatus combat : new ArrayList<>(activeCombats)) {
            boolean normal = combat.getAttackers().stream().anyMatch(t -> attackers.contains(t.getController().getAbbreviation())) && combat.getDefenders().stream().anyMatch(t -> defenders.contains(t.getController().getAbbreviation()));
            boolean reversed = combat.getAttackers().stream().anyMatch(t -> defenders.contains(t.getController().getAbbreviation())) && combat.getDefenders().stream().anyMatch(t -> attackers.contains(t.getController().getAbbreviation()));

            if (normal || reversed) cleanupCombat(combat);
        }
    }

    private CombatStatus findCombat(List<TroopStack> attackers, List<TroopStack> defenders) {
        Set<Tile> tiles = new HashSet<>();

        attackers.forEach(t -> tiles.add(t.getTile()));
        defenders.forEach(t -> tiles.add(t.getTile()));

        if (tiles.size() != 1) return null;

        Tile tile = tiles.iterator().next();

        return activeCombats.stream().filter(c -> !c.isFinished() && !c.getAttackers().isEmpty() && c.getAttackers().iterator().next().getTile().equals(tile)).findFirst().orElse(null);
    }

    //TODO: add time integration!
    private void tickCombat(CombatStatus combat) {
        try {
            if (combat.isFinished() || !activeCombats.contains(combat)) {
                cleanupCombat(combat);
                return;
            }

            checkRemoved(combat);
            removeMissingTroops(combat);

            removeOrg(combat, combat.getAttackers(), 2);
            removeOrg(combat, combat.getDefenders(), 0); //i dont think this is called on defenders the way its done on attackers in hoi4

            int attackDamage = rollDamage(combat.getAttackers(), 1.0);
            int defendDamage = rollDamage(combat.getDefenders(), DEFENDER_DAMAGE_NEGATION);

            applyDamage(combat, combat.getDefenders(), attackDamage);
            applyDamage(combat, combat.getAttackers(), defendDamage);

            combat.tick();

            if (isCombatOver(combat)) cleanupCombat(combat);
            else broadcast(combat);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void removeByCountry(Country country) {
        for (CombatStatus combat : new ArrayList<>(activeCombats)) {
            List<TroopStack> toRemove = Stream.concat(combat.getAttackers().stream(), combat.getDefenders().stream()).filter(stack -> Objects.equals(stack.getController().getAbbreviation(), country.getAbbreviation())).toList();

            for (TroopStack stack : toRemove) handleOOH(combat, stack);

            if (isCombatOver(combat)) cleanupCombat(combat);
            else if (!toRemove.isEmpty()) broadcast(combat);
        }
    }

    private void broadcast(CombatStatus combat) {
        CombatSyncPacket packet = new CombatSyncPacket(combat, Server.getInstance().getTroopManager());

        Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().getClients(), packet);
    }

    private boolean isActive(TroopStack stack) {
        return stack.getHp() > 0 && stack.isAlive() && !stack.isBroken();
    }

    private void checkRemoved(CombatStatus combat) {
        combat.getAttackers().forEach(c -> {
            if(!c.isAlive()) c.setHp(0);

        });

        combat.getDefenders().forEach(c -> {
            if(!c.isAlive()) c.setHp(0);

        });
    }

    private void removeOrg(CombatStatus combat, Set<TroopStack> stacks, int extra) {
        List<TroopStack> broken = new ArrayList<>();

        for (TroopStack stack : stacks) {
            if (stack.getHp() <= 0) continue;

            stack.setOrg(stack.getOrg() - ORG_RM_PER_TICK - extra);

            if (stack.getOrg() <= 0) broken.add(stack);
        }

        for (TroopStack stack : broken) handleOOH(combat, stack);
    }

    private void removeMissingTroops(CombatStatus combat) {
        var troopManager = Server.getInstance().getTroopManager();

        List<TroopStack> toRemove = Stream.concat(combat.getAttackers().stream(), combat.getDefenders().stream()).filter(stack -> !troopManager.has(stack)).toList();

        for (TroopStack stack : toRemove) {
            stack.setAlive(false);
            handleOOH(combat, stack);
        }
    }

    private void handleOOH(CombatStatus combat, TroopStack stack) {
        combat.getAttackers().remove(stack);
        combat.getDefenders().remove(stack);

        for (Consumer<TroopStack> listener : troopOutOfHealthListeners) {
            try {
                listener.accept(stack);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        var server = Server.getInstance();
        var troopManager = server.getTroopManager();

        if (!troopManager.has(stack)) return;

        if (stack.getHp() <= 0 || !stack.isAlive()) {
            troopManager.removeTroop(stack);
            return;
        }

        List<Tile> fallbacks = new ArrayList<>(server.getTileManager().findNeighbors(stack.getTile()));

        fallbacks.removeIf(tile -> !Objects.equals(tile.getController().getAbbreviation(), stack.getController().getAbbreviation()) && !server.getWarManager().fightsTogetherWith(tile.getController(), stack.getController()));

        Set<Tile> enemyTiles = new HashSet<>();

        for (TroopStack other : troopManager.getTroopStacks().values()) {
            if (other == stack) continue;

            boolean friendly = Objects.equals(other.getController().getAbbreviation(), stack.getController().getAbbreviation()) || server.getWarManager().fightsTogetherWith(other.getController(), stack.getController());

            if (!friendly) enemyTiles.add(other.getTile());
        }

        fallbacks.removeIf(enemyTiles::contains);

        if (fallbacks.isEmpty()) {
            troopManager.removeTroop(stack);
            return;
        }

        Tile fallback = fallbacks.stream().filter(tile -> troopManager.getAt(tile).stream().noneMatch(this::isInCombat)).findFirst().orElse(fallbacks.getFirst());

        String reply = "troop=" + troopManager.getUuid(stack) + ";tile=" + fallback.getAbbreviation() + ";type=" + MoveStatus.SUCCESS.ordinal();

        troopManager.removePathfinding(stack);
        stack.setTile(fallback);

        server.getSendTool().broadcast(server.getServerSocket().getClients(), new ReplyPacket(Type.TROOPS_MOVE, reply));
    }

    private int rollDamage(Set<TroopStack> stacks, double modifier) {
        int attack = stacks.stream().filter(this::isActive).mapToInt(TroopStack::getAttack).sum();

        double roll = 1.0 - DAMAGE_VARIANCE + ThreadLocalRandom.current().nextDouble() * 2 * DAMAGE_VARIANCE;

        return (int) Math.round(attack * modifier * roll);
    }

    private void applyDamage(CombatStatus combat, Set<TroopStack> stacks, int damage) {
        List<TroopStack> targets = stacks.stream().filter(s -> s.getHp() > 0).toList();

        if (targets.isEmpty() || damage <= 0) return;

        int perStack = damage / targets.size();
        int remainder = damage % targets.size();

        List<TroopStack> out = new ArrayList<>();

        for (int i = 0; i < targets.size(); i++) {
            TroopStack stack = targets.get(i);

            int hit = perStack + (i < remainder ? 1 : 0);
            int orgHit = (int) Math.round(hit * ORG_DAMAGE_MULTIPLIER);

            stack.setHp(Math.max(0, stack.getHp() - hit));
            stack.setOrg(Math.max(0, stack.getOrg() - orgHit));

            if (stack.getHp() == 0 || stack.getOrg() == 0) out.add(stack);
        }

        for (TroopStack stack : out) {
            handleOOH(combat, stack);
        }
    }

    private boolean isCombatOver(CombatStatus combat) {
        boolean attackersDeadOrBroken = combat.getAttackers().stream().allMatch(s -> s.getHp() <= 0 || !s.isAlive()/*|| s.isBroken()*/);
        boolean defendersDeadOrBroken = combat.getDefenders().stream().allMatch(s -> s.getHp() <= 0 || !s.isAlive()/*|| s.isBroken()*/);

        return attackersDeadOrBroken || defendersDeadOrBroken;
    }

    private void cleanupCombat(CombatStatus combat) {
        if (combat.getTask() != null) {
            combat.getTask().cancel(false);
        }

        if (!activeCombats.remove(combat)) return;

        combat.getAttackers().forEach(TroopStack::resetAttributes);
        combat.getDefenders().forEach(TroopStack::resetAttributes);

        combatListeners.forEach(l -> {
            try {l.accept(combat); } catch (Exception ignored) {}
        });

        ReplyPacket packet = new ReplyPacket(Type.COMBAT_OVER, combat.getUuid().toString());

        Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().getClients(), packet);
    }

    public boolean isInCombat(TroopStack stack) {
        return getCombat(stack).map(c -> !c.isFinished() && !isCombatOver(c)).orElse(false);
    }

    public Optional<CombatStatus> getCombat(TroopStack stack) {
        return activeCombats.stream().filter(f -> f.getDefenders().contains(stack) || f.getAttackers().contains(stack)).findFirst();
    }

    public Set<CombatStatus> getActiveCombats() {
        return activeCombats;
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    //Dummy for testing
    public TroopStack whoWins(TroopStack a, TroopStack b) {
        if (a.getTroops().size() > b.getTroops().size()) return a;
        if (b.getTroops().size() > a.getTroops().size()) return b;
        return ThreadLocalRandom.current().nextBoolean() ? a : b;
    }

    public Set<TroopStack> whoWins(Set<TroopStack> a, Set<TroopStack> b) {
        int sizeA = a.stream().mapToInt(t -> t.getTroops().size()).sum();
        int sizeB = b.stream().mapToInt(t -> t.getTroops().size()).sum();

        if (sizeA > sizeB) return a;
        if (sizeB > sizeA) return b;
        return ThreadLocalRandom.current().nextBoolean() ? a : b;
    }

    public void onCombatFinished(Consumer<CombatStatus> callback) {
        combatListeners.add(callback);
    }

    public void onTroopDeath(Consumer<TroopStack> callback) {
        troopOutOfHealthListeners.add(callback);
    }
}