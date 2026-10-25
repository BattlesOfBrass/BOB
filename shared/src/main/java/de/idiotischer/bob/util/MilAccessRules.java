package de.idiotischer.bob.util;

import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.war.WarResolver;

public class MilAccessRules {

    public static boolean canHave(WarResolver r, Country requested, Country requesting) {
        return r.fightsTogetherWith(requested, requesting) || !requesting.getRulingIdeology().abbreviation().equals(requested.getRulingIdeology().abbreviation());
    }
}
