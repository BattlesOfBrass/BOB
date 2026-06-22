package de.idiotischer.bob.conference;

import de.idiotischer.bob.country.Country;
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
    private Map<Country, List<Tile>> defeatPoints = new HashMap<>();



    public Map<Pair<Country, Country>, List<Tile>> getDisputed() {
        return Map.of();
    }
}
