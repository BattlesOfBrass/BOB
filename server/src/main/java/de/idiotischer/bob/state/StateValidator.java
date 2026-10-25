package de.idiotischer.bob.state;

import de.idiotischer.bob.country.Country;

//ggf full static machen
public class StateValidator {

    public boolean isChangeValid(State state, Country current, Country next) {
        return true;
    }
}
