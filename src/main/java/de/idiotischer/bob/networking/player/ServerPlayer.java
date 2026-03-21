package de.idiotischer.bob.networking.player;

import de.idiotischer.bob.networking.country.Country;

import java.net.InetSocketAddress;

public class ServerPlayer extends LocalPlayer {
    private final InetSocketAddress address;

    public ServerPlayer(InetSocketAddress address, Country country) {
        super(country);
        this.address = address;
    }

    @Override
    public InetSocketAddress address() {
        return address;
    }
}
