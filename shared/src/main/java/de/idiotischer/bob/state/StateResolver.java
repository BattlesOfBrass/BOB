package de.idiotischer.bob.state;

import de.idiotischer.bob.country.Country;

public interface StateResolver {

    State resolve(String abbreviation);
}
