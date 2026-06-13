package de.idiotischer.bob.war;

import de.idiotischer.bob.country.Country;

import java.util.List;

//keeps stuff like surrender progress etc
public record WarStatus(String name, String abbr, List<Country> enemies, List<Country> alliesCalledIn){
}
