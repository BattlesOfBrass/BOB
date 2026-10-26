package de.idiotischer.bob.faction;

public interface FactionResolver {

    Faction.FactionRule ruleBy(String abbreviation);
    Faction factionBy(String abbreviation);
}
