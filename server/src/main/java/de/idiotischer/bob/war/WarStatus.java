package de.idiotischer.bob.war;

import de.idiotischer.bob.country.Country;

import java.util.List;
import java.util.Map;
import java.util.Set;

//keeps stuff like surrender progress etc
public class WarStatus {
    private Map<Country, Integer> baseVP;
    private Map<Country, Integer> currentVP;
    private String name;
    private String abbr;
    private Set<Country> attackers;
    private Set<Country> defenders;

    public WarStatus(Map<Country, Integer> baseVP, Map<Country, Integer> currentVP, String name, String abbr, Set<Country> attackers, Set<Country> defenders) {
        this.baseVP = baseVP;
        this.currentVP = currentVP;
        this.name = name;
        this.abbr = abbr;
        this.attackers = attackers;
        this.defenders = defenders;
    }

    public void setAttackers(Set<Country> attackers) {
        this.attackers = attackers;
    }

    public void setDefenders(Set<Country> defenders) {
        this.defenders = defenders;
    }

    public Set<Country> getAttackers() {
        return attackers;
    }

    public Set<Country> getDefenders() {
        return defenders;
    }

    public String getAbbr() {
        return abbr;
    }

    public String getName() {
        return name;
    }

    public Map<Country, Integer> getCurrentVP() {
        return currentVP;
    }

    public Map<Country, Integer> getBaseVP() {
        return baseVP;
    }

    public void setBaseVP(Map<Country, Integer> baseVP) {
        this.baseVP = baseVP;
    }

    public void setCurrentVP(Map<Country, Integer> currentVP) {
        this.currentVP = currentVP;
    }
}
