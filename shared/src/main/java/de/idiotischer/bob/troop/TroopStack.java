package de.idiotischer.bob.troop;

import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.state.State;

import java.util.List;

public record TroopStack(List<Troop> troops) {

    public Country getOwner() {
        return null;
    }

    public Country getController() {
        return null;
    }

    public boolean isVisible() {
        return true;
    }

    public State getState() {
        return null;
    }
}
