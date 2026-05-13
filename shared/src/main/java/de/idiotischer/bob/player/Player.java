package de.idiotischer.bob.player;

import de.idiotischer.bob.auth.Credentials;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.troop.Troop;

import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.List;
import java.util.UUID;

public interface Player {
    static Player of(UUID uuid) {
        return null;
    }

    default String name() {
        return "";
    }
    default void name(String name) {}

    UUID uuid();
    void uuid(UUID uuid);

    Country country();

    void country(Country country);

    List<Troop> selectedTroops();

    default InetSocketAddress address() {
        return null;
    }

    default AsynchronousSocketChannel clientChannel() {
        return null;
    }

    default boolean authorized() {
        return true;
    }

    default boolean authorize(Credentials credentials) {return true;}
}
