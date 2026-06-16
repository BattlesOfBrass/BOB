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

    public boolean canAttackOrMove(TroopStack stack, Tile target) {
        Country c = stack.getController();
        Tile tile = stack.getTile();

        Country tileOwner = tile.getController();
        Country targetCountry = target.getController();


        return true;
    }

    public static MoveStatus validate(Player mover, TroopStack troopStack, Tile tile, TroopResolver tr, TileResolver resolver) {
        Set<Tile> neighbours = resolver.findNeighbors(troopStack.getTile());

        Country troopController = troopStack.getController();
        Country tileController = troopStack.getTile().getController();
        Country newTileController = tile.getController();

        if(!Objects.equals(newTileController.getAbbreviation(), troopController.getAbbreviation())) {
            if (!Server.getInstance().getWarManager().isAtWar(troopController, newTileController)) return MoveStatus.FAILURE;
        }

        if(!Objects.equals(tileController.getAbbreviation(), troopController.getAbbreviation())) {
            if(!Server.getInstance().getWarManager().fightsTogetherWith(troopController, tileController)) return MoveStatus.FAILURE;
        }

        if (!neighbours.contains(tile)) {

            List<Tile> path = Server.getInstance().getTroopManager().findPath(troopStack,tile,resolver);
            Server.getInstance().getTroopManager().addTroopPath(troopStack,path);
            Server.getInstance().getTroopManager().startMovement(troopStack);

            boolean pathFound = path != null;

            if (!pathFound) return MoveStatus.FAILURE;
            else return MoveStatus.FAILURE_STARTED_PATHFINDING;
        }

        Set<TroopStack> toStacks = tr.getAt(tile);

        if (!toStacks.isEmpty()) {

            if (Server.getInstance()
                    .getWarManager()
                    .isEnemy(troopController, tile.getController())) {

                Set<TroopStack> fromStacks = tr.getAt(troopStack.getTile());

                if (!Server.getInstance()
                        .getCombatManager()
                        .whoWins(fromStacks, toStacks)
                        .contains(troopStack)) {

                    return MoveStatus.FAILURE_FIGHT;
                } else {
                    Server.getInstance().getTroopManager().removeTroops(toStacks);
                }
            }
        }
        if(mover == null) {
            //do smth else
            return MoveStatus.SUCCESS;
        }

        if(!Objects.equals(mover.country().getAbbreviation(), troopController.getAbbreviation())) return MoveStatus.FAILURE_NO_CONTROL;

        return MoveStatus.SUCCESS;
    }
}
