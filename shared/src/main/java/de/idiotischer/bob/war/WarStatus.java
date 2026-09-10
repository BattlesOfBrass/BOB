package de.idiotischer.bob.war;

import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.country.CountryResolver;

import java.util.*;
import java.util.stream.Collectors;

//keeps stuff like surrender progress etc
public class WarStatus {
    private Map<Country, Integer> baseVP;
    private Map<Country, Integer> currentVP;
    private String name;
    private String abbr;
    private LinkedHashSet<Country> attackers;
    private LinkedHashSet<Country> defenders;
    private boolean hasAttackingWon;
    private boolean ended = true;

    public WarStatus(Map<Country, Integer> baseVP, Map<Country, Integer> currentVP, String name, String abbr, LinkedHashSet<Country> attackers, LinkedHashSet<Country> defenders) {
        this.baseVP = baseVP;
        this.currentVP = currentVP;
        this.name = name;
        this.abbr = abbr;
        this.attackers = attackers;
        this.defenders = defenders;
    }

    public void setAttackers(LinkedHashSet<Country> attackers) {
        this.attackers = attackers;
    }

    public void setDefenders(LinkedHashSet<Country> defenders) {
        this.defenders = defenders;
    }

    public LinkedHashSet<Country> getAttackers() {
        return attackers;
    }

    public LinkedHashSet<Country> getDefenders() {
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

    public String toDataString() {
        String attackersStr = attackers.stream().map(c -> c.getAbbreviation() + ":" + currentVP.getOrDefault(c, 0)).collect(Collectors.joining(","));
        String defendersStr = defenders.stream().map(c -> c.getAbbreviation() + ":" + currentVP.getOrDefault(c, 0)).collect(Collectors.joining(","));

        return abbr + "|" + name + "|" + attackersStr + "|" + defendersStr;
    }

    public static WarStatus fromString(String s, CountryResolver cr) {

        String[] parts = s.split("\\|");

        String abbr = parts[0];
        String name = parts[1];

        Map<Country, Integer> baseVP = new HashMap<>();
        Map<Country, Integer> currentVP = new HashMap<>();

        LinkedHashSet<Country> attackers = new LinkedHashSet<>();
        LinkedHashSet<Country> defenders = new LinkedHashSet<>();

        for (String entry : parts[2].split(",")) {
            if (entry.isEmpty()) continue;

            String[] p = entry.split(":");
            Country c = cr.byAbbreviation(p[0]);
            int vp = Integer.parseInt(p[1]);

            attackers.add(c);
            baseVP.put(c, vp);
            currentVP.put(c, vp);
        }

        for (String entry : parts[3].split(",")) {
            if (entry.isEmpty()) continue;

            String[] p = entry.split(":");
            Country c = cr.byAbbreviation(p[0]);
            int vp = Integer.parseInt(p[1]);

            defenders.add(c);
            baseVP.put(c, vp);
            currentVP.put(c, vp);
        }

        return new WarStatus(baseVP, currentVP, name, abbr, attackers, defenders);
    }

    public void setAttackingWon(boolean hasAttackingWon) {
        this.hasAttackingWon = hasAttackingWon;
    }

    public boolean hasAttackingWon() {
        return hasAttackingWon;
    }

    public void end() {
        this.ended = true;
    }

    public boolean isEnded() {
        return ended;
    }

    public Set<Country> getWinningSideByParticipation() {
        Map<Country, Double> participation = getParticipation();

        double attackersParticipation = attackers.stream().mapToDouble(c -> participation.getOrDefault(c, 0.0)).sum();
        double defendersParticipation = defenders.stream().mapToDouble(c -> participation.getOrDefault(c, 0.0)).sum();

        return attackersParticipation >= defendersParticipation ? attackers : defenders;
    }

    public Map<Country, Double> getParticipation() {
        return calculateParticipation(attackers, defenders);
    }

    public Map<Country, Double> getAggressorParticipation() {
        return calculateParticipation(attackers);
    }

    public Map<Country, Double> getLooserParticipation() {
        Set<Country> looser = hasAttackingWon ? defenders : attackers;
        return calculateParticipation(looser);
    }

    private Map<Country, Double> calculateParticipation(Set<Country>... sides) {
        Set<Country> countries = Arrays.stream(sides).flatMap(Set::stream).collect(Collectors.toSet());

        int totalVP = countries.stream().mapToInt(c -> currentVP.getOrDefault(c, 0)).sum();

        if (totalVP == 0) return countries.stream().collect(Collectors.toMap(c -> c, c -> 0.0));

        return countries.stream().collect(Collectors.toMap(c -> c, c -> currentVP.getOrDefault(c, 0) * 100.0 / totalVP));
    }

    public float getSurrenderProgressAttackers() {
        Map<Country, Double> participation = getParticipation();

        double attackersParticipation = attackers.stream().mapToDouble(c -> participation.getOrDefault(c, 0.0)).sum();
        double defendersParticipation = defenders.stream().mapToDouble(c -> participation.getOrDefault(c, 0.0)).sum();

        if (attackersParticipation == 0 && defendersParticipation == 0) return 0.0f;


        double maxParticipation = Math.max(attackersParticipation, defendersParticipation);

        return (float) (attackersParticipation / maxParticipation);
    }

    public float getSurrenderProgressDefenders() {
        Map<Country, Double> participation = getParticipation();

        double attackersParticipation = attackers.stream().mapToDouble(c -> participation.getOrDefault(c, 0.0)).sum();
        double defendersParticipation = defenders.stream().mapToDouble(c -> participation.getOrDefault(c, 0.0)).sum();

        if (attackersParticipation == 0 && defendersParticipation == 0) return 0.0f;

        double maxParticipation = Math.max(attackersParticipation, defendersParticipation);

        return (float) (defendersParticipation / maxParticipation);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WarStatus other)) return false;

        return abbr.equals(other.abbr);
    }

    @Override
    public int hashCode() {
        return abbr.hashCode();
    }

}
