package de.idiotischer.bob.troop;

import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.tile.Tile;

//Definiert wo truppen actually hin können und wo nd, zum beispiel ohne MA nicht in andere länder etc
//TODO: so coden dass man das easy für navy adapten kann
//TODO: for denmark etc add a isConnectedTo: [ "tile1", "tile2" ] to tiles so we can use it here
public class MoveGrid {

    public boolean canAttackOrMove(TroopStack stack, Tile target) {
        Country c = stack.getController();
        Tile tile = stack.getTile();

        Country tileOwner = tile.getController();
        Country targetCountry = target.getController();


        return true;
    }
}
