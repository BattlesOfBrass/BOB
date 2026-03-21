package de.idiotischer.bob.networking.player;

import de.idiotischer.bob.networking.country.Country;
import de.idiotischer.bob.networking.troop.Troop;

import java.net.InetSocketAddress;
import java.util.List;

public interface Player {
    Country country();
    void country(Country country);

    List<Troop> selectedTroops();

    InetSocketAddress address();
}
