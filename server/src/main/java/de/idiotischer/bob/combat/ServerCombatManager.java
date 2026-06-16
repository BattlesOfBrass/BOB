package de.idiotischer.bob.combat;

import de.idiotischer.bob.troop.TroopStack;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class ServerCombatManager {
    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

    private final Set<CombatStatus> activeCombats = ConcurrentHashMap.newKeySet();
    private final ConcurrentMap<TroopStack, CombatStatus> troopCombatMap = new ConcurrentHashMap<>();
    private final Set<Consumer<CombatStatus>> combatListeners = ConcurrentHashMap.newKeySet();
    private final Set<Consumer<TroopStack>> troopDeathListeners = ConcurrentHashMap.newKeySet();

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
        troopCombatMap.clear();

        executor.shutdownNow();
        executor = Executors.newScheduledThreadPool(2);
    }

    public CombatStatus enterCombat(List<TroopStack> attackers, List<TroopStack> defenders) {
        List<TroopStack> validAttackers = attackers.stream().filter(stack -> !isInCombat(stack)).toList();

        List<TroopStack> validDefenders = defenders.stream().filter(stack -> !isInCombat(stack)).toList();

        if (validAttackers.isEmpty() || validDefenders.isEmpty()) return null;

        CombatStatus existing = findCombat(validAttackers, validDefenders);

        if (existing != null) {
            mergeIntoCombat(existing, validAttackers, validDefenders);
            return existing;
        }

        CombatStatus combat = new CombatStatus(new HashSet<>(validAttackers), new HashSet<>(validDefenders));

        activeCombats.add(combat);

        validAttackers.forEach(stack -> troopCombatMap.put(stack, combat));
        validDefenders.forEach(stack -> troopCombatMap.put(stack, combat));

        ScheduledFuture<?> future = executor.scheduleAtFixedRate(() -> tickCombat(combat), 0, 1, TimeUnit.SECONDS);

        combat.setTask(future);

        return combat;
    }

    private void mergeIntoCombat(CombatStatus combat, List<TroopStack> attackers, List<TroopStack> defenders) {
        for (TroopStack stack : attackers) {
            combat.addAttacker(stack);
            troopCombatMap.put(stack, combat);
        }

        for (TroopStack stack : defenders) {
            combat.addDefender(stack);
            troopCombatMap.put(stack, combat);
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

        return activeCombats.stream()
                .filter(c -> !c.isFinished() && !c.getAttackers().isEmpty() && new ArrayList<>(c.getAttackers()).getFirst().getTile().equals(tile))
                .findFirst()
                .orElse(null);
    }

    private void tickCombat(CombatStatus combat) {
        if (combat.isFinished()) {
            cleanupCombat(combat);
            return;
        }

        removeOrg(combat.getAttackers());
        removeOrg(combat.getDefenders());

        int totalAttackDamage = combat.getAttackers().stream().filter(s -> !s.isBroken()).mapToInt(TroopStack::getAttack).sum();
        int totalDefDamage = (int) Math.round(combat.getDefenders().stream().filter(s -> !s.isBroken()).mapToInt(TroopStack::getAttack).sum() * DEFENDER_DAMAGE_NEGATION);

        applyDamage(combat, combat.getDefenders(), totalAttackDamage);
        applyDamage(combat, combat.getAttackers(), totalDefDamage);

        combat.tick();

        if (isCombatOver(combat)) {
            cleanupCombat(combat);
        }
    }

    private void removeOrg(Set<TroopStack> stacks) {
        for (TroopStack stack : stacks) {
            if (stack.getHp() <= 0) continue;

            int oldOrg = stack.getOrg();
            int newOrg = oldOrg - ORG_RM_PER_TICK;

            stack.setOrg(newOrg);
        }
    }

    private void handleOOH(CombatStatus combat, TroopStack stack) {
        combat.getAttackers().remove(stack);
        combat.getDefenders().remove(stack);

        troopCombatMap.remove(stack);

        for (Consumer<TroopStack> listener : troopDeathListeners) {
            try {
                listener.accept(stack);
            } catch (Exception e) {
                e.printStackTrace();
            }
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
    }

    private boolean isCombatOver(CombatStatus combat) {
        boolean attackersDeadOrBroken = combat.getAttackers().stream().allMatch(s -> s.getHp() <= 0 || s.isBroken());
        boolean defendersDeadOrBroken = combat.getDefenders().stream().allMatch(s -> s.getHp() <= 0 || s.isBroken());

        return attackersDeadOrBroken || defendersDeadOrBroken;
    }

    private void cleanupCombat(CombatStatus combat) {
        activeCombats.remove(combat);

        combat.getAttackers().forEach(TroopStack::resetAttributes);
        combat.getDefenders().forEach(TroopStack::resetAttributes);

        combat.getAttackers().forEach(troopCombatMap::remove);
        combat.getDefenders().forEach(troopCombatMap::remove);

        if (combat.getTask() != null) {
            combat.getTask().cancel(false);
        }

        for (Consumer<CombatStatus> listener : combatListeners) {
            try {
                listener.accept(combat);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean joinCombat(CombatStatus combat, TroopStack stack, boolean attackerSide) {
        if (combat == null || combat.isFinished()) return false;
        if (isInCombat(stack)) return false;


        if (attackerSide) combat.addAttacker(stack);
        else combat.addDefender(stack);

        troopCombatMap.put(stack, combat);

        return true;
    }

    public boolean isInCombat(TroopStack stack) {
        return troopCombatMap.containsKey(stack);
    }

    public CombatStatus getCombat(TroopStack stack) {
        return troopCombatMap.get(stack);
    }

    public Optional<CombatStatus> findCombat(TroopStack stack) {
        return Optional.ofNullable(troopCombatMap.get(stack));
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
        troopDeathListeners.add(callback);
    }
}
