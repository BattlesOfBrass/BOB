package de.idiotischer.bob.tile;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public interface TileResolver {
    Tile byAbbreviation(String abbreviation);

    Tile fromPos(int x, int y);

    Set<Tile> findNeighbors(Tile tile);

    Tile.TileConnection getConnection(Tile a, Tile b);

    Tile.TileConnection getConnection(UUID uuid);

    boolean hasConnection(Tile a, Tile b);
}
