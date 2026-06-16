package de.idiotischer.bob.combat;

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

    public CombatStatus(Set<TroopStack> attackers, Set<TroopStack> defenders) {
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

    private int getStrength(Set<TroopStack> side) {
        return side.stream().mapToInt(TroopStack::getHp).sum();
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
}