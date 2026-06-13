package de.idiotischer.bob.troop;

import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.player.Player;
import de.idiotischer.bob.tile.Tile;
import de.idiotischer.bob.tile.TileResolver;

import javax.net.ServerSocketFactory;
import java.util.Objects;
import java.util.Set;

//Definiert wo truppen actually hin können und wo nd, zum beispiel ohne MA nicht in andere länder etc
//TODO: so coden dass man das easy für navy adapten kann
//TODO: for denmark etc add a isConnectedTo: [ "tile1", "tile2" ] to tiles so we can use it here
public class TroopValidator {

    public boolean canAttackOrMove(TroopStack stack, Tile target) {
        Country c = stack.getController();
        Tile tile = stack.getTile();

        Country tileOwner = tile.getController();
        Country targetCountry = target.getController();


        return true;
    }

    public static MoveStatus validate(Player mover, TroopStack troopStack, Tile tile, TileResolver resolver) {
        Set<Tile> neighbours = resolver.findNeighbors(troopStack.getTile());

        if(!neighbours.contains(tile)) return MoveStatus.FAILURE;

        if(mover == null) {
            //do smth else
            return MoveStatus.SUCCESS;
        }

        if(!Objects.equals(mover.country().getAbbreviation(), troopStack.getController().getAbbreviation())) return MoveStatus.NO_CONTROL;

        return MoveStatus.SUCCESS;
    }
}
