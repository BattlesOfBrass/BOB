package de.idiotischer.bob.country;

import de.idiotischer.bob.tile.Tile;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.stream.Stream;

public interface CountryResolver {
    Country byAbbreviation(String abbreviation);

    default Country byJson(String json) {
        return null;
    }

    List<Tile> getOwned(Country country);
    List<Tile> getControlled(Country country);

    default List<Tile> getAllTiles(Country country) {
        return Stream.concat(
                getOwned(country).stream(),
                getControlled(country).stream()
        ).toList();
    }

    boolean isAllied(Country a, Country b);
    boolean anyAlliedWith(List<Country> testers, Country country);
}
