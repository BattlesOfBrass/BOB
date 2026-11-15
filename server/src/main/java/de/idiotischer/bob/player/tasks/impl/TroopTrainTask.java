package de.idiotischer.bob.player.tasks.impl;

import de.idiotischer.bob.Server;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.player.tasks.Task;
import de.idiotischer.bob.tile.Tile;
import de.idiotischer.bob.troop.TrainEntry;
import de.idiotischer.bob.troop.TroopStack;

import java.util.*;

public class TroopTrainTask implements Task {

    private static final int SIZE = 1;
    private static final int MAX_QUEUE = 8;

    private static final long TPD = 1440L;
    private static final long COOLDOWN_AT_PEACE = 14 * TPD;
    private static final long COOLDOWN_AT_WAR = 3 * TPD;

    private static final double GOAL_PER_TILE_PEACE = 0.2;
    private static final double GOAL_PER_TILE_WAR = 0.75;
    private static final int MIN = 1;

    private static final String DEFAULT_NAME = "Infantry";
    private static final String DEFAULT_TEMPLATE = "";

    private final Map<String, Long> lastStart = new HashMap<>();
    private final Set<String> startedThisRun = new HashSet<>();

    @Override
    public boolean canExecute(String country) {
        Server server = Server.getInstance();

        Country c = server.getCountryManager().byAbbreviation(country);
        if (c == null) return false;

        if (server.getConferenceManager().anyActive()) return false;

        if (countTraining(country) >= MAX_QUEUE) return false;

        boolean atWar = server.getWarManager().isAtWar(country);

        long now = server.getGameManager().getTimeTicks();
        Long last = lastStart.get(country);
        if (last != null && now - last < (atWar ? COOLDOWN_AT_WAR : COOLDOWN_AT_PEACE)) return false;

        if (pickSpawnTile(c) == null) return false;

        if (!hasEnoughManpower(c)) return false;

        return countStacks(country) + countTraining(country) < getTarget(c, atWar);
    }

    @Override
    public double getPriority(String country) {
        if (!canExecute(country)) return 0;

        return Server.getInstance().getWarManager().isAtWar(country) ? 20 : 90;
    }

    @Override
    public void tick(String country) {
        if (startedThisRun.contains(country)) return;

        Server server = Server.getInstance();

        Country c = server.getCountryManager().byAbbreviation(country);
        if (c == null) return;

        Tile spawn = pickSpawnTile(c);
        if (spawn == null) return;

        TroopStack stack = new TroopStack(DEFAULT_NAME, spawn, c, c, SIZE, DEFAULT_TEMPLATE);

        server.getTroopManager().addTroopTraining(stack);

        lastStart.put(country, server.getGameManager().getTimeTicks());
        startedThisRun.add(country);
    }

    @Override
    public boolean finished(String country) {
        return startedThisRun.remove(country);
    }

    private int countStacks(String country) {
        return Server.getInstance().getTroopManager().getForController(country).size();
    }

    private int countTraining(String country) {
        int n = 0;

        for (TrainEntry entry : Server.getInstance().getTroopManager().getTraining()) {
            Country controller = entry.getFinalStack().getController();

            if (controller != null && controller.getAbbreviation().equals(country)) n++;
        }

        return n;
    }

    private int getTarget(Country c, boolean atWar) {
        int tiles = 0;

        for (Tile tile : Server.getInstance().getTileManager().getTileSet())
            if (tile.getController() != null && tile.getController().getAbbreviation().equals(c.getAbbreviation())) tiles++;

        double perTile = atWar ? GOAL_PER_TILE_WAR : GOAL_PER_TILE_PEACE;

        return Math.max(MIN, (int) Math.ceil(tiles * perTile));
    }

    private boolean hasEnoughManpower(Country c) {
        long needed = 9 * 1000L * SIZE;

        return c.getManpower() >= needed;
    }

    private Tile pickSpawnTile(Country c) {
        Tile best = null;
        int bestCount = Integer.MAX_VALUE;

        for (Tile tile : Server.getInstance().getTileManager().getTileSet()) {
            if (tile.getController() == null || !tile.getController().getAbbreviation().equals(c.getAbbreviation())) continue;

            int count = Server.getInstance().getTroopManager().getAt(tile).size();

            if (count < bestCount) {
                best = tile;
                bestCount = count;
            }
        }

        return best;
    }
}