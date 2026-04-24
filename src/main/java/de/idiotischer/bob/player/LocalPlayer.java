package de.idiotischer.bob.player;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.troop.Troop;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.UUID;

public class LocalPlayer implements Player {
    private Country country;

    public LocalPlayer(Country country) {
        this.country = country;
    }

    @Override
    public UUID uuid() {
        return null;
    }

    @Override
    public Country country() {
        return country;
    }

    @Override
    public void country(Country country) {
        this.country = country;
    }

    @Override
    public List<Troop> selectedTroops() {
        return List.of();
    }

    @Override
    public InetSocketAddress address() {
        return new InetSocketAddress(BOB.getInstance().getClient().getHost(), BOB.getInstance().getClient().getPort());
    }
}
