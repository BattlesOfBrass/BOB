package de.idiotischer.bob.player;

import de.idiotischer.bob.SharedCore;
import de.idiotischer.bob.auth.Credentials;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.country.CountryResolver;
import de.idiotischer.bob.troop.Troop;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class ServerPlayer implements Player {

    private final InetSocketAddress address;
    private final CountryResolver r;
    private boolean authorized;
    private UUID uuid;
    private String country;
    private final AsynchronousSocketChannel clientChannel;
    private String name;

    public ServerPlayer(InetSocketAddress address, String country, UUID uuid, CountryResolver r, AsynchronousSocketChannel clientChannel) {
        this.name = "";
        this.country = country;
        this.address = address;
        this.authorized = false;
        this.uuid = uuid;
        this.clientChannel = clientChannel;
        this.r = r;
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
    @Nullable
    public Country country() {
        return r.byAbbreviation(country);
    }

    @Override
    public void country(String country) {
        this.country = country;

        Country c = country();
        if(c != null) c.setPlayer(this);
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
    public boolean authorize(Credentials credentials) {
        if (credentials == null) return false;

        String name = credentials.username();

        if(name == null) name = "Player";
        if(name.isEmpty()) name = "Player";

        int max = SharedCore.getMaxNameLength();

        if (name.length() > max) name = name.substring(0, max);

        this.name(name);
        this.authorized = true;

        return true;
    }

    @Override
    public boolean hasOP() {
        return true;
    }
}
