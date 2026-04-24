package de.idiotischer.bob.troop;

import de.idiotischer.bob.country.Country;

import java.util.ArrayList;
import java.util.List;

public class TroopManager {

    private List<TroopStack> troops = new ArrayList<>();

    public List<TroopStack> getEnemy() {
        return List.of();
    }

    public List<TroopStack> getVisible(Country country) {
        return List.of();
    }

    public List<TroopStack> getFor(Country country) {
        return List.of();
    }

    public List<TroopStack> getAll() {
        return troops;
    }
}
