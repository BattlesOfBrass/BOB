package de.idiotischer.bob.troop;

import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.tile.Tile;
import de.idiotischer.bob.tile.TileResolver;

import java.util.List;
import java.util.Set;

public interface TroopResolver {
    MoveStatus canTraverse(Country troopController, Tile current, Tile neighbour);
    List<Tile> findPath(TroopStack troopStack, Tile destination, TileResolver resolver);
    Set<TroopStack> getAt(Tile tile);
    boolean hasStack(Tile tile);
}
