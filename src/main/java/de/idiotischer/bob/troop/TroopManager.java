package de.idiotischer.bob.troop;

import de.idiotischer.bob.country.Country;

import java.util.ArrayList;
import java.util.List;

public class TroopManager {

    private List<Troop> troops = new ArrayList<>();

    public List<Troop> getEnemy() {
        return List.of();
    }

    public List<Troop> getVisible(Country country) {
        return List.of();
    }

    public List<Troop> getFor(Country country) {
        return List.of();
    }

    public List<Troop> getAll() {
        return troops;
    }
}
