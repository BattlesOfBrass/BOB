package de.idiotischer.bob.player;

import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.troop.Troop;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.UUID;

public interface Player {
    static Player of(UUID uuid) {
        return null;
    }

    UUID uuid();

    Country country();

    void country(Country country);

    List<Troop> selectedTroops();

    InetSocketAddress address();
}
