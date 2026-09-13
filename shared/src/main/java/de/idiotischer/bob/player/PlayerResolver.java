package de.idiotischer.bob.player;

import de.idiotischer.bob.country.Country;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.UUID;

public interface PlayerResolver {
    Player resolve(UUID uuid);

    Player resolve(InetSocketAddress address);

    //could be multiple (although i haven't coded it to be this way but theoretically multiple can play one country bc of the setting)
    List<Player> resolve(Country country);
}
