package de.idiotischer.bob.conference;

import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.country.CountryResolver;
import de.idiotischer.bob.tile.Tile;
import de.idiotischer.bob.war.WarStatus;
import it.unimi.dsi.fastutil.Pair;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.*;

public class PeaceConference {

    private final CountryResolver resolver;
    private final Set<AsynchronousSocketChannel> channels;
    private Map<Country, Integer> baseVP;
    private Map<Country, Integer> currentVP;

    private final List<Country> defeated;
    private final List<Country> winners;

    public PeaceConference(CountryResolver resolver, Map<Country, Integer> baseVP, Map<Country, Integer> currentVP, Set<Country> defeated, Set<Country> winners ) {
        this(null,resolver,baseVP,currentVP,defeated,winners);
    }

    public PeaceConference(Set<AsynchronousSocketChannel> channels, CountryResolver resolver, Map<Country, Integer> baseVP, Map<Country, Integer> currentVP, Set<Country> defeated, Set<Country> winners ) {
        this.channels = channels;
        this.resolver = resolver;
        this.baseVP = baseVP;
        this.currentVP = currentVP;
        this.defeated = new ArrayList<>(defeated);
        this.winners = new ArrayList<>(winners);
    }

    public List<Tile> getTilesToReinstate() { //for example if i take lithuania and its  my ally? idk what i wrote there but its to add the countries back on the map if they were allied to you
        return defeated.stream().flatMap(c -> {

            List<Tile> tiles = resolver.getControlled(c);

            tiles.removeIf(t -> resolver.anyAlliedWith(winners, t.getOwner()));

            return tiles.stream();
        }).toList();
    }

    //server side method since i use controllerforall and thats a networking method
    public void reinstate() {
        if(channels == null) return;

        getTilesToReinstate().forEach(t -> t.setControllerForAll(channels, t.getOwner()));
    }

    public List<Tile> getTakableTiles(CountryResolver resolver) {
        return defeated.stream().flatMap(c -> resolver.getOwned(c).stream()).toList();
    }

    public Map<Pair<Country, Country>, List<Tile>> getDisputed() {
        return Map.of();
    }


    public PeaceConference fromWar(Set<AsynchronousSocketChannel> channels, CountryResolver resolver, @NonNull WarStatus status) {
        if(!status.isEnded()) return null;
        return new PeaceConference(channels, resolver, status.getBaseVP(), status.getCurrentVP(),
                status.hasAttackingWon() ? status.getDefenders() : status.getAttackers(),
                status.hasAttackingWon() ? status.getAttackers() : status.getDefenders()
        );
    }

    @ApiStatus.Obsolete
    public PeaceConference fromWar(CountryResolver resolver, @NonNull WarStatus status) {
        if(!status.isEnded()) return null;
        return new PeaceConference(resolver, status.getBaseVP(), status.getCurrentVP(),
                status.hasAttackingWon() ? status.getDefenders() : status.getAttackers(),
                status.hasAttackingWon() ? status.getAttackers() : status.getDefenders()
        );
    }
}
