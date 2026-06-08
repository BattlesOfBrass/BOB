package de.idiotischer.bob.country;

import org.jetbrains.annotations.ApiStatus;

public interface CountryResolver {
    Country byAbbreviation(String abbreviation);

    default Country byJson(String json) {
        return null;
    }
}
