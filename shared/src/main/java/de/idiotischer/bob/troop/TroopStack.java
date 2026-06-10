package de.idiotischer.bob.troop;

import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.tile.Tile;

import java.util.List;

public class TroopStack extends Troop {

    private final List<Troop> troops;
    private Tile tile;

    public TroopStack(Tile tile, Country controller, List<Troop> troops) {
        super(tile, controller);

        this.tile = tile;
        this.troops = troops;

        troops.forEach(troop -> troop.setTile(tile));
    }

    public Tile getTile() {
        return tile;
    }

    public void setTile(Tile tile) {
        this.tile = tile;

        this.troops.forEach(troop -> troop.setTile(tile));
    }

    public Country getOwner() {
        return null;
    }

    public Country getController() {
        return null;
    }

    public List<Troop> getTroops() {
        return troops;
    }
}
