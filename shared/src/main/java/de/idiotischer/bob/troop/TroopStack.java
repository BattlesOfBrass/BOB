package de.idiotischer.bob.troop;

import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.state.State;

import java.util.List;

public class TroopStack extends Troop {

    private final List<Troop> troops;

    public TroopStack(List<Troop> troops) {
        this.troops = troops;
    }

    public Country getOwner() {
        return null;
    }

    public List<Troop> getTroops() {
        return troops;
    }
}
