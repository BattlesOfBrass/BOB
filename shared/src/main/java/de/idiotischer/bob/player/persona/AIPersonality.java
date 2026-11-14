package de.idiotischer.bob.player.persona;

public class AIPersonality {

    private double aggression;
    private double diplomacy;
    private double economy;
    private double research;

    public AIPersonality(double aggression, double diplomacy, double economy, double research) {
        this.aggression = aggression;
        this.diplomacy = diplomacy;
        this.economy = economy;
        this.research = research;
    }

    public double getAggression() {
        return aggression;
    }

    public double getDiplomacy() {
        return diplomacy;
    }

    public double getEconomy() {
        return economy;
    }

    public double getResearch() {
        return research;
    }

    public void updateTrait(double set, String traitName) {
        switch (traitName.toLowerCase()) {
            case "aggression": aggression = set;
            case "diplomacy": diplomacy = set;
            case "economy": economy = set;
            case "research": research = set;
            default: break;
        }
    }

    public String toDataString() {
        return aggression + "#" + diplomacy + "#" + economy + "#" + research;
    }

    public static AIPersonality fromDataString(String data) {
        String[] parts = data.split("#");
        if(parts.length != 4) return null;
        double aggression = Double.parseDouble(parts[0]);
        double diplomacy = Double.parseDouble(parts[1]);
        double economy = Double.parseDouble(parts[2]);
        double research = Double.parseDouble(parts[3]);

        return new AIPersonality(aggression,diplomacy,economy,research);
    }
}