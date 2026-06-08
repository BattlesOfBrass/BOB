package de.idiotischer.bob.state.event;

import de.craftsblock.craftscore.event.CancellableEvent;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.state.State;

public class StateChangedEvent extends CancellableEvent {
    public StateChangedEvent(Country prevCont, Country newCont, State state) {
    }
}
