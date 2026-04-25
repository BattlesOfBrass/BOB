package de.idiotischer.bob.player;

import de.idiotischer.bob.Server;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.country.CountryResolver;
import de.idiotischer.bob.networking.packet.impl.PlayerChangedCountryPacket;
import de.idiotischer.bob.networking.packet.impl.PlayerJoinPacket;
import de.idiotischer.bob.networking.packet.impl.PlayerQuitPacket;
import de.idiotischer.bob.networking.packet.impl.pp.RequestPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;
import de.idiotischer.bob.util.UUIDUtil;
import it.unimi.dsi.fastutil.Pair;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.stream.Collectors;

public class PlayerManager implements PlayerResolver {

    private final Set<Player> players = new HashSet<>();

    public Player createPlayer(InetSocketAddress address) {
        List<InetSocketAddress> addresses = players.stream().map(Player::address).toList();

        if(addresses.contains(address)) return resolve(address);

        Set<UUID> used = players.stream()
                .map(Player::uuid)
                .collect(Collectors.toSet());

        return new ServerPlayer(address,null, UUIDUtil.getUnused(used));
    }

    public Player createPlayer(UUID uuid, InetSocketAddress address) {
        List<InetSocketAddress> addresses = players.stream().map(Player::address).toList();

        if(addresses.contains(address)) return resolve(address);

        return new ServerPlayer(address,null, uuid);
    }


    public void changeCountry(UUID uuid, Country country) {
        Player p = resolve(uuid);

        changeCountry(p, country);
    }

    public void resetCountries() {
        players.forEach(p -> {
            changeCountry(p,null);
        });
    }

    public void changeCountry(Player player, Country country) {
        if(player == null) return;

        if(!hasPlayer(country) || Server.getInstance().getServerSocket().getHostUtil().isCoop()) player.country(country);

        Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().getClients(),
                new RequestPacket(Type.PLAYER_CHANGE, constructChange(player, country))
        );
    }

    public void addPlayer(UUID uuid) {
        Player player = resolve(uuid);

        addPlayer(player);
    }

    public void addPlayer(Player player) {
        players.removeIf(p -> player.uuid().equals(player.uuid()));

        players.add(player);
    }

    public void removePlayer(UUID uuid) {
        Player player = resolve(uuid);

        removePlayer(player);
    }

    public void removePlayer(Player player) {
        players.remove(player);
    }

    public boolean hasPlayer(Country c) {
        return players.stream().anyMatch(p -> p.country() != null && p.country().equals(c));
    }

    public String constructChange(Player player, Country country) {
        return player.uuid() + ";" + country.getAbbreviation();
    }

    public Pair<Player,Country> deconstructChange(CountryResolver r, String s) {
        String[] split = s.split(";");

        UUID uuid =  UUID.fromString(split[0]);
        Country c = r.byAbbreviation(split[1]);

        return Pair.of(this.resolve(uuid), c);
    }

    @Override
    public Player resolve(@NotNull UUID uuid) {
        return players.stream().filter(p -> uuid.equals(p.uuid())).findFirst().orElse(null);
    }

    @Override
    public Player resolve(@NotNull InetSocketAddress address) {
        return players.stream().filter(p -> address.equals(p.address())).findFirst().orElse(null);
    }
}
