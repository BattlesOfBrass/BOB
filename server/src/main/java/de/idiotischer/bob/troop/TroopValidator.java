package de.idiotischer.bob.troop;

import de.idiotischer.bob.Server;
import de.idiotischer.bob.combat.CombatStatus;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.networking.packet.impl.pp.ReplyPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;
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


            Set<TroopStack> enemyStacks = Server.getInstance().getTroopManager().getAt(tile);
            Set<TroopStack> ownStacks = Server.getInstance().getTroopManager().getAt(troopStack.getTile());

            CombatStatus combatHere = Server.getInstance().getCombatManager().enterCombat(new ArrayList<>(ownStacks), new ArrayList<>(enemyStacks));

            if (combatHere != null) {

                Server.getInstance().getCombatManager().onCombatFinished(combat -> {

                    var attackers = combat.getAttackers();
                    var defenders = combat.getDefenders();

                    List<TroopStack> all = new ArrayList<>();
                    all.addAll(attackers);
                    all.addAll(defenders);

                    Set<TroopStack> pushable = Server.getInstance().getTroopManager().getAt(tile);

                    if(!pushable.isEmpty()) {
                        List<Tile> fallbacks = new ArrayList<>(Server.getInstance().getTileManager().findNeighbors(tile));

                        fallbacks.removeIf(tile1 -> !Objects.equals(tile1.getController().getAbbreviation(), new ArrayList<>(pushable).getFirst().getController().getAbbreviation()) &&
                                !Server.getInstance().getWarManager().fightsTogetherWith(tile1.getController(), new ArrayList<>(pushable).getFirst().getController()));

                        if(!git fallbacks.isEmpty()) {
                            Tile tile2 = fallbacks.getFirst();

                            pushable.forEach(p -> {
                                String reply = "troop=" + Server.getInstance().getTroopManager().getUuid(p) + ";tile=" + tile2.getAbbreviation() + ";type=" + MoveStatus.SUCCESS.ordinal();

                                Server.getInstance().getTroopManager().removePathfinding(p);
                                p.setTile(tile);

                                Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().getClients(), new ReplyPacket(Type.TROOPS_MOVE, reply));
                            });
                        } else {
                            pushable.forEach(p -> {Server.getInstance().getTroopManager().removeTroop(p);});
                        }
                    }

                });

                return MoveStatus.FAILURE_FIGHT;
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
