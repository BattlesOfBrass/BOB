package de.idiotischer.bob.combat;

import de.idiotischer.bob.troop.TroopStack;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;

public class ServerCombatManager {
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

    public void enterCombat(TroopStack a, TroopStack b) {}

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
}
