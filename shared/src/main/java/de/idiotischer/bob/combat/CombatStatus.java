package de.idiotischer.bob.combat;

import de.idiotischer.bob.troop.TroopResolver;
import de.idiotischer.bob.troop.TroopStack;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;

public class CombatStatus {

    private final Set<TroopStack> attackers;
    private final Set<TroopStack> defenders;
    private final Set<TroopStack> baseAttackers;
    private final Set<TroopStack> baseDefenders;

    private boolean finished;
    private ScheduledFuture<?> task;
    private final UUID uuid;

    public CombatStatus(UUID uuid, Set<TroopStack> attackers, Set<TroopStack> defenders) {
        this.uuid = uuid;
        this.attackers = new HashSet<>(attackers);
        this.defenders = new HashSet<>(defenders);
        this.baseAttackers = new HashSet<>(attackers);
        this.baseDefenders = new HashSet<>(defenders);
    }

    public void tick() {

        int attackerStrength = getStrength(attackers);
        int defenderStrength = getStrength(defenders);

        if (attackerStrength <= 0) {
            finished = true;
            return;
        }

        if (defenderStrength <= 0) {
            finished = true;
            return;
        }

        damageSide(attackers, defenderStrength);
        damageSide(defenders, attackerStrength);
    }

    private void damageSide(Set<TroopStack> side, int enemyStrength) {

        for (TroopStack stack : side) {

            if (stack.getTroops().isEmpty()) {
                continue;
            }

            if (ThreadLocalRandom.current().nextDouble() < 0.2) {
                stack.getTroops().removeFirst();
            }
        }
    }

    public int getStrength(Set<TroopStack> side) {
        return side.stream().mapToInt(TroopStack::getHp).sum();
    }
    public int getPower(Set<TroopStack> side) {
        return side.stream().mapToInt(TroopStack::getAttack).sum();
    }

    public boolean contains(TroopStack stack) {
        return attackers.contains(stack) || defenders.contains(stack);
    }

    public void addAttacker(TroopStack stack) {
        attackers.add(stack);
        baseAttackers.add(stack);
    }

    public void addDefender(TroopStack stack) {
        defenders.add(stack);
        baseDefenders.add(stack);
    }

    public boolean isFinished() {
        return finished;
    }

    public Set<TroopStack> getAttackers() {
        return attackers;
    }

    public Set<TroopStack> getDefenders() {
        return defenders;
    }

    public Set<TroopStack> getWinner() {

        if (!finished) {
            return null;
        }

        return getStrength(attackers)
                >= getStrength(defenders)
                ? attackers
                : defenders;
    }

    public void setTask(ScheduledFuture<?> task) {
        this.task = task;
    }

    public ScheduledFuture<?> getTask() {
        return task;
    }

    public Set<TroopStack> getBaseDefenders() {
        return baseDefenders;
    }

    public Set<TroopStack> getBaseAttackers() {
        return baseAttackers;
    }

    public String toDataString(TroopResolver resolver) {
        return uuid.toString() + ";" + finished + ";" +
                serializeStacks(attackers,resolver) + ";" +
                serializeStacks(defenders,resolver) + ";" +
                serializeStacks(baseAttackers,resolver) + ";" +
                serializeStacks(baseDefenders,resolver);
    }

    private String serializeStacks(Set<TroopStack> stacks, TroopResolver resolver) {
        StringBuilder sb = new StringBuilder();

        int i = 0;
        for (TroopStack stack : stacks) {
            sb.append(resolver.getUuid(stack).toString());

            if (++i < stacks.size()) {
                sb.append("|");
            }
        }

        return sb.toString();
    }

    public static CombatStatus fromDataString(String data, TroopResolver resolver) {
        String[] split = data.split(";", -1);

        UUID uuid = UUID.fromString(split[0]);
        boolean finished = Boolean.parseBoolean(split[1]);

        Set<TroopStack> attackers = deserializeStacks(split[2], resolver);
        Set<TroopStack> defenders = deserializeStacks(split[3], resolver);
        Set<TroopStack> baseAttackers = deserializeStacks(split[4], resolver);
        Set<TroopStack> baseDefenders = deserializeStacks(split[5], resolver);

        CombatStatus status = new CombatStatus(uuid, attackers, defenders);

        status.baseAttackers.clear();
        status.baseAttackers.addAll(baseAttackers);

        status.baseDefenders.clear();
        status.baseDefenders.addAll(baseDefenders);

        status.finished = finished;

        return status;
    }

    private static Set<TroopStack> deserializeStacks(String data, TroopResolver resolver) {
        Set<TroopStack> result = new HashSet<>();

        if (data == null || data.isEmpty()) {
            return result;
        }

        for (String uuidString : data.split("\\|")) {
            UUID uuid = UUID.fromString(uuidString);

            TroopStack stack = resolver.getTroop(uuid);
            if (stack != null) {
                result.add(stack);
            }
        }

        return result;
    }

    public UUID getUuid() {
        return uuid;
    }
}