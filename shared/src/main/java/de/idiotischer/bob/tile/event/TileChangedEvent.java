package de.idiotischer.bob.tile.event;

import de.craftsblock.craftscore.event.CancellableEvent;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.tile.Tile;

public class TileChangedEvent extends CancellableEvent {
    public TileChangedEvent(Country prevCont, Country newCont, Tile tile) {
    }
}
