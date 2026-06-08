package de.idiotischer.bob.tile;

public interface TileResolver {
    Tile byAbbreviation(String abbreviation);

    Tile fromPos(int x, int y);
}
