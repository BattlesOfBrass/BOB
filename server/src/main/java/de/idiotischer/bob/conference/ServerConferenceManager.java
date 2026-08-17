package de.idiotischer.bob.conference;

import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.war.WarStatus;

public class ServerConferenceManager {
    public void createFor(WarStatus status) {
        PeaceConference conference = null;
        //
        //
        //
        //

        beginCycle();
    }

    public void beginCycle() {

    }

    public void nextCycle(Country guyWhoCanChoose) {

        if(guyWhoCanChoose == null || guyWhoCanChoose.getPlayer() == null) {
            nextCycle(null); //any other participant
        }
    }

    public void finalizeConference() {
    }
}
