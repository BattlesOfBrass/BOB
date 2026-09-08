package de.idiotischer.bob.state;

import de.idiotischer.bob.conference.PeaceConference;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.country.CountryResolver;
import de.idiotischer.bob.tile.Tile;
import de.idiotischer.bob.tile.TileResolver;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public record State(Country owner, List<Tile> tiles, String abbreviation, String name) {

    public Country getController() {
        return tiles.stream()
                .map(Tile::getController)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    public String serialize() {
        return "owner=" + (owner != null ? owner.getAbbreviation() : "")
                + ";tiles=["
                + tiles.stream()
                .map(Tile::getAbbreviation)
                .collect(Collectors.joining(";"))
                + "]"
                + ";abbreviation=" + abbreviation
                + ";name=" + name;
    }

    public static State deserialize(String data, CountryResolver countryResolver, TileResolver tileResolver) {
        Map<String, String> values = Arrays.stream(data.split(";"))
                .map(s -> s.split("=", 2))
                .filter(a -> a.length == 2)
                .collect(Collectors.toMap(a -> a[0], a -> a[1]));

        Country owner = countryResolver.byAbbreviation(values.get("owner"));

        String tilesRaw = values.get("tiles");
        List<Tile> tiles = List.of();

        if (tilesRaw != null) {
            if (tilesRaw.startsWith("[") && tilesRaw.endsWith("]")) {
                tilesRaw = tilesRaw.substring(1, tilesRaw.length() - 1);
            }

            if (!tilesRaw.isBlank()) {
                tiles = Arrays.stream(tilesRaw.split(";"))
                        .map(tileResolver::byAbbreviation)
                        .toList();
            }
        }

        return new State(owner, tiles, values.get("abbreviation"), values.get("name"));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof State other)) return false;

        return Objects.equals(abbreviation, other.abbreviation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(abbreviation);
    }
}