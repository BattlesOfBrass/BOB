package de.idiotischer.bob.conference;

import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.country.CountryResolver;
import de.idiotischer.bob.tile.Tile;
import it.unimi.dsi.fastutil.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PeaceConference {

    private List<Country> defeated = new ArrayList<>();
    private List<Country> winners = new ArrayList<>();

    private Map<Country, Integer> victoryPoints = new HashMap<>();

    public List<Tile> getTilesToReinstate(CountryResolver resolver) { //for example if i take lithuania and its
        return defeated.stream().flatMap(c -> {

            List<Tile> tiles = resolver.getControlled(c);

            tiles.removeIf(t -> resolver.anyAlliedWith(winners, t.getOwner()));

            return tiles.stream();
        }).toList();
    }

    public List<Tile> getTakableTiles(CountryResolver resolver) {
        return defeated.stream().flatMap(c -> resolver.getOwned(c).stream()).toList();
    }


    public Map<Pair<Country, Country>, List<Tile>> getDisputed() {
        return Map.of();
    }
}
