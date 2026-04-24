package de.idiotischer.bob.player;

import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.troop.Troop;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.UUID;

//TODO: implement brain
public class ArtificalPlayer implements Player {
    @Override
    public UUID uuid() {
        return null;
    }

    @Override
    public Country country() {
        return null;
    }

    @Override
    public void country(Country country) {

    }

    @Override
    public List<Troop> selectedTroops() {
        return List.of();
    }

    @Override
    public InetSocketAddress address() {
        return null;
    }
}
