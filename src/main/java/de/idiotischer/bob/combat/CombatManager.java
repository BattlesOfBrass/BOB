package de.idiotischer.bob.combat;

import de.idiotischer.bob.troop.TroopStack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class CombatManager {
    private final Set<CombatStatus> activeCombats = new HashSet<>();

    public void addStatus(List<CombatStatus> statuses) {
        statuses.forEach(this::addStatus);
    }

    public void addStatus(CombatStatus status) {
        remove(status.getUuid());
        if(!status.isFinished()) activeCombats.add(status);
    }

    public void remove(UUID uuid) {
        activeCombats.removeIf(combat -> combat.getUuid().equals(uuid));
    }

    public void clear() {
        activeCombats.clear();
    }

    public boolean isInCombat(TroopStack stack) {
        Set<TroopStack> all = activeCombats.stream()
                .flatMap(combat -> combat.getAttackers().stream())
                .collect(Collectors.toSet());
        all.addAll(activeCombats.stream()
                .flatMap(combat -> combat.getDefenders().stream())
                .collect(Collectors.toSet()));


        return all.contains(stack);
    }

    public CombatStatus getCombat(TroopStack stack) {
        return activeCombats.stream().filter(f -> f.getDefenders().contains(stack) || f.getAttackers().contains(stack)).findFirst().get();
    }

    public CombatStatus getCombat(UUID stack) {
        return activeCombats.stream().filter(f -> f.getUuid().equals(stack)).findFirst().get();
    }

    public Set<CombatStatus> getActiveCombats() {
        return activeCombats;
    }
}
