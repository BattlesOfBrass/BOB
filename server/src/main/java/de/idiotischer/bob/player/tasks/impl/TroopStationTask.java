package de.idiotischer.bob.player.tasks.impl;

import de.idiotischer.bob.Server;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.player.tasks.Task;
import de.idiotischer.bob.tile.Tile;
import de.idiotischer.bob.troop.TroopStack;

import java.util.*;

public class TroopStationTask implements Task {

    @Override
    public boolean canExecute(String country) {
        return !getBorderTiles(country).isEmpty();
    }

    @Override
    public double getPriority(String country) {
        if (!canExecute(country)) return 0;

        return Server.getInstance().getWarManager().isAtWar(country) ? 90 : 40;
    }

    @Override
    public void tick(String country) {
        Set<Tile> borderTiles = getBorderTiles(country);

        if (borderTiles.isEmpty()) return;
        List<TroopStack> troops = Server.getInstance().getTroopManager().getForController(country);

        int maxDefenseTroops = (int) Math.floor(troops.size()/* * 0.75*/); //later make ai unstack etc

        List<TroopStack> availableTroops = troops.stream().filter(t -> !borderTiles.contains(t.getTile())).toList();

        long alreadyDefending = troops.stream().filter(t -> borderTiles.contains(t.getTile())).count();

        int remainingCapacity = Math.max(0, maxDefenseTroops - (int) alreadyDefending);

        availableTroops = availableTroops.stream().limit(remainingCapacity).toList();

        Map<Tile, List<TroopStack>> stationed = new HashMap<>();

        for (TroopStack troop : troops) stationed.computeIfAbsent(troop.getTile(), k -> new ArrayList<>()).add(troop);

        List<Tile> sortedBorders = new ArrayList<>(borderTiles);

        sortedBorders.sort(Comparator.comparingInt(tile -> stationed.getOrDefault(tile, List.of()).size()));

        int index = 0;

        for (TroopStack troop : availableTroops) {
            Tile destination = sortedBorders.get(index % sortedBorders.size());

            List<Tile> path = Server.getInstance().getTroopManager().findPath(troop, destination, Server.getInstance().getTileManager());
            Server.getInstance().getTroopManager().addTroopPath(troop, path);
            Server.getInstance().getTroopManager().startMovement(troop);

            index++;
        }

    }

    @Override
    public boolean finished(String country) {
        return getBorderTiles(country).isEmpty();
    }

    private Set<Tile> getBorderTiles(String country) {
        Map<String, Set<Tile>> neighbors = Server.getInstance().getTileManager().getNeighborsOwn(country);

        Set<Tile> borderTiles = new HashSet<>();

        for (Set<Tile> tiles : neighbors.values()) borderTiles.addAll(tiles);

        return borderTiles;
    }
}