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
    private final Map<Country, Integer> baseVP;
    private final Map<Country, Integer> currentVP;

    private final List<Country> defeated;
    private final List<Country> winners;

    private final Map<Tile, List<Pair<Country, Integer>>> tileClaims = new HashMap<>();
    private final Map<Tile, Country> finalizedAnnexations = new HashMap<>();

    private Country currentTurn;
    private int turnCount;

    public PeaceConference(CountryResolver resolver, Map<Country, Integer> baseVP, Map<Country, Integer> currentVP, Set<Country> defeated, Set<Country> winners) {
        this(null, resolver, baseVP, currentVP, defeated, winners);
    }

    public PeaceConference(Set<AsynchronousSocketChannel> channels, CountryResolver resolver, Map<Country, Integer> baseVP, Map<Country, Integer> currentVP, Set<Country> defeated, Set<Country> winners) {
        this.channels = channels;
        this.resolver = resolver;
        this.baseVP = baseVP;
        this.currentVP = currentVP;
        this.defeated = new ArrayList<>(defeated);
        this.winners = new ArrayList<>(winners);
    }

    public void startConference() {
        turnCount = 0;
        tileClaims.clear();
        finalizedAnnexations.clear();

        if (winners.isEmpty()) {
            currentTurn = null;
            return;
        }

        sortVP();
        currentTurn = winners.getFirst();
    }

    public void nextTurn() {
        if (winners.isEmpty()) {
            currentTurn = null;
            return;
        }

        int currentIndex = winners.indexOf(currentTurn);

        if (currentIndex == -1 || currentIndex >= winners.size() - 1) {
            turnCount++;
            sortVP();
            currentTurn = winners.getFirst();
        } else {
            currentTurn = winners.get(currentIndex + 1);
        }
    }

    public boolean proposeTileAnnex(Tile tile, Country country) {
        if (!isItsTurn(country) || finalizedAnnexations.containsKey(tile)) {
            return false;
        }

        List<Pair<Country, Integer>> claims = tileClaims.getOrDefault(tile, Collections.emptyList());
        if (!claims.isEmpty() && claims.getLast().left().equals(country)) {
            return false;
        }

        int cost = calculateTileCost(tile);
        int available = currentVP.getOrDefault(country, 0);

        if (available < cost) {
            return false;
        }

        currentVP.put(country, available - cost);
        dispute(tile, country);

        return true;
    }

    public void dispute(Tile tile, Country country) {
        tileClaims.computeIfAbsent(tile, k -> new ArrayList<>())
                .add(Pair.of(country, turnCount));
    }

    public int calculateTileCost(Tile tile) {
        int baseCost = Math.max(1, tile.getVictoryPoints());
        List<Pair<Country, Integer>> claims = tileClaims.getOrDefault(tile, Collections.emptyList());

        if (claims.isEmpty()) {
            return baseCost;
        }

        int contestCount = claims.size();
        return baseCost + (contestCount * 2) + turnCount;
    }

    public boolean isDisputed(Tile tile) {
        List<Pair<Country, Integer>> claims = tileClaims.getOrDefault(tile, Collections.emptyList());
        return claims.size() > 1;
    }

    public Map<Pair<Country, Country>, List<Tile>> getDisputed() {
        Map<Pair<Country, Country>, List<Tile>> disputedMap = new HashMap<>();

        for (Map.Entry<Tile, List<Pair<Country, Integer>>> entry : tileClaims.entrySet()) {
            List<Pair<Country, Integer>> claims = entry.getValue();
            if (claims.size() > 1) {
                Country firstClaimant = claims.get(0).left();
                Country secondClaimant = claims.get(claims.size() - 1).left();
                Pair<Country, Country> pair = Pair.of(firstClaimant, secondClaimant);

                disputedMap.computeIfAbsent(pair, k -> new ArrayList<>()).add(entry.getKey());
            }
        }
        return disputedMap;
    }

    public void endConference() {
        for (Map.Entry<Tile, List<Pair<Country, Integer>>> entry : tileClaims.entrySet()) {
            Tile tile = entry.getKey();
            List<Pair<Country, Integer>> claims = entry.getValue();

            if (!claims.isEmpty()) {
                Country winner = claims.getLast().left();
                finalizedAnnexations.put(tile, winner);
                tile.setOwner(winner);

                if (channels != null) {
                    tile.setControllerForAll(channels, winner);
                }
            }
        }

        reinstate();
    }

    public List<Tile> getTilesToReinstate() {
        return defeated.stream().flatMap(c -> {
            List<Tile> tiles = resolver.getControlled(c);
            tiles.removeIf(t -> resolver.anyAlliedWith(winners, t.getOwner()));
            return tiles.stream();
        }).toList();
    }

    public List<Tile> getTakableTiles() {
        return defeated.stream().flatMap(c -> {
            List<Tile> tiles = resolver.getControlled(c);
            tiles.removeIf(t -> !resolver.anyAlliedWith(winners, t.getOwner()));
            return tiles.stream();
        }).toList();
    }

    public void reinstate() {
        if (channels == null) return;

        getTilesToReinstate().forEach(t -> {
            t.setControllerForAll(channels, t.getOwner());
        });
    }

    public List<Tile> getTakableTiles(CountryResolver resolver) {
        return defeated.stream()

                .flatMap(c -> resolver.getOwned(c).stream())
                .filter(tile -> !finalizedAnnexations.containsKey(tile))
                .toList();
    }

    public boolean isItsTurn(Country country) {
        return country != null && currentTurn != null && country.equals(currentTurn);
    }

    private void sortVP() {
        winners.sort((c1, c2) -> Integer.compare(
                currentVP.getOrDefault(c2, 0),
                currentVP.getOrDefault(c1, 0)
        ));
    }

    public PeaceConference fromWar(Set<AsynchronousSocketChannel> channels, CountryResolver resolver, @NonNull WarStatus status) {
        if (!status.isEnded()) return null;
        return new PeaceConference(channels, resolver, status.getBaseVP(), status.getCurrentVP(),
                status.hasAttackingWon() ? status.getDefenders() : status.getAttackers(),
                status.hasAttackingWon() ? status.getAttackers() : status.getDefenders()
        );
    }

    @ApiStatus.Obsolete
    public PeaceConference fromWar(CountryResolver resolver, @NonNull WarStatus status) {
        if (!status.isEnded()) return null;
        return new PeaceConference(resolver, status.getBaseVP(), status.getCurrentVP(),
                status.hasAttackingWon() ? status.getDefenders() : status.getAttackers(),
                status.hasAttackingWon() ? status.getAttackers() : status.getDefenders()
        );
    }

    public List<Country> getWinners() {
        return winners;
    }

    public List<Country> getDefeated() {
        return defeated;
    }

    //TODO: add methods to update parts for the client like for example the guy that has the current turn etc and add serializers
}