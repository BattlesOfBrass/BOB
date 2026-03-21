package de.idiotischer.bob.networking.player;

import de.idiotischer.bob.networking.BOB;
import de.idiotischer.bob.networking.country.Country;
import de.idiotischer.bob.networking.troop.Troop;

import java.net.InetSocketAddress;
import java.util.List;

public class LocalPlayer implements Player {
    private Country country;

    public LocalPlayer(Country country) {
        this.country = country;
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
