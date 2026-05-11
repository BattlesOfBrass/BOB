package de.idiotischer.bob.player;

import de.idiotischer.bob.auth.Credentials;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.troop.Troop;

import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.List;
import java.util.UUID;

public class ServerPlayer implements Player {

    private final InetSocketAddress address;
    private boolean authorized;
    private UUID uuid;
    private Country country;
    private final AsynchronousSocketChannel clientChannel;
    private String name;

    public ServerPlayer(InetSocketAddress address, Country country, UUID uuid, AsynchronousSocketChannel clientChannel) {
        this.name = "";
        this.country = country;
        this.address = address;
        this.authorized = false;
        this.uuid = uuid;
        this.clientChannel = clientChannel;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void name(String name) {
        this.name = name;
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

    @Override
    public AsynchronousSocketChannel clientChannel() {
        return clientChannel;
    }

    @Override
    public boolean authorized() {
        return authorized;
    }

    @Override
    public void authorize(Credentials credentials) {
        if(credentials == null) return;
        //if(!"somepwfromtheserverwhichidonthaveaccesstosinceweareinshared".equals(credentials.lobbyPW())) return;

        this.name(credentials.username());
        this.authorized = true;
    }
}
