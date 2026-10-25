package de.idiotischer.bob.war;

import de.idiotischer.bob.country.Country;

public interface WarResolver {
    boolean fightsTogetherWith(Country one, Country two);
}
