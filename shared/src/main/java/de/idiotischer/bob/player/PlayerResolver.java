package de.idiotischer.bob.player;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.UUID;

public interface PlayerResolver {
    Player resolve(UUID uuid);

    Player resolve(InetSocketAddress address);
}
