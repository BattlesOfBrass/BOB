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

public class ServerCombatManager {
    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

    private final Set<CombatStatus> activeCombats = new HashSet<>();
    private final Set<Consumer<CombatStatus>> combatListeners = ConcurrentHashMap.newKeySet();
    private final Set<Consumer<TroopStack>> troopOutOfHealthListeners = ConcurrentHashMap.newKeySet();

    private static final double DEFENDER_DAMAGE_NEGATION = 0.4;
    private static final int ORG_RM_PER_TICK = 5;
    private static final double ORG_DAMAGE_MULTIPLIER = 1.5;

    public void reload() {
        for (CombatStatus combat : activeCombats) {
            ScheduledFuture<?> task = combat.getTask();
            if (task != null) {
                task.cancel(false);
            }
        }

        activeCombats.clear();

        Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().getClients(), new ReplyPacket(Type.CLEAR_COMBATS));

        executor.shutdownNow();
        executor = Executors.newScheduledThreadPool(2);
    }

    public CombatStatus enterCombat(List<TroopStack> attackers, List<TroopStack> defenders) {
        List<TroopStack> validAttackers = attackers.stream().filter(stack -> !isInCombat(stack)).toList();

        List<TroopStack> validDefenders = defenders.stream().filter(stack -> !isInCombat(stack)).toList();

        Set<String> attackerSet = new HashSet<>(validAttackers).stream().map(t -> t.getController().getAbbreviation()).collect(Collectors.toSet());
        for (TroopStack defender : validDefenders) {
            if (attackerSet.contains(defender.getController().getAbbreviation())) {
                return null;
            }
        }

        if (validAttackers.isEmpty() || validDefenders.isEmpty()) return null;

        CombatStatus existing = findCombat(validAttackers, validDefenders);

        if (existing != null) {
            mergeIntoCombat(existing, validAttackers, validDefenders);
            return existing;
        }

        UUID id = UUIDUtil.getUnused(activeCombats.stream().map(CombatStatus::getUuid).collect(Collectors.toSet()));

        CombatStatus combat = new CombatStatus(id, new HashSet<>(validAttackers), new HashSet<>(validDefenders));

        activeCombats.add(combat);

        ScheduledFuture<?> future = executor.scheduleAtFixedRate(() -> tickCombat(combat), 0, 1, TimeUnit.SECONDS);

        combat.setTask(future);

        CombatSyncPacket packet = new CombatSyncPacket(combat, Server.getInstance().getTroopManager());

        Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().getClients(), packet);

        return combat;
    }

    private void mergeIntoCombat(CombatStatus combat, List<TroopStack> attackers, List<TroopStack> defenders) {
        for (TroopStack stack : attackers) {
            combat.addAttacker(stack);
        }

        for (TroopStack stack : defenders) {
            combat.addDefender(stack);
        }

        CombatSyncPacket packet = new CombatSyncPacket(combat, Server.getInstance().getTroopManager());

        Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().getClients(), packet);
    }

    public void removeByWar(WarStatus war) {
        Set<String> attackers = war.getAttackers().stream().map(Country::getAbbreviation).collect(Collectors.toSet());

        Set<String> defenders = war.getDefenders().stream().map(Country::getAbbreviation).collect(Collectors.toSet());

        for (CombatStatus combat : new ArrayList<>(activeCombats)) {
            boolean normal = combat.getAttackers().stream().anyMatch(t -> attackers.contains(t.getController().getAbbreviation())) && combat.getDefenders().stream().anyMatch(t -> defenders.contains(t.getController().getAbbreviation()));
            boolean reversed = combat.getAttackers().stream().anyMatch(t -> defenders.contains(t.getController().getAbbreviation())) && combat.getDefenders().stream().anyMatch(t -> attackers.contains(t.getController().getAbbreviation()));

            if (normal || reversed) {
                cleanupCombat(combat);
            }
        }
    }

    private CombatStatus findCombat(List<TroopStack> attackers, List<TroopStack> defenders) {
        Set<Object> tiles = new HashSet<>();

        attackers.forEach(t -> tiles.add(t.getTile()));
        defenders.forEach(t -> tiles.add(t.getTile()));

        if (tiles.size() != 1) {
            return null;
        }

        Object tile = tiles.iterator().next();

        return activeCombats.stream().filter(c -> !c.isFinished() && !c.getAttackers().isEmpty() && new ArrayList<>(c.getAttackers()).getFirst().getTile().equals(tile)).findFirst().orElse(null);
    }

    private void tickCombat(CombatStatus combat) {
        if (combat.isFinished() || !activeCombats.contains(combat)) {
            cleanupCombat(combat);
            return;
        }

        checkRemoved(combat);
        removeMissingTroops(combat);

        Set<TroopStack> attackers = combat.getAttackers();
        Set<TroopStack> defenders = combat.getDefenders();

        removeOrg(combat, attackers, 2);
        removeOrg(combat, defenders, 0); //i dont think this is called on defenders the way its done on attackers in hoi4

        int attackDamage = attackers.stream().filter(s -> s.getHp() > 0 && !s.isBroken()).mapToInt(TroopStack::getAttack).sum();
        int defendDamage = (int) Math.round(defenders.stream().filter(s -> s.getHp() > 0 && !s.isBroken()).mapToInt(TroopStack::getAttack).sum() * DEFENDER_DAMAGE_NEGATION);

        applyDamage(combat, defenders, attackDamage);
        applyDamage(combat, attackers, defendDamage);

        combat.tick();

        if (isCombatOver(combat)) {
            cleanupCombat(combat);
        } else {
            broadcast(combat);
        }
    }

    public void removeByCountry(Country country) {
        for (CombatStatus combat : new ArrayList<>(activeCombats)) {
            List<TroopStack> toRemove = Stream.concat(
                            combat.getAttackers().stream(),
                            combat.getDefenders().stream()
                    )
                    .filter(stack -> Objects.equals(stack.getController().getAbbreviation(), country.getAbbreviation()))
                    .toList();

            for (TroopStack stack : toRemove) {
                handleOOH(combat, stack);
            }

            if (isCombatOver(combat)) {
                cleanupCombat(combat);
            }
        }
    }

    private void broadcast(CombatStatus combat) {
        CombatSyncPacket packet = new CombatSyncPacket(combat, Server.getInstance().getTroopManager());

        Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().getClients(), packet);
    }

    private void checkRemoved(CombatStatus combat) {
        combat.getAttackers().forEach(c -> {
            if(!c.isAlive()) {
                c.setHp(0);
            }
        });

        combat.getDefenders().forEach(c -> {
            if(!c.isAlive()) {
                c.setHp(0);
            }
        });
    }

    private void removeOrg(CombatStatus combat, Set<TroopStack> stacks, int extra) {
        for (TroopStack stack : stacks) {
            if (stack.getHp() <= 0) {
                continue;
            }

            int oldOrg = stack.getOrg();
            int newOrg = oldOrg - ORG_RM_PER_TICK - extra;

            stack.setOrg(newOrg);

            if (stack.getOrg() <= 0) {
                handleOOH(combat, stack);
            }
        }
    }

    private void removeMissingTroops(CombatStatus combat) {
        var troopManager = Server.getInstance().getTroopManager();

        List<TroopStack> toRemove = new ArrayList<>();

        for (TroopStack stack : combat.getAttackers()) {
            if (!troopManager.has(stack)) {
                stack.setAlive(false);
                toRemove.add(stack);
            }
        }

        for (TroopStack stack : combat.getDefenders()) {
            if (!troopManager.has(stack)) {
                stack.setAlive(false);
                toRemove.add(stack);
            }
        }

        for (TroopStack stack : toRemove) {
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

        CombatSyncPacket packet = new CombatSyncPacket(combat, Server.getInstance().getTroopManager());

        Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().getClients(), packet);

        List<Tile> fallbacks = new ArrayList<>(Server.getInstance().getTileManager().findNeighbors(stack.getTile()));

        fallbacks.removeIf(tile ->
                !Objects.equals(tile.getController().getAbbreviation(), stack.getController().getAbbreviation()) && !Server.getInstance().getWarManager().fightsTogetherWith(tile.getController(), stack.getController())
        );

        if (!fallbacks.isEmpty()) {
            Tile fallback = fallbacks.getFirst();

            String reply = "troop=" + Server.getInstance().getTroopManager().getUuid(stack) + ";tile=" + fallback.getAbbreviation() + ";type=" + MoveStatus.SUCCESS.ordinal();

            Server.getInstance().getTroopManager().removePathfinding(stack);
            stack.setTile(fallback);

            Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().getClients(), new ReplyPacket(Type.TROOPS_MOVE, reply));
        } else {
            Server.getInstance().getTroopManager().removeTroop(stack);
        }
    }

    private void applyDamage(CombatStatus combat, Set<TroopStack> stacks, int damage) {
        if (stacks.isEmpty()) return;

        int perStack = damage / stacks.size();
        if (perStack == 0 && damage > 0) perStack = 1;

        List<TroopStack> dead = new ArrayList<>();

        for (TroopStack stack : stacks) {
            int oldHp = stack.getHp();
            int newHp = Math.max(0, oldHp - perStack);

            stack.setHp(newHp);

            if (oldHp > 0 && newHp == 0) {
                dead.add(stack);
            }
        }

        for (TroopStack deadStack : dead) {
            handleOOH(combat, deadStack);
        }

        CombatSyncPacket packet = new CombatSyncPacket(combat, Server.getInstance().getTroopManager());

        Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().getClients(), packet);
    }

    private boolean isCombatOver(CombatStatus combat) {
        boolean attackersDeadOrBroken = combat.getAttackers().stream().allMatch(s -> s.getHp() <= 0 || !s.isAlive()/*|| s.isBroken()*/);
        boolean defendersDeadOrBroken = combat.getDefenders().stream().allMatch(s -> s.getHp() <= 0 || !s.isAlive()/*|| s.isBroken()*/);

        return attackersDeadOrBroken || defendersDeadOrBroken;
    }

    private void cleanupCombat(CombatStatus combat) {
        activeCombats.remove(combat);

        if (combat.getTask() != null) {
            combat.getTask().cancel(false);
        }

        combat.getAttackers().forEach(TroopStack::resetAttributes);
        combat.getDefenders().forEach(TroopStack::resetAttributes);

        combatListeners.forEach(l -> {
            try {l.accept(combat); } catch (Exception ignored) {}
        });

        ReplyPacket packet = new ReplyPacket(Type.COMBAT_OVER, combat.getUuid().toString());

        Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().getClients(), packet);
    }

    public boolean isInCombat(TroopStack stack) {
        Optional<CombatStatus> status = getCombat(stack);

        if(status.isEmpty()) return false;

        boolean isOver = isCombatOver(status.orElse(null));

        boolean finished = status.get().isFinished();

        return isOver && finished;
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
