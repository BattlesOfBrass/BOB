package de.idiotischer.bob.war;

import de.idiotischer.bob.country.Country;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public interface WarResolver {
    boolean fightsTogetherWith(Country one, Country two);
    boolean isAtWar(Country country);
    boolean isAtWar(Country a, Country b) ;

    boolean isAtWar(String country) ;

    boolean isAtWar(String a, String b) ;

    Set<WarStatus> getOrCreateWars(Country c);

    boolean isEnemy(Country a, Country b);
}
