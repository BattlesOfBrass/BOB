package de.idiotischer.bob.troop;

import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.state.State;
import de.idiotischer.bob.tile.Tile;

public class Troop {

    private Tile tile;
    private Country controller;


    public Troop(Tile tile, Country controller) {
        this.tile = tile;
        this.controller = controller;
    }

    public String getName() {
        return "";
    }

    public boolean isVisible() {
        return true;
    }

    public String getTemplate() {
        return ""; //again (like in all structures i build) this i
    }

    public Tile getTile() {
        return tile;
    }

    public void setTile(Tile tile) {
        this.tile = tile;
    }

    public Country getController() {
        return controller;
    }
}
