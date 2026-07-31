package de.idiotischer.bob.conference;

import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.state.State;
import de.idiotischer.bob.tile.Tile;
import it.unimi.dsi.fastutil.Pair;

import java.util.List;
import java.util.Map;

public record ConferenceStatus(Map<Country, Integer> baseVP, Map<Country, Integer> currentVP, /*The stuff that we inherit from WarStatus*/
                               List<Country> losers,
                               List<Country> winners,
                               Map<Country, List<State>> willAnnex,
                               Map<Country, List<Pair<State, Country>>> willPuppet,
                               Map<Country, List<Pair<State, Country>>> willLiberate
) {
    public Pair<List<Country>, State> getNationDispute(State state) {
        return null;
    }

    public boolean isDisputed(State state) {
        return false;
    }
}
