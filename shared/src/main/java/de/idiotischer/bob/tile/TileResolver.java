package de.idiotischer.bob.tile;

import java.util.Set;

public interface TileResolver {
    Tile byAbbreviation(String abbreviation);

    Tile fromPos(int x, int y);

    Set<Tile> findNeighbors(Tile tile);
}
