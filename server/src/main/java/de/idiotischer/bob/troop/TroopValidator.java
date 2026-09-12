package de.idiotischer.bob.troop;

import de.idiotischer.bob.Server;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.player.Player;
import de.idiotischer.bob.tile.Tile;
import de.idiotischer.bob.tile.TileResolver;

import java.util.*;

//Definiert wo truppen actually hin können und wo nd, zum beispiel ohne MA nicht in andere länder etc
//TODO: so coden dass man das easy für navy adapten kann
//TODO: for denmark etc add a isConnectedTo: [ "tile1", "tile2" ] to tiles so we can use it here
public class TroopValidator {


    public static MoveStatus validate(Player mover, TroopStack troopStack, Tile tile, TroopResolver tr, TileResolver resolver) {
        Set<Tile> neighbours = resolver.findNeighbors(troopStack.getTile());

        Country troopController = troopStack.getController();
        Country tileController = troopStack.getTile().getController();
        Country newTileController = tile.getController();

        if (!Objects.equals(newTileController.getAbbreviation(), troopController.getAbbreviation())) {
            boolean isAtWar = Server.getInstance().getWarManager().isAtWar(troopController, newTileController);
            boolean hasMilAccess = newTileController.hasCountryMilAccess(troopController);

            if (!isAtWar && !hasMilAccess) return MoveStatus.FAILURE;
        }

        if (!Objects.equals(tileController.getAbbreviation(), troopController.getAbbreviation())) {
            boolean fightsTogether = Server.getInstance().getWarManager().fightsTogetherWith(troopController, tileController);
            boolean hasMilAccess = tileController.hasCountryMilAccess(troopController);

            if (!fightsTogether && !hasMilAccess) return MoveStatus.FAILURE;
        }

        List<Tile> path = Server.getInstance().getTroopManager().findPath(troopStack, tile, resolver);
        Server.getInstance().getTroopManager().addTroopPath(troopStack, path);
        Server.getInstance().getTroopManager().startMovement(troopStack);

        boolean pathFound = path != null;

        if (!pathFound) return MoveStatus.FAILURE;
        else return MoveStatus.FAILURE_STARTED_PATHFINDING;
    }
}
