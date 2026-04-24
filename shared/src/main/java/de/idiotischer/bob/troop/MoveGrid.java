package de.idiotischer.bob.troop;

import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.state.State;

//Definiert wo truppen actually hin können und wo nd, zum beispiel ohne MA nicht in andere länder etc
//TODO: so coden dass man das easy für navy adapten kann
//TODO: for denmark etc add a isConnectedTo: [ "state1", "state2" ] to states so we can use it here
public class MoveGrid {

    public boolean canAttackOrMove(TroopStack stack, State target) {
        Country c = stack.getController();
        State state = stack.getState();

        Country stateOwner = state.getController();
        Country targetCountry = target.getController();


        return true;
    }
}
