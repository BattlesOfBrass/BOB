package de.idiotischer.bob.troop;

import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.country.CountryResolver;
import de.idiotischer.bob.state.State;
import de.idiotischer.bob.tile.Tile;
import de.idiotischer.bob.tile.TileResolver;
import it.unimi.dsi.fastutil.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

//TODO: make troops contain their uuid
public class Troop {

    private final String name;
    protected String template;
    private Tile tile;
    private Country controller;
    private Country owner;


    public Troop(String name,Tile tile, Country owner, Country controller, String template) {
        this.name = name;
        this.owner = owner;
        this.template = template;
        this.tile = tile;
        this.controller = controller;
    }

    public String getName() {
        return name;
    }

    public boolean isVisible() {
        return true;
    }

    public String getTemplate() {
        return template; //again (like in all structures i build) this i
    }

    public Tile getTile() {
        return tile;
    }

    public void setTile(Tile tile) {
        this.tile = tile;
    }

    public Country getOwner() {
        return owner;
    }

    public Country getController() {
        return controller != null ? controller : owner;
    }

}
