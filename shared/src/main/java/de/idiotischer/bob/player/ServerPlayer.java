package de.idiotischer.bob.player;

import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.troop.Troop;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.UUID;

public class ServerPlayer implements Player {

    private final InetSocketAddress address;
    private UUID uuid;
    private Country country;

    public ServerPlayer(InetSocketAddress address, Country country, UUID uuid) {
        this.country = country;
        this.address = address;
        this.uuid = uuid;
    }

    @Override
    public UUID uuid() {
        return uuid;
    }

    @Override
    public void uuid(UUID uuid) {
        this.uuid = uuid;
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
        return address;
    }
}
